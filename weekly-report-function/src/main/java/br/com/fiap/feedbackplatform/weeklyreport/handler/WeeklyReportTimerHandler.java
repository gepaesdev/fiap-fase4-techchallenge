package br.com.fiap.feedbackplatform.weeklyreport.handler;

import br.com.fiap.feedbackplatform.weeklyreport.function.WeeklyReportFunction;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import org.springframework.stereotype.Component;

/**
 * Azure Functions timer binding: generates the weekly report every Monday at 08:00 Brasília time.
 *
 * <p>The schedule is in UTC (Linux plans do not support time zones for timers): 11:00 UTC = 08:00 BRT, since
 * Brazil has no daylight saving time. For demos the function can be triggered on demand through the Functions
 * admin endpoint ({@code POST /admin/functions/weekly-report}).
 */
@Component
public class WeeklyReportTimerHandler {

    static final String FUNCTION_NAME = "weekly-report";

    /** NCRONTAB: second minute hour day month day-of-week → Mondays at 11:00 UTC. */
    static final String EVERY_MONDAY_AT_8_AM_BRASILIA = "0 0 11 * * 1";

    private final WeeklyReportFunction weeklyReportFunction;

    public WeeklyReportTimerHandler(WeeklyReportFunction weeklyReportFunction) {
        this.weeklyReportFunction = weeklyReportFunction;
    }

    @FunctionName(FUNCTION_NAME)
    public void run(
            @TimerTrigger(name = "timer", schedule = EVERY_MONDAY_AT_8_AM_BRASILIA) String timerInfo,
            ExecutionContext context) {
        weeklyReportFunction.get();
    }
}
