# Relatório final — SBI (A3, Garantia da Qualidade de Software e Banco de Dados)

Situação em 14/10/2026, com o código das etapas 2A a 5 empilhado em PRs abertos (#13 a #17 e o PR desta etapa). Todo número abaixo vem de uma execução real; o comando ou a run da CI está indicado ao lado.

## 1. Entrega

| Etapa | Conteúdo | PR |
| --- | --- | --- |
| 2A Banco | Flyway V5/V6, seed demo, Hibernate em `validate` em todos os perfis, job `bd` | #13 |
| 2B Segurança | Hierarquia GERENTE > VENDEDOR, bloqueio de login, matriz de acesso executável, `ErrorResponse` com `code` | #14 |
| 2C Endpoints | As 45 operações do contrato v4 com testes, correção do D022 (V7), verificação ao vivo do contrato | #15 |
| 3 Frontend | Next.js em TypeScript estrito, Zod gerado do v4, PDV, vendas, clientes e telas de gestão, design SBI v1.1 e WCAG 2.2 AA | #16 |
| 4 Testes e CI | CI em quatro jobs, casos restantes, métricas R1, rastreabilidade | #17 |
| 5 Documentação | README, ambiente de demonstração em um comando, arquitetura, roteiro, este relatório | `docs/final-delivery` |

## 2. Testes executados

| Suíte | Resultado | Como reproduzir |
| --- | --- | --- |
| Java unitários | 233 testes, 0 falhas | `.\mvnw.cmd -q clean verify` |
| Java integração, no PostgreSQL 18 descartável via Docker Compose (`docker-compose.test.yml`, porta 15432) | 544 testes, 0 falhas, 0 ignorados | `scripts\test.ps1 -Mode Integration` |
| Matriz de acesso | 221 combinações (anônimo, vendedor, gerente) | `AccessMatrixTest`, dentro da integração |
| Contrato estático (Node) | 35/35; `audit-contract` sem erros | `npm test`; `node scripts\audit-contract.mjs` |
| Contrato ao vivo | 45 de 45 operações exercitadas, 0 divergências | `scripts\test-contract.ps1 -HttpOnly` (`reports/contrato/live-v4/live-contract.json`) |
| HTTP legado e sessão | 6/6 e 7/7 | mesmo runner |
| Frontend: lógica | 20/20 | `cd frontend; npm test` |
| Frontend: E2E e axe | 14/14 (CT-WEB-101 a 109, CT-E2E-002, CT-UI-001/002); sem violação WCAG 2.2 AA nas telas dos dois perfis; sem rolagem horizontal em 320 px | `scripts\test-contract.ps1 -HttpOnly -Frontend` |
| Banco (`psql`) | 51 PASS, 0 FAIL, inclusive a corrida da última unidade (CC-01) | job `bd` da CI |
| CI | run 38545269733: `test (backend)`, `bd`, `contract` e `frontend` com success (head `33d37cd`) | `gh run view 38545269733` |

### 2.1 Atualização depois da evolução (14/10/2026, WP-EVO-1 a WP-EVO-9)

A evolução (PRs #23 a #29, empilhados e não integrados) acrescentou cadastro de usuários com revogação de sessão, resumo do cliente, CNPJ alfanumérico, desconto com alçada, massa Olist e a marca SBI v1.0. Números da última execução, no topo da pilha (`docs/evolution`); a tabela acima fica como registro da etapa 5.

| Suíte | Resultado | Como reproduzir |
| --- | --- | --- |
| Java integração | 649 testes, 0 falhas, 0 ignorados | `.\mvnw.cmd -Pintegration clean verify` |
| Matriz de acesso | 260 combinações | `AccessMatrixTest`, dentro da integração |
| Contrato (Node) | 40/40 | `npm test` na raiz |
| Contrato ao vivo | contrato v5: 58 de 58 operações, 0 divergências | `scripts\test-contract.ps1 -HttpOnly` (`reports/contrato/live-v5/live-contract.json`) |
| Frontend: lógica | 36/36 | `cd frontend; npm test` |
| Frontend: E2E e axe | 24/24, inclusive marca (CT-WEB-120, 121), usuários, cliente, desconto | `scripts\test-contract.ps1 -HttpOnly -Frontend` |
| Banco (`psql`) | 61 PASS e 11 PASS da massa Olist | job `bd` da CI |
| CI | run 38584668603 (PR #29, `de887e9`): os quatro jobs com success | `gh run view 38584668603` |

As métricas abaixo (R1) são anteriores à evolução e não foram medidas de novo.

## 3. Métricas R0 × R1

| Indicador | R0 (06/10) | R1 (14/10) |
| --- | ---: | ---: |
| Testes Java | 17 | 544 |
| Cobertura de instruções | 33,3% | 93,1% |
| Cobertura de branches | 10% | 80,5% |
| Cobertura de linhas | 32,6% | 90,4% |
| PMD | 44 | 105 |
| CPD (blocos / linhas) | 9 / 241 | 18 / 253 |
| SpotBugs | 46 | 83 |

O código cresceu 3,4 vezes em instruções. PMD e SpotBugs cresceram sobretudo com estilo de chaves e `EI_EXPOSE_REP*` em `record`s. A análise completa está em [../reports/metricas/comparativo-r0-r1.md](../reports/metricas/comparativo-r0-r1.md).

## 4. Defeitos

Registro completo em [defeitos.md](defeitos.md).

- **Corrigidos e integrados à `main` na fase 0**: D003, D004, D006, D008, D010, D011, D012, D016, D017, D018, D019, D020 e D021.
- **Corrigidos nas etapas desta entrega**:
  - D022: cartão completo gravado pelo `/payment` legado;
  - D023: `GET /orders` legado com 500 diante de pedido de retirada;
  - D024: decimal aceito como inteiro;
  - D025: `enum` do contrato sem `null`;
  - D026: OpenAPI ao vivo divergente do v4.

  Cada um tem teste de regressão.
- **Encontrados pelos próprios testes durante o desenvolvimento**, antes de qualquer merge:
  - SQL com espaço perdido em bloco de texto (busca de clientes e extrato);
  - rolagem horizontal em 320 px causada por texto acessível posicionado fora da tabela;
  - E2E que dependia de variável de ambiente presente só no build.

## 5. Conformidade de contrato

- O contrato v4 tem as 45 operações `implemented`. Cada uma só mudou de status com teste verde. Resultados que podem ficar negativos (líquido de estornos, lucro, realizado) usam `SignedMoney`, sem mínimo; os demais valores continuam em `Money` ≥ 0 ([decisoes/dinheiro.md](decisoes/dinheiro.md)).
- O `live-contract.mjs` compara o `/v3/api-docs` com o v4 (caminho, método, parâmetros, corpo e códigos de sucesso) e valida com Ajv as respostas reais de sucesso e de erro.
- Os schemas Zod do frontend são gerados do v4, e a CI falha se houver diff (`npm run api:check`).
- Limite conhecido: os schemas do OpenAPI gerado não são comparados campo a campo (D-EXEC-07).

## 6. Banco de dados

- O esquema vem só do Flyway (V1–V7), com CHECKs como última barreira (estoque nunca negativo, cartão sem número completo, documento só com dígitos) e gatilhos para custo médio, histórico de preço, autor da promoção e soma das metas.
- O seed demo com as views confere com o modelo:
  - `GET /management/revenue?period=D90` no ambiente `docker-compose.demo.yml` respondeu faturamento líquido de R$ 186.400,00 e 65 pedidos pagos em 14/10/2026;
  - são os mesmos números registrados para `v_faturamento_diario` em [bd/modelo-erp-pdv.md](bd/modelo-erp-pdv.md).

## 7. Pendências reais

- Revisão humana e integração na ordem #13 → #14 → #15 → #16 → #17 → PR desta etapa (squash merge, feito pela equipe).
- Recriar o banco de desenvolvimento `postgres-order` com `pg_dump` antes (A2A-2): a V7 é nova.
- Decidir metas de qualidade (PMD, SpotBugs, cobertura) em `docs/decisoes/`.
- Ensaiar o [roteiro de demonstração](roteiro-demonstracao.md) com a equipe.
