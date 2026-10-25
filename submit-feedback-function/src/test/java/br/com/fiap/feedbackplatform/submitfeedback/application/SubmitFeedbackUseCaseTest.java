package br.com.fiap.feedbackplatform.submitfeedback.application;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.InvalidFeedbackException;
import br.com.fiap.feedbackplatform.core.domain.Urgency;
import br.com.fiap.feedbackplatform.core.port.FeedbackWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubmitFeedbackUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-20T14:30:00Z");

    @Mock
    private FeedbackWriter feedbackWriter;

    private SubmitFeedbackUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SubmitFeedbackUseCase(feedbackWriter, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldPersistAndReturnTheSubmittedFeedback() {
        // given
        SubmitFeedbackCommand command = new SubmitFeedbackCommand("Could not hear the teacher", 1);

        // when
        Feedback feedback = useCase.submit(command);

        // then
        assertThat(feedback.description()).isEqualTo("Could not hear the teacher");
        assertThat(feedback.urgency()).isEqualTo(Urgency.CRITICAL);
        assertThat(feedback.createdAt()).isEqualTo(NOW);
        verify(feedbackWriter).save(feedback);
    }

    @Test
    void shouldNotPersistWhenFeedbackIsInvalid() {
        // given
        SubmitFeedbackCommand command = new SubmitFeedbackCommand("Fine", 11);

        // when / then
        assertThatThrownBy(() -> useCase.submit(command)).isInstanceOf(InvalidFeedbackException.class);
        verify(feedbackWriter, never()).save(any());
    }
}
