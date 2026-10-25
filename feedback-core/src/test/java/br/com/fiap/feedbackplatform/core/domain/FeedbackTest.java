package br.com.fiap.feedbackplatform.core.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedbackTest {

    private static final Instant SUBMISSION_INSTANT = Instant.parse("2026-10-20T14:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(SUBMISSION_INSTANT, ZoneOffset.UTC);
    private static final String VALID_DESCRIPTION = "Great class, very clear examples";

    @Test
    void shouldSubmitFeedbackWithGeneratedIdAndClockInstant() {
        // when
        Feedback feedback = Feedback.submit(VALID_DESCRIPTION, new Rating(8), FIXED_CLOCK);

        // then
        assertThat(feedback.id()).isNotBlank();
        assertThat(feedback.description()).isEqualTo(VALID_DESCRIPTION);
        assertThat(feedback.rating().value()).isEqualTo(8);
        assertThat(feedback.createdAt()).isEqualTo(SUBMISSION_INSTANT);
    }

    @Test
    void shouldGenerateDistinctIdsForEachSubmission() {
        // when
        Feedback first = Feedback.submit(VALID_DESCRIPTION, new Rating(8), FIXED_CLOCK);
        Feedback second = Feedback.submit(VALID_DESCRIPTION, new Rating(8), FIXED_CLOCK);

        // then
        assertThat(first.id()).isNotEqualTo(second.id());
    }

    @Test
    void shouldStripDescription() {
        // when
        Feedback feedback = Feedback.submit("  Too fast  ", new Rating(5), FIXED_CLOCK);

        // then
        assertThat(feedback.description()).isEqualTo("Too fast");
    }

    @Test
    void shouldDeriveCriticalUrgencyFromLowRating() {
        // when
        Feedback feedback = Feedback.submit(VALID_DESCRIPTION, new Rating(2), FIXED_CLOCK);

        // then
        assertThat(feedback.urgency()).isEqualTo(Urgency.CRITICAL);
        assertThat(feedback.isCritical()).isTrue();
    }

    @Test
    void shouldNotBeCriticalWhenRatingIsHigh() {
        // when
        Feedback feedback = Feedback.submit(VALID_DESCRIPTION, new Rating(9), FIXED_CLOCK);

        // then
        assertThat(feedback.urgency()).isEqualTo(Urgency.LOW);
        assertThat(feedback.isCritical()).isFalse();
    }

    @Test
    void shouldExposeCreatedDateInUtc() {
        // given
        Instant lateNightInBrazilButNextDayInUtc = Instant.parse("2026-10-21T01:00:00Z");

        // when
        Feedback feedback = new Feedback("id-1", VALID_DESCRIPTION, new Rating(7), lateNightInBrazilButNextDayInUtc);

        // then
        assertThat(feedback.createdDate()).isEqualTo(LocalDate.of(2026, 10, 21));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void shouldRejectBlankDescription(String description) {
        // when / then
        assertThatThrownBy(() -> Feedback.submit(description, new Rating(5), FIXED_CLOCK))
                .isInstanceOf(InvalidFeedbackException.class)
                .hasMessage("Description must not be blank");
    }

    @Test
    void shouldAcceptDescriptionAtMaxLength() {
        // given
        String description = "a".repeat(Feedback.DESCRIPTION_MAX_LENGTH);

        // when
        Feedback feedback = Feedback.submit(description, new Rating(5), FIXED_CLOCK);

        // then
        assertThat(feedback.description()).hasSize(Feedback.DESCRIPTION_MAX_LENGTH);
    }

    @Test
    void shouldRejectDescriptionLongerThanMaxLength() {
        // given
        String description = "a".repeat(Feedback.DESCRIPTION_MAX_LENGTH + 1);

        // when / then
        assertThatThrownBy(() -> Feedback.submit(description, new Rating(5), FIXED_CLOCK))
                .isInstanceOf(InvalidFeedbackException.class)
                .hasMessageContaining("at most 2000 characters");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectBlankId(String id) {
        // when / then
        assertThatThrownBy(() -> new Feedback(id, VALID_DESCRIPTION, new Rating(5), SUBMISSION_INSTANT))
                .isInstanceOf(InvalidFeedbackException.class)
                .hasMessage("Feedback id must not be blank");
    }

    @Test
    void shouldRejectNullRating() {
        // when / then
        assertThatThrownBy(() -> new Feedback("id-1", VALID_DESCRIPTION, null, SUBMISSION_INSTANT))
                .isInstanceOf(InvalidFeedbackException.class)
                .hasMessage("Rating must not be null");
    }

    @Test
    void shouldRejectNullCreationInstant() {
        // when / then
        assertThatThrownBy(() -> new Feedback("id-1", VALID_DESCRIPTION, new Rating(5), null))
                .isInstanceOf(InvalidFeedbackException.class)
                .hasMessage("Creation instant must not be null");
    }
}
