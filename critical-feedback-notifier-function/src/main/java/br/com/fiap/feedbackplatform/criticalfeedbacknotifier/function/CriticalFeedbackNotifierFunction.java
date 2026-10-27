package br.com.fiap.feedbackplatform.criticalfeedbacknotifier.function;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.criticalfeedbacknotifier.application.NotifyCriticalFeedbackUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Cloud-agnostic Spring Cloud Function: consumes a batch of newly stored feedback and delegates to the use case.
 */
@Component
public class CriticalFeedbackNotifierFunction implements Consumer<List<Feedback>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CriticalFeedbackNotifierFunction.class);

    private final NotifyCriticalFeedbackUseCase notifyCriticalFeedbackUseCase;

    public CriticalFeedbackNotifierFunction(NotifyCriticalFeedbackUseCase notifyCriticalFeedbackUseCase) {
        this.notifyCriticalFeedbackUseCase = notifyCriticalFeedbackUseCase;
    }

    @Override
    public void accept(List<Feedback> newFeedback) {
        List<Feedback> notified = notifyCriticalFeedbackUseCase.notifyCritical(newFeedback);
        notified.forEach(feedback -> LOGGER.info("CriticalFeedbackNotified id={}", feedback.id()));
        LOGGER.info("Processed {} new feedback, {} critical notified", newFeedback.size(), notified.size());
    }
}
