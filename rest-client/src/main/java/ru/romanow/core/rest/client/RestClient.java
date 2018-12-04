package ru.romanow.core.rest.client;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.romanow.core.rest.client.exception.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static java.lang.String.format;
import static org.apache.http.util.TextUtils.isEmpty;
import static ru.romanow.core.rest.client.utils.JsonSerializer.fromJson;
import static ru.romanow.core.rest.client.utils.JsonSerializer.toJson;

public class RestClient {
    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

    private static final int DEFAULT_REQUEST_TIMEOUT = 3000;
    private static final int SOCKET_TIMEOUT = 300;
    private static final int CONNECTION_TIMEOUT = 100;
    private static final int MAX_CONNECTIONS = 100;

    private final CloseableHttpAsyncClient httpClient;

    public RestClient() {
        RequestConfig requestConfig = RequestConfig
                .custom()
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectTimeout(CONNECTION_TIMEOUT).build();
        this.httpClient = HttpAsyncClients
                .custom()
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnTotal(MAX_CONNECTIONS)
                .build();
    }

    // region Builders
    public <RESP> GetRequestBuilder<RESP> get(@Nonnull String url, @Nonnull Class<RESP> responseClass) {
        return new GetRequestBuilder<>(url, responseClass);
    }

    public <RESP> PostRequestBuilder<RESP> post(@Nonnull String url, @Nonnull Class<RESP> requestClass) {
        return new PostRequestBuilder<>(url, requestClass);
    }

    public <RESP> PatchRequestBuilder<RESP> patch(@Nonnull String url, @Nonnull Class<RESP> requestClass) {
        return new PatchRequestBuilder<>(url, requestClass);
    }

    public <RESP> PutRequestBuilder<RESP> put(@Nonnull String url, @Nonnull Class<RESP> requestClass) {
        return new PutRequestBuilder<>(url, requestClass);
    }

    public <RESP> DeleteRequestBuilder<RESP> delete(@Nonnull String url, @Nonnull Class<RESP> requestClass) {
        return new DeleteRequestBuilder<>(url, requestClass);
    }
    // endregion

    public abstract class RequestBuilder<RESP, T extends RequestBuilder<RESP, T>> {
        protected String url;
        protected Map<String, String> params;
        protected Map<String, String> headers;

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
        private boolean processTimeoutExceptions;
        private TimeoutExceptionMapper<? extends RuntimeException> timeoutExceptionMapping;

        public RequestBuilder(@Nonnull String url, @Nonnull Class<RESP> responseClass) {
            this.url = url;
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
            this.retryServerError = true;
            this.timeoutTimeUnit = TimeUnit.MILLISECONDS;
            this.processTimeoutExceptions = true;
            this.retryCount = 0;
        }

        @Nonnull
        public T defaultResponse(Supplier<Optional<RESP>> defaultResponse) {
            this.defaultResponse = defaultResponse;
            return getThis();
        }

        @Nonnull
        public T errorResponseClass(Integer statusCode, Class<?> errorResponseClass) {
            this.errorResponseClass.put(statusCode, errorResponseClass);
            return getThis();
        }

        @Nonnull
        public T processClientExceptions(boolean process) {
            this.processClientExceptions = process;
            return getThis();
        }

        @Nonnull
        public T processServerExceptions(boolean process) {
            this.processServerExceptions = process;
            return getThis();
        }

        @Nonnull
        public T addExceptionMapping(int status, @Nonnull ExceptionMapper<? extends RuntimeException, HttpStatusBasedException> mapping) {
            this.exceptionMapping.put(status, mapping);
            return getThis();
        }

        @Nonnull
        public T processResourceExceptions(boolean processResourceExceptions) {
            this.processResourceExceptions = processResourceExceptions;
            return getThis();
        }

        @Nonnull
        public T resourceExceptionMapper(
                ExceptionMapper<? extends RuntimeException, HttpRestResourceException> resourceExceptionMapper) {
            this.resourceExceptionMapper = resourceExceptionMapper;
            return getThis();
        }

        @Nonnull
        public T requestProcessingTimeout(int requestProcessingTimeout, TimeUnit timeoutTimeUnit) {
            this.requestProcessingTimeout = requestProcessingTimeout;
            this.timeoutTimeUnit = timeoutTimeUnit;
            return getThis();
        }

        @Nonnull
        public T retryCount(int retryCount) {
            this.retryCount = retryCount;
            return getThis();
        }

        @Nonnull
        public T retryServerError(boolean retryServerError) {
            this.retryServerError = retryServerError;
            return getThis();
        }

        @Nonnull
        public T processTimeoutExceptions(boolean processTimeoutExceptions) {
            this.processTimeoutExceptions = processTimeoutExceptions;
            return getThis();
        }

        @Nonnull
        public T timeoutExceptionMapping(
                TimeoutExceptionMapper<? extends RuntimeException> timeoutExceptionMapping) {
            this.timeoutExceptionMapping = timeoutExceptionMapping;
            return getThis();
        }

        @Nonnull
        public T addParam(String name, String value) {
            this.params.put(name, value);
            return getThis();
        }

        @Nonnull
        public T addHeader(String name, String value) {
            this.headers.put(name, value);
            return getThis();
        }

        @Nonnull
        public T addParams(Map<String, String> params) {
            this.params = params;
            return getThis();
        }

        @Nonnull
        public Optional<RESP> execute() {
            try {
                httpClient.start();
                final HttpRequestBase request = prepareRequest();
                return executeRequest(request, retryCount);
            } finally {
                try {
                    httpClient.close();
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }

        @Nonnull
        protected Optional<RESP> executeRequest(@Nonnull HttpRequestBase request, int retryCount) {
            final Future<HttpResponse> response = httpClient.execute(request, null);
            try {
                final HttpResponse httpResponse = response.get(this.requestProcessingTimeout, this.timeoutTimeUnit);
                final int status = httpResponse.getStatusLine().getStatusCode();
                final String reason = httpResponse.getStatusLine().getReasonPhrase();

                if (isOk(status)) {
                    return getResponseData(httpResponse.getEntity());
                } else if (isClientError(status) && processClientExceptions) {
                    final String message = format("Request to '%s' failed with client error: %d:%s",
                                                  this.url, status, reason);
                    logger.warn(message);

                    final HttpRestClientException exception =
                            new HttpRestClientException(status, reason, getResponseBody(httpResponse.getEntity()));
                    if (this.exceptionMapping.containsKey(status)) {
                        this.exceptionMapping.get(status).produce(exception);
                    } else {
                        throw exception;
                    }
                } else if (isServerError(status)) {
                    if (this.retryServerError && retryCount > 0) {
                        return executeRequest(request, retryCount - 1);
                    }

                    if (this.processServerExceptions) {
                        final String message = format("Request to '%s' failed with server error: %d:%s",
                                                      this.url, status, reason);
                        logger.warn(message);

                        final HttpRestServerException exception =
                                new HttpRestServerException(status, reason, getResponseBody(httpResponse.getEntity()));
                        if (this.exceptionMapping.containsKey(status)) {
                            this.exceptionMapping.get(status).produce(exception);
                        } else {
                            throw exception;
                        }
                    }
                }
            } catch (ExecutionException exception) {
                logger.error("Execution", exception);
            } catch (TimeoutException exception) {
                if (retryCount > 0) {
                    return executeRequest(request, retryCount - 1);
                }
                final String message = format("Request to '%s' failed with timeout", this.url);
                logger.warn(message);

                if (this.processTimeoutExceptions && this.timeoutExceptionMapping != null) {
                    this.timeoutExceptionMapping.produce(new HttpRestTimeoutException(exception));
                }
            } catch (InterruptedException exception) {
                logger.error("InterruptedException", exception);
            }

            return defaultResponse.get();
        }

        @Nonnull
        private Optional<RESP> getResponseData(@Nonnull HttpEntity entity) {
            final String response = getResponseBody(entity);
            return Optional.ofNullable(fromJson(response, this.responseClass));
        }

        @Nullable
        private String getResponseBody(@Nonnull HttpEntity entity) {
            try {
                final String response = EntityUtils.toString(entity, Charset.forName("UTF-8"));
                return !isEmpty(response) ? response : null;
            } catch (IOException exception) {
                logger.warn("Parse response body failed: {}", exception.getMessage());
                return null;
            }
        }

        private boolean isOk(int status) {
            return status >= 200 && status < 300;
        }

        private boolean isClientError(int status) {
            return status >= 400 && status < 500;
        }

        private boolean isServerError(int status) {
            return status >= 500;
        }

        @Nonnull
        protected URI buildUrl() {
            try {
                URIBuilder builder = new URIBuilder(this.url);
                this.params.forEach(builder::addParameter);
                return builder.build();
            } catch (URISyntaxException exception) {
                throw new RuntimeException(exception);
            }
        }

        @Nonnull
        protected abstract HttpRequestBase prepareRequest();

        @Nonnull
        protected abstract T getThis();
    }

    // region Get builder
    public class GetRequestBuilder<RESP>
            extends RequestBuilder<RESP, GetRequestBuilder<RESP>> {

        public GetRequestBuilder(String url, Class<RESP> requestClass) {
            super(url, requestClass);
        }

        @Nonnull
        @Override
        protected HttpGet prepareRequest() {
            final HttpGet get = new HttpGet(buildUrl());
            this.headers.forEach(get::setHeader);
            return get;
        }

        @Nonnull
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

        public PostRequestBuilder<RESP> requestBody(Object requestBody) {
            this.requestBody = requestBody;
            return getThis();
        }

        @Nonnull
        @Override
        protected HttpPost prepareRequest() {
            final HttpPost post = new HttpPost(buildUrl());
            this.headers.forEach(post::setHeader);
            post.setEntity(new StringEntity(toJson(this.requestBody), ContentType.APPLICATION_JSON));
            return post;
        }

        @Nonnull
        @Override
        protected PostRequestBuilder<RESP> getThis() {
            return this;
        }
    }
    // endregion

    // region Patch builder
    public class PatchRequestBuilder<RESP>
            extends RequestBuilder<RESP, PatchRequestBuilder<RESP>> {
        private Object requestBody;

        public PatchRequestBuilder(String url, Class<RESP> requestClass) {
            super(url, requestClass);
        }

        public PatchRequestBuilder<RESP> requestBody(Object requestBody) {
            this.requestBody = requestBody;
            return getThis();
        }

        @Nonnull
        @Override
        protected HttpPatch prepareRequest() {
            final HttpPatch patch = new HttpPatch(buildUrl());
            this.headers.forEach(patch::setHeader);
            patch.setEntity(new StringEntity(toJson(this.requestBody), ContentType.APPLICATION_JSON));
            return patch;
        }

        @Nonnull
        @Override
        protected PatchRequestBuilder<RESP> getThis() {
            return this;
        }
    }
    // endregion

    // region Put builder
    public class PutRequestBuilder<RESP>
            extends RequestBuilder<RESP, PutRequestBuilder<RESP>> {
        private Object requestBody;

        public PutRequestBuilder(String url, Class<RESP> requestClass) {
            super(url, requestClass);
        }

        @Nonnull
        public PutRequestBuilder<RESP> requestBody(Object requestBody) {
            this.requestBody = requestBody;
            return getThis();
        }

        @Nonnull
        @Override
        protected HttpPut prepareRequest() {
            final HttpPut put = new HttpPut(buildUrl());
            this.headers.forEach(put::setHeader);
            put.setEntity(new StringEntity(toJson(this.requestBody), ContentType.APPLICATION_JSON));
            return put;
        }

        @Nonnull
        @Override
        protected PutRequestBuilder<RESP> getThis() {
            return this;
        }
    }
    // endregion

    // region Delete builder
    public class DeleteRequestBuilder<RESP>
            extends RequestBuilder<RESP, DeleteRequestBuilder<RESP>> {
        public DeleteRequestBuilder(String url, Class<RESP> requestClass) {
            super(url, requestClass);
        }

        @Nonnull
        @Override
        protected HttpDelete prepareRequest() {
            final HttpDelete delete = new HttpDelete(buildUrl());
            this.headers.forEach(delete::setHeader);
            return delete;
        }

        @Nonnull
        @Override
        protected DeleteRequestBuilder<RESP> getThis() {
            return this;
        }
    }
    // endregion
}
