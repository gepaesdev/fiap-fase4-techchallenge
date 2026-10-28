package br.com.fiap.feedbackplatform.weeklyreport.application;

import br.com.fiap.feedbackplatform.core.domain.Feedback;
import br.com.fiap.feedbackplatform.core.domain.Rating;
import br.com.fiap.feedbackplatform.core.port.AdminNotifier;
import br.com.fiap.feedbackplatform.core.port.FeedbackReader;
import br.com.fiap.feedbackplatform.weeklyreport.report.WeeklyReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateWeeklyReportUseCaseTest {

    /** Monday 26/10/2026 08:00 in Brasília — the moment the production schedule fires. */
    private static final Instant SCHEDULE_TIME = Instant.parse("2026-10-26T11:00:00Z");
    private static final Clock CLOCK = Clock.fixed(SCHEDULE_TIME, ZoneOffset.UTC);

    @Mock
    private FeedbackReader feedbackReader;

    @Mock
    private WeeklyReportRepository weeklyReportRepository;

    @Mock
    private AdminNotifier adminNotifier;

    @Test
    void shouldReportThePreviousWeekReadingOverlappingUtcDatesAndKeepingOnlyBrasiliaWeekFeedback() {
        // given
        Feedback beforeWeek = feedback("before", "2026-10-19T02:00:00Z");   // Sunday 18/10 23:00 BRT
        Feedback insideWeek = feedback("inside", "2026-10-21T12:00:00Z");
        Feedback lastMinute = feedback("last", "2026-10-26T02:30:00Z");     // Sunday 25/10 23:30 BRT
        when(feedbackReader.findCreatedBetween(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 26)))
                .thenReturn(List.of(beforeWeek, insideWeek, lastMinute));

        // when
        WeeklyReport report = useCase(1).generate();

        // then
        assertThat(report.week().startDate()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(report.feedback()).containsExactly(insideWeek, lastMinute);
        assertThat(report.generatedAt()).isEqualTo(SCHEDULE_TIME);
    }

    @Test
    void shouldSaveTheReportBeforeEmailingIt() {
        // given
        when(feedbackReader.findCreatedBetween(any(), any())).thenReturn(List.of());

        // when
        WeeklyReport report = useCase(1).generate();

        // then
        InOrder order = inOrder(weeklyReportRepository, adminNotifier);
        order.verify(weeklyReportRepository).save(report);
        order.verify(adminNotifier).notify(any());
    }

    @Test
    void shouldReportTheCurrentWeekWhenOffsetIsZero() {
        // given
        when(feedbackReader.findCreatedBetween(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 2)))
                .thenReturn(List.of());

        // when
        WeeklyReport report = useCase(0).generate();

        // then
        assertThat(report.week().startDate()).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void shouldRejectNegativeOffset() {
        assertThatThrownBy(() -> useCase(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    private GenerateWeeklyReportUseCase useCase(int weekOffset) {
        return new GenerateWeeklyReportUseCase(feedbackReader, weeklyReportRepository, adminNotifier,
                new WeeklyReportEmailComposer(), CLOCK, weekOffset);
    }

    private static Feedback feedback(String id, String createdAt) {
        return new Feedback(id, "Description " + id, new Rating(6), Instant.parse(createdAt));
    }
}
