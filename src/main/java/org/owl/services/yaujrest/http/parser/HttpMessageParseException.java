package org.owl.services.yaujrest.http.parser;

/**
 * Исключение выбрасываемое в случае ошибки синтаксического разбора HTTP-сообщения
 */
public class HttpMessageParseException extends RuntimeException {
    public HttpMessageParseException(String message) {
        super(message);
    }
}
