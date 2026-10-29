// Least-privilege access for each function's managed identity. Each function gets only what its single
// responsibility needs:
//
// | Function                    | Cosmos DB                                      | Key Vault        | E-mail |
// |-----------------------------|------------------------------------------------|------------------|--------|
// | submit-feedback             | create items in `feedbacks`                    | -                | -      |
// | critical-feedback-notifier  | read `feedbacks` (change feed), write `leases` | read admin list  | send   |
// | weekly-report               | read `feedbacks`, write `weekly-reports`       | read admin list  | send   |

@description('Cosmos DB account name.')
param cosmosAccountName string

@description('Cosmos DB database name.')
param databaseName string

param feedbackContainerName string
param leasesContainerName string
param reportsContainerName string

@description('Custom "Feedback Writer" data-plane role id.')
param feedbackWriterRoleId string

@description('Custom "Metadata Reader" data-plane role id.')
param metadataReaderRoleId string

param keyVaultName string
param adminEmailsSecretName string

param communicationServiceName string

@description('Custom "Email Sender" role id.')
param emailSenderRoleId string

param submitPrincipalId string
param notifierPrincipalId string
param reportPrincipalId string

var cosmosBuiltInDataReaderRole = '00000000-0000-0000-0000-000000000001'
var cosmosBuiltInDataContributorRole = '00000000-0000-0000-0000-000000000002'
var keyVaultSecretsUserRoleId = '4633458b-17de-408a-b874-0445c86b69e6'

resource cosmosAccount 'Microsoft.DocumentDB/databaseAccounts@2024-11-15' existing = {
  name: cosmosAccountName
}

resource vault 'Microsoft.KeyVault/vaults@2023-07-01' existing = {
  name: keyVaultName
}

resource adminEmailsSecret 'Microsoft.KeyVault/vaults/secrets@2023-07-01' existing = {
  parent: vault
  name: adminEmailsSecretName
}

resource communicationService 'Microsoft.Communication/communicationServices@2025-05-01' existing = {
  name: communicationServiceName
}

var containerScope = '${cosmosAccount.id}/dbs/${databaseName}/colls'
var builtInRole = '${cosmosAccount.id}/sqlRoleDefinitions'

var cosmosAssignments = [
  { principalId: submitPrincipalId, roleId: metadataReaderRoleId, scope: cosmosAccount.id }
  { principalId: submitPrincipalId, roleId: feedbackWriterRoleId, scope: '${containerScope}/${feedbackContainerName}' }
  { principalId: notifierPrincipalId, roleId: metadataReaderRoleId, scope: cosmosAccount.id }
  { principalId: notifierPrincipalId, roleId: '${builtInRole}/${cosmosBuiltInDataReaderRole}', scope: '${containerScope}/${feedbackContainerName}' }
  { principalId: notifierPrincipalId, roleId: '${builtInRole}/${cosmosBuiltInDataContributorRole}', scope: '${containerScope}/${leasesContainerName}' }
  { principalId: reportPrincipalId, roleId: metadataReaderRoleId, scope: cosmosAccount.id }
  { principalId: reportPrincipalId, roleId: '${builtInRole}/${cosmosBuiltInDataReaderRole}', scope: '${containerScope}/${feedbackContainerName}' }
  { principalId: reportPrincipalId, roleId: '${builtInRole}/${cosmosBuiltInDataContributorRole}', scope: '${containerScope}/${reportsContainerName}' }
]

resource cosmosRoleAssignments 'Microsoft.DocumentDB/databaseAccounts/sqlRoleAssignments@2024-11-15' = [
  for assignment in cosmosAssignments: {
    parent: cosmosAccount
    name: guid(assignment.principalId, assignment.roleId, assignment.scope)
    properties: {
      principalId: assignment.principalId
      roleDefinitionId: assignment.roleId
      scope: assignment.scope
    }
  }
]

var emailSenderPrincipalIds = [
  notifierPrincipalId
  reportPrincipalId
]

resource secretReaders 'Microsoft.Authorization/roleAssignments@2022-04-01' = [
  for principalId in emailSenderPrincipalIds: {
    name: guid(adminEmailsSecret.id, principalId, keyVaultSecretsUserRoleId)
    scope: adminEmailsSecret
    properties: {
      roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', keyVaultSecretsUserRoleId)
      principalId: principalId
      principalType: 'ServicePrincipal'
    }
  }
]

resource emailSenders 'Microsoft.Authorization/roleAssignments@2022-04-01' = [
  for principalId in emailSenderPrincipalIds: {
    name: guid(communicationService.id, principalId, emailSenderRoleId)
    scope: communicationService
    properties: {
      roleDefinitionId: emailSenderRoleId
      principalId: principalId
      principalType: 'ServicePrincipal'
    }
  }
]
