package br.com.fiap.feedbackplatform.weeklyreport;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Rating;
import br.com.fiap.feedbackplatform.core.notification.AdminNotification;
import br.com.fiap.feedbackplatform.core.port.AdminNotifier;
import br.com.fiap.feedbackplatform.core.port.FeedbackReader;
import br.com.fiap.feedbackplatform.weeklyreport.application.WeeklyReportRepository;
import br.com.fiap.feedbackplatform.weeklyreport.handler.WeeklyReportTimerHandler;
import com.azure.cosmos.CosmosClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Boots the real Spring context (as the Azure adapter does) with only the cloud boundaries mocked and fires the
 * timer handler, proving the wiring and configuration work together end to end.
 */
@SpringBootTest(properties = "report.week-offset=0")
class WeeklyReportApplicationTest {

    @MockitoBean
    private CosmosClient cosmosClient;

    @MockitoBean
    private FeedbackReader feedbackReader;

    @MockitoBean
    private WeeklyReportRepository weeklyReportRepository;

    @MockitoBean
    private AdminNotifier adminNotifier;

    @Autowired
    private WeeklyReportTimerHandler handler;

    @Test
    void shouldGenerateStoreAndEmailTheReportWhenTheTimerFires() {
        // given — one feedback created right now, inside the current week (offset 0)
        when(feedbackReader.findCreatedBetween(any(), any())).thenReturn(List.of(
                new Feedback("f-1", "Áudio ruim", new Rating(2), Instant.now())));

        // when
        handler.run("{}", null);

        // then
        verify(weeklyReportRepository).save(any());
        ArgumentCaptor<AdminNotification> sent = ArgumentCaptor.forClass(AdminNotification.class);
        verify(adminNotifier).notify(sent.capture());
        assertThat(sent.getValue().subject()).startsWith("Relatório semanal de avaliações");
        assertThat(sent.getValue().plainTextBody())
                .contains("Total de avaliações: 1")
                .contains("Média das notas: 2,0")
                .contains("CRITICAL: 1");
    }
}
