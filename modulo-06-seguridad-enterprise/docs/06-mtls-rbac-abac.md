# 06. mTLS + RBAC + ABAC

## Por qué combinarlos

- **RBAC** responde: “¿qué puede hacer este rol?”
- **ABAC** responde: “¿sobre qué tenant, región o recurso puede hacerlo?”
- **mTLS** responde: “¿qué servicio está realmente al otro lado del socket?”

## Lectura correcta

No compiten. Se complementan:

```mermaid
flowchart LR
    TLS["mTLS<br/>identidad de servicio"]
    JWT["JWT<br/>identidad de usuario"]
    RBAC["RBAC<br/>rol"]
    ABAC["ABAC<br/>tenant + region"]
    DEC["Decisión final"]

    TLS --> DEC
    JWT --> RBAC
    JWT --> ABAC
    RBAC --> DEC
    ABAC --> DEC
```

## Dónde se ve en el código

- `order-service-spring/.../client/TlsMaterialLoader.java`
- `order-service-spring/.../client/InventoryMtlsClient.java`
- `inventory-service-quarkus/.../security/InternalClientPolicyService.java`

## Frase para clase

> El JWT protege la identidad del usuario.  
> El mTLS protege la identidad del microservicio.
