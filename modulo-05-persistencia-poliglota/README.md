# Módulo 5 – Persistencia Políglota

> Curso: **Arquitectura de Microservicios Pro: Spring Boot + Quarkus en AWS y Azure**  
> JoeDayz.pe · Java 21 / Spring Boot 4 / Quarkus 3 / PostgreSQL / MongoDB / Redis

En este módulo llevamos el e-commerce multi-tenant a una capa de persistencia más realista:
**PostgreSQL** para transaccional, **MongoDB** para documentos y **Redis** para performance
y control operacional. Además, dejamos atrás el `tenant_id` compartido de los módulos previos
para mostrar **multi-tenancy a nivel base de datos**.

## Objetivos de aprendizaje

1. Usar **Spring Data JPA** con **PostgreSQL** y migraciones **Flyway**.
2. Usar **Quarkus Panache** con **PostgreSQL** y migraciones **Liquibase**.
3. Modelar un caso documental con **Spring Data MongoDB**.
4. Aplicar **Redis** para caching distribuido y rate limiting.
5. Implementar **database-per-tenant** con `X-Tenant-ID` como selector.

## Microservicios del módulo

| Proyecto | Rol | Stack | Puerto |
|----------|-----|-------|--------|
| [catalog-service-spring](catalog-service-spring/) | Catálogo de productos | Spring Boot 4 + Spring Data JPA + Flyway + Redis | **8081** |
| [inventory-service-quarkus](inventory-service-quarkus/) | Stock y reservas | Quarkus 3 + Panache + Liquibase | **8084** |
| [notification-service-spring](notification-service-spring/) | Historial documental de notificaciones | Spring Boot 4 + Spring Data MongoDB | **8087** |

## Infraestructura local

El stack de datos vive en [`docker-compose/`](docker-compose/):

| Servicio | Puerto | Uso |
|----------|--------|-----|
| PostgreSQL | 5432 | Bases `catalog_*` e `inventory_*` por tenant |
| Redis | 6379 | Caché de catálogo + rate limiting |
| MongoDB | 27017 | Bases `notifications_<tenant>` |

Levanta todo con cualquiera de estas opciones:

```bash
cd modulo-05-persistencia-poliglota/docker-compose
podman compose up -d
```

```bash
cd modulo-05-persistencia-poliglota/docker-compose
docker compose up -d
```

## Cómo ejecutar los servicios

```bash
cd modulo-05-persistencia-poliglota/catalog-service-spring
mvn spring-boot:run
```

```bash
cd modulo-05-persistencia-poliglota/inventory-service-quarkus
mvn quarkus:dev
```

```bash
cd modulo-05-persistencia-poliglota/notification-service-spring
mvn spring-boot:run
```

## Smoke test rápido

### 1) Catalog con Spring Data JPA + Flyway + Redis

```bash
curl -H 'X-Tenant-ID: tienda-deportes' \
  http://localhost:8081/api/v1/products
```

```bash
curl -X POST http://localhost:8081/api/v1/products \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: libreria-lima' \
  -d '{
    "sku":"LIB-CLOUD-01",
    "name":"Cloud Native Patterns",
    "description":"Libro para arquitectos",
    "category":"libros",
    "price":140.00,
    "currency":"PEN"
  }'
```

### 2) Inventory con Quarkus Panache + Liquibase

```bash
curl -H 'X-Tenant-ID: tienda-deportes' \
  http://localhost:8084/api/v1/inventory
```

```bash
curl -X POST http://localhost:8084/api/v1/inventory/LIB-DDD-01/reserve \
  -H 'Content-Type: application/json' \
  -H 'X-Tenant-ID: libreria-lima' \
  -d '{"quantity":2}'
```

### 3) Notification con Spring Data MongoDB

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

## Qué demuestra cada servicio

### Catalog Service (Spring)

- `tenant/TenantWebFilter.java`: exige `X-Tenant-ID`.
- `config/DataSourceConfig.java`: enruta JPA al datasource del tenant.
- `config/FlywayMigrationConfig.java`: ejecuta Flyway en cada base.
- `config/RateLimitService.java`: usa Redis para rate limiting distribuido.
- `service/ProductService.java`: cachea lecturas con Redis.

### Inventory Service (Quarkus)

- `tenant/InventoryTenantResolver.java`: conecta `X-Tenant-ID` con Hibernate multi-tenant.
- `domain/InventoryItem.java`: entidad Panache con menos boilerplate.
- `service/InventoryService.java`: reserva stock en transacción.
- `config/InventorySeedData.java`: carga datos demo por tenant.
- `src/main/resources/db/changelog.xml`: esquema controlado por Liquibase.

### Notification Service (Spring MongoDB)

- `tenant/TenantMongoTemplateFactory.java`: crea un `MongoTemplate` por base tenant.
- `domain/NotificationMessage.java`: documento Mongo.
- `service/NotificationService.java`: guarda y lista notificaciones por tenant.

## Guía didáctica paso a paso

La explicación completa del módulo, archivo por archivo y con flujo de prueba para clase, está en:

- [docs/01-guia-paso-a-paso.md](docs/01-guia-paso-a-paso.md)
