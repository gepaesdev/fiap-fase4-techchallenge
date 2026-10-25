package br.com.fiap.feedbackplatform.submitfeedback.function;

import java.util.List;

/**
 * Thrown when the request payload is malformed or violates the API contract.
 * Carries every violation so the client can fix all of them at once.
 */
public class RequestValidationException extends RuntimeException {

    private final List<String> violations;

    public RequestValidationException(List<String> violations) {
        super("Invalid request: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() {
        return violations;
    }
}
