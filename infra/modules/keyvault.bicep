// Key Vault holding the administrators' e-mail addresses, so they never live in source code or plain app settings.
// Authorization uses Azure RBAC (no access policies); only the functions that send e-mail may read the secret.

@description('Azure region.')
param location string

@description('Tags applied to every resource.')
param tags object

@description('Resource name suffix that keeps global names unique.')
param suffix string

@secure()
@description('Comma-separated administrator e-mail addresses.')
param adminEmails string

var adminEmailsSecretName = 'admin-emails'

resource vault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: take('kv-feedback-${suffix}', 24)
  location: location
  tags: tags
  properties: {
    tenantId: subscription().tenantId
    sku: {
      family: 'A'
      name: 'standard'
    }
    enableRbacAuthorization: true
    enableSoftDelete: true
    softDeleteRetentionInDays: 7
    publicNetworkAccess: 'Enabled'
  }
}

resource adminEmailsSecret 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: vault
  name: adminEmailsSecretName
  tags: tags
  properties: {
    value: adminEmails
    contentType: 'text/plain; comma-separated e-mail addresses'
  }
}

output name string = vault.name
output adminEmailsSecretName string = adminEmailsSecret.name
output adminEmailsReference string = '@Microsoft.KeyVault(VaultName=${vault.name};SecretName=${adminEmailsSecret.name})'
