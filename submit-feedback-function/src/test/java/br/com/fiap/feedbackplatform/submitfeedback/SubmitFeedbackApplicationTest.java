package br.com.fiap.feedbackplatform.submitfeedback;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Urgency;
import br.com.fiap.feedbackplatform.core.port.FeedbackWriter;
import br.com.fiap.feedbackplatform.submitfeedback.handler.FakeHttpRequestMessage;
import br.com.fiap.feedbackplatform.submitfeedback.handler.SubmitFeedbackHttpHandler;
import com.azure.cosmos.CosmosClient;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Boots the real Spring context (as the Azure adapter does) with only the Cosmos DB boundary mocked,
 * proving the wiring, JSON configuration and validation work together end to end.
 */
@SpringBootTest
class SubmitFeedbackApplicationTest {

    @MockitoBean
    private CosmosClient cosmosClient;

    @MockitoBean
    private FeedbackWriter feedbackWriter;

    @Autowired
    private SubmitFeedbackHttpHandler handler;

    @Test
    void shouldPersistCriticalFeedbackAndReturnCreated() {
        // when
        HttpResponseMessage response = handler.run(
                FakeHttpRequestMessage.withBody("{\"descricao\":\"Video kept freezing\",\"nota\":3}"), null);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat((String) response.getBody()).contains("\"urgency\":\"CRITICAL\"");

        ArgumentCaptor<Feedback> saved = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackWriter).save(saved.capture());
        assertThat(saved.getValue().description()).isEqualTo("Video kept freezing");
        assertThat(saved.getValue().urgency()).isEqualTo(Urgency.CRITICAL);
    }

    @Test
    void shouldRejectFractionalRatingInsteadOfTruncatingIt() {
        // when
        HttpResponseMessage response = handler.run(
                FakeHttpRequestMessage.withBody("{\"descricao\":\"Good\",\"nota\":7.5}"), null);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(feedbackWriter);
    }

    @Test
    void shouldRejectRatingAboveTen() {
        // when
        HttpResponseMessage response = handler.run(
                FakeHttpRequestMessage.withBody("{\"descricao\":\"Good\",\"nota\":11}"), null);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat((String) response.getBody()).contains("nota must be between 0 and 10");
        verifyNoInteractions(feedbackWriter);
    }
}
