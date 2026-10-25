package br.com.fiap.feedbackplatform.submitfeedback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring entry point. On Azure it is bootstrapped by the Spring Cloud Function adapter (via {@code MAIN_CLASS} /
 * the jar manifest), which then hands Spring-managed handler instances to the Functions runtime.
 */
@SpringBootApplication
public class SubmitFeedbackApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubmitFeedbackApplication.class, args);
    }
}
