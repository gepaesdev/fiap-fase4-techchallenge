package br.com.fiap.feedbackplatform.weeklyreport.function;

import br.com.fiap.feedbackplatform.weeklyreport.application.GenerateWeeklyReportUseCase;
import br.com.fiap.feedbackplatform.weeklyreport.report.WeeklyReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Cloud-agnostic Spring Cloud Function: produces the weekly report on demand (the schedule lives in the handler).
 */
@Component
public class WeeklyReportFunction implements Supplier<WeeklyReport> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeeklyReportFunction.class);

    private final GenerateWeeklyReportUseCase generateWeeklyReportUseCase;

    public WeeklyReportFunction(GenerateWeeklyReportUseCase generateWeeklyReportUseCase) {
        this.generateWeeklyReportUseCase = generateWeeklyReportUseCase;
    }

    @Override
    public WeeklyReport get() {
        WeeklyReport report = generateWeeklyReportUseCase.generate();
        LOGGER.info("WeeklyReportGenerated weekStart={} totalFeedback={} averageRating={}",
                report.week().startDate(), report.totalFeedback(), report.averageRating().orElse(null));
        return report;
    }
}
