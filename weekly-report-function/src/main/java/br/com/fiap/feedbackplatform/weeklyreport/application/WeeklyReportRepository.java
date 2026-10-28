package br.com.fiap.feedbackplatform.weeklyreport.application;

import br.com.fiap.feedbackplatform.weeklyreport.report.WeeklyReport;

/**
 * Output port for keeping a history of generated reports, one per week.
 */
public interface WeeklyReportRepository {

    /**
     * Stores the report, replacing any previous report of the same week so a re-run (retry or manual trigger)
     * never creates duplicates.
     */
    void save(WeeklyReport report);
}
