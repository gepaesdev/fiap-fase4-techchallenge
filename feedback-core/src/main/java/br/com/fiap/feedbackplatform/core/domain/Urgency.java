package br.com.fiap.feedbackplatform.core.domain;

/**
 * How urgently administrators should look at a feedback. Derived from the rating by {@link UrgencyClassifier}.
 */
public enum Urgency {
    CRITICAL,
    MEDIUM,
    LOW
}
