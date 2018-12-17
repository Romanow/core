package ru.romanow.core.spring.rest.client.exception;

public abstract class HttpRestException
        extends RuntimeException {
    public HttpRestException(String message) {
        super(message);
    }

    public HttpRestException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpRestException(Throwable cause) {
        super(cause);
    }
}
