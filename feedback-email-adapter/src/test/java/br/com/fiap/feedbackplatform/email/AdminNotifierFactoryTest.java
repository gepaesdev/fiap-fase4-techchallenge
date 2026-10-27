package br.com.fiap.feedbackplatform.email;

import br.com.fiap.feedbackplatform.core.notification.AdminNotification;
import br.com.fiap.feedbackplatform.core.port.AdminNotifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AdminNotifierFactoryTest {

    private static final List<String> RECIPIENTS = List.of("admin@example.com");

    @Test
    void shouldUseLogNotifierForLocalDevelopment() {
        // given
        EmailProperties properties = new EmailProperties(EmailProvider.LOG, null, null, RECIPIENTS);

        // when
        AdminNotifier notifier = AdminNotifierFactory.create(properties);

        // then
        assertThat(notifier).isInstanceOf(LoggingAdminNotifier.class);
        assertThatCode(() -> notifier.notify(new AdminNotification("Subject", "Text", "<p>Html</p>")))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldUseAzureNotifierInTheCloud() {
        // given
        EmailProperties properties = new EmailProperties(
                EmailProvider.AZURE, "https://example.communication.azure.com",
                "DoNotReply@example.azurecomm.net", RECIPIENTS);

        // when
        AdminNotifier notifier = AdminNotifierFactory.create(properties);

        // then
        assertThat(notifier).isInstanceOf(AzureEmailAdminNotifier.class);
    }

    @Test
    void shouldDefaultToAzureAndEmptyRecipientsWhenNotConfigured() {
        // when
        EmailProperties properties = new EmailProperties(null, null, null, null);

        // then
        assertThat(properties.provider()).isEqualTo(EmailProvider.AZURE);
        assertThat(properties.adminRecipients()).isEmpty();
    }
}
