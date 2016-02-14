package ru.romanow.rest.client.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.romanow.rest.client.model.TestAuthRequest;
import ru.romanow.rest.client.model.TestAuthResponse;

/**
 * Created by ronin on 13.02.16
 */
@RestController
@RequestMapping("/rest")
public class TestController {

    public static final String CHECK_ENDPOINT = "/check";
    public static final String AUTH_ENDPOINT = "/auth";
    public static final String PING_ENDPOINT = "/ping";

    @RequestMapping(value = CHECK_ENDPOINT, method = RequestMethod.GET)
    public Boolean check(@RequestParam String user) {
        return user.length() > 5;
    }

    @RequestMapping(value = AUTH_ENDPOINT, method = RequestMethod.POST)
    public TestAuthResponse auth(@RequestBody TestAuthRequest request) {
        return null;
    }

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = PING_ENDPOINT, method = RequestMethod.GET)
    public String ping() {
        return "ok";
    }
}
