package br.com.fiap.feedbackplatform.weeklyreport.application;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Rating;
import br.com.fiap.feedbackplatform.core.notification.AdminNotification;
import br.com.fiap.feedbackplatform.weeklyreport.report.ReportWeek;
import br.com.fiap.feedbackplatform.weeklyreport.report.WeeklyReport;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyReportEmailComposerTest {

    private static final ReportWeek WEEK = new ReportWeek(LocalDate.of(2026, 10, 19));
    private static final Instant GENERATED_AT = Instant.parse("2026-10-26T11:00:00Z");

    private final WeeklyReportEmailComposer composer = new WeeklyReportEmailComposer();

    @Test
    void shouldContainEveryItemRequiredByTheChallenge() {
        // given
        WeeklyReport report = WeeklyReport.generate(WEEK, List.of(
                feedback("Áudio inaudível", 2, "2026-10-20T14:30:00Z"),
                feedback("Ótima aula", 9, "2026-10-22T12:00:00Z")), GENERATED_AT);

        // when
        AdminNotification notification = composer.compose(report);

        // then
        assertThat(notification.subject())
                .isEqualTo("Relatório semanal de avaliações — 19/10/2026 a 25/10/2026");
        assertThat(notification.plainTextBody())
                .contains("Total de avaliações: 2")
                .contains("Média das notas: 5,5")
                .contains("CRITICAL: 1")
                .contains("MEDIUM: 0")
                .contains("LOW: 1")
                .contains("ter. 20/10/2026: 1")
                .contains("20/10/2026 11:30 | CRITICAL | nota 2 | Áudio inaudível")
                .contains("22/10/2026 09:00 | LOW | nota 9 | Ótima aula");
        assertThat(notification.htmlBody())
                .contains("<td>CRITICAL</td><td>1</td>")
                .contains("Áudio inaudível");
    }

    @Test
    void shouldStateClearlyWhenTheWeekHadNoFeedback() {
        // given
        WeeklyReport report = WeeklyReport.generate(WEEK, List.of(), GENERATED_AT);

        // when
        AdminNotification notification = composer.compose(report);

        // then
        assertThat(notification.plainTextBody())
                .contains("Total de avaliações: 0")
                .contains("Média das notas: —")
                .contains("Nenhuma avaliação recebida nesta semana.");
        assertThat(notification.htmlBody()).contains("Nenhuma avaliação recebida nesta semana.");
    }

    @Test
    void shouldEscapeStudentTextInHtml() {
        // given
        WeeklyReport report = WeeklyReport.generate(WEEK,
                List.of(feedback("<script>x</script>", 5, "2026-10-20T14:30:00Z")), GENERATED_AT);

        // when
        AdminNotification notification = composer.compose(report);

        // then
        assertThat(notification.htmlBody()).doesNotContain("<script>").contains("&lt;script&gt;");
    }

    private static Feedback feedback(String description, int rating, String createdAt) {
        return new Feedback("id-" + rating, description, new Rating(rating), Instant.parse(createdAt));
    }
}
