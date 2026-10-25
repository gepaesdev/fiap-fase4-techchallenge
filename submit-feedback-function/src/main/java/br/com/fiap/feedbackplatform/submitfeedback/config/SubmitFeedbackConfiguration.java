package br.com.fiap.feedbackplatform.submitfeedback.config;

import br.com.fiap.feedbackplatform.core.port.FeedbackWriter;
import br.com.fiap.feedbackplatform.cosmos.CosmosContainerFactory;
import br.com.fiap.feedbackplatform.cosmos.CosmosFeedbackWriter;
import br.com.fiap.feedbackplatform.cosmos.CosmosProperties;
import br.com.fiap.feedbackplatform.submitfeedback.application.SubmitFeedbackUseCase;
import com.azure.cosmos.CosmosClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the use case to its Cosmos DB adapter. The client is created once per function instance and reused
 * across invocations, as recommended for serverless workloads.
 */
@Configuration
@EnableConfigurationProperties(CosmosProperties.class)
public class SubmitFeedbackConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean(destroyMethod = "close")
    public CosmosClient cosmosClient(CosmosProperties cosmosProperties) {
        return CosmosContainerFactory.createClient(cosmosProperties);
    }

    @Bean
    public FeedbackWriter feedbackWriter(CosmosClient cosmosClient, CosmosProperties cosmosProperties) {
        return new CosmosFeedbackWriter(CosmosContainerFactory.getContainer(cosmosClient, cosmosProperties));
    }

    @Bean
    public SubmitFeedbackUseCase submitFeedbackUseCase(FeedbackWriter feedbackWriter, Clock clock) {
        return new SubmitFeedbackUseCase(feedbackWriter, clock);
    }
}
