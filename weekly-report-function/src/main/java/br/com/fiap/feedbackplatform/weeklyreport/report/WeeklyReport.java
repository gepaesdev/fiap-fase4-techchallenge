package br.com.fiap.feedbackplatform.weeklyreport.report;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Urgency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Weekly summary of feedback, as required by the challenge: every feedback (description, urgency, submission
 * date), the count per day, the count per urgency and the average rating.
 *
 * <p>Every day of the week and every urgency level is always present (with zero when empty), so the report reads
 * the same way whether the week was busy or quiet.
 *
 * @param week           the reported week
 * @param generatedAt    when the report was produced
 * @param feedback       feedback of the week, oldest first
 * @param countByDay     feedback count for each day of the week (Brasília time), Monday first
 * @param countByUrgency feedback count for each urgency level
 */
public record WeeklyReport(
        ReportWeek week,
        Instant generatedAt,
        List<Feedback> feedback,
        Map<LocalDate, Long> countByDay,
        Map<Urgency, Long> countByUrgency) {

    private static final int AVERAGE_SCALE = 2;

    public WeeklyReport {
        feedback = List.copyOf(feedback);
        countByDay = Collections.unmodifiableMap(new LinkedHashMap<>(countByDay));
        countByUrgency = Collections.unmodifiableMap(new EnumMap<>(countByUrgency));
    }

    public static WeeklyReport generate(ReportWeek week, List<Feedback> weekFeedback, Instant generatedAt) {
        List<Feedback> sortedFeedback = weekFeedback.stream()
                .sorted(Comparator.comparing(Feedback::createdAt))
                .toList();
        return new WeeklyReport(week, generatedAt, sortedFeedback,
                countByDay(week, sortedFeedback), countByUrgency(sortedFeedback));
    }

    public int totalFeedback() {
        return feedback.size();
    }

    /**
     * @return average rating rounded to two decimals, or empty when the week had no feedback
     */
    public Optional<BigDecimal> averageRating() {
        if (feedback.isEmpty()) {
            return Optional.empty();
        }
        int ratingSum = feedback.stream().mapToInt(item -> item.rating().value()).sum();
        return Optional.of(BigDecimal.valueOf(ratingSum)
                .divide(BigDecimal.valueOf(feedback.size()), AVERAGE_SCALE, RoundingMode.HALF_UP));
    }

    private static Map<LocalDate, Long> countByDay(ReportWeek week, List<Feedback> feedback) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        week.days().forEach(day -> counts.put(day, 0L));
        feedback.forEach(item -> counts.merge(week.localDateOf(item.createdAt()), 1L, Long::sum));
        return counts;
    }

    private static Map<Urgency, Long> countByUrgency(List<Feedback> feedback) {
        Map<Urgency, Long> counts = new EnumMap<>(Urgency.class);
        for (Urgency urgency : Urgency.values()) {
            counts.put(urgency, 0L);
        }
        feedback.forEach(item -> counts.merge(item.urgency(), 1L, Long::sum));
        return counts;
    }
}
