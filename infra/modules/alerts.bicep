// Monitoring: alert rules that e-mail the operators when a function fails, plus a workbook (dashboard) with
// executions, failures and business events (feedback by urgency, critical alerts, weekly reports).

@description('Tags applied to every resource.')
param tags object

@description('Resource name suffix that keeps global names unique.')
param suffix string

@description('Application Insights resource id.')
param appInsightsId string

@description('Who receives operational alerts.')
param alertEmail string

resource actionGroup 'Microsoft.Insights/actionGroups@2023-01-01' = {
  name: 'ag-feedback-${suffix}'
  location: 'global'
  tags: tags
  properties: {
    groupShortName: 'feedbackops'
    enabled: true
    emailReceivers: [
      {
        name: 'operators'
        emailAddress: alertEmail
        useCommonAlertSchema: true
      }
    ]
  }
}

var alertRules = [
  {
    name: 'alert-function-failures-${suffix}'
    description: 'Uma ou mais execuções de função falharam nos últimos 5 minutos (inclui respostas HTTP 5xx).'
    metricName: 'requests/failed'
    severity: 1
  }
  {
    name: 'alert-exceptions-${suffix}'
    description: 'Exceções não tratadas registradas pelas funções nos últimos 5 minutos.'
    metricName: 'exceptions/count'
    severity: 2
  }
]

resource metricAlerts 'Microsoft.Insights/metricAlerts@2018-03-01' = [
  for rule in alertRules: {
    name: rule.name
    location: 'global'
    tags: tags
    properties: {
      description: rule.description
      severity: rule.severity
      enabled: true
      scopes: [
        appInsightsId
      ]
      evaluationFrequency: 'PT5M'
      windowSize: 'PT5M'
      autoMitigate: true
      criteria: {
        'odata.type': 'Microsoft.Azure.Monitor.SingleResourceMultipleMetricCriteria'
        allOf: [
          {
            criterionType: 'StaticThresholdCriterion'
            name: 'above-zero'
            metricNamespace: 'microsoft.insights/components'
            metricName: rule.metricName
            operator: 'GreaterThan'
            threshold: 0
            timeAggregation: 'Count'
          }
        ]
      }
      actions: [
        {
          actionGroupId: actionGroup.id
        }
      ]
    }
  }
]

var workbookQueries = [
  {
    title: 'Execuções e falhas por função (últimas 24h)'
    visualization: 'table'
    query: 'requests | where timestamp > ago(24h) | summarize Execucoes = count(), Falhas = countif(success == false), DuracaoMediaMs = round(avg(duration), 0) by Funcao = operation_Name'
  }
  {
    title: 'Execuções ao longo do tempo'
    visualization: 'timechart'
    query: 'requests | where timestamp > ago(7d) | summarize Execucoes = count() by bin(timestamp, 1h), Funcao = operation_Name'
  }
  {
    title: 'Avaliações recebidas por urgência (últimos 7 dias)'
    visualization: 'piechart'
    query: 'traces | where timestamp > ago(7d) and message has "FeedbackReceived" | extend Urgencia = extract(@"urgency=(\\w+)", 1, message) | summarize Quantidade = count() by Urgencia'
  }
  {
    title: 'Alertas de avaliação crítica enviados (por dia)'
    visualization: 'barchart'
    query: 'traces | where timestamp > ago(30d) and message has "CriticalFeedbackNotified" | summarize Alertas = count() by bin(timestamp, 1d)'
  }
  {
    title: 'Relatórios semanais gerados'
    visualization: 'table'
    query: 'traces | where timestamp > ago(90d) and message has "WeeklyReportGenerated" | extend Semana = extract(@"weekStart=([0-9-]+)", 1, message), Total = extract(@"totalFeedback=(\\d+)", 1, message), Media = extract(@"averageRating=([0-9.]+|null)", 1, message) | project timestamp, Semana, Total, Media | order by timestamp desc'
  }
  {
    title: 'Exceções por tipo'
    visualization: 'table'
    query: 'exceptions | where timestamp > ago(7d) | summarize Quantidade = count() by Tipo = type, Funcao = operation_Name | order by Quantidade desc'
  }
]

var workbookContent = {
  version: 'Notebook/1.0'
  items: concat(
    [
      {
        type: 1
        content: {
          json: '## Plataforma de Feedback — monitoramento\nExecuções das funções serverless, falhas e eventos de negócio.'
        }
        name: 'header'
      }
    ],
    map(range(0, length(workbookQueries)), index => {
      type: 3
      content: {
        version: 'KqlItem/1.0'
        title: workbookQueries[index].title
        query: workbookQueries[index].query
        size: 0
        queryType: 0
        resourceType: 'microsoft.insights/components'
        visualization: workbookQueries[index].visualization
      }
      name: 'query-${index}'
    })
  )
  fallbackResourceIds: [
    appInsightsId
  ]
}

resource workbook 'Microsoft.Insights/workbooks@2023-06-01' = {
  name: guid(resourceGroup().id, 'feedback-platform-workbook')
  location: resourceGroup().location
  tags: tags
  kind: 'shared'
  properties: {
    displayName: 'Plataforma de Feedback - Monitoramento'
    category: 'workbook'
    sourceId: appInsightsId
    serializedData: string(workbookContent)
  }
}
