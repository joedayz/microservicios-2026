#!/usr/bin/env bash
set -euo pipefail

VAULT_ADDR="${VAULT_ADDR:-http://localhost:8200}"
VAULT_TOKEN="${VAULT_TOKEN:-root}"

curl --fail --silent --show-error \
  --request POST \
  --header "X-Vault-Token: ${VAULT_TOKEN}" \
  --header "Content-Type: application/json" \
  --data '{"data":{"client-id":"order-service-mtls-client"}}' \
  "${VAULT_ADDR}/v1/secret/data/module6/inventory-client" >/dev/null

echo "Secreto module6/inventory-client cargado en Vault (${VAULT_ADDR})"
