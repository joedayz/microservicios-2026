#!/usr/bin/env bash
# Pruebas con curl (y grpcurl opcional) contra los NodePorts de Kind.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

require_cmd curl
require_cmd kubectl

# Usar 127.0.0.1 (no localhost): en macOS curl puede ir a ::1 y el
# port-mapping de Kind/Podman solo escucha en IPv4.
CATALOG_URL="${CATALOG_URL:-http://127.0.0.1:8081}"
ORDER_URL="${ORDER_URL:-http://127.0.0.1:8085}"
INVENTORY_GRPC="${INVENTORY_GRPC:-127.0.0.1:9090}"
TENANT="${TENANT:-tienda-deportes}"
SKU="${SKU:-ZAP-RUN-42}"

wait_http() {
  local url="$1"
  local name="$2"
  local attempts="${3:-60}"
  log "Esperando ${name} (${url})..."
  for ((i = 1; i <= attempts; i++)); do
    if curl -sf "${url}" >/dev/null 2>&1; then
      printf '    OK (%s)\n' "${name}"
      return 0
    fi
    printf '    intento %s/%s...\n' "${i}" "${attempts}"
    sleep 2
  done
  die "${name} no respondió a tiempo: ${url}
¿Services en NodePort? → kubectl get svc
¿Pods Ready? → kubectl get pods"
}

pretty() {
  if command -v jq >/dev/null 2>&1; then
    jq .
  else
    cat
  fi
}

wait_http "${CATALOG_URL}/actuator/health" "catalog-service"
wait_http "${ORDER_URL}/actuator/health" "order-service"

log "1) Catalog — listar productos"
curl -sS "${CATALOG_URL}/api/v1/products" \
  -H "X-Tenant-ID: ${TENANT}" | pretty

log "2) Catalog — producto por SKU (${SKU})"
curl -sS "${CATALOG_URL}/api/v1/products/${SKU}" \
  -H "X-Tenant-ID: ${TENANT}" | pretty

log "3) Checkout RestClient"
curl -sS -X POST "${ORDER_URL}/api/v1/checkout" \
  -H 'Content-Type: application/json' \
  -H "X-Tenant-ID: ${TENANT}" \
  -H 'X-Client-Style: restclient' \
  -d "{\"sku\":\"${SKU}\",\"quantity\":2}" | pretty

log "4) Checkout WebClient"
curl -sS -X POST "${ORDER_URL}/api/v1/checkout" \
  -H 'Content-Type: application/json' \
  -H "X-Tenant-ID: ${TENANT}" \
  -H 'X-Client-Style: webclient' \
  -d "{\"sku\":\"${SKU}\",\"quantity\":1}" | pretty

log "5) Checkout Feign"
curl -sS -X POST "${ORDER_URL}/api/v1/checkout" \
  -H 'Content-Type: application/json' \
  -H "X-Tenant-ID: ${TENANT}" \
  -H 'X-Client-Style: feign' \
  -d "{\"sku\":\"${SKU}\",\"quantity\":1}" | pretty

if command -v grpcurl >/dev/null 2>&1; then
  log "6) Inventory gRPC CheckStock (grpcurl)"
  grpcurl -plaintext \
    -d "{\"tenant_id\":\"${TENANT}\",\"sku\":\"${SKU}\",\"quantity\":2}" \
    "${INVENTORY_GRPC}" inventory.v1.InventoryService/CheckStock | pretty
else
  log "6) Inventory gRPC — omitido (instala grpcurl para probar :9090)"
fi

log "Smoke OK"
