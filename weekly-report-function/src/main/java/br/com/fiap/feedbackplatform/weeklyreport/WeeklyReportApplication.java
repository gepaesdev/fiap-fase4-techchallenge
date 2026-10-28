package br.com.fiap.feedbackplatform.weeklyreport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring entry point, bootstrapped on Azure by the Spring Cloud Function adapter (via {@code MAIN_CLASS} /
 * the jar manifest).
 */
@SpringBootApplication
public class WeeklyReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeeklyReportApplication.class, args);
    }
}
