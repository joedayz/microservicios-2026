// Módulo 7 - Azure API Management edge (SKU Developer para demo).
// Despliega:
//   * Instancia APIM.
//   * Producto "joedayz-microservices-edge".
//   * APIs "orders" e "inventory" con operaciones y policies XML.
//   * Suscripcion demo con clave visible en outputs.

@description('Nombre unico del servicio APIM (3-50 chars, minusculas).')
param apimName string

@description('Correo del publisher (obligatorio en APIM).')
param publisherEmail string = 'demo@joedayz.pe'

@description('Nombre del publisher.')
param publisherName string = 'JoeDayz Demo'

@description('Location (Azure region).')
param location string = resourceGroup().location

@description('URL publica del order-service (backend).')
param orderBackendUrl string = 'https://order-service.internal.joedayz.pe'

@description('URL publica del inventory-service (backend).')
param inventoryBackendUrl string = 'https://inventory-service.internal.joedayz.pe'

@description('Contenido policy global (leer con loadTextContent).')
param globalPolicyXml string = loadTextContent('policies/global.xml')

@description('Contenido policy orders (leer con loadTextContent).')
param ordersPolicyXml string = loadTextContent('policies/orders.xml')

@description('Contenido policy inventory (leer con loadTextContent).')
param inventoryPolicyXml string = loadTextContent('policies/inventory.xml')

resource apim 'Microsoft.ApiManagement/service@2023-05-01-preview' = {
  name: apimName
  location: location
  sku: {
    name: 'Developer'
    capacity: 1
  }
  properties: {
    publisherEmail: publisherEmail
    publisherName: publisherName
    virtualNetworkType: 'None'
  }
  identity: {
    type: 'SystemAssigned'
  }
}

resource product 'Microsoft.ApiManagement/service/products@2023-05-01-preview' = {
  parent: apim
  name: 'joedayz-microservices-edge'
  properties: {
    displayName: 'JoeDayz Microservices Edge'
    description: 'Producto demo modulo 7'
    subscriptionRequired: true
    approvalRequired: false
    state: 'published'
  }
}

resource productPolicy 'Microsoft.ApiManagement/service/products/policies@2023-05-01-preview' = {
  parent: product
  name: 'policy'
  properties: {
    format: 'rawxml'
    value: globalPolicyXml
  }
}

// ---------- Orders API ----------
resource ordersApi 'Microsoft.ApiManagement/service/apis@2023-05-01-preview' = {
  parent: apim
  name: 'orders'
  properties: {
    displayName: 'Orders API'
    path: 'gateway/orders'
    protocols: ['https']
    serviceUrl: orderBackendUrl
    subscriptionRequired: true
  }
}

resource ordersListOp 'Microsoft.ApiManagement/service/apis/operations@2023-05-01-preview' = {
  parent: ordersApi
  name: 'list-orders'
  properties: {
    displayName: 'List orders'
    method: 'GET'
    urlTemplate: '/api/v1/tenants/{tenantId}/orders'
    templateParameters: [
      { name: 'tenantId', type: 'string', required: true }
    ]
  }
}

resource ordersListPolicy 'Microsoft.ApiManagement/service/apis/operations/policies@2023-05-01-preview' = {
  parent: ordersListOp
  name: 'policy'
  properties: {
    format: 'rawxml'
    value: ordersPolicyXml
  }
}

resource ordersCreateOp 'Microsoft.ApiManagement/service/apis/operations@2023-05-01-preview' = {
  parent: ordersApi
  name: 'create-order'
  properties: {
    displayName: 'Create order'
    method: 'POST'
    urlTemplate: '/api/v1/tenants/{tenantId}/orders'
    templateParameters: [
      { name: 'tenantId', type: 'string', required: true }
    ]
  }
}

// ---------- Inventory API ----------
resource inventoryApi 'Microsoft.ApiManagement/service/apis@2023-05-01-preview' = {
  parent: apim
  name: 'inventory'
  properties: {
    displayName: 'Inventory API'
    path: 'gateway/inventory'
    protocols: ['https']
    serviceUrl: inventoryBackendUrl
    subscriptionRequired: true
  }
}

resource inventoryGetOp 'Microsoft.ApiManagement/service/apis/operations@2023-05-01-preview' = {
  parent: inventoryApi
  name: 'get-inventory'
  properties: {
    displayName: 'Get inventory item'
    method: 'GET'
    urlTemplate: '/api/v1/tenants/{tenantId}/inventory/{sku}'
    templateParameters: [
      { name: 'tenantId', type: 'string', required: true }
      { name: 'sku', type: 'string', required: true }
    ]
  }
}

resource inventoryGetPolicy 'Microsoft.ApiManagement/service/apis/operations/policies@2023-05-01-preview' = {
  parent: inventoryGetOp
  name: 'policy'
  properties: {
    format: 'rawxml'
    value: inventoryPolicyXml
  }
}

// ---------- Product ↔ API links ----------
resource productOrders 'Microsoft.ApiManagement/service/products/apis@2023-05-01-preview' = {
  parent: product
  name: 'orders'
  dependsOn: [ ordersApi ]
}

resource productInventory 'Microsoft.ApiManagement/service/products/apis@2023-05-01-preview' = {
  parent: product
  name: 'inventory'
  dependsOn: [ inventoryApi ]
}

// ---------- Suscripcion demo ----------
resource demoSubscription 'Microsoft.ApiManagement/service/subscriptions@2023-05-01-preview' = {
  parent: apim
  name: 'demo-subscription'
  properties: {
    displayName: 'Demo subscription'
    scope: '/products/${product.name}'
    state: 'active'
  }
}

output apimGatewayUrl string = apim.properties.gatewayUrl
output subscriptionKeyResourceId string = demoSubscription.id
output tip string = 'Obtener la clave con: az apim subscription show --ids ${demoSubscription.id} --query primaryKey -o tsv'
