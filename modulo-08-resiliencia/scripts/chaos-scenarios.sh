#!/usr/bin/env bash
# chaos-scenarios.sh - activa/desactiva modos de falla en el flaky-downstream.
set -euo pipefail

FLAKY_URL="${FLAKY_URL:-http://localhost:8090}"
SCENARIO="${1:-help}"

apply() {
  curl -fsS -X POST "$FLAKY_URL/flaky/config" \
    -H 'Content-Type: application/json' \
    -d "$1" | jq
}

case "$SCENARIO" in
  ok)      apply '{"failRate":0.0,"latencyMs":50,"mode":"OK"}' ;;
  fail30)  apply '{"failRate":0.3,"latencyMs":100,"mode":"FAIL"}' ;;
  fail70)  apply '{"failRate":0.7,"latencyMs":200,"mode":"FAIL"}' ;;
  slow)    apply '{"failRate":0.0,"latencyMs":600,"mode":"SLOW"}' ;;
  timeout) apply '{"failRate":0.0,"latencyMs":0,"mode":"TIMEOUT"}' ;;
  status)  curl -fsS "$FLAKY_URL/flaky/config" | jq ;;
  *)
    cat <<EOF
Uso: $0 <scenario>
  ok       - respuestas normales
  fail30   - 30% de fallos 500
  fail70   - 70% de fallos 500 (dispara Circuit Breaker)
  slow     - respuestas lentas (dispara slowCallRateThreshold)
  timeout  - cuelga la conexion (dispara TimeLimiter/Timeout)
  status   - muestra config actual del flaky
EOF
    ;;
esac

