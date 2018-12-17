package ru.romanow.core.spring.rest.client.exception;

public abstract class HttpStatusBasedException
        extends HttpRestException {
    private int responseStatus;
    private String responseMessage;
    private Object body;

    public HttpStatusBasedException(String message, int responseStatus, String responseMessage) {
        super(message);
        this.responseStatus = responseStatus;
        this.responseMessage = responseMessage;
    }

    public HttpStatusBasedException(String message, int responseStatus, String responseMessage, Object body) {
        super(message);
        this.responseStatus = responseStatus;
        this.responseMessage = responseMessage;
        this.body = body;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public Object getBody() {
        return body;
    }
}
