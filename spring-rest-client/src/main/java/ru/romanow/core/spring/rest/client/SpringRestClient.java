package ru.romanow.core.spring.rest.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.romanow.core.spring.rest.client.exception.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static java.lang.String.format;
import static ru.romanow.core.spring.rest.client.utils.JsonSerializer.fromJson;

public class SpringRestClient {
    private static final Logger logger = LoggerFactory.getLogger(SpringRestClient.class);

    private static final int DEFAULT_REQUEST_TIMEOUT = 3000;

    private final WebClient webClient;

    public SpringRestClient(WebClient webClient) {
        this.webClient = webClient;
    }

    // region Builders
    public <RESP> RequestBuilder<RESP> get(@Nonnull String url, @Nonnull Class<RESP> responseClass) {
        return new RequestBuilder<>(url, HttpMethod.GET, responseClass);
    }

    public <RESP> RequestBuilder<RESP> post(@Nonnull String url, @Nonnull Object requestBody, @Nonnull Class<RESP> requestClass) {
        return new RequestBuilder<>(url, HttpMethod.POST, requestBody, requestClass);
    }

    public <RESP> RequestBuilder<RESP> patch(@Nonnull String url, @Nonnull Object requestBody, @Nonnull Class<RESP> requestClass) {
        return new RequestBuilder<>(url, HttpMethod.PATCH, requestBody, requestClass);
    }

    public <RESP> RequestBuilder<RESP> put(@Nonnull String url, @Nonnull Object requestBody, @Nonnull Class<RESP> requestClass) {
        return new RequestBuilder<>(url, HttpMethod.PUT, requestBody, requestClass);
    }

    public <RESP> RequestBuilder<RESP> delete(@Nonnull String url, @Nonnull Class<RESP> requestClass) {
        return new RequestBuilder<>(url, HttpMethod.DELETE, requestClass);
    }
    // endregion

    public class RequestBuilder<RESP> {
        private String url;
        private HttpMethod method;
        private Object requestBody;
        private Map<String, String> params;
        private Map<String, String> headers;

        private Class<RESP> responseClass;

        private Supplier<Optional<RESP>> defaultResponse;
        private Map<Integer, Class> errorResponseClass;

        private boolean processClientExceptions;
        private boolean processServerExceptions;
        private Map<Integer, ExceptionMapper<? extends RuntimeException, HttpStatusBasedException>> exceptionMapping;

        private boolean processResourceExceptions;
        private ExceptionMapper<? extends RuntimeException, HttpRestResourceException> resourceExceptionMapper;

        private int requestProcessingTimeout;
        private TemporalUnit timeoutTimeUnit;
        private int retryCount;
        private boolean retryServerError;
        private boolean retryConnectionError;
        private boolean processTimeoutExceptions;
        private TimeoutExceptionMapper<? extends RuntimeException> timeoutExceptionMapping;

        RequestBuilder(@Nonnull String url, @Nonnull HttpMethod httpMethod, @Nullable Object requestBody, @Nonnull Class<RESP> responseClass) {
            this.url = url;
            this.method = httpMethod;
            this.params = new HashMap<>();
            this.headers = new HashMap<>();
            this.responseClass = responseClass;
            this.defaultResponse = Optional::empty;
            this.errorResponseClass = new HashMap<>();

            this.processClientExceptions = true;
            this.processServerExceptions = true;
            this.exceptionMapping = new HashMap<>();

            this.processResourceExceptions = true;

            this.requestProcessingTimeout = DEFAULT_REQUEST_TIMEOUT;
            this.retryServerError = false;
            this.retryConnectionError = false;
            this.timeoutTimeUnit = ChronoUnit.MILLIS;
            this.processTimeoutExceptions = true;
            this.retryCount = 0;
        }

        RequestBuilder(@Nonnull String url, @Nonnull HttpMethod httpMethod, @Nonnull Class<RESP> responseClass) {
            this(url, httpMethod, null, responseClass);
        }

        @Nonnull
        public RequestBuilder<RESP> defaultResponse(@Nonnull Supplier<Optional<RESP>> defaultResponse) {
            this.defaultResponse = defaultResponse;
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> errorResponseClass(int statusCode, Class<?> errorResponseClass) {
            this.errorResponseClass.put(statusCode, errorResponseClass);
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> processClientExceptions(boolean process) {
            this.processClientExceptions = process;
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> processServerExceptions(boolean process) {
            this.processServerExceptions = process;
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> addExceptionMapping(int status, @Nonnull ExceptionMapper<? extends RuntimeException, HttpStatusBasedException> mapping) {
            this.exceptionMapping.put(status, mapping);
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> processResourceExceptions(boolean process) {
            this.processResourceExceptions = process;
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> resourceExceptionMapper(
                @Nonnull ExceptionMapper<? extends RuntimeException, HttpRestResourceException> resourceExceptionMapper) {
            this.resourceExceptionMapper = resourceExceptionMapper;
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> requestProcessingTimeout(int requestProcessingTimeout, @Nonnull TemporalUnit timeoutTimeUnit) {
            this.requestProcessingTimeout = requestProcessingTimeout;
            this.timeoutTimeUnit = timeoutTimeUnit;
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> retryCount(int retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> retryServerError(boolean retry) {
            this.retryServerError = retry;
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> retryConnectionError(boolean retry) {
            this.retryConnectionError = retry;
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> processTimeoutExceptions(boolean processTimeoutExceptions) {
            this.processTimeoutExceptions = processTimeoutExceptions;
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> timeoutExceptionMapping(
                @Nonnull TimeoutExceptionMapper<? extends RuntimeException> timeoutExceptionMapping) {
            this.timeoutExceptionMapping = timeoutExceptionMapping;
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> addParam(@Nonnull String name, @Nullable String value) {
            this.params.put(name, value);
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> addHeader(@Nonnull String name, @Nonnull String value) {
            this.headers.put(name, value);
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> addParams(@Nonnull Map<String, String> params) {
            this.params = params;
            return this;
        }

        @Nonnull
        public Optional<RESP> execute() {
            final Mono<ClientResponse> response = prepareRequest()
                    .exchange()
                    .timeout(Duration.of(requestProcessingTimeout, timeoutTimeUnit))
                    .retry(retryCount, this::retry)
                    .doOnSuccess(body -> body.bodyToMono(responseClass))
                    .doOnError(HttpClientErrorException.class, this::processClientError)
                    .doOnError(HttpServerErrorException.class, this::processServerError)
                    .doOnError(ResourceAccessException.class, this::processResourceError)
                    .doOnError(TimeoutException.class, this::processTimeoutError);

            return defaultResponse.get();
        }

        private boolean retry(Throwable throwable) {
            if (throwable instanceof HttpServerErrorException) {
                return this.retryServerError;
            } else if (throwable instanceof ResourceAccessException) {
                return this.retryConnectionError;
            }

            return throwable instanceof TimeoutException;
        }

        private void processTimeoutError(TimeoutException exception) {
            final String message = format("Request to '%s' failed with timeout", this.url);
            logger.warn(message);

            if (this.processTimeoutExceptions) {
                final HttpRestTimeoutException timeoutException = new HttpRestTimeoutException(exception);
                if (this.timeoutExceptionMapping != null) {
                    throw this.timeoutExceptionMapping.produce(timeoutException);
                } else {
                    throw timeoutException;
                }
            }
        }

        private void processClientError(HttpClientErrorException exception) {
            final int status = exception.getRawStatusCode();
            final String reason = exception.getStatusText();
            final String message = format("Request to '%s' failed with client error: %d:%s", this.url, status, reason);
            logger.warn(message);

            final HttpRestClientException customException =
                    new HttpRestClientException(status, reason, getErrorResponse(exception));
            if (this.exceptionMapping.containsKey(status)) {
                throw this.exceptionMapping.get(status).produce(customException);
            } else {
                throw exception;
            }
        }

        private void processServerError(HttpServerErrorException exception) {
            final int status = exception.getRawStatusCode();
            final String reason = exception.getStatusText();
            final String message = format("Request to '%s' failed with server error: %d:%s", this.url, status, reason);
            logger.warn(message);

            if (this.processServerExceptions) {
                final HttpRestServerException customException =
                        new HttpRestServerException(status, reason, getErrorResponse(exception));
                if (this.exceptionMapping.containsKey(status)) {
                    throw this.exceptionMapping.get(status).produce(customException);
                } else {
                    throw exception;
                }
            }
        }

        private void processResourceError(ResourceAccessException exception) {
            final String message = format("Can't establish connection to '%s'", this.url);
            logger.warn(message);

            if (this.processResourceExceptions) {
                final HttpRestResourceException resourceException = new HttpRestResourceException(exception);
                if (this.resourceExceptionMapper != null) {
                    throw this.resourceExceptionMapper.produce(resourceException);
                } else {
                    throw resourceException;
                }
            }
        }

        private Object getErrorResponse(RestClientResponseException exception) {
            final String response = exception.getResponseBodyAsString();
            final Class<?> cls = this.errorResponseClass.get(exception.getRawStatusCode());
            return cls != null ? fromJson(response, cls) : response;
        }

        @Nonnull
        private WebClient.RequestBodySpec prepareRequest() {
            final WebClient.RequestBodyUriSpec method = webClient.method(HttpMethod.GET);
            headers.forEach(method::header);
            return method.uri(url, params);
        }
    }
}
