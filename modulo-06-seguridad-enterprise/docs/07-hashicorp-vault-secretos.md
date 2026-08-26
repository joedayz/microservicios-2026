# 07. HashiCorp Vault para gestión de secretos

## Problema

Si el `client-id`, password o certificado vive hardcodeado en el repo, ya perdiste.

## Solución en la demo

Vault guarda el secreto:

```text
secret/data/module6/inventory-client
```

y `order-service-spring` lo consulta con `VaultSecretClient`.

## Flujo

1. Vault corre en modo dev para laboratorio.
2. `scripts/02-vault-seed.sh` crea el secreto demo.
3. `InventoryMtlsClient` pide el valor antes de preparar la llamada interna.

## Punto fino para explicar

El secreto no reemplaza al certificado:

- el **certificado** prueba identidad de servicio a nivel TLS;
- el **secreto** complementa configuración operativa sin quedar en el código.
