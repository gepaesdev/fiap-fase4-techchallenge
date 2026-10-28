package br.com.fiap.feedbackplatform.weeklyreport.infrastructure;

import br.com.fiap.feedbackplatform.weeklyreport.application.WeeklyReportRepository;
import br.com.fiap.feedbackplatform.weeklyreport.report.WeeklyReport;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;

/**
 * Keeps the report history in Cosmos DB. Uses upsert (keyed by week start date) so re-running a week replaces its
 * report instead of duplicating it.
 */
public class CosmosWeeklyReportRepository implements WeeklyReportRepository {

    private final CosmosContainer container;

    public CosmosWeeklyReportRepository(CosmosContainer container) {
        this.container = container;
    }

    @Override
    public void save(WeeklyReport report) {
        WeeklyReportDocument document = WeeklyReportDocument.fromDomain(report);
        container.upsertItem(document, new PartitionKey(document.id()), new CosmosItemRequestOptions());
    }
}
