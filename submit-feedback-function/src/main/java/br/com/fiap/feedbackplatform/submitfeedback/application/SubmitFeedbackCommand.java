package br.com.fiap.feedbackplatform.submitfeedback.application;

/**
 * Input of {@link SubmitFeedbackUseCase}, already free of transport concerns (JSON field names, HTTP).
 */
public record SubmitFeedbackCommand(String description, int rating) {
}
