package br.com.fiap.feedbackplatform.cosmos;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cosmos DB connection settings, bound from app settings ({@code COSMOS_ENDPOINT}, {@code COSMOS_DATABASE},
 * {@code COSMOS_CONTAINER}, {@code COSMOS_REPORTS_CONTAINER} and — for local development only —
 * {@code COSMOS_KEY} and {@code COSMOS_EMULATOR}).
 *
 * @param endpoint         account endpoint, e.g. {@code https://<account>.documents.azure.com:443/}
 * @param database         database name
 * @param container        feedback container name
 * @param reportsContainer weekly reports container name (defaults to {@value #DEFAULT_REPORTS_CONTAINER})
 * @param key              account key — <b>local emulator only</b>; leave empty in Azure so the Managed Identity
 *                         is used
 * @param emulator         {@code true} only against the local emulator: disables endpoint discovery (the emulator
 *                         advertises an https address even when served over http) and creates the database and
 *                         containers on startup. In Azure they are provisioned by Bicep and the functions have no
 *                         permission to create them.
 */
@ConfigurationProperties(prefix = "cosmos")
public record CosmosProperties(String endpoint, String database, String container, String reportsContainer,
                               String key, boolean emulator) {

    public static final String DEFAULT_REPORTS_CONTAINER = "weekly-reports";

    public CosmosProperties {
        reportsContainer = reportsContainer == null || reportsContainer.isBlank()
                ? DEFAULT_REPORTS_CONTAINER
                : reportsContainer;
    }

    public boolean usesAccountKey() {
        return key != null && !key.isBlank();
    }
}
