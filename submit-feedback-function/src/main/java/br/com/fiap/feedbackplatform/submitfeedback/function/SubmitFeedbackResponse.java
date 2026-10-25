package br.com.fiap.feedbackplatform.submitfeedback.function;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Urgency;

import java.time.Instant;

/**
 * Confirmation returned to the student after the feedback is stored.
 */
public record SubmitFeedbackResponse(String id, Urgency urgency, Instant createdAt) {

    public static SubmitFeedbackResponse from(Feedback feedback) {
        return new SubmitFeedbackResponse(feedback.id(), feedback.urgency(), feedback.createdAt());
    }
}
