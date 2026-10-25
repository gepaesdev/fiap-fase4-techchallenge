package br.com.fiap.feedbackplatform.cosmos;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.identity.DefaultAzureCredentialBuilder;

/**
 * Builds the Cosmos DB client. In Azure it authenticates with the function's Managed Identity (no secrets);
 * an account key is accepted only to reach the local emulator.
 *
 * <p>Gateway mode is used on purpose: it keeps the number of open connections low, which Microsoft recommends
 * for serverless hosting plans, and it is the only mode supported by the local (vNext) emulator.
 */
public final class CosmosContainerFactory {

    private CosmosContainerFactory() {
    }

    public static CosmosClient createClient(CosmosProperties properties) {
        CosmosClientBuilder builder = new CosmosClientBuilder()
                .endpoint(properties.endpoint())
                .gatewayMode()
                .endpointDiscoveryEnabled(!properties.emulator())
                .contentResponseOnWriteEnabled(false);

        if (properties.usesAccountKey()) {
            return builder.key(properties.key()).buildClient();
        }
        return builder.credential(new DefaultAzureCredentialBuilder().build()).buildClient();
    }

    public static CosmosContainer getContainer(CosmosClient client, CosmosProperties properties) {
        if (properties.emulator()) {
            return createContainerIfMissing(client, properties);
        }
        return client.getDatabase(properties.database()).getContainer(properties.container());
    }

    private static CosmosContainer createContainerIfMissing(CosmosClient client, CosmosProperties properties) {
        client.createDatabaseIfNotExists(properties.database());
        CosmosDatabase database = client.getDatabase(properties.database());
        database.createContainerIfNotExists(
                new CosmosContainerProperties(properties.container(), FeedbackDocument.PARTITION_KEY_PATH));
        return database.getContainer(properties.container());
    }
}
