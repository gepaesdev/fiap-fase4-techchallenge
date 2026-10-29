#!/usr/bin/env bash
# Deletes the whole Azure environment (all data is lost) and purges the soft-deleted Key Vault, so the
# environment can be recreated later with ./infra/scripts/deploy.sh using the same names.
#
# Usage:
#   ./infra/scripts/destroy.sh            (asks for confirmation)
#   ./infra/scripts/destroy.sh --yes      (no confirmation)
set -euo pipefail

RESOURCE_GROUP="${RESOURCE_GROUP:-rg-feedback-platform-dev}"

if ! az group exists --name "${RESOURCE_GROUP}" | grep -q true; then
  echo "Resource group ${RESOURCE_GROUP} does not exist. Nothing to delete."
  exit 0
fi

if [[ "${1:-}" != "--yes" ]]; then
  read -r -p "Delete ${RESOURCE_GROUP} and ALL its data? Type the resource group name to confirm: " answer
  [[ "${answer}" == "${RESOURCE_GROUP}" ]] || { echo "Cancelled."; exit 1; }
fi

VAULT_NAMES=$(az keyvault list --resource-group "${RESOURCE_GROUP}" --query "[].name" -o tsv)

echo "Deleting ${RESOURCE_GROUP} (this takes a few minutes)..."
az group delete --name "${RESOURCE_GROUP}" --yes

for vault in ${VAULT_NAMES}; do
  echo "Purging soft-deleted Key Vault ${vault}..."
  az keyvault purge --name "${vault}"
done

echo "Environment deleted. Recreate it with ./infra/scripts/deploy.sh"
