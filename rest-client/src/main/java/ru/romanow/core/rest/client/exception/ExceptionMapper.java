package ru.romanow.core.rest.client.exception;

@FunctionalInterface
public interface ExceptionMapper<T extends RuntimeException, F extends HttpRestException> {
    T produce(F exception);
}