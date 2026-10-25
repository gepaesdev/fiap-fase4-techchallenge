package br.com.fiap.feedbackplatform.cosmos;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Rating;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CosmosFeedbackWriterTest {

    @Mock
    private CosmosContainer container;

    @InjectMocks
    private CosmosFeedbackWriter writer;

    @Test
    void shouldCreateDocumentInTheSubmissionDatePartition() {
        // given
        Feedback feedback = new Feedback("id-1", "Nice", new Rating(9), Instant.parse("2026-10-20T14:30:00Z"));

        // when
        writer.save(feedback);

        // then
        verify(container).createItem(
                eq(FeedbackDocument.fromDomain(feedback)),
                eq(new PartitionKey("2026-10-20")),
                any(CosmosItemRequestOptions.class));
    }
}
