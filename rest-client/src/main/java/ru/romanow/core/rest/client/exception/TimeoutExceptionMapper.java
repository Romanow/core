package ru.romanow.core.rest.client.exception;

@FunctionalInterface
public interface TimeoutExceptionMapper<T extends RuntimeException> {
    T produce(HttpRestTimeoutException exception);
}