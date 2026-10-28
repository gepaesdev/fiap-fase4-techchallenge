package br.com.fiap.feedbackplatform.weeklyreport.report;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static br.com.fiap.feedbackplatform.core.notification.NotificationFormatting.ADMIN_TIME_ZONE;

/**
 * A report week: Monday to Sunday in Brasília time (the administrators' calendar), even though feedback is stored
 * in UTC.
 *
 * @param startDate the Monday that opens the week
 */
public record ReportWeek(LocalDate startDate) {

    private static final int DAYS_IN_WEEK = 7;

    public ReportWeek {
        if (startDate.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("A report week starts on Monday, but got " + startDate);
        }
    }

    public static ReportWeek containing(LocalDate date) {
        return new ReportWeek(date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
    }

    /**
     * @param today       current date in Brasília time
     * @param weeksBefore {@code 0} for the current week, {@code 1} for the previous one, and so on
     */
    public static ReportWeek weeksBefore(LocalDate today, int weeksBefore) {
        if (weeksBefore < 0) {
            throw new IllegalArgumentException("Week offset must not be negative, but was " + weeksBefore);
        }
        return new ReportWeek(containing(today).startDate().minusWeeks(weeksBefore));
    }

    public LocalDate endDate() {
        return startDate.plusDays(DAYS_IN_WEEK - 1L);
    }

    public List<LocalDate> days() {
        return startDate.datesUntil(startDate.plusDays(DAYS_IN_WEEK)).toList();
    }

    public boolean contains(Instant instant) {
        return !instant.isBefore(startInstant()) && instant.isBefore(endInstantExclusive());
    }

    /** Day of the week (Brasília time) on which the instant happened. */
    public LocalDate localDateOf(Instant instant) {
        return LocalDate.ofInstant(instant, ADMIN_TIME_ZONE);
    }

    /** First UTC storage date that may hold feedback of this week. */
    public LocalDate firstUtcDate() {
        return LocalDate.ofInstant(startInstant(), ZoneOffset.UTC);
    }

    /** Last UTC storage date that may hold feedback of this week. */
    public LocalDate lastUtcDate() {
        return LocalDate.ofInstant(endInstantExclusive().minusNanos(1), ZoneOffset.UTC);
    }

    private Instant startInstant() {
        return startDate.atStartOfDay(ADMIN_TIME_ZONE).toInstant();
    }

    private Instant endInstantExclusive() {
        return startDate.plusDays(DAYS_IN_WEEK).atStartOfDay(ADMIN_TIME_ZONE).toInstant();
    }
}
