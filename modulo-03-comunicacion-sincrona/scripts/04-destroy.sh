#!/usr/bin/env bash
# Elimina el cluster Kind del Módulo 3.

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

require_cmd kind
require_cmd podman

if ! cluster_exists; then
  log "Cluster '${CLUSTER_NAME}' no existe — nada que borrar."
  exit 0
fi

log "Borrando Kind cluster '${CLUSTER_NAME}'..."
if ! kind delete cluster --name "${CLUSTER_NAME}"; then
  die "kind delete falló (¿Kind ≤0.32 + Podman 6?). Prueba: brew install --HEAD kind"
fi
log "Cluster eliminado."
