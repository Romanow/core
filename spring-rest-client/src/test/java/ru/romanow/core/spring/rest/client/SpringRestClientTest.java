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
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;
import ru.romanow.core.spring.rest.client.exception.HttpRestClientException;
import ru.romanow.core.spring.rest.client.exception.HttpRestResourceException;
import ru.romanow.core.spring.rest.client.exception.HttpRestServerException;
import ru.romanow.core.spring.rest.client.exceptions.CustomException;
import ru.romanow.core.spring.rest.client.model.AuthRequest;
import ru.romanow.core.spring.rest.client.model.AuthResponse;
import ru.romanow.core.spring.rest.client.model.PingResponse;
import ru.romanow.core.spring.rest.client.model.SimpleResponse;

import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.util.StringUtils.hasLength;
import static ru.romanow.core.spring.rest.client.Constants.*;
import static ru.romanow.core.spring.rest.client.utils.JsonSerializer.fromJson;
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
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
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
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(toJson(new PingResponse(headerParam))));

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
        server.expect(requestTo(format("%s?%s=%s", QUERY_PARAM, queryName, queryParam)))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(toJson(new SimpleResponse(queryParam))));

        final Optional<SimpleResponse> response =
                restClient.get(QUERY_PARAM, SimpleResponse.class)
                        .addParam(queryName, queryParam)
                        .execute();

        Assert.assertTrue(response.isPresent());
        assertEquals(queryParam, response.get().getMessage());
    }

    @Test
    public void testPostSuccess() {
        final UUID uin = UUID.randomUUID();
        final AuthResponse authResponse = new AuthResponse(uin, 1000L, true);
        final AuthRequest auth = new AuthRequest("ronin", "test");
        server.expect(requestTo(AUTH))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(toJson(auth)))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .body(toJson(authResponse)));

        final Optional<AuthResponse> response =
                restClient.post(AUTH, auth, AuthResponse.class).execute();

        Assert.assertTrue(response.isPresent());
        Assert.assertEquals(uin, response.get().getUin());
    }

    @Test
    public void testClientErrorSuppress() {
        server.expect(requestTo(BAD_REQUEST_ERROR))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        final Optional<Void> response =
                restClient.get(BAD_REQUEST_ERROR, Void.class)
                        .processClientExceptions(false)
                        .execute();

        assertFalse(response.isPresent());
    }

    @Test
    public void testClientError() {
        server.expect(requestTo(BAD_REQUEST_ERROR))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        try {
            restClient.get(BAD_REQUEST_ERROR, Void.class).execute();
        } catch (HttpRestClientException exception) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getResponseStatus());
            assertNull(exception.getBody());
        }
    }

    @Test
    public void testClientErrorWithBody() {
        final SimpleResponse simpleResponse = new SimpleResponse("Bad Request");
        server.expect(requestTo(BAD_REQUEST_ERROR_BODY))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body(toJson(simpleResponse)));

        try {
            restClient.get(BAD_REQUEST_ERROR_BODY, Void.class).execute();
        } catch (HttpRestClientException exception) {
            assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getResponseStatus());
            assertNotNull(exception.getBody());
            assertEquals(simpleResponse, fromJson(exception.getBody().toString(), SimpleResponse.class));
        }
    }

    @Test
    public void testClientErrorWithBodyCustomMapping() {
        final SimpleResponse simpleResponse = new SimpleResponse("Bad Request");
        server.expect(requestTo(BAD_REQUEST_ERROR_BODY))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .body(toJson(simpleResponse)));

        try {
            restClient
                    .get(BAD_REQUEST_ERROR_BODY, Void.class)
                    .addExceptionMapping(HttpStatus.BAD_REQUEST.value(), (ex) -> new CustomException(ex.getBody().toString()))
                    .execute();
        } catch (CustomException exception) {
            assertEquals(toJson(new SimpleResponse("Bad Request")), exception.getMessage());
        }
    }

    @Test
    public void testServerErrorSuppress() {
        server.expect(requestTo(BAD_GATEWAY_ERROR))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        final Optional<Void> response =
                restClient.get(BAD_GATEWAY_ERROR, Void.class)
                        .processServerExceptions(false)
                        .execute();

        assertFalse(response.isPresent());
    }

    @Test
    public void testServerError() {
        server.expect(requestTo(BAD_GATEWAY_ERROR))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        try {
            restClient.get(BAD_GATEWAY_ERROR, Void.class).execute();
        } catch (HttpRestServerException exception) {
            assertEquals(HttpStatus.BAD_GATEWAY.value(), exception.getResponseStatus());
            assertNull(exception.getBody());
        }
    }

    @Test
    public void testServerErrorWithBody() {
        final SimpleResponse simpleResponse = new SimpleResponse("Bad Gateway");
        server.expect(requestTo(BAD_GATEWAY_ERROR))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .body(toJson(simpleResponse)));

        try {
            restClient.get(BAD_GATEWAY_ERROR, Void.class).execute();
        } catch (HttpRestServerException exception) {
            assertEquals(HttpStatus.BAD_GATEWAY.value(), exception.getResponseStatus());
            assertNotNull(exception.getBody());
            assertEquals(simpleResponse, fromJson(exception.getBody().toString(), SimpleResponse.class));
        }
    }

    @Test
    public void testServerErrorWithBodyCustomMapping() {
        final SimpleResponse simpleResponse = new SimpleResponse("Bad Gateway");
        server.expect(requestTo(BAD_GATEWAY_ERROR_BODY))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .body(toJson(simpleResponse)));

        try {
            restClient
                    .get(BAD_GATEWAY_ERROR_BODY, Void.class)
                    .addExceptionMapping(HttpStatus.BAD_GATEWAY.value(), (ex) -> new CustomException(ex.getBody().toString()))
                    .execute();
        } catch (CustomException exception) {
            assertEquals(toJson(new SimpleResponse("Bad Gateway")), exception.getMessage());
        }
    }
}
