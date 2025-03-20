package org.owl.services.yaujrest.controller;

/**
 * Исключение выбрасываемое при попытке вызова метода {@code do*()} класса {@code Controller},
 * который не переопределен в реализации
 */
public class NotImplementedException extends RuntimeException {
    public NotImplementedException(String message) {
        super(message);
    }
}
