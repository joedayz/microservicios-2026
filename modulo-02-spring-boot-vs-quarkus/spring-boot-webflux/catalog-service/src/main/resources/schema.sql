CREATE TABLE IF NOT EXISTS "products" (
    "sku" VARCHAR(32) PRIMARY KEY,
    "tenant_id" VARCHAR(64) NOT NULL,
    "name" VARCHAR(255) NOT NULL,
    "description" VARCHAR(512),
    "price" DECIMAL(12, 2) NOT NULL,
    "currency" VARCHAR(3) NOT NULL
);
