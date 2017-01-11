package ru.romanow.core.rest.client.exception;

public class HttpRestTimeoutException
        extends HttpRestException {
    public HttpRestTimeoutException(Throwable cause) {
        super(cause);
    }
}
