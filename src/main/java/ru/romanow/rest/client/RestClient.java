package ru.romanow.rest.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import ru.romanow.rest.client.exceptions.RestResponseException;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Created by ronin on 12.02.16
 */
public class RestClient {
    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

    private final RestTemplate restTemplate;

    public RestClient(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public <REQ> PostRequestBuilder<REQ> post(String url, Class<REQ> requestClass) {
        return new PostRequestBuilder<REQ>(url, requestClass);
    }

    public <REQ> GetRequestBuilder<REQ> get(String url, Class<REQ> requestClass) {
        return new GetRequestBuilder<REQ>(url, requestClass);
    }

    public abstract class RequestBuilder<REQ> {
        protected String url;
        protected Class<REQ> responseType;
        protected Map<Integer, Function<HttpStatusCodeException, ? extends RuntimeException>> exceptionMapping;

        public RequestBuilder(String url, Class<REQ> responseType) {
            this.url = url;
            this.responseType = responseType;
            this.exceptionMapping = new HashMap<>();
        }

        public RequestBuilder<REQ> addExceptionMapping(
                Integer statusCode,
                Function<HttpStatusCodeException, ? extends RuntimeException> mapper) {
            this.exceptionMapping.put(statusCode, mapper);
            return this;
        }

        public Optional<REQ> make() {
            try {
                ResponseEntity<REQ> response = makeRequest();
                if (response.getStatusCode().is2xxSuccessful()) {
                    return Optional.ofNullable(response.getBody());
                }
            } catch (HttpStatusCodeException exception) {
                Integer statusCode = exception.getStatusCode().value();
                logger.info("Client return with code {}", statusCode);
                if (exceptionMapping.containsKey(statusCode)) {
                    Function<HttpStatusCodeException, ? extends RuntimeException> mapper =
                            exceptionMapping.get(statusCode);
                    throw mapper.apply(exception);
                }

                throw exception;
            } catch (ResourceAccessException exception) {
                logger.error("I/O exception while processing the request", exception);
                throw exception;
            }

            return Optional.empty();
        }

        private <E extends RestResponseException> E buildRestException(
                HttpStatusCodeException exception, Class<E> exceptionClass) {
            try {
                Constructor<E> constructor = exceptionClass
                        .getConstructor(Integer.class, String.class, String.class);
                return constructor.newInstance(exception.getStatusCode().value(),
                                               exception.getStatusText(),
                                               exception.getResponseBodyAsString());
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        protected abstract ResponseEntity<REQ> makeRequest();
    }

    public class PostRequestBuilder<REQ>
            extends RequestBuilder<REQ> {

        private Object requestBody;

        public PostRequestBuilder(String url, Class<REQ> requestClass) {
            super(url, requestClass);
        }

        public PostRequestBuilder<REQ> requestBody(Object requestBody) {
            this.requestBody = requestBody;
            return this;
        }

        @Override
        protected ResponseEntity<REQ> makeRequest() {
            return restTemplate.postForEntity(url, requestBody, responseType);
        }
    }

    public class GetRequestBuilder<REQ>
            extends RequestBuilder<REQ> {
        private Map<String, Object> params;

        public GetRequestBuilder(String url, Class<REQ> requestClass) {
            super(url, requestClass);

            params = new HashMap<>();
        }

        public GetRequestBuilder<REQ> addParam(String name, String value) {
            this.params.put(name, value);
            return this;
        }

        public GetRequestBuilder<REQ> params(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        @Override
        protected ResponseEntity<REQ> makeRequest() {
            return restTemplate.getForEntity(url, responseType, params);
        }
    }
}
