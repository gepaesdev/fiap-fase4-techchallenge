package br.com.fiap.feedbackplatform.cosmos;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Rating;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FeedbackDocumentTest {

    private static final Feedback CRITICAL_FEEDBACK =
            new Feedback("id-1", "Audio was inaudible", new Rating(2), Instant.parse("2026-10-20T14:30:00Z"));

    @Test
    void shouldMapDomainToDocumentWithPartitionKeyAndDerivedUrgency() {
        // when
        FeedbackDocument document = FeedbackDocument.fromDomain(CRITICAL_FEEDBACK);

        // then
        assertThat(document.id()).isEqualTo("id-1");
        assertThat(document.createdDate()).isEqualTo("2026-10-20");
        assertThat(document.description()).isEqualTo("Audio was inaudible");
        assertThat(document.rating()).isEqualTo(2);
        assertThat(document.urgency()).isEqualTo("CRITICAL");
        assertThat(document.createdAt()).isEqualTo("2026-10-20T14:30:00Z");
    }

    @Test
    void shouldRestoreTheSameDomainFeedback() {
        // given
        FeedbackDocument document = FeedbackDocument.fromDomain(CRITICAL_FEEDBACK);

        // when
        Feedback restored = document.toDomain();

        // then
        assertThat(restored).isEqualTo(CRITICAL_FEEDBACK);
    }
}
