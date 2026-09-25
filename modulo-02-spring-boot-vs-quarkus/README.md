# Módulo 2 – Workshop Spring Boot vs Quarkus

> **Web MVC · WebFlux · Quarkus REST · Virtual Threads · Native Image**
>
> Java 21 · Spring Boot 4 · Quarkus 3

En el módulo 1 modelamos `Catalog`. Aquí implementamos el mismo servicio cinco
veces para comparar modelos de ejecución sin cambiar el dominio ni el contrato.

Al finalizar habrás comprobado:

1. El mismo resultado en Spring MVC, WebFlux y Quarkus.
2. Aislamiento por `X-Tenant-ID`.
3. Diferencias entre código bloqueante y reactivo.
4. Platform threads frente a virtual threads.
5. Startup y memoria de las cinco variantes.
6. Opcionalmente, un ejecutable nativo de Quarkus.

## Servicios del laboratorio

```text
Spring Boot MVC                http://localhost:8081
Spring Boot WebFlux            http://localhost:8082
Quarkus REST                   http://localhost:8083
Spring Boot MVC + VT           http://localhost:8084
Quarkus REST + VT              http://localhost:8085
```

Cada servicio usa H2 en memoria y carga los mismos cuatro productos:

- `tienda-deportes`: `ZAP-RUN-42`, `MEDIAS-01`;
- `libreria-lima`: `LIB-DDD-01`, `JAVA-25-01`.

No necesitas Docker para el flujo JVM.

## 0. Prerrequisitos

- JDK 21 o superior.
- Maven 3.9 o superior.
- `curl`; en Windows también puedes usar PowerShell 7+.
- Para el benchmark automático: bash, `python3`, `ps` y `lsof`
  (macOS/Linux, Git Bash completo o WSL).
- Para native image: GraalVM 21+ o un motor de contenedores.

```bash
java -version
mvn -version
```

## 1. Compilar y ejecutar las pruebas

Desde la raíz del repositorio ejecuta cada proyecto:

```bash
cd modulo-02-spring-boot-vs-quarkus/spring-boot-mvc/catalog-service
mvn -q test

cd ../../spring-boot-webflux/catalog-service
mvn -q test

cd ../../quarkus/catalog-service
mvn -q test

cd ../../spring-boot-virtual-threads/catalog-service
mvn -q test

cd ../../quarkus-virtual-threads/catalog-service
mvn -q test
```

En PowerShell los mismos comandos funcionan. También puedes abrir una terminal
en cada carpeta y ejecutar únicamente `mvn test`.

## 2. Levantar las cinco implementaciones

Usa cinco terminales y déjalas abiertas.

### Terminal 1 — Spring MVC

```bash
cd modulo-02-spring-boot-vs-quarkus/spring-boot-mvc/catalog-service
mvn spring-boot:run
```

### Terminal 2 — Spring WebFlux

```bash
cd modulo-02-spring-boot-vs-quarkus/spring-boot-webflux/catalog-service
mvn spring-boot:run
```

### Terminal 3 — Quarkus

```bash
cd modulo-02-spring-boot-vs-quarkus/quarkus/catalog-service
mvn quarkus:dev
```

### Terminal 4 — Spring MVC con virtual threads

```bash
cd modulo-02-spring-boot-vs-quarkus/spring-boot-virtual-threads/catalog-service
mvn spring-boot:run
```

### Terminal 5 — Quarkus con virtual threads

```bash
cd modulo-02-spring-boot-vs-quarkus/quarkus-virtual-threads/catalog-service
mvn quarkus:dev
```

> Quarkus dev mode permite editar código y recargar sin reiniciar manualmente.
> Spring Boot se ejecuta aquí en modo normal.

## 3. Verificar health

### macOS / Linux / Git Bash

```bash
curl -sS http://localhost:8081/actuator/health
curl -sS http://localhost:8082/actuator/health
curl -sS http://localhost:8083/q/health
curl -sS http://localhost:8084/actuator/health
curl -sS http://localhost:8085/q/health
```

### Windows PowerShell

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health | ConvertTo-Json -Depth 4
Invoke-RestMethod http://localhost:8082/actuator/health | ConvertTo-Json -Depth 4
Invoke-RestMethod http://localhost:8083/q/health | ConvertTo-Json -Depth 4
Invoke-RestMethod http://localhost:8084/actuator/health | ConvertTo-Json -Depth 4
Invoke-RestMethod http://localhost:8085/q/health | ConvertTo-Json -Depth 4
```

Los cinco deben responder `UP`.

## 4. Comparar el mismo contrato

### 4.1 Listar productos de tienda-deportes

**macOS / Linux / Git Bash**

```bash
for port in 8081 8082 8083 8084 8085; do
  echo "=== puerto $port ==="
  curl -sS -H 'X-Tenant-ID: tienda-deportes' \
    "http://localhost:$port/api/v1/products" | jq
done
```

**Windows PowerShell**

```powershell
8081..8085 | ForEach-Object {
  Write-Host "=== puerto $_ ==="
  Invoke-RestMethod "http://localhost:$_/api/v1/products" `
    -Headers @{ 'X-Tenant-ID' = 'tienda-deportes' } |
    ConvertTo-Json -Depth 5
}
```

Cada implementación debe devolver dos productos, entre ellos:

```json
{
  "sku": "ZAP-RUN-42",
  "name": "Zapatilla Running Pro",
  "price": 300.00,
  "currency": "PEN"
}
```

### 4.2 Consultar un SKU

**macOS / Linux / Git Bash**

```bash
curl -sS -H 'X-Tenant-ID: tienda-deportes' \
  http://localhost:8081/api/v1/products/ZAP-RUN-42 | jq
```

**Windows PowerShell**

```powershell
Invoke-RestMethod http://localhost:8081/api/v1/products/ZAP-RUN-42 `
  -Headers @{ 'X-Tenant-ID' = 'tienda-deportes' } |
  ConvertTo-Json
```

Repite cambiando el puerto. El contrato y los datos deben ser equivalentes.

## 5. Comprobar aislamiento multi-tenant

Pide el mismo catálogo con dos tenants:

### macOS / Linux / Git Bash

```bash
curl -sS -H 'X-Tenant-ID: tienda-deportes' \
  http://localhost:8081/api/v1/products | jq '.[].sku'

curl -sS -H 'X-Tenant-ID: libreria-lima' \
  http://localhost:8081/api/v1/products | jq '.[].sku'
```

### Windows PowerShell

```powershell
(Invoke-RestMethod http://localhost:8081/api/v1/products `
  -Headers @{ 'X-Tenant-ID' = 'tienda-deportes' }).sku

(Invoke-RestMethod http://localhost:8081/api/v1/products `
  -Headers @{ 'X-Tenant-ID' = 'libreria-lima' }).sku
```

`tienda-deportes` no debe ver `LIB-DDD-01`; `libreria-lima` no debe ver
`ZAP-RUN-42`.

Prueba ahora sin header:

```bash
curl -i http://localhost:8081/api/v1/products
```

La petición debe rechazarse. El tenant no es un filtro opcional.

### Punto de código

Compara:

- [`MVC TenantWebFilter`](spring-boot-mvc/catalog-service/src/main/java/pe/joedayz/microservicios/catalog/tenant/TenantWebFilter.java):
  contexto imperativo por petición.
- [`WebFlux TenantWebFilter`](spring-boot-webflux/catalog-service/src/main/java/pe/joedayz/microservicios/catalog/tenant/TenantWebFilter.java):
  guarda el tenant en Reactor Context, no en `ThreadLocal`.
- [`Quarkus TenantRequestFilter`](quarkus/catalog-service/src/main/java/pe/joedayz/microservicios/catalog/tenant/TenantRequestFilter.java):
  captura el header en JAX-RS.

En WebFlux una petición puede cambiar de hilo. Por eso un `ThreadLocal` tradicional
no es una forma segura de transportar contexto reactivo.

## 6. Comparar MVC, WebFlux y Quarkus

Abre los controladores:

- [`Spring MVC ProductController`](spring-boot-mvc/catalog-service/src/main/java/pe/joedayz/microservicios/catalog/api/ProductController.java):
  devuelve `List` y `ResponseEntity`.
- [`Spring WebFlux ProductController`](spring-boot-webflux/catalog-service/src/main/java/pe/joedayz/microservicios/catalog/api/ProductController.java):
  devuelve `Flux` y `Mono`.
- [`Quarkus ProductResource`](quarkus/catalog-service/src/main/java/pe/joedayz/microservicios/catalog/api/ProductResource.java):
  usa JAX-RS y Panache.

Los tres entregan el mismo contrato, pero no el mismo modelo de ejecución:

- MVC es imperativo y bloqueante.
- WebFlux compone un pipeline no bloqueante.
- Quarkus REST integra el endpoint con Vert.x y Panache.

No elijas WebFlux solo porque devuelve `Mono`: aporta valor cuando la cadena
completa, incluida la persistencia, es no bloqueante.

## 7. Workshop de virtual threads

El endpoint didáctico `/_thread` existe en MVC, Quarkus clásico, Spring VT y
Quarkus VT. WebFlux no lo expone porque su comparación se centra en Reactor y
event loops.

### macOS / Linux / Git Bash

```bash
for port in 8081 8083 8084 8085; do
  echo "=== puerto $port ==="
  curl -sS -H 'X-Tenant-ID: tienda-deportes' \
    "http://localhost:$port/api/v1/products/_thread" | jq
done
```

### Windows PowerShell

```powershell
8081,8083,8084,8085 | ForEach-Object {
  Write-Host "=== puerto $_ ==="
  Invoke-RestMethod "http://localhost:$_/api/v1/products/_thread" `
    -Headers @{ 'X-Tenant-ID' = 'tienda-deportes' } |
    ConvertTo-Json
}
```

Resultados relevantes:

- `8081` Spring MVC clásico: `"virtual": false`.
- `8084` Spring con VT: `"virtual": true`.
- `8085` Quarkus con `@RunOnVirtualThread`: `"virtual": true`.
- `8083` sirve como baseline Quarkus sin la anotación.

### Punto de código Spring

Abre
[`application.yml`](spring-boot-virtual-threads/catalog-service/src/main/resources/application.yml):

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

El controller sigue siendo imperativo. Spring cambia el executor.

### Punto de código Quarkus

Abre
[`ProductResource.java`](quarkus-virtual-threads/catalog-service/src/main/java/pe/joedayz/microservicios/catalog/api/ProductResource.java).

`@RunOnVirtualThread` mueve cada endpoint bloqueante a un virtual thread. No
debes ejecutar JDBC bloqueante sobre el event loop.

## 8. Ejecutar el benchmark JVM

Primero detén los cinco servicios con `Ctrl+C`; el script necesita libres los
puertos `8081` a `8085`.

### macOS / Linux / Git Bash / WSL

```bash
cd modulo-02-spring-boot-vs-quarkus/benchmarks
chmod +x run-benchmarks.sh
./run-benchmarks.sh
```

El script:

1. compila los cinco proyectos;
2. arranca cada JAR;
3. espera su health endpoint;
4. mide startup y RSS;
5. detiene el proceso antes de iniciar el siguiente.

La salida tiene esta forma:

```text
Servicio               | Modo     | Startup ms   | RSS MB
Spring Boot MVC        | JVM      | ...          | ...
Spring Boot WebFlux    | JVM      | ...          | ...
Quarkus                | JVM      | ...          | ...
Spring Boot + VT       | JVM      | ...          | ...
Quarkus + VT           | JVM      | ...          | ...
```

Ejecuta tres veces y promedia. Un único arranque está afectado por caché de disco,
descarga de clases y actividad del sistema operativo.

### Windows PowerShell

El script usa herramientas POSIX (`bash`, `ps`, `lsof`, `python3`). Ejecútalo
desde Git Bash o WSL:

```bash
cd /ruta/al/repositorio/modulo-02-spring-boot-vs-quarkus/benchmarks
./run-benchmarks.sh
```

En PowerShell puro puedes comparar manualmente los tiempos que imprimen Spring y
Quarkus al arrancar, pero no será la misma medición automatizada de RSS.

## 9. Build nativo opcional

Lee primero
[`docs/04-graalvm-native-build.md`](docs/04-graalvm-native-build.md).

Con GraalVM instalado:

```bash
cd modulo-02-spring-boot-vs-quarkus/quarkus/catalog-service
mvn package -Dnative -DskipTests \
  -Dquarkus.native.container-build=false
./target/catalog-service-1.0.0-runner
```

Con Docker/Podman:

```bash
mvn package -Dnative -DskipTests \
  -Dquarkus.native.container-build=true
```

El container build produce un binario Linux. En Windows debe ejecutarse desde
WSL/contenedor, no como `.exe` nativo del host.

Para incluirlo en el benchmark:

```bash
cd ../../../benchmarks
./run-benchmarks.sh --native --skip-build
```

## 10. Retos para experimentar

### Reto A: nuevo tenant

Agrega productos de `moda-boutique` a los `data.sql` e `import.sql`. Reinicia los
servicios y consulta usando `X-Tenant-ID: moda-boutique`.

### Reto B: romper el aislamiento

Quita temporalmente el filtro `tenantId` de un repositorio.

Comprobación: un tenant empieza a ver productos ajenos. Restáuralo después; este
es el tipo de defecto que una prueba de aislamiento debe impedir.

### Reto C: comparar código

Implementa una transformación adicional del nombre en los tres controladores.
Compara el estilo imperativo, Reactor y Panache sin cambiar el JSON final.

## Solución de problemas

### Un puerto ya está ocupado

Detén el proceso anterior. En macOS/Linux:

```bash
lsof -iTCP:8081 -sTCP:LISTEN
```

En PowerShell:

```powershell
Get-NetTCPConnection -LocalPort 8081
```

### Un catálogo responde vacío

- Comprueba que enviaste `X-Tenant-ID`.
- Usa exactamente `tienda-deportes` o `libreria-lima`.
- Revisa que `data.sql` o `import.sql` se ejecutó al arrancar.

### El benchmark omite un servicio

- Libera todos los puertos.
- Ejecuta primero sin `--skip-build`.
- Comprueba que se generaron los JAR en cada carpeta `target/`.

### Native image falla por memoria

Asigna más memoria al motor de contenedores o usa GraalVM local. El build nativo
consume bastante más memoria que el build JVM.

## Lecturas del módulo

1. [Comparativa Spring Boot vs Quarkus](docs/01-comparativa-spring-vs-quarkus.md).
2. [Spring Boot Web MVC y WebFlux](docs/02-spring-boot-webmvc-webflux.md).
3. [Quarkus, Panache y REST](docs/03-quarkus-panache-resteasy-reactive.md).
4. [Build nativo con GraalVM](docs/04-graalvm-native-build.md).
5. [Benchmarks de startup y memoria](docs/05-benchmarks-startup-memoria.md).
6. [Virtual Threads](docs/06-virtual-threads.md).

## Siguiente módulo

En el [módulo 3](../modulo-03-comunicacion-sincrona/) Catalog se comunica con
Order e Inventory mediante REST y gRPC.
