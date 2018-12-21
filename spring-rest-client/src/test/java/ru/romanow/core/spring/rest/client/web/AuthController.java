package ru.romanow.core.spring.rest.client.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.romanow.core.spring.rest.client.model.SimpleResponse;

import static ru.romanow.core.spring.rest.client.Constants.*;

@RestController
public class AuthController {
    private static int timeoutCounter = 0;
    private static int serverErrorCounter = 0;

    @GetMapping(value = BAD_GATEWAY_ERROR_RETRY)
    public ResponseEntity<SimpleResponse> serverErrorRetry() {
        if (serverErrorCounter++ < 2) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
        return ResponseEntity.ok(new SimpleResponse("Bad Gateway"));
    }

    @GetMapping(value = TIMEOUT)
    public void timeout() throws InterruptedException {
        Thread.sleep(5 * 1000);
    }

    @GetMapping(value = TIMEOUT_RETRY)
    public SimpleResponse timeoutRetry() throws InterruptedException {
        if (timeoutCounter++ < 2) {
            Thread.sleep(5 * 1000);
        }
        return new SimpleResponse("OK");
    }
}
