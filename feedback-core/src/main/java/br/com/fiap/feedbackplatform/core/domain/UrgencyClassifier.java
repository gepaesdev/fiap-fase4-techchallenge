package br.com.fiap.feedbackplatform.core.domain;

/**
 * Single source of truth for the urgency rule: {@code 0–3 → CRITICAL}, {@code 4–6 → MEDIUM}, {@code 7–10 → LOW}.
 */
public final class UrgencyClassifier {

    private static final int CRITICAL_MAX_RATING = 3;
    private static final int MEDIUM_MAX_RATING = 6;

    private UrgencyClassifier() {
    }

    public static Urgency classify(Rating rating) {
        if (rating.value() <= CRITICAL_MAX_RATING) {
            return Urgency.CRITICAL;
        }
        if (rating.value() <= MEDIUM_MAX_RATING) {
            return Urgency.MEDIUM;
        }
        return Urgency.LOW;
    }
}
