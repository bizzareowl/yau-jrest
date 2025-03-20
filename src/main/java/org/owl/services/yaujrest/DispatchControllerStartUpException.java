package org.owl.services.yaujrest;

public class DispatchControllerStartUpException extends RuntimeException {
    public DispatchControllerStartUpException(String message, Exception exception) {
        super(message, exception);
    }
}
