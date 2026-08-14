#!/usr/bin/env bash
# Build Maven → Podman images → kind load image-archive → kubectl apply
# Orden: primero infraestructura Kafka, luego servicios (dependen de Kafka).

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

SERVICES=(catalog-service-spring inventory-service-spring order-service-spring)

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

log "Desplegando infraestructura Kafka..."
kubectl apply -f "${ROOT_DIR}/k8s/kafka.yaml"
kubectl apply -f "${ROOT_DIR}/k8s/kind/nodeports.yaml"

log "Esperando Kafka Ready (puede tardar ~60s)..."
kubectl rollout status deployment/kafka --timeout=120s
kubectl rollout status deployment/schema-registry --timeout=120s

log "Desplegando servicios de negocio..."
kubectl apply -f "${ROOT_DIR}/k8s/catalog-deployment.yaml"
kubectl apply -f "${ROOT_DIR}/k8s/inventory-deployment.yaml"
kubectl apply -f "${ROOT_DIR}/k8s/order-deployment.yaml"

log "Esperando Deployments Ready..."
kubectl rollout status deployment/catalog-service --timeout=180s
kubectl rollout status deployment/inventory-service --timeout=180s
kubectl rollout status deployment/order-service --timeout=180s

log "Pods:"
kubectl get pods -l 'app in (kafka,schema-registry,kafka-ui,catalog-service,inventory-service,order-service)' -o wide
log "Services:"
kubectl get svc kafka catalog-service inventory-service order-service kafka-ui

log "Listo. Siguiente: ./scripts/03-smoke.sh"
