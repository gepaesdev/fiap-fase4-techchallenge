package br.com.fiap.feedbackplatform.weeklyreport.application;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.port.AdminNotifier;
import br.com.fiap.feedbackplatform.core.port.FeedbackReader;
import br.com.fiap.feedbackplatform.weeklyreport.report.ReportWeek;
import br.com.fiap.feedbackplatform.weeklyreport.report.WeeklyReport;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static br.com.fiap.feedbackplatform.core.notification.NotificationFormatting.ADMIN_TIME_ZONE;

/**
 * Produces the weekly report: reads the week's feedback, aggregates it, keeps it in the report history and
 * e-mails it to the administrators.
 *
 * <p>The report is saved before it is e-mailed; if the e-mail fails the invocation fails, and a re-run simply
 * overwrites the same week's report.
 */
public class GenerateWeeklyReportUseCase {

    private final FeedbackReader feedbackReader;
    private final WeeklyReportRepository weeklyReportRepository;
    private final AdminNotifier adminNotifier;
    private final WeeklyReportEmailComposer emailComposer;
    private final Clock clock;
    private final int weekOffset;

    /**
     * @param weekOffset which week to report: {@code 1} = previous full week (production schedule),
     *                   {@code 0} = current week so far (local tests and live demos)
     */
    public GenerateWeeklyReportUseCase(FeedbackReader feedbackReader, WeeklyReportRepository weeklyReportRepository,
                                       AdminNotifier adminNotifier, WeeklyReportEmailComposer emailComposer,
                                       Clock clock, int weekOffset) {
        if (weekOffset < 0) {
            throw new IllegalArgumentException("Report week offset must not be negative, but was " + weekOffset);
        }
        this.feedbackReader = feedbackReader;
        this.weeklyReportRepository = weeklyReportRepository;
        this.adminNotifier = adminNotifier;
        this.emailComposer = emailComposer;
        this.clock = clock;
        this.weekOffset = weekOffset;
    }

    public WeeklyReport generate() {
        LocalDate today = LocalDate.now(clock.withZone(ADMIN_TIME_ZONE));
        ReportWeek week = ReportWeek.weeksBefore(today, weekOffset);

        WeeklyReport report = WeeklyReport.generate(week, findFeedbackOf(week), clock.instant());

        weeklyReportRepository.save(report);
        adminNotifier.notify(emailComposer.compose(report));
        return report;
    }

    private List<Feedback> findFeedbackOf(ReportWeek week) {
        // Storage partitions are UTC dates, which do not align with Brasília days: read the overlapping UTC
        // dates and keep only what falls inside the Brasília week.
        return feedbackReader.findCreatedBetween(week.firstUtcDate(), week.lastUtcDate()).stream()
                .filter(feedback -> week.contains(feedback.createdAt()))
                .toList();
    }
}
