package ru.romanow.core.rest.client.model;

/**
 * User: romanow
 * Date: 15.02.16
 */
public class SimpleResponse {
    private String message;

    public SimpleResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
