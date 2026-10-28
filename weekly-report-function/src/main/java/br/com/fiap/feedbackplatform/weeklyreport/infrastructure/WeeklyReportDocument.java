package br.com.fiap.feedbackplatform.weeklyreport.infrastructure;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.weeklyreport.report.WeeklyReport;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persistence shape of a {@link WeeklyReport} in the Cosmos DB {@code weekly-reports} container.
 * The id is the week's start date (yyyy-MM-dd), so any past week can be fetched directly by date.
 *
 * @param averageRating {@code null} when the week had no feedback
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WeeklyReportDocument(
        String id,
        String weekStart,
        String weekEnd,
        String generatedAt,
        int totalFeedback,
        BigDecimal averageRating,
        Map<String, Long> countByDay,
        Map<String, Long> countByUrgency,
        List<FeedbackEntry> feedback) {

    public static final String PARTITION_KEY_PATH = "/id";

    /**
     * One feedback of the week, with the fields the challenge requires in the report.
     */
    public record FeedbackEntry(String id, String description, String urgency, int rating, String createdAt) {

        static FeedbackEntry from(Feedback feedback) {
            return new FeedbackEntry(feedback.id(), feedback.description(), feedback.urgency().name(),
                    feedback.rating().value(), feedback.createdAt().toString());
        }
    }

    public static WeeklyReportDocument fromDomain(WeeklyReport report) {
        Map<String, Long> countByDay = new LinkedHashMap<>();
        report.countByDay().forEach((day, count) -> countByDay.put(day.toString(), count));
        Map<String, Long> countByUrgency = new LinkedHashMap<>();
        report.countByUrgency().forEach((urgency, count) -> countByUrgency.put(urgency.name(), count));

        String weekStart = report.week().startDate().toString();
        return new WeeklyReportDocument(
                weekStart,
                weekStart,
                report.week().endDate().toString(),
                report.generatedAt().toString(),
                report.totalFeedback(),
                report.averageRating().orElse(null),
                countByDay,
                countByUrgency,
                report.feedback().stream().map(FeedbackEntry::from).toList());
    }
}
