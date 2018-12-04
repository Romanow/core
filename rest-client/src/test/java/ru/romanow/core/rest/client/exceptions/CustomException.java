package ru.romanow.core.rest.client.exceptions;

public class CustomException
        extends RuntimeException {
    public CustomException(String message) {
        super(message);
    }
}
