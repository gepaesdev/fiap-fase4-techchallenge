package br.com.fiap.feedbackplatform.email;

import br.com.fiap.feedbackplatform.core.port.AdminNotifier;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;

/**
 * Creates the {@link AdminNotifier} for the configured provider. In Azure the client authenticates with the
 * function's Managed Identity, so no connection string or access key is needed.
 */
public final class AdminNotifierFactory {

    private AdminNotifierFactory() {
    }

    public static AdminNotifier create(EmailProperties properties) {
        return switch (properties.provider()) {
            case LOG -> new LoggingAdminNotifier(properties.adminRecipients());
            case AZURE -> new AzureEmailAdminNotifier(
                    new EmailClientBuilder()
                            .endpoint(properties.endpoint())
                            .credential(new DefaultAzureCredentialBuilder().build())
                            .buildClient(),
                    properties.senderAddress(),
                    properties.adminRecipients());
        };
    }
}
