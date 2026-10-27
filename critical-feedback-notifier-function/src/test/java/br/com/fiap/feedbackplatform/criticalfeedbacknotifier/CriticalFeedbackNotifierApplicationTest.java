package br.com.fiap.feedbackplatform.criticalfeedbacknotifier;

import br.com.fiap.feedbackplatform.core.notification.AdminNotification;
import br.com.fiap.feedbackplatform.core.port.AdminNotifier;
import br.com.fiap.feedbackplatform.criticalfeedbacknotifier.handler.CriticalFeedbackChangeFeedHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Boots the real Spring context (as the Azure adapter does) with only the e-mail boundary mocked, and feeds the
 * handler documents shaped exactly like the Cosmos DB change feed delivers them (including system properties).
 */
@SpringBootTest
class CriticalFeedbackNotifierApplicationTest {

    private static final String CRITICAL_DOCUMENT = """
            {"id":"critical-1","createdDate":"2026-10-25","description":"Áudio inaudível durante a aula",
             "rating":2,"urgency":"CRITICAL","createdAt":"2026-10-25T14:16:00Z",
             "_rid":"abc==","_self":"dbs/x/colls/y/docs/z/","_etag":"\\"0000\\"","_attachments":"attachments/",
             "_ts":1793024160,"_lsn":7}
            """;

    private static final String LOW_DOCUMENT = """
            {"id":"low-1","createdDate":"2026-10-25","description":"Ótima aula","rating":9,"urgency":"LOW",
             "createdAt":"2026-10-25T14:17:00Z","_rid":"def==","_ts":1793024220}
            """;

    @MockitoBean
    private AdminNotifier adminNotifier;

    @Autowired
    private CriticalFeedbackChangeFeedHandler handler;

    @Test
    void shouldEmailAdministratorsOnlyAboutCriticalFeedback() {
        // when
        handler.run(jsonArray(CRITICAL_DOCUMENT, LOW_DOCUMENT), null);

        // then
        ArgumentCaptor<AdminNotification> sent = ArgumentCaptor.forClass(AdminNotification.class);
        verify(adminNotifier, times(1)).notify(sent.capture());
        assertThat(sent.getValue().plainTextBody())
                .contains("Descrição: Áudio inaudível durante a aula")
                .contains("Urgência: CRITICAL")
                .contains("Data de envio: 25/10/2026 11:16");
    }

    @Test
    void shouldSkipMalformedDocumentsAndKeepProcessingTheBatch() {
        // when
        handler.run(jsonArray("\"not an object\"", "{\"id\":\"x\",\"rating\":99}", CRITICAL_DOCUMENT), null);

        // then
        verify(adminNotifier, times(1)).notify(any());
    }

    @Test
    void shouldNotEmailWhenBatchHasNoCriticalFeedback() {
        // when
        handler.run(jsonArray(LOW_DOCUMENT), null);

        // then
        verifyNoInteractions(adminNotifier);
    }

    /** Mirrors the trigger payload: the whole batch as a single JSON array string. */
    private static String jsonArray(String... documents) {
        return "[" + String.join(",", documents) + "]";
    }
}
