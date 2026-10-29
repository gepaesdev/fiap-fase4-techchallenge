// Feedback Platform — complete Azure environment (FIAP Pós Tech Phase 4).
// Deploy:  az deployment sub create -l brazilsouth -f infra/main.bicep -p infra/main.bicepparam
targetScope = 'subscription'

@description('Azure region (Azure for Students allows brazilsouth, which also keeps data in Brazil).')
param location string = 'brazilsouth'

@description('Environment name, part of resource names and tags.')
param environmentName string = 'dev'

@secure()
@description('Comma-separated administrator e-mails that receive critical alerts and weekly reports.')
param adminEmails string

@description('E-mail that receives operational alerts and budget notifications.')
param alertEmail string

@description('1 = report the previous full week (production); 0 = current week (demo).')
@allowed([0, 1])
param reportWeekOffset int = 1

@description('Monthly budget in USD.')
param budgetAmount int = 5

@description('First day of the budget period.')
param budgetStartDate string = '${utcNow('yyyy-MM')}-01'

@description('Create the cost budget.')
param enableBudget bool = true

@description('Object ids of team members who get access to the resource group.')
param teamMemberPrincipalIds array = []

@description('Role granted to team members.')
@allowed(['Reader', 'Contributor'])
param teamMemberRole string = 'Reader'

var projectName = 'feedback-platform'
var resourceGroupName = 'rg-${projectName}-${environmentName}'
var suffix = take(uniqueString(subscription().id, resourceGroupName), 8)
var tags = {
  project: projectName
  environment: environmentName
  managedBy: 'bicep'
}

var functions = {
  submit: {
    name: 'func-feedback-submit-${suffix}'
    mainClass: 'br.com.fiap.feedbackplatform.submitfeedback.SubmitFeedbackApplication'
    deploymentContainer: 'deploy-submit-feedback'
  }
  notifier: {
    name: 'func-feedback-notifier-${suffix}'
    mainClass: 'br.com.fiap.feedbackplatform.criticalfeedbacknotifier.CriticalFeedbackNotifierApplication'
    deploymentContainer: 'deploy-critical-feedback-notifier'
  }
  report: {
    name: 'func-feedback-report-${suffix}'
    mainClass: 'br.com.fiap.feedbackplatform.weeklyreport.WeeklyReportApplication'
    deploymentContainer: 'deploy-weekly-report'
  }
}

resource resourceGroup 'Microsoft.Resources/resourceGroups@2024-03-01' = {
  name: resourceGroupName
  location: location
  tags: tags
}

module monitoring 'modules/monitoring.bicep' = {
  scope: resourceGroup
  params: {
    location: location
    tags: tags
    suffix: suffix
  }
}

module storage 'modules/storage.bicep' = {
  scope: resourceGroup
  params: {
    location: location
    tags: tags
    suffix: suffix
    deploymentContainerNames: [
      functions.submit.deploymentContainer
      functions.notifier.deploymentContainer
      functions.report.deploymentContainer
    ]
  }
}

module cosmos 'modules/cosmos.bicep' = {
  scope: resourceGroup
  params: {
    location: location
    tags: tags
    suffix: suffix
  }
}

module communication 'modules/communication.bicep' = {
  scope: resourceGroup
  params: {
    tags: tags
    suffix: suffix
  }
}

module keyVault 'modules/keyvault.bicep' = {
  scope: resourceGroup
  params: {
    location: location
    tags: tags
    suffix: suffix
    adminEmails: adminEmails
  }
}

var cosmosSettings = [
  { name: 'COSMOS_ENDPOINT', value: cosmos.outputs.endpoint }
  { name: 'COSMOS_DATABASE', value: cosmos.outputs.databaseName }
  { name: 'COSMOS_CONTAINER', value: cosmos.outputs.feedbackContainerName }
]

var emailSettings = [
  { name: 'EMAIL_PROVIDER', value: 'azure' }
  { name: 'ACS_ENDPOINT', value: communication.outputs.endpoint }
  { name: 'EMAIL_SENDER_ADDRESS', value: communication.outputs.senderAddress }
  { name: 'ADMIN_EMAILS', value: keyVault.outputs.adminEmailsReference }
]

module submitFunction 'modules/function-app.bicep' = {
  scope: resourceGroup
  params: {
    name: functions.submit.name
    location: location
    tags: tags
    storageAccountName: storage.outputs.name
    storageBlobEndpoint: storage.outputs.blobEndpoint
    deploymentContainerName: functions.submit.deploymentContainer
    appInsightsName: monitoring.outputs.appInsightsName
    mainClass: functions.submit.mainClass
    appSettings: cosmosSettings
  }
}

module notifierFunction 'modules/function-app.bicep' = {
  scope: resourceGroup
  params: {
    name: functions.notifier.name
    location: location
    tags: tags
    storageAccountName: storage.outputs.name
    storageBlobEndpoint: storage.outputs.blobEndpoint
    deploymentContainerName: functions.notifier.deploymentContainer
    appInsightsName: monitoring.outputs.appInsightsName
    mainClass: functions.notifier.mainClass
    appSettings: concat(emailSettings, [
      // Identity-based connection used by the Cosmos DB trigger (change feed).
      { name: 'CosmosDbConnection__accountEndpoint', value: cosmos.outputs.endpoint }
      { name: 'COSMOS_DATABASE', value: cosmos.outputs.databaseName }
      { name: 'COSMOS_CONTAINER', value: cosmos.outputs.feedbackContainerName }
    ])
  }
}

module reportFunction 'modules/function-app.bicep' = {
  scope: resourceGroup
  params: {
    name: functions.report.name
    location: location
    tags: tags
    storageAccountName: storage.outputs.name
    storageBlobEndpoint: storage.outputs.blobEndpoint
    deploymentContainerName: functions.report.deploymentContainer
    appInsightsName: monitoring.outputs.appInsightsName
    mainClass: functions.report.mainClass
    appSettings: concat(cosmosSettings, emailSettings, [
      { name: 'COSMOS_REPORTS_CONTAINER', value: cosmos.outputs.reportsContainerName }
      { name: 'REPORT_WEEK_OFFSET', value: string(reportWeekOffset) }
    ])
  }
}

module access 'modules/access.bicep' = {
  scope: resourceGroup
  params: {
    cosmosAccountName: cosmos.outputs.accountName
    databaseName: cosmos.outputs.databaseName
    feedbackContainerName: cosmos.outputs.feedbackContainerName
    leasesContainerName: cosmos.outputs.leasesContainerName
    reportsContainerName: cosmos.outputs.reportsContainerName
    feedbackWriterRoleId: cosmos.outputs.feedbackWriterRoleId
    metadataReaderRoleId: cosmos.outputs.metadataReaderRoleId
    keyVaultName: keyVault.outputs.name
    adminEmailsSecretName: keyVault.outputs.adminEmailsSecretName
    communicationServiceName: communication.outputs.communicationServiceName
    emailSenderRoleId: communication.outputs.emailSenderRoleId
    submitPrincipalId: submitFunction.outputs.principalId
    notifierPrincipalId: notifierFunction.outputs.principalId
    reportPrincipalId: reportFunction.outputs.principalId
  }
}

module alerts 'modules/alerts.bicep' = {
  scope: resourceGroup
  params: {
    tags: tags
    suffix: suffix
    appInsightsId: monitoring.outputs.appInsightsId
    alertEmail: alertEmail
  }
}

module governance 'modules/governance.bicep' = {
  scope: resourceGroup
  params: {
    budgetAmount: budgetAmount
    budgetStartDate: budgetStartDate
    budgetEmail: alertEmail
    enableBudget: enableBudget
    teamMemberPrincipalIds: teamMemberPrincipalIds
    teamMemberRole: teamMemberRole
  }
}

output resourceGroupName string = resourceGroup.name
output submitFunctionAppName string = submitFunction.outputs.name
output notifierFunctionAppName string = notifierFunction.outputs.name
output reportFunctionAppName string = reportFunction.outputs.name
output submitFeedbackUrl string = 'https://${submitFunction.outputs.defaultHostName}/api/avaliacao'
output reportBaseUrl string = 'https://${reportFunction.outputs.defaultHostName}'
output cosmosEndpoint string = cosmos.outputs.endpoint
output emailSenderAddress string = communication.outputs.senderAddress
