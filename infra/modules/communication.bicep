// E-mail delivery: Email Communication Service with a free Azure-managed domain (DoNotReply@<id>.azurecomm.net),
// linked to a Communication Services resource. Data is kept in Brazil.

@description('Tags applied to every resource.')
param tags object

@description('Resource name suffix that keeps global names unique.')
param suffix string

@description('Where Communication Services stores data at rest.')
param dataLocation string = 'Brazil'

resource emailService 'Microsoft.Communication/emailServices@2023-04-01' = {
  name: 'ecs-feedback-${suffix}'
  location: 'global'
  tags: tags
  properties: {
    dataLocation: dataLocation
  }
}

resource managedDomain 'Microsoft.Communication/emailServices/domains@2023-04-01' = {
  parent: emailService
  name: 'AzureManagedDomain'
  location: 'global'
  tags: tags
  properties: {
    domainManagement: 'AzureManaged'
    userEngagementTracking: 'Disabled'
  }
}

resource communicationService 'Microsoft.Communication/communicationServices@2025-05-01' = {
  name: 'acs-feedback-${suffix}'
  location: 'global'
  tags: tags
  properties: {
    dataLocation: dataLocation
    linkedDomains: [
      managedDomain.id
    ]
    // Senders must use Microsoft Entra ID (managed identity); access keys cannot be used.
    disableLocalAuth: true
  }
}

// The built-in "Communication and Email Service Owner" role can also delete resources and list keys. This
// custom role keeps only what sending an e-mail with Microsoft Entra ID requires.
resource emailSenderRole 'Microsoft.Authorization/roleDefinitions@2022-04-01' = {
  name: guid(resourceGroup().id, 'feedback-email-sender')
  properties: {
    roleName: 'Feedback Platform Email Sender (${suffix})'
    description: 'Send e-mails through the feedback platform Communication Services resource.'
    type: 'CustomRole'
    assignableScopes: [
      resourceGroup().id
    ]
    permissions: [
      {
        actions: [
          'Microsoft.Communication/CommunicationServices/Read'
          'Microsoft.Communication/CommunicationServices/Write'
          'Microsoft.Communication/EmailServices/read'
        ]
      }
    ]
  }
}

output communicationServiceName string = communicationService.name
output endpoint string = 'https://${communicationService.properties.hostName}'
output senderAddress string = 'DoNotReply@${managedDomain.properties.mailFromSenderDomain}'
output emailSenderRoleId string = emailSenderRole.id
