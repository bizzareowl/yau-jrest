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

public final class HttpMessageParser {

    private static final class Lexer {

        private static final int BUFFER_SIZE = 1024;

        private final InputStream inputStream;

        private byte[] buffer;

        private int currentPosition;

        private boolean eof;

        private Lexer(final InputStream inputStream) {
            this.inputStream = inputStream;
            try {
                this.buffer = this.inputStream.readNBytes(BUFFER_SIZE);
            } catch (IOException ioe) {
                throw new HttpMessageParseException("Unable to parse HTTP request");
            }
        }

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

    public HttpResponse parseHttpResponse(final InputStream inputStream) throws HttpMessageParseException {
        final Lexer lexer = new Lexer(inputStream);
        return parseHttpResponse(lexer);
    }

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

    private String parseReason(final Lexer lexer) {
        if ((char) lexer.fetch() == '\r') {
            return null;
        }

        return parseValue(lexer, ch -> isVChar(ch) || isOBSText(ch) || ch == '\t' || ch == ' ');
    }

    public HttpRequest parseHttpRequest(final InputStream inputStream) throws HttpMessageParseException {
        final Lexer lexer = new Lexer(inputStream);
        return parseHttpRequest(lexer);
    }

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

    private Method parseMethod(final Lexer lexer) {
        final String value = parseValue(lexer, this::isTChar);
        return Method.valueOf(value);
    }

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

    private URI parseUri(final Lexer lexer) {
        return URI.create(parseValue(lexer, ch -> !Character.isWhitespace(ch)));
    }

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

    private Map.Entry<String, String> parseHeader(final Lexer lexer) {
        final String fieldName = parseValue(lexer, this::isTChar);
        match(lexer, ':');
        matchOWS(lexer);
        final String fieldValue = parseFieldValue(lexer);
        matchOWS(lexer);
        matchCRLF(lexer);

        return Map.entry(fieldName, fieldValue);
    }

    private String parseFieldValue(final Lexer lexer) {
        return parseValue(lexer, ch -> isVChar(ch) || isOBSText(ch) || ch == '\t' || ch == ' ');
    }

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

    private void matchCRLF(final Lexer lexer) {
        match(lexer, '\r');
        match(lexer, '\n');
    }

    private void matchOWS(final Lexer lexer) {
        while ((char) lexer.fetch() == ' ' || (char) lexer.fetch() == '\t') {
            try {
                lexer.next();
            } catch (IOException ioe) {
                throw new HttpMessageParseException("Error while matching optional whitespaces");
            }
        }
    }

    private boolean isTChar(final char ch) {
        return Character.isLetter(ch) || Character.isDigit(ch) || ch == '!' || ch == '#' || ch == '$' || ch == '%'
                || ch == '&' || ch == '\'' || ch == '*' || ch == '+' || ch == '-' || ch == '.' || ch == '^' || ch == '_'
                || ch == '`' || ch == '|' || ch =='~';
    }

    private boolean isVChar(final char ch) {
        return ch >= 0x21 && ch <= 0x7E;
    }

    private boolean isOBSText(final char ch) {
        return ch >= 0x80 && ch <= 0xFF;
    }

}
