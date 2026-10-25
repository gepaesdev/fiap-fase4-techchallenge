package br.com.fiap.feedbackplatform.core.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatingTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 5, 10})
    void shouldAcceptValuesWithinRange(int value) {
        // when
        Rating rating = new Rating(value);

        // then
        assertThat(rating.value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 11, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void shouldRejectValuesOutOfRange(int value) {
        // when / then
        assertThatThrownBy(() -> new Rating(value))
                .isInstanceOf(InvalidFeedbackException.class)
                .hasMessageContaining("between 0 and 10");
    }
}
