package org.owl.services.yaujrest.http.parser;

import org.owl.services.yaujrest.http.HttpResponse;
import org.owl.services.yaujrest.http.Method;
import org.owl.services.yaujrest.http.Version;
import org.owl.services.yaujrest.http.HttpRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Синтаксический анализатор HTTP-сообщений.
 * <p>
 * Позволяет получать представление HTTP-запросов в виде объектов класса {@link HttpRequest},
 * а также HTTP-ответов в виде объектов класса {@link HttpResponse}.
 */
public final class HttpMessageParser {

    /**
     * Лексический анализатор.
     * Упрощает работу с входным потоком HTTP-сообщения
     */
    private static final class Lexer {

        /**
         * Максимальный размер буффера
         */
        private static final int BUFFER_SIZE = 1024;

        /**
         * Входной поток HTTP-сообщения
         */
        private final InputStream inputStream;

        /**
         * Буффер входных байтов
         */
        private byte[] buffer;

        /**
         * Текущая позиция в буфере входных байтов
         */
        private int currentPosition;

        /**
         * Флаг достижения конца входного потока HTTP-сообщения
         */
        private boolean eof;

        /**
         * Стандартный конструктор лексического анализатора.
         * <p>
         * Инициализирует буффер входных байтов
         * @param inputStream входной поток HTTP-сообщения
         */
        private Lexer(final InputStream inputStream) {
            this.inputStream = inputStream;
            try {
                this.buffer = this.inputStream.readNBytes(BUFFER_SIZE);
            } catch (IOException ioe) {
                throw new HttpMessageParseException("Unable to parse HTTP request");
            }
        }

        /**
         * Возвращает текущий байт в буффере входных байтов
         * @return текущий байт в буфере
         */
        public byte fetch() {
            if (this.currentPosition >= this.buffer.length) {
                try {
                    next();
                } catch (IOException ioe) {
                    throw new HttpMessageParseException("Unable to parse HTTP request");
                }
            }

            if (this.eof) {
                return -1;
            }

            return this.buffer[this.currentPosition];
        }

        /**
         * Перемещает указатель текущего байта в буфере на следующую позицию.
         * <p>
         * При достижении конца буффера считывает очередную порцию байтов из входного потока HTTP-сообщения. В случае
         * если входной поток достиг конца устанавливает флаг {@code this.eof = true}.
         * @throws IOException исключение выбрасываемое при ошибке чтения
         * очередной порции байтов из входного потока HTTP-сообщения
         */
        public void next() throws IOException {
            if (Objects.isNull(this.buffer) || this.currentPosition >= this.buffer.length) {
                try {
                    this.buffer = this.inputStream.readNBytes(BUFFER_SIZE);
                    this.currentPosition = 0;

                    if (this.buffer.length == 0) {
                        this.eof = true;
                        this.inputStream.close();
                    }
                } catch (IOException ioe) {
                    this.inputStream.close();
                    throw ioe;
                }
            } else {
                this.currentPosition++;
            }
        }
    }

    /**
     * Анализирует входной поток байтов HTTP-ответа и в случае корректного формата
     * возвращает объект класса {@link HttpResponse}
     * @param inputStream входной поток байтов HTTP-ответа
     * @return объект класса {@link HttpResponse}
     * @throws HttpMessageParseException выбрасывается в случае некорректного формата HTTP-ответа
     */
    public HttpResponse parseHttpResponse(final InputStream inputStream) throws HttpMessageParseException {
        final Lexer lexer = new Lexer(inputStream);
        return parseHttpResponse(lexer);
    }

    /**
     * Анализирует входной поток байтов HTTP-ответа и в случае корректного формата
     * возвращает объект класса {@link HttpResponse}
     * @param lexer лексический анализатор входного потока байтов
     * @return объект класса {@link HttpResponse}
     */
    private HttpResponse parseHttpResponse(final Lexer lexer) {
        final Version version = parseVersion(lexer);
        match(lexer, ' ');

        final int statusCode = parseStatusCode(lexer);
        match(lexer, ' ');

        final String reason = parseReason(lexer);
        matchCRLF(lexer);

        final Map<String, String> headers = parseHeaders(lexer);
        matchCRLF(lexer);

        final byte[] body = parseBody(lexer);

        return new HttpResponse(version, statusCode, reason, headers, body);
    }

    /**
     * Анализирует код ответа и возвращает его числовое представление
     * @param lexer лексический анализатор входного потока байтов
     * @return числовое представление кода ответа
     */
    private int parseStatusCode(final Lexer lexer) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            stringBuilder.append((char) lexer.fetch());
            try {
                lexer.next();
            } catch (IOException ioe) {
                throw new HttpMessageParseException("Error while parsing status code");
            }
        }
        return Integer.parseInt(stringBuilder.toString());
    }

    /**
     * Анализирует поясняющую фразу HTTP-ответа, в случае ее присутствия возвращает строковое представление
     * @param lexer лексический анализатор входного потока байтов
     * @return строковое представление пояснительной фразы HTTP-ответа при ее наличии, иначе {@code null}
     */
    private String parseReason(final Lexer lexer) {
        if ((char) lexer.fetch() == '\r') {
            return null;
        }

        return parseValue(lexer, ch -> isVChar(ch) || isOBSText(ch) || ch == '\t' || ch == ' ');
    }

    /**
     * Анализирует входной поток байтов HTTP-запроса и в случае корректного формата
     * возвращает объект класса {@link HttpRequest}
     * @param inputStream входной поток байтов HTTP-запроса
     * @return объект класса {@link HttpRequest}
     * @throws HttpMessageParseException выбрасывается в случае некорректного формата HTTP-запроса
     */
    public HttpRequest parseHttpRequest(final InputStream inputStream) throws HttpMessageParseException {
        final Lexer lexer = new Lexer(inputStream);
        return parseHttpRequest(lexer);
    }

    /**
     * Анализирует входной поток байтов HTTP-запроса и в случае корректного формата
     * возвращает объект класса {@link HttpRequest}
     * @param lexer лексический анализатор входного потока байтов
     * @return объект класса {@link HttpRequest}
     */
    private HttpRequest parseHttpRequest(final Lexer lexer) {
        final Method method = parseMethod(lexer);
        match(lexer, ' ');

        final URI uri = parseUri(lexer);
        match(lexer, ' ');

        final Version version = parseVersion(lexer);
        matchCRLF(lexer);

        final Map<String, String> headers = parseHeaders(lexer);
        matchCRLF(lexer);

        final byte[] body = parseBody(lexer);

        return new HttpRequest(method, uri, version, headers, body);
    }

    /**
     * Анализирует метод HTTP-запроса
     * @param lexer лексический анализатор входного потока байтов
     * @return значение метода HTTP-запроса в виде {@link Method}
     */
    private Method parseMethod(final Lexer lexer) {
        final String value = parseValue(lexer, this::isTChar);
        return Method.valueOf(value);
    }

    /**
     * Анализирует строковое значение в зависимости от предиката, затем возвращает его
     * @param lexer лексический анализатор входного потока байтов
     * @param predicate предикат, определяющий терминальные символы для строкового значения
     * @return текущее строковое значение
     */
    private String parseValue(final Lexer lexer, final Predicate<Character> predicate) {
        final StringBuilder stringBuilder = new StringBuilder();
        while (predicate.test((char) lexer.fetch())) {
            stringBuilder.append((char) lexer.fetch());
            try {
                lexer.next();
            } catch (IOException ioe) {
                throw new HttpMessageParseException("Error while parsing value");
            }
        }

        if (stringBuilder.isEmpty()) {
            throw new HttpMessageParseException("Expected value or token");
        }

        return stringBuilder.toString();
    }

    /**
     * Анализирует URI HTTP-запроса
     * @param lexer лексический анализатор входного потока байтов
     * @return объект класса {@link URI}
     */
    private URI parseUri(final Lexer lexer) {
        return URI.create(parseValue(lexer, ch -> !Character.isWhitespace(ch)));
    }

    /**
     * Анализирует используемую HTTP-запросом версию HTTP-протокола
     * @param lexer лексический анализатор входного потока байтов
     * @return объект-значение {@link Version}
     */
    private Version parseVersion(final Lexer lexer) {
        final String httpWord = parseValue(lexer, this::isTChar);

        if (!httpWord.equals("HTTP")) {
            throw new HttpMessageParseException("Unexpected token while parsing HTTP version");
        }

        try {
            match(lexer, '/');
            final int major = Integer.parseInt(String.valueOf((char) lexer.fetch()));
            lexer.next();
            match(lexer, '.');
            final int minor = Integer.parseInt(String.valueOf((char) lexer.fetch()));
            lexer.next();

            return new Version(major, minor);
        } catch (IOException ioe) {
            throw new HttpMessageParseException("Error while matching character");
        }
    }

    /**
     * Анализирует заголовки HTTP-сообщения
     * @param lexer лексический анализатор входного потока байтов
     * @return заголовки HTTP-сообщения в объекте интерфейса {@link Map}
     */
    private Map<String, String> parseHeaders(final Lexer lexer) {
        if ((char) lexer.fetch() == '\r') {
            return null;
        }

        final Map<String, String> result = new HashMap<>();
        while ((char) lexer.fetch() != '\r') {
            final Map.Entry<String, String> header = parseHeader(lexer);
            result.put(header.getKey(), header.getValue());
        }

        return result;
    }

    /**
     * Анализирует текущий HTTP-заголовок
     * @param lexer лексический анализатор входного потока байтов
     * @return заголовок HTTP-сообщения в объекте интерфейса {@link Map.Entry}
     */
    private Map.Entry<String, String> parseHeader(final Lexer lexer) {
        final String fieldName = parseValue(lexer, this::isTChar);
        match(lexer, ':');
        matchOWS(lexer);
        final String fieldValue = parseFieldValue(lexer);
        matchOWS(lexer);
        matchCRLF(lexer);

        return Map.entry(fieldName, fieldValue);
    }

    /**
     * Анализирует значение HTTP-заголовка
     * @param lexer лексический анализатор входного потока байтов
     * @return строковое значение HTTP-заголовка
     */
    private String parseFieldValue(final Lexer lexer) {
        return parseValue(lexer, ch -> isVChar(ch) || isOBSText(ch) || ch == '\t' || ch == ' ');
    }

    /**
     * Анализирует тело HTTP-сообщения
     * @param lexer лексический анализатор входного потока байтов
     * @return массив байт тела HTTP-сообщения
     */
    private byte[] parseBody(final Lexer lexer) {
        final List<Byte> body = new ArrayList<>();
        while (lexer.fetch() != -1) {
            body.add(lexer.fetch());
            try {
                lexer.next();
            } catch (IOException ioe) {
                throw new HttpMessageParseException("Error while parsing request body");
            }
        }

        if (body.isEmpty()) {
            return null;
        }

        final byte[] bytes = new byte[body.size()];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = body.get(i);
        }

        return bytes;
    }

    /**
     * Проверяет входной поток байтов на наличие проверяемого символа
     * @param lexer лексический анализатор входного потока байтов
     * @param ch проверяемый символ
     */
    private void match(final Lexer lexer, final char ch) {
        if ((char) lexer.fetch() != ch) {
            throw new HttpMessageParseException("Unexpected character occurred while parsing HTTP message: " + ch);
        }

        try {
            lexer.next();
        } catch (IOException ioe) {
            throw new HttpMessageParseException("Error while matching character");
        }
    }

    /**
     * Проверяет входной поток байтов на наличие последовательности символов CRLF
     * @param lexer лексический анализатор входного потока байтов
     */
    private void matchCRLF(final Lexer lexer) {
        match(lexer, '\r');
        match(lexer, '\n');
    }

    /**
     * Проверяет входной поток на наличие опциональных "пустых" символов
     * @param lexer лексический анализатор входного потока байтов
     */
    private void matchOWS(final Lexer lexer) {
        while ((char) lexer.fetch() == ' ' || (char) lexer.fetch() == '\t') {
            try {
                lexer.next();
            } catch (IOException ioe) {
                throw new HttpMessageParseException("Error while matching optional whitespaces");
            }
        }
    }

    /**
     * Проверяет, входит ли символ в список символов допустимых в нахождении в токенах
     * @param ch проверяемый символ
     * @return {@code true} если входит, иначе {@code false}
     */
    private boolean isTChar(final char ch) {
        return Character.isLetter(ch) || Character.isDigit(ch) || ch == '!' || ch == '#' || ch == '$' || ch == '%'
                || ch == '&' || ch == '\'' || ch == '*' || ch == '+' || ch == '-' || ch == '.' || ch == '^' || ch == '_'
                || ch == '`' || ch == '|' || ch =='~';
    }

    /**
     * Проверяет, входит ли символ в видимые (visible) символы
     * @param ch проверяемый символ
     * @return {@code true} если входит, иначе {@code false}
     */
    private boolean isVChar(final char ch) {
        return ch >= 0x21 && ch <= 0x7E;
    }

    /**
     * Проверяет, входит ли символ в "высокие" символы US-ASCII
     * @param ch проверяемый символ
     * @return {@code true} если входит, иначе {@code false}
     */
    private boolean isOBSText(final char ch) {
        return ch >= 0x80 && ch <= 0xFF;
    }

}
