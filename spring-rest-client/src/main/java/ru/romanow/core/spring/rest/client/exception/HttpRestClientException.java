package ru.romanow.core.spring.rest.client.exception;

public class HttpRestClientException
        extends HttpStatusBasedException {

    public HttpRestClientException(int responseStatus, String responseMessage) {
        this(responseStatus, responseMessage, null);
    }

    public HttpRestClientException(int responseStatus, String responseMessage, Object body) {
        super("Client error " + responseStatus + ": " + responseMessage, responseStatus, responseMessage, body);
    }
}
