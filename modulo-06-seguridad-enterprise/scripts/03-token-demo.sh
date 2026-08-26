#!/usr/bin/env bash
# Pide un access token a Keycloak para un usuario de la demo y prueba
# GET/POST contra Order Service para mostrar RBAC + ABAC en vivo.
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
REALM="${REALM:-joedayz-microservices}"
CLIENT_ID="${CLIENT_ID:-student-portal}"
ORDER_SERVICE_URL="${ORDER_SERVICE_URL:-http://localhost:8086}"
TENANT="${TENANT:-tienda-deportes}"
REGION="${REGION:-PE}"

USERNAME="${1:-}"
PASSWORD="${2:-secret123}"

if [[ -z "${USERNAME}" ]]; then
  echo "Uso: $0 <username> [password]" >&2
  echo "Ejemplos:" >&2
  echo "  $0 ana-reader     # Paso 5: lectura permitida, escritura denegada" >&2
  echo "  $0 bruno-manager  # Paso 6: escritura permitida en tienda-deportes/PE" >&2
  echo "  $0 carla-admin" >&2
  exit 1
fi

for bin in curl jq; do
  command -v "${bin}" >/dev/null 2>&1 || { echo "Falta '${bin}' en el PATH." >&2; exit 1; }
done

decode_jwt_payload() {
  local payload
  payload=$(echo "$1" | cut -d '.' -f2 | tr '_-' '/+' | tr -d '\n')
  case $((${#payload} % 4)) in
    2) payload="${payload}==" ;;
    3) payload="${payload}=" ;;
  esac
  echo "${payload}" | base64 --decode 2>/dev/null
}

call() {
  local method="$1" url="$2" body="${3:-}"
  local response status
  if [[ -n "${body}" ]]; then
    response=$(curl --silent --show-error -w '\n%{http_code}' \
      -X "${method}" "${url}" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H 'Content-Type: application/json' \
      -d "${body}")
  else
    response=$(curl --silent --show-error -w '\n%{http_code}' \
      -X "${method}" "${url}" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}")
  fi
  status="${response##*$'\n'}"
  echo "HTTP ${status}"
  echo "${response%$'\n'*}" | jq '.' 2>/dev/null || echo "${response%$'\n'*}"
}

echo "==> Pidiendo access token para ${USERNAME}..."
TOKEN_RESPONSE=$(curl --fail --silent --show-error \
  -X POST "${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token" \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d "client_id=${CLIENT_ID}" \
  -d "username=${USERNAME}" \
  -d "password=${PASSWORD}")

ACCESS_TOKEN=$(echo "${TOKEN_RESPONSE}" | jq -r '.access_token')

if [[ -z "${ACCESS_TOKEN}" || "${ACCESS_TOKEN}" == "null" ]]; then
  echo "No se pudo obtener el access_token. Respuesta de Keycloak:" >&2
  echo "${TOKEN_RESPONSE}" >&2
  exit 1
fi

echo "==> Claims relevantes del JWT (realm_access, tenant_id, region):"
decode_jwt_payload "${ACCESS_TOKEN}" | jq '{realm_access, tenant_id, region}'

echo
echo "==> [1] GET  /api/v1/tenants/${TENANT}/orders (lectura)"
call GET "${ORDER_SERVICE_URL}/api/v1/tenants/${TENANT}/orders"

echo
echo "==> [2] POST /api/v1/tenants/${TENANT}/orders (escritura en su propio tenant/region)"
call POST "${ORDER_SERVICE_URL}/api/v1/tenants/${TENANT}/orders" \
  "{\"sku\":\"ZAP-RUN-42\",\"quantity\":2,\"shippingRegion\":\"${REGION}\"}"
