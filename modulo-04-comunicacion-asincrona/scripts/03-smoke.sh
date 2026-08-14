#!/usr/bin/env bash
# Smoke test del flujo Saga completo en K8s (Módulo 4).
# Usa 127.0.0.1 (no localhost): en macOS curl puede ir a ::1 y el
# port-mapping de Kind/Podman solo escucha en IPv4.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

require_cmd curl
require_cmd kubectl

ORDER_URL="${ORDER_URL:-http://127.0.0.1:8086}"
KAFKA_UI_URL="${KAFKA_UI_URL:-http://127.0.0.1:8090}"
TENANT="${TENANT:-demo-tenant}"
SKU="${SKU:-SKU-001}"

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
¿Pods Ready?           → kubectl get pods"
}

pretty() {
  if command -v jq >/dev/null 2>&1; then
    jq .
  else
    cat
  fi
}

wait_http "${ORDER_URL}/actuator/health" "order-service"

log "1) Crear orden (Saga happy-path: SKU-001 con stock)"
curl -sS -X POST "${ORDER_URL}/api/v1/orders" \
  -H 'Content-Type: application/json' \
  -H "X-Tenant-ID: ${TENANT}" \
  -d "{\"customerId\":\"customer-001\",\"sku\":\"${SKU}\",\"quantity\":2}" | pretty

log "2) Crear orden (Saga compensación: SKU sin stock)"
curl -sS -X POST "${ORDER_URL}/api/v1/orders" \
  -H 'Content-Type: application/json' \
  -H "X-Tenant-ID: ${TENANT}" \
  -d '{"customerId":"customer-002","sku":"SKU-003","quantity":1}' | pretty

log "3) Crear orden (Saga compensación: SKU inexistente)"
curl -sS -X POST "${ORDER_URL}/api/v1/orders" \
  -H 'Content-Type: application/json' \
  -H "X-Tenant-ID: ${TENANT}" \
  -d '{"customerId":"customer-003","sku":"SKU-NOEXISTE","quantity":1}' | pretty

log "Kafka UI disponible en: ${KAFKA_UI_URL}"
log "Smoke OK — revisa los tópicos en Kafka UI para ver los eventos."
