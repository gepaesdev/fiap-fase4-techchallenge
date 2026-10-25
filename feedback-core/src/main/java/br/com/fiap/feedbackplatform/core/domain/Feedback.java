package br.com.fiap.feedbackplatform.core.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * A student's evaluation of a class.
 *
 * <p>Urgency is not stored as input: it is always derived from the rating, so a feedback can never hold an
 * inconsistent pair such as rating 9 with {@link Urgency#CRITICAL}.
 *
 * @param id          unique identifier
 * @param description free-text comment, stripped and never blank
 * @param rating      score from 0 to 10
 * @param createdAt   submission instant (UTC)
 */
public record Feedback(String id, String description, Rating rating, Instant createdAt) {

    public static final int DESCRIPTION_MAX_LENGTH = 2000;

    public Feedback {
        requireNotBlank(id, "Feedback id must not be blank");
        requireNotBlank(description, "Description must not be blank");
        description = description.strip();
        if (description.length() > DESCRIPTION_MAX_LENGTH) {
            throw new InvalidFeedbackException(
                    "Description must have at most %d characters".formatted(DESCRIPTION_MAX_LENGTH));
        }
        if (rating == null) {
            throw new InvalidFeedbackException("Rating must not be null");
        }
        if (createdAt == null) {
            throw new InvalidFeedbackException("Creation instant must not be null");
        }
    }

    /**
     * Creates a brand-new feedback with a generated id, timestamped by the given clock.
     */
    public static Feedback submit(String description, Rating rating, Clock clock) {
        return new Feedback(UUID.randomUUID().toString(), description, rating, clock.instant());
    }

    public Urgency urgency() {
        return UrgencyClassifier.classify(rating);
    }

    public boolean isCritical() {
        return urgency() == Urgency.CRITICAL;
    }

    /**
     * Submission date in UTC; used as the partition key so a week of feedback is read without a full scan.
     */
    public LocalDate createdDate() {
        return LocalDate.ofInstant(createdAt, ZoneOffset.UTC);
    }

    private static void requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidFeedbackException(message);
        }
    }
}
