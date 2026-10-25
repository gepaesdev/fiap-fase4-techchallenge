package br.com.fiap.feedbackplatform.submitfeedback.handler;

import br.com.fiap.feedbackplatform.core.domain.InvalidFeedbackException;
import br.com.fiap.feedbackplatform.submitfeedback.function.RequestValidationException;
import br.com.fiap.feedbackplatform.submitfeedback.function.SubmitFeedbackFunction;
import br.com.fiap.feedbackplatform.submitfeedback.function.SubmitFeedbackRequest;
import br.com.fiap.feedbackplatform.submitfeedback.function.SubmitFeedbackResponse;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

/**
 * Azure Functions HTTP binding for {@code POST /api/avaliacao}. Only translates HTTP to the function and
 * back: {@code 201} on success, {@code 400} for client errors. Unexpected failures propagate so the
 * Functions host records them as failed executions (which drives the monitoring alerts).
 */
@Component
public class SubmitFeedbackHttpHandler {

    static final String FUNCTION_NAME = "submit-feedback";
    static final String ROUTE = "avaliacao";

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String INVALID_REQUEST = "Invalid request";

    private final SubmitFeedbackFunction submitFeedbackFunction;
    private final JsonMapper jsonMapper;

    public SubmitFeedbackHttpHandler(SubmitFeedbackFunction submitFeedbackFunction, JsonMapper jsonMapper) {
        this.submitFeedbackFunction = submitFeedbackFunction;
        this.jsonMapper = jsonMapper;
    }

    @FunctionName(FUNCTION_NAME)
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = "request",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.FUNCTION,
                    route = ROUTE)
            HttpRequestMessage<Optional<String>> request,
            ExecutionContext context) {
        try {
            SubmitFeedbackRequest submitFeedbackRequest = parseBody(request);
            SubmitFeedbackResponse response = submitFeedbackFunction.apply(submitFeedbackRequest);
            return jsonResponse(request, HttpStatus.CREATED, response);
        } catch (RequestValidationException exception) {
            return badRequest(request, exception.violations());
        } catch (InvalidFeedbackException exception) {
            return badRequest(request, List.of(exception.getMessage()));
        }
    }

    private SubmitFeedbackRequest parseBody(HttpRequestMessage<Optional<String>> request) {
        String body = request.getBody()
                .filter(content -> !content.isBlank())
                .orElseThrow(() -> new RequestValidationException(List.of("request body is required")));
        try {
            return jsonMapper.readValue(body, SubmitFeedbackRequest.class);
        } catch (JacksonException exception) {
            throw new RequestValidationException(
                    List.of("request body must be a JSON object with 'descricao' (string) and 'nota' (integer)"));
        }
    }

    private HttpResponseMessage badRequest(HttpRequestMessage<Optional<String>> request, List<String> details) {
        return jsonResponse(request, HttpStatus.BAD_REQUEST, new ErrorResponse(INVALID_REQUEST, details));
    }

    private HttpResponseMessage jsonResponse(
            HttpRequestMessage<Optional<String>> request, HttpStatus status, Object body) {
        return request.createResponseBuilder(status)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(body))
                .build();
    }
}
