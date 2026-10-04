# Order Management Spring

Documentação e responsabilidades atualizadas por **Matheus Siqueira** em 04/10/2026: [índice dos 64 cards e briefings](docs/gestao/README.md), [PDFs e fontes de referência](docs/referencias/README.md). Código e evidências estão na branch `a3-contract-validation`, aguardando revisão da equipe.

API REST para gestão de pedidos, desenvolvida como projeto de portfólio para praticar arquitetura backend com Spring Boot, JPA/Hibernate e PostgreSQL.

Continuação de 04/10: contrato planejado v3 com sessão/CSRF, autenticação Spring e frontend Next.js de acesso. Veja [execução e limites desta rodada](docs/execucao-sessao.md). As rotas legadas exigem gerente; as contas sintéticas existem somente no runner de banco descartável. As áreas de vendas, metas e estoque seguem pendentes.

O sistema modela um fluxo completo de e-commerce: clientes (pessoa física ou jurídica), produtos (físicos ou digitais), pedidos com múltiplos itens, pagamentos (cartão, PIX ou boleto) e endereços de entrega.

## Stack

- **Java 25**
- **Spring Boot 4.1**
- **Spring Data JPA / Hibernate**
- **PostgreSQL 18** (rodando via Docker)
- **Bean Validation** (Jakarta Validation)
- **Springdoc OpenAPI** (Swagger UI)
- **Maven**

## Arquitetura

O projeto segue uma separação clássica em camadas:

```
controllers/   → endpoints REST
services/      → regras de negócio
repositories/  → acesso a dados (Spring Data JPA)
entities/      → modelo de domínio (JPA)
dto/           → objetos de request/response, isolando a API do modelo interno
exceptions/    → exceções customizadas + tratamento global de erros
```

### Modelagem com herança

Três entidades usam herança JPA (`SINGLE_TABLE`) para modelar variações de um mesmo conceito:

- **Client**: `IndividualClient` (pessoa física, com CPF) ou `CorporateClient` (pessoa jurídica, com CNPJ)
- **Product**: `PhysicalProduct` (com peso, calcula frete) ou `DigitalProduct` (com link de download, frete gratuito)
- **Payment**: `CardPayment`, `PixPayment` ou `BoletoPayment`, cada um com sua própria lógica de `processPayment()`

Os DTOs de request/response usam um campo `type` para indicar qual subtipo está sendo criado ou retornado, e os campos específicos de cada subtipo só aparecem no JSON quando fazem sentido (via `@JsonInclude(NON_NULL)`).

## Como rodar o projeto

### Execução do A3 no Windows

Com JDK 25 (`JAVA_HOME`) configurado, na pasta `order-management-api`:

```powershell
# Unitários, sem PostgreSQL
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1

# Unitários + contexto + persistência, com Docker Desktop ativo
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1 -Mode Integration

# Auditoria estrutural do contrato planejado, com Node.js
node --test scripts/audit-contract.test.mjs
node scripts/audit-contract.mjs
```

O runner pode localizar o JDK portátil já baixado em `../tmp/tools/jdk-25`; em outra máquina, configure `JAVA_HOME`. O modo integração cria e encerra um PostgreSQL exclusivo na porta 15432, database `order_management_test`, com `docker-compose.test.yml`. A massa `OrderFixtures` é sintética e cada teste de persistência usa rollback. Relatórios Java ficam em `target/surefire-reports` e são preservados como artefato no CI. Para testes diretos: `.\mvnw.cmd test` ou `.\mvnw.cmd -Pintegration verify` após iniciar o banco de teste.

Duas rodadas anteriores da suíte Java passaram com seis testes cada; veja [relatório histórico](reports/execucoes/run-2026-10-03-continuacao.md). A auditoria atual usa Ajv 8/2020-12, preserva a v2 e valida a v3 de sessão/CSRF. A revisão do baseline documenta 400/404. Autenticação e frontend de acesso foram adicionados; vendas, metas e estoque seguem pendentes. Veja [estado da implementação e verificações](docs/execucao-sessao.md).

Para concluir o spike de contrato e HTTP com banco descartável:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-contract.ps1
```

O runner instala as dependências Node fixadas, usa o schema oficial OpenAPI versionado e inicia uma API de teste na porta 18080 com banco exclusivo na porta 15432. Também verifica autenticação real com contas sintéticas. `-Frontend` acrescenta build/E2E do Next.js; `-Preview` mantém o sistema disponível depois dos testes. Não execute simultaneamente com `test.ps1 -Mode Integration`.

A [revisão dos erros](reports/execucoes/run-2026-10-04-erros-contrato.md) documenta POST `/products` 400 e GET `/products/{id}` 404 em um novo contrato, preservando o baseline original. Passaram 18 testes Node, a validação da revisão e os seis casos HTTP com validação do OpenAPI ao vivo e dos corpos de erro. Evidência conferida em 04/10 às 00:42:24: `http-swagger-errors-v1.json`. A exportação Springdoc documenta os erros e aceita details como array/null. Ver [rodada Swagger](reports/execucoes/run-2026-10-04-swagger-erros.md).

Para subir a API de desenvolvimento com o banco na porta 5432:

```powershell
docker compose up -d postgres
.\mvnw.cmd spring-boot:run
```

O `ddl-auto=update` atual é um baseline de desenvolvimento. As migrações Flyway do produto ainda estão pendentes.

### 1. Suba o PostgreSQL via Docker

```bash
docker run --name postgres-order \
  -e POSTGRES_PASSWORD=senha123 \
  -e POSTGRES_DB=order_management \
  -p 5432:5432 \
  -d postgres
```

### 2. Configure o `application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/order_management
spring.datasource.username=postgres
spring.datasource.password=senha123
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```

### 3. Rode a aplicação

Via Maven:
```bash
./mvnw spring-boot:run
```

Ou diretamente pela sua IDE, executando `OrderManagementSpringApplication`.

A API sobe em `http://localhost:8080`.

### 4. Explore pelo Swagger

Com a aplicação rodando, acesse:

```
http://localhost:8080/swagger-ui/index.html
```

Lá você encontra todos os endpoints documentados, com os DTOs de request e a opção de testar direto pelo navegador.

## Endpoints principais

| Recurso | Método | Rota | Descrição |
|---|---|---|---|
| Clients | GET | `/clients` | Lista todos os clientes |
| Clients | GET | `/clients/{id}` | Busca cliente por id |
| Clients | POST | `/clients` | Cria um cliente |
| Clients | PUT | `/clients/{id}` | Atualiza um cliente |
| Clients | DELETE | `/clients/{id}` | Remove um cliente |
| Products | GET / POST / PUT / DELETE | `/products` | CRUD de produtos |
| Address | GET / POST / PUT / DELETE | `/address` | CRUD de endereços |
| Payment | GET / POST | `/payment` | Consulta e criação de pagamentos |
| Payment | PUT | `/payment/{id}` | Edita um pagamento (somente se `PENDING`) |
| Payment | POST | `/payment/{id}/process` | Processa um pagamento pendente |
| Orders | GET / POST | `/orders` | Consulta e criação de pedidos completos |
| Orders | PUT | `/orders/{id}/status` | Atualiza o status do pedido |
| Orders | PUT | `/orders/{id}/address` | Atualiza o endereço de entrega (bloqueado após envio) |

## Tratamento de erros

Todas as respostas de erro seguem um formato padronizado:

```json
{
  "timestamp": "2026-08-31T11:33:01.32",
  "status": 404,
  "error": "Not Found",
  "message": "Payment not found, id: 9999",
  "details": null
}
```

| Situação | Status |
|---|---|
| Recurso não encontrado | 404 |
| Regra de negócio violada (ex: tipo de pagamento inválido) | 400 |
| Campos inválidos (Bean Validation) | 400, com lista em `details` |
| Erro inesperado | 500 |

## Exemplos de uso

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

## Possíveis próximos passos

- Autenticação e autorização com Spring Security + JWT
- Testes automatizados (unitários com JUnit/Mockito, integração com `@SpringBootTest`)
- Interface web consumindo a API
