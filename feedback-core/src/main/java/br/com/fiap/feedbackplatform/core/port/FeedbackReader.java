package br.com.fiap.feedbackplatform.core.port;

import br.com.fiap.feedbackplatform.core.domain.Feedback;

import java.time.LocalDate;
import java.util.List;

/**
 * Output port for querying stored feedback.
 */
public interface FeedbackReader {

    /**
     * @param startDate first UTC creation date, inclusive
     * @param endDate   last UTC creation date, inclusive
     * @return feedback created in the period, or an empty list when there is none
     */
    List<Feedback> findCreatedBetween(LocalDate startDate, LocalDate endDate);
}
