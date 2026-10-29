// Access governance and cost control for the resource group:
// - Azure Policy denies resources without the `project` tag (every resource stays attributable);
// - a monthly budget warns before the student credits are consumed;
// - team members get access individually (the FIAP tenant does not allow creating security groups).

@description('Tag every resource must carry.')
param requiredTagName string = 'project'

@description('Monthly budget in USD for the resource group.')
param budgetAmount int

@description('First day of the budget period (yyyy-MM-01).')
param budgetStartDate string

@description('Who receives budget notifications.')
param budgetEmail string

@description('Create the budget (disable if the subscription offer does not support budgets).')
param enableBudget bool = true

@description('Object ids of team members who get access to the resource group.')
param teamMemberPrincipalIds array = []

@description('Role granted to team members.')
@allowed(['Reader', 'Contributor'])
param teamMemberRole string = 'Reader'

var requireTagPolicyDefinitionId = '871b6d14-10aa-478d-b590-94f262ecfa99'
var roleIds = {
  Reader: 'acdd72a7-3385-48ef-bd42-f606fba81ae7'
  Contributor: 'b24988ac-6180-42dd-ab88-20f7382dd24c'
}

resource requireProjectTag 'Microsoft.Authorization/policyAssignments@2024-04-01' = {
  name: 'require-${requiredTagName}-tag'
  properties: {
    displayName: 'Exigir a tag "${requiredTagName}" em todos os recursos'
    description: 'Governança: recursos sem a tag ${requiredTagName} são bloqueados.'
    policyDefinitionId: tenantResourceId('Microsoft.Authorization/policyDefinitions', requireTagPolicyDefinitionId)
    parameters: {
      tagName: {
        value: requiredTagName
      }
    }
  }
}

resource budget 'Microsoft.Consumption/budgets@2023-11-01' = if (enableBudget) {
  name: 'budget-feedback-platform'
  properties: {
    category: 'Cost'
    amount: budgetAmount
    timeGrain: 'Monthly'
    timePeriod: {
      startDate: budgetStartDate
    }
    notifications: {
      actual50Percent: {
        enabled: true
        operator: 'GreaterThanOrEqualTo'
        threshold: 50
        thresholdType: 'Actual'
        contactEmails: [
          budgetEmail
        ]
      }
      forecast100Percent: {
        enabled: true
        operator: 'GreaterThanOrEqualTo'
        threshold: 100
        thresholdType: 'Forecasted'
        contactEmails: [
          budgetEmail
        ]
      }
    }
  }
}

resource teamAccess 'Microsoft.Authorization/roleAssignments@2022-04-01' = [
  for principalId in teamMemberPrincipalIds: {
    name: guid(resourceGroup().id, principalId, roleIds[teamMemberRole])
    properties: {
      roleDefinitionId: subscriptionResourceId('Microsoft.Authorization/roleDefinitions', roleIds[teamMemberRole])
      principalId: principalId
      principalType: 'User'
    }
  }
]
