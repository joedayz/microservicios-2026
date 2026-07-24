#!/usr/bin/env bash
# Benchmarks Modulo 2 - JoeDayz.pe
# Mide startup time (ms) y memoria RSS (MB) de cada microservicio Catalog.
#
# Uso:
#   ./run-benchmarks.sh                      # solo JVM
#   ./run-benchmarks.sh --native             # JVM + Quarkus Native
#   ./run-benchmarks.sh --native --skip-build

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

RUN_NATIVE=false
SKIP_BUILD=false

for arg in "$@"; do
  case "$arg" in
    --native) RUN_NATIVE=true ;;
    --skip-build) SKIP_BUILD=true ;;
    -h|--help)
      cat <<'EOF'
Uso: ./run-benchmarks.sh [--native] [--skip-build]

  sin flags       Mide Spring Boot MVC, WebFlux y Quarkus en modo JVM.
  --native        Igual que arriba + Quarkus Native.
  --skip-build    No ejecuta mvn package.

Quarkus Native en Mac (una vez):
  cd quarkus/catalog-service
  mvn package -Dnative -DskipTests -Dquarkus.native.container-build=false

Libera los puertos 8081-8083 antes de correr el script.
EOF
      exit 0
      ;;
  esac
done

if ! command -v curl >/dev/null 2>&1; then
  echo "curl es requerido"
  exit 1
fi

print_header() {
  printf "\n%-22s | %-8s | %-12s | %-10s\n" "Servicio" "Modo" "Startup ms" "RSS MB"
  printf "%s\n" "-----------------------|----------|--------------|------------"
}

port_in_use() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
  else
    return 1
  fi
}

rss_mb_of() {
  local pid="$1"
  local rss_kb
  rss_kb=$(ps -o rss= -p "$pid" 2>/dev/null | tr -d ' ' || true)
  if [[ -z "$rss_kb" ]]; then
    rss_kb=0
  fi
  python3 -c "print(round(${rss_kb}/1024, 1))"
}

measure_jar() {
  local name="$1"
  local jar="$2"
  local health_url="$3"
  local port="$4"

  if [[ ! -f "$jar" ]]; then
    echo "SKIP $name: no existe $jar"
    return 0
  fi

  if port_in_use "$port"; then
    echo "SKIP $name JVM: puerto $port ocupado. Deten el proceso y reintenta."
    return 0
  fi

  local log
  log="$(mktemp)"
  local start end elapsed pid rss_mb

  start=$(python3 -c 'import time; print(int(time.time()*1000))')
  SERVER_PORT="$port" java -jar "$jar" >"$log" 2>&1 &
  pid=$!

  local attempts=0
  until curl -sf "$health_url" >/dev/null 2>&1; do
    sleep 0.1
    attempts=$((attempts + 1))
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "ERROR $name: proceso murio. Log:"
      tail -20 "$log"
      rm -f "$log"
      return 0
    fi
    if [[ $attempts -gt 150 ]]; then
      echo "ERROR $name: timeout esperando health"
      kill "$pid" 2>/dev/null || true
      wait "$pid" 2>/dev/null || true
      rm -f "$log"
      return 0
    fi
  done

  end=$(python3 -c 'import time; print(int(time.time()*1000))')
  elapsed=$((end - start))

  sleep 1
  if ! kill -0 "$pid" 2>/dev/null; then
    echo "ERROR $name: proceso murio tras health. Puerto $port ya en uso? Log:"
    tail -20 "$log"
    rm -f "$log"
    return 0
  fi
  rss_mb=$(rss_mb_of "$pid")

  printf "%-22s | %-8s | %-12s | %-10s\n" "$name" "JVM" "$elapsed" "$rss_mb"

  kill "$pid" 2>/dev/null || true
  wait "$pid" 2>/dev/null || true
  rm -f "$log"
}

measure_native() {
  local name="$1"
  local runner="$2"
  local health_url="$3"
  local port="$4"

  if [[ ! -f "$runner" ]]; then
    echo "SKIP $name native: compila con:"
    echo "  cd quarkus/catalog-service && mvn package -Dnative -DskipTests -Dquarkus.native.container-build=false"
    return 0
  fi

  if port_in_use "$port"; then
    echo "SKIP $name native: puerto $port ocupado. Deten el proceso y reintenta."
    return 0
  fi

  local log
  log="$(mktemp)"
  local start end elapsed pid rss_mb

  start=$(python3 -c 'import time; print(int(time.time()*1000))')
  SERVER_PORT="$port" "$runner" >"$log" 2>&1 &
  pid=$!

  local attempts=0
  until curl -sf "$health_url" >/dev/null 2>&1; do
    sleep 0.05
    attempts=$((attempts + 1))
    if ! kill -0 "$pid" 2>/dev/null; then
      echo "ERROR $name native: proceso murio. Log:"
      tail -20 "$log"
      rm -f "$log"
      return 0
    fi
    if [[ $attempts -gt 200 ]]; then
      echo "ERROR $name native: timeout"
      kill "$pid" 2>/dev/null || true
      wait "$pid" 2>/dev/null || true
      rm -f "$log"
      return 0
    fi
  done

  end=$(python3 -c 'import time; print(int(time.time()*1000))')
  elapsed=$((end - start))

  sleep 0.5
  if ! kill -0 "$pid" 2>/dev/null; then
    echo "ERROR $name native: proceso murio tras health. Puerto $port ya en uso? Log:"
    tail -20 "$log"
    rm -f "$log"
    return 0
  fi
  rss_mb=$(rss_mb_of "$pid")

  printf "%-22s | %-8s | %-12s | %-10s\n" "$name" "Native" "$elapsed" "$rss_mb"

  kill "$pid" 2>/dev/null || true
  wait "$pid" 2>/dev/null || true
  rm -f "$log"
}

MVC_JAR="$MODULE_DIR/spring-boot-mvc/catalog-service/target/catalog-service-mvc-1.0.0.jar"
FLUX_JAR="$MODULE_DIR/spring-boot-webflux/catalog-service/target/catalog-service-webflux-1.0.0.jar"
Q_JAR="$MODULE_DIR/quarkus/catalog-service/target/quarkus-app/quarkus-run.jar"
Q_RUNNER="$MODULE_DIR/quarkus/catalog-service/target/catalog-service-1.0.0-runner"

echo "============================================================"
echo " Benchmarks Modulo 2 - Catalog Service - JoeDayz.pe"
echo " Java: $(java -version 2>&1 | head -1)"
if [[ "$RUN_NATIVE" == true ]]; then
  echo " Modo: JVM + Quarkus Native"
else
  echo " Modo: solo JVM  -  agrega --native para incluir Quarkus Native"
fi
echo "============================================================"

if [[ "$SKIP_BUILD" == false ]]; then
  echo ""
  echo "Compilando proyectos - mvn package -DskipTests ..."
  (cd "$MODULE_DIR/spring-boot-mvc/catalog-service" && mvn -q package -DskipTests)
  (cd "$MODULE_DIR/spring-boot-webflux/catalog-service" && mvn -q package -DskipTests)
  (cd "$MODULE_DIR/quarkus/catalog-service" && mvn -q package -DskipTests)
  echo "Compilacion OK."
fi

if [[ "$RUN_NATIVE" == true && ! -f "$Q_RUNNER" ]]; then
  echo ""
  echo "AVISO: no hay runner native en:"
  echo "  $Q_RUNNER"
  echo "Compilalo antes en Mac:"
  echo "  cd quarkus/catalog-service && mvn package -Dnative -DskipTests -Dquarkus.native.container-build=false"
  echo "El resto del benchmark JVM se ejecutara igual."
fi

print_header
measure_jar "Spring Boot MVC" "$MVC_JAR" "http://localhost:8081/actuator/health" 8081
measure_jar "Spring Boot WebFlux" "$FLUX_JAR" "http://localhost:8082/actuator/health" 8082
measure_jar "Quarkus" "$Q_JAR" "http://localhost:8083/q/health" 8083

if [[ "$RUN_NATIVE" == true ]]; then
  measure_native "Quarkus" "$Q_RUNNER" "http://localhost:8083/q/health" 8083
fi

echo ""
echo "Nota: valores orientativos. Ejecuta 3 veces y promedia para clase."
if [[ "$RUN_NATIVE" == false ]]; then
  echo "Para incluir native: $0 --native --skip-build"
fi
