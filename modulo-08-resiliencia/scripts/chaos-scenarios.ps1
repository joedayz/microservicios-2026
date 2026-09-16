# chaos-scenarios.ps1 - version PowerShell para alumnos Windows.
# Activa/desactiva modos de falla en el flaky-downstream.
param(
    [Parameter(Position = 0)]
    [ValidateSet("ok", "fail30", "fail70", "slow", "timeout", "status", "help")]
    [string]$Scenario = "help"
)
$ErrorActionPreference = "Stop"

$FLAKY_URL = if ($env:FLAKY_URL) { $env:FLAKY_URL } else { "http://localhost:8090" }

function Apply($json) {
    Invoke-RestMethod -Method Post -Uri "$FLAKY_URL/flaky/config" `
      -ContentType 'application/json' -Body $json | ConvertTo-Json
}

switch ($Scenario) {
    "ok"      { Apply '{"failRate":0.0,"latencyMs":50,"mode":"OK"}' }
    "fail30"  { Apply '{"failRate":0.3,"latencyMs":100,"mode":"FAIL"}' }
    "fail70"  { Apply '{"failRate":0.7,"latencyMs":200,"mode":"FAIL"}' }
    "slow"    { Apply '{"failRate":0.0,"latencyMs":600,"mode":"SLOW"}' }
    "timeout" { Apply '{"failRate":0.0,"latencyMs":0,"mode":"TIMEOUT"}' }
    "status"  { Invoke-RestMethod -Uri "$FLAKY_URL/flaky/config" | ConvertTo-Json }
    default {
        @"
Uso: .\chaos-scenarios.ps1 <scenario>
  ok       - respuestas normales
  fail30   - 30% de fallos 500
  fail70   - 70% de fallos 500 (dispara Circuit Breaker)
  slow     - respuestas lentas (dispara slowCallRateThreshold)
  timeout  - cuelga la conexion (dispara TimeLimiter/Timeout)
  status   - muestra config actual del flaky
"@ | Write-Host
    }
}

