package br.com.fiap.feedbackplatform.criticalfeedbacknotifier.config;

import br.com.fiap.feedbackplatform.core.port.AdminNotifier;
import br.com.fiap.feedbackplatform.criticalfeedbacknotifier.application.CriticalFeedbackEmailComposer;
import br.com.fiap.feedbackplatform.criticalfeedbacknotifier.application.NotifyCriticalFeedbackUseCase;
import br.com.fiap.feedbackplatform.email.AdminNotifierFactory;
import br.com.fiap.feedbackplatform.email.EmailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the use case to the e-mail adapter selected by configuration (Azure Communication Services or log).
 * This function reads Cosmos DB only through its trigger binding, so it needs no Cosmos client of its own.
 */
@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class CriticalFeedbackNotifierConfiguration {

    @Bean
    public AdminNotifier adminNotifier(EmailProperties emailProperties) {
        return AdminNotifierFactory.create(emailProperties);
    }

    @Bean
    public CriticalFeedbackEmailComposer criticalFeedbackEmailComposer() {
        return new CriticalFeedbackEmailComposer();
    }

    @Bean
    public NotifyCriticalFeedbackUseCase notifyCriticalFeedbackUseCase(
            AdminNotifier adminNotifier, CriticalFeedbackEmailComposer criticalFeedbackEmailComposer) {
        return new NotifyCriticalFeedbackUseCase(adminNotifier, criticalFeedbackEmailComposer);
    }
}
