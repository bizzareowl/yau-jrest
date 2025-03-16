package org.owl.services.yaujrest.http.request;

import org.owl.services.yaujrest.http.Version;
import org.owl.services.yaujrest.http.Method;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HttpRequestParser {

    private static final int MARK_READ_LIMIT = 1024;

    public HttpRequest parse(final InputStream requestInputStream) {
        try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(requestInputStream))) {
            final Method method = parseMethod(bufferedReader);
            final URI uri = parseURI(bufferedReader);
            final Version version = parseVersion(bufferedReader);
            skipCRLF(bufferedReader);

            final Map<String, String> headers = isNotCRLF(bufferedReader) ? parseHeaders(bufferedReader) : null;
            skipCRLF(bufferedReader);

            final int[] body = parseBody(bufferedReader);

            return new HttpRequest(method, uri, version, headers, body);
        } catch (IOException | URISyntaxException ioe) {
            throw new HttpRequestParseException("Error while parsing HTTP request", ioe);
        }
    }

    private Method parseMethod(final Reader reader) throws IOException {
        skipWhitespaces(reader);
        return Method.valueOf(readNextString(reader));
    }

    private URI parseURI(final Reader reader) throws IOException, URISyntaxException {
        skipWhitespaces(reader);
        return new URI(readNextString(reader));
    }

    private Version parseVersion(final Reader reader) throws IOException {
        skipWhitespaces(reader);
        return readNextString(reader).equals("HTTP/1.1") ? Version.HTTP_1_1 : Version.UNSUPPORTED;
    }

    private Map<String, String> parseHeaders(final Reader reader) throws IOException {
        skipWhitespaces(reader);
        final Map<String, String> headers = new HashMap<>();
        while (isNotCRLF(reader)) {
            final Map.Entry<String, String> header = parseHeader(reader);
            headers.put(header.getKey(), header.getValue());
            skipCRLF(reader);
        }
        return headers;
    }

    private Map.Entry<String, String> parseHeader(final Reader reader) throws IOException {
        skipWhitespaces(reader);
        final String key = readNextString(reader);
        final int currentChar = reader.read();

        if ((char) currentChar != ':') {
            throw new HttpRequestParseException("Colon (:) expected");
        }

        skipWhitespaces(reader);
        final String value = readNextString(reader);
        return Map.entry(key, value);
    }

    private int[] parseBody(final Reader reader) throws IOException {
        final List<Integer> body = new ArrayList<>();
        int currentChar = reader.read();
        while (currentChar != -1) {
            body.add(currentChar);
            currentChar = reader.read();
        }

        return !body.isEmpty() ? body.stream().mapToInt(i -> i).toArray() : null;
    }

    private void skipWhitespaces(final Reader reader) throws IOException {
        reader.mark(MARK_READ_LIMIT);
        int currentChar = reader.read();

        if (!Character.isWhitespace((char) currentChar)) {
            reader.reset();
            return;
        }

        while (Character.isWhitespace((char) currentChar)) {
            reader.mark(MARK_READ_LIMIT);
            currentChar = reader.read();
        }
        reader.reset();
    }

    private String readNextString(final Reader reader) throws IOException {
        final StringBuilder stringBuilder = new StringBuilder();
        int currentChar = reader.read();
        while (isIdentifierCharacter((char) currentChar)) {
            stringBuilder.append((char) currentChar);
            reader.mark(MARK_READ_LIMIT);
            currentChar = reader.read();
        }
        reader.reset();
        return stringBuilder.toString();
    }

    private void skipCRLF(final Reader reader) throws IOException {
        final int firstChar = reader.read(), secondChar = reader.read();
        if (!((char) firstChar == '\r' && (char) secondChar == '\n')) {
            throw new HttpRequestParseException("CRLF expected");
        }
    }

    private boolean isNotCRLF(final Reader reader) throws IOException {
        reader.mark(MARK_READ_LIMIT);
        final int firstChar = reader.read(), secondChar = reader.read();
        final boolean result = firstChar == '\r' && secondChar == '\n';
        reader.reset();
        return !result;
    }

    private boolean isIdentifierCharacter(final char ch) {
        return !Character.isWhitespace(ch) && ch != ':';
    }

}
