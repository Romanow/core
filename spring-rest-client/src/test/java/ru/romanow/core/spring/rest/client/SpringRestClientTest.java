package ru.romanow.core.spring.rest.client;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.romanow.core.spring.rest.client.model.AuthRequest;
import ru.romanow.core.spring.rest.client.model.AuthResponse;
import ru.romanow.core.spring.rest.client.model.PingResponse;
import ru.romanow.core.spring.rest.client.model.SimpleResponse;

import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.romanow.core.spring.rest.client.Constants.*;
import static ru.romanow.core.spring.rest.client.utils.JsonSerializer.toJson;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestServerConfiguration.class)
public class SpringRestClientTest {

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer server;
    private SpringRestClient restClient;

    @Before
    public void init() {
        server = MockRestServiceServer.createServer(restTemplate);
        restClient = new SpringRestClient(restTemplate);
    }

    @Test
    public void testGetSuccess() {
        final String resp = "OK";
        server.expect(requestTo(PING))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(toJson(new PingResponse(resp))));

        Optional<PingResponse> result = restClient.get(PING, PingResponse.class).execute();

        server.verify();

        assertTrue(result.isPresent());
        assertEquals(resp, result.get().getMessage());
    }

    @Test
    public void testGetWithHeaderSuccess() {
        final String headerName = "X-CUSTOM-HEADER";
        final String headerParam = "test";
        server.expect(requestTo(CUSTOM_HEADER))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(headerName, headerParam))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(toJson(new PingResponse("OK"))));

        final Optional<SimpleResponse> response =
                restClient.get(CUSTOM_HEADER, SimpleResponse.class)
                        .addHeader(headerName, headerParam)
                        .execute();

        Assert.assertTrue(response.isPresent());
        assertEquals(headerParam, response.get().getMessage());
    }

    @Test
    public void testGetWithParamSuccess() {
        final String queryParam = "test";
        final String queryName = "query";
        server.expect(requestTo(QUERY_PARAM))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam(queryName, queryParam))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(toJson(new PingResponse(queryParam))));

        final Optional<SimpleResponse> response =
                restClient.get(QUERY_PARAM, SimpleResponse.class)
                        .addParam(queryName, queryParam)
                        .execute();

        Assert.assertTrue(response.isPresent());
        assertEquals(queryParam, response.get().getMessage());
    }

    @Test
    public void testPostSuccess() {
        final AuthRequest auth = new AuthRequest("ronin", "test");
        server.expect(requestTo(QUERY_PARAM))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(toJson(auth)))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON));

        final Optional<AuthResponse> response =
                restClient.post(AUTH, auth, AuthResponse.class).execute();

        Assert.assertTrue(response.isPresent());
    }

//    @Test
//    public void testClientErrorSuppress() {
//        final String url = format("http://localhost:%d/%s", port, BAD_REQUEST_ERROR);
//        final Optional<Void> response =
//                restClient.get(url, Void.class)
//                        .processClientExceptions(false)
//                        .execute();
//
//        assertFalse(response.isPresent());
//    }
//
//    @Test
//    public void testClientError() {
//        final String url = format("http://localhost:%d/%s", port, BAD_REQUEST_ERROR);
//        try {
//            final Optional<Void> response =
//                    restClient.get(url, Void.class)
//                            .execute();
//        } catch (HttpRestClientException exception) {
//            assertEquals(HttpStatus.SC_BAD_REQUEST, exception.getResponseStatus());
//            assertNull(exception.getBody());
//        }
//    }
//
//    @Test
//    public void testClientErrorWithBody() {
//        final String url = format("http://localhost:%d/%s", port, BAD_REQUEST_ERROR_BODY);
//        try {
//            final Optional<Void> response =
//                    restClient.get(url, Void.class)
//                            .execute();
//        } catch (HttpRestClientException exception) {
//            assertEquals(HttpStatus.SC_BAD_REQUEST, exception.getResponseStatus());
//            assertNotNull(exception.getBody());
//            assertEquals(toJson(new SimpleResponse("Bad Request")), exception.getBody());
//        }
//    }
//
//    @Test
//    public void testClientErrorWithBodyCustomMapping() {
//        final String url = format("http://localhost:%d/%s", port, BAD_REQUEST_ERROR_BODY);
//        try {
//            final Optional<Void> response = restClient
//                    .get(url, Void.class)
//                    .addExceptionMapping(HttpStatus.SC_BAD_REQUEST, (ex) -> new CustomException(ex.getBody().toString()))
//                    .execute();
//        } catch (CustomException exception) {
//            assertEquals(toJson(new SimpleResponse("Bad Request")), exception.getMessage());
//        }
//    }
//
//    @Test
//    public void testServerErrorSuppress() {
//        final String url = format("http://localhost:%d/%s", port, BAD_GATEWAY_ERROR);
//        final Optional<Void> response =
//                restClient.get(url, Void.class)
//                        .processServerExceptions(false)
//                        .execute();
//
//        assertFalse(response.isPresent());
//    }
//
//    @Test
//    public void testServerError() {
//        final String url = format("http://localhost:%d/%s", port, BAD_GATEWAY_ERROR);
//        try {
//            final Optional<Void> response =
//                    restClient.get(url, Void.class)
//                            .execute();
//        } catch (HttpRestServerException exception) {
//            assertEquals(HttpStatus.SC_BAD_GATEWAY, exception.getResponseStatus());
//            assertNull(exception.getBody());
//        }
//    }
//
//    @Test
//    public void testServerErrorWithBody() {
//        final String url = format("http://localhost:%d/%s", port, BAD_GATEWAY_ERROR_BODY);
//        try {
//            final Optional<SimpleResponse> response =
//                    restClient.get(url, SimpleResponse.class)
//                            .execute();
//        } catch (HttpRestServerException exception) {
//            assertEquals(HttpStatus.SC_BAD_GATEWAY, exception.getResponseStatus());
//            assertNotNull(exception.getBody());
//            assertEquals(toJson(new SimpleResponse("Bad Gateway")), exception.getBody());
//        }
//    }
//
//    @Test
//    public void testServerErrorWithBodyCustomMapping() {
//        final String url = format("http://localhost:%d/%s", port, BAD_GATEWAY_ERROR_BODY);
//        try {
//            final Optional<SimpleResponse> response = restClient
//                    .get(url, SimpleResponse.class)
//                    .addExceptionMapping(HttpStatus.SC_BAD_GATEWAY, (ex) -> new CustomException(ex.getBody().toString()))
//                    .execute();
//        } catch (CustomException exception) {
//            assertEquals(toJson(new SimpleResponse("Bad Gateway")), exception.getMessage());
//        }
//    }
//
//    @Test
//    public void testServerErrorRetry() {
//        final String url = format("http://localhost:%d/%s", port, BAD_GATEWAY_ERROR_RETRY);
//        final Optional<SimpleResponse> response =
//                restClient.get(url, SimpleResponse.class)
//                        .retryServerError(true)
//                        .retryCount(3)
//                        .execute();
//
//        Assert.assertTrue(response.isPresent());
//        assertEquals("Bad Gateway", response.get().getMessage());
//    }
//
//    @Test(expected = HttpRestResourceException.class)
//    public void testConnectionError() {
//        final String url = "http://localhost:5000/test";
//        final Optional<Void> response = restClient
//                .get(url, Void.class)
//                .execute();
//    }
//
//    @Test(expected = CustomException.class)
//    public void testConnectionErrorMapping() {
//        final String url = "http://localhost:5000/test";
//        final Optional<Void> response = restClient
//                .get(url, Void.class)
//                .resourceExceptionMapper((ex) -> new CustomException(ex.getMessage()))
//                .execute();
//    }
//
//    @Test
//    public void testTimeoutSuppress() {
//        final String url = format("http://localhost:%d/%s", port, TIMEOUT);
//        final long start = System.currentTimeMillis();
//        final Optional<Void> response = restClient
//                .get(url, Void.class)
//                .requestProcessingTimeout(1, TimeUnit.SECONDS)
//                .processTimeoutExceptions(false)
//                .execute();
//
//        final long duration = System.currentTimeMillis() - start;
//
//        if (duration > 2 * 1000) {
//            fail("Timeout not working");
//        }
//    }
//
//    @Test(expected = HttpRestTimeoutException.class)
//    public void testTimeout() {
//        final String url = format("http://localhost:%d/%s", port, TIMEOUT);
//        final Optional<Void> response = restClient
//                .get(url, Void.class)
//                .requestProcessingTimeout(1, TimeUnit.SECONDS)
//                .execute();
//    }
//
//    @Test(expected = CustomException.class)
//    public void testTimeoutMapping() {
//        final String url = format("http://localhost:%d/%s", port, TIMEOUT);
//        final Optional<Void> response = restClient
//                .get(url, Void.class)
//                .requestProcessingTimeout(1, TimeUnit.SECONDS)
//                .timeoutExceptionMapping((ex) -> new CustomException(ex.getMessage()))
//                .execute();
//    }
//
//    @Test
//    public void testTimeoutRetry() {
//        final String url = format("http://localhost:%d/%s", port, TIMEOUT_RETRY);
//        final long start = System.currentTimeMillis();
//        final Optional<SimpleResponse> response = restClient
//                .get(url, SimpleResponse.class)
//                .requestProcessingTimeout(1, TimeUnit.SECONDS)
//                .retryCount(3)
//                .execute();
//
//        final long duration = System.currentTimeMillis() - start;
//
//        if (duration > 3 * 1000) {
//            fail("Timeout not working");
//        }
//        Assert.assertTrue(response.isPresent());
//        assertEquals("OK", response.get().getMessage());
//    }
//
//    @Test
//    public void testInternalErrorMapping() {
//        String url = "/error";
//        server.expect(requestTo(url))
//                .andExpect(method(HttpMethod.GET))
//                .andRespond(withServerError());
//
//        try {
//            restClient.get(url, Boolean.class)
//                    .addExceptionMapping(HttpStatus.INTERNAL_SERVER_ERROR.value(), CustomException::new)
//                    .execute();
//        } catch (CustomException exception) {
//            assertEquals(HttpServerErrorException.class, exception.getCause().getClass());
//        }
//        server.verify();
//    }
//
//    @Test
//    public void testBadRequestThrowing() {
//        String url = "/error";
//        server.expect(requestTo(url))
//                .andExpect(method(HttpMethod.GET))
//                .andRespond(withBadRequest());
//
//        try {
//            restClient.get(url, Boolean.class).execute();
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
//                .andExpect(method(HttpMethod.GET))
//                .andRespond(withServerError());
//
//        final String message = "test";
//        Optional<SimpleResponse> response =
//                restClient.get(url, SimpleResponse.class)
//                        .processServerExceptions(false)
//                        .processServerExceptions(false)
//                        .defaultResponse(() -> of(new SimpleResponse(message)))
//                        .execute();
//
//        server.verify();
//
//        assertTrue(response.isPresent());
//        assertEquals(message, response.get().getMessage());
//    }
//
//    @Test
//    public void testPostSuccess() {
//        String url = "/auth";
//        String login = "ronin";
//        String password = "qwerty";
//
//        AuthRequest request =
//                new AuthRequest(login, password);
//
//        final UUID uin = UUID.randomUUID();
//        final long expiredIn = 123L;
//        AuthResponse response =
//                new AuthResponse(uin, expiredIn, true);
//
//        server.expect(requestTo(url))
//                .andExpect(content().string(gson.toJson(request)))
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
//                .andExpect(method(HttpMethod.POST))
//                .andRespond(withSuccess(gson.toJson(response), MediaType.APPLICATION_JSON_UTF8));
//
//        Optional<AuthResponse> result =
//                restClient.post(url, request, AuthResponse.class)
//                        .execute();
//
//        server.verify();
//
//        assertTrue(result.isPresent());
//        AuthResponse authResponse = result.get();
//        assertEquals(uin, authResponse.getUin());
//        assertEquals(expiredIn, authResponse.getExpiredIn());
//    }
}
