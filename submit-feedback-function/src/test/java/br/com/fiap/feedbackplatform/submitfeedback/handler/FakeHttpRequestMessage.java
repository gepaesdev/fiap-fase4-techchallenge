package br.com.fiap.feedbackplatform.submitfeedback.handler;

import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.HttpStatusType;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory stand-in for the Azure Functions HTTP request, so handlers are tested without the Functions host.
 */
public final class FakeHttpRequestMessage implements HttpRequestMessage<Optional<String>> {

    private final Optional<String> body;

    private FakeHttpRequestMessage(Optional<String> body) {
        this.body = body;
    }

    public static FakeHttpRequestMessage withBody(String body) {
        return new FakeHttpRequestMessage(Optional.ofNullable(body));
    }

    public static FakeHttpRequestMessage withoutBody() {
        return new FakeHttpRequestMessage(Optional.empty());
    }

    @Override
    public URI getUri() {
        return URI.create("http://localhost:7071/api/avaliacao");
    }

    @Override
    public HttpMethod getHttpMethod() {
        return HttpMethod.POST;
    }

    @Override
    public Map<String, String> getHeaders() {
        return Map.of();
    }

    @Override
    public Map<String, String> getQueryParameters() {
        return Map.of();
    }

    @Override
    public Optional<String> getBody() {
        return body;
    }

    @Override
    public HttpResponseMessage.Builder createResponseBuilder(HttpStatus status) {
        return new FakeResponseBuilder(status);
    }

    @Override
    public HttpResponseMessage.Builder createResponseBuilder(HttpStatusType status) {
        return new FakeResponseBuilder(status);
    }

    private static final class FakeResponseBuilder implements HttpResponseMessage.Builder {

        private final Map<String, String> headers = new HashMap<>();
        private HttpStatusType status;
        private Object body;

        private FakeResponseBuilder(HttpStatusType status) {
            this.status = status;
        }

        @Override
        public HttpResponseMessage.Builder status(HttpStatusType status) {
            this.status = status;
            return this;
        }

        @Override
        public HttpResponseMessage.Builder header(String key, String value) {
            headers.put(key, value);
            return this;
        }

        @Override
        public HttpResponseMessage.Builder body(Object body) {
            this.body = body;
            return this;
        }

        @Override
        public HttpResponseMessage build() {
            return new FakeResponse(status, Map.copyOf(headers), body);
        }
    }

    private record FakeResponse(HttpStatusType status, Map<String, String> headers, Object body)
            implements HttpResponseMessage {

        @Override
        public HttpStatusType getStatus() {
            return status;
        }

        @Override
        public String getHeader(String key) {
            return headers.get(key);
        }

        @Override
        public Object getBody() {
            return body;
        }
    }
}
