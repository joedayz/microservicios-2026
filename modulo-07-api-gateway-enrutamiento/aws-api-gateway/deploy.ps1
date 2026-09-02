# ============================================================================
#  Demo AWS API Gateway - Modulo 7 (Windows PowerShell)
# ============================================================================
[CmdletBinding()]
param(
    [ValidateSet('check','deploy','outputs','test','destroy')]
    [string]$Action = 'check'
)

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

$StackName = if ($env:STACK_NAME) { $env:STACK_NAME } else { 'joedayz-edge-modulo7' }
$Region    = if ($env:AWS_REGION) { $env:AWS_REGION } else { 'us-east-1' }

function Require-Cmd { param($Name, $HelpUrl)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        Write-Host "[ERROR] Falta '$Name'. Instala: $HelpUrl" -ForegroundColor Red
        exit 1
    }
}

function Check-Prereqs {
    Require-Cmd 'aws' 'https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html'
    Require-Cmd 'sam' 'https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html'
    aws --version
    sam --version
    Write-Host "[info] Verificando credenciales..."
    aws sts get-caller-identity --output table
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[ERROR] No hay credenciales activas. Ejecuta: aws configure" -ForegroundColor Red
        exit 1
    }
    Write-Host "[info] Region: $Region"
    Write-Host "[ok] Prerequisitos OK" -ForegroundColor Green
}

function Show-Outputs {
    aws cloudformation describe-stacks --stack-name $StackName --region $Region `
        --query 'Stacks[0].Outputs' --output table
}

switch ($Action) {
    'check' { Check-Prereqs }

    'deploy' {
        Check-Prereqs
        Write-Host "[info] sam build"
        sam build --template template.yaml

        $OrderBackend     = if ($env:ORDER_BACKEND)      { $env:ORDER_BACKEND }     else { 'order-service.internal.joedayz.pe' }
        $InventoryBackend = if ($env:INVENTORY_BACKEND)  { $env:INVENTORY_BACKEND } else { 'inventory-service.internal.joedayz.pe' }
        $VpcLinkId        = if ($env:VPC_LINK_ID)        { $env:VPC_LINK_ID }       else { 'none' }
        $KeycloakIssuer   = if ($env:KEYCLOAK_ISSUER)    { $env:KEYCLOAK_ISSUER }   else { 'http://localhost:8180/realms/joedayz-microservices' }

        Write-Host "[info] sam deploy hacia stack='$StackName' region='$Region'"
        sam deploy `
            --stack-name $StackName `
            --region $Region `
            --capabilities CAPABILITY_IAM `
            --resolve-s3 `
            --no-confirm-changeset `
            --no-fail-on-empty-changeset `
            --parameter-overrides `
                "OrderBackend=$OrderBackend" `
                "InventoryBackend=$InventoryBackend" `
                "VpcLinkId=$VpcLinkId" `
                "KeycloakIssuer=$KeycloakIssuer"

        Write-Host "[ok] Stack desplegado" -ForegroundColor Green
        Show-Outputs
    }

    'outputs' { Show-Outputs }

    'test' {
        Require-Cmd 'aws' ''
        $ApiUrl = aws cloudformation describe-stacks --stack-name $StackName --region $Region `
            --query 'Stacks[0].Outputs[?OutputKey==`ApiInvokeUrl`].OutputValue' --output text
        $KeyId = aws cloudformation describe-stacks --stack-name $StackName --region $Region `
            --query 'Stacks[0].Outputs[?OutputKey==`DemoApiKeyId`].OutputValue' --output text
        $KeyValue = aws apigateway get-api-key --api-key $KeyId --include-value `
            --region $Region --query 'value' --output text
        Write-Host "[info] URL: $ApiUrl"
        Write-Host "[info] Key: $($KeyValue.Substring(0,8))..."
        Invoke-WebRequest -UseBasicParsing `
            -Uri "$ApiUrl/gateway/inventory/api/v1/tenants/tienda-deportes/inventory/ZAP-RUN-42?region=PE" `
            -Headers @{ 'x-api-key' = $KeyValue } | Select-Object StatusCode, Content
    }

    'destroy' {
        Require-Cmd 'aws' ''
        aws cloudformation delete-stack --stack-name $StackName --region $Region
        Write-Host "[ok] Stack en eliminacion" -ForegroundColor Green
    }
}
