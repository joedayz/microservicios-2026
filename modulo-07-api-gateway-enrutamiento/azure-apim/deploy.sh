#!/usr/bin/env bash
# ============================================================================
#  Demo Azure API Management - Modulo 7
#  ---------------------------------------------------------------------------
#  Despliega en TU subscription de Azure usando Bicep.
#
#  Uso:
#    ./deploy.sh check     Verifica prerequisitos (az cli, login, subscription)
#    ./deploy.sh deploy    Crea RG + APIM + APIs + policies
#    ./deploy.sh outputs   Muestra los outputs del deployment
#    ./deploy.sh test      Llama /gateway/inventory con la subscription key
#    ./deploy.sh destroy   Borra el resource group completo (async)
#
#  IMPORTANTE: APIM Developer SKU tarda 30-45 min la primera vez.
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

RESOURCE_GROUP="${RESOURCE_GROUP:-rg-joedayz-modulo7}"
LOCATION="${LOCATION:-eastus}"
APIM_NAME="${APIM_NAME:-apim-joedayz-$RANDOM}"
DEPLOYMENT_NAME="${DEPLOYMENT_NAME:-modulo7-edge}"

info() { printf "\033[1;34m[info]\033[0m %s\n" "$*"; }
ok()   { printf "\033[1;32m[ok]\033[0m %s\n"   "$*"; }
err()  { printf "\033[1;31m[error]\033[0m %s\n" "$*" >&2; }

check_prereqs() {
    if ! command -v az >/dev/null 2>&1; then
        err "Falta 'az' (Azure CLI). Instala: https://learn.microsoft.com/cli/azure/install-azure-cli"
        exit 1
    fi
    info "Azure CLI: $(az version --query '"azure-cli"' -o tsv)"
    if ! az account show >/dev/null 2>&1; then
        err "No hay sesion activa. Ejecuta:  az login"
        exit 1
    fi
    info "Subscription: $(az account show --query name -o tsv) [$(az account show --query id -o tsv)]"
    info "Tenant:       $(az account show --query tenantId -o tsv)"
    info "Resource group: $RESOURCE_GROUP  |  Location: $LOCATION"
    info "APIM name:      $APIM_NAME"
    ok "Prerequisitos OK"
}

show_outputs() {
    az deployment group show --resource-group "$RESOURCE_GROUP" \
        --name "$DEPLOYMENT_NAME" --query 'properties.outputs' -o json
}

case "${1:-check}" in
    check) check_prereqs ;;

    deploy)
        check_prereqs

        info "Paso 1: crear resource group '$RESOURCE_GROUP' en '$LOCATION' (idempotente)"
        az group create -n "$RESOURCE_GROUP" -l "$LOCATION" -o table

        info "Paso 2: desplegar Bicep (APIM Developer tarda 30-45min la 1ra vez)"
        info "        Puedes seguir el progreso en el portal Azure -> Resource group -> Deployments"
        az deployment group create \
            --resource-group "$RESOURCE_GROUP" \
            --name "$DEPLOYMENT_NAME" \
            --template-file main.bicep \
            --parameters apimName="$APIM_NAME" \
                         publisherEmail="${PUBLISHER_EMAIL:-demo@joedayz.pe}" \
                         publisherName="${PUBLISHER_NAME:-JoeDayz Demo}" \
                         orderBackendUrl="${ORDER_BACKEND_URL:-https://order-service.internal.joedayz.pe}" \
                         inventoryBackendUrl="${INVENTORY_BACKEND_URL:-https://inventory-service.internal.joedayz.pe}" \
            -o table

        ok "Deployment terminado"
        show_outputs
        ;;

    outputs) show_outputs ;;

    test)
        GATEWAY_URL=$(az deployment group show -g "$RESOURCE_GROUP" -n "$DEPLOYMENT_NAME" \
            --query 'properties.outputs.apimGatewayUrl.value' -o tsv)
        SUB_ID=$(az deployment group show -g "$RESOURCE_GROUP" -n "$DEPLOYMENT_NAME" \
            --query 'properties.outputs.subscriptionKeyResourceId.value' -o tsv)
        KEY=$(az rest --method post \
            --uri "https://management.azure.com${SUB_ID}/listSecrets?api-version=2023-05-01-preview" \
            --query primaryKey -o tsv)
        info "Gateway: $GATEWAY_URL"
        info "Sub key: ${KEY:0:8}..."
        JWT=$(cat token.txt 2>/dev/null || echo dummy)
        curl -i -H "Ocp-Apim-Subscription-Key: $KEY" \
             -H "X-Tenant-ID: tienda-deportes" \
             -H "Authorization: Bearer $JWT" \
             "$GATEWAY_URL/gateway/inventory/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE" \
             || true
        ;;

    destroy)
        info "Borrando resource group '$RESOURCE_GROUP' (async)"
        az group delete -n "$RESOURCE_GROUP" --yes --no-wait
        ok "Delete en background. Verificar con:  az group show -n $RESOURCE_GROUP"
        ;;

    *)
        echo "Uso: $0 {check|deploy|outputs|test|destroy}" >&2
        exit 1
        ;;
esac
