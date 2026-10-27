package br.com.fiap.feedbackplatform.email;

/**
 * Thrown when an e-mail could not be delivered. Propagating it makes the function execution fail, which triggers
 * the retry policy and the failure alerts.
 */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message) {
        super(message);
    }

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
