package br.com.fiap.feedbackplatform.weeklyreport.config;

import br.com.fiap.feedbackplatform.core.port.AdminNotifier;
import br.com.fiap.feedbackplatform.core.port.FeedbackReader;
import br.com.fiap.feedbackplatform.cosmos.CosmosContainerFactory;
import br.com.fiap.feedbackplatform.cosmos.CosmosFeedbackReader;
import br.com.fiap.feedbackplatform.cosmos.CosmosProperties;
import br.com.fiap.feedbackplatform.email.AdminNotifierFactory;
import br.com.fiap.feedbackplatform.email.EmailProperties;
import br.com.fiap.feedbackplatform.weeklyreport.application.GenerateWeeklyReportUseCase;
import br.com.fiap.feedbackplatform.weeklyreport.application.WeeklyReportEmailComposer;
import br.com.fiap.feedbackplatform.weeklyreport.application.WeeklyReportRepository;
import br.com.fiap.feedbackplatform.weeklyreport.infrastructure.CosmosWeeklyReportRepository;
import br.com.fiap.feedbackplatform.weeklyreport.infrastructure.WeeklyReportDocument;
import com.azure.cosmos.CosmosClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the report use case to Cosmos DB (feedback reader + report history) and to the e-mail adapter.
 */
@Configuration
@EnableConfigurationProperties({CosmosProperties.class, EmailProperties.class, ReportProperties.class})
public class WeeklyReportConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean(destroyMethod = "close")
    public CosmosClient cosmosClient(CosmosProperties cosmosProperties) {
        return CosmosContainerFactory.createClient(cosmosProperties);
    }

    @Bean
    public FeedbackReader feedbackReader(CosmosClient cosmosClient, CosmosProperties cosmosProperties) {
        return new CosmosFeedbackReader(CosmosContainerFactory.getFeedbackContainer(cosmosClient, cosmosProperties));
    }

    @Bean
    public WeeklyReportRepository weeklyReportRepository(CosmosClient cosmosClient,
                                                         CosmosProperties cosmosProperties) {
        return new CosmosWeeklyReportRepository(CosmosContainerFactory.getContainer(
                cosmosClient, cosmosProperties, cosmosProperties.reportsContainer(),
                WeeklyReportDocument.PARTITION_KEY_PATH));
    }

    @Bean
    public AdminNotifier adminNotifier(EmailProperties emailProperties) {
        return AdminNotifierFactory.create(emailProperties);
    }

    @Bean
    public WeeklyReportEmailComposer weeklyReportEmailComposer() {
        return new WeeklyReportEmailComposer();
    }

    @Bean
    public GenerateWeeklyReportUseCase generateWeeklyReportUseCase(
            FeedbackReader feedbackReader, WeeklyReportRepository weeklyReportRepository,
            AdminNotifier adminNotifier, WeeklyReportEmailComposer weeklyReportEmailComposer,
            Clock clock, ReportProperties reportProperties) {
        return new GenerateWeeklyReportUseCase(feedbackReader, weeklyReportRepository, adminNotifier,
                weeklyReportEmailComposer, clock, reportProperties.weekOffset());
    }
}
