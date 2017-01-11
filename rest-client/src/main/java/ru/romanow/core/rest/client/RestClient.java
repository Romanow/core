package ru.romanow.core.rest.client;

import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import ru.romanow.core.rest.client.exception.ExceptionMapper;
import ru.romanow.core.rest.client.exception.HttpRestResourceException;
import ru.romanow.core.rest.client.exception.HttpStatusBasedException;
import ru.romanow.core.rest.client.exception.TimeoutExceptionMapper;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by ronin on 12.02.16
 */
public class RestClient {
    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

    private static final int DEFAULT_REQUEST_TIMEOUT = 3000;
    
    private final AsyncRestTemplate restTemplate;

    public RestClient() {
        this.restTemplate = new AsyncRestTemplate();
    }

    // region Builders
    public <RESP> GetRequestBuilder<RESP> get(@Nonnull String url, @Nonnull Class<RESP> responseClass) {
        return new GetRequestBuilder<>(url, responseClass);
    }

    public <RESP> PostRequestBuilder<RESP> post(@Nonnull String url, @Nonnull TypeToken<RESP> responseType) {
        return new PostRequestBuilder<>(url, responseType);
    }

    public <RESP> PostRequestBuilder<RESP> post(String url, Class<RESP> requestClass) {
        return new PostRequestBuilder<>(url, requestClass);
    }

    public <RESP> GetRequestBuilder<RESP> get(String url, TypeToken<RESP> responseType) {
        return new GetRequestBuilder<>(url, responseType);
    }
    // endregion

    public abstract class RequestBuilder<RESP, T extends RequestBuilder<RESP, T>> {
        protected String url;

        protected Class<RESP> responseClass;
        protected TypeToken<RESP> responseType;

        protected Supplier<Optional<RESP>> defaultResponse;
        protected Map<Integer, Class> errorResponseClass;

        protected boolean processClientExceptions;
        protected boolean processServerExceptions;
        protected Set<Integer> ignoredErrors;
        protected Map<Integer, ExceptionMapper<? extends RuntimeException, HttpStatusBasedException>> exceptionMapping;

        protected boolean processResourceExceptions;
        protected ExceptionMapper<? extends RuntimeException, HttpRestResourceException> resourceExceptionMapper;

        protected int requestProcessingTimeout;
        protected TimeUnit timeoutTimeUnit;
        protected int retryCount;
        protected boolean processTimeoutExceptions;
        protected TimeoutExceptionMapper<? extends RuntimeException> timeoutExceptionMapping;

        protected Map<String, Object> params;

        public RequestBuilder(@Nonnull String url, @Nonnull Class<RESP> responseClass) {
            Objects.requireNonNull(url);
            Objects.requireNonNull(responseClass);

            this.url = url;
            this.responseClass = responseClass;
            this.defaultResponse = Optional::empty;
            this.errorResponseClass = new HashMap<>();
            
            this.processClientExceptions = true;
            this.processServerExceptions = true;
            this.ignoredErrors = new HashSet<>();
            this.exceptionMapping = new HashMap<>();
            
            this.processResourceExceptions = true;
            
            this.requestProcessingTimeout = DEFAULT_REQUEST_TIMEOUT;
            this.timeoutTimeUnit = TimeUnit.MILLISECONDS;
            this.processTimeoutExceptions = true;
            this.retryCount = 0;
        }

        public RequestBuilder(@Nonnull String url, @Nonnull TypeToken<RESP> responseType) {
            Objects.requireNonNull(url);
            Objects.requireNonNull(responseType);

            this.url = url;
            this.responseType = responseType;
            this.defaultResponse = Optional::empty;
            this.errorResponseClass = new HashMap<>();
            
            this.processClientExceptions = true;
            this.processServerExceptions = true;
            this.ignoredErrors = new HashSet<>();
            this.exceptionMapping = new HashMap<>();
            
            this.processResourceExceptions = true;
            
            this.requestProcessingTimeout = DEFAULT_REQUEST_TIMEOUT;
            this.timeoutTimeUnit = TimeUnit.MILLISECONDS;
            this.processTimeoutExceptions = true;
            this.retryCount = 0;
        }

        public RequestBuilder defaultResponse(Supplier<Optional<RESP>> defaultResponse) {
            this.defaultResponse = defaultResponse;

            return getThis();
        }

        public RequestBuilder errorResponseClass(Integer statusCode, Class<?> errorResponseClass) {
            this.errorResponseClass.put(statusCode, errorResponseClass);

            return getThis();
        }

        public RequestBuilder processClientExceptions(boolean process) {
            this.processClientExceptions = process;

            return getThis();
        }

        public RequestBuilder processServerExceptions(boolean process) {
            this.processServerExceptions = process;

            return getThis();
        }

        public RequestBuilder addIgnoredErrors(Integer ... statusCodes) {
            this.ignoredErrors.addAll(Arrays.asList(statusCodes));

            return getThis();
        }

        public RequestBuilder addExceptionMapping(
                Map<Integer, ExceptionMapper<? extends RuntimeException, HttpStatusBasedException>> exceptionMapping) {
            this.exceptionMapping = exceptionMapping;
            return getThis();
        }

        public RequestBuilder processResourceExceptions(boolean processResourceExceptions) {
            this.processResourceExceptions = processResourceExceptions;
            return getThis();
        }

        public RequestBuilder resourceExceptionMapper(
                ExceptionMapper<? extends RuntimeException, HttpRestResourceException> resourceExceptionMapper) {
            this.resourceExceptionMapper = resourceExceptionMapper;
            return getThis();
        }

        public T requestProcessingTimeout(int requestProcessingTimeout, TimeUnit timeoutTimeUnit) {
            this.requestProcessingTimeout = requestProcessingTimeout;
            this.timeoutTimeUnit = timeoutTimeUnit;

            return getThis();
        }

        public T retryCount(int retryCount) {
            this.retryCount = retryCount;

            return getThis();
        }
        public RequestBuilder processTimeoutExceptions(boolean processTimeoutExceptions) {
            this.processTimeoutExceptions = processTimeoutExceptions;
            return getThis();
        }

        public RequestBuilder timeoutExceptionMapping(
                TimeoutExceptionMapper<? extends RuntimeException> timeoutExceptionMapping) {
            this.timeoutExceptionMapping = timeoutExceptionMapping;
            return getThis();
        }

        public RequestBuilder addParam(String name, Object value) {
            this.params.put(name, value);

            return getThis();
        }

        public RequestBuilder addParams(Map<String, Object> params) {
            this.params = params;

            return getThis();
        }

        public Optional<RESP> make() {
            ListenableFuture<ResponseEntity<RESP>> response = makeRequest();

            return defaultResponse.get();
        }

        protected abstract ListenableFuture<ResponseEntity<RESP>> makeRequest();

        protected abstract T getThis();
    }

    // region Get builder
    public class GetRequestBuilder<RESP>
            extends RequestBuilder<RESP, GetRequestBuilder<RESP>> {

        public GetRequestBuilder(String url, Class<RESP> requestClass) {
            super(url, requestClass);
        }

        public GetRequestBuilder(String url, TypeToken<RESP> responseType) {
            super(url, responseType);
        }

        @Override
        protected ListenableFuture<ResponseEntity<RESP>> makeRequest() {
            return restTemplate.getForEntity(url, responseClass, params);
        }

        @Override
        protected GetRequestBuilder<RESP> getThis() {
            return this;
        }
    }
    // endregion

    // region Post builder
    public class PostRequestBuilder<RESP>
            extends RequestBuilder<RESP, PostRequestBuilder<RESP>> {

        private Object requestBody;

        public PostRequestBuilder(String url, Class<RESP> requestClass) {
            super(url, requestClass);
        }

        public PostRequestBuilder(String url, TypeToken<RESP> responseType) {
            super(url, responseType);
        }

        public PostRequestBuilder<RESP> requestBody(Object requestBody) {
            this.requestBody = requestBody;
            return getThis();
        }

        @Override
        protected ListenableFuture<ResponseEntity<RESP>> makeRequest() {
            return restTemplate.postForEntity(url, responseClass, requestBody);
        }

        @Override
        protected PostRequestBuilder<RESP> getThis() {
            return this;
        }
    }
    // endregion
}
