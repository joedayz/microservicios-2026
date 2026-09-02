# AWS API Gateway – Guía de despliegue (Módulo 7)

> **Este despliegue crea recursos reales en TU cuenta AWS.**
> Al terminar la clase ejecuta `destroy` para evitar cargos.

## 🎯 Qué vas a lograr

Un **REST API en AWS API Gateway** con:

- Lambda authorizer que valida JWT (issuer de Keycloak).
- Usage Plan con throttling (`RateLimit=10`, `BurstLimit=20`) y quota diaria.
- Request validators (body y parámetros).
- API Key demo.
- Gateway responses `401` / `429` personalizados.
- (Opcional) Hook para asociar **AWS WAFv2**.

Todo empaquetado como **AWS SAM** (CloudFormation por debajo).

---

## ✅ Requisitos por sistema

| Tu entorno | Instalar | Comando de verificación |
|-----------|----------|--------------------------|
| **macOS** | `brew install awscli aws-sam-cli` | `aws --version && sam --version` |
| **Windows** | Instaladores oficiales AWS CLI v2 y SAM CLI, **o** `winget install Amazon.AWSCLI Amazon.SAM-CLI` | `aws --version; sam --version` |
| **Linux** | Ver [docs oficiales](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html) | igual |

**Cuenta y permisos:**

- Credenciales configuradas: `aws configure` **o** `aws sso login`.
- Permisos: `apigateway:*`, `cloudformation:*`, `iam:*`, `lambda:*`, `s3:*`, `wafv2:*` (para el WebACL opcional).
- SAM crea un bucket S3 automático (`--resolve-s3`) para artefactos.

---

## 📂 Qué archivo hace qué

| Archivo | Rol | ¿Editas tú? |
|---------|-----|-------------|
| `openapi.yaml` | **Definición de las rutas** con extensiones `x-amazon-apigateway-*` (integraciones HTTP_PROXY, validators, auth). | Sí, para agregar/quitar endpoints. |
| `template.yaml` | **SAM template** que crea: la API (importa `openapi.yaml`), la Lambda authorizer, el Usage Plan, la API Key. | Sí, para cambiar throttling, memoria de Lambda, etc. |
| `deploy.sh` / `deploy.ps1` | Wrappers de `sam build` + `sam deploy` con verificación de prerequisitos. | No. |
| `DEPLOY.md` | Este archivo. Empieza aquí. | No. |
| `README.md` | Referencia y equivalencias con SCG. | No. |

**Regla:** primero editas `openapi.yaml` si tocas rutas, o `template.yaml` si tocas
infra. Nunca cambies el `deploy.sh` para "arreglar" algo — es solo wrapper.

---

## 🚀 Paso a paso (macOS / Linux / WSL / Git Bash)

### Paso 1 – Verificar prerequisitos

```bash
cd modulo-07-api-gateway-enrutamiento/aws-api-gateway
./deploy.sh check
```

Salida esperada:

```
[info] AWS CLI:  aws-cli/2.x
[info] SAM CLI:  SAM CLI, version 1.x
[info] Verificando credenciales...
+------+---------------------------------+
| ...  | 123456789012  (tu cuenta)       |
+------+---------------------------------+
[ok] Prerequisitos OK
```

Si falla:
- `Falta 'aws'` → instala AWS CLI.
- `Falta 'sam'` → instala SAM CLI.
- `no hay credenciales` → `aws configure` o `aws sso login`.

### Paso 2 – Configurar variables (opcional)

```bash
export STACK_NAME=joedayz-edge-modulo7
export AWS_REGION=us-east-1
export VPC_LINK_ID=none                       # "none" = demo pública sin VPC Link
export ORDER_BACKEND=order.demo.joedayz.pe    # NLB / dominio publico de tu backend
export INVENTORY_BACKEND=inventory.demo.joedayz.pe
export KEYCLOAK_ISSUER=http://localhost:8180/realms/joedayz-microservices
```

### Paso 3 – Desplegar

```bash
./deploy.sh deploy
```

**Qué hace paso a paso:**
1. `sam build --template template.yaml` → empaqueta la Lambda inline y valida OpenAPI.
2. `sam deploy` → sube artefactos a S3 y crea/actualiza el stack CloudFormation.
3. Imprime los outputs: `ApiInvokeUrl`, `AuthorizerFunctionArn`.

Tiempo esperado: **2–4 min** la primera vez, **~1 min** en updates.

### Paso 4 – Probar

```bash
./deploy.sh test
```

Hace un `GET` directo (sin API key — el throttling está a nivel de método, no por key).

### Paso 5 – Ver throttling en vivo

```bash
API_URL=$(aws cloudformation describe-stacks --stack-name joedayz-edge-modulo7 \
  --region us-east-1 \
  --query 'Stacks[0].Outputs[?OutputKey==`ApiInvokeUrl`].OutputValue' --output text)

for i in $(seq 1 100); do
  curl -o /dev/null -s -w "%{http_code}\n" \
    "$API_URL/gateway/orders/api/v1/tenants/tienda-deportes/orders?n=$i"
done | sort | uniq -c
```

Verás `200` y `429` mezclados (`ThrottlingBurstLimit: 40`, `RateLimit: 20/s` en `template.yaml`).

> 💡 **API Gateway usa CloudFront por debajo.** Los errores `4xx` se cachean unos minutos en el edge. Si un `403` no desaparece tras un `update-rest-api`, agrega un query string único (`?_=$RANDOM`) para bypass del cache.

### Paso 6 – Destruir

```bash
./deploy.sh destroy
```

Verifica que el stack desapareció:

```bash
aws cloudformation wait stack-delete-complete --stack-name joedayz-edge-modulo7
```

---

## 🪟 Paso a paso (Windows PowerShell)

Idéntico usando `.ps1`:

```powershell
cd modulo-07-api-gateway-enrutamiento\aws-api-gateway
$env:AWS_REGION = 'us-east-1'
$env:VPC_LINK_ID = 'none'
.\deploy.ps1 check
.\deploy.ps1 deploy
.\deploy.ps1 test
.\deploy.ps1 destroy
```

Si PowerShell bloquea el script:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

---

## 🧭 Flujo mental (cuándo edito cada archivo)

```
¿Quiero cambiar rutas / validators / auth?     → editar openapi.yaml → ./deploy.sh deploy
¿Quiero cambiar throttling / usage plan / Lambda? → editar template.yaml → ./deploy.sh deploy
¿Quiero probar rápido?                         → ./deploy.sh test
¿Ya terminé la clase?                          → ./deploy.sh destroy
```

---

## 💰 Costo aproximado

- API Gateway REST: **USD 3.50 por millón de requests**.
- Lambda authorizer: primer millón **gratis**.
- CloudWatch logs: casi cero para la demo.

En una clase típica no llegarás a USD 0.05. **Aun así, ejecuta `destroy` al terminar.**

---

## 🩺 Troubleshooting

| Síntoma | Causa | Fix |
|---------|-------|-----|
| `Falta 'sam'` | SAM CLI no instalado | Ver tabla de requisitos |
| `The security token expired` | Sesión SSO caducó | `aws sso login` |
| `User is not authorized to perform: iam:CreateRole` | Falta permiso | Pedir al admin `AWSCloudFormationFullAccess` + `IAMFullAccess` |
| `Unable to import API` | `openapi.yaml` con variables sin resolver | Verificar `stageVariables.orderBackend` etc. |
| `401` siempre | El JWT no valida | Ver logs de la Lambda authorizer: `sam logs -n JwtAuthorizerFunction --stack-name joedayz-edge-modulo7 --tail` |
| `403 Missing Authentication Token` | URL o método mal escritos | `aws apigateway get-resources --rest-api-id <id>` |

---

## 📚 Siguiente lectura

- [`README.md`](README.md) – equivalencias detalladas AWS API Gateway ↔ Spring Cloud Gateway.
- [`openapi.yaml`](openapi.yaml) – definición de rutas.
- [`template.yaml`](template.yaml) – recursos AWS que se crean.
