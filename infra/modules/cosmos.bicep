// Cosmos DB for NoSQL in serverless mode: no provisioned throughput, billed only per request (cents at this
// project's volume), so there is no fixed monthly cost. Key-based access is disabled: every client (the
// functions) must authenticate with Microsoft Entra ID and is limited by data-plane RBAC.

@description('Azure region.')
param location string

@description('Tags applied to every resource.')
param tags object

@description('Resource name suffix that keeps global names unique.')
param suffix string

@description('Database name.')
param databaseName string = 'feedback-platform'

var feedbackContainerName = 'feedbacks'
var leasesContainerName = 'leases'
var reportsContainerName = 'weekly-reports'

resource account 'Microsoft.DocumentDB/databaseAccounts@2024-11-15' = {
  name: 'cosmos-feedback-${suffix}'
  location: location
  tags: tags
  kind: 'GlobalDocumentDB'
  properties: {
    databaseAccountOfferType: 'Standard'
    capabilities: [
      {
        name: 'EnableServerless'
      }
    ]
    disableLocalAuth: true
    disableKeyBasedMetadataWriteAccess: true
    minimalTlsVersion: 'Tls12'
    consistencyPolicy: {
      defaultConsistencyLevel: 'Session'
    }
    locations: [
      {
        locationName: location
        failoverPriority: 0
        isZoneRedundant: false
      }
    ]
    backupPolicy: {
      type: 'Periodic'
      periodicModeProperties: {
        backupIntervalInMinutes: 240
        backupRetentionIntervalInHours: 8
        backupStorageRedundancy: 'Local'
      }
    }
  }
}

resource database 'Microsoft.DocumentDB/databaseAccounts/sqlDatabases@2024-11-15' = {
  parent: account
  name: databaseName
  tags: tags
  properties: {
    resource: {
      id: databaseName
    }
  }
}

resource feedbackContainer 'Microsoft.DocumentDB/databaseAccounts/sqlDatabases/containers@2024-11-15' = {
  parent: database
  name: feedbackContainerName
  tags: tags
  properties: {
    resource: {
      id: feedbackContainerName
      partitionKey: {
        paths: ['/createdDate']
        kind: 'Hash'
      }
    }
  }
}

// Change feed checkpoints of the critical-feedback-notifier trigger.
resource leasesContainer 'Microsoft.DocumentDB/databaseAccounts/sqlDatabases/containers@2024-11-15' = {
  parent: database
  name: leasesContainerName
  tags: tags
  properties: {
    resource: {
      id: leasesContainerName
      partitionKey: {
        paths: ['/id']
        kind: 'Hash'
      }
    }
  }
}

resource reportsContainer 'Microsoft.DocumentDB/databaseAccounts/sqlDatabases/containers@2024-11-15' = {
  parent: database
  name: reportsContainerName
  tags: tags
  properties: {
    resource: {
      id: reportsContainerName
      partitionKey: {
        paths: ['/id']
        kind: 'Hash'
      }
    }
  }
}

// Least-privilege data-plane role for the submit function: it can create feedback documents and nothing else
// (no read, update or delete).
resource feedbackWriterRole 'Microsoft.DocumentDB/databaseAccounts/sqlRoleDefinitions@2024-11-15' = {
  parent: account
  name: guid(account.id, 'feedback-writer')
  properties: {
    roleName: 'Feedback Writer'
    type: 'CustomRole'
    assignableScopes: [
      account.id
    ]
    permissions: [
      {
        dataActions: [
          'Microsoft.DocumentDB/databaseAccounts/readMetadata'
          'Microsoft.DocumentDB/databaseAccounts/sqlDatabases/containers/items/create'
        ]
      }
    ]
  }
}

// Account metadata is read by every SDK client on startup. Granted at account scope on its own so the data
// roles can stay scoped to single containers.
resource metadataReaderRole 'Microsoft.DocumentDB/databaseAccounts/sqlRoleDefinitions@2024-11-15' = {
  parent: account
  name: guid(account.id, 'metadata-reader')
  properties: {
    roleName: 'Metadata Reader'
    type: 'CustomRole'
    assignableScopes: [
      account.id
    ]
    permissions: [
      {
        dataActions: [
          'Microsoft.DocumentDB/databaseAccounts/readMetadata'
        ]
      }
    ]
  }
}

output accountName string = account.name
output accountId string = account.id
output endpoint string = account.properties.documentEndpoint
output databaseName string = database.name
output feedbackContainerName string = feedbackContainer.name
output leasesContainerName string = leasesContainer.name
output reportsContainerName string = reportsContainer.name
output feedbackWriterRoleId string = feedbackWriterRole.id
output metadataReaderRoleId string = metadataReaderRole.id
