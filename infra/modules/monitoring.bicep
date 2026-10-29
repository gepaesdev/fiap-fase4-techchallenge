// Observability backbone: Log Analytics workspace + workspace-based Application Insights.
// A daily ingestion cap keeps the workspace inside the free monthly allowance.

@description('Azure region.')
param location string

@description('Tags applied to every resource.')
param tags object

@description('Resource name suffix that keeps global names unique.')
param suffix string

@description('Daily ingestion cap in GB (5 GB/month are free; 0.15 GB/day stays below it).')
param dailyQuotaGb string = '0.15'

resource workspace 'Microsoft.OperationalInsights/workspaces@2023-09-01' = {
  name: 'log-feedback-${suffix}'
  location: location
  tags: tags
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
    workspaceCapping: {
      dailyQuotaGb: json(dailyQuotaGb)
    }
  }
}

resource appInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: 'appi-feedback-${suffix}'
  location: location
  tags: tags
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: workspace.id
    // Telemetry is accepted only from Microsoft Entra-authenticated senders (the functions' managed identities).
    DisableLocalAuth: true
  }
}

output appInsightsId string = appInsights.id
output appInsightsName string = appInsights.name
output appInsightsConnectionString string = appInsights.properties.ConnectionString
output workspaceId string = workspace.id
