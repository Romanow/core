## Rest Client

Simple wrapper over Apache AsyncHttpClient.
* Thread-safe, run client on every user request (invoke execute() method).
* Builder pattern for request.
* Mapping response to Java object.
* Request timeout. 
* Implements retry on timeouts, connection failures and server errors (turns on in builder).
* Suppress timeout, connection failures and client (4xx) and server (5xx) errors. In this case returning default response (optional::empty by default).
* Status code mapping to java Exceptions.

### Usage
```java
final Optional<SimpleResponse> response =
    restClient.get(url, SimpleResponse.class)
              .addParam("query", param)
              .addHeader("Accept-Language", "ru-RU, en:q=0.8")
              .addExceptionMapping(HttpStatus.SC_BAD_GATEWAY, (ex) -> new CustomException(ex.getBody().toString()))
              .processServerExceptions(false)
              .retryServerError(true)
              .retryCount(3)
              .execute();
```

### Params

| Method | Description |
| ----- |-------------|
| addParam(String name, String value) | add query param |
| addHeader(String name, String value) | add request header |
| defaultResponse(Supplier<Optional<RESP>> defaultResponse) | default response on suppressed errors (by default Optional.empty()) |
| errorResponseClass(Integer statusCode, Class<?> errorResponseClass) | mapping for serialize error body |
| processClientExceptions(boolean process) | throw exception on client errors (4xx) or return default response |
| processServerExceptions(boolean process) | throw exception on server errors (5xx) or return default response |
| addExceptionMapping(int status, ExceptionMapper<? extends RuntimeException, HttpStatusBasedException> mapping) | mapping HTTP status on custom exception |
| processResourceExceptions(boolean process) | throw exception on connection errors or return default response |
| resourceExceptionMapper(ExceptionMapper<? extends RuntimeException, HttpRestResourceException> resourceExceptionMapper) | mapping connection error on custom exception |
| processTimeoutExceptions(boolean processTimeoutExceptions) | throw exception request timeout or return default response |
| timeoutExceptionMapping(TimeoutExceptionMapper<? extends RuntimeException> timeoutExceptionMapping) | mapping timeout error on custom exception |
| requestProcessingTimeout(int requestProcessingTimeout, TimeUnit timeoutTimeUnit) | request timeout |
| retryCount(int retryCount) | retry count on error |
| retryServerError(boolean retry) | retry on server (5xx) errors |
| retryConnectionError(boolean retry) | retry on connection errors |
| execute() | execute request |
