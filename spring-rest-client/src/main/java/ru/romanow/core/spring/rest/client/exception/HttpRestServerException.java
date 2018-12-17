package ru.romanow.core.spring.rest.client.exception;

public class HttpRestServerException
        extends HttpStatusBasedException {

    public HttpRestServerException(int responseStatus, String responseMessage) {
        this(responseStatus, responseMessage, null);
    }

    public HttpRestServerException( int responseStatus, String responseMessage, Object body) {
        super("Server error " + responseStatus + " - " + responseMessage, responseStatus, responseMessage, body);
    }
}
