package ru.romanow.core.rest.client;

import org.openjdk.jmh.annotations.*;
import org.springframework.web.client.RestTemplate;
import ru.romanow.core.spring.rest.client.SpringRestClient;

@BenchmarkMode(Mode.All)
@Warmup(iterations = 1, time = 2)
@Measurement(iterations = 10, time = 2)
@Fork(value = 1, warmups = 1)
public class RestClientStressTest {
    private static final String TEST_URL = "http://example.com";

    @State(Scope.Benchmark)
    public static class RestClientState {
        final RestClient restClient = new RestClient();
    }

    @Benchmark
    public void testApacheRestClient(RestClientState state) {
        state.restClient.get(TEST_URL, Void.class).execute();
    }

    @State(Scope.Benchmark)
    public static class SpringRestClientState {
        final SpringRestClient restClient = new SpringRestClient(buildRestTemplate());

        private RestTemplate buildRestTemplate() {
            return new RestTemplate();
        }
    }

    @Benchmark
    public void testSpringRestClient(SpringRestClientState state) {
        state.restClient.get(TEST_URL, Void.class).execute();
    }
}
