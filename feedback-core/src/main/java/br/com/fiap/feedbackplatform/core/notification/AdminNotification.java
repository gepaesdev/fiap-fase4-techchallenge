package br.com.fiap.feedbackplatform.core.notification;

/**
 * Message addressed to the platform administrators. Carries both a plain-text and an HTML version so every
 * e-mail client can render it.
 *
 * @param subject       e-mail subject
 * @param plainTextBody body for clients that do not render HTML
 * @param htmlBody      body with markup; any user-provided text in it must already be HTML-escaped
 */
public record AdminNotification(String subject, String plainTextBody, String htmlBody) {

    public AdminNotification {
        requireNotBlank(subject, "subject");
        requireNotBlank(plainTextBody, "plainTextBody");
        requireNotBlank(htmlBody, "htmlBody");
    }

    private static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Admin notification " + fieldName + " must not be blank");
        }
    }
}
