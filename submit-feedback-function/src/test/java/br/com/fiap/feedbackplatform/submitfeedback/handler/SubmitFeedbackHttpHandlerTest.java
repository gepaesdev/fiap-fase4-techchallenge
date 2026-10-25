package br.com.fiap.feedbackplatform.submitfeedback.handler;

import br.com.fiap.feedbackplatform.core.domain.InvalidFeedbackException;
import br.com.fiap.feedbackplatform.core.domain.Urgency;
import br.com.fiap.feedbackplatform.submitfeedback.function.RequestValidationException;
import br.com.fiap.feedbackplatform.submitfeedback.function.SubmitFeedbackFunction;
import br.com.fiap.feedbackplatform.submitfeedback.function.SubmitFeedbackRequest;
import br.com.fiap.feedbackplatform.submitfeedback.function.SubmitFeedbackResponse;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitFeedbackHttpHandlerTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Mock
    private SubmitFeedbackFunction submitFeedbackFunction;

    private SubmitFeedbackHttpHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SubmitFeedbackHttpHandler(submitFeedbackFunction, jsonMapper);
    }

    @Test
    void shouldReturnCreatedWithConfirmationBody() {
        // given
        SubmitFeedbackResponse confirmation =
                new SubmitFeedbackResponse("id-1", Urgency.CRITICAL, Instant.parse("2026-10-20T14:30:00Z"));
        when(submitFeedbackFunction.apply(new SubmitFeedbackRequest("No audio", 2))).thenReturn(confirmation);

        // when
        HttpResponseMessage response = handler.run(
                FakeHttpRequestMessage.withBody("{\"descricao\":\"No audio\",\"nota\":2}"), null);

        // then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeader("Content-Type")).isEqualTo("application/json");
        assertThat(jsonMapper.readValue((String) response.getBody(), SubmitFeedbackResponse.class))
                .isEqualTo(confirmation);
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsMissing() {
        // when
        HttpResponseMessage response = handler.run(FakeHttpRequestMessage.withoutBody(), null);

        // then
        assertBadRequest(response, List.of("request body is required"));
        verifyNoInteractions(submitFeedbackFunction);
    }

    @Test
    void shouldReturnBadRequestWhenBodyIsBlank() {
        // when
        HttpResponseMessage response = handler.run(FakeHttpRequestMessage.withBody("  "), null);

        // then
        assertBadRequest(response, List.of("request body is required"));
    }

    @Test
    void shouldReturnBadRequestWhenJsonIsMalformed() {
        // when
        HttpResponseMessage response = handler.run(FakeHttpRequestMessage.withBody("{\"nota\": \"ten\""), null);

        // then
        assertBadRequest(response,
                List.of("request body must be a JSON object with 'descricao' (string) and 'nota' (integer)"));
        verifyNoInteractions(submitFeedbackFunction);
    }

    @Test
    void shouldReturnBadRequestWithValidationViolations() {
        // given
        when(submitFeedbackFunction.apply(any()))
                .thenThrow(new RequestValidationException(List.of("nota must be between 0 and 10")));

        // when
        HttpResponseMessage response = handler.run(
                FakeHttpRequestMessage.withBody("{\"descricao\":\"ok\",\"nota\":11}"), null);

        // then
        assertBadRequest(response, List.of("nota must be between 0 and 10"));
    }

    @Test
    void shouldReturnBadRequestWhenDomainRuleIsBroken() {
        // given
        when(submitFeedbackFunction.apply(any())).thenThrow(new InvalidFeedbackException("Rating must not be null"));

        // when
        HttpResponseMessage response = handler.run(
                FakeHttpRequestMessage.withBody("{\"descricao\":\"ok\",\"nota\":5}"), null);

        // then
        assertBadRequest(response, List.of("Rating must not be null"));
    }

    @Test
    void shouldPropagateUnexpectedFailuresSoTheHostRecordsThem() {
        // given
        when(submitFeedbackFunction.apply(any())).thenThrow(new IllegalStateException("Cosmos DB unavailable"));

        // when / then
        assertThatThrownBy(() -> handler.run(
                FakeHttpRequestMessage.withBody("{\"descricao\":\"ok\",\"nota\":5}"), null))
                .isInstanceOf(IllegalStateException.class);
    }

    private void assertBadRequest(HttpResponseMessage response, List<String> expectedDetails) {
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(jsonMapper.readValue((String) response.getBody(), ErrorResponse.class))
                .isEqualTo(new ErrorResponse("Invalid request", expectedDetails));
    }
}
