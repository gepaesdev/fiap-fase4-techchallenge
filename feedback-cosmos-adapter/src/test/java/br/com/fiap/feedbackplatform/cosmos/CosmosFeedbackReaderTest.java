package br.com.fiap.feedbackplatform.cosmos;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Rating;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.util.CosmosPagedIterable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CosmosFeedbackReaderTest {

    @Mock
    private CosmosContainer container;

    @InjectMocks
    private CosmosFeedbackReader reader;

    @Test
    void shouldQueryEachDayInItsOwnPartitionAndReturnFeedbackSortedByCreation() {
        // given
        Feedback later = feedback("later", "2026-10-21T10:00:00Z");
        Feedback earlier = feedback("earlier", "2026-10-20T09:00:00Z");
        CosmosPagedIterable<FeedbackDocument> firstDayPage = pageOf(later);
        CosmosPagedIterable<FeedbackDocument> secondDayPage = pageOf(earlier);
        when(container.queryItems(eq("SELECT * FROM c"), any(CosmosQueryRequestOptions.class),
                eq(FeedbackDocument.class)))
                .thenReturn(firstDayPage)
                .thenReturn(secondDayPage);

        // when
        List<Feedback> result = reader.findCreatedBetween(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 21));

        // then
        assertThat(result).containsExactly(earlier, later);
        ArgumentCaptor<CosmosQueryRequestOptions> options = ArgumentCaptor.forClass(CosmosQueryRequestOptions.class);
        verify(container, times(2)).queryItems(eq("SELECT * FROM c"), options.capture(), eq(FeedbackDocument.class));
        assertThat(options.getAllValues())
                .extracting(option -> option.getPartitionKey().toString())
                .containsExactly("[\"2026-10-20\"]", "[\"2026-10-21\"]");
    }

    @Test
    void shouldRejectInvertedRange() {
        // when / then
        assertThatThrownBy(() -> reader.findCreatedBetween(LocalDate.of(2026, 10, 21), LocalDate.of(2026, 10, 20)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @SuppressWarnings("unchecked")
    private static CosmosPagedIterable<FeedbackDocument> pageOf(Feedback... feedback) {
        CosmosPagedIterable<FeedbackDocument> page = mock(CosmosPagedIterable.class);
        when(page.stream()).thenReturn(Stream.of(feedback).map(FeedbackDocument::fromDomain));
        return page;
    }

    private static Feedback feedback(String id, String createdAt) {
        return new Feedback(id, "Description " + id, new Rating(5), Instant.parse(createdAt));
    }
}
