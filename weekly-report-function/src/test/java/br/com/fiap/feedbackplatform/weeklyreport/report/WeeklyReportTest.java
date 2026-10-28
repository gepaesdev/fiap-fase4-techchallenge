package br.com.fiap.feedbackplatform.weeklyreport.report;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Rating;
import br.com.fiap.feedbackplatform.core.domain.Urgency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class WeeklyReportTest {

    private static final ReportWeek WEEK = new ReportWeek(LocalDate.of(2026, 10, 19));
    private static final Instant GENERATED_AT = Instant.parse("2026-10-26T11:00:00Z");

    @Test
    void shouldCountFeedbackPerBrasiliaDayIncludingEmptyDays() {
        // given — the second one is Tuesday 22:30 in Brasília although already Wednesday in UTC
        List<Feedback> feedback = List.of(
                feedback("a", 9, "2026-10-19T12:00:00Z"),
                feedback("b", 5, "2026-10-21T01:30:00Z"),
                feedback("c", 2, "2026-10-20T15:00:00Z"));

        // when
        WeeklyReport report = WeeklyReport.generate(WEEK, feedback, GENERATED_AT);

        // then
        assertThat(report.countByDay()).containsExactly(
                entry(LocalDate.of(2026, 10, 19), 1L),
                entry(LocalDate.of(2026, 10, 20), 2L),
                entry(LocalDate.of(2026, 10, 21), 0L),
                entry(LocalDate.of(2026, 10, 22), 0L),
                entry(LocalDate.of(2026, 10, 23), 0L),
                entry(LocalDate.of(2026, 10, 24), 0L),
                entry(LocalDate.of(2026, 10, 25), 0L));
    }

    @Test
    void shouldCountFeedbackPerUrgencyIncludingEmptyLevels() {
        // given
        List<Feedback> feedback = List.of(
                feedback("a", 0, "2026-10-19T12:00:00Z"),
                feedback("b", 3, "2026-10-19T13:00:00Z"),
                feedback("c", 10, "2026-10-19T14:00:00Z"));

        // when
        WeeklyReport report = WeeklyReport.generate(WEEK, feedback, GENERATED_AT);

        // then
        assertThat(report.countByUrgency()).containsExactly(
                entry(Urgency.CRITICAL, 2L), entry(Urgency.MEDIUM, 0L), entry(Urgency.LOW, 1L));
    }

    @Test
    void shouldComputeAverageRoundedToTwoDecimals() {
        // given — (10 + 5 + 4) / 3 = 6.333...
        List<Feedback> feedback = List.of(
                feedback("a", 10, "2026-10-19T12:00:00Z"),
                feedback("b", 5, "2026-10-19T13:00:00Z"),
                feedback("c", 4, "2026-10-19T14:00:00Z"));

        // when
        WeeklyReport report = WeeklyReport.generate(WEEK, feedback, GENERATED_AT);

        // then
        assertThat(report.totalFeedback()).isEqualTo(3);
        assertThat(report.averageRating()).contains(new BigDecimal("6.33"));
    }

    @Test
    void shouldListFeedbackOldestFirst() {
        // given
        Feedback newer = feedback("newer", 7, "2026-10-22T12:00:00Z");
        Feedback older = feedback("older", 7, "2026-10-19T12:00:00Z");

        // when
        WeeklyReport report = WeeklyReport.generate(WEEK, List.of(newer, older), GENERATED_AT);

        // then
        assertThat(report.feedback()).containsExactly(older, newer);
    }

    @Test
    void shouldProduceAnEmptyButCompleteReportForAQuietWeek() {
        // when
        WeeklyReport report = WeeklyReport.generate(WEEK, List.of(), GENERATED_AT);

        // then
        assertThat(report.totalFeedback()).isZero();
        assertThat(report.averageRating()).isEmpty();
        assertThat(report.countByDay()).hasSize(7).allSatisfy((day, count) -> assertThat(count).isZero());
        assertThat(report.countByUrgency()).hasSize(3).allSatisfy((urgency, count) -> assertThat(count).isZero());
    }

    private static Feedback feedback(String id, int rating, String createdAt) {
        return new Feedback(id, "Description " + id, new Rating(rating), Instant.parse(createdAt));
    }
}
