package br.com.fiap.feedbackplatform.submitfeedback.function;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Rating;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Public API payload of {@code POST /avaliacao}. JSON field names follow the challenge contract
 * ({@code descricao}, {@code nota}); Java names stay in English.
 */
public record SubmitFeedbackRequest(
        @JsonProperty("descricao")
        @NotBlank(message = "descricao must not be blank")
        @Size(max = Feedback.DESCRIPTION_MAX_LENGTH, message = "descricao must have at most {max} characters")
        String description,

        @JsonProperty("nota")
        @NotNull(message = "nota is required")
        @Min(value = Rating.MIN_VALUE, message = "nota must be between 0 and 10")
        @Max(value = Rating.MAX_VALUE, message = "nota must be between 0 and 10")
        Integer rating) {
}
