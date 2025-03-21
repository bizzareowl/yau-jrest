package org.owl.services.yaujrest;

/**
 * Исключение выбрасываемое в случае непредвиденной ошибки во время запуска главного обработчика
 */
public class DispatchControllerStartUpException extends RuntimeException {
    public DispatchControllerStartUpException(String message, Exception exception) {
        super(message, exception);
    }
}
