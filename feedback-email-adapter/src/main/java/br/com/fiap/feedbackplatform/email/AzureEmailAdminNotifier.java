package br.com.fiap.feedbackplatform.email;

import br.com.fiap.feedbackplatform.core.notification.AdminNotification;
import br.com.fiap.feedbackplatform.core.port.AdminNotifier;
import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.core.util.polling.LongRunningOperationStatus;
import com.azure.core.util.polling.PollResponse;

import java.time.Duration;
import java.util.List;

/**
 * Sends administrator notifications through Azure Communication Services Email.
 * Waits for the send operation to finish so a failure surfaces in this invocation (and is retried).
 */
public class AzureEmailAdminNotifier implements AdminNotifier {

    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(60);

    private final EmailClient emailClient;
    private final String senderAddress;
    private final List<EmailAddress> recipients;

    public AzureEmailAdminNotifier(EmailClient emailClient, String senderAddress, List<String> recipientAddresses) {
        if (senderAddress == null || senderAddress.isBlank()) {
            throw new IllegalStateException("E-mail sender address is not configured (EMAIL_SENDER_ADDRESS)");
        }
        if (recipientAddresses.isEmpty()) {
            throw new IllegalStateException("No administrator e-mail configured (ADMIN_EMAILS)");
        }
        this.emailClient = emailClient;
        this.senderAddress = senderAddress;
        this.recipients = recipientAddresses.stream().map(EmailAddress::new).toList();
    }

    @Override
    public void notify(AdminNotification notification) {
        EmailMessage message = new EmailMessage()
                .setSenderAddress(senderAddress)
                .setToRecipients(recipients)
                .setSubject(notification.subject())
                .setBodyPlainText(notification.plainTextBody())
                .setBodyHtml(notification.htmlBody());

        PollResponse<EmailSendResult> response = emailClient.beginSend(message).waitForCompletion(SEND_TIMEOUT);

        if (response.getStatus() != LongRunningOperationStatus.SUCCESSFULLY_COMPLETED) {
            throw new EmailDeliveryException("E-mail delivery did not complete, status: " + response.getStatus());
        }
    }
}
