#!/usr/bin/env bash
# One-time setup that lets GitHub Actions deploy to Azure WITHOUT any stored password (OpenID Connect):
#   1. creates a Microsoft Entra application + service principal for the pipeline;
#   2. trusts tokens issued by GitHub for this repository's "production" environment (federated credential);
#   3. grants it "Website Contributor" on the resource group only (deploy code to the Function Apps, nothing else);
#   4. stores the identifiers as GitHub environment secrets and the resource names as repository variables.
#
# Prerequisites: az login (Owner of the subscription), gh auth login, and the infrastructure already deployed.
# Usage: ./infra/scripts/setup-github-oidc.sh
set -euo pipefail

RESOURCE_GROUP="${RESOURCE_GROUP:-rg-feedback-platform-dev}"
APP_DISPLAY_NAME="${APP_DISPLAY_NAME:-github-deploy-feedback-platform}"
ENVIRONMENT_NAME="production"
WEBSITE_CONTRIBUTOR_ROLE="de139f84-1756-47ae-9be6-808fbbe84772"

REPOSITORY="$(gh repo view --json nameWithOwner -q .nameWithOwner)"
SUBSCRIPTION_ID="$(az account show --query id -o tsv)"
TENANT_ID="$(az account show --query tenantId -o tsv)"
RESOURCE_GROUP_ID="$(az group show --name "${RESOURCE_GROUP}" --query id -o tsv)"

echo "Repository: ${REPOSITORY} | Resource group: ${RESOURCE_GROUP}"

# 1. Application + service principal (reused when they already exist).
CLIENT_ID="$(az ad app list --display-name "${APP_DISPLAY_NAME}" --query "[0].appId" -o tsv)"
if [[ -z "${CLIENT_ID}" ]]; then
  CLIENT_ID="$(az ad app create --display-name "${APP_DISPLAY_NAME}" --query appId -o tsv)"
  echo "Created application ${APP_DISPLAY_NAME}"
fi
PRINCIPAL_ID="$(az ad sp list --filter "appId eq '${CLIENT_ID}'" --query "[0].id" -o tsv)"
if [[ -z "${PRINCIPAL_ID}" ]]; then
  PRINCIPAL_ID="$(az ad sp create --id "${CLIENT_ID}" --query id -o tsv)"
  echo "Created service principal"
fi

# 2. Federated credential: only workflows running in this repository's production environment get tokens.
SUBJECT="repo:${REPOSITORY}:environment:${ENVIRONMENT_NAME}"
if [[ -z "$(az ad app federated-credential list --id "${CLIENT_ID}" --query "[?subject=='${SUBJECT}'].name" -o tsv)" ]]; then
  az ad app federated-credential create --id "${CLIENT_ID}" --parameters "{
    \"name\": \"github-${ENVIRONMENT_NAME}\",
    \"issuer\": \"https://token.actions.githubusercontent.com\",
    \"subject\": \"${SUBJECT}\",
    \"audiences\": [\"api://AzureADTokenExchange\"]
  }" --output none
  echo "Created federated credential for ${SUBJECT}"
fi

# 3. Least privilege: deploy code to the Function Apps of this resource group, nothing else.
if [[ -z "$(az role assignment list --assignee "${PRINCIPAL_ID}" --scope "${RESOURCE_GROUP_ID}" --role "${WEBSITE_CONTRIBUTOR_ROLE}" --query "[0].id" -o tsv)" ]]; then
  az role assignment create --assignee-object-id "${PRINCIPAL_ID}" --assignee-principal-type ServicePrincipal \
    --role "${WEBSITE_CONTRIBUTOR_ROLE}" --scope "${RESOURCE_GROUP_ID}" --output none
  echo "Granted Website Contributor on ${RESOURCE_GROUP}"
fi

# 4. GitHub configuration: identifiers as environment secrets, resource names as repository variables.
gh api --method PUT "repos/${REPOSITORY}/environments/${ENVIRONMENT_NAME}" --silent
gh secret set AZURE_CLIENT_ID --env "${ENVIRONMENT_NAME}" --body "${CLIENT_ID}"
gh secret set AZURE_TENANT_ID --env "${ENVIRONMENT_NAME}" --body "${TENANT_ID}"
gh secret set AZURE_SUBSCRIPTION_ID --env "${ENVIRONMENT_NAME}" --body "${SUBSCRIPTION_ID}"

function_app_name() {
  az functionapp list --resource-group "${RESOURCE_GROUP}" --query "[?starts_with(name, '$1')].name | [0]" -o tsv
}
gh variable set AZURE_RESOURCE_GROUP --body "${RESOURCE_GROUP}"
gh variable set SUBMIT_FUNCTION_APP --body "$(function_app_name func-feedback-submit)"
gh variable set NOTIFIER_FUNCTION_APP --body "$(function_app_name func-feedback-notifier)"
gh variable set REPORT_FUNCTION_APP --body "$(function_app_name func-feedback-report)"

echo "Done. Pushes to main now build, test and deploy through GitHub Actions."
