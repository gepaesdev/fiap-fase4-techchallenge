package br.com.fiap.feedbackplatform.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * E-mail delivery settings, bound from app settings ({@code EMAIL_PROVIDER}, {@code ACS_ENDPOINT},
 * {@code EMAIL_SENDER_ADDRESS}, {@code ADMIN_EMAILS}). Recipients are configuration — in Azure they come from
 * Key Vault — and never live in the source code.
 *
 * @param provider        {@link EmailProvider#AZURE} in the cloud, {@link EmailProvider#LOG} locally
 * @param endpoint        Azure Communication Services endpoint, e.g. {@code https://<name>.communication.azure.com}
 * @param senderAddress   verified sender, e.g. {@code DoNotReply@<domain>.azurecomm.net}
 * @param adminRecipients administrator e-mail addresses (comma-separated in the app setting)
 */
@ConfigurationProperties(prefix = "email")
public record EmailProperties(EmailProvider provider, String endpoint, String senderAddress,
                              List<String> adminRecipients) {

    public EmailProperties {
        provider = provider == null ? EmailProvider.AZURE : provider;
        adminRecipients = adminRecipients == null ? List.of() : List.copyOf(adminRecipients);
    }
}
