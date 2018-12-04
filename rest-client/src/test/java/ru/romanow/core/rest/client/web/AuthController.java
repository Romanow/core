package ru.romanow.core.rest.client.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.romanow.core.rest.client.model.AuthRequest;
import ru.romanow.core.rest.client.model.AuthResponse;
import ru.romanow.core.rest.client.model.PingResponse;

import java.util.UUID;

@RestController
public class AuthController {
    public static final String AUTH = "/auth";
    public static final String PING = "/ping";
    public static final String BAD_REQUEST_ERROR = "/client-error";
    public static final String BAD_REQUEST_ERROR_BODY = "/error/client/body";
    public static final String BAD_GATEWAY_ERROR = "/error/server";

    @PostMapping(value = AUTH,
            consumes = MediaType.APPLICATION_JSON_UTF8_VALUE,
            produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public AuthResponse auth(@RequestBody AuthRequest request) {
        return new AuthResponse(UUID.randomUUID(), 100L, true);
    }

    @GetMapping(value = PING, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public PingResponse ping() {
        return new PingResponse("OK");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @GetMapping(value = BAD_REQUEST_ERROR)
    public void clientError() {}

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @GetMapping(value = BAD_REQUEST_ERROR_BODY, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public PingResponse clientErrorWithBody() {
        return new PingResponse("Bad Request");
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @GetMapping(value = BAD_GATEWAY_ERROR)
    public void serverError() {}
}
