package ru.romanow.core.rest.client.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

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
        return new PostRequestBuilder<>(url, requestClass);
    }

    public <REQ> GetRequestBuilder<REQ> get(String url, Class<REQ> requestClass) {
        return new GetRequestBuilder<>(url, requestClass);
    }

    public abstract class RequestBuilder<REQ> {
        protected String url;
        protected Class<REQ> responseType;
        protected boolean processClientExceptions;
        protected boolean processServerExceptions;
        protected boolean processResourceExceptions;
        protected Optional<REQ> defaultResponse;
        protected Function<ResourceAccessException, ? extends RuntimeException> resourceExceptionMapper;
        protected Map<Integer, Function<HttpStatusCodeException, ? extends RuntimeException>> exceptionMapping;

        public RequestBuilder(String url, Class<REQ> responseType) {
            this.url = url;
            this.responseType = responseType;
            this.processClientExceptions = true;
            this.processServerExceptions = true;
            this.processResourceExceptions = true;
            this.defaultResponse = Optional.<REQ>empty();
            this.exceptionMapping = new HashMap<>();
        }

        public RequestBuilder<REQ> addExceptionMapping(
                Integer statusCode,
                Function<HttpStatusCodeException, ? extends RuntimeException> mapper) {
            this.exceptionMapping.put(statusCode, mapper);
            return this;
        }

        public RequestBuilder<REQ> processClientExceptions(boolean processClientExceptions) {
            this.processClientExceptions = processClientExceptions;
            return this;
        }

        public RequestBuilder<REQ> processServerExceptions(boolean processServerExceptions) {
            this.processServerExceptions = processServerExceptions;
            return this;
        }

        public RequestBuilder<REQ> setProcessResourceExceptions(boolean processResourceExceptions) {
            this.processResourceExceptions = processResourceExceptions;
            return this;
        }

        public RequestBuilder<REQ> defaultResponse(Optional<REQ> defaultResponse) {
            this.defaultResponse = defaultResponse;
            return this;
        }

        public RequestBuilder<REQ> resourceExceptionMapper(
                Function<ResourceAccessException, ? extends RuntimeException> resourceExceptionMapper) {
            this.resourceExceptionMapper = resourceExceptionMapper;
            return this;
        }

        public Optional<REQ> make() {
            try {
                ResponseEntity<REQ> response = makeRequest();
                if (response.getStatusCode().is2xxSuccessful()) {
                    return Optional.ofNullable(response.getBody());
                }
            } catch (HttpStatusCodeException exception) {
                HttpStatus statusCode = exception.getStatusCode();
                logger.warn("Client return with code {}", statusCode);
                if (exceptionMapping.containsKey(statusCode.value())) {
                    Function<HttpStatusCodeException, ? extends RuntimeException> mapper =
                            exceptionMapping.get(statusCode.value());
                    throw mapper.apply(exception);
                }

                if (statusCode.is4xxClientError() && processClientExceptions ||
                        statusCode.is5xxServerError() && processServerExceptions) {
                    throw exception;
                }
            } catch (ResourceAccessException exception) {
                logger.warn("I/O exception while processing the request: {}", exception.getMessage());
                if (resourceExceptionMapper != null) {
                    throw resourceExceptionMapper.apply(exception);
                }

                if (processResourceExceptions) {
                    throw exception;
                }
            }

            return defaultResponse;
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
