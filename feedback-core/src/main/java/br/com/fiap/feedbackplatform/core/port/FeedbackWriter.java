package br.com.fiap.feedbackplatform.core.port;

import br.com.fiap.feedbackplatform.core.domain.Feedback;

/**
 * Output port for persisting feedback. Kept separate from {@link FeedbackReader} so each function depends
 * only on the capability it needs (and gets only the matching cloud permission).
 */
public interface FeedbackWriter {

    void save(Feedback feedback);
}
