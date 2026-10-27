package br.com.fiap.feedbackplatform.criticalfeedbacknotifier.handler;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.InvalidFeedbackException;
import br.com.fiap.feedbackplatform.cosmos.FeedbackDocument;
import br.com.fiap.feedbackplatform.criticalfeedbacknotifier.function.CriticalFeedbackNotifierFunction;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;
import com.microsoft.azure.functions.annotation.ExponentialBackoffRetry;
import com.microsoft.azure.functions.annotation.FunctionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

/**
 * Azure Functions binding to the Cosmos DB change feed of the {@code feedbacks} container. Every document stored
 * by the submit function arrives here asynchronously, so the submit function never knows about notifications.
 *
 * <p>The trigger delivers the batch as one JSON array string (the Java worker cannot bind object documents to
 * {@code String[]}). If sending the e-mail fails, the exception propagates and the exponential back-off retry
 * policy re-runs the batch. Malformed documents are skipped (and logged) so a single bad record cannot block the
 * feed forever.
 */
@Component
public class CriticalFeedbackChangeFeedHandler {

    static final String FUNCTION_NAME = "critical-feedback-notifier";

    private static final Logger LOGGER = LoggerFactory.getLogger(CriticalFeedbackChangeFeedHandler.class);

    private final CriticalFeedbackNotifierFunction criticalFeedbackNotifierFunction;
    private final JsonMapper jsonMapper;

    public CriticalFeedbackChangeFeedHandler(CriticalFeedbackNotifierFunction criticalFeedbackNotifierFunction,
                                             JsonMapper jsonMapper) {
        this.criticalFeedbackNotifierFunction = criticalFeedbackNotifierFunction;
        this.jsonMapper = jsonMapper;
    }

    @FunctionName(FUNCTION_NAME)
    @ExponentialBackoffRetry(maxRetryCount = 5, minimumInterval = "00:00:05", maximumInterval = "00:05:00")
    public void run(
            @CosmosDBTrigger(
                    name = "documents",
                    databaseName = "%COSMOS_DATABASE%",
                    containerName = "%COSMOS_CONTAINER%",
                    leaseContainerName = "leases",
                    createLeaseContainerIfNotExists = true,
                    connection = "CosmosDbConnection")
            String documentsJsonArray,
            ExecutionContext context) {
        List<Feedback> newFeedback = jsonMapper.readTree(documentsJsonArray).valueStream()
                .map(this::toFeedback)
                .flatMap(Optional::stream)
                .toList();
        criticalFeedbackNotifierFunction.accept(newFeedback);
    }

    private Optional<Feedback> toFeedback(JsonNode document) {
        try {
            return Optional.of(jsonMapper.treeToValue(document, FeedbackDocument.class).toDomain());
        } catch (JacksonException | InvalidFeedbackException | IllegalArgumentException exception) {
            LOGGER.warn("Skipping malformed feedback document from the change feed: {}", exception.getMessage());
            return Optional.empty();
        }
    }
}
