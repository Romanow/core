package ru.romanow.core.spring.rest.client.exception;

public class HttpRestTimeoutException
        extends HttpRestException {
    public HttpRestTimeoutException(Throwable cause) {
        super(cause);
    }
}
