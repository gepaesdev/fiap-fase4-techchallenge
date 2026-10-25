package br.com.fiap.feedbackplatform.core.domain;

/**
 * Thrown when a feedback violates a domain rule (e.g. blank description or rating out of range).
 * Callers at the edge (HTTP handler) translate it into a client error.
 */
public class InvalidFeedbackException extends RuntimeException {

    public InvalidFeedbackException(String message) {
        super(message);
    }
}
