# API legada (rotas da versão anterior ao ERP/PDV)

As rotas `/orders`, `/payment` e `/address`, e `POST /customers/{id}/address`, continuam disponíveis e só aceitam o GERENTE. O contrato delas está nos baselines `config/openapi/baseline-*.json`; as telas do SBI usam o contrato v4 (`config/openapi/target-loja-gestao-openapi-2026-10-14-v4.json`). Os erros seguem o mesmo `ErrorResponse` com `code` do v4.

Desde a ETAPA 2C, o cartão grava só os 4 últimos dígitos (D022).

## Exemplos das rotas legadas

### Criar um pagamento PIX (`POST /payment`)

**Request:**
```json
{
  "type": "PIX",
  "amount": 150.00,
  "orderId": 3,
  "pixKey": "carla@email.com",
  "pixHolderName": "Carla Souza"
}
```

**Response (200):**
```json
{
  "id": 8,
  "type": "PIX",
  "amount": 150.00,
  "status": "PENDING",
  "orderId": 3,
  "pixKey": "carla@email.com",
  "pixHolderName": "Carla Souza"
}
```

### Criar um pedido completo (`POST /orders`)

Suporta tanto um cliente já existente (`clientId`) quanto os dados de um cliente novo (`client`).

**Request:**
```json
{
  "client": {
    "type": "INDIVIDUAL",
    "name": "Carla Souza",
    "email": "carla@email.com",
    "birthDate": "1990-04-15",
    "cpf": "14445677788"
  },
  "items": [
    { "productId": 1, "quantity": 1 }
  ],
  "payment": {
    "type": "PIX",
    "amount": 450.00,
    "pixKey": "carla@email.com",
    "pixHolderName": "Carla Souza"
  },
  "shippingAddress": {
    "street": "Rua das Flores",
    "number": "2",
    "complement": "Casa",
    "neighborhood": "Boa Viagem",
    "city": "Recife",
    "state": "PE",
    "zipCode": "50000000"
  }
}
```

**Response (200):**
```json
{
  "id": 6,
  "moment": "2026-08-31T11:19:31.88",
  "status": "PENDING_PAYMENT",
  "client": {
    "id": 6,
    "type": "INDIVIDUAL",
    "name": "Carla Souza",
    "email": "carla@email.com",
    "birthDate": "1990-04-15",
    "cpf": "14445677788"
  },
  "items": [
    {
      "id": 6,
      "product": {
        "id": 1,
        "type": "PHYSICAL",
        "name": "Micro-ondas",
        "price": 450.00,
        "description": "Micro-ondas 30L",
        "weight": 5.0
      },
      "quantity": 1,
      "price": 450.00
    }
  ],
  "payment": {
    "id": 8,
    "type": "PIX",
    "amount": 450.00,
    "status": "PENDING",
    "pixKey": "carla@email.com",
    "pixHolderName": "Carla Souza"
  },
  "shippingAddress": {
    "id": 5,
    "street": "Rua das Flores",
    "number": "2",
    "complement": "Casa",
    "neighborhood": "Boa Viagem",
    "city": "Recife",
    "state": "PE",
    "zipCode": "50000000"
  },
  "total": 450.00
}
```
