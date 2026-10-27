package br.com.fiap.feedbackplatform.criticalfeedbacknotifier.application;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.notification.AdminNotification;

import static br.com.fiap.feedbackplatform.core.notification.NotificationFormatting.escapeHtml;
import static br.com.fiap.feedbackplatform.core.notification.NotificationFormatting.formatDateTime;

/**
 * Builds the urgent-feedback e-mail. Content follows the challenge statement: description, urgency and
 * submission date. Text is in Portuguese because it is read by the (Brazilian) administrators.
 */
public class CriticalFeedbackEmailComposer {

    static final String SUBJECT = "[ALERTA] Avaliação crítica recebida";

    private static final String PLAIN_TEXT_TEMPLATE = """
            Uma avaliação crítica foi recebida e requer atenção.

            Descrição: %s
            Urgência: %s
            Nota: %d
            Data de envio: %s (horário de Brasília)
            ID da avaliação: %s
            """;

    private static final String HTML_TEMPLATE = """
            <h2 style="color:#b42318">Avaliação crítica recebida</h2>
            <p>Uma avaliação crítica foi recebida e requer atenção.</p>
            <table cellpadding="6" style="border-collapse:collapse">
              <tr><th align="left">Descrição</th><td>%s</td></tr>
              <tr><th align="left">Urgência</th><td><strong>%s</strong></td></tr>
              <tr><th align="left">Nota</th><td>%d</td></tr>
              <tr><th align="left">Data de envio</th><td>%s (horário de Brasília)</td></tr>
              <tr><th align="left">ID da avaliação</th><td>%s</td></tr>
            </table>
            """;

    public AdminNotification compose(Feedback feedback) {
        String submissionDate = formatDateTime(feedback.createdAt());
        String urgency = feedback.urgency().name();
        int rating = feedback.rating().value();

        String plainText = PLAIN_TEXT_TEMPLATE.formatted(
                feedback.description(), urgency, rating, submissionDate, feedback.id());
        String html = HTML_TEMPLATE.formatted(
                escapeHtml(feedback.description()), urgency, rating, submissionDate, feedback.id());

        return new AdminNotification(SUBJECT, plainText, html);
    }
}
