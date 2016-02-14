package ru.romanow.rest.client.exceptions;

/**
 * Created by ronin on 13.02.16
 */
public class RestResponseException
        extends RuntimeException {
    private Integer statusCode;
    private String statusMessage;
    private Object body;

    public RestResponseException(Integer statusCode, String statusMessage) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    public RestResponseException(Integer statusCode, String statusMessage, Object body) {
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.body = body;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Object getBody() {
        return body;
    }
}
