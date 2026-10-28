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

    public static CosmosContainer getFeedbackContainer(CosmosClient client, CosmosProperties properties) {
        return getContainer(client, properties, properties.container(), FeedbackDocument.PARTITION_KEY_PATH);
    }

    /**
     * Returns the container, creating it first when running against the local emulator.
     */
    public static CosmosContainer getContainer(CosmosClient client, CosmosProperties properties,
                                               String containerName, String partitionKeyPath) {
        if (properties.emulator()) {
            return createContainerIfMissing(client, properties.database(), containerName, partitionKeyPath);
        }
        return client.getDatabase(properties.database()).getContainer(containerName);
    }

    private static CosmosContainer createContainerIfMissing(CosmosClient client, String databaseName,
                                                            String containerName, String partitionKeyPath) {
        client.createDatabaseIfNotExists(databaseName);
        CosmosDatabase database = client.getDatabase(databaseName);
        database.createContainerIfNotExists(new CosmosContainerProperties(containerName, partitionKeyPath));
        return database.getContainer(containerName);
    }
}
