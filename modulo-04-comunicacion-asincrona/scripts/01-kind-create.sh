#!/usr/bin/env bash
# Crea el cluster Kind del Módulo 4 (con port-mappings locales).

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

require_tools

if cluster_exists; then
  log "Cluster '${CLUSTER_NAME}' ya existe — no se recrea."
  kubectl cluster-info --context "kind-${CLUSTER_NAME}"
  exit 0
fi

log "Creando Kind cluster '${CLUSTER_NAME}' (provider=${KIND_EXPERIMENTAL_PROVIDER})..."
kind create cluster \
  --name "${CLUSTER_NAME}" \
  --config "${ROOT_DIR}/k8s/kind/config.yaml"

kubectl cluster-info --context "kind-${CLUSTER_NAME}"
log "Listo. Siguiente: ./scripts/02-deploy.sh"
