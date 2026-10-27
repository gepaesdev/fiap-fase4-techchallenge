package br.com.fiap.feedbackplatform.criticalfeedbacknotifier.application;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Rating;
import br.com.fiap.feedbackplatform.core.notification.AdminNotification;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CriticalFeedbackEmailComposerTest {

    private final CriticalFeedbackEmailComposer composer = new CriticalFeedbackEmailComposer();

    @Test
    void shouldContainDescriptionUrgencyAndSubmissionDateRequiredByTheChallenge() {
        // given
        Feedback feedback = new Feedback(
                "id-1", "Áudio inaudível durante a aula", new Rating(2), Instant.parse("2026-10-25T14:16:00Z"));

        // when
        AdminNotification notification = composer.compose(feedback);

        // then
        assertThat(notification.subject()).isEqualTo("[ALERTA] Avaliação crítica recebida");
        assertThat(notification.plainTextBody())
                .contains("Descrição: Áudio inaudível durante a aula")
                .contains("Urgência: CRITICAL")
                .contains("Data de envio: 25/10/2026 11:16 (horário de Brasília)")
                .contains("Nota: 2")
                .contains("ID da avaliação: id-1");
        assertThat(notification.htmlBody())
                .contains("Áudio inaudível durante a aula")
                .contains("<strong>CRITICAL</strong>")
                .contains("25/10/2026 11:16");
    }

    @Test
    void shouldEscapeStudentTextInHtmlBody() {
        // given
        Feedback feedback = new Feedback(
                "id-2", "<a href=\"http://phishing\">clique</a>", new Rating(0), Instant.parse("2026-10-25T14:16:00Z"));

        // when
        AdminNotification notification = composer.compose(feedback);

        // then
        assertThat(notification.htmlBody())
                .doesNotContain("<a href")
                .contains("&lt;a href=&quot;http://phishing&quot;&gt;clique&lt;/a&gt;");
    }
}
