package ru.romanow.core.spring.rest.client.exceptions;

public class CustomException
        extends RuntimeException {
    public CustomException(Throwable cause) {
        super(cause);
    }

    public CustomException(String message) {
        super(message);
    }
}
