package ru.romanow.core.rest.client;

import com.google.common.net.MediaType;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.romanow.core.rest.client.exception.ExceptionMapper;
import ru.romanow.core.rest.client.exception.HttpRestResourceException;
import ru.romanow.core.rest.client.exception.HttpStatusBasedException;
import ru.romanow.core.rest.client.exception.TimeoutExceptionMapper;
import ru.romanow.core.rest.client.utils.JsonSerializer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static ru.romanow.core.rest.client.utils.JsonSerializer.toJson;

public class RestClient {
    private static final Logger logger = LoggerFactory.getLogger(RestClient.class);

    private static final int DEFAULT_REQUEST_TIMEOUT = 3000;
    
    private final CloseableHttpAsyncClient httpClient;

    public RestClient() {
        this.httpClient = HttpAsyncClients.createDefault();
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

    public <RESP> PutRequestBuilder<RESP> post(@Nonnull String url, @Nonnull Class<RESP> requestClass) {
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
            this.timeoutTimeUnit = TimeUnit.MILLISECONDS;
            this.processTimeoutExceptions = true;
            this.retryCount = 0;
        }

        public T defaultResponse(Supplier<Optional<RESP>> defaultResponse) {
            this.defaultResponse = defaultResponse;
            return getThis();
        }

        public T errorResponseClass(Integer statusCode, Class<?> errorResponseClass) {
            this.errorResponseClass.put(statusCode, errorResponseClass);
            return getThis();
        }

        public T processClientExceptions(boolean process) {
            this.processClientExceptions = process;
            return getThis();
        }

        public T processServerExceptions(boolean process) {
            this.processServerExceptions = process;
            return getThis();
        }

        public T addExceptionMapping(
                Map<Integer, ExceptionMapper<? extends RuntimeException, HttpStatusBasedException>> exceptionMapping) {
            this.exceptionMapping = exceptionMapping;
            return getThis();
        }

        public T processResourceExceptions(boolean processResourceExceptions) {
            this.processResourceExceptions = processResourceExceptions;
            return getThis();
        }

        public T resourceExceptionMapper(
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

        public T processTimeoutExceptions(boolean processTimeoutExceptions) {
            this.processTimeoutExceptions = processTimeoutExceptions;
            return getThis();
        }

        public T timeoutExceptionMapping(
                TimeoutExceptionMapper<? extends RuntimeException> timeoutExceptionMapping) {
            this.timeoutExceptionMapping = timeoutExceptionMapping;
            return getThis();
        }

        public T addParam(String name, String value) {
            this.params.put(name, value);
            return getThis();
        }

        public T addHeader(String name, String value) {
            this.headers.put(name, value);
            return getThis();
        }

        public T addParams(Map<String, String> params) {
            this.params = params;
            return getThis();
        }

        public Optional<RESP> execute() {
            try {
                httpClient.start();
                return executeRequest();
            } finally {
                try {
                    httpClient.close();
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            }
        }

        protected Optional<RESP> executeRequest() {

        }

        protected URI buildUrl() {
            try {
                URIBuilder builder = new URIBuilder(url);
                params.forEach(builder::addParameter);
                return builder.build();
            } catch (URISyntaxException exception) {
                throw new RuntimeException(exception);
            }
        }

        protected abstract HttpRequestBase prepareRequest();

        protected abstract T getThis();
    }

    // region Get builder
    public class GetRequestBuilder<RESP>
            extends RequestBuilder<RESP, GetRequestBuilder<RESP>> {

        public GetRequestBuilder(String url, Class<RESP> requestClass) {
            super(url, requestClass);
        }

        @Override
        protected HttpGet prepareRequest() {
            HttpGet get = new HttpGet(buildUrl());
            headers.forEach(get::setHeader);
            return get;
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

        public PostRequestBuilder<RESP> requestBody(Object requestBody) {
            this.requestBody = requestBody;
            return getThis();
        }

        @Override
        protected HttpPost prepareRequest() {
            HttpPost post = new HttpPost(buildUrl());
            headers.forEach(post::setHeader);
            post.setEntity(new StringEntity(toJson(requestBody), ContentType.APPLICATION_JSON));
            return post;
        }

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

        @Override
        protected HttpPatch prepareRequest() {
            HttpPatch patch = new HttpPatch(buildUrl());
            headers.forEach(patch::setHeader);
            patch.setEntity(new StringEntity(toJson(requestBody), ContentType.APPLICATION_JSON));
            return patch;
        }

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

        public PutRequestBuilder<RESP> requestBody(Object requestBody) {
            this.requestBody = requestBody;
            return getThis();
        }

        @Override
        protected HttpPut prepareRequest() {
            HttpPut put = new HttpPut(buildUrl());
            headers.forEach(put::setHeader);
            put.setEntity(new StringEntity(toJson(requestBody), ContentType.APPLICATION_JSON));
            return put;
        }

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

        @Override
        protected HttpDelete prepareRequest() {
            HttpDelete delete = new HttpDelete(buildUrl());
            headers.forEach(delete::setHeader);
            return delete;
        }

        @Override
        protected DeleteRequestBuilder<RESP> getThis() {
            return this;
        }
    }
    // endregion
}
