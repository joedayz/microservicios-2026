# Módulo 5 – Workshop de persistencia políglota

> **PostgreSQL · MongoDB · Redis · Flyway · Liquibase**
>
> Java 21 · Spring Boot 4 · Quarkus 3

Este workshop persiste los datos del e-commerce multi-tenant con la tecnología
adecuada para cada caso:

```text
Cliente + X-Tenant-ID
├─ catalog-service-spring      :8081 ─ PostgreSQL + JPA + Flyway
│                                      └─ Redis: caché + rate limit
├─ inventory-service-quarkus   :8084 ─ PostgreSQL + Panache + Liquibase
└─ notification-service-spring :8087 ─ MongoDB
```

Catalog e inventory usan una base PostgreSQL distinta por tenant. Notification
calcula una base MongoDB `notifications_<tenant_normalizado>`. Redis separa sus
claves de caché y rate limit por tenant.

Al finalizar habrás:

1. levantado y comprobado PostgreSQL, MongoDB y Redis;
2. iniciado los tres microservicios desde Maven;
3. observado migraciones Flyway y Liquibase;
4. persistido con JPA, Panache y Spring Data MongoDB;
5. comprobado caché, rate limiting y aislamiento database-per-tenant;
6. inspeccionado los datos con las herramientas incluidas en los contenedores.

## 0. Prerrequisitos

Necesitas:

- JDK 21 o superior;
- Maven 3.9 o superior;
- Docker Desktop en Windows/macOS, o Docker/Podman en Linux;
- puertos libres `5432`, `6379`, `8081`, `8084`, `8087` y `27017`;
- `curl` en macOS/Linux; Windows PowerShell 7+ usa `Invoke-RestMethod`.

Ejecuta los comandos desde la raíz del repositorio
`microservicios-2026`, salvo que un paso indique otra terminal.

### Comprobar herramientas

**macOS / Linux**

```bash
java -version
mvn -version
docker version
docker compose version
curl --version
```

**Windows PowerShell**

```powershell
java -version
mvn -version
docker version
docker compose version
$PSVersionTable.PSVersion
```

> Con Podman sustituye `docker compose` por `podman compose` y `docker exec`
> por `podman exec` en todo el workshop.

## 1. Levantar PostgreSQL, MongoDB y Redis

Abre la **Terminal 1**.

**Windows, macOS y Linux con Docker**

```bash
cd modulo-05-persistencia-poliglota/docker-compose
docker compose up -d
docker compose ps
```

**macOS o Linux con Podman**

```bash
cd modulo-05-persistencia-poliglota/docker-compose
podman compose up -d
podman compose ps
```

Debes ver `modulo5-postgres`, `modulo5-redis` y `modulo5-mongo`. Espera a que
los tres aparezcan como `healthy`.

El compose usa exactamente las herramientas que traen sus imágenes para los
health checks. Compruébalas también de forma explícita:

**Docker (también desde PowerShell)**

```bash
docker exec modulo5-postgres pg_isready -U postgres -d postgres
docker exec modulo5-redis redis-cli ping
docker exec modulo5-mongo mongosh --quiet --eval "db.adminCommand('ping').ok"
```

**Podman**

```bash
podman exec modulo5-postgres pg_isready -U postgres -d postgres
podman exec modulo5-redis redis-cli ping
podman exec modulo5-mongo mongosh --quiet --eval "db.adminCommand('ping').ok"
```

Los resultados esperados son:

- PostgreSQL: `accepting connections`;
- Redis: `PONG`;
- MongoDB: `1`.

El script
[`01-create-databases.sql`](docker-compose/postgres/init/01-create-databases.sql)
crea estas cuatro bases al inicializar el contenedor:

```text
catalog_tienda_deportes
catalog_libreria_lima
inventory_tienda_deportes
inventory_libreria_lima
```

MongoDB crea cada base de notificaciones al guardar el primer documento.

## 2. Iniciar catalog-service-spring

Abre la **Terminal 2** y déjala ejecutándose:

```bash
cd modulo-05-persistencia-poliglota/catalog-service-spring
mvn spring-boot:run
```

Al arrancar, Flyway ejecuta
[`V1__create_products.sql`](catalog-service-spring/src/main/resources/db/migration/V1__create_products.sql)
en las dos bases de catálogo y `DemoDataSeeder` carga dos productos diferentes
en cada una.

Comprueba el servicio desde otra terminal.

**macOS / Linux**

```bash
curl -sS http://localhost:8081/actuator/health
```

**Windows PowerShell**

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health |
  ConvertTo-Json -Depth 5
```

El estado esperado es `UP`.

## 3. Iniciar inventory-service-quarkus

Abre la **Terminal 3** y déjala ejecutándose:

```bash
cd modulo-05-persistencia-poliglota/inventory-service-quarkus
mvn quarkus:dev
```

Liquibase crea `inventory_items` en las dos bases de inventory.
`InventorySeedData` carga stock deportivo en una y libros en la otra.

**macOS / Linux**

```bash
curl -sS http://localhost:8084/q/health/ready
```

**Windows PowerShell**

```powershell
Invoke-RestMethod http://localhost:8084/q/health/ready |
  ConvertTo-Json -Depth 5
```

El estado esperado es `UP`.

## 4. Iniciar notification-service-spring

Abre la **Terminal 4** y déjala ejecutándose:

```bash
cd modulo-05-persistencia-poliglota/notification-service-spring
mvn spring-boot:run
```

**macOS / Linux**

```bash
curl -sS http://localhost:8087/actuator/health
```

**Windows PowerShell**

```powershell
Invoke-RestMethod http://localhost:8087/actuator/health |
  ConvertTo-Json -Depth 5
```

El estado esperado es `UP`.

## 5. Experimento Catalog: JPA + Flyway

El mismo endpoint selecciona PostgreSQL con `X-Tenant-ID`.

### macOS / Linux

```bash
curl -sS -H 'X-Tenant-ID: tienda-deportes' \
  http://localhost:8081/api/v1/products

curl -sS -H 'X-Tenant-ID: libreria-lima' \
  http://localhost:8081/api/v1/products
```

### Windows PowerShell

```powershell
Invoke-RestMethod http://localhost:8081/api/v1/products `
  -Headers @{ 'X-Tenant-ID' = 'tienda-deportes' } |
  ConvertTo-Json -Depth 5

Invoke-RestMethod http://localhost:8081/api/v1/products `
  -Headers @{ 'X-Tenant-ID' = 'libreria-lima' } |
  ConvertTo-Json -Depth 5
```

Resultados esperados:

- `tienda-deportes`: `ZAP-RUN-42` y `BAL-FUT-01`;
- `libreria-lima`: `LIB-DDD-01` y `JAVA-25-01`.

Crea un libro mediante JPA.

**macOS / Linux**

```bash
curl -sS -X POST http://localhost:8081/api/v1/products \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: libreria-lima' \
  -d '{
    "sku":"LIB-MSA-01",
    "name":"Microservices Architecture",
    "description":"Libro del workshop",
    "category":"libros",
    "price":135.00,
    "currency":"PEN"
  }'
```

**Windows PowerShell**

```powershell
$Product = @{
  sku = 'LIB-MSA-01'
  name = 'Microservices Architecture'
  description = 'Libro del workshop'
  category = 'libros'
  price = 135.00
  currency = 'PEN'
} | ConvertTo-Json

Invoke-RestMethod -Method Post http://localhost:8081/api/v1/products `
  -ContentType 'application/json' `
  -Headers @{ 'X-Tenant-ID' = 'libreria-lima' } `
  -Body $Product
```

La respuesta HTTP es `201 Created` y contiene `LIB-MSA-01`.

### Inspeccionar JPA y Flyway con psql

`psql` está dentro de `modulo5-postgres`; no necesitas instalarlo en el host.

**macOS / Linux**

```bash
docker exec modulo5-postgres psql -U postgres -d catalog_libreria_lima \
  -c "TABLE products;"
docker exec modulo5-postgres psql -U postgres -d catalog_libreria_lima \
  -c 'SELECT installed_rank, version, description, success FROM "flyway_schema_history";'
docker exec modulo5-postgres psql -U postgres -d catalog_tienda_deportes \
  -c "SELECT sku, name FROM products ORDER BY sku;"
```

**Windows PowerShell**

```powershell
docker exec modulo5-postgres psql -U postgres -d catalog_libreria_lima -c "TABLE products;"
docker exec modulo5-postgres psql -U postgres -d catalog_libreria_lima -c 'SELECT installed_rank, version, description, success FROM "flyway_schema_history";'
docker exec modulo5-postgres psql -U postgres -d catalog_tienda_deportes -c "SELECT sku, name FROM products ORDER BY sku;"
```

Con Podman cambia `docker` por `podman`. Debes ver el libro nuevo solo en
`catalog_libreria_lima`; Flyway debe registrar la versión `1` como exitosa.

### Punto de código

- [`TenantWebFilter.java`](catalog-service-spring/src/main/java/pe/joedayz/microservicios/catalog/tenant/TenantWebFilter.java)
  exige `X-Tenant-ID`.
- [`DataSourceConfig.java`](catalog-service-spring/src/main/java/pe/joedayz/microservicios/catalog/config/DataSourceConfig.java)
  normaliza el tenant y enruta JPA a su `DataSource`.
- [`FlywayMigrationConfig.java`](catalog-service-spring/src/main/java/pe/joedayz/microservicios/catalog/config/FlywayMigrationConfig.java)
  migra cada datasource antes de publicar el datasource enrutado.
- [`DemoDataSeeder.java`](catalog-service-spring/src/main/java/pe/joedayz/microservicios/catalog/config/DemoDataSeeder.java)
  carga datos distintos por tenant.

## 6. Experimento Catalog: caché Redis

Lee dos veces el mismo SKU.

**macOS / Linux**

```bash
curl -sS -H 'X-Tenant-ID: libreria-lima' \
  http://localhost:8081/api/v1/products/LIB-MSA-01
curl -sS -H 'X-Tenant-ID: libreria-lima' \
  http://localhost:8081/api/v1/products/LIB-MSA-01
```

**Windows PowerShell**

```powershell
1..2 | ForEach-Object {
  Invoke-RestMethod http://localhost:8081/api/v1/products/LIB-MSA-01 `
    -Headers @{ 'X-Tenant-ID' = 'libreria-lima' }
}
```

Inspecciona Redis con la herramienta incluida en el contenedor:

```bash
docker exec modulo5-redis redis-cli --scan --pattern 'catalog-*'
docker exec modulo5-redis redis-cli TTL \
  'catalog-product-by-sku::libreria_lima:sku:LIB-MSA-01'
```

En PowerShell ejecuta el segundo comando en una sola línea:

```powershell
docker exec modulo5-redis redis-cli TTL 'catalog-product-by-sku::libreria_lima:sku:LIB-MSA-01'
```

El primer comando debe mostrar una clave que contiene
`libreria_lima:sku:LIB-MSA-01`. El TTL debe ser positivo y no mayor a `300`
segundos.

[`ProductService.java`](catalog-service-spring/src/main/java/pe/joedayz/microservicios/catalog/service/ProductService.java)
usa `@Cacheable`; [`ProductCacheKeys.java`](catalog-service-spring/src/main/java/pe/joedayz/microservicios/catalog/service/ProductCacheKeys.java)
incluye el tenant en la clave. Al crear un producto, el servicio invalida la
caché del listado del tenant.

## 7. Experimento Catalog: rate limit en Redis

La configuración permite 30 solicitudes por minuto, por tenant y por IP. Envía
35 solicitudes seguidas; las lecturas anteriores de este minuto también cuentan.

**macOS / Linux**

```bash
for i in $(seq 1 35); do
  curl -sS -o /dev/null -w '%{http_code}\n' \
    -H 'X-Tenant-ID: tienda-deportes' \
    http://localhost:8081/api/v1/products
done
```

**Windows PowerShell 7+**

```powershell
1..35 | ForEach-Object {
  $Response = Invoke-WebRequest http://localhost:8081/api/v1/products `
    -Headers @{ 'X-Tenant-ID' = 'tienda-deportes' } `
    -SkipHttpErrorCheck
  $Response.StatusCode
}
```

Al principio verás `200`; al superar el límite verás `429`. Inspecciona los
contadores y su vencimiento:

```bash
docker exec modulo5-redis redis-cli --scan \
  --pattern 'catalog:rate-limit:tienda_deportes:*'
```

Las claves incluyen tenant, IP cliente y minuto. Expiran automáticamente en dos
minutos. Espera al siguiente minuto antes de continuar si este experimento
bloqueó temporalmente el tenant.

[`RateLimitService.java`](catalog-service-spring/src/main/java/pe/joedayz/microservicios/catalog/config/RateLimitService.java)
hace el incremento atómico y
[`RedisRateLimitFilter.java`](catalog-service-spring/src/main/java/pe/joedayz/microservicios/catalog/config/RedisRateLimitFilter.java)
responde `429`.

## 8. Experimento Inventory: Panache + Liquibase

Consulta y reserva dos libros.

### macOS / Linux

```bash
curl -sS -H 'X-Tenant-ID: libreria-lima' \
  http://localhost:8084/api/v1/inventory/LIB-DDD-01

curl -sS -X POST \
  http://localhost:8084/api/v1/inventory/LIB-DDD-01/reserve \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: libreria-lima' \
  -d '{"quantity":2}'
```

### Windows PowerShell

```powershell
Invoke-RestMethod http://localhost:8084/api/v1/inventory/LIB-DDD-01 `
  -Headers @{ 'X-Tenant-ID' = 'libreria-lima' } |
  ConvertTo-Json

Invoke-RestMethod -Method Post `
  http://localhost:8084/api/v1/inventory/LIB-DDD-01/reserve `
  -ContentType 'application/json' `
  -Headers @{ 'X-Tenant-ID' = 'libreria-lima' } `
  -Body '{"quantity":2}' |
  ConvertTo-Json
```

Antes de reservar, `availableQuantity` es `15` y `reservedQuantity` es `0`.
Después deben ser `13` y `2`.

### Inspeccionar Panache y Liquibase con psql

**macOS / Linux**

```bash
docker exec modulo5-postgres psql -U postgres -d inventory_libreria_lima \
  -c "TABLE inventory_items;"
docker exec modulo5-postgres psql -U postgres -d inventory_libreria_lima \
  -c 'SELECT id, author, exectype FROM databasechangelog;'
docker exec modulo5-postgres psql -U postgres -d inventory_tienda_deportes \
  -c "TABLE inventory_items;"
```

**Windows PowerShell**

```powershell
docker exec modulo5-postgres psql -U postgres -d inventory_libreria_lima -c "TABLE inventory_items;"
docker exec modulo5-postgres psql -U postgres -d inventory_libreria_lima -c "SELECT id, author, exectype FROM databasechangelog;"
docker exec modulo5-postgres psql -U postgres -d inventory_tienda_deportes -c "TABLE inventory_items;"
```

Liquibase debe mostrar el changeset `1-create-inventory-items` con
`EXECTYPE=EXECUTED`. La base deportiva conserva sus SKU y cantidades.

### Punto de código

- [`TenantRequestFilter.java`](inventory-service-quarkus/src/main/java/pe/joedayz/microservicios/inventory/tenant/TenantRequestFilter.java)
  captura el header.
- [`InventoryTenantResolver.java`](inventory-service-quarkus/src/main/java/pe/joedayz/microservicios/inventory/tenant/InventoryTenantResolver.java)
  entrega a Hibernate la clave del datasource.
- [`InventoryItem.java`](inventory-service-quarkus/src/main/java/pe/joedayz/microservicios/inventory/domain/InventoryItem.java)
  extiende `PanacheEntityBase`.
- [`InventoryService.java`](inventory-service-quarkus/src/main/java/pe/joedayz/microservicios/inventory/service/InventoryService.java)
  actualiza stock dentro de `@Transactional`.
- [`db/changelog.xml`](inventory-service-quarkus/src/main/resources/db/changelog.xml)
  versiona la tabla.

## 9. Experimento Notification: documentos MongoDB

Crea una notificación para cada tenant.

### macOS / Linux

```bash
curl -sS -X POST http://localhost:8087/api/v1/notifications \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: tienda-deportes' \
  -d '{
    "customerId":"cust-sports-workshop",
    "channel":"EMAIL",
    "subject":"Pedido confirmado",
    "body":"Tu pedido deportivo fue confirmado"
  }'

curl -sS -X POST http://localhost:8087/api/v1/notifications \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: libreria-lima' \
  -d '{
    "customerId":"cust-books-workshop",
    "channel":"EMAIL",
    "subject":"Libro disponible",
    "body":"Tu libro ya esta disponible"
  }'
```

### Windows PowerShell

```powershell
$SportsNotification = @{
  customerId = 'cust-sports-workshop'
  channel = 'EMAIL'
  subject = 'Pedido confirmado'
  body = 'Tu pedido deportivo fue confirmado'
} | ConvertTo-Json

Invoke-RestMethod -Method Post http://localhost:8087/api/v1/notifications `
  -ContentType 'application/json' `
  -Headers @{ 'X-Tenant-ID' = 'tienda-deportes' } `
  -Body $SportsNotification

$BookNotification = @{
  customerId = 'cust-books-workshop'
  channel = 'EMAIL'
  subject = 'Libro disponible'
  body = 'Tu libro ya esta disponible'
} | ConvertTo-Json

Invoke-RestMethod -Method Post http://localhost:8087/api/v1/notifications `
  -ContentType 'application/json' `
  -Headers @{ 'X-Tenant-ID' = 'libreria-lima' } `
  -Body $BookNotification
```

Cada respuesta es `201 Created`, tiene un `id`, conserva el `tenantId` y nace
con `status: PENDING`.

Lista cada tenant:

**macOS / Linux**

```bash
curl -sS -H 'X-Tenant-ID: tienda-deportes' \
  http://localhost:8087/api/v1/notifications
curl -sS -H 'X-Tenant-ID: libreria-lima' \
  http://localhost:8087/api/v1/notifications
```

**Windows PowerShell**

```powershell
Invoke-RestMethod http://localhost:8087/api/v1/notifications `
  -Headers @{ 'X-Tenant-ID' = 'tienda-deportes' } |
  ConvertTo-Json -Depth 5
Invoke-RestMethod http://localhost:8087/api/v1/notifications `
  -Headers @{ 'X-Tenant-ID' = 'libreria-lima' } |
  ConvertTo-Json -Depth 5
```

### Inspeccionar documentos con mongosh

`mongosh` ya está dentro de `modulo5-mongo`.

**macOS / Linux**

```bash
docker exec modulo5-mongo mongosh --quiet --eval \
  "db.getSiblingDB('notifications_tienda_deportes').notifications.find({}, {_id:0, tenantId:1, customerId:1, status:1}).toArray()"
docker exec modulo5-mongo mongosh --quiet --eval \
  "db.getSiblingDB('notifications_libreria_lima').notifications.find({}, {_id:0, tenantId:1, customerId:1, status:1}).toArray()"
```

**Windows PowerShell**

```powershell
docker exec modulo5-mongo mongosh --quiet --eval "db.getSiblingDB('notifications_tienda_deportes').notifications.find({}, {_id:0, tenantId:1, customerId:1, status:1}).toArray()"
docker exec modulo5-mongo mongosh --quiet --eval "db.getSiblingDB('notifications_libreria_lima').notifications.find({}, {_id:0, tenantId:1, customerId:1, status:1}).toArray()"
```

En la primera base debe aparecer `cust-sports-workshop`; en la segunda,
`cust-books-workshop`.

[`TenantMongoTemplateFactory.java`](notification-service-spring/src/main/java/pe/joedayz/microservicios/notification/tenant/TenantMongoTemplateFactory.java)
crea y reutiliza un `MongoTemplate` por base;
[`NotificationMessage.java`](notification-service-spring/src/main/java/pe/joedayz/microservicios/notification/domain/NotificationMessage.java)
declara la colección `notifications`; y
[`NotificationService.java`](notification-service-spring/src/main/java/pe/joedayz/microservicios/notification/service/NotificationService.java)
guarda y ordena los documentos.

## 10. Comprobar el aislamiento database-per-tenant

La prueba no depende solo de las respuestas HTTP: consulta directamente cada
motor.

**macOS / Linux**

```bash
docker exec modulo5-postgres psql -U postgres -d catalog_libreria_lima \
  -c "SELECT sku FROM products WHERE sku = 'LIB-MSA-01';"
docker exec modulo5-postgres psql -U postgres -d catalog_tienda_deportes \
  -c "SELECT sku FROM products WHERE sku = 'LIB-MSA-01';"

docker exec modulo5-postgres psql -U postgres -d inventory_libreria_lima \
  -c "SELECT sku, available_quantity, reserved_quantity FROM inventory_items ORDER BY sku;"
docker exec modulo5-postgres psql -U postgres -d inventory_tienda_deportes \
  -c "SELECT sku, available_quantity, reserved_quantity FROM inventory_items ORDER BY sku;"

docker exec modulo5-mongo mongosh --quiet --eval \
  "db.getSiblingDB('notifications_tienda_deportes').notifications.countDocuments({customerId:'cust-books-workshop'})"
docker exec modulo5-mongo mongosh --quiet --eval \
  "db.getSiblingDB('notifications_libreria_lima').notifications.countDocuments({customerId:'cust-books-workshop'})"
```

**Windows PowerShell**

```powershell
docker exec modulo5-postgres psql -U postgres -d catalog_libreria_lima -c "SELECT sku FROM products WHERE sku = 'LIB-MSA-01';"
docker exec modulo5-postgres psql -U postgres -d catalog_tienda_deportes -c "SELECT sku FROM products WHERE sku = 'LIB-MSA-01';"
docker exec modulo5-postgres psql -U postgres -d inventory_libreria_lima -c "SELECT sku, available_quantity, reserved_quantity FROM inventory_items ORDER BY sku;"
docker exec modulo5-postgres psql -U postgres -d inventory_tienda_deportes -c "SELECT sku, available_quantity, reserved_quantity FROM inventory_items ORDER BY sku;"
docker exec modulo5-mongo mongosh --quiet --eval "db.getSiblingDB('notifications_tienda_deportes').notifications.countDocuments({customerId:'cust-books-workshop'})"
docker exec modulo5-mongo mongosh --quiet --eval "db.getSiblingDB('notifications_libreria_lima').notifications.countDocuments({customerId:'cust-books-workshop'})"
```

Resultados esperados:

- `LIB-MSA-01` devuelve una fila en `catalog_libreria_lima` y ninguna en
  `catalog_tienda_deportes`;
- inventory muestra SKU de libros y deportes en bases separadas;
- el documento de libros cuenta `0` en la base deportiva y `1` en la base de
  librería;
- no existe una columna `tenant_id` que mezcle tenants dentro de esas tablas.

El aislamiento es físico por base, pero los tres servicios siguen dependiendo
de resolver correctamente `X-Tenant-ID` antes de acceder a datos.

> **Riesgo que debes reconocer:** el filtro comprueba que el header no esté
> vacío, pero no mantiene una lista común de tenants autorizados. En Catalog,
> un tenant desconocido no coincide con ningún datasource y
> `AbstractRoutingDataSource` puede usar el datasource por defecto. Notification
> puede crear una base `notifications_<tenant>` nueva. Inventory, en cambio,
> falla si la clave no corresponde a un tenant Hibernate configurado. Por eso
> este laboratorio demuestra separación física, pero todavía no una frontera
> de seguridad completa. En producción valida el tenant contra un registro
> autorizado y rechaza cualquier valor desconocido antes de resolver la base.

## 11. Restaurar los datos del laboratorio

Estos comandos eliminan solo los datos creados por este workshop y restauran la
reserva de ejemplo.

**macOS / Linux**

```bash
docker exec modulo5-postgres psql -U postgres -d catalog_libreria_lima \
  -c "DELETE FROM products WHERE sku = 'LIB-MSA-01';"
docker exec modulo5-postgres psql -U postgres -d inventory_libreria_lima \
  -c "UPDATE inventory_items SET available_quantity = 15, reserved_quantity = 0 WHERE sku = 'LIB-DDD-01';"
docker exec modulo5-mongo mongosh --quiet --eval \
  "db.getSiblingDB('notifications_tienda_deportes').notifications.deleteMany({customerId:'cust-sports-workshop'})"
docker exec modulo5-mongo mongosh --quiet --eval \
  "db.getSiblingDB('notifications_libreria_lima').notifications.deleteMany({customerId:'cust-books-workshop'})"
docker exec modulo5-redis redis-cli FLUSHDB
```

**Windows PowerShell**

```powershell
docker exec modulo5-postgres psql -U postgres -d catalog_libreria_lima -c "DELETE FROM products WHERE sku = 'LIB-MSA-01';"
docker exec modulo5-postgres psql -U postgres -d inventory_libreria_lima -c "UPDATE inventory_items SET available_quantity = 15, reserved_quantity = 0 WHERE sku = 'LIB-DDD-01';"
docker exec modulo5-mongo mongosh --quiet --eval "db.getSiblingDB('notifications_tienda_deportes').notifications.deleteMany({customerId:'cust-sports-workshop'})"
docker exec modulo5-mongo mongosh --quiet --eval "db.getSiblingDB('notifications_libreria_lima').notifications.deleteMany({customerId:'cust-books-workshop'})"
docker exec modulo5-redis redis-cli FLUSHDB
```

Con Podman cambia `docker` por `podman`. `FLUSHDB` es apropiado solo para este
Redis local del laboratorio.

Para empezar totalmente desde cero, detén primero los tres procesos Maven y
recrea los contenedores:

```bash
cd modulo-05-persistencia-poliglota/docker-compose
docker compose down
docker compose up -d
```

El compose no declara volúmenes persistentes: al eliminar sus contenedores se
eliminan los datos. Al arrancar de nuevo, PostgreSQL recrea las cuatro bases;
Flyway, Liquibase y los seeders se ejecutan cuando reinicies los servicios.

## 12. Detener el workshop

1. Pulsa `Ctrl+C` en las terminales 2, 3 y 4.
2. Detén la infraestructura.

**Docker**

```bash
cd modulo-05-persistencia-poliglota/docker-compose
docker compose down
```

**Podman**

```bash
cd modulo-05-persistencia-poliglota/docker-compose
podman compose down
```

## Solución de problemas

### Un contenedor no llega a `healthy`

```bash
cd modulo-05-persistencia-poliglota/docker-compose
docker compose ps
docker compose logs postgres redis mongo
```

Busca puertos ocupados o un runtime detenido. En macOS con Podman, inicia antes
la máquina con `podman machine start`.

### PostgreSQL no contiene las cuatro bases

El script de `docker-entrypoint-initdb.d` solo corre al inicializar un contenedor
nuevo. Ejecuta `docker compose down` y luego `docker compose up -d`. Este
laboratorio no usa volúmenes nombrados, por lo que recrear los contenedores
reinicializa los datos.

### Catalog no inicia

- confirma PostgreSQL con `pg_isready`;
- confirma Redis con `redis-cli ping`;
- revisa las URLs y credenciales de
  [`application.yml`](catalog-service-spring/src/main/resources/application.yml);
- busca errores Flyway de las dos bases en la Terminal 2.

### Inventory no inicia

- confirma que existen `inventory_tienda_deportes` e
  `inventory_libreria_lima`;
- revisa los dos datasources y las dos configuraciones Liquibase en
  [`application.properties`](inventory-service-quarkus/src/main/resources/application.properties);
- consulta `databasechangelog` con `psql`.

### Notification no guarda documentos

- comprueba MongoDB con `mongosh`;
- revisa `MONGODB_URI` en
  [`application.yml`](notification-service-spring/src/main/resources/application.yml);
- recuerda que una base MongoDB no aparece hasta que contiene el primer
  documento.

### La API responde 400

Los endpoints de negocio exigen exactamente el header `X-Tenant-ID`. Los
health checks `/actuator/*` y `/q/*` están excluidos.

### Catalog responde 429

Se alcanzó el límite por tenant, IP y minuto. Espera al siguiente minuto o,
solo en este laboratorio, limpia Redis con:

```bash
docker exec modulo5-redis redis-cli FLUSHDB
```

### El puerto ya está ocupado

Detén el proceso que usa el puerto. También puedes cambiar el puerto del
servicio con `SERVER_PORT`, pero deberás adaptar todas las URLs del workshop.
Para los motores, cambia el mapeo del compose y la URL correspondiente en la
configuración del servicio.

## Archivos clave para estudiar

- [`docker-compose.yml`](docker-compose/docker-compose.yml): imágenes, puertos
  y health checks reales.
- [`catalog/application.yml`](catalog-service-spring/src/main/resources/application.yml):
  datasources por tenant, Redis y TTL.
- [`inventory/application.properties`](inventory-service-quarkus/src/main/resources/application.properties):
  Hibernate `DATABASE`, datasources y Liquibase por tenant.
- [`notification/application.yml`](notification-service-spring/src/main/resources/application.yml):
  conexión MongoDB y prefijo de bases.
- [`ProductService.java`](catalog-service-spring/src/main/java/pe/joedayz/microservicios/catalog/service/ProductService.java):
  JPA y anotaciones de caché.
- [`InventoryService.java`](inventory-service-quarkus/src/main/java/pe/joedayz/microservicios/inventory/service/InventoryService.java):
  reserva transaccional con Panache.
- [`TenantMongoTemplateFactory.java`](notification-service-spring/src/main/java/pe/joedayz/microservicios/notification/tenant/TenantMongoTemplateFactory.java):
  selección dinámica de base documental.

## Lecturas del módulo

- [Guía paso a paso de persistencia](docs/01-guia-paso-a-paso.md)
