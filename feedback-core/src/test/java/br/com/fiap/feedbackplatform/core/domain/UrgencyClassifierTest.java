package br.com.fiap.feedbackplatform.core.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class UrgencyClassifierTest {

    @ParameterizedTest(name = "rating {0} -> {1}")
    @CsvSource({
            "0, CRITICAL",
            "3, CRITICAL",
            "4, MEDIUM",
            "6, MEDIUM",
            "7, LOW",
            "10, LOW"
    })
    void shouldClassifyUrgencyAtEveryBoundary(int ratingValue, Urgency expectedUrgency) {
        // given
        Rating rating = new Rating(ratingValue);

        // when
        Urgency urgency = UrgencyClassifier.classify(rating);

        // then
        assertThat(urgency).isEqualTo(expectedUrgency);
    }
}
