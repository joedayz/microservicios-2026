#!/usr/bin/env bash
# Build Maven → Podman images → kind load image-archive → kubectl apply

set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=common.sh
source "${SCRIPT_DIR}/common.sh"

require_tools

cluster_exists \
  || die "Cluster '${CLUSTER_NAME}' no existe. Ejecuta primero:

  cd $(dirname "${SCRIPT_DIR}")
  ./scripts/01-kind-create.sh
  ./scripts/02-deploy.sh"

SERVICES=(catalog-service inventory-service order-service)

log "Provider Kind: ${KIND_EXPERIMENTAL_PROVIDER}"

log "Compilando JARs (Maven)..."
for svc in "${SERVICES[@]}"; do
  mvn -f "${ROOT_DIR}/${svc}/pom.xml" -q package -DskipTests
done

log "Construyendo imágenes con Podman..."
for svc in "${SERVICES[@]}"; do
  image="${IMAGE_PREFIX}/${svc}:${IMAGE_TAG}"
  podman build -t "${image}" "${ROOT_DIR}/${svc}"
done

log "Cargando imágenes en Kind (${CLUSTER_NAME})..."
for svc in "${SERVICES[@]}"; do
  kind_load_podman_image "${svc}"
done

log "Aplicando manifiestos k8s/..."
kubectl apply -f "${ROOT_DIR}/k8s/catalog-deployment.yaml"
kubectl apply -f "${ROOT_DIR}/k8s/inventory-deployment.yaml"
kubectl apply -f "${ROOT_DIR}/k8s/order-deployment.yaml"
kubectl apply -f "${ROOT_DIR}/k8s/kind/nodeports.yaml"

log "Esperando Deployments Ready..."
kubectl rollout status deployment/catalog-service --timeout=180s
kubectl rollout status deployment/inventory-service --timeout=180s
kubectl rollout status deployment/order-service --timeout=180s

log "Pods:"
kubectl get pods -l 'app in (catalog-service,inventory-service,order-service)' -o wide
log "Services:"
kubectl get svc catalog-service inventory-service order-service

log "Listo. Siguiente: ./scripts/03-smoke.sh"
