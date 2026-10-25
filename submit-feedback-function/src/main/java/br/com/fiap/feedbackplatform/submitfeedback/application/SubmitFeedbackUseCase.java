package br.com.fiap.feedbackplatform.submitfeedback.application;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Rating;
import br.com.fiap.feedbackplatform.core.port.FeedbackWriter;

import java.time.Clock;

/**
 * Registers a student's feedback. Its only job is to create and persist it — notifying administrators about
 * critical feedback happens asynchronously in another function, triggered by the database change feed.
 */
public class SubmitFeedbackUseCase {

    private final FeedbackWriter feedbackWriter;
    private final Clock clock;

    public SubmitFeedbackUseCase(FeedbackWriter feedbackWriter, Clock clock) {
        this.feedbackWriter = feedbackWriter;
        this.clock = clock;
    }

    /**
     * @throws br.com.fiap.feedbackplatform.core.domain.InvalidFeedbackException when the input breaks a domain rule
     */
    public Feedback submit(SubmitFeedbackCommand command) {
        Feedback feedback = Feedback.submit(command.description(), new Rating(command.rating()), clock);
        feedbackWriter.save(feedback);
        return feedback;
    }
}
