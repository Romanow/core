package ru.romanow.rest.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.romanow.rest.client.configuration.TestServerConfiguration;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

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
    public void testGet() {
        server.expect(requestTo("/ping"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK));

        restClient.get("/ping", Boolean.class).make();

        server.verify();
    }

//    public void testGet() {
//        String login = "ronin";
//        String password = "qwerty";
//
//        TestAuthRequest request = new TestAuthRequest(login, password);
//        TestAuthResponse response = new TestAuthResponse("123", 123L, true);
//
//        server.expect(requestTo("/auth"))
//              .andExpect(content().string(JsonFactory.toJson(request)))
//              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//              .andExpect(method(HttpMethod.POST))
//              .andRespond(withStatus(HttpStatus.OK)
//                                  .contentType(MediaType.APPLICATION_JSON)
//                                  .body(JsonFactory.toJson(response)));
//
//        Optional<TestAuthResponse> result =
//                restClient.post("/auth", TestAuthResponse.class)
//                          .requestBody(request)
//                          .make();
//        assertEquals(true, result.get().getActive());
//
//        server.verify();
//    }
}