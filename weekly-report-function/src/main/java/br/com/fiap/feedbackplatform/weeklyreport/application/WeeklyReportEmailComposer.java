package br.com.fiap.feedbackplatform.weeklyreport.application;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Urgency;
import br.com.fiap.feedbackplatform.core.notification.AdminNotification;
import br.com.fiap.feedbackplatform.weeklyreport.report.WeeklyReport;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static br.com.fiap.feedbackplatform.core.notification.NotificationFormatting.escapeHtml;
import static br.com.fiap.feedbackplatform.core.notification.NotificationFormatting.formatDate;
import static br.com.fiap.feedbackplatform.core.notification.NotificationFormatting.formatDateTime;

/**
 * Builds the weekly report e-mail in Portuguese, the administrators' language.
 */
public class WeeklyReportEmailComposer {

    private static final Locale BRAZILIAN_PORTUGUESE = Locale.of("pt", "BR");
    private static final String NO_AVERAGE = "—";
    private static final String NO_FEEDBACK_MESSAGE = "Nenhuma avaliação recebida nesta semana.";

    public AdminNotification compose(WeeklyReport report) {
        String period = formatDate(report.week().startDate()) + " a " + formatDate(report.week().endDate());
        String subject = "Relatório semanal de avaliações — " + period;
        return new AdminNotification(subject, plainText(report, period), html(report, period));
    }

    private String plainText(WeeklyReport report, String period) {
        StringBuilder text = new StringBuilder()
                .append("Relatório semanal de avaliações — ").append(period).append("\n\n")
                .append("Total de avaliações: ").append(report.totalFeedback()).append('\n')
                .append("Média das notas: ").append(formatAverage(report)).append("\n\n")
                .append("Avaliações por urgência:\n");
        report.countByUrgency().forEach((urgency, count) ->
                text.append("  ").append(urgency).append(": ").append(count).append('\n'));
        text.append("\nAvaliações por dia:\n");
        report.countByDay().forEach((day, count) ->
                text.append("  ").append(dayLabel(day)).append(": ").append(count).append('\n'));
        text.append("\nAvaliações da semana:\n");
        if (report.feedback().isEmpty()) {
            text.append("  ").append(NO_FEEDBACK_MESSAGE).append('\n');
        }
        report.feedback().forEach(feedback -> text.append("  ")
                .append(formatDateTime(feedback.createdAt())).append(" | ")
                .append(feedback.urgency()).append(" | nota ").append(feedback.rating().value()).append(" | ")
                .append(feedback.description()).append('\n'));
        return text.toString();
    }

    private String html(WeeklyReport report, String period) {
        return """
                <h2>Relatório semanal de avaliações</h2>
                <p><strong>Período:</strong> %s (horário de Brasília)</p>
                <p><strong>Total de avaliações:</strong> %d &nbsp;|&nbsp; <strong>Média das notas:</strong> %s</p>
                <h3>Avaliações por urgência</h3>
                %s
                <h3>Avaliações por dia</h3>
                %s
                <h3>Avaliações da semana</h3>
                %s
                """.formatted(period, report.totalFeedback(), formatAverage(report),
                urgencyTable(report.countByUrgency()), dayTable(report.countByDay()), feedbackTable(report));
    }

    private String urgencyTable(Map<Urgency, Long> countByUrgency) {
        String rows = countByUrgency.entrySet().stream()
                .map(entry -> row(entry.getKey().name(), String.valueOf(entry.getValue())))
                .collect(Collectors.joining());
        return table(row("Urgência", "Quantidade"), rows);
    }

    private String dayTable(Map<LocalDate, Long> countByDay) {
        String rows = countByDay.entrySet().stream()
                .map(entry -> row(dayLabel(entry.getKey()), String.valueOf(entry.getValue())))
                .collect(Collectors.joining());
        return table(row("Dia", "Quantidade"), rows);
    }

    private String feedbackTable(WeeklyReport report) {
        if (report.feedback().isEmpty()) {
            return "<p>" + NO_FEEDBACK_MESSAGE + "</p>";
        }
        String rows = report.feedback().stream()
                .map(this::feedbackRow)
                .collect(Collectors.joining());
        return table(row("Data de envio", "Urgência", "Nota", "Descrição"), rows);
    }

    private String feedbackRow(Feedback feedback) {
        return row(formatDateTime(feedback.createdAt()), feedback.urgency().name(),
                String.valueOf(feedback.rating().value()), escapeHtml(feedback.description()));
    }

    private static String table(String header, String rows) {
        return "<table border=\"1\" cellpadding=\"6\" style=\"border-collapse:collapse\">" + header + rows
                + "</table>";
    }

    private static String row(String... cells) {
        return Arrays.stream(cells).map(cell -> "<td>" + cell + "</td>")
                .collect(Collectors.joining("", "<tr>", "</tr>"));
    }

    private static String dayLabel(LocalDate day) {
        return day.getDayOfWeek().getDisplayName(TextStyle.SHORT, BRAZILIAN_PORTUGUESE) + " " + formatDate(day);
    }

    private static String formatAverage(WeeklyReport report) {
        return report.averageRating().map(WeeklyReportEmailComposer::formatDecimal).orElse(NO_AVERAGE);
    }

    private static String formatDecimal(BigDecimal value) {
        return new DecimalFormat("0.0#", DecimalFormatSymbols.getInstance(BRAZILIAN_PORTUGUESE)).format(value);
    }
}
