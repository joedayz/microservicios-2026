# AWS API Gateway – Módulo 7

Ejemplo desplegable en **AWS API Gateway (REST)** que replica el edge del
`api-gateway-service`:

- Enrutamiento hacia `order-service` e `inventory-service` (vía **VPC Link**).
- Request validators (`params-only` y `full`).
- **Usage plan** con throttling (`BurstLimit`, `RateLimit`) y quota diaria.
- **Lambda authorizer** para validar JWT de Keycloak.
- Resource policy que restringe el acceso a un VPC endpoint.
- Gateway responses personalizados para `429` y `401`.
- Referencia a **AWS WAFv2** vía `WebACLAssociation` (fuera del OpenAPI).

## Archivos

| Archivo | Descripción |
|---------|-------------|
| [`openapi.yaml`](openapi.yaml) | Definición OpenAPI 3 con extensiones `x-amazon-apigateway-*`. |

## Desplegar

```bash
aws apigateway import-rest-api --body fileb://openapi.yaml
```

Antes:
1. Publicar la Lambda `JwtAuthorizerFunction` (Node.js/Python que valida el JWT contra el JWKS de Keycloak).
2. Crear el VPC Link y anotar su ID en el stage variable `vpcLinkId`.
3. Configurar stage variables `orderBackend`, `inventoryBackend` con los NLB internos.
4. Asociar un WebACL de **AWS WAFv2** con reglas managed: `AWSManagedRulesCommonRuleSet`, `AWSManagedRulesSQLiRuleSet`.

## Equivalencias con Spring Cloud Gateway

| SCG (`application.yml`) | AWS API Gateway |
|-------------------------|-----------------|
| `RequestRateLimiter` | `UsagePlan` + `Throttle` + `MethodSettings.ThrottlingRateLimit` |
| `CircuitBreaker` | Retornar 5xx + Route 53 health checks / Lambda destinations |
| `Retry` filter | El cliente debe reintentar; API GW no reintenta backends HTTP |
| `TimeLimiter` | `timeoutInMillis` en la integración (máx 29s) |
| `globalcors` | Configurar CORS por método (`OPTIONS` + `x-amazon-apigateway-integration`) |
| `BasicWafFilter` | **AWS WAFv2** WebACL |
| `oauth2ResourceServer` | Lambda authorizer o **JWT authorizer** (HTTP API v2) |
| `CorrelationIdGatewayFilter` | `x-request-id` + CloudWatch logs con `$context.requestId` |
