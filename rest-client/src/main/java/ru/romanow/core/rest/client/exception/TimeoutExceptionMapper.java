package ru.romanow.core.rest.client.exception;

import java.util.concurrent.TimeoutException;

@FunctionalInterface
public interface TimeoutExceptionMapper<T extends RuntimeException> {
    T produce(TimeoutException exception);
}