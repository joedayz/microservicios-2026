# Coloca una orden y muestra el traceId para Grafana Tempo.
$ErrorActionPreference = 'Stop'
$OrderUrl = if ($env:ORDER_URL) { $env:ORDER_URL } else { 'http://localhost:8087' }
$Tenant = if ($env:TENANT) { $env:TENANT } else { 'tienda-deportes' }

Write-Host "`n==> Colocando orden de $Tenant" -ForegroundColor Cyan
$body = @{
  orderId = 'O-obs-1'
  sku     = 'ZAP-RUN-42'
  qty     = 1
} | ConvertTo-Json

$response = Invoke-RestMethod -Method Post "$OrderUrl/api/v1/orders" `
  -ContentType 'application/json' `
  -Headers @{ 'X-Tenant-Id' = $Tenant } `
  -Body $body

$response | ConvertTo-Json -Depth 6
if (-not $response.traceId -or $response.traceId -eq 'none') {
  throw 'La orden no trajo traceId.'
}

Write-Host "`ntraceId: $($response.traceId)"
Write-Host "status: $($response.status)  degradation: $($response.degradation)"
Write-Host "Grafana: http://localhost:3000  (Explore -> Tempo)"
Write-Host "Loki:    {service_name=`"order-service-spring`"} | trace_id=`"$($response.traceId)`""
