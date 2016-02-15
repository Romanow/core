package ru.romanow.rest.client;

import org.boon.json.JsonFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.romanow.rest.client.configuration.TestServerConfiguration;
import ru.romanow.rest.client.model.AuthRequest;
import ru.romanow.rest.client.model.AuthResponse;
import ru.romanow.rest.client.model.SimpleResponse;

import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Created by ronin on 12.02.16
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestServerConfiguration.class)
public class RestClientTest {

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer server;
    private RestClient restClient;

    @Before
    public void init() {
        server = MockRestServiceServer.createServer(restTemplate);
        restClient = new RestClient(restTemplate);
    }

    @Test
    public void testGetSuccess() {
        String url = "/ping";
        server.expect(requestTo(url))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withStatus(HttpStatus.OK)
                                  .contentType(MediaType.APPLICATION_JSON)
                                  .body("true"));

        Optional<Boolean> result =
                restClient.get(url, Boolean.class).make();

        server.verify();

        assertTrue(result.isPresent());
        assertTrue(result.get());
    }

    @Test
    public void testInternalErrorMapping() {
        String url = "/error";
        server.expect(requestTo(url))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withServerError());

        try {
            restClient.get(url, Boolean.class)
                      .addExceptionMapping(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                           RuntimeException::new)
                      .make();
        } catch (RuntimeException exception) {
            assertEquals(HttpServerErrorException.class, exception.getCause().getClass());
        }
        server.verify();
    }

    @Test
    public void testBadRequestThrowing() {
        String url = "/error";
        server.expect(requestTo(url))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withBadRequest());

        try {
            restClient.get(url, Boolean.class).make();
        } catch (RestClientException exception) {
            assertEquals(HttpClientErrorException.class, exception.getClass());
        }
        server.verify();
    }

    @Test
    public void testInternalErrorSuppress() {
        String url = "/error";
        server.expect(requestTo(url))
              .andExpect(method(HttpMethod.GET))
              .andRespond(withServerError());

        final String message = "test";
        Optional<SimpleResponse> response =
                restClient.get(url, SimpleResponse.class)
                          .processServerExceptions(false)
                          .processServerExceptions(false)
                          .defaultResponse(Optional.of(new SimpleResponse(message)))
                          .make();

        server.verify();

        assertTrue(response.isPresent());
        assertEquals(message, response.get().getMessage());
    }

    @Test
    public void testPostSuccess() {
        String url = "/auth";
        String login = "ronin";
        String password = "qwerty";

        AuthRequest request =
                new AuthRequest(login, password);

        String uin = "123";
        Long expiredIn = 123L;
        AuthResponse response =
                new AuthResponse(uin, expiredIn, true);

        server.expect(requestTo(url))
              .andExpect(content().string(JsonFactory.toJson(request)))
              .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
              .andExpect(method(HttpMethod.POST))
              .andRespond(withSuccess(JsonFactory.toJson(response), MediaType.APPLICATION_JSON_UTF8));

        Optional<AuthResponse> result =
                restClient.post(url, AuthResponse.class)
                          .requestBody(request)
                          .make();

        server.verify();

        assertTrue(result.isPresent());
        AuthResponse authResponse = result.get();
        assertTrue(authResponse.getActive());
        assertEquals(uin, authResponse.getUin());
        assertEquals(expiredIn, authResponse.getExpiredIn());
    }
}