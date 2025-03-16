package org.owl.services.yaujrest.http.request;

public class HttpRequestParseException extends RuntimeException {

    public HttpRequestParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpRequestParseException(String message) {
        super(message);
    }
}
