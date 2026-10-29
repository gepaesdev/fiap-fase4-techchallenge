#!/usr/bin/env bash
# Creates or updates the whole Azure environment from infra/main.bicep.
#
# Usage:
#   ADMIN_EMAILS="admin@example.com" ./infra/scripts/deploy.sh
#
# Optional environment variables:
#   ALERT_EMAIL         who receives operational alerts (defaults to ADMIN_EMAILS)
#   REPORT_WEEK_OFFSET  1 = previous full week (default), 0 = current week (demo)
#   LOCATION            Azure region (default: brazilsouth)
set -euo pipefail

: "${ADMIN_EMAILS:?Set ADMIN_EMAILS with the administrator e-mail(s), comma-separated}"
LOCATION="${LOCATION:-brazilsouth}"
INFRA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEPLOYMENT_NAME="feedback-platform-$(date +%Y%m%d%H%M%S)"

echo "Deploying to subscription: $(az account show --query name -o tsv)"
echo "Region: ${LOCATION} | Deployment: ${DEPLOYMENT_NAME}"

az deployment sub create \
  --name "${DEPLOYMENT_NAME}" \
  --location "${LOCATION}" \
  --template-file "${INFRA_DIR}/main.bicep" \
  --parameters "${INFRA_DIR}/main.bicepparam" \
  --query "properties.outputs" \
  --output json
