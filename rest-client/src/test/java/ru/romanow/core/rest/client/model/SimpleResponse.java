package ru.romanow.core.rest.client.model;

public class SimpleResponse {
    private String message;

    public SimpleResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
