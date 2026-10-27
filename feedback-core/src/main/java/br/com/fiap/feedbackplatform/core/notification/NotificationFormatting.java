package br.com.fiap.feedbackplatform.core.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Formatting shared by every message sent to administrators. Dates are stored in UTC but shown in Brasília time,
 * the administrators' time zone.
 */
public final class NotificationFormatting {

    public static final ZoneId ADMIN_TIME_ZONE = ZoneId.of("America/Sao_Paulo");

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private NotificationFormatting() {
    }

    public static String formatDateTime(Instant instant) {
        return DATE_TIME_FORMATTER.format(instant.atZone(ADMIN_TIME_ZONE));
    }

    public static String formatDate(LocalDate date) {
        return DATE_FORMATTER.format(date);
    }

    /**
     * Escapes text written by students before it is placed in an HTML e-mail, so it cannot inject markup or links.
     */
    public static String escapeHtml(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (char character : text.toCharArray()) {
            escaped.append(switch (character) {
                case '&' -> "&amp;";
                case '<' -> "&lt;";
                case '>' -> "&gt;";
                case '"' -> "&quot;";
                case '\'' -> "&#39;";
                default -> String.valueOf(character);
            });
        }
        return escaped.toString();
    }
}
