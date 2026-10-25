package br.com.fiap.feedbackplatform.core.domain;

/**
 * Score a student gives to a class, always within {@value #MIN_VALUE}..{@value #MAX_VALUE}.
 */
public record Rating(int value) {

    public static final int MIN_VALUE = 0;
    public static final int MAX_VALUE = 10;

    public Rating {
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new InvalidFeedbackException(
                    "Rating must be between %d and %d, but was %d".formatted(MIN_VALUE, MAX_VALUE, value));
        }
    }
}
