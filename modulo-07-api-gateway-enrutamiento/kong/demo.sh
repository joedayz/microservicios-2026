#!/usr/bin/env bash
# ============================================================================
#  Demo Kong Gateway - Modulo 7
#  ---------------------------------------------------------------------------
#  Detecta automaticamente Docker o Podman (incluidos Podman machine en macOS
#  y podman-compose). Todo corre LOCAL - no toca ninguna nube.
#
#  Uso:
#    ./demo.sh up       Arranca Kong (proxy :8000, admin :8001)
#    ./demo.sh status   Muestra servicios, rutas y plugins cargados
#    ./demo.sh smoke    Ejecuta pruebas de humo (routing, WAF, rate-limit)
#    ./demo.sh reload   Recarga kong.yml sin bajar el contenedor
#    ./demo.sh logs     Muestra los logs del proxy en tiempo real
#    ./demo.sh down     Detiene y borra el contenedor + volumenes
# ============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PROXY_URL="${PROXY_URL:-http://localhost:8000}"
ADMIN_URL="${ADMIN_URL:-http://localhost:8001}"

# ---------------------------------------------------------------------------
#  Deteccion del motor de contenedores.
#    - Docker Desktop (Mac / Windows / Linux) -> "docker compose"
#    - Podman >= 4.4 con "podman compose"     -> "podman compose"
#    - podman-compose (script Python)          -> "podman-compose"
# ---------------------------------------------------------------------------
detect_engine() {
    if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
        echo "docker compose"; return
    fi
    if command -v podman >/dev/null 2>&1 && podman compose version >/dev/null 2>&1; then
        echo "podman compose"; return
    fi
    if command -v podman-compose >/dev/null 2>&1; then
        echo "podman-compose"; return
    fi
    echo ""
}

COMPOSE="$(detect_engine)"
if [[ -z "$COMPOSE" ]]; then
    cat >&2 <<EOF
[ERROR] No encontre ni Docker ni Podman con soporte de compose.

Instala uno de estos:
  - Docker Desktop      https://www.docker.com/products/docker-desktop
  - Podman + Machine    https://podman.io/  (macOS: brew install podman)
    Recuerda ejecutar: podman machine init && podman machine start

Luego vuelve a correr:  ./demo.sh up
EOF
    exit 1
fi

echo "[info] Usando motor: $COMPOSE"

wait_for() {
    local url="$1" timeout="${2:-90}" elapsed=0
    echo "[wait] $url ..."
    until curl -sf -o /dev/null "$url"; do
        sleep 2
        elapsed=$((elapsed + 2))
        if [[ $elapsed -ge $timeout ]]; then
            echo "[error] Timeout esperando $url" >&2
            $COMPOSE logs --tail=50 >&2 || true
            exit 1
        fi
    done
    echo "[ok] $url respondio"
}

case "${1:-up}" in
    up)
        echo "=========================================================="
        echo " Paso 1: Bajando cualquier instancia previa"
        echo "=========================================================="
        $COMPOSE down -v 2>/dev/null || true

        echo "=========================================================="
        echo " Paso 2: Levantando Kong 3.7 en modo DB-less"
        echo "=========================================================="
        $COMPOSE up -d

        echo "=========================================================="
        echo " Paso 3: Esperando a que la Admin API responda"
        echo "=========================================================="
        wait_for "$ADMIN_URL/status" 90

        echo "=========================================================="
        echo " Paso 4: Verificando que kong.yml se cargo"
        echo "=========================================================="
        curl -s "$ADMIN_URL/services" | head -c 500 ; echo
        echo
        echo "[LISTO]  Proxy:  $PROXY_URL"
        echo "         Admin:  $ADMIN_URL"
        echo "         Sigue:  ./demo.sh smoke"
        ;;
    status)
        echo "== Services =="; curl -s "$ADMIN_URL/services" | head -c 800; echo
        echo "== Routes ==";   curl -s "$ADMIN_URL/routes"   | head -c 800; echo
        echo "== Plugins ==";  curl -s "$ADMIN_URL/plugins"  | head -c 800; echo
        ;;
    smoke)
        echo "=========================================================="
        echo " Test 1: routing hacia inventory-service"
        echo " (necesita inventory-service-quarkus corriendo en :8084)"
        echo "=========================================================="
        curl -i -H 'X-Tenant-ID: tienda-deportes' \
            "$PROXY_URL/gateway/inventory/api/v1/tenants/tienda-deportes/inventory" \
            || true
        echo
        echo "=========================================================="
        echo " Test 2: WAF - bot-detection debe devolver 403"
        echo "=========================================================="
        curl -i -H 'User-Agent: sqlmap/1.8' -H 'X-Tenant-ID: tienda-deportes' \
            "$PROXY_URL/gateway/orders/api/v1/tenants/tienda-deportes/orders" \
            || true
        echo
        echo "=========================================================="
        echo " Test 3: rate-limit - la #61 dentro de un minuto debe dar 429"
        echo "=========================================================="
        for i in $(seq 1 70); do
            code=$(curl -o /dev/null -s -w '%{http_code}' \
                -H 'X-Tenant-ID: demo-rate' \
                "$PROXY_URL/gateway/inventory/api/v1/tenants/demo-rate/inventory") || true
            if [[ "$code" == "429" ]]; then
                echo "[OK] Recibimos 429 en la request #$i (rate-limit activo)"
                break
            fi
            [[ $((i % 10)) -eq 0 ]] && echo "  request #$i -> $code"
        done
        ;;
    reload)
        echo "[info] Recargando kong.yml sin bajar Kong"
        curl -sf -X POST "$ADMIN_URL/config" -F config=@kong.yml \
            && echo "[ok] Config recargada"
        ;;
    logs)
        $COMPOSE logs -f kong
        ;;
    down)
        $COMPOSE down -v
        echo "[ok] Kong detenido y volumenes borrados"
        ;;
    *)
        echo "Uso: $0 {up|status|smoke|reload|logs|down}" >&2
        exit 1
        ;;
esac
