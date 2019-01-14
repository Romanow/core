package ru.romanow.core.rest.client;

import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.romanow.core.rest.client.exception.HttpRestClientException;
import ru.romanow.core.rest.client.exception.HttpRestResourceException;
import ru.romanow.core.rest.client.exception.HttpRestServerException;
import ru.romanow.core.rest.client.exception.HttpRestTimeoutException;
import ru.romanow.core.rest.client.exceptions.CustomException;
import ru.romanow.core.rest.client.model.AuthRequest;
import ru.romanow.core.rest.client.model.AuthResponse;
import ru.romanow.core.rest.client.model.PingResponse;
import ru.romanow.core.rest.client.model.SimpleResponse;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.junit.Assert.*;
import static ru.romanow.core.rest.client.utils.JsonSerializer.toJson;
import static ru.romanow.core.rest.client.web.AuthController.*;

// TODO stress tests
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
            final Optional<SimpleResponse> response =
                    restClient.get(url, SimpleResponse.class)
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
            final Optional<SimpleResponse> response = restClient
                    .get(url, SimpleResponse.class)
                    .addExceptionMapping(HttpStatus.SC_BAD_GATEWAY, (ex) -> new CustomException(ex.getBody().toString()))
                    .execute();
        } catch (CustomException exception) {
            assertEquals(toJson(new SimpleResponse("Bad Gateway")), exception.getMessage());
        }
    }

    @Test
    public void testServerErrorRetry() {
        final String url = format("http://localhost:%d/%s", port, BAD_GATEWAY_ERROR_RETRY);
        final Optional<SimpleResponse> response =
                restClient.get(url, SimpleResponse.class)
                          .retryServerError(true)
                          .retryCount(3)
                          .execute();

        assertTrue(response.isPresent());
        assertEquals("Bad Gateway", response.get().getMessage());
    }

    @Test(expected = HttpRestResourceException.class)
    public void testConnectionError() {
        final String url = "http://localhost:5000/test";
        final Optional<Void> response = restClient
                .get(url, Void.class)
                .execute();
    }

    @Test(expected = CustomException.class)
    public void testConnectionErrorMapping() {
        final String url = "http://localhost:5000/test";
        final Optional<Void> response = restClient
                .get(url, Void.class)
                .resourceExceptionMapper((ex) -> new CustomException(ex.getMessage()))
                .execute();
    }

    @Test
    public void testTimeoutSuppress() {
        final String url = format("http://localhost:%d/%s", port, TIMEOUT);
        final long start = System.currentTimeMillis();
        final Optional<Void> response = restClient
                .get(url, Void.class)
                .requestProcessingTimeout(1, TimeUnit.SECONDS)
                .processTimeoutExceptions(false)
                .execute();

        final long duration = System.currentTimeMillis() - start;

        if (duration > 2 * 1000) {
            fail("Timeout not working");
        }
    }

    @Test(expected = HttpRestTimeoutException.class)
    public void testTimeout() {
        final String url = format("http://localhost:%d/%s", port, TIMEOUT);
        final Optional<Void> response = restClient
                .get(url, Void.class)
                .requestProcessingTimeout(1, TimeUnit.SECONDS)
                .execute();
    }

    @Test(expected = CustomException.class)
    public void testTimeoutMapping() {
        final String url = format("http://localhost:%d/%s", port, TIMEOUT);
        final Optional<Void> response = restClient
                .get(url, Void.class)
                .requestProcessingTimeout(1, TimeUnit.SECONDS)
                .timeoutExceptionMapping((ex) -> new CustomException(ex.getMessage()))
                .execute();
    }

    @Test
    public void testTimeoutRetry() {
        final String url = format("http://localhost:%d/%s", port, TIMEOUT_RETRY);
        final long start = System.currentTimeMillis();
        final Optional<SimpleResponse> response = restClient
                .get(url, SimpleResponse.class)
                .requestProcessingTimeout(1, TimeUnit.SECONDS)
                .retryCount(3)
                .execute();

        final long duration = System.currentTimeMillis() - start;

        if (duration > 5 * 1000) {
            fail("Timeout not working");
        }
        assertTrue(response.isPresent());
        assertEquals("OK", response.get().getMessage());
    }
}