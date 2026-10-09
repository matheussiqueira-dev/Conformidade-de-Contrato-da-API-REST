# Plano do que falta — Projeto A3 (Loja Gestão / Order Management API)

Atualizado em 09/10/2026 por **Matheus Siqueira** (situação do GitHub conferida em 09/10). Quadro: https://trello.com/b/ombHh0vo

Este documento lista tudo o que ainda precisa ser feito até a entrega, **como** fazer, **com o que** fazer, **quem** faz e **quem revisa**. Ele complementa o [índice dos cards](README.md) e não substitui os briefings detalhados de cada card.

---

## 1. Situação verificada em 07/10/2026 (GitHub reconferido em 09/10/2026)

### 1.1 Repositórios GitHub

| Repositório | Estado em 09/10 | Conclusão |
| --- | --- | --- |
| `JkbSousa/order-management-api` (`upstream`, repositório original da equipe) | Último push em 01/09/2026 (`606f316`, "Update PaymentService.java"). Só existe `main`; nenhum PR. | **Sem atualizações** desde 01/09. Mantido apenas como referência histórica. |
| `matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST` (`origin`, **repositório oficial do projeto desde 08/10**) | Último push em 08/10 às 16:39 (horário de Brasília); **nenhum commit, PR, issue ou revisão nova desde então**. Na noite de 08/10 o histórico de `main` e `conformidade-main` foi reescrito (mensagens e autoria dos commits; o conteúdo dos arquivos não mudou). Hashes atuais: `main` = `3f10c37` (merge do PR #2), `conformidade-main` = `d8ccb66`, suíte nova = `7a4270c`. A CI passou em `main` e em `conformidade-main` depois da reescrita. PR #3 fechado. Branch `a3-contract-validation` (PR #1 e #4, já mesclados) ainda existe no remoto. | Todo o trabalho passa a ser concentrado neste repositório. Equipe avisada no card GIT-01 em 08/10 (clonar e seguir branch por card → PR → revisão). **Quem clonou antes da noite de 08/10 precisa clonar de novo** (ou `git fetch` + `git reset` para `origin/main`). |
| PR #5 — FIX-01 (`fix/FIX-01-pagamentos` → `main`) | Aberto em 08/10. Em 09/10 o GitHub o marcava como **em conflito**: a branch tinha sido criada sobre o histórico antigo (`1a46cbc`) e o PR mostrava 16 commits e 223 arquivos. **09/10: rebase sobre `3f10c37`** (`ded3e4d`) e commit `6a8c8e5` com os 3 apontamentos do cubic que eram do FIX-01. Agora o PR tem **2 commits, 7 arquivos, sem conflito**; `scripts/test.ps1`: 104 testes, 0 falhas. Versão anterior guardada na branch local `backup/FIX-01-pre-rebase-2026-10-09`. | Aguarda CI e **revisão de Allan**; depois merge e reteste no card 16. |
| PR #6 — card 19 (`chore/19-credenciais-env` → `main`) | Aberto em 09/10: senhas de banco saem do código (`TEST_DB_PASSWORD` no banco de teste/CI, gerada por execução; `SPRING_DATASOURCE_PASSWORD` no banco de desenvolvimento). `scripts/test.ps1`: 90 testes, 0 falhas. Modo Integration e contrato não rodaram localmente (Docker desligado). | Aguarda CI e revisão de Allan. **Depois do merge, cada integrante define `SPRING_DATASOURCE_PASSWORD` antes de subir o banco local.** |
| Cópia local | Alterações de senha publicadas no PR #6. Versão anterior à reescrita preservada na branch local `backup/pre-rewrite-2026-10-08`. | Versionar este plano (`docs/plano-restante`). |

### 1.2 Código e testes

- **Backend**: Java 25, Spring Boot 4.1.0, Spring Data JPA, PostgreSQL 18, Flyway (V1 legado + V2 usuários/autoria), Spring Security com sessão/CSRF e perfis Gerente/Vendedor, Springdoc 2.8.5.
- **Frontend**: Next.js 16.3.8 + React 19.3 com tela de acesso (login/sessão/logout) e 2 testes E2E Playwright 1.63. As quatro áreas de negócio (Gestão, Meu desempenho, Vender, Estoque) **não existem ainda**.
- **Contrato**: OpenAPI baseline (03/10), revisão de erros (04/10) e alvo v3 com sessão; auditoria Ajv 8 / JSON Schema 2020-12; 12 casos HTTP ao vivo.
- **Qualidade (R0, 06/10, suíte original de 17 testes)**: cobertura de instruções 33,3%, de branches 10%; PMD 44 violações; CPD 9 blocos/241 linhas; SpotBugs 46 achados. Relatório: `reports/metricas/r0-baseline/resumo.md`.
- **Nova suíte (07/10, versionada em 08/10)**: `scripts/test.ps1` executa **111 testes: 90 passam, 20 falham e 1 dá erro**. As falhas não são regressões: são testes novos que expõem **defeitos reais** do código legado (tabela na seção 3.2). Em 08/10 esses testes receberam `@Tag("known-defect")`, ficaram fora da execução padrão e a CI passou (commit `7a4270c` após a reescrita; era `f51c5a4`).
- **Ambiente normal**: ainda usa `ddl-auto=update` com Flyway desligado e não tem contas de acesso; hoje só é possível logar no runner de teste (`scripts/test-contract.ps1 -Preview`).

### 1.3 Calendário que limita o plano

| Evento (calendário UNIFG) | Período |
| --- | --- |
| A1 — 1ª oportunidade (semana de provas, baixa capacidade) | 19/10 a 24/10 |
| A1 — 2ª oportunidade | 09/11 a 14/11 |
| **A3 — entrega e apresentação** | **30/11 a 05/12** |
| A2 | 07/12 a 09/12 |

Prazo interno de entrega mantido: **29/11** (pacote pronto) para apresentar a partir de 30/11.

---

## 2. Mudanças feitas no Trello em 07/10

**Movidos de "A fazer" para "Em andamento"** (há entrega concreta no repositório local):

| Card | Motivo |
| --- | --- |
| [Allan] 09 · Análise estática | PMD/CPD/SpotBugs configurados e baseline R0 medido. |
| [Gabriel] 12 · Caixa-branca e cobertura (JaCoCo) | JaCoCo instrumentado, cobertura R0 medida. |
| [Matheus] 18 · Métricas | `scripts/quality-metrics.mjs` e R0 gravados. |

**Descrições atualizadas** (seção "Atualização 07/10/2026"): 09, 12, 18, 16 (lista de defeitos), QA-01 (nova suíte), 10 (IDs de caso já usados). Comentário no card 15 (CI) sobre os relatórios de qualidade e o risco de quebrar o pipeline.

**Cards novos** (mesmo padrão `[Integrante] CÓDIGO · Título`, briefing, dependências, aceite e checklist com responsável em cada item):

| Card | Lista | Responsável / revisor | Prazo |
| --- | --- | --- | --- |
| QA-09 · Publicar suíte R1 e triar os 21 testes que falham | Em andamento | Matheus / Gabriel | 10/10 |
| GIT-01 · Publicar commits locais e integrar ao repositório da equipe | A fazer | Gabriel (+ Matheus no push) / Allan | 11/10 |
| REV-01 · Rodada de revisões nominais dos cards "Em revisão" | A fazer | Francisco / Matheus | 14/10 |
| FIX-01 · Corrigir defeitos de pagamento | A fazer | Gabriel / Allan | 18/10 |
| FIX-02 · Corrigir exclusão inexistente, regressão de status e exceção de cliente | A fazer | Francisco / Allan | 18/10 |
| ENV-01 · Ambiente de demonstração (Flyway, contas, massa sintética) | Backlog | Matheus / Gabriel | 01/11 |
| CT-02 · Contrato v3 e testes para metas, estoque e vendas | Backlog | Allan / Gabriel | 08/11 |

Observações:
- Os cards novos ainda **não têm membros atribuídos**. Eles indicam o responsável no título e em cada item de checklist; **cada integrante deve se adicionar como membro** do próprio card.
- Nenhum card foi movido para "Concluído": não há revisão nominal registrada (regra do planejamento). Isso é o objetivo de REV-01.
- Os cards **BI-01 a BI-08** pertencem a outro projeto (Inteligência de vendas e rentabilidade, repositório próprio) e não foram alterados.

---

## 3. O que falta fazer — por frente de trabalho

Legenda de prioridade: **P0** bloqueia outros cards ou a nota; **P1** necessário para a entrega; **P2** desejável se houver tempo.

### 3.1 Frente A — Integração do código e da equipe (P0, esta semana)

| Card | O que fazer | Como fazer | Ferramentas | Resp. / Rev. |
| --- | --- | --- | --- | --- |
| **QA-09** | Versionar a nova suíte e explicar as 21 falhas. **08/10: suíte versionada em `f51c5a4` com `known-defect` e CI verde; defeitos registrados no card 16 e card em "Em revisão"; falta a revisão de Gabriel.** | 1) Classificar cada falha: defeito de produto, erro do teste ou regra ainda não aprovada (ex.: tabela de status depende de DOM-01). 2) Para defeito confirmado, manter o teste com `@Tag("known-defect")` + ID do defeito; o perfil de CI exclui só essa tag até o fix. 3) Commit rastreável e lista para o card 16. Nunca usar `@Disabled` sem ID. | JUnit 5 (`@Tag`, `@ParameterizedTest`), Mockito, AssertJ, Surefire (`excludedGroups`), `scripts/test.ps1` | Matheus / Gabriel |
| **GIT-01** | Uma só linha de código revisada. **08/10: PR #2 mesclado com CI verde, PR #3 fechado, fork adotado como repositório oficial e equipe avisada no card; falta a revisão de Allan e atualizar os links dos cards que ainda apontam para `a3-contract-validation`.** | 1) Push de `conformidade-main` com CI verde. 2) Revisar e mesclar PR #2; PR #3 fechado em 08/10 (o locator já havia sido corrigido em `fa321ae`). 3) Decisão de 08/10: o fork `matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST` é o repositório oficial; cada integrante clona esse repositório. 4) Fluxo daqui em diante: branch por card → PR → revisão de outro integrante → merge. | Git, GitHub PRs, GitHub Actions (`ci.yml`) | Gabriel (+Matheus) / Allan |
| **REV-01** | Fechar revisões pendentes de 15 cards. | Tabela `docs/gestao/revisoes.md` (card, artefato, revisor, data, resultado, link). Duas sessões de 1 h (sugestão 09/10 e 13/10). Checklist do revisor: artefato está na main, links funcionam, aceite atendido, evidência sanitizada. Aprovado → Concluído com comentário; ajustes → volta para Em andamento. | Trello, Markdown | Francisco / Matheus |

Cards em "Em revisão" que dependem de REV-01: 01 Setup, 02 Requisitos, 03 Validação, 04 Critérios, 07 Plano de testes, 15 CI, PL-01, DOM-01, ARQ-01, API-01, QA-04, Spike de contract testing, Extrair contrato OpenAPI, Ambiente reprodutível + seed, README.

### 3.2 Frente B — Correção de defeitos encontrados (P0, até 18/10)

Defeitos revelados pela execução de 07/10 (registrados no card 16 em 08/10). **08/10: FIX-01 implementado no PR #5** (D012, D003, D006, D004 e D016; os testes passaram para `regression`). **09/10: PR rebaseado sobre `main` (`3f10c37`) e sem conflito; falta revisão de Allan e reteste no card 16.** Dos 24 apontamentos do cubic, os 3 que tocavam o FIX-01 foram corrigidos em `6a8c8e5`: `PaymentRules.cents` rejeita `Double` infinito/NaN com `PaymentException` (400), `PaymentService.insert` passou a ser `@Transactional` (lê `Order.total()` com itens LAZY) e o teste de vencimento do boleto usa data fixa (`BoletoPayment.processPayment(LocalDate)`). Os demais apontam arquivos que já estão na `main` (cards, relatórios R0 com caminhos locais da máquina, `openapi-runtime-errors.json`, `quality-metrics.mjs`) e devem virar tarefas dos cards correspondentes, não do FIX-01. Os dois defeitos antes sem ID são D004 (reprocessamento) e D008 (cliente sem CPF/CNPJ).

| ID | Defeito | Severidade | Onde corrigir | Card |
| --- | --- | --- | --- | --- |
| D012 | Pagamento com valor diferente do total do pedido é aceito (POST /orders e POST /payment). | Crítica | `OrderService`, `PaymentService` | FIX-01 |
| D003 | Boleto vencido marca o pedido como PAID. | Alta | `BoletoPayment.processPayment`, `PaymentService` | FIX-01 |
| D006 | Boleto sem vencimento é aceito; processamento lança NullPointerException. | Alta | `PaymentService.buildPayment`, `BoletoPayment` | FIX-01 |
| — | Pagamento não PENDING pode ser reprocessado (NPE em `processAndSave`). | Alta | `PaymentService.processAndSave` | FIX-01 |
| D016 | Tipo de pagamento depende do locale (`toUpperCase()` sem `Locale`). | Média | `PaymentService`, `OrderService` (usar `Locale.ROOT`) | FIX-01 |
| D010 | Status do pedido regride (DELIVERED→PENDING_PAYMENT, SHIPPED→PAID, PAID→PENDING_PAYMENT). | Alta | `OrderService.updateStatus` (tabela de transições) | FIX-02 |
| D011 | DELETE de produto/cliente/endereço/pedido/pagamento inexistente não retorna 404. | Média | 5 serviços: `existsById` antes de `deleteById` | FIX-02 |
| — | Cliente sem CPF/CNPJ lança `ProductException` (tipo de exceção errado). | Baixa | `ClientService.buildClient` | FIX-02 |

**Como**: corrigir sempre na camada de serviço, antes de salvar; manter o teste que expôs o defeito como regressão (`@Tag("regression")`), remover `known-defect`, executar `scripts/test.ps1` e `scripts/test-contract.ps1`; se o status HTTP mudar, atualizar o contrato de erros. **Allan** registra em `docs/defeitos.md` (ou GitHub Issues): ID, RF/CA, severidade, passos, esperado × obtido, commit, correção, reteste.

**Ferramentas**: Java/Spring, JUnit 5, Mockito, AssertJ, Ajv (contrato), Node `scripts/http-contract.mjs`.

### 3.3 Frente C — Domínio da loja: API e banco (P1, 12/10 a 08/11)

Ordem recomendada (caminho crítico): **DOM-02 → BD-02/SEC-01 → API-03 → API-06 → API-02 → API-04 → API-05 → ENV-01**.

| Card | O que fazer | Como fazer | Ferramentas | Resp. / Rev. |
| --- | --- | --- | --- | --- |
| **DOM-02** BigDecimal/NUMERIC | Trocar `Double` por `BigDecimal` em entidades e DTOs de dinheiro (Order, OrderItem, Product, Payment e subclasses). | Migração Flyway `V3__money_numeric.sql` com `ALTER COLUMN ... TYPE NUMERIC(12,2)`; escala 2 e `RoundingMode.HALF_UP` (decisão DOM-01); comparar com `compareTo`, nunca `equals`; serializar como número no JSON; atualizar contrato. Rodar QA-04/CT-MONEY-002 como regressão. | JPA `@Column(precision=12, scale=2)`, Flyway, Jackson, JUnit | Gabriel / Allan |
| **BD-02** Flyway e integridade | Adotar Flyway também no perfil normal; política para bancos antigos; FKs, CHECKs (quantidade > 0, valores ≥ 0), índices. | V3+ incrementais; `baseline-on-migrate` só com cópia de segurança; testar upgrade e rollback em cópia sintética; `FlywayMigrationTest` com Hibernate `validate`. | Flyway, PostgreSQL 18, Docker Compose, Testcontainers/compose de teste | Matheus / Gabriel |
| **BD-01** DER e dicionário | Completar DER e dicionário com tabelas novas (metas, estoque, lotes, movimentos) e análise de normalização (1FN–3FN). | Atualizar `docs/bd/dicionario-dados.md` e `docs/bd/migracoes.md`; gerar DER a partir do esquema real. | dbdiagram/DBeaver/Mermaid ER | Matheus / Allan |
| **SEC-01** Autoria do pedido | Terminar: consulta "minhas vendas" por vendedor; teste HTTP com dois vendedores e gerente; contrato atualizado. | `OrderRepository.findBySellerId`; filtro por `AppPrincipal` no serviço; 403 para ID de colega. | Spring Security, JPA, Node HTTP tests | Matheus / Gabriel |
| **API-03** Estoque atômico | Reserva ao criar pedido, saída no pagamento confirmado, liberação no cancelamento. Disponível = físico − reservado. | Tabelas `stock_balance` e `stock_movement`; `@Transactional` + bloqueio pessimista (`SELECT ... FOR UPDATE`) ou `@Version`; testes de concorrência com dois pedidos para a última unidade. | Spring `@Transactional`, JPA `@Lock`, PostgreSQL, JUnit + `ExecutorService` | Gabriel / Allan |
| **API-06** Pagamento consistente | Confirmar pagamento, estorno e efeitos (estoque, realizado) sem estado parcial; idempotência. | Máquina de estados de pagamento; chave de idempotência por pedido; estorno gera movimento inverso; testes de rollback real (QA-02). | Spring, JPA, Flyway, JUnit | Gabriel / Allan |
| **API-02** Metas | Meta mensal da loja, distribuição por vendedor (soma = meta da loja), realizado = pagos − estornos; meta zero exibe "Sem meta". | Tabelas `monthly_goal` e `seller_goal`; consulta agregada por mês/vendedor no fuso America/Fortaleza; vendedor só vê a própria meta. | Spring, JPA/consulta nativa, `java.time` | Matheus / Gabriel |
| **API-04** Entradas e custo | Gerente registra entradas/lotes com quantidade, custo e preço; custo histórico preservado na venda (custo médio, decisão DOM-01). | Tabela `stock_entry`; custo médio ponderado recalculado na entrada; custo copiado para o item do pedido; DTO de vendedor **sem** custo/margem. | Spring, JPA, BigDecimal | Matheus / Gabriel |
| **API-05** Histórico do cliente (P2) | Histórico de compras e atendimento compartilhado entre vendedores. | Consulta por cliente com paginação; respeitar perfil. | Spring Data `Pageable` | Gabriel / Allan |
| **BD-03** SQL, views e índices | Consultas de relatório (vendas por vendedor/mês, estoque abaixo do mínimo), views e análise `EXPLAIN ANALYZE`. | Views em migração Flyway; comparar plano antes/depois do índice. | PostgreSQL, `EXPLAIN (ANALYZE, BUFFERS)` | Allan / Matheus |
| **ENV-01** Ambiente de demo | Perfil `demo` com Flyway + validate, contas e massa sintética, `scripts/demo.ps1`. | Seed por perfil (gerente, Ana/Bruno/Carla, catálogo, metas de setembro); senhas via variável de ambiente. | Spring profiles, Flyway, Docker Compose, PowerShell | Matheus / Gabriel |

**Corte de escopo se faltar tempo** (decisão a registrar em PL-01): manter API-03, API-06 e API-02 (sustentam Vender, Meu desempenho e Gestão); reduzir API-04 a entrada simples sem lote; adiar API-05.

### 3.4 Frente D — Interface (P1, 19/10 a 22/11)

| Card | O que fazer | Como fazer | Ferramentas | Resp. / Rev. |
| --- | --- | --- | --- | --- |
| **UX-01** | Fechar fonte visual, estados (vazio, carregando, erro, sucesso) e navegação das quatro áreas. | Derivar de `direcao-criativa.md` e do Guia Visual; tokens de cor/tipografia; protótipo das 4 telas. | Figma (opcional), Markdown | Francisco / Matheus |
| **UI-01** | Base visual e navegação por perfil (já iniciada). | Layout comum, menu por perfil vindo da sessão (`/auth/me`), rotas protegidas; o servidor continua autorizando. | Next.js App Router, React 19, CSS | Matheus / Francisco |
| **UI-04** Vender | Cliente → Produtos → Pagamento. | Formulário em etapas; total sempre do servidor; só produtos disponíveis (API-03). | Next.js, fetch com CSRF (`lib/auth-client.mjs`) | Matheus / Allan |
| **UI-03** Meu desempenho | Meta, realizado e vendas do próprio vendedor. | Consumir API-02/SEC-01; barra de atingimento; "Sem meta" quando zero. | Next.js | Matheus / Allan |
| **UI-02** Gestão | Loja, equipe, metas e alerta de estoque. | Consumir API-02/API-03; tabela por vendedor; alertas de estoque mínimo. | Next.js | Matheus / Allan |
| **UI-05** Estoque (P2) | Saldo, reserva, custo e preço (só gerente). | Consumir API-03/API-04. | Next.js | Matheus / Allan |
| **QA-07** | Aceitação das quatro telas e regressão por perfil. | Cenários Dado/Quando/Então por perfil; Playwright com contas sintéticas; teclado, foco, contraste e 200% de zoom. | Playwright, axe-core (`@axe-core/playwright`, se aprovado) | Allan / Gabriel |

### 3.5 Frente E — Testes e contrato (P1, contínuo até 15/11)

| Card | O que fazer | Como fazer | Ferramentas | Resp. / Rev. |
| --- | --- | --- | --- | --- |
| **QA-01** | Unitários das regras (nova suíte já escrita). | Revisar a suíte de QA-09; acrescentar regras de BigDecimal, estoque e pagamento à medida que as APIs saírem. | JUnit 5, Mockito, AssertJ | Gabriel / Allan |
| **QA-02** | Integração com banco real: constraints e rollback. | Testes `@Tag("integration")` contra PostgreSQL descartável (porta 15432); provar rollback de reserva/pagamento. | `scripts/test.ps1 -Mode Integration`, Docker | Matheus / Gabriel |
| **QA-03** | Testes de sistema HTTP e aceitação. | Ampliar `http-contract.mjs`/`http-session.mjs` com fluxos completos (criar pedido → pagar → consultar). | Node 24, Ajv | Allan / Gabriel |
| **11** Caixa-preta de contrato | Consolidar técnicas (partição, valor-limite, tabela de decisão) nos testes de contrato. | Relacionar cada caso HTTP a um CT do catálogo. | Ajv, Node test runner | Gabriel / Allan |
| **13** Níveis de teste | Mostrar unitário, integração, sistema, aceitação e regressão com exemplos reais. | Tabela nível → arquivo → comando → resultado. | Surefire/Failsafe, Playwright | Gabriel / Allan |
| **CT-02** | Contrato das rotas novas e testes por perfil. | Contract-first; 1 positivo + 2 negativos por rota; vendedor → recurso de colega = 403. | OpenAPI 3.1, Ajv, Springdoc | Allan / Gabriel |
| **10** Catálogo ≥30 | `docs/catalogo-casos.md`. | ID, RF/CA, técnica, pré-condição, massa, passos, esperado, nível, automação, resultado. Reusar os ~35 IDs já citados nos testes. | Markdown (template do card de Templates) | Allan / Gabriel |
| **12** Cobertura | R0 × R1 e análise de caminhos de 3 métodos. | Grafo de fluxo, complexidade ciclomática e caminhos independentes de `PaymentService.update`, `OrderService.buildPayment`, `PixPayment.processPayment`. | JaCoCo 0.8.15 | Gabriel / Matheus |
| **QA-05** Desempenho (P2) | p95 local sob protocolo definido. | Seed estável; 3 endpoints críticos; aquecimento + N requisições; registrar máquina e versões. | k6 ou autocannon | Allan / Matheus |
| **QA-06** Schemathesis (P2) | Testes gerados a partir do contrato. | Rodar contra a API de teste; triar achados como defeito ou ruído. | Schemathesis (Python, venv próprio) | Gabriel / Allan |

### 3.6 Frente F — Qualidade, documentação e evidências (P1)

| Card | O que fazer | Como fazer | Ferramentas | Resp. / Rev. | Prazo |
| --- | --- | --- | --- | --- | --- |
| **Templates** | Modelos de caso de teste, defeito e relatório. | 3 arquivos em `docs/templates/`. Destrava 10, 16 e 20. | Markdown | Matheus / Francisco | 11/10 |
| **05** Plano de Qualidade | Cobrir os 11 itens de qualidade do enunciado. | Objetivos, atributos (ISO 25010), papéis, padrões, revisões, métricas, ferramentas, riscos. | Markdown | Francisco / Allan | 11/10 |
| **06** Análise de risco | Probabilidade × impacto por funcionalidade. | Matriz; priorizar testes pelos riscos altos (pagamento, estoque, autorização). | Planilha/Markdown | Allan / Francisco | 11/10 |
| **08** Inspeção formal | Revisão técnica formal de 1 módulo. | Papéis (moderador, autor, leitor, registrador), checklist, ata e defeitos encontrados (sugestão: `PaymentService`). | Markdown | Allan / Matheus | 18/10 |
| **09** Estática | Triagem R0 e comparação R1. | Classificar achados; priorizar DM_CONVERT_CASE, NP_NULL; ESLint para o frontend. | PMD 3.28 (plugin), SpotBugs 4.10, ESLint | Allan / Matheus | 20/11 |
| **16** Defeitos | Registro completo e reteste. | Ver seção 3.2. | Markdown ou GitHub Issues | Allan / Matheus | 15/11 |
| **17** Regressão | `docs/melhoria-continua.md` com antes/depois. | Mesmo caso antes e depois do fix, com commits. | Git, relatórios | Matheus / Gabriel | 15/11 |
| **18** Métricas | R0, R1 (após fixes) e R2 (fim do domínio). | `node scripts/quality-metrics.mjs` após `mvnw verify`; acrescentar densidade de defeitos, taxa de reteste, rastreabilidade. | Node, JaCoCo, PMD, SpotBugs | Matheus / Gabriel | 22/11 |
| **19** Segurança e ética | Ameaças, privacidade, uso de IA. | Revisar sessão/CSRF/cookies; mover senha de desenvolvimento do `application.properties` para variável de ambiente; massa sintética; nenhuma credencial em logs/capturas. | OWASP Top 10, `npm audit`, OWASP Dependency-Check (opcional) | Francisco / Allan | 20/11 |
| **DOC-01** Rubrica e rastreabilidade | Matriz dos 14 critérios + RF → CA → CT → execução → defeito. | `docs/matriz-rubrica.md` e `docs/rastreabilidade.md`. | Markdown | Francisco / Matheus | 25/10 |

### 3.7 Frente G — Entrega final (P1, 16/11 a 29/11)

| Card | O que fazer | Como fazer | Resp. / Rev. | Prazo |
| --- | --- | --- | --- | --- |
| **20** Relatório técnico final | Consolidar os 10 entregáveis. | Seguir os templates; cada número com origem, comando e data; separar o que foi executado do que ficou planejado. | Francisco / Matheus | 26/11 |
| **README + 1 comando** | Execução reproduzível. | Atualizar com `scripts/demo.ps1` (ENV-01) e pré-requisitos. | Gabriel / Allan | 24/11 |
| **Slides** | Apresentação de 18 min. | Roteiro do manual: problema → arquitetura → teste ao vivo → defeito → BD → métricas → riscos. | Francisco / Gabriel | 26/11 |
| **Plano B da demo** | Gravação de contingência. | Gravar a demo completa com o ambiente ENV-01; guardar localmente e no drive da equipe. | Francisco / Allan | 26/11 |
| **21** Apresentação | Ensaios. | 2 ensaios cronometrados (sugestão 22/11 e 27/11); todos explicam um teste. | Gabriel / todos | 28/11 |
| **Checklist final** | Conferir 14 critérios e pacote. | Usar DOC-01; tag Git da versão entregue; comprovante de envio. | Matheus / Francisco | 29/11 |

---

## 4. Cronograma semanal sugerido

| Semana | Foco | Saída esperada |
| --- | --- | --- |
| 07/10–11/10 | QA-09, GIT-01, Templates, 05, 06, início REV-01 | Código publicado, CI verde, revisões iniciadas, plano de qualidade e riscos. |
| 12/10–18/10 | FIX-01, FIX-02, DOM-02, 08 inspeção, 10 catálogo, REV-01 | Defeitos críticos corrigidos e retestados; dinheiro em BigDecimal; catálogo ≥30. **R1 de métricas.** |
| 19/10–25/10 | **Semana de A1 — capacidade reduzida.** BD-02/SEC-01, DOC-01, UX-01 | Flyway no perfil normal; autoria completa; matriz da rubrica. |
| 26/10–01/11 | API-03, API-06, QA-02, ENV-01, BD-01 | Estoque e pagamento consistentes com testes de transação; ambiente de demo. |
| 02/11–08/11 | API-02, API-04, CT-02, QA-03, UI-04 | Metas e entradas; contrato das rotas novas; tela Vender. |
| 09/11–15/11 | **A1 2ª oportunidade.** UI-03, UI-02, 16, 17, 12 | Telas Meu desempenho e Gestão; defeitos e regressão consolidados. |
| 16/11–22/11 | QA-07, UI-05 (se houver tempo), 09, 18, 19, QA-05 | Aceitação por perfil; **R2 de métricas**; segurança; desempenho. |
| 23/11–29/11 | 20, README, slides, plano B, 21, checklist final | Pacote completo, ensaio, gravação, tag de entrega. |
| 30/11–05/12 | **Apresentação A3** | — |

## 5. Carga por integrante (cards abertos do A3)

| Integrante | Frente principal | Cards abertos (resumo) |
| --- | --- | --- |
| **Matheus** | BD, metas/custo, interface, métricas | QA-09, ENV-01, BD-01, BD-02, SEC-01, QA-02, API-02, API-04, UI-01…05, 17, 18, Templates, Checklist final |
| **Gabriel** | Integração, dinheiro, estoque, pagamento, testes | GIT-01, FIX-01, DOM-02, API-03, API-05, API-06, QA-01, 11, 12, 13, QA-06, README, 21 |
| **Allan** | Qualidade e verificação | 06, 08, 09, 10, 16, CT-02, BD-03, QA-03, QA-05, QA-07, QA-08 |
| **Francisco** | Documentação, revisão, UX e um fix técnico | REV-01, FIX-02, 05, 19, 20, DOC-01, UX-01, Slides, Plano B |

Matheus concentra a interface (UI-01 a UI-05). Se o ritmo de 26/10 mostrar atraso, **UI-05 passa para Francisco com apoio de Matheus** e UI-02 pode ser simplificada (tabela sem gráficos).

## 6. Ferramentas e stack consolidada

| Camada | Ferramenta (versão) | Uso |
| --- | --- | --- |
| Backend | Java 25, Spring Boot 4.1.0, Spring Data JPA, Spring Security | API, regras, sessão/CSRF, perfis |
| Banco | PostgreSQL 18 (Docker), Flyway | Esquema versionado, integridade, transações |
| Contrato | OpenAPI 3.1, Springdoc 2.8.5, Ajv 8.20 + ajv-formats 3.0.1 | Contrato aprovado e validação ao vivo |
| Frontend | Next.js 16.3.8, React 19.3.0 | Quatro áreas por perfil |
| Testes | JUnit 5, Mockito, AssertJ, Node test runner, Playwright 1.63 | Unitário, integração, sistema, E2E |
| Qualidade | JaCoCo 0.8.15, PMD (plugin 3.28), CPD, SpotBugs (4.10), `scripts/quality-metrics.mjs` | Cobertura, estática, métricas R0/R1/R2 |
| CI | GitHub Actions (`ci.yml`), artefatos surefire | Execução a cada push/PR |
| Gestão | Trello, `docs/gestao/` | Cards, briefings, revisões |
| Opcionais (P2) | k6/autocannon, Schemathesis, axe-core | Desempenho, contrato gerado, acessibilidade |

Comandos principais (na pasta `order-management-api`):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test.ps1 -Mode Integration
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-contract.ps1 -Frontend
node scripts/quality-metrics.mjs
```

## 7. Riscos e decisões pendentes

| Risco | Impacto | Mitigação |
| --- | --- | --- |
| Trabalho só na máquina local (10 commits + testes não versionados). | Perda de trabalho; equipe revisando versão velha. | **Resolvido em 08/10**: commits e suíte publicados no repositório oficial. **09/10**: restam locais a remoção de senhas (12 arquivos) e este plano. |
| Histórico reescrito em 08/10 (hashes mudaram). | Clones antigos e branches criadas antes divergem da `main` (caso do PR #5); links de commit antigos nos relatórios deixam de bater. | Todos reclonam; rebase das branches abertas; citar os hashes novos (`3f10c37`, `d8ccb66`, `7a4270c`). |
| CI quebrar ao publicar a suíte com 21 falhas. | Bloqueio de todos os PRs. | Tag `known-defect` excluída no CI só até os FIX. CI verde em 08/10 (`f51c5a4`). |
| Carga concentrada em Matheus (interface + BD + métricas). | Telas incompletas na entrega. | Corte de escopo da seção 3.3; UI-05 para Francisco se necessário. |
| Semanas de A1 (19–24/10 e 09–14/11). | Atraso no domínio. | Tarefas pequenas nessas semanas; nada crítico com prazo nelas. |
| Ambiente normal sem Flyway/contas. | Demo ao vivo falhar. | ENV-01 + Plano B gravado. |
| Download npm instável (ECONNRESET em 05/10). | Build do frontend local falhar. | Usar o CI como evidência; cache npm do projeto; repetir em outra rede. |
| Confirmações docentes (grupo de 4, rubrica de BD, formato da entrega). | Escopo final pode mudar. | Registrar resposta formal em DOC-01; manter BD-01/02/03 no plano. |

## 8. Definição de pronto (vale para todos os cards)

1. Artefato na `main` (via PR revisado por outro integrante).
2. Testes pertinentes executados; comando, data, commit e resultado registrados.
3. Evidência sanitizada (sem senhas, tokens ou dados pessoais).
4. Revisor nominal aprovou no card (comentário com data).
5. Briefing do card e este plano atualizados quando o escopo mudar.
