# Azure API Management – Guía de despliegue (Módulo 7)

> **Este despliegue crea recursos reales en TU subscription Azure.**
> APIM Developer SKU cuesta ~USD 50/mes prorrateado (~USD 0.07/hora).
> Al terminar la clase ejecuta `destroy`.

## 🎯 Qué vas a lograr

Un **Azure API Management** con:

- 1 producto (`joedayz-microservices-edge`) con policy global (CORS, `validate-jwt`, `rate-limit-by-key`, WAF básico).
- 2 APIs (`orders`, `inventory`) con operaciones definidas y policies por-operación (retry, fallback, cache).
- 1 suscripción demo con clave visible en outputs del deployment.

Todo empaquetado como **Bicep** (template ARM moderno).

---

## ⚠️ Aviso didáctico

**APIM Developer SKU tarda 30–45 min en aprovisionarse la primera vez.**
Para clases en vivo:

1. **Antes de clase:** ejecuta `./deploy.sh deploy` una vez → deja APIM levantado.
2. **En clase:** solo modificas policies XML y vuelves a correr `./deploy.sh deploy`. Ahora tarda **< 2 min** (solo cambia policies, no recrea el servicio).

---

## ✅ Requisitos por sistema

| Tu entorno | Instalar | Verificar |
|-----------|----------|-----------|
| **macOS** | `brew install azure-cli` | `az version` |
| **Windows** | `winget install Microsoft.AzureCLI` o instalador MSI | `az version` |
| **Linux** | `curl -sL https://aka.ms/InstallAzureCLIDeb \| sudo bash` | `az version` |

**Cuenta y permisos:**

- Ejecutar `az login` (abre navegador).
- Seleccionar subscription: `az account set --subscription <ID>`.
- Rol mínimo: `Contributor` en la subscription **o** en el resource group.

---

## 📂 Qué archivo hace qué

| Archivo | Rol | ¿Editas tú? |
|---------|-----|-------------|
| `main.bicep` | Template Bicep: crea APIM, producto, APIs, operaciones, subscription. Importa policies con `loadTextContent()`. | Sí, si agregas APIs u operaciones. |
| `policies/global.xml` | Policy del **producto** (afecta a todas las APIs). CORS, JWT, rate-limit, WAF. | Sí, para cambiar políticas transversales. |
| `policies/orders.xml` | Policy de la **operación** `orders.list-orders`. Retry + fallback + rewrite-uri. | Sí. |
| `policies/inventory.xml` | Policy de la operación `inventory.get-inventory`. Cache + rate-limit propio. | Sí. |
| `deploy.sh` / `deploy.ps1` | Wrappers de `az group create` + `az deployment group create`. | No. |
| `DEPLOY.md` | Este archivo. Empieza aquí. | No. |

**Regla:** editas Bicep para infra, XML para reglas de tráfico. El `deploy.sh` no
se edita.

---

## 🚀 Paso a paso (macOS / Linux / WSL / Git Bash)

### Paso 1 – Verificar

```bash
cd modulo-07-api-gateway-enrutamiento/azure-apim
./deploy.sh check
```

### Paso 2 – Configurar (opcional)

```bash
export RESOURCE_GROUP=rg-joedayz-modulo7
export LOCATION=eastus                      # o brazilsouth para LATAM
export APIM_NAME=apim-joedayz-lat           # nombre GLOBAL único
export PUBLISHER_EMAIL=tu-correo@empresa.com
export ORDER_BACKEND_URL=https://order.demo.joedayz.pe
export INVENTORY_BACKEND_URL=https://inventory.demo.joedayz.pe
```

### Paso 3 – Desplegar

```bash
./deploy.sh deploy
```

**Qué hace paso a paso:**
1. `az group create` — idempotente.
2. `az deployment group create` — lee `main.bicep`, resuelve `loadTextContent()` sobre las policies, envía el ARM template.
3. Azure aprovisiona APIM y sus APIs.
4. Al terminar, imprime `apimGatewayUrl` y `subscriptionKeyResourceId`.

⏱ **Tiempo:**
- 1ra vez: **30–45 min** (aprovisionamiento del servicio APIM).
- Updates: **1–3 min** (solo cambia policies/APIs).

Sigue el progreso:
```bash
az deployment group list -g rg-joedayz-modulo7 -o table
```
O en el portal → tu resource group → **Deployments**.

### Paso 4 – Probar

Guarda un JWT válido de Keycloak en `token.txt`:

```bash
curl -s -X POST 'http://localhost:8180/realms/joedayz-microservices/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password&client_id=student-portal&username=bruno-manager&password=***' \
  | jq -r '.access_token' > token.txt
```

Luego:

```bash
./deploy.sh test
```

El script recupera la subscription key con `az rest ... listSecrets` y hace un
`GET` con `Ocp-Apim-Subscription-Key`, `X-Tenant-ID` y `Authorization: Bearer`.

### Paso 5 – Iterar sobre policies

Edita cualquier `policies/*.xml` y vuelve a correr:

```bash
./deploy.sh deploy
```

Bicep detecta el cambio (`loadTextContent()`), envía el nuevo XML y APIM lo
aplica en segundos. **No hay que recrear el servicio.**

Alternativa rápida (sin Bicep) — actualizar solo una policy:

```bash
az apim api operation policy create \
  -g rg-joedayz-modulo7 --service-name $APIM_NAME \
  --api-id inventory --operation-id get-inventory \
  --xml-path policies/inventory.xml
```

### Paso 6 – Ver el trace de una request

En el portal → **APIM** → **APIs** → Orders → **Test** → activar **Trace** →
enviar request. Ves cada policy con su tiempo.

### Paso 7 – Destruir

```bash
./deploy.sh destroy
```

Borra el resource group entero (async). Para eliminar el APIM del soft-delete:

```bash
az apim deletedservice purge --service-name $APIM_NAME --location $LOCATION
```

---

## 🪟 Paso a paso (Windows PowerShell)

```powershell
cd modulo-07-api-gateway-enrutamiento\azure-apim
$env:LOCATION = 'eastus'
$env:APIM_NAME = 'apim-joedayz-lat'
.\deploy.ps1 check
.\deploy.ps1 deploy
.\deploy.ps1 test
.\deploy.ps1 destroy
```

---

## 🧭 Flujo mental (cuándo edito cada archivo)

```
¿Nueva API o operación?         → editar main.bicep → ./deploy.sh deploy
¿Cambio en JWT/CORS/rate-limit? → editar policies/global.xml → ./deploy.sh deploy
¿Cambio en fallback o cache?    → editar policies/orders.xml o inventory.xml → ./deploy.sh deploy
¿Probar rápido?                 → ./deploy.sh test
¿Terminé la clase?              → ./deploy.sh destroy
```

---

## 💰 Costo aproximado

| SKU | Uso | Costo/hora | Notas |
|-----|-----|-----------|-------|
| **Developer** (este demo) | Aprendizaje | ~USD 0.07 | Sin SLA. **NO** para producción. |
| Basic v2 | Producción básica | ~USD 0.20 | Con SLA 99.95%. |
| Standard v2 | Producción | ~USD 0.90 | Multi-region opcional. |
| Premium | Enterprise | desde ~USD 3.30 | Multi-region + VNet. |

**Recuerda `destroy` al terminar.** Un fin de semana olvidado = ~USD 3.5.

---

## 🩺 Troubleshooting

| Síntoma | Causa | Fix |
|---------|-------|-----|
| `Falta 'az'` | Azure CLI no instalado | Ver tabla de requisitos |
| `No hay sesion activa` | Falta `az login` | `az login` |
| `The name 'apim-xxx' is already taken` | El nombre APIM es global | Cambia `APIM_NAME` a algo único |
| Deployment tarda una eternidad | APIM Developer normal la 1ra vez | Espera 30–45 min |
| `401 – Access denied due to invalid subscription key` | Falta header `Ocp-Apim-Subscription-Key` | El `test` ya lo agrega — revisar tu curl manual |
| `401 – JWT validation failed` | Issuer o audience no coincide | Ajustar `policies/global.xml` → `<openid-config url="...">` |
| Cambios en XML no se aplican | Cache del portal | Recargar portal; `az deployment` sí reaplica |
| Costos altos inesperados | APIM olvidado | `az group delete` + `az apim deletedservice purge` |

---

## 📚 Siguiente lectura

- [`README.md`](README.md) – equivalencias detalladas Azure APIM ↔ Spring Cloud Gateway.
- [`main.bicep`](main.bicep) – recursos que se crean.
- [`policies/`](policies/) – XML de cada política.
