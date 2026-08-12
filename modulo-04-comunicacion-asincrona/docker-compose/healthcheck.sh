#!/usr/bin/env bash
# Verifica que Kafka + Schema Registry estén listos. Módulo 4 — JoeDayz.pe

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

log() { printf '\n==> %s\n' "$*"; }
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

wait_for() {
  local name="$1" cmd="$2" attempts="${3:-30}"
  log "Esperando ${name}..."
  for ((i = 1; i <= attempts; i++)); do
    if eval "${cmd}" >/dev/null 2>&1; then
      printf '    OK (%s)\n' "${name}"
      return 0
    fi
    printf '    intento %s/%s...\n' "${i}" "${attempts}"
    sleep 2
  done
  die "${name} no respondió a tiempo"
}

cd "${SCRIPT_DIR}"

wait_for "Kafka broker" "docker compose exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092"
wait_for "Schema Registry" "curl -sf http://localhost:8085/subjects"

log "Kafka + Schema Registry listos. UI disponible en http://localhost:8090"
