#!/usr/bin/env bash
# Helpers compartidos — Módulo 3 Kind lab (Podman)

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CLUSTER_NAME="${CLUSTER_NAME:-microservicios-m03}"
IMAGE_TAG="${IMAGE_TAG:-1.0.0}"
IMAGE_PREFIX="${IMAGE_PREFIX:-joedayz}"

# Kind + Podman (experimental provider)
export KIND_EXPERIMENTAL_PROVIDER="${KIND_EXPERIMENTAL_PROVIDER:-podman}"

log() { printf '\n==> %s\n' "$*"; }
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Falta comando '$1'. Instálalo y reintenta."
}

require_tools() {
  require_cmd kind
  require_cmd kubectl
  require_cmd podman
  require_cmd mvn
  require_cmd curl
  ensure_kind_podman_compat
}

# Podman 6 + Kind ≤0.32: `kind get clusters` falla si YA hay nodos
# (usa index .Labels). No bloqueamos el lab: la verdad es Podman.
# Ver: https://github.com/kubernetes-sigs/kind/issues/4201
ensure_kind_podman_compat() {
  podman info >/dev/null 2>&1 \
    || die "Podman no responde. En macOS: podman machine start"

  local out
  if out="$(kind get clusters 2>&1)"; then
    return 0
  fi
  if printf '%s' "${out}" | grep -q 'cannot index slice/array with type string'; then
    log "AVISO: kind get clusters falla (Kind ≤0.32 + Podman 6)."
    log "Los scripts usan labels de Podman para detectar el cluster."
    log "Recomendado más adelante: brew install --HEAD kind"
    return 0
  fi
  # Sin nodos a veces kind aún falla por otros motivos; no abortar aquí.
  return 0
}

cluster_exists() {
  # Fuente de verdad: nodo Kind en Podman (no depender de `kind get clusters`)
  podman ps -a \
    --filter "label=io.x-k8s.kind.cluster=${CLUSTER_NAME}" \
    --format '{{.ID}}' 2>/dev/null | grep -q .
}

# Podman en macOS/rootless suele guardar como localhost/<name>
image_ref() {
  local name="${IMAGE_PREFIX}/$1:${IMAGE_TAG}"
  if podman image exists "${name}" 2>/dev/null; then
    printf '%s\n' "${name}"
  elif podman image exists "localhost/${name}" 2>/dev/null; then
    printf 'localhost/%s\n' "${name}"
  else
    die "Imagen no encontrada en Podman: ${name} (ni localhost/${name})"
  fi
}

# Carga imagen local de Podman en el nodo Kind (docker-image no sirve con provider=podman).
# Podman etiqueta como localhost/<name>; kubelet pide docker.io/<name> → hay que retagear
# dentro del nodo tras el load.
kind_load_podman_image() {
  local svc="$1"
  local logical="${IMAGE_PREFIX}/${svc}:${IMAGE_TAG}"
  local local_ref
  local_ref="$(image_ref "${svc}")"
  local archive node
  archive="$(mktemp "${TMPDIR:-/tmp}/kind-${svc}.XXXXXX.tar")"
  node="${CLUSTER_NAME}-control-plane"

  log "Cargando ${logical} en Kind (${CLUSTER_NAME}) vía image-archive..."
  podman save -o "${archive}" "${local_ref}"
  kind load image-archive "${archive}" --name "${CLUSTER_NAME}"
  rm -f "${archive}"

  # Tras el load suele quedar solo localhost/joedayz/...; el Deployment usa joedayz/...
  local from="localhost/${logical}"
  local to_docker="docker.io/${logical}"
  if podman exec "${node}" ctr -n k8s.io images ls -q 2>/dev/null | grep -q .; then
    podman exec "${node}" ctr -n k8s.io images tag "${from}" "${logical}" 2>/dev/null || true
    podman exec "${node}" ctr -n k8s.io images tag "${from}" "${to_docker}" 2>/dev/null || true
  fi
}
