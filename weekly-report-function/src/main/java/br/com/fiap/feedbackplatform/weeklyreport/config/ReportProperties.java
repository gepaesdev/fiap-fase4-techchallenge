package br.com.fiap.feedbackplatform.weeklyreport.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Weekly report settings, bound from app settings ({@code REPORT_WEEK_OFFSET}).
 *
 * @param weekOffset {@code 1} (default) reports the previous full week; {@code 0} reports the current week so far,
 *                   useful for local tests and live demos
 */
@ConfigurationProperties(prefix = "report")
public record ReportProperties(int weekOffset) {
}
