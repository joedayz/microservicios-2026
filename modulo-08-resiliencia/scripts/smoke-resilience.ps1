# smoke-resilience.ps1 - version PowerShell para alumnos Windows.
# Dispara carga contra order-service y muestra las transiciones del Circuit Breaker.
$ErrorActionPreference = "Stop"

$ORDER_URL = if ($env:ORDER_URL) { $env:ORDER_URL } else { "http://localhost:8087" }
$FLAKY_URL = if ($env:FLAKY_URL) { $env:FLAKY_URL } else { "http://localhost:8090" }

function Banner($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }

Banner "0. Restaurando flaky a OK"
Invoke-RestMethod -Method Post -Uri "$FLAKY_URL/flaky/config" -ContentType 'application/json' `
  -Body '{"failRate":0.0,"latencyMs":50,"mode":"OK"}' | ConvertTo-Json

Banner "1. Readiness inicial"
Invoke-RestMethod -Uri "$ORDER_URL/actuator/health/readiness" | ConvertTo-Json -Depth 5

Banner "2. Configurando flaky para 70% de fallos"
Invoke-RestMethod -Method Post -Uri "$FLAKY_URL/flaky/config" -ContentType 'application/json' `
  -Body '{"failRate":0.7,"latencyMs":200,"mode":"FAIL"}' | ConvertTo-Json

Banner "3. Enviando 30 ordenes"
1..30 | ForEach-Object {
    try {
        $r = Invoke-WebRequest -Method Post -Uri "$ORDER_URL/api/v1/orders" `
          -ContentType 'application/json' `
          -Body ("{{`"orderId`":`"O-{0}`",`"sku`":`"ZAP-RUN-42`",`"qty`":1}}" -f $_) `
          -SkipHttpErrorCheck
        Write-Host -NoNewline "$($r.StatusCode) "
    } catch { Write-Host -NoNewline "ERR " }
}
Write-Host ""

Banner "4. Estado de los Circuit Breakers"
Invoke-RestMethod -Uri "$ORDER_URL/actuator/circuitbreakers" | ConvertTo-Json -Depth 6

Banner "5. Readiness (debe estar OUT_OF_SERVICE si el CB abrio)"
try {
    Invoke-RestMethod -Uri "$ORDER_URL/actuator/health/readiness" | ConvertTo-Json -Depth 5
} catch [Microsoft.PowerShell.Commands.HttpResponseException] {
    Write-Host $_.ErrorDetails.Message
}

Banner "6. Restaurando flaky a OK y esperando 12s"
Invoke-RestMethod -Method Post -Uri "$FLAKY_URL/flaky/config" -ContentType 'application/json' `
  -Body '{"failRate":0.0,"latencyMs":50,"mode":"OK"}' | ConvertTo-Json
Start-Sleep -Seconds 12

Banner "7. Enviando 10 ordenes para forzar HALF_OPEN -> CLOSED"
100..109 | ForEach-Object {
    try {
        $r = Invoke-WebRequest -Method Post -Uri "$ORDER_URL/api/v1/orders" `
          -ContentType 'application/json' `
          -Body ("{{`"orderId`":`"O-{0}`",`"sku`":`"ZAP-RUN-42`",`"qty`":1}}" -f $_) `
          -SkipHttpErrorCheck
        Write-Host -NoNewline "$($r.StatusCode) "
    } catch { Write-Host -NoNewline "ERR " }
}
Write-Host ""

Banner "8. Estado final de los Circuit Breakers"
Invoke-RestMethod -Uri "$ORDER_URL/actuator/circuitbreakers" | ConvertTo-Json -Depth 6
Invoke-RestMethod -Uri "$ORDER_URL/actuator/health/readiness" | ConvertTo-Json -Depth 5

