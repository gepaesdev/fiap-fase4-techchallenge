package br.com.fiap.feedbackplatform.submitfeedback.function;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.submitfeedback.application.SubmitFeedbackCommand;
import br.com.fiap.feedbackplatform.submitfeedback.application.SubmitFeedbackUseCase;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

/**
 * Cloud-agnostic Spring Cloud Function: validates the API contract and delegates to the use case.
 * It knows nothing about Azure, so the same bean could be exposed through another provider's adapter.
 */
@Component
public class SubmitFeedbackFunction implements Function<SubmitFeedbackRequest, SubmitFeedbackResponse> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitFeedbackFunction.class);

    private final SubmitFeedbackUseCase submitFeedbackUseCase;
    private final Validator validator;

    public SubmitFeedbackFunction(SubmitFeedbackUseCase submitFeedbackUseCase, Validator validator) {
        this.submitFeedbackUseCase = submitFeedbackUseCase;
        this.validator = validator;
    }

    /**
     * @throws RequestValidationException when the payload violates the API contract
     */
    @Override
    public SubmitFeedbackResponse apply(SubmitFeedbackRequest request) {
        validate(request);

        Feedback feedback = submitFeedbackUseCase.submit(
                new SubmitFeedbackCommand(request.description(), request.rating()));

        // Description is never logged: it is free text written by a student and may contain personal data.
        LOGGER.info("FeedbackReceived id={} urgency={}", feedback.id(), feedback.urgency());
        return SubmitFeedbackResponse.from(feedback);
    }

    private void validate(SubmitFeedbackRequest request) {
        if (request == null) {
            throw new RequestValidationException(List.of("request body is required"));
        }
        List<String> violations = validator.validate(request).stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .toList();
        if (!violations.isEmpty()) {
            throw new RequestValidationException(violations);
        }
    }
}
