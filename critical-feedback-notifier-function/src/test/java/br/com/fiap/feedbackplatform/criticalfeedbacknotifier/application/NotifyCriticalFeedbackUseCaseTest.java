package br.com.fiap.feedbackplatform.criticalfeedbacknotifier.application;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Rating;
import br.com.fiap.feedbackplatform.core.notification.AdminNotification;
import br.com.fiap.feedbackplatform.core.port.AdminNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotifyCriticalFeedbackUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-25T14:16:00Z");

    @Mock
    private AdminNotifier adminNotifier;

    private NotifyCriticalFeedbackUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new NotifyCriticalFeedbackUseCase(adminNotifier, new CriticalFeedbackEmailComposer());
    }

    @Test
    void shouldNotifyOnlyCriticalFeedback() {
        // given
        Feedback critical = feedback("critical", 3);
        Feedback medium = feedback("medium", 4);
        Feedback low = feedback("low", 10);

        // when
        List<Feedback> notified = useCase.notifyCritical(List.of(critical, medium, low));

        // then
        assertThat(notified).containsExactly(critical);
        ArgumentCaptor<AdminNotification> sent = ArgumentCaptor.forClass(AdminNotification.class);
        verify(adminNotifier, times(1)).notify(sent.capture());
        assertThat(sent.getValue().plainTextBody()).contains("ID da avaliação: critical");
    }

    @Test
    void shouldSendOneMessagePerCriticalFeedback() {
        // when
        List<Feedback> notified = useCase.notifyCritical(List.of(feedback("a", 0), feedback("b", 1)));

        // then
        assertThat(notified).hasSize(2);
        verify(adminNotifier, times(2)).notify(any());
    }

    @Test
    void shouldNotNotifyWhenThereIsNoCriticalFeedback() {
        // when
        List<Feedback> notified = useCase.notifyCritical(List.of(feedback("low", 9)));

        // then
        assertThat(notified).isEmpty();
        verifyNoInteractions(adminNotifier);
    }

    @Test
    void shouldPropagateDeliveryFailureSoTheTriggerRetries() {
        // given
        doThrow(new IllegalStateException("ACS unavailable")).when(adminNotifier).notify(any());

        // when / then
        assertThatThrownBy(() -> useCase.notifyCritical(List.of(feedback("critical", 2))))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Feedback feedback(String id, int rating) {
        return new Feedback(id, "Description " + id, new Rating(rating), NOW);
    }
}
