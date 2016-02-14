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
import org.springframework.web.client.RestTemplate;
import ru.romanow.rest.client.configuration.TestServerConfiguration;
import ru.romanow.rest.client.model.TestAuthRequest;
import ru.romanow.rest.client.model.TestAuthResponse;

import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
    public void testPostSuccess() {
        String url = "/auth";
        String login = "ronin";
        String password = "qwerty";

        TestAuthRequest request =
                new TestAuthRequest(login, password);

        String uin = "123";
        Long expiredIn = 123L;
        TestAuthResponse response =
                new TestAuthResponse(uin, expiredIn, true);

        server.expect(requestTo(url))
              .andExpect(content().string(JsonFactory.toJson(request)))
              .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
              .andExpect(method(HttpMethod.POST))
              .andRespond(withSuccess(JsonFactory.toJson(response), MediaType.APPLICATION_JSON_UTF8));

        Optional<TestAuthResponse> result =
                restClient.post(url, TestAuthResponse.class)
                          .requestBody(request)
                          .make();

        server.verify();

        assertTrue(result.isPresent());
        TestAuthResponse authResponse = result.get();
        assertTrue(authResponse.getActive());
        assertEquals(uin, authResponse.getUin());
        assertEquals(expiredIn, authResponse.getExpiredIn());
    }
}