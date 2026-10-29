// Storage account required by Azure Functions: host state (AzureWebJobsStorage) and one deployment-package
// container per Function App (Flex Consumption). Shared keys are disabled: access is identity-only.

@description('Azure region.')
param location string

@description('Tags applied to every resource.')
param tags object

@description('Resource name suffix that keeps global names unique.')
param suffix string

@description('Blob containers that receive each Function App deployment package.')
param deploymentContainerNames array

resource storageAccount 'Microsoft.Storage/storageAccounts@2023-05-01' = {
  name: take('stfeedback${suffix}', 24)
  location: location
  tags: tags
  kind: 'StorageV2'
  sku: {
    name: 'Standard_LRS'
  }
  properties: {
    minimumTlsVersion: 'TLS1_2'
    supportsHttpsTrafficOnly: true
    allowBlobPublicAccess: false
    allowSharedKeyAccess: false
    defaultToOAuthAuthentication: true
    encryption: {
      keySource: 'Microsoft.Storage'
      services: {
        blob: {
          enabled: true
        }
      }
    }
  }
}

resource blobService 'Microsoft.Storage/storageAccounts/blobServices@2023-05-01' = {
  parent: storageAccount
  name: 'default'
}

resource deploymentContainers 'Microsoft.Storage/storageAccounts/blobServices/containers@2023-05-01' = [
  for containerName in deploymentContainerNames: {
    parent: blobService
    name: containerName
    properties: {
      publicAccess: 'None'
    }
  }
]

output name string = storageAccount.name
output id string = storageAccount.id
output blobEndpoint string = storageAccount.properties.primaryEndpoints.blob
