package br.com.fiap.feedbackplatform.submitfeedback.function;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Rating;
import br.com.fiap.feedbackplatform.core.domain.Urgency;
import br.com.fiap.feedbackplatform.submitfeedback.application.SubmitFeedbackCommand;
import br.com.fiap.feedbackplatform.submitfeedback.application.SubmitFeedbackUseCase;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitFeedbackFunctionTest {

    private static ValidatorFactory validatorFactory;

    @Mock
    private SubmitFeedbackUseCase submitFeedbackUseCase;

    private SubmitFeedbackFunction function;

    @BeforeAll
    static void createValidatorFactory() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        Validator validator = validatorFactory.getValidator();
        function = new SubmitFeedbackFunction(submitFeedbackUseCase, validator);
    }

    @Test
    void shouldSubmitValidRequestAndReturnConfirmation() {
        // given
        Instant createdAt = Instant.parse("2026-10-20T14:30:00Z");
        Feedback stored = new Feedback("id-1", "Great class", new Rating(9), createdAt);
        when(submitFeedbackUseCase.submit(new SubmitFeedbackCommand("Great class", 9))).thenReturn(stored);

        // when
        SubmitFeedbackResponse response = function.apply(new SubmitFeedbackRequest("Great class", 9));

        // then
        assertThat(response).isEqualTo(new SubmitFeedbackResponse("id-1", Urgency.LOW, createdAt));
    }

    @Test
    void shouldReportEveryViolationUsingApiFieldNames() {
        // given
        SubmitFeedbackRequest request = new SubmitFeedbackRequest(" ", null);

        // when / then
        assertThatThrownBy(() -> function.apply(request))
                .isInstanceOfSatisfying(RequestValidationException.class, exception ->
                        assertThat(exception.violations())
                                .containsExactly("descricao must not be blank", "nota is required"));
        verifyNoInteractions(submitFeedbackUseCase);
    }

    @Test
    void shouldRejectRatingOutOfRange() {
        // given
        SubmitFeedbackRequest request = new SubmitFeedbackRequest("Too long", 11);

        // when / then
        assertThatThrownBy(() -> function.apply(request))
                .isInstanceOfSatisfying(RequestValidationException.class, exception ->
                        assertThat(exception.violations()).containsExactly("nota must be between 0 and 10"));
    }

    @Test
    void shouldRejectDescriptionLongerThanAllowed() {
        // given
        SubmitFeedbackRequest request =
                new SubmitFeedbackRequest("a".repeat(Feedback.DESCRIPTION_MAX_LENGTH + 1), 5);

        // when / then
        assertThatThrownBy(() -> function.apply(request))
                .isInstanceOfSatisfying(RequestValidationException.class, exception ->
                        assertThat(exception.violations())
                                .containsExactly("descricao must have at most 2000 characters"));
    }

    @Test
    void shouldRejectMissingRequest() {
        // when / then
        assertThatThrownBy(() -> function.apply(null))
                .isInstanceOfSatisfying(RequestValidationException.class, exception ->
                        assertThat(exception.violations()).containsExactly("request body is required"));
        verifyNoInteractions(submitFeedbackUseCase);
    }

    @Test
    void shouldRejectNegativeRatingWithoutCallingUseCase() {
        // when / then
        assertThatThrownBy(() -> function.apply(new SubmitFeedbackRequest("ok", -1)))
                .isInstanceOfSatisfying(RequestValidationException.class, exception ->
                        assertThat(exception.violations()).containsExactly("nota must be between 0 and 10"));
        verifyNoInteractions(submitFeedbackUseCase);
    }
}
