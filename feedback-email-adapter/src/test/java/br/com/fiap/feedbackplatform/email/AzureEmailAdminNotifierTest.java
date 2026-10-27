package br.com.fiap.feedbackplatform.email;

import br.com.fiap.feedbackplatform.core.notification.AdminNotification;
import com.azure.communication.email.EmailClient;
import com.azure.communication.email.models.EmailAddress;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.core.util.polling.LongRunningOperationStatus;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureEmailAdminNotifierTest {

    private static final String SENDER = "DoNotReply@example.azurecomm.net";
    private static final List<String> RECIPIENTS = List.of("admin1@example.com", "admin2@example.com");
    private static final AdminNotification NOTIFICATION =
            new AdminNotification("Subject", "Plain text", "<p>Html</p>");

    @Mock
    private EmailClient emailClient;

    @Mock
    private SyncPoller<EmailSendResult, EmailSendResult> poller;

    @Test
    void shouldSendMessageToEveryAdministrator() {
        // given
        givenSendFinishesWith(LongRunningOperationStatus.SUCCESSFULLY_COMPLETED);
        AzureEmailAdminNotifier notifier = new AzureEmailAdminNotifier(emailClient, SENDER, RECIPIENTS);

        // when
        notifier.notify(NOTIFICATION);

        // then
        ArgumentCaptor<EmailMessage> sent = ArgumentCaptor.forClass(EmailMessage.class);
        verify(emailClient).beginSend(sent.capture());
        EmailMessage message = sent.getValue();
        assertThat(message.getSenderAddress()).isEqualTo(SENDER);
        assertThat(message.getToRecipients()).extracting(EmailAddress::getAddress)
                .containsExactlyElementsOf(RECIPIENTS);
        assertThat(message.getSubject()).isEqualTo("Subject");
        assertThat(message.getBodyPlainText()).isEqualTo("Plain text");
        assertThat(message.getBodyHtml()).isEqualTo("<p>Html</p>");
    }

    @Test
    void shouldFailWhenDeliveryDoesNotComplete() {
        // given
        givenSendFinishesWith(LongRunningOperationStatus.FAILED);
        AzureEmailAdminNotifier notifier = new AzureEmailAdminNotifier(emailClient, SENDER, RECIPIENTS);

        // when / then
        assertThatThrownBy(() -> notifier.notify(NOTIFICATION))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageContaining("FAILED");
    }

    @Test
    void shouldRefuseToStartWithoutRecipients() {
        // when / then
        assertThatThrownBy(() -> new AzureEmailAdminNotifier(emailClient, SENDER, List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_EMAILS");
    }

    @Test
    void shouldRefuseToStartWithoutSender() {
        // when / then
        assertThatThrownBy(() -> new AzureEmailAdminNotifier(emailClient, " ", RECIPIENTS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EMAIL_SENDER_ADDRESS");
    }

    private void givenSendFinishesWith(LongRunningOperationStatus status) {
        when(emailClient.beginSend(any(EmailMessage.class))).thenReturn(poller);
        when(poller.waitForCompletion(any(Duration.class))).thenReturn(new PollResponse<>(status, null));
    }
}
