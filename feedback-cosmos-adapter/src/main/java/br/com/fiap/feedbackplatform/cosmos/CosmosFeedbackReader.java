package br.com.fiap.feedbackplatform.cosmos;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.port.FeedbackReader;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Reads feedback for a date range with one single-partition query per day. Because {@code createdDate} is the
 * partition key, each query touches exactly one partition — cheap in Request Units and never a full scan.
 */
public class CosmosFeedbackReader implements FeedbackReader {

    private static final String SELECT_ALL = "SELECT * FROM c";

    private final CosmosContainer container;

    public CosmosFeedbackReader(CosmosContainer container) {
        this.container = container;
    }

    @Override
    public List<Feedback> findCreatedBetween(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date " + endDate + " is before start date " + startDate);
        }
        return startDate.datesUntil(endDate.plusDays(1))
                .flatMap(this::findCreatedOn)
                .sorted(Comparator.comparing(Feedback::createdAt))
                .toList();
    }

    private Stream<Feedback> findCreatedOn(LocalDate date) {
        CosmosQueryRequestOptions options = new CosmosQueryRequestOptions()
                .setPartitionKey(new PartitionKey(date.toString()));
        return container.queryItems(SELECT_ALL, options, FeedbackDocument.class)
                .stream()
                .map(FeedbackDocument::toDomain);
    }
}
