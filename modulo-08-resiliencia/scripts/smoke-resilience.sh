#!/usr/bin/env bash
# smoke-resilience.sh - dispara carga contra order-service y muestra las
# transiciones del Circuit Breaker.
set -euo pipefail

ORDER_URL="${ORDER_URL:-http://localhost:8087}"
FLAKY_URL="${FLAKY_URL:-http://localhost:8090}"

need() {
  command -v "$1" >/dev/null 2>&1 || { echo "Falta $1"; exit 1; }
}
need curl
need jq

banner() { printf "\n\033[1;34m==> %s\033[0m\n" "$*"; }

banner "0. Restaurando flaky a OK"
curl -fsS -X POST "$FLAKY_URL/flaky/config" \
  -H 'Content-Type: application/json' \
  -d '{"failRate":0.0,"latencyMs":50,"mode":"OK"}' | jq

banner "1. Readiness inicial"
curl -fsS "$ORDER_URL/actuator/health/readiness" | jq

banner "2. Configurando flaky para 70% de fallos"
curl -fsS -X POST "$FLAKY_URL/flaky/config" \
  -H 'Content-Type: application/json' \
  -d '{"failRate":0.7,"latencyMs":200,"mode":"FAIL"}' | jq

banner "3. Enviando 30 ordenes"
for i in $(seq 1 30); do
  code=$(curl -s -o /dev/null -w '%{http_code}' \
    -X POST "$ORDER_URL/api/v1/orders" \
    -H 'Content-Type: application/json' \
    -d "{\"orderId\":\"O-$i\",\"sku\":\"ZAP-RUN-42\",\"qty\":1}")
  printf "%s " "$code"
done
printf "\n"

banner "4. Estado de los Circuit Breakers"
curl -fsS "$ORDER_URL/actuator/circuitbreakers" | jq

banner "5. Readiness (debe estar OUT_OF_SERVICE si el CB abrio)"
curl -fsS "$ORDER_URL/actuator/health/readiness" | jq || true

banner "6. Restaurando flaky a OK y esperando 12s (waitDurationInOpenState + margen)"
curl -fsS -X POST "$FLAKY_URL/flaky/config" \
  -H 'Content-Type: application/json' \
  -d '{"failRate":0.0,"latencyMs":50,"mode":"OK"}' | jq
sleep 12

banner "7. Enviando 10 ordenes para forzar HALF_OPEN -> CLOSED"
for i in $(seq 100 109); do
  code=$(curl -s -o /dev/null -w '%{http_code}' \
    -X POST "$ORDER_URL/api/v1/orders" \
    -H 'Content-Type: application/json' \
    -d "{\"orderId\":\"O-$i\",\"sku\":\"ZAP-RUN-42\",\"qty\":1}")
  printf "%s " "$code"
done
printf "\n"

banner "8. Estado final de los Circuit Breakers"
curl -fsS "$ORDER_URL/actuator/circuitbreakers" | jq
curl -fsS "$ORDER_URL/actuator/health/readiness" | jq

