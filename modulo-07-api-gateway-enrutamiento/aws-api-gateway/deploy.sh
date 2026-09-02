#!/usr/bin/env bash
# ============================================================================
#  Demo AWS API Gateway - Modulo 7
#  ---------------------------------------------------------------------------
#  Despliega en TU cuenta AWS usando AWS SAM.
#
#  Uso:
#    ./deploy.sh check    Verifica prerequisitos (aws, sam, credenciales)
#    ./deploy.sh deploy   sam build + sam deploy (crea el CloudFormation stack)
#    ./deploy.sh test     Llama la API con la API key generada
#    ./deploy.sh outputs  Muestra los outputs del stack
#    ./deploy.sh destroy  Elimina el stack completo
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

STACK_NAME="${STACK_NAME:-joedayz-edge-modulo7}"
REGION="${AWS_REGION:-us-east-1}"

info() { printf "\033[1;34m[info]\033[0m %s\n" "$*"; }
ok()   { printf "\033[1;32m[ok]\033[0m %s\n"   "$*"; }
err()  { printf "\033[1;31m[error]\033[0m %s\n" "$*" >&2; }

require() {
    if ! command -v "$1" >/dev/null 2>&1; then
        err "Falta '$1'. $2"; exit 1
    fi
}

check_prereqs() {
    require aws "Instala AWS CLI v2: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html"
    require sam "Instala AWS SAM CLI: https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html"
    info "AWS CLI:  $(aws --version 2>&1)"
    info "SAM CLI:  $(sam --version 2>&1)"
    info "Verificando credenciales..."
    if ! aws sts get-caller-identity --output table; then
        err "No hay credenciales activas. Ejecuta:  aws configure   (o aws sso login)"
        exit 1
    fi
    info "Region activa: $REGION"
    ok "Prerequisitos OK"
}

show_outputs() {
    aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$REGION" \
        --query 'Stacks[0].Outputs' --output table
}

case "${1:-check}" in
    check) check_prereqs ;;

    deploy)
        check_prereqs
        info "sam build"
        sam build --template template.yaml

        info "sam deploy hacia stack='$STACK_NAME' region='$REGION'"
        sam deploy \
            --stack-name "$STACK_NAME" \
            --region "$REGION" \
            --capabilities CAPABILITY_IAM \
            --resolve-s3 \
            --no-confirm-changeset \
            --no-fail-on-empty-changeset \
            --parameter-overrides \
                OrderBackend="${ORDER_BACKEND:-order-service.internal.joedayz.pe}" \
                InventoryBackend="${INVENTORY_BACKEND:-inventory-service.internal.joedayz.pe}" \
                VpcLinkId="${VPC_LINK_ID:-none}" \
                KeycloakIssuer="${KEYCLOAK_ISSUER:-http://localhost:8180/realms/joedayz-microservices}"
        ok "Stack desplegado"
        show_outputs
        ;;

    outputs) show_outputs ;;

    test)
        require aws "Instala AWS CLI"
        API_URL=$(aws cloudformation describe-stacks --stack-name "$STACK_NAME" --region "$REGION" \
            --query 'Stacks[0].Outputs[?OutputKey==`ApiInvokeUrl`].OutputValue' --output text)
        info "URL: $API_URL"
        curl -i \
            "$API_URL/gateway/inventory/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE" \
            || true
        ;;

    destroy)
        require aws "Instala AWS CLI"
        info "Eliminando stack $STACK_NAME (async)"
        aws cloudformation delete-stack --stack-name "$STACK_NAME" --region "$REGION"
        ok "Stack en eliminacion. Sigue con: aws cloudformation wait stack-delete-complete --stack-name $STACK_NAME"
        ;;

    *)
        echo "Uso: $0 {check|deploy|outputs|test|destroy}" >&2
        exit 1
        ;;
esac
