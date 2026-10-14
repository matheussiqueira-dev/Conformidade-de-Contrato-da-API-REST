# Plano de testes

Data: 03/10/2026.

Este plano define como testar a Order Management API e a evolucao Loja Gestao de forma reproduzivel. Ele cobre o baseline atual, a evolucao API/BD e as quatro telas planejadas. Nenhum resultado de execucao e declarado aqui: este documento define estrategia, dados, selecao e evidencias.

## Objetivos

- Demonstrar conformidade entre requisitos, contrato, API, banco e interface.
- Preservar o baseline original antes das ampliacoes.
- Exercitar comportamento positivo, negativo, autorizacao por perfil, integridade de estoque e regras monetarias.
- Registrar evidencias rastreaveis para a rubrica academica: RF/RNF, criterios de aceitacao, casos, execucoes, defeitos e retestes.

## Escopo

| Area | Inclui | Estado atual |
| --- | --- | --- |
| API baseline | Clientes, produtos, enderecos, pedidos e pagamentos simulados. | Codigo local observado. |
| Contrato REST | Exportacao OpenAPI do baseline e contrato alvo da evolucao. | Baseline e dois alvos versionados; auditoria estrutural executada, validador 2020-12 e HTTP pendentes. |
| Banco de dados | Persistencia PostgreSQL, integridade, transacoes, seed e consultas. | Pendente de modelo evolutivo. |
| Autenticacao/autorizacao | Gerente, vendedor, 401/403 e dados por perfil. | Alvo, nao implementado. |
| Metas e realizado | Meta da loja, distribuicao, realizado pago menos estornos e meta zero. | Alvo, nao implementado. |
| Estoque e custo | Reserva, saida, cancelamento, entrada por lote e custo historico. | Alvo, nao implementado. |
| Frontend | Login, sessao e logout em Next.js; Gestao, Meu desempenho, Vender e Estoque administrativo na evolucao. | Acesso com testes E2E; areas de dominio pendentes. |

Fora do escopo planejado: gateway real de pagamento, dados reais, hospedagem publica obrigatoria e certificacao de producao.

## Premissas e bloqueios

| Item | Situacao | Tratamento |
| --- | --- | --- |
| Maven Wrapper | Corrigido com `.mvn/wrapper/maven-wrapper.properties` apontando para Maven 3.9.11. | Usar `.\mvnw.cmd` nas execucoes formais. |
| Maven no PATH | `mvn` nao encontrado no ambiente atual. | Resolvido pelo wrapper; nao depender de Maven global. |
| JDK | Nao havia `java`/`javac` no PATH; JDK 25 portatil foi baixado para `tmp/tools/jdk-25`. | Definir `JAVA_HOME` para o JDK portatil ou instalar JDK 25 no sistema. |
| PostgreSQL | Container Docker `postgres-order` criado conforme README. | Para automacao futura, preferir Testcontainers ou Compose versionado. |
| Stack frontend | Ainda nao definida. | Plano E2E usa Playwright/Cypress como placeholder ate ARQ-01. |
| Autenticacao | Ainda nao implementada. | Casos de autorizacao ficam planejados ate API-01. |
| Estoque/custo | Ainda nao implementados. | Casos de concorrencia ficam planejados ate API-03/API-04. |

## Niveis de teste

| Nivel | Objetivo | Ferramenta sugerida | Evidencia |
| --- | --- | --- | --- |
| Unitario | Regras isoladas: calculo monetario, metas, estados e validacoes. | JUnit 5, Mockito quando necessario. | Relatorio Surefire e cobertura. |
| Integracao | Persistencia JPA, repositories, transacoes e rollback. | Spring Boot Test + Testcontainers PostgreSQL. | Logs, relatorios e seed usado. |
| API / contrato | Validar requests/responses HTTP e schema OpenAPI. | MockMvc/WebTestClient ou RestAssured; validador OpenAPI. | Requisicao/resposta sanitizada e relatorio. |
| Sistema | Fluxos completos API + banco: pedido, pagamento, estoque, metas. | Teste HTTP contra app subindo com banco descartavel. | Run id, ambiente, dados e resultados. |
| E2E interface | Fluxos de gerente/vendedor nas quatro telas. | A definir em ARQ-01. | Screenshots, video opcional e relatorio E2E. |
| Regressao | Garantir que correcoes nao quebrem baseline e regras criticas. | Suite selecionada em CI. | Comparacao antes/depois. |

## Tecnicas caixa-preta

| Tecnica | Uso no projeto |
| --- | --- |
| Particionamento de equivalencia | Tipos validos/invalidos de cliente, produto e pagamento; perfis gerente/vendedor; estados com e sem dados. |
| Analise de valor limite | Quantidade 0/1, meta zero, saldo 0/1, limite de desconto, valores monetarios minimos e datas de boleto. |
| Tabela de decisao | Autorizacao por perfil, soma das metas, alteracao de pedidos de terceiros e visibilidade de custo. |
| Transicao de estados | Pedido, pagamento, reserva, cancelamento, estorno e devolucao. |

O enunciado exige ao menos tres tecnicas caixa-preta; este plano usa quatro para cobrir melhor risco e rastreabilidade.

## Dados de teste

| Grupo | Dados propostos |
| --- | --- |
| Usuarios | Gerente: `gerente@loja.test`; vendedores: `ana@loja.test`, `bruno@loja.test`, `carla@loja.test`. Credenciais devem ser sinteticas. |
| Metas | Loja R$ 240.000; Ana/Bruno/Carla R$ 80.000 cada; periodo setembro/2026 America/Fortaleza. |
| Realizado demonstrativo | Ana R$ 68.400; Bruno R$ 62.000; Carla R$ 56.000; loja R$ 186.400. |
| Venda demonstrativa | Marina Costa; iPhone 128 GB Preto R$ 4.500; AirPods Branco R$ 1.200; total R$ 5.700. |
| Estoque | Produto com saldo 1 para concorrencia; produto sem saldo; produto com custo desconhecido; lote com custo definido. |
| Baseline | Cliente individual, cliente corporativo, produto fisico, produto digital, pagamento CARD/PIX/BOLETO. |

Dados reais nao devem ser usados em logs, prints, seed ou relatorios.

## Ambiente e versoes a registrar por execucao

Cada execucao formal deve registrar:

```text
run_id; data_hora; commit_api; commit_testes; contrato_versao;
hash_contrato; JDK; Maven; Spring Boot; PostgreSQL;
sistema_operacional; comando; perfil_ativo; seed;
testes_executados; aprovados; falhas; erros; ignorados;
relatorios; defeitos_associados; observacoes
```

Comandos alvo depois de corrigir o ambiente:

```powershell
.\mvnw.cmd test
.\mvnw.cmd verify
.\mvnw.cmd spring-boot:run
```

O wrapper foi restaurado e recebeu correção para `Target` nulo no PowerShell. Falhas de acesso ao JDK/Docker devem ser registradas como ambiente, sem aprovação nem falha de produto presumida.

### Separação da suíte implementada na continuação

`mvnw.cmd test` seleciona unitários sem banco. `mvnw.cmd -Pintegration verify` também inclui os testes `@Tag("integration")`, com perfil Spring `integration`, PostgreSQL exclusivo na porta 15432 e banco `order_management_test`. O runner `scripts/test.ps1 -Mode Integration` usa Compose descartável. `OrderFixtures` cria a massa sintética e os testes de persistência usam rollback; não há seed automático na base de desenvolvimento.

CT-MONEY-001 e a recarga de preço histórico de CT-MONEY-003 foram implementados e conferidos nas duas rodadas da suíte Java ampliada, com seis testes aprovados por rodada. Os seis casos HTTP da rodada Swagger passaram, incluindo schemas dos corpos 400/404 e documento OpenAPI ao vivo. Evidências: `reports/execucoes/run-2026-10-03-continuacao.md` e `reports/execucoes/run-2026-10-04-swagger-erros.md`. Isso não aprova os demais casos ainda não implementados nem autenticação/perfis.

## Mapa caso -> nivel -> tecnica -> risco

| Caso | Criterios | Nivel | Tecnica | Risco coberto |
| --- | --- | --- | --- | --- |
| CT-AUTH-001 | CA-01-01 | API | Tabela de decisao | Rota protegida exposta sem sessao. |
| CT-AUTH-002 | CA-01-02 | API | Tabela de decisao | Vendedor acessa dados de gerente. |
| CT-AUTH-003 | CA-01-03 | API/Sistema | Tabela de decisao | Gerente sem visao consolidada correta. |
| CT-CLIENT-001 | CA-03-01 | API | Particionamento | Cliente individual invalido aceito. |
| CT-CLIENT-002 | CA-03-02 | API | Particionamento | Cliente corporativo invalido aceito. |
| CT-PRODUCT-001 | CA-04-01 | API | Particionamento | Produto fisico sem peso aceito. |
| CT-PRODUCT-002 | CA-04-02 | API | Particionamento | Produto digital sem link aceito. |
| CT-ORDER-001 | CA-05-01 | Sistema | Particionamento | Pedido valido nao persiste fluxo minimo. |
| CT-ORDER-002 | CA-05-02 | Integracao/Sistema | Particionamento | Cliente novo nao e persistido com pedido. |
| CT-ORDER-003 | CA-05-03 | API | Particionamento | Pedido sem cliente retorna erro inconsistente. |
| CT-ORDER-004 | CA-06-01 | Sistema | Tabela de decisao | Vendedor historico incorreto. |
| CT-SEC-001 | CA-06-02 | API | Tabela de decisao | Vendedor falsificado no payload. |
| CT-MONEY-001 | CA-07-01 | Unitario/API | Analise de valor limite | Total demonstrativo diferente de R$ 5.700. |
| CT-MONEY-002 | CA-07-02 | Unitario/API | Analise de valor limite | Quantidade multiplicada duas vezes. Reproduzido, corrigido e retestado em 03/10/2026. |
| CT-MONEY-003 | CA-07-03 | Integracao | Transicao de estados | Alteracao de catalogo muda venda passada. |
| CT-PAY-001 | CA-08-01 | API/Sistema | Transicao de estados | Pedido nao muda para pago apos aprovacao. |
| CT-PAY-002 | CA-08-02 | Sistema | Transicao de estados | Evento repetido duplica realizado ou estoque. |
| CT-PAY-003 | CA-08-03 | Sistema | Transicao de estados | Estorno nao reduz realizado. |
| CT-GOAL-001 | CA-09-01 | Unitario/API | Analise de valor limite | Soma das metas diverge da loja. |
| CT-GOAL-002 | CA-09-02 | API | Tabela de decisao | Distribuicao invalida e aceita. |
| CT-GOAL-003 | CA-10-01 | Unitario/API | Analise de valor limite | Atingimento calculado errado. |
| CT-GOAL-004 | CA-10-02 | Unitario/API | Analise de valor limite | Meta zero causa divisao por zero. |
| CT-GOAL-005 | CA-10-03 | Sistema | Tabela de decisao | Pagamento pendente entra no realizado. |
| CT-PERF-001 | CA-11-01 | API/E2E | Tabela de decisao | Vendedor ve dados de colegas. |
| CT-SEC-002 | CA-11-02 | API | Tabela de decisao | Consulta por ID vaza outro vendedor. |
| CT-MGMT-001 | CA-12-01 | API/E2E | Particionamento | Gerente recebe consolidado incorreto. |
| CT-MGMT-002 | CA-12-02 | E2E | Particionamento | Periodo sem dados mostra numeros ficticios. |
| CT-STOCK-001 | CA-13-01 | Integracao/Sistema | Analise de valor limite | Duas reservas da ultima unidade. |
| CT-STOCK-002 | CA-13-02 | Sistema | Transicao de estados | Cancelamento nao libera reserva. |
| CT-STOCK-003 | CA-13-03 | Sistema | Transicao de estados | Pagamento nao converte reserva em saida. |
| CT-COST-001 | CA-14-01 | Integracao/API | Particionamento | Entrada de lote sem auditoria/custo. |
| CT-COST-002 | CA-14-02 | API | Tabela de decisao | Custo aparece no JSON do vendedor. |
| CT-COST-003 | CA-14-03 | Integracao | Transicao de estados | Custo historico e sobrescrito. |
| CT-E2E-001 | CA-15-01 | E2E | Particionamento | Fluxo de venda nao conclui pedido valido. |
| CT-E2E-002 | CA-15-02 | E2E/Sistema | Analise de valor limite | Erro de estoque duplica pedido ou perde formulario. |
| CT-UI-001 | CA-16-01 | E2E | Tabela de decisao | Menu de gerente incompleto. |
| CT-UI-002 | CA-16-02 | E2E | Tabela de decisao | Vendedor ve Estoque administrativo. |
| CT-UI-003 | CA-16-03 | E2E | Particionamento | Interface nao trata erro de API. |

## Casos da evolucao ERP/PDV (RF-17 a RF-23)

Planejados em 14/10/2026 (ETAPA 1). Nenhum foi executado. IDs a partir de 101 para nao colidir com os casos acima. A etapa indica onde o caso e implementado: 2A banco, 2B seguranca, 2C endpoints, 3 frontend, 4 suite final e CI.

| Caso | Criterios | Nivel | Tecnica | Etapa | Risco coberto |
| --- | --- | --- | --- | --- | --- |
| CT-SALE-101 | CA-19-02 | Unitario | Analise de valor limite | 2C | Desconto percentual ou por valor calculado errado; desconto maior que o item; arredondamento diferente de HALF_UP (equivalente Java do BD-26). |
| CT-SALE-102 | CA-19-03 | Unitario | Particionamento | 2C | Hash de idempotencia muda com a ordem das chaves do JSON. |
| CT-SALE-103 | CA-19-03 | Integracao | Transicao de estados | 2C | Repeticao cria segundo pedido ou segunda reserva; corrida com a mesma chave cria dois pedidos. |
| CT-SALE-104 | CA-22-04, CA-22-05 | Integracao | Particionamento | 2C | Produto inativo ou insumo vendido; preco alterado depois muda item antigo (BD-25). |
| CT-CRM-101 | CA-20-01, CA-20-03 | Unitario | Particionamento | 2C | Mascara de CPF/CNPJ errada; normalizacao perde digitos. |
| CT-CRM-102 | CA-20-01, CA-20-02 | Integracao | Particionamento | 2C | Digito verificador invalido aceito; documento duplicado aceito. |
| CT-CRM-103 | CA-21-01, CA-21-02 | Integracao | Tabela de decisao | 2C | Historico fora de ordem, sem paginacao ou com custo. |
| CT-CRM-104 | CA-20-04 | Integracao | Transicao de estados | 2C | Edicao concorrente sobrescreve dados; documento alterado pelo PUT. |
| CT-CRM-105 | CA-20-05 | API | Tabela de decisao | 2C | Cliente com pedido excluido; vendedor exclui cliente. |
| CT-AUTH-101 | CA-18-01 a CA-18-05 | Unitario | Analise de valor limite | 2B | Bloqueio na falha errada; sem desbloqueio; BCrypt pulado para conta bloqueada, inativa ou inexistente. |
| CT-AUTH-102 | CA-18-02 a CA-18-04 | API | Transicao de estados | 2B | Resposta revela conta bloqueada ou inativa; desbloqueio nao ocorre apos 15 min (Clock). |
| CT-CONTRACT-101 | CA-20-01, CA-19-02 | Unitario | Particionamento | 2C | Bean Validation aceita CPF/CNPJ invalido, entrega sem endereco ou cartao sem final. |
| CT-CONTRACT-102 | CA-23-01 | Sistema | Particionamento | 2C | OpenAPI ao vivo diverge do v4 nas operacoes `implemented`, inclusive erros. |
| CT-CONTRACT-103 | - | Build | - | 3 | Schemas Zod gerados divergem do contrato (`api:generate` com diff). |
| CT-SEC-101 | CA-17-01, CA-17-02 | API | Tabela de decisao | 2B | Linha da matriz com status diferente do esperado; corpo de erro fora do `ErrorResponse`. |
| CT-SEC-102 | CA-17-02 | API | Tabela de decisao | 2B | Endpoint mapeado sem linha na matriz (exposicao acidental). |
| CT-SEC-103 | CA-17-01 | API | Tabela de decisao | 2B | Gerente barrado em operacao do vendedor (URL ou metodo). |
| CT-SEC-104 | CA-17-02 | API | Particionamento | 2B | Mutacao sem CSRF ou com token de outra sessao aceita. |
| CT-SEC-105 | CA-18-03 | API | Transicao de estados | 2B | ID de sessao mantido apos login; sessao valida apos logout. |
| CT-SEC-110 | CA-19-02 | API | Particionamento | 2C | Vendedor injeta vendedor, preco, total ou desconto no corpo. |
| CT-SEC-111 | CA-19-02 | API | Particionamento | 2C | Autoria forjada por header ou parametro. |
| CT-SEC-112 | CA-19-07 | API | Tabela de decisao | 2C | Vendedor le, paga ou cancela pedido alheio; estoque alterado nessa tentativa. |
| CT-SEC-113 | CA-19-01, CA-20-03, CA-21-02 | API | Particionamento | 2C | Custo, margem, documento completo ou dados de colega no JSON do vendedor. |
| CT-SEC-114 | CA-17-02 | API | Tabela de decisao | 2C | Vendedor alcanca backoffice, legado ou exclusao de cliente. |
| CT-SEC-115 | CA-17-02 | API | Particionamento | 2B | Variacao de caminho ou override de metodo abre acesso; firewall responde 500. |
| CT-SEC-116 | CA-19-03 | API | Tabela de decisao | 2C | Mesma `Idempotency-Key` de outro vendedor devolve pedido alheio. |
| CT-STOCK-101 | CA-19-02 | API | Analise de valor limite | 2C | Quantidade 0, negativa, decimal ou acima de 999 aceita. |
| CT-STOCK-102 | CA-19-04 | Integracao | Analise de valor limite | 2C | Venda acima do disponivel aceita ou altera saldo (BD-28). |
| CT-STOCK-103 | CA-19-04 | Integracao | Transicao de estados | 2C | Reserva parcial permanece quando o 2o item falha. |
| CT-STOCK-104 | CA-19-06 | Integracao | Analise de valor limite | 2C | Duas vendas da ultima unidade (equivalente Java do CC-01). |
| CT-STOCK-105 | CA-19-06 | Integracao | Transicao de estados | 2C | Deadlock entre vendas, confirmacoes e cancelamentos com itens em ordem inversa. |
| CT-STOCK-106 | CA-19-05 | Integracao | Transicao de estados | 2C | Confirmacao repetida gera segunda SAIDA (BD-19). |
| CT-STOCK-107 | CA-19-07 | Integracao | Transicao de estados | 2C | Cancelamento nao libera reserva (BD-21); pedido pago cancelado. |
| CT-STOCK-108 | CA-22-03 | Integracao | Analise de valor limite | 2C | Ajuste de saida acima do disponivel; ajuste sem motivo; entrada altera custo medio. |
| CT-STOCK-109 | CA-22-03 | Integracao/BD | Analise de valor limite | 2A/2C | Escrita que deixaria saldo negativo passa no banco ou vira 500 na API. |
| CT-WEB-101 | CA-19-01 a CA-19-05 | E2E | Particionamento | 3 | Fluxo de venda com Pix nao conclui ou nao aparece em vendas e no historico. |
| CT-WEB-102 | CA-23-01, CA-23-03 | E2E | Transicao de estados | 3 | Baixa de estoque ou faturamento nao aparece para o gerente em ate 15 s. |
| CT-WEB-103 | CA-17-02 | E2E | Tabela de decisao | 3 | Vendedor abre Gestao ou ve o menu de Gestao. |
| CT-WEB-104 | CA-19-03 | E2E | Transicao de estados | 3 | Duplo clique ou recarga cria dois pedidos. |
| CT-WEB-105 | CA-22-02 | E2E | Transicao de estados | 4 | Entrada de estoque nao aumenta o disponivel no PDV. |
| CT-WEB-106 | CA-22-01 | E2E | Transicao de estados | 4 | Historico de preco sem autor ou motivo; venda antiga muda de preco. |
| CT-WEB-107 | CA-19-02 | E2E | Particionamento | 4 | Promocao liberada nao aparece no PDV ou nao entra na cotacao. |
| CT-WEB-108 | CA-19-07 | E2E | Tabela de decisao | 4 | Vendedor ve pedido de colega. |
| CT-WEB-109 | CA-23-03 | E2E/Acessibilidade | - | 4 | Violacao WCAG 2.2 AA (axe-core) ou rolagem horizontal em 320 px, nos dois perfis. |

Os cenarios BD-19, BD-21, BD-25, BD-26, BD-28 e CC-01 do pacote SQL continuam no job `bd` (psql) e ganham os equivalentes Java CT-STOCK-106, CT-STOCK-107, CT-SALE-104, CT-SALE-101, CT-STOCK-102 e CT-STOCK-104, para que as duas implementacoes nao divirjam.

## Selecao inicial da suite

| Prioridade | Casos | Motivo |
| --- | --- | --- |
| P0 | CT-MONEY-001, CT-MONEY-002, CT-ORDER-001, CT-PAY-001 | Validam o baseline e o risco do total do pedido. |
| P0 | CT-AUTH-001, CT-AUTH-002, CT-SEC-001, CT-SEC-002 | Autorizacao e vazamento de dados sao riscos altos. |
| P0 | CT-STOCK-001, CT-STOCK-002, CT-STOCK-003 | Estoque e concorrencia afetam integridade. |
| P1 | CT-GOAL-001 a CT-GOAL-005 | Metas sustentam Gestao e Meu desempenho. |
| P1 | CT-COST-001 a CT-COST-003 | Custos sao sensiveis e exigem historico. |
| P1 | CT-E2E-001, CT-E2E-002, CT-UI-001 a CT-UI-003 | Validam as quatro areas quando a interface existir. |
| P2 | CT-CLIENT-001 a CT-PRODUCT-002, CT-MGMT-001, CT-MGMT-002 | Complementam regressao e comportamento de catalogo. |

## Criterios de entrada e saida

Entrada para execucao formal:

- Ambiente Maven corrigido e versao registrada.
- Banco isolado ou Testcontainers configurado.
- Contrato baseline exportado ou bloqueio documentado.
- Seed sintetico versionado.
- Casos selecionados vinculados a RF/CA.

Saida para considerar rodada valida:

- Relatorio bruto preservado, mesmo com falhas.
- Falhas classificadas como produto, teste, contrato ou ambiente.
- Defeitos confirmados vinculados a caso e evidencia.
- Reteste registrado para defeitos corrigidos.
- Diferenca entre baseline e evolucao explicada.

## Evidencias

| Tipo | Local sugerido |
| --- | --- |
| Relatorios de teste | `reports/testes/<run_id>/` |
| Requisicoes/respostas sanitizadas | `reports/http/<run_id>/` |
| Cobertura | `reports/cobertura/<run_id>/` |
| Analise estatica | `reports/analise-estatica/<run_id>/` |
| E2E | `reports/e2e/<run_id>/` |
| Resumo da execucao | `reports/execucoes/<run_id>.md` |

Arquivos grandes podem ficar fora do Git se o pacote final guardar copia acessivel. O resumo versionado deve apontar para o artefato.

## Atualizacao 09/10/2026 (rascunho para revisao)

- A matriz de risco existe: [analise-risco.md](analise-risco.md). A prioridade da suite passa a seguir os riscos R01-R13: pagamento (R01) e total do pedido (R02) sao Criticos; status, autenticacao, dinheiro e migracao sao Altos.
- O catalogo com 43 casos e os 10 campos do enunciado esta em [catalogo-casos.md](catalogo-casos.md); ele reaproveita os IDs desta tabela (CT-MONEY, CT-ORDER, CT-PAY, CT-AUTH) e os IDs ja usados nos testes.
- O plano de qualidade (objetivos, atributos ISO/IEC 25010, criterios de entrada, saida e aprovacao) esta em [plano-qualidade.md](plano-qualidade.md); os criterios de entrada e saida desta pagina continuam validos e foram resumidos la.
- Defeitos com os 13 campos: [defeitos.md](defeitos.md). Templates: [templates/](templates/README.md).

## Proximas acoes

1. Criar seed minimo para cliente, produto, pedido e pagamento.
2. Expandir CT-MONEY-001 e CT-MONEY-003 para cobrir total demonstrativo e preservacao de preco historico.
3. Versionar configuracao de ambiente local, preferencialmente com Docker Compose.
4. Atualizar este plano apos a matriz de risco e a decisao de stack.
5. Separar testes de contexto que dependem de PostgreSQL de testes unitarios rapidos.
