# ============================================================================
#  Demo Kong Gateway - Modulo 7 (Windows PowerShell)
#  ---------------------------------------------------------------------------
#  Version PowerShell del demo.sh para alumnos en Windows sin WSL / Git Bash.
#  Detecta Docker Desktop o Podman Desktop automaticamente.
#
#  Uso:
#    .\demo.ps1 up
#    .\demo.ps1 status
#    .\demo.ps1 smoke
#    .\demo.ps1 reload
#    .\demo.ps1 logs
#    .\demo.ps1 down
# ============================================================================
[CmdletBinding()]
param(
    [ValidateSet('up','status','smoke','reload','logs','down')]
    [string]$Action = 'up'
)

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$ProxyUrl = if ($env:PROXY_URL) { $env:PROXY_URL } else { 'http://localhost:8000' }
$AdminUrl = if ($env:ADMIN_URL) { $env:ADMIN_URL } else { 'http://localhost:8001' }

function Get-Compose {
    if (Get-Command docker -ErrorAction SilentlyContinue) {
        $r = docker compose version 2>$null
        if ($LASTEXITCODE -eq 0) { return @('docker','compose') }
    }
    if (Get-Command podman -ErrorAction SilentlyContinue) {
        $r = podman compose version 2>$null
        if ($LASTEXITCODE -eq 0) { return @('podman','compose') }
    }
    if (Get-Command podman-compose -ErrorAction SilentlyContinue) {
        return @('podman-compose')
    }
    return $null
}

$Compose = Get-Compose
if (-not $Compose) {
    Write-Host "[ERROR] No encontre Docker ni Podman con compose." -ForegroundColor Red
    Write-Host "Instala Docker Desktop o Podman Desktop y vuelve a intentar."
    exit 1
}
Write-Host "[info] Motor: $($Compose -join ' ')"

function Invoke-Compose { param([string[]]$Args)
    & $Compose[0] @($Compose[1..($Compose.Length-1)] + $Args)
}

function Wait-For { param([string]$Url, [int]$TimeoutSec = 90)
    Write-Host "[wait] $Url ..."
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
        try {
            $r = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 3
            if ($r.StatusCode -eq 200) { Write-Host "[ok] $Url respondio"; return }
        } catch { Start-Sleep -Seconds 2 }
    }
    throw "Timeout esperando $Url"
}

switch ($Action) {
    'up' {
        Write-Host "== Paso 1: down previo =="
        Invoke-Compose @('down','-v') 2>$null | Out-Null
        Write-Host "== Paso 2: docker compose up -d =="
        Invoke-Compose @('up','-d')
        Write-Host "== Paso 3: esperar admin API =="
        Wait-For "$AdminUrl/status" 90
        Write-Host "== Paso 4: verificar kong.yml =="
        (Invoke-WebRequest -UseBasicParsing "$AdminUrl/services").Content.Substring(0,[Math]::Min(500,$_.Length))
        Write-Host "`n[LISTO]  Proxy:  $ProxyUrl"
        Write-Host   "         Admin:  $AdminUrl"
        Write-Host   "         Sigue:  .\demo.ps1 smoke"
    }
    'status' {
        'services','routes','plugins' | ForEach-Object {
            Write-Host "== $_ =="
            (Invoke-WebRequest -UseBasicParsing "$AdminUrl/$_").Content
        }
    }
    'smoke' {
        Write-Host "== Test 1: routing =="
        try {
            Invoke-WebRequest -UseBasicParsing "$ProxyUrl/gateway/inventory/api/v1/tenants/tienda-deportes/inventory" `
                -Headers @{ 'X-Tenant-ID'='tienda-deportes' } | Select-Object StatusCode, StatusDescription
        } catch { Write-Host $_.Exception.Message }

        Write-Host "`n== Test 2: WAF (esperado 403) =="
        try {
            Invoke-WebRequest -UseBasicParsing "$ProxyUrl/gateway/orders/api/v1/tenants/tienda-deportes/orders" `
                -Headers @{ 'User-Agent'='sqlmap/1.8'; 'X-Tenant-ID'='tienda-deportes' } | Select-Object StatusCode
        } catch { Write-Host "StatusCode: $($_.Exception.Response.StatusCode.value__)" }

        Write-Host "`n== Test 3: rate-limit (esperamos 429 antes de la #70) =="
        for ($i=1; $i -le 70; $i++) {
            try {
                $r = Invoke-WebRequest -UseBasicParsing "$ProxyUrl/gateway/inventory/api/v1/tenants/demo-rate/inventory" `
                    -Headers @{ 'X-Tenant-ID'='demo-rate' }
                $code = $r.StatusCode
            } catch { $code = $_.Exception.Response.StatusCode.value__ }
            if ($code -eq 429) { Write-Host "[OK] 429 en la request #$i"; break }
            if ($i % 10 -eq 0) { Write-Host "  #$i -> $code" }
        }
    }
    'reload' {
        Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$AdminUrl/config" `
            -Form @{ config = Get-Item .\kong.yml } | Out-Null
        Write-Host "[ok] kong.yml recargado"
    }
    'logs'   { Invoke-Compose @('logs','-f','kong') }
    'down'   { Invoke-Compose @('down','-v'); Write-Host "[ok] Kong detenido" }
}
