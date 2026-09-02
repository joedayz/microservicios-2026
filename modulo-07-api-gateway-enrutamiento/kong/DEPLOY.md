# Kong Gateway – Guía de despliegue (Módulo 7)

> **Kong corre 100% en tu máquina.** No necesitas cuenta de nube.

## 🎯 Qué vas a lograr

Un **Kong Gateway 3.7 en modo DB-less** apuntando a los mismos backends que el
Spring Cloud Gateway del módulo, para comparar dos productos que resuelven el
mismo problema (routing, rate-limit, CORS, WAF, JWT).

```
Cliente ─► Kong :8000 ─► order-service   :8086
                     └─► inventory-service :8084
```

---

## ✅ Requisitos por sistema

| Tu entorno | Qué instalar | Notas |
|-----------|--------------|-------|
| **macOS + Docker Desktop** | Docker Desktop 4.30+ | Todo funciona out of the box. |
| **macOS + Podman** (setup del instructor) | `brew install podman` + `podman machine init && podman machine start` | El script detecta `podman compose` automáticamente. |
| **Windows 11 + Docker Desktop** | Docker Desktop con WSL2 | Usar `demo.ps1` (PowerShell) **o** `demo.sh` desde Git Bash / WSL. |
| **Windows 11 + Podman Desktop** | Podman Desktop 5+ | Usar `demo.ps1`. |
| **Linux** | Docker Engine + `docker compose` plugin | Cualquier distribución reciente. |

**Verificar antes de empezar:**

```bash
docker compose version    # o:  podman compose version
```

Si sale versión → estás listo. Si sale error → instala el que corresponda de la tabla.

---

## 📂 Qué archivo hace qué

| Archivo | Rol |
|---------|-----|
| `docker-compose.yml` | Levanta el contenedor de Kong y monta `kong.yml`. **No lo edites** salvo que cambies puertos. |
| `kong.yml` | **Configuración declarativa**: rutas, plugins, consumers. Aquí sí editas para agregar rutas o cambiar límites. |
| `demo.sh` | Orquesta todo desde macOS / Linux / Git Bash. Auto-detecta Docker o Podman. |
| `demo.ps1` | Equivalente para Windows PowerShell puro. |
| `DEPLOY.md` | Este archivo. Empieza aquí. |
| `README.md` | Referencia técnica y equivalencias con Spring Cloud Gateway. |

---

## 🚀 Paso a paso (macOS / Linux / Git Bash / WSL)

### Paso 1 – Ir a la carpeta

```bash
cd modulo-07-api-gateway-enrutamiento/kong
```

### Paso 2 – Levantar Kong

```bash
./demo.sh up
```

**Qué hace:**
1. Detecta si tienes `docker compose` o `podman compose`.
2. Baja cualquier instancia previa (idempotente).
3. Arranca el contenedor.
4. Espera a que la Admin API (`:8001/status`) responda con `200`.
5. Imprime los servicios cargados desde `kong.yml`.

**Cómo verificar que salió bien:**

```bash
curl http://localhost:8001/status     # → JSON con "database": {"reachable": true}
```

### Paso 3 – Ver la configuración cargada

```bash
./demo.sh status
```

Muestra `services`, `routes` y `plugins` que Kong tiene activos.

### Paso 4 – Ejecutar smoke test

```bash
./demo.sh smoke
```

Corre 3 pruebas:

| # | Qué prueba | Resultado esperado |
|---|-----------|--------------------|
| 1 | Routing hacia `inventory-service` | `200 OK` (si el backend está arriba) o `502` (si no) |
| 2 | WAF con `User-Agent: sqlmap/1.8` | `403 Forbidden` |
| 3 | 70 requests seguidas | `429 Too Many Requests` a partir de la #61 |

> 💡 Si ves `502` en el test 1: es porque `inventory-service-quarkus` no está corriendo. No es un fallo de Kong.

### Paso 5 – Editar `kong.yml` y recargar

Edita `kong.yml` (por ejemplo cambia `minute: 60` → `minute: 30`) y aplica **sin bajar el contenedor**:

```bash
./demo.sh reload
```

### Paso 6 – Ver logs en vivo

```bash
./demo.sh logs
```

### Paso 7 – Bajar todo

```bash
./demo.sh down
```

---

## 🪟 Paso a paso (Windows PowerShell)

Idéntico, pero con `.ps1`:

```powershell
cd modulo-07-api-gateway-enrutamiento\kong
.\demo.ps1 up
.\demo.ps1 smoke
.\demo.ps1 down
```

Si PowerShell bloquea el script:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

---

## 🌐 Cómo Kong llega a los backends de la máquina host

Dentro del contenedor, `localhost` es el propio contenedor — no tu Mac / PC.
Por eso `kong.yml` usa `http://host.docker.internal:8086` y
`docker-compose.yml` incluye:

```yaml
extra_hosts:
  - "host.docker.internal:host-gateway"
```

Compatibilidad de `host-gateway`:

| Motor | ¿Funciona? |
|-------|-----------|
| Docker Desktop (Mac / Windows / Linux) | ✅ nativo |
| Docker Engine Linux ≥ 20.10 | ✅ |
| Podman ≥ 4.7 | ✅ |
| Podman < 4.7 | ⚠️ usar la IP de la VM (`podman machine ssh -- ip route` → default) |

---

## 🩺 Troubleshooting

| Síntoma | Causa | Fix |
|---------|-------|-----|
| `./demo.sh: Permission denied` | Falta bit ejecutable | `chmod +x demo.sh` |
| `no encontre Docker ni Podman` | Ninguno instalado o en PATH | Instala Docker Desktop **o** verifica `podman machine start` |
| `502 Bad Gateway` en el proxy | Backend no arrancado | Levanta `inventory-service-quarkus` (`mvn quarkus:dev`) y/o `order-service-spring` |
| Puertos `8000` u `8001` ocupados | Otra app los usa | `lsof -i :8000` (Mac/Linux) o `Get-NetTCPConnection -LocalPort 8000` (Win) y liberarlos |
| Kong reinicia en loop | `kong.yml` inválido | `./demo.sh logs` → busca `failed to parse declarative config` |
| Podman en Mac: `host.docker.internal` no resuelve | Podman machine < 4.7 | Actualiza (`brew upgrade podman`) o usa IP de la VM |

---

## 📚 Siguiente lectura

- [`README.md`](README.md) – equivalencias detalladas Kong ↔ Spring Cloud Gateway.
- [`kong.yml`](kong.yml) – toda la config declarativa comentada.
