package br.com.fiap.feedbackplatform.weeklyreport.report;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportWeekTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);

    @ParameterizedTest
    @ValueSource(strings = {"2026-10-19", "2026-10-22", "2026-10-25"})
    void shouldFindTheMondayOfAnyDayInTheWeek(String date) {
        // when
        ReportWeek week = ReportWeek.containing(LocalDate.parse(date));

        // then
        assertThat(week.startDate()).isEqualTo(MONDAY);
        assertThat(week.endDate()).isEqualTo(LocalDate.of(2026, 10, 25));
    }

    @Test
    void shouldReturnPreviousWeekWhenOffsetIsOne() {
        // given
        LocalDate mondayMorningOfTheSchedule = LocalDate.of(2026, 10, 26);

        // when
        ReportWeek week = ReportWeek.weeksBefore(mondayMorningOfTheSchedule, 1);

        // then
        assertThat(week.startDate()).isEqualTo(MONDAY);
    }

    @Test
    void shouldReturnCurrentWeekWhenOffsetIsZero() {
        // when
        ReportWeek week = ReportWeek.weeksBefore(LocalDate.of(2026, 10, 27), 0);

        // then
        assertThat(week.startDate()).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void shouldRejectNegativeOffset() {
        assertThatThrownBy(() -> ReportWeek.weeksBefore(MONDAY, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectWeekNotStartingOnMonday() {
        assertThatThrownBy(() -> new ReportWeek(LocalDate.of(2026, 10, 20)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldListTheSevenDaysFromMondayToSunday() {
        // when / then
        assertThat(new ReportWeek(MONDAY).days())
                .hasSize(7)
                .startsWith(MONDAY)
                .endsWith(LocalDate.of(2026, 10, 25));
    }

    @Test
    void shouldUseBrasiliaBoundariesInsteadOfUtc() {
        // given
        ReportWeek week = new ReportWeek(MONDAY);

        // then — Monday 00:00 BRT is 03:00 UTC; Sunday 23:59 BRT is already Monday in UTC
        assertThat(week.contains(Instant.parse("2026-10-19T02:59:59Z"))).isFalse();
        assertThat(week.contains(Instant.parse("2026-10-19T03:00:00Z"))).isTrue();
        assertThat(week.contains(Instant.parse("2026-10-26T02:59:59Z"))).isTrue();
        assertThat(week.contains(Instant.parse("2026-10-26T03:00:00Z"))).isFalse();
    }

    @Test
    void shouldCoverEveryUtcStorageDateThatOverlapsTheWeek() {
        // given
        ReportWeek week = new ReportWeek(MONDAY);

        // then
        assertThat(week.firstUtcDate()).isEqualTo(MONDAY);
        assertThat(week.lastUtcDate()).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void shouldResolveTheBrasiliaDayOfAnInstant() {
        // when / then
        assertThat(new ReportWeek(MONDAY).localDateOf(Instant.parse("2026-10-21T01:30:00Z")))
                .isEqualTo(LocalDate.of(2026, 10, 20));
    }
}
