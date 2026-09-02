# Azure API Management – Módulo 7

Ejemplo de policies de **Azure API Management (APIM)** que replica el edge del
`api-gateway-service` para el producto `joedayz-microservices-edge`.

## Estructura

```
azure-apim/
├── README.md
└── policies/
    ├── global.xml       # scope: product / all-operations
    ├── orders.xml       # scope: API "orders" (rewrite + retry + fallback)
    └── inventory.xml    # scope: API "inventory" (cache + rate-limit)
```

## Aplicar policies

Vía Azure CLI (ejemplo para el scope global de un producto):

```bash
az apim product policy import \
  --resource-group rg-joedayz-lat \
  --service-name apim-joedayz-lat \
  --product-id joedayz-microservices-edge \
  --xml-path ./policies/global.xml
```

Para operaciones específicas:

```bash
az apim api operation policy import \
  --resource-group rg-joedayz-lat \
  --service-name apim-joedayz-lat \
  --api-id orders \
  --operation-id list-orders \
  --xml-path ./policies/orders.xml
```

## Equivalencias con Spring Cloud Gateway

| SCG | Azure APIM |
|-----|------------|
| `RequestRateLimiter` + Redis | `<rate-limit-by-key>` |
| `CircuitBreaker` + `FallbackController` | `<retry>` + `<choose>` con `<return-response>` |
| `Retry` filter | `<retry>` en `<backend>` |
| `TimeLimiter` | atributo `timeout` en `<forward-request>` |
| `globalcors` | `<cors>` |
| `oauth2ResourceServer` | `<validate-jwt>` con `<openid-config>` |
| `BasicWafFilter` | `<choose>` + `<return-response>`, complementado con **Azure WAF (App Gateway)** |
| `CorrelationIdGatewayFilter` | `<set-variable>` + `<set-header>` con `Guid.NewGuid()` |
| `StripPrefix=2` | `<rewrite-uri>` |
| `AddResponseHeader` | `<set-header>` en `<outbound>` |
