#!/usr/bin/env bash
# Benchmarks Modulo 2 - JoeDayz.pe
# Mide startup time (ms) y memoria RSS (MB) de cada microservicio Catalog.
#
# Uso:
#   ./run-benchmarks.sh
#   ./run-benchmarks.sh --native
#   ./run-benchmarks.sh --skip-build

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
      echo "Uso: $0 [--native] [--skip-build]"
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

measure_jar() {
  local name="$1"
  local jar="$2"
  local health_url="$3"
  local port="$4"

  if [[ ! -f "$jar" ]]; then
    echo "SKIP $name: no existe $jar"
    return
  fi

  local log
  log="$(mktemp)"
  local start end elapsed pid rss_kb rss_mb

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
      return
    fi
    if [[ $attempts -gt 150 ]]; then
      echo "ERROR $name: timeout esperando health"
      kill "$pid" 2>/dev/null || true
      rm -f "$log"
      return
    fi
  done

  end=$(python3 -c 'import time; print(int(time.time()*1000))')
  elapsed=$((end - start))

  sleep 1
  if [[ "$(uname)" == "Darwin" ]]; then
    rss_kb=$(ps -o rss= -p "$pid" | tr -d ' ')
  else
    rss_kb=$(ps -o rss= -p "$pid" | tr -d ' ')
  fi
  rss_mb=$(python3 -c "print(round(${rss_kb:-0}/1024, 1))")

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
    echo "  cd quarkus/catalog-service && mvn package -Dnative -DskipTests"
    return
  fi

  local log
  log="$(mktemp)"
  local start end elapsed pid rss_kb rss_mb

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
      return
    fi
    if [[ $attempts -gt 200 ]]; then
      echo "ERROR $name native: timeout"
      kill "$pid" 2>/dev/null || true
      rm -f "$log"
      return
    fi
  done

  end=$(python3 -c 'import time; print(int(time.time()*1000))')
  elapsed=$((end - start))

  sleep 0.5
  rss_kb=$(ps -o rss= -p "$pid" | tr -d ' ')
  rss_mb=$(python3 -c "print(round(${rss_kb:-0}/1024, 1))")

  printf "%-22s | %-8s | %-12s | %-10s\n" "$name" "Native" "$elapsed" "$rss_mb"

  kill "$pid" 2>/dev/null || true
  wait "$pid" 2>/dev/null || true
  rm -f "$log"
}

echo "============================================================"
echo " Benchmarks Modulo 2 - Catalog Service (JoeDayz.pe)"
echo " Java: $(java -version 2>&1 | head -1)"
echo "============================================================"

if [[ "$SKIP_BUILD" == false ]]; then
  echo ""
  echo "Compilando proyectos (mvn package -DskipTests)..."
  (cd "$MODULE_DIR/spring-boot-mvc/catalog-service" && mvn -q package -DskipTests)
  (cd "$MODULE_DIR/spring-boot-webflux/catalog-service" && mvn -q package -DskipTests)
  (cd "$MODULE_DIR/quarkus/catalog-service" && mvn -q package -DskipTests)
  echo "Compilacion OK."
fi

MVC_JAR="$MODULE_DIR/spring-boot-mvc/catalog-service/target/catalog-service-mvc-1.0.0.jar"
FLUX_JAR="$MODULE_DIR/spring-boot-webflux/catalog-service/target/catalog-service-webflux-1.0.0.jar"
Q_RUNNER="$MODULE_DIR/quarkus/catalog-service/target/catalog-service-1.0.0-runner"

print_header
measure_jar "Spring Boot MVC" "$MVC_JAR" "http://localhost:8081/actuator/health" 8081
measure_jar "Spring Boot WebFlux" "$FLUX_JAR" "http://localhost:8082/actuator/health" 8082
measure_jar "Quarkus" "$MODULE_DIR/quarkus/catalog-service/target/quarkus-app/quarkus-run.jar" "http://localhost:8083/q/health" 8083

if [[ "$RUN_NATIVE" == true ]]; then
  measure_native "Quarkus" "$Q_RUNNER" "http://localhost:8083/q/health" 8083
fi

echo ""
echo "Nota: valores orientativos. Ejecuta 3 veces y promedia para clase."
echo "Para Quarkus native: $0 --native (tras mvn package -Dnative)"
