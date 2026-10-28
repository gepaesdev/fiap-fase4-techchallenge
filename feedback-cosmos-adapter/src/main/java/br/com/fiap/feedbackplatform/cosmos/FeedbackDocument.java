package br.com.fiap.feedbackplatform.cosmos;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Rating;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Persistence shape of a {@link Feedback} in the Cosmos DB {@code feedbacks} container.
 *
 * <p>{@code urgency} is stored (although derived) so consumers of the change feed and reports can filter and
 * group without re-implementing the rule. {@code createdDate} is the partition key. Cosmos system properties
 * ({@code _rid}, {@code _ts}, {@code _etag}...) are ignored when reading.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FeedbackDocument(
        String id,
        String createdDate,
        String description,
        int rating,
        String urgency,
        String createdAt) {

    public static final String PARTITION_KEY_PATH = "/createdDate";

    public static FeedbackDocument fromDomain(Feedback feedback) {
        return new FeedbackDocument(
                feedback.id(),
                feedback.createdDate().toString(),
                feedback.description(),
                feedback.rating().value(),
                feedback.urgency().name(),
                feedback.createdAt().toString());
    }

    public Feedback toDomain() {
        return new Feedback(id, description, new Rating(rating), Instant.parse(createdAt));
    }
}
