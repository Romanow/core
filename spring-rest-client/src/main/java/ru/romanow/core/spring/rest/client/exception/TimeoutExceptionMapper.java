package ru.romanow.core.spring.rest.client.exception;

@FunctionalInterface
public interface TimeoutExceptionMapper<T extends RuntimeException> {
    T produce(HttpRestTimeoutException exception);
}
