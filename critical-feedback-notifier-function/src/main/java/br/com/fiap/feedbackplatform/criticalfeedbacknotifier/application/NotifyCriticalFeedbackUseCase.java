package br.com.fiap.feedbackplatform.criticalfeedbacknotifier.application;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.port.AdminNotifier;

import java.util.List;

/**
 * Alerts administrators about critical feedback. Receives every newly stored feedback and ignores the
 * non-critical ones — persisting feedback is not its concern.
 */
public class NotifyCriticalFeedbackUseCase {

    private final AdminNotifier adminNotifier;
    private final CriticalFeedbackEmailComposer emailComposer;

    public NotifyCriticalFeedbackUseCase(AdminNotifier adminNotifier, CriticalFeedbackEmailComposer emailComposer) {
        this.adminNotifier = adminNotifier;
        this.emailComposer = emailComposer;
    }

    /**
     * @return the critical feedback that administrators were notified about
     */
    public List<Feedback> notifyCritical(List<Feedback> newFeedback) {
        List<Feedback> criticalFeedback = newFeedback.stream()
                .filter(Feedback::isCritical)
                .toList();
        criticalFeedback.forEach(feedback -> adminNotifier.notify(emailComposer.compose(feedback)));
        return criticalFeedback;
    }
}
