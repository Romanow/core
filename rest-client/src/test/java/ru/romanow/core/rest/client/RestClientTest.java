package ru.romanow.core.rest.client;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.romanow.core.rest.client.exception.HttpRestClientException;
import ru.romanow.core.rest.client.exception.HttpRestServerException;
import ru.romanow.core.rest.client.exceptions.CustomException;
import ru.romanow.core.rest.client.model.AuthRequest;
import ru.romanow.core.rest.client.model.AuthResponse;
import ru.romanow.core.rest.client.model.PingResponse;
import ru.romanow.core.rest.client.model.SimpleResponse;

import java.util.Optional;

import static java.lang.String.format;
import static org.junit.Assert.*;
import static ru.romanow.core.rest.client.utils.JsonSerializer.toJson;
import static ru.romanow.core.rest.client.web.AuthController.*;

/*
 TODO
 1. retryServerError
 2. timeout retry
 3. timeout mapping
 4. connection error
 5. host not found error
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RestClientTest {

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @Before
    public void init() {
        restClient = new RestClient();
    }

    @Test
    public void testGetSuccess() {
        final String url = format("http://localhost:%d/%s", port, PING);
        final Optional<PingResponse> response =
                restClient.get(url, PingResponse.class)
                          .execute();

        assertTrue(response.isPresent());
    }

    @Test
    public void testGetWithHeaderSuccess() {
        final String url = format("http://localhost:%d/%s", port, CUSTOM_HEADER);
        final String param = "test";
        final Optional<SimpleResponse> response =
                restClient.get(url, SimpleResponse.class)
                          .addHeader("X-CUSTOM-HEADER", param)
                          .execute();

        assertTrue(response.isPresent());
        assertEquals(param, response.get().getMessage());
    }

    @Test
    public void testGetWithParamSuccess() {
        final String url = format("http://localhost:%d/%s", port, QUERY_PARAM);
        final String param = "test";
        final Optional<SimpleResponse> response =
                restClient.get(url, SimpleResponse.class)
                          .addParam("query", param)
                          .execute();

        assertTrue(response.isPresent());
        assertEquals(param, response.get().getMessage());
    }

    @Test
    public void testPostSuccess() {
        final String url = format("http://localhost:%d/%s", port, AUTH);
        final Optional<AuthResponse> response =
                restClient.post(url, AuthResponse.class)
                          .requestBody(new AuthRequest("ronin", "test"))
                          .execute();

        assertTrue(response.isPresent());
    }

    @Test
    public void testClientErrorSuppress() {
        final String url = format("http://localhost:%d/%s", port, BAD_REQUEST_ERROR);
        final Optional<Void> response =
                restClient.get(url, Void.class)
                          .processClientExceptions(false)
                          .execute();

        assertFalse(response.isPresent());
    }

    @Test
    public void testClientError() {
        final String url = format("http://localhost:%d/%s", port, BAD_REQUEST_ERROR);
        try {
            final Optional<Void> response =
                    restClient.get(url, Void.class)
                              .execute();
        } catch (HttpRestClientException exception) {
            assertEquals(HttpStatus.SC_BAD_REQUEST, exception.getResponseStatus());
            assertNull(exception.getBody());
        }
    }

    @Test
    public void testClientErrorWithBody() {
        final String url = format("http://localhost:%d/%s", port, BAD_REQUEST_ERROR_BODY);
        try {
            final Optional<Void> response =
                    restClient.get(url, Void.class)
                              .execute();
        } catch (HttpRestClientException exception) {
            assertEquals(HttpStatus.SC_BAD_REQUEST, exception.getResponseStatus());
            assertNotNull(exception.getBody());
            assertEquals(toJson(new SimpleResponse("Bad Request")), exception.getBody());
        }
    }

    @Test
    public void testClientErrorWithBodyCustomMapping() {
        final String url = format("http://localhost:%d/%s", port, BAD_REQUEST_ERROR_BODY);
        try {
            final Optional<Void> response = restClient
                    .get(url, Void.class)
                    .addExceptionMapping(HttpStatus.SC_BAD_REQUEST, (ex) -> new CustomException(ex.getBody().toString()))
                    .execute();
        } catch (CustomException exception) {
            assertEquals(toJson(new SimpleResponse("Bad Request")), exception.getMessage());
        }
    }

    @Test
    public void testServerErrorSuppress() {
        final String url = format("http://localhost:%d/%s", port, BAD_GATEWAY_ERROR);
        final Optional<Void> response =
                restClient.get(url, Void.class)
                          .processServerExceptions(false)
                          .execute();

        assertFalse(response.isPresent());
    }

    @Test
    public void testServerError() {
        final String url = format("http://localhost:%d/%s", port, BAD_GATEWAY_ERROR);
        try {
            final Optional<Void> response =
                    restClient.get(url, Void.class)
                              .execute();
        } catch (HttpRestServerException exception) {
            assertEquals(HttpStatus.SC_BAD_GATEWAY, exception.getResponseStatus());
            assertNull(exception.getBody());
        }
    }

    @Test
    public void testServerErrorWithBody() {
        final String url = format("http://localhost:%d/%s", port, BAD_GATEWAY_ERROR_BODY);
        try {
            final Optional<Void> response =
                    restClient.get(url, Void.class)
                              .execute();
        } catch (HttpRestServerException exception) {
            assertEquals(HttpStatus.SC_BAD_GATEWAY, exception.getResponseStatus());
            assertNotNull(exception.getBody());
            assertEquals(toJson(new SimpleResponse("Bad Gateway")), exception.getBody());
        }
    }

    @Test
    public void testServerErrorWithBodyCustomMapping() {
        final String url = format("http://localhost:%d/%s", port, BAD_GATEWAY_ERROR_BODY);
        try {
            final Optional<Void> response = restClient
                    .get(url, Void.class)
                    .addExceptionMapping(HttpStatus.SC_BAD_GATEWAY, (ex) -> new CustomException(ex.getBody().toString()))
                    .execute();
        } catch (CustomException exception) {
            assertEquals(toJson(new SimpleResponse("Bad Gateway")), exception.getMessage());
        }
    }

//    @Test
//    public void testInternalErrorMapping() {
//        String url = "/error";
//        server.expect(requestTo(url))
//              .andExpect(method(HttpMethod.GET))
//              .andRespond(withServerError());
//
//        try {
//            restClient.get(url, Boolean.class)
//                      .addExceptionMapping(HttpStatus.INTERNAL_SERVER_ERROR.value(),
//                                           RuntimeException::new)
//                      .make();
//        } catch (RuntimeException exception) {
//            assertEquals(HttpServerErrorException.class, exception.getCause().getClass());
//        }
//        server.verify();
//    }
//
//    @Test
//    public void testBadRequestThrowing() {
//        String url = "/error";
//        server.expect(requestTo(url))
//              .andExpect(method(HttpMethod.GET))
//              .andRespond(withBadRequest());
//
//        try {
//            restClient.get(url, Boolean.class).make();
//        } catch (RestClientException exception) {
//            assertEquals(HttpClientErrorException.class, exception.getClass());
//        }
//        server.verify();
//    }
//
//    @Test
//    public void testInternalErrorSuppress() {
//        String url = "/error";
//        server.expect(requestTo(url))
//              .andExpect(method(HttpMethod.GET))
//              .andRespond(withServerError());
//
//        final String message = "test";
//        Optional<PingResponse> response =
//                restClient.get(url, PingResponse.class)
//                          .processServerExceptions(false)
//                          .processServerExceptions(false)
//                          .defaultResponse(Optional.of(new PingResponse(message)))
//                          .make();
//
//        server.verify();
//
//        assertTrue(response.isPresent());
//        assertEquals(message, response.get().getMessage());
//    }

}