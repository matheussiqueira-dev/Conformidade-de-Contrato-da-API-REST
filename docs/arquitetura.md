# Arquitetura do SBI

## Componentes

```mermaid
flowchart LR
  subgraph Navegador
    UI["Next.js 16 (React 19, TypeScript)<br/>telas /vender, /vendas, /clientes, /gestao..."]
  end
  subgraph "Servidor Next"
    PX["proxy.ts<br/>sem cookie -> /entrar"]
    SSR["layouts por área<br/>GET /auth/me com o cookie"]
    RW["rewrite /api/backend/* -> API"]
  end
  subgraph "API Spring Boot 4.1"
    SEC["SecurityFilterChain<br/>sessão + CSRF, regras de URL"]
    CTL["Controllers finos<br/>Bean Validation"]
    SVC["Serviços<br/>@PreAuthorize + @Transactional"]
    REPO["Repositórios JdbcClient<br/>(JPA só no legado)"]
    ERR["GlobalExceptionHandler<br/>ErrorResponse com code"]
  end
  DB[("PostgreSQL 18<br/>Flyway V1–V7, CHECKs, gatilhos, views V6")]
  UI --> PX --> SSR
  UI --> RW --> SEC --> CTL --> SVC --> REPO --> DB
  CTL -.-> ERR
  SSR --> SEC
```

- O frontend fala com a API sempre pela mesma origem (`/api/backend/*`, rewrite do Next). O cookie de sessão é HttpOnly e o token CSRF vai num cabeçalho em toda mutação (`frontend/lib/api/http.ts`).
- Os schemas Zod do frontend são gerados do contrato v4 pelo orval (`frontend/lib/api/generated/`). A CI falha se a geração produzir diff.
- A API valida a entrada com Bean Validation, responde todo erro como `ErrorResponse` com `code` e documenta as operações no Springdoc. O `scripts/live-contract.mjs` compara o OpenAPI ao vivo com o v4.

## Camadas da API (módulos novos em `api/<módulo>/`)

| Camada | Papel | Exemplo |
| --- | --- | --- |
| Controller | Parâmetros (`@Valid`, `@Min`, `@Size`), usuário da sessão (`@AuthenticationPrincipal`), status HTTP | `SalesController` |
| Serviço | Regras, `@PreAuthorize` (segunda barreira), `@Transactional`, dono do dado (terceira barreira), `DbSessionContext` para os gatilhos, auditoria | `SalesService` |
| Repositório | SQL explícito com `JdbcClient` e `record`; nunca devolve custo ao vendedor | `SalesRepository` |
| DTOs | `record`s com Bean Validation; polimorfismo com `sealed interface` + `@JsonTypeInfo` | `SalesDtos` |

Módulos: `sales` (PDV), `customers`, `backoffice` (produtos, categorias, estoque), `management` (faturamento, metas, estorno, análises), `promotions` e `common` (Money, paginação, endereço, usuário da sessão).

## Fluxo de venda

```mermaid
sequenceDiagram
  participant V as Vendedor (/vender)
  participant A as API
  participant B as PostgreSQL
  V->>A: POST /sales/quote (itens)
  A->>B: preços, promoções vigentes, disponível
  A-->>V: subtotal, desconto, total (calculados no servidor)
  V->>A: POST /sales/orders + Idempotency-Key
  A->>B: INSERT orders (UNIQUE seller_id + client_request_id)
  loop itens em ordem crescente de product_id
    A->>B: UPDATE stock_balance SET reserved = reserved + q WHERE on_hand - reserved >= q
  end
  A-->>V: 201 (ou 200 na repetição, 409 STOCK_INSUFFICIENT sem reserva parcial)
  V->>A: POST /sales/orders/{id}/payment
  A->>B: SELECT ... FOR UPDATE; baixa on_hand e reserved; grava custo do momento; SAIDA
  A-->>V: 200 PAID (repetir não tem efeito)
```

- O vendedor do pedido é sempre o usuário da sessão. Propriedade desconhecida no corpo responde 400, então ninguém envia preço, total ou vendedor.
- O cancelamento só vale para pedido pendente: libera a reserva com movimento `LIBERACAO`. Pedido pago responde 409.
- Os CHECKs do banco (`reserved <= on_hand`, `on_hand >= 0`) são a última barreira. A violação vira 409, nunca 500.

## Segurança em três barreiras

1. **URL**: `SecurityConfig` aplica a matriz `src/test/resources/security/access-matrix.csv`, com a hierarquia GERENTE > VENDEDOR. O `AccessMatrixTest` executa as 221 combinações (anônimo, vendedor, gerente) e falha se aparecer uma rota sem linha na matriz.
2. **Método**: `@PreAuthorize` em cada serviço, com a mesma hierarquia.
3. **Dono do dado**: o serviço confere o vendedor do pedido; objeto de outro dono responde 404, sem revelar que existe.

Complementos:
- sessão + CSRF;
- bloqueio após 5 falhas de login por 15 min, com resposta igual e BCrypt executado nos quatro casos;
- documento mascarado para o vendedor;
- custo e margem só para o gerente;
- cartão guarda só os 4 últimos dígitos;
- auditoria (`audit_event`) de login, cancelamento, ajuste de estoque, cliente e situação de produto.

## Matriz de acesso resumida

| Área | Anônimo | Vendedor | Gerente |
| --- | --- | --- | --- |
| `/auth/login`, `/auth/csrf`, `/auth/logout` | sim | sim | sim |
| PDV (`/sales/*`, `/orders/{id}/cancel`) | 401 | sim (só os próprios pedidos) | sim |
| Clientes (`/customers`, histórico) | 401 | sim (documento mascarado; não exclui) | sim |
| `/performance/me`, `/promotions/active` | 401 | sim | sim |
| Backoffice (`/products`, `/categories`, `/stock/*`) | 401 | 403 | sim |
| Gestão (`/management/*`, `/goals/*`, `/analytics/*`, `/promotions`, estorno) | 401 | 403 | sim |
| Rotas legadas (`/orders`, `/payment`, `/address`) | 401 | 403 | sim |

A matriz completa, linha a linha, está em [decisoes/matriz-acesso.md](decisoes/matriz-acesso.md).

## Banco

O Flyway é a única fonte do esquema, e o Hibernate roda em `validate` em todos os perfis.

| Migração | Conteúdo |
| --- | --- |
| V1–V4 | Legado e dinheiro em NUMERIC(12,2) |
| V5 | Catálogo, estoque com custo médio por gatilho, promoções, metas com soma checada no commit, idempotência e auditoria |
| V6 | Views dos painéis |
| V7 | Mascara cartões legados |

O seed demo fica em `db/demo`. Detalhes em [bd/modelo-erp-pdv.md](bd/modelo-erp-pdv.md).
