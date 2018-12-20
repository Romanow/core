package ru.romanow.core.spring.rest.client;

public class Constants {
    public static final String AUTH = "/auth";
    public static final String PING = "/ping";
    public static final String CUSTOM_HEADER = "/header/custom";
    public static final String QUERY_PARAM = "/query";
    public static final String BAD_REQUEST_ERROR = "/client-error";
    public static final String BAD_REQUEST_ERROR_BODY = "/error/client/body";
    public static final String BAD_GATEWAY_ERROR = "/error/server";
    public static final String BAD_GATEWAY_ERROR_BODY = "/error/server/body";
    public static final String BAD_GATEWAY_ERROR_RETRY = "/error/server/retry";
    public static final String TIMEOUT = "/timeout";
    public static final String TIMEOUT_RETRY = "/timeout/retry";
}
