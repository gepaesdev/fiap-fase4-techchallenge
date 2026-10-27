package br.com.fiap.feedbackplatform.criticalfeedbacknotifier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring entry point, bootstrapped on Azure by the Spring Cloud Function adapter (via {@code MAIN_CLASS} /
 * the jar manifest).
 */
@SpringBootApplication
public class CriticalFeedbackNotifierApplication {

    public static void main(String[] args) {
        SpringApplication.run(CriticalFeedbackNotifierApplication.class, args);
    }
}
