package br.com.fiap.feedbackplatform.weeklyreport.infrastructure;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Rating;
import br.com.fiap.feedbackplatform.weeklyreport.report.ReportWeek;
import br.com.fiap.feedbackplatform.weeklyreport.report.WeeklyReport;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CosmosWeeklyReportRepositoryTest {

    @Mock
    private CosmosContainer container;

    @InjectMocks
    private CosmosWeeklyReportRepository repository;

    @Test
    void shouldUpsertReportKeyedByWeekStartDate() {
        // given
        WeeklyReport report = WeeklyReport.generate(
                new ReportWeek(LocalDate.of(2026, 10, 19)),
                List.of(new Feedback("f-1", "Áudio ruim", new Rating(2), Instant.parse("2026-10-20T14:30:00Z"))),
                Instant.parse("2026-10-26T11:00:00Z"));

        // when
        repository.save(report);

        // then
        ArgumentCaptor<WeeklyReportDocument> saved = ArgumentCaptor.forClass(WeeklyReportDocument.class);
        verify(container).upsertItem(saved.capture(), eq(new PartitionKey("2026-10-19")),
                any(CosmosItemRequestOptions.class));
        WeeklyReportDocument document = saved.getValue();
        assertThat(document.id()).isEqualTo("2026-10-19");
        assertThat(document.weekEnd()).isEqualTo("2026-10-25");
        assertThat(document.totalFeedback()).isEqualTo(1);
        assertThat(document.averageRating()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(document.countByDay()).containsEntry("2026-10-20", 1L).hasSize(7);
        assertThat(document.countByUrgency()).containsEntry("CRITICAL", 1L).containsEntry("LOW", 0L);
        assertThat(document.feedback()).singleElement()
                .satisfies(entry -> {
                    assertThat(entry.description()).isEqualTo("Áudio ruim");
                    assertThat(entry.urgency()).isEqualTo("CRITICAL");
                    assertThat(entry.createdAt()).isEqualTo("2026-10-20T14:30:00Z");
                });
    }

    @Test
    void shouldStoreNullAverageForAWeekWithoutFeedback() {
        // given
        WeeklyReport report = WeeklyReport.generate(
                new ReportWeek(LocalDate.of(2026, 10, 19)), List.of(), Instant.parse("2026-10-26T11:00:00Z"));

        // when
        WeeklyReportDocument document = WeeklyReportDocument.fromDomain(report);

        // then
        assertThat(document.averageRating()).isNull();
        assertThat(document.feedback()).isEmpty();
    }
}
