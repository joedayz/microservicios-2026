# Guía paso a paso – Módulo 5 de Persistencia

Esta guía está pensada para explicar el módulo en clase, mostrando **qué hace cada pieza**
y cómo probarla rápidamente con **Podman** o **Docker Desktop**.

## 1. Qué problema resolvemos

Hasta el módulo 4 la aplicación ya podía:

- exponer APIs;
- propagarse con `X-Tenant-ID`;
- comunicarse de forma síncrona y asíncrona.

En este módulo resolvemos otra pregunta crítica:

> **¿Dónde vive cada tipo de dato y cómo aislamos cada tenant sin mezclar información?**

La respuesta en esta demo es:

| Tipo de dato | Tecnología | Microservicio |
|--------------|------------|---------------|
| Productos | PostgreSQL + JPA | `catalog-service-spring` |
| Stock y reservas | PostgreSQL + Panache | `inventory-service-quarkus` |
| Notificaciones | MongoDB | `notification-service-spring` |
| Caché + rate limiting | Redis | `catalog-service-spring` |

## 2. Levantar la infraestructura

Desde la raíz del módulo:

```bash
cd modulo-05-persistencia-poliglota/docker-compose
podman compose up -d
```

o:

```bash
cd modulo-05-persistencia-poliglota/docker-compose
docker compose up -d
```

Qué sube:

1. `postgres` con cuatro bases:
   - `catalog_tienda_deportes`
   - `catalog_libreria_lima`
   - `inventory_tienda_deportes`
   - `inventory_libreria_lima`
2. `redis` para cache compartido y rate limiting.
3. `mongo` para crear bases `notifications_<tenant>`.

Verifica salud:

```bash
docker ps
docker logs modulo5-postgres --tail 30
docker logs modulo5-redis --tail 30
docker logs modulo5-mongo --tail 30
```

Con Podman, cambia `docker` por `podman`.

## 3. Arrancar los tres servicios

### Catalog Service

```bash
cd modulo-05-persistencia-poliglota/catalog-service-spring
mvn spring-boot:run
```

Puntos importantes del código:

- `TenantWebFilter` valida el header `X-Tenant-ID`.
- `DataSourceConfig` transforma el tenant en la clave del datasource.
- `FlywayMigrationConfig` corre `db/migration/V1__create_products.sql` en cada base.
- `DemoDataSeeder` siembra productos distintos por tenant.
- `RateLimitService` escribe contadores en Redis.
- `ProductService` cachea listados y lecturas por SKU.

### Inventory Service

```bash
cd modulo-05-persistencia-poliglota/inventory-service-quarkus
mvn quarkus:dev
```

Puntos clave:

- `TenantRequestFilter` exige `X-Tenant-ID`.
- `InventoryTenantResolver` le dice a Hibernate qué datasource usar.
- `quarkus.hibernate-orm.multitenant=DATABASE` activa database-per-tenant.
- `db/changelog.xml` crea la tabla `inventory_items`.
- `InventorySeedData` carga datos de demo en cada base.
- `InventoryItem` usa **Panache** para reducir boilerplate.

### Notification Service

```bash
cd modulo-05-persistencia-poliglota/notification-service-spring
mvn spring-boot:run
```

Puntos clave:

- `TenantMongoTemplateFactory` crea un `MongoTemplate` por tenant.
- La base se calcula como `notifications_<tenant-normalizado>`.
- `NotificationMessage` es el documento guardado en la colección `notifications`.

## 4. Prueba guiada del flujo

### Paso 1: comprobar el catálogo por tenant

```bash
curl -H 'X-Tenant-ID: tienda-deportes' \
  http://localhost:8081/api/v1/products
```

Debes ver productos deportivos.

```bash
curl -H 'X-Tenant-ID: libreria-lima' \
  http://localhost:8081/api/v1/products
```

Debes ver libros técnicos.

**Qué explica este paso**

- mismo endpoint;
- mismo código;
- distinta base de datos según tenant.

### Paso 2: crear un producto y mostrar Flyway + cache + rate limit

```bash
curl -X POST http://localhost:8081/api/v1/products \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: libreria-lima' \
  -d '{
    "sku":"LIB-MSA-01",
    "name":"Microservices Architecture",
    "description":"Libro del curso",
    "category":"libros",
    "price":135.00,
    "currency":"PEN"
  }'
```

Luego:

```bash
curl -H 'X-Tenant-ID: libreria-lima' \
  http://localhost:8081/api/v1/products/LIB-MSA-01
```

**Qué explica este paso**

- Flyway ya creó el esquema al arrancar.
- JPA persiste el nuevo producto.
- Redis puede cachear la lectura del SKU.
- el filtro de rate limiting protege el endpoint.

### Paso 3: reservar stock en Quarkus

```bash
curl -H 'X-Tenant-ID: libreria-lima' \
  http://localhost:8084/api/v1/inventory/LIB-DDD-01
```

```bash
curl -X POST http://localhost:8084/api/v1/inventory/LIB-DDD-01/reserve \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: libreria-lima' \
  -d '{"quantity":2}'
```

**Qué explica este paso**

- Panache simplifica la entidad y la consulta.
- Liquibase versiona el esquema.
- la transacción actualiza `availableQuantity` y `reservedQuantity`.
- Quarkus decide el datasource usando el tenant resolver.

### Paso 4: guardar una notificación documental

```bash
curl -X POST http://localhost:8087/api/v1/notifications \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: tienda-deportes' \
  -d '{
    "customerId":"cust-001",
    "channel":"EMAIL",
    "subject":"Pedido confirmado",
    "body":"Tu pedido ya fue confirmado"
  }'
```

```bash
curl -H 'X-Tenant-ID: tienda-deportes' \
  http://localhost:8087/api/v1/notifications
```

**Qué explica este paso**

- MongoDB sirve bien para payloads documentales;
- cada tenant cae en una base distinta;
- Spring Data MongoDB sigue siendo simple aunque la base cambie dinámicamente.

## 5. Cómo contar la historia del código

### A. Spring + PostgreSQL + Flyway

Empieza por estas clases:

1. `catalog-service-spring/.../TenantWebFilter.java`
2. `catalog-service-spring/.../DataSourceConfig.java`
3. `catalog-service-spring/.../FlywayMigrationConfig.java`
4. `catalog-service-spring/.../ProductService.java`

Mensaje didáctico:

> Spring Data JPA no sabe “quién es el tenant”; solo ve un datasource.  
> Nosotros resolvemos el tenant antes y le entregamos la conexión correcta.

### B. Quarkus + Panache + Liquibase

Explica estas piezas:

1. `inventory-service-quarkus/.../TenantRequestFilter.java`
2. `inventory-service-quarkus/.../InventoryTenantResolver.java`
3. `inventory-service-quarkus/.../InventoryItem.java`
4. `inventory-service-quarkus/src/main/resources/db/changelog.xml`

Mensaje didáctico:

> Quarkus reduce el boilerplate con Panache, pero el concepto multi-tenant sigue siendo el mismo:
> resolver tenant, elegir base y operar dentro de una transacción.

### C. MongoDB por tenant

Explica:

1. `notification-service-spring/.../TenantMongoTemplateFactory.java`
2. `notification-service-spring/.../NotificationService.java`
3. `notification-service-spring/.../NotificationMessage.java`

Mensaje didáctico:

> En Mongo no cambiamos de tabla ni schema: cambiamos de **base** y seguimos trabajando
> con documentos desde Spring Data MongoDB.

## 6. Qué revisar si algo falla

### Si Catalog no levanta

- valida que PostgreSQL esté en `localhost:5432`;
- revisa que existan las bases creadas por `01-create-databases.sql`;
- confirma Redis en `localhost:6379`.

### Si Inventory no levanta

- revisa `application.properties`;
- confirma que Quarkus vea ambos datasources;
- verifica que Liquibase haya creado `inventory_items`.

### Si Notification no guarda

- valida `MONGODB_URI`;
- entra al contenedor y lista bases:

```bash
mongosh mongodb://localhost:27017/admin --eval "show dbs"
```

## 7. Cierre pedagógico

La idea final del módulo es que el alumno vea que **persistencia políglota** no significa
“usar muchas bases porque sí”, sino elegir la tecnología según el problema:

- **PostgreSQL** para consistencia y transacciones;
- **MongoDB** para documentos;
- **Redis** para velocidad y control distribuido;
- **Flyway/Liquibase** para versionar esquema;
- **database-per-tenant** para aislamiento fuerte.
