package br.com.fiap.feedbackplatform.core.notification;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationFormattingTest {

    @Test
    void shouldFormatInstantInBrasiliaTime() {
        // given
        Instant utcInstant = Instant.parse("2026-10-21T01:30:00Z");

        // when
        String formatted = NotificationFormatting.formatDateTime(utcInstant);

        // then
        assertThat(formatted).isEqualTo("20/10/2026 22:30");
    }

    @Test
    void shouldFormatDateInBrazilianPattern() {
        // when
        String formatted = NotificationFormatting.formatDate(LocalDate.of(2026, 10, 5));

        // then
        assertThat(formatted).isEqualTo("05/10/2026");
    }

    @Test
    void shouldEscapeHtmlSpecialCharacters() {
        // given
        String studentText = "<script>alert('x')</script> & \"quotes\"";

        // when
        String escaped = NotificationFormatting.escapeHtml(studentText);

        // then
        assertThat(escaped)
                .isEqualTo("&lt;script&gt;alert(&#39;x&#39;)&lt;/script&gt; &amp; &quot;quotes&quot;");
    }

    @Test
    void shouldKeepPlainTextAndAccentsUnchanged() {
        // when
        String escaped = NotificationFormatting.escapeHtml("Áudio inaudível durante a aula");

        // then
        assertThat(escaped).isEqualTo("Áudio inaudível durante a aula");
    }
}
