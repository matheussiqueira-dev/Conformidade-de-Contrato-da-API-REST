# Dinheiro: BigDecimal e NUMERIC(12,2) (DOM-02)

Data: 09/10/2026. Card: [DOM-02](../gestao/cards/DOM-02.md). Regra de origem: PEND-DOM-06 em [regras-loja.md](regras-loja.md) — dinheiro com 2 casas decimais e HALF_UP, confirmado por Matheus em 04/10/2026.

Este documento registra a tarefa T1 do card (inventário e estratégia) e o que foi entregue nas tarefas T2–T4 na branch `feat/DOM-02-bigdecimal` (commit `ce3f222`, PR #9). A revisão nominal do card continua pendente.

## Regra

| Item | Decisão |
| --- | --- |
| Tipo Java | `java.math.BigDecimal`, sempre construído a partir de texto ou de outro `BigDecimal` (nunca de `double`). |
| Escala | 2 casas decimais. |
| Arredondamento | `RoundingMode.HALF_UP` (1.005 → 1.01; 1.004 → 1.00). |
| Coluna | `NUMERIC(12,2)`: máximo `9999999999.99`. |
| Ponto único | `entities/Money.java` (`Money.of`, `PRECISION`, `SCALE`, `MAX`). Nenhum outro código chama `setScale` para dinheiro. |
| Fora do escopo | Quantidade (`Integer`) e peso (`Double`, kg) não são dinheiro e não foram convertidos. Percentual (1 casa) continua proposta em PEND-DOM-06 e ainda não existe no código. |

## Inventário (T1)

Levantado com `git grep -n -E '\bDouble\b|\bdouble\b' origin/main -- src/main/java` e conferido com `V1__legacy_schema.sql`. Caminhos relativos a `src/main/java/com/swee/ordermanagementspring/`.

### Persistido

| Campo | Classe | Coluna | Antes | Depois | Onde normaliza |
| --- | --- | --- | --- | --- | --- |
| Preço do produto | `entities/product/Product.price` | `product.price` | `Double` / `double precision` | `BigDecimal` / `numeric(12,2)` | construtor e `setPrice` |
| Preço histórico do item | `entities/OrderItem.price` | `order_item.price` | `Double` / `double precision` | `BigDecimal` / `numeric(12,2)` | construtor e `setPrice` |
| Valor do pagamento | `entities/payment/Payment.amount` (Pix, Cartão, Boleto) | `payment.amount` | `Double` / `double precision` | `BigDecimal` / `numeric(12,2)` | construtor e `setAmount` |

### Calculado (não persistido)

| Valor | Método | Antes | Depois |
| --- | --- | --- | --- |
| Subtotal do item | `OrderItem.subTotal()` | `quantity * price` em `double` | `Money.of(price × quantity)`: uma única multiplicação sobre o preço histórico |
| Total do pedido | `Order.total()` | soma em `double` | soma de subtotais já em 2 casas; pedido vazio = `0.00` |
| Frete | `Product.calculateShippingValue()` | `double` (20.0 + 8.0 por kg acima de 2) | `BigDecimal`: `20.00` + `8.00` × kg excedente, normalizado com `Money.of`; digital = `0.00` |
| Comparação pagamento × total | `services/PaymentRules.requireAmountMatchesTotal` | `Double` convertido para centavos, com guarda contra `NaN`/`Infinity` | `BigDecimal` dos dois lados em 2 casas; a guarda deixou de ser necessária |

### API (DTOs)

| DTO | Campo | Direção |
| --- | --- | --- |
| `ProductRequestDTO` / `ProductResponseDTO` | `price` | entrada / saída |
| `PaymentRequestDTO` / `PaymentResponseDTO` | `amount` | entrada / saída |
| `OrderPaymentRequestDTO` | `amount` | entrada |
| `OrderItemResponseDTO` | `price` | saída |
| `OrderResponseDTO` | `total` | saída |

Mantidos como `Double`: `ProductRequestDTO.weight`, `ProductResponseDTO.weight` e `PhysicalProduct.weight`. O frontend (`frontend/app`) não lê nem calcula preço, total ou valor; nenhuma mudança foi necessária lá.

## Entrada e validação

- Os campos de entrada mantêm `@NotNull` e `@Positive` e ganharam `@DecimalMax(Money.MAX)`. Valor acima de `9999999999.99` retorna 400 antes de chegar ao banco.
- Valor não numérico (ex.: `"price": "abc"`) ou JSON malformado agora retorna 400 `Validation Error` pelo `GlobalExceptionHandler` (`HttpMessageNotReadableException`); antes resultava em 500.
- **Mais de 2 casas é aceito e normalizado, não rejeitado:** `10.005` vira `10.01` na entidade. Para pagamento, a comparação com o total também é feita em 2 casas, então `100.004` é aceito para um pedido de `100.00`. Se a equipe preferir rejeitar, a mudança é acrescentar `@Digits(integer = 10, fraction = 2)` aos DTOs de entrada; essa decisão não foi tomada.

## JSON e contrato

O tipo no JSON continua `number`. Jackson serializa o `BigDecimal` com a escala da entidade (`200.00`); clientes JavaScript leem como `200`. Os baselines em `config/openapi/` (03/10 e 04/10) ainda registram `format: double` para esses campos; eles documentam o estado anterior e não foram regenerados. O CI de contrato, OpenAPI ao vivo e E2E passou com a mudança (ver Evidências).

## Migração V3

Arquivo: `src/main/resources/db/migration/V3__money_numeric.sql`.

```sql
ALTER TABLE product    ALTER COLUMN price  TYPE numeric(12,2) USING round(price::numeric, 2);
ALTER TABLE order_item ALTER COLUMN price  TYPE numeric(12,2) USING round(price::numeric, 2);
ALTER TABLE payment    ALTER COLUMN amount TYPE numeric(12,2) USING round(amount::numeric, 2);
```

- `round(numeric, 2)` do PostgreSQL arredonda metade para longe do zero, o que equivale a HALF_UP para os valores positivos validados pela API.
- Por que 12,2: cobre até 9.999.999.999,99, muito acima de qualquer preço ou pedido da loja, e mantém o limite igual em coluna e DTO.
- Valor legado acima do limite faz a migração **falhar** (`numeric field overflow`); a V3 não trunca nada em silêncio. O PostgreSQL executa a V3 em uma transação, então uma falha não deixa colunas meio convertidas.
- Os pedidos existentes mantêm o preço histórico do item: a V3 só arredonda `order_item.price` e não lê `product.price`.

### Antes de migrar um banco real

Seguir o roteiro de [migracoes.md](../bd/migracoes.md) (backup, cópia, comparação de esquema, baseline explícito). Na cópia, antes da V3, gerar o relatório do que muda com o arredondamento:

```sql
SELECT 'product' AS tabela, id, price AS antes, round(price::numeric, 2) AS depois
  FROM product WHERE price::numeric <> round(price::numeric, 2)
UNION ALL
SELECT 'order_item', id, price, round(price::numeric, 2)
  FROM order_item WHERE price::numeric <> round(price::numeric, 2)
UNION ALL
SELECT 'payment', id, amount, round(amount::numeric, 2)
  FROM payment WHERE amount::numeric <> round(amount::numeric, 2);

SELECT max(price) FROM product;
SELECT max(price) FROM order_item;
SELECT max(amount) FROM payment;  -- nenhum pode passar de 9999999999.99
```

Registrar o resultado (quantidade de linhas alteradas e maior diferença) junto da evidência da migração. Se aparecer valor acima do limite, corrigir o dado na origem com a equipe; não aumentar a precisão nem truncar só para a migração passar.

### Rollback

A aplicação depois da V3 espera `numeric` (Hibernate `validate`). Voltar só a coluna para `double precision` deixaria a versão nova sem subir, e a versão antiga perderia os valores já arredondados. O rollback suportado é: interromper escritas, restaurar o backup ensaiado e voltar para a versão da aplicação anterior ao DOM-02. Não usar `flyway clean`.

## Evidências

| Cenário do card | Teste | Resultado |
| --- | --- | --- |
| 100.00 × 2 → subtotal 200.00, preço histórico 100.00 | `ProductAndOrderCalculationTest.itemSubtotalMultipliesQuantityOnce`, `subtotalKeepsHistoricalUnitPrice` | passou |
| 1.005 → 1.01 HALF_UP | `ProductAndOrderCalculationTest.moneyUsesTwoDecimalsHalfUp` | passou |
| 0.10 + 0.20 = 0.30 | `ProductAndOrderCalculationTest.itemsWithCentsAddExactly` | passou |
| Valor zero, negativo ou acima do limite rejeitado pela validação do DTO (que o `GlobalExceptionHandler` já converte em 400) | `MoneyValidationTest.productPriceLimits`, `paymentAmountLimits` | passou |
| Valor não numérico (`"abc"`) → 400 | sem teste automatizado; coberto só pelo handler `HttpMessageNotReadableException` | não verificado por teste |
| Migração de massa sintética double → numeric (1.005 → 1.01; 0.1 + 0.2 → 0.30; 1.004 → 1.00) | `FlywayMigrationTest.v3ConvertsLegacyDoubleMoneyToNumericHalfUp` | passou |
| Limite da coluna sem overflow silencioso | `FlywayMigrationTest.v3FailsInsteadOfTruncatingValuesAboveColumnLimit` | passou |
| Instalação limpa V1 → V3 | `FlywayMigrationTest.emptyDatabaseMigratesThroughV3AndRejectsUnknownSeller` | passou |

Execução local em 09/10/2026 (Windows, JDK 25 portátil, PostgreSQL 18 via `docker-compose.test.yml`): `scripts/test.ps1 -Mode Unit` com 130 testes e 0 falhas; `scripts/test.ps1 -Mode Integration` com 137 testes e 0 falhas.

CI do GitHub no commit `ce3f222` (inclui Hibernate `validate` sobre o esquema Flyway, contrato HTTP e E2E Playwright):

- push: https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37960769072
- pull request: https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37960815649

Todos com massa sintética e bancos descartáveis; nenhum banco da loja foi migrado.

## Pendências

- Adicionar teste HTTP (MockMvc ou `scripts/http-contract.mjs`) para `"price": "abc"` → 400 sem persistência.
- Decidir se entrada com mais de 2 casas deve ser rejeitada (`@Digits`) em vez de normalizada.
- Executar o relatório de arredondamento e a V3 em uma cópia do banco real antes de qualquer migração em produção.
- Regenerar os baselines OpenAPI quando a equipe decidir atualizar o contrato publicado.
- Revisão nominal do card DOM-02.
