package br.com.fiap.feedbackplatform.core.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminNotificationTest {

    @Test
    void shouldCreateNotificationWithAllParts() {
        // when
        AdminNotification notification = new AdminNotification("Subject", "Text", "<p>Html</p>");

        // then
        assertThat(notification.subject()).isEqualTo("Subject");
        assertThat(notification.plainTextBody()).isEqualTo("Text");
        assertThat(notification.htmlBody()).isEqualTo("<p>Html</p>");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectBlankSubject(String subject) {
        // when / then
        assertThatThrownBy(() -> new AdminNotification(subject, "Text", "<p>Html</p>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectBlankPlainTextBody(String body) {
        // when / then
        assertThatThrownBy(() -> new AdminNotification("Subject", body, "<p>Html</p>"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("plainTextBody");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldRejectBlankHtmlBody(String body) {
        // when / then
        assertThatThrownBy(() -> new AdminNotification("Subject", "Text", body))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("htmlBody");
    }
}
