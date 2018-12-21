package ru.romanow.core.spring.rest.client;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.romanow.core.spring.rest.client.exception.HttpRestClientException;
import ru.romanow.core.spring.rest.client.exception.HttpRestResourceException;
import ru.romanow.core.spring.rest.client.exception.HttpRestServerException;
import ru.romanow.core.spring.rest.client.exception.HttpRestTimeoutException;
import ru.romanow.core.spring.rest.client.exceptions.CustomException;
import ru.romanow.core.spring.rest.client.model.AuthRequest;
import ru.romanow.core.spring.rest.client.model.AuthResponse;
import ru.romanow.core.spring.rest.client.model.PingResponse;
import ru.romanow.core.spring.rest.client.model.SimpleResponse;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.romanow.core.spring.rest.client.Constants.*;
import static ru.romanow.core.spring.rest.client.utils.JsonSerializer.fromJson;
import static ru.romanow.core.spring.rest.client.utils.JsonSerializer.toJson;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SpringRestClientServerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplate restTemplate;

    private SpringRestClient restClient;

    @Before
    public void init() {
        restClient = new SpringRestClient(restTemplate);
    }

    @Test
    public void testServerErrorRetry() {
        final String url = format("http://localhost:%d/%s", port, BAD_GATEWAY_ERROR_RETRY);
        final Optional<SimpleResponse> response =
                restClient.get(url, SimpleResponse.class)
                        .retryServerError(true)
                        .retryCount(3)
                        .execute();

        Assert.assertTrue(response.isPresent());
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

        if (duration > 3 * 1000) {
            fail("Timeout not working");
        }
        Assert.assertTrue(response.isPresent());
        assertEquals("OK", response.get().getMessage());
    }
}
