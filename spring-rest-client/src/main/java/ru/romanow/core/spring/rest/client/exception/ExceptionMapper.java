package ru.romanow.core.spring.rest.client.exception;

@FunctionalInterface
public interface ExceptionMapper<T extends RuntimeException, F extends HttpRestException> {
    T produce(F exception);
}
