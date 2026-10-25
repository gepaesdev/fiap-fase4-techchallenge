package br.com.fiap.feedbackplatform.cosmos;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.port.FeedbackWriter;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;

/**
 * Persists feedback as {@link FeedbackDocument}s. Uses {@code create} (not upsert) so an id collision fails
 * loudly instead of silently overwriting a student's feedback.
 */
public class CosmosFeedbackWriter implements FeedbackWriter {

    private final CosmosContainer container;

    public CosmosFeedbackWriter(CosmosContainer container) {
        this.container = container;
    }

    @Override
    public void save(Feedback feedback) {
        FeedbackDocument document = FeedbackDocument.fromDomain(feedback);
        container.createItem(document, new PartitionKey(document.createdDate()), new CosmosItemRequestOptions());
    }
}
