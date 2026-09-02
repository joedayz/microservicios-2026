# ============================================================================
#  Demo Azure APIM - Modulo 7 (Windows PowerShell)
# ============================================================================
[CmdletBinding()]
param(
    [ValidateSet('check','deploy','outputs','test','destroy')]
    [string]$Action = 'check'
)

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$ResourceGroup  = if ($env:RESOURCE_GROUP)  { $env:RESOURCE_GROUP }  else { 'rg-joedayz-modulo7' }
$Location       = if ($env:LOCATION)        { $env:LOCATION }        else { 'eastus' }
$ApimName       = if ($env:APIM_NAME)       { $env:APIM_NAME }       else { "apim-joedayz-$(Get-Random)" }
$DeploymentName = if ($env:DEPLOYMENT_NAME) { $env:DEPLOYMENT_NAME } else { 'modulo7-edge' }

function Check-Prereqs {
    if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
        Write-Host "[ERROR] Falta 'az' (Azure CLI). Instala desde: https://learn.microsoft.com/cli/azure/install-azure-cli" -ForegroundColor Red
        exit 1
    }
    Write-Host "[info] Azure CLI: $(az version --query '\"azure-cli\"' -o tsv)"
    az account show > $null 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] No hay sesion activa. Ejecuta: az login" -ForegroundColor Red
        exit 1
    }
    $subName = az account show --query name -o tsv
    $subId   = az account show --query id -o tsv
    Write-Host "[info] Subscription: $subName [$subId]"
    Write-Host "[info] Resource group: $ResourceGroup  |  Location: $Location"
    Write-Host "[info] APIM name:      $ApimName"
    Write-Host "[ok] Prerequisitos OK" -ForegroundColor Green
}

function Show-Outputs {
    az deployment group show --resource-group $ResourceGroup --name $DeploymentName `
        --query 'properties.outputs' -o json
}

switch ($Action) {
    'check' { Check-Prereqs }

    'deploy' {
        Check-Prereqs
        Write-Host "[info] Paso 1: crear resource group"
        az group create -n $ResourceGroup -l $Location -o table

        $PublisherEmail    = if ($env:PUBLISHER_EMAIL)      { $env:PUBLISHER_EMAIL }      else { 'demo@joedayz.pe' }
        $PublisherName     = if ($env:PUBLISHER_NAME)       { $env:PUBLISHER_NAME }       else { 'JoeDayz Demo' }
        $OrderBackendUrl   = if ($env:ORDER_BACKEND_URL)    { $env:ORDER_BACKEND_URL }    else { 'https://order-service.internal.joedayz.pe' }
        $InvBackendUrl     = if ($env:INVENTORY_BACKEND_URL){ $env:INVENTORY_BACKEND_URL} else { 'https://inventory-service.internal.joedayz.pe' }

        Write-Host "[info] Paso 2: desplegar Bicep (APIM Developer tarda 30-45min la 1ra vez)"
        az deployment group create `
            --resource-group $ResourceGroup `
            --name $DeploymentName `
            --template-file main.bicep `
            --parameters `
                "apimName=$ApimName" `
                "publisherEmail=$PublisherEmail" `
                "publisherName=$PublisherName" `
                "orderBackendUrl=$OrderBackendUrl" `
                "inventoryBackendUrl=$InvBackendUrl" `
            -o table

        Write-Host "[ok] Deployment terminado" -ForegroundColor Green
        Show-Outputs
    }

    'outputs' { Show-Outputs }

    'test' {
        $GatewayUrl = az deployment group show -g $ResourceGroup -n $DeploymentName `
            --query 'properties.outputs.apimGatewayUrl.value' -o tsv
        $SubId = az deployment group show -g $ResourceGroup -n $DeploymentName `
            --query 'properties.outputs.subscriptionKeyResourceId.value' -o tsv
        $Key = az rest --method post `
            --uri "https://management.azure.com${SubId}/listSecrets?api-version=2023-05-01-preview" `
            --query primaryKey -o tsv
        Write-Host "[info] Gateway: $GatewayUrl"
        Write-Host "[info] Sub key: $($Key.Substring(0,8))..."
        $Jwt = if (Test-Path token.txt) { Get-Content token.txt -Raw } else { 'dummy' }
        Invoke-WebRequest -UseBasicParsing `
            -Uri "$GatewayUrl/gateway/inventory/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE" `
            -Headers @{
                'Ocp-Apim-Subscription-Key' = $Key
                'X-Tenant-ID' = 'tienda-deportes'
                'Authorization' = "Bearer $Jwt"
            } | Select-Object StatusCode, Content
    }

    'destroy' {
        az group delete -n $ResourceGroup --yes --no-wait
        Write-Host "[ok] Resource group '$ResourceGroup' en eliminacion (async)" -ForegroundColor Green
    }
}
