#!/usr/bin/env bash
# Coloca una orden del tenant tienda-deportes y muestra el traceId
# para buscarlo en Grafana Tempo.
set -euo pipefail

ORDER_URL="${ORDER_URL:-http://localhost:8087}"
TEMPO_URL="${TEMPO_URL:-http://localhost:3200}"
TENANT="${TENANT:-tienda-deportes}"

need() { command -v "$1" >/dev/null 2>&1 || { echo "Falta $1"; exit 1; }; }
need curl
need jq

banner() { printf "\n\033[1;34m==> %s\033[0m\n" "$*"; }

banner "1. Colocando orden de ${TENANT}"
body=$(curl -fsS -X POST "$ORDER_URL/api/v1/orders" \
  -H 'Content-Type: application/json' \
  -H "X-Tenant-Id: ${TENANT}" \
  -d '{"orderId":"O-obs-1","sku":"ZAP-RUN-42","qty":1}')
echo "$body" | jq

trace=$(echo "$body" | jq -r '.traceId')
status=$(echo "$body" | jq -r '.status')
degradation=$(echo "$body" | jq -r '.degradation')

if [[ "$trace" == "none" || -z "$trace" ]]; then
  echo "La orden no trajo traceId. Revisa que order-service haya arrancado con el starter OpenTelemetry."
  exit 1
fi

banner "2. Orden ${status} / ${degradation}"
echo "traceId: $trace"
echo "Grafana: http://localhost:3000  (Explore → Tempo → pega el traceId)"
echo "Loki:    {service_name=\"order-service-spring\"} | trace_id=\"$trace\""

banner "3. Esperando 3s a que el collector entregue la traza a Tempo"
sleep 3
code=$(curl -s -o /tmp/modulo9-trace.json -w '%{http_code}' "$TEMPO_URL/api/traces/$trace" || true)
if [[ "$code" == "200" ]]; then
  services=$(jq -r '[.batches[]?.resource.attributes[]? | select(.key=="service.name") | .value.stringValue] | unique | join(", ")' /tmp/modulo9-trace.json 2>/dev/null || true)
  echo "Tempo respondio 200. Servicios en la traza: ${services:-revisa el JSON en /tmp/modulo9-trace.json}"
else
  echo "Tempo aun no tiene la traza (HTTP ${code}). Es normal si el stack acaba de subir: busca $trace en Grafana en unos segundos."
fi
