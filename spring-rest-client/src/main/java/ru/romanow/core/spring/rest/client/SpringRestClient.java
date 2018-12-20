package ru.romanow.core.spring.rest.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;
import ru.romanow.core.spring.rest.client.exception.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static ru.romanow.core.spring.rest.client.utils.JsonSerializer.fromJson;

public class SpringRestClient {
    private static final Logger logger = LoggerFactory.getLogger(SpringRestClient.class);

    private static final int DEFAULT_REQUEST_TIMEOUT = 3000;

    private final RestTemplate restTemplate;

    public SpringRestClient(@Nonnull RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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
        private MultiValueMap<String, String> params;
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
        private TimeUnit timeoutTimeUnit;
        private int retryCount;
        private boolean retryServerError;
        private boolean retryConnectionError;
        private boolean processTimeoutExceptions;
        private TimeoutExceptionMapper<? extends RuntimeException> timeoutExceptionMapping;

        RequestBuilder(@Nonnull String url, @Nonnull HttpMethod httpMethod, @Nullable Object requestBody, @Nonnull Class<RESP> responseClass) {
            this.url = url;
            this.requestBody = requestBody;
            this.method = httpMethod;
            this.params = new LinkedMultiValueMap<>();
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
            this.timeoutTimeUnit = TimeUnit.MILLISECONDS;
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
        public RequestBuilder<RESP> requestProcessingTimeout(int requestProcessingTimeout, @Nonnull TimeUnit timeoutTimeUnit) {
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
            this.params.add(name, value);
            return this;
        }

        @Nonnull
        public RequestBuilder<RESP> addHeader(@Nonnull String name, @Nonnull String value) {
            this.headers.put(name, value);
            return this;
        }

        @Nonnull
        public Optional<RESP> execute() {
            return executeRequest(buildRequest(), retryCount);
        }

        @Nonnull
        private Optional<RESP> executeRequest(RequestEntity<?> request, int retryCount) {
            final CompletableFuture<ResponseEntity<RESP>> future =
                    supplyAsync(() -> restTemplate.exchange(request, responseClass));
            try {
                ResponseEntity<RESP> response = future.get(this.requestProcessingTimeout, this.timeoutTimeUnit);
                if (response.getStatusCode().is2xxSuccessful()) {
                    return ofNullable(response.getBody());
                }
            } catch (ExecutionException exception) {
                if (exception.getCause() instanceof ResourceAccessException) {
                    if (this.retryConnectionError && retryCount > 0) {
                        return executeRequest(request, retryCount - 1);
                    }
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
                } else if (exception.getCause() instanceof HttpClientErrorException) {
                    final HttpClientErrorException clientErrorException = (HttpClientErrorException)exception.getCause();
                    final int status = clientErrorException.getRawStatusCode();
                    final String reason = clientErrorException.getStatusText();

                    final String message = format("Request to '%s' failed with client error: %d:%s", this.url, status, reason);
                    logger.warn(message);

                    if (processClientExceptions) {
                        final HttpRestClientException customException =
                                new HttpRestClientException(status, reason,
                                        getErrorResponseBody(status, clientErrorException.getResponseBodyAsString()));
                        if (this.exceptionMapping.containsKey(status)) {
                            throw this.exceptionMapping.get(status).produce(customException);
                        } else {
                            throw customException;
                        }
                    }
                } else if (exception.getCause() instanceof HttpServerErrorException) {
                    if (this.retryServerError && retryCount > 0) {
                        return executeRequest(request, retryCount - 1);
                    }

                    final HttpServerErrorException serverErrorException = (HttpServerErrorException)exception.getCause();
                    final int status = serverErrorException.getRawStatusCode();
                    final String reason = serverErrorException.getStatusText();

                    if (this.processServerExceptions) {
                        final String message = format("Request to '%s' failed with server error: %d:%s", this.url, status, reason);
                        logger.warn(message);

                        final HttpRestServerException customException =
                                new HttpRestServerException(status, reason,
                                        getErrorResponseBody(status, serverErrorException.getResponseBodyAsString()));
                        if (this.exceptionMapping.containsKey(status)) {
                            throw this.exceptionMapping.get(status).produce(customException);
                        } else {
                            throw customException;
                        }
                    }
                } else {
                    throw new RuntimeException(exception);
                }
            } catch (TimeoutException exception) {
                if (retryCount > 0) {
                    return executeRequest(request, retryCount - 1);
                }
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
            } catch (InterruptedException exception) {
                logger.error("InterruptedException", exception);
            }

            return defaultResponse.get();
        }

        private Object getErrorResponseBody(int status, @Nullable String response) {
            if (this.errorResponseClass.containsKey(status)) {
                final Class<?> cls = this.errorResponseClass.get(status);
                return fromJson(response, cls);
            }
            return response;
        }

        @Nonnull
        private URI buildUri() {
            return UriComponentsBuilder
                    .fromUriString(this.url)
                    .queryParams(this.params)
                    .build()
                    .toUri();
        }

        @Nonnull
        private RequestEntity<?> buildRequest() {
            RequestEntity.BodyBuilder request = RequestEntity.method(this.method, buildUri());
            headers.forEach(request::header);
            if (requestBody != null) {
                request.contentType(MediaType.APPLICATION_JSON_UTF8);
                request.body(requestBody);
            }
            return request.build();
        }
    }
}
