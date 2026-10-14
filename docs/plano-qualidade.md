# Plano de Qualidade de Software — Loja Gestão / Order Management API

Versão 0.1 (rascunho), 09/10/2026. Preparado por Matheus Siqueira para o card 05. **Responsável: Francisco. Revisor: Allan.** O documento só vale como aprovado depois da revisão nominal registrada no card.

Cobre os 11 itens mínimos da seção 4.2 do enunciado (`docs/referencias/Projeto-A3.pdf`) e usa a ISO/IEC 25010 como modelo de atributos. Complementa, sem repetir, o [plano de testes](plano-testes.md), os [critérios de aceitação](criterios-aceitacao.md) e a [análise de risco](analise-risco.md).

## 1. Objetivos de qualidade

O sistema sob teste (SUT) é a API REST de pedidos (Spring Boot 4.1, Java 25, PostgreSQL 18) com a interface de acesso em Next.js. A pergunta a responder é a do enunciado: *como demonstrar, por evidências, que o software atende aos requisitos com um nível aceitável de qualidade?*

| # | Objetivo | Indicador | Meta para a entrega (29/11) | Situação conhecida |
| --- | --- | --- | --- | --- |
| O1 | Nenhum defeito grave aberto | Defeitos Críticos/Altos com status diferente de Encerrado | 0 | 7 Críticos/Altos abertos (D003, D004, D006, D010, D012, D017, D018); todos com correção em PR ou branch, nenhum mesclado. Ver `docs/defeitos.md` |
| O2 | Contrato respeitado | Casos HTTP de contrato aprovados contra o OpenAPI 3.1 | 100% | 12/12 em 09/10 (FIX-03, `-HttpOnly`) |
| O3 | Regras críticas testadas por dentro | Cobertura de branches do pacote `services` | ≥ 60% (proposta; confirmar após R1) | 10,2% no R0 (06/10, suíte original) |
| O4 | Cobertura global de código | Cobertura de instruções (JaCoCo) | ≥ 70% (proposta; confirmar após R1) | 33,3% no R0 |
| O5 | Manutenibilidade não piora | Achados PMD + SpotBugs e blocos CPD | R2 ≤ R0 (44 / 46 / 9) | R1 pendente (QA-10) |
| O6 | Rastreabilidade | RF implementados com RF → CA → CT → execução → defeito | 100% dos RF com status Observado/Parcial | Matriz inicial em `criterios-aceitacao.md`; consolidação no DOC-01 |
| O7 | Reprodutibilidade | Resultados reproduzidos ao vivo com um comando | Todos os números do relatório final | Runners `scripts/test.ps1` e `scripts/test-contract.ps1`; demo depende do ENV-01 |

As metas O3 e O4 são propostas: o R0 mediu só a suíte original de 17 testes. Elas devem ser confirmadas ou ajustadas pela equipe depois da medição R1, com a justificativa registrada aqui.

## 2. Atributos de qualidade avaliados (ISO/IEC 25010)

| Atributo | Subcaracterística | Como é avaliado | Indicador | Ferramenta / evidência |
| --- | --- | --- | --- | --- |
| Adequação funcional | Correção, completude | Casos derivados dos critérios de aceitação | % de CA com caso aprovado | JUnit, `docs/catalogo-casos.md` |
| Confiabilidade | Maturidade, tolerância a falhas | Defeitos encontrados e taxa de falha da suíte; concorrência | Taxa de falha (falhas/testes executados); defeitos por severidade | Surefire, `ConcurrencyTest`, `docs/defeitos.md` |
| Segurança | Confidencialidade, integridade, autenticidade | 401/403 por perfil, CSRF, segredos fora do código, achados estáticos | Casos de sessão aprovados; vulnerabilidades/achados abertos | `http-session.mjs`, `SessionSecurityTest`, SpotBugs, `npm audit` |
| Manutenibilidade | Modularidade, analisabilidade, testabilidade | Complexidade, duplicação, violações de regra | CC média/máxima, métodos com CC > 10, blocos CPD, violações PMD | JaCoCo, PMD 7, CPD, SpotBugs 4.10, `scripts/quality-metrics.mjs` |
| Eficiência | Comportamento no tempo | p95 local sob protocolo definido (RNF-06) | p95 em ms por endpoint crítico | QA-05 (P2; pode ser cortado — ver plano restante, seção 9) |
| Usabilidade | Operabilidade, acessibilidade | Estados de erro/vazio, teclado, foco, viewport móvel | Tarefas E2E concluídas; erro acessível exibido | Playwright (`frontend/e2e/session.spec.mjs`) |
| Portabilidade | Adaptabilidade, instalabilidade | Mesma suíte em Windows (local) e Linux (CI) | Execuções aprovadas por ambiente | `scripts/*.ps1` + GitHub Actions |
| Compatibilidade | Interoperabilidade | Respostas conformes ao contrato OpenAPI 3.1 / JSON Schema 2020-12 | Pontos de schema sem achado | Ajv 8, `scripts/http-contract.mjs` |

## 3. Critérios de aceitação

Fonte única: [`criterios-aceitacao.md`](criterios-aceitacao.md), no formato Dado/Quando/Então, com IDs `CA-RR-NN` ligados a cada RF. Regras deste plano:

- Um requisito só é dado como atendido quando todos os seus CA têm caso de teste aprovado com evidência reproduzível.
- Requisito com status "Alvo" (não implementado) não é contado como aprovado nem como defeito.
- Mudança de texto de CA é registrada em [`validacao-requisitos.md`](validacao-requisitos.md); IDs não são reaproveitados.

## 4. Estratégia de testes

1. **Baseada em risco**: a intensidade de teste segue a [matriz de risco](analise-risco.md). Pagamento, total do pedido e status recebem tabelas de decisão, valor-limite, concorrência e testes de regressão; cadastros de baixo risco recebem particionamento simples.
2. **Contract-first**: o contrato OpenAPI aprovado em `config/openapi/` é o oráculo dos testes HTTP. Diferença entre a API ao vivo e o contrato é defeito ou mudança de contrato registrada, nunca ajuste silencioso.
3. **Teste antes da correção**: todo defeito ganha um teste que falha antes do fix (commit separado) e passa depois, marcado `@Tag("regression")`. Exemplo: FIX-03 (`reports/execucoes/run-2026-10-09-fix-03.md`).
4. **Caixa-preta**: particionamento de equivalência, análise de valor-limite, tabela de decisão e transição de estados (4 técnicas; o enunciado exige 3).
5. **Caixa-branca**: cobertura de instruções e branches (JaCoCo) e caminhos independentes de métodos com maior complexidade (`PaymentService.update`, `OrderService.buildPayment`, `PixPayment.processPayment`; card 12).
6. **Análise estática antes × depois**: PMD, CPD e SpotBugs na fase `verify`; R0 (06/10), R1 (após os merges dos FIX) e R2 (fim do domínio). ESLint no frontend (QA-12).
7. **Revisão técnica formal**: uma inspeção com ata (card 08, sugestão `PaymentService`), usando [`templates/ata-inspecao.md`](templates/ata-inspecao.md).

## 5. Níveis de teste

| Nível | O que verifica | Ferramenta | Onde está | Comando |
| --- | --- | --- | --- | --- |
| Unitário | Regras isoladas com repositórios simulados | JUnit 5, Mockito, AssertJ | `src/test/java/.../services`, `entities`, `dto` | `scripts/test.ps1` |
| Integração | JPA, constraints, transações, migrações Flyway, concorrência com banco real | Spring Boot Test, PostgreSQL 18 descartável | `OrderPersistenceTest`, `FlywayMigrationTest`, `ConcurrencyTest` (`@Tag("integration")`) | `scripts/test.ps1 -Mode Integration` |
| Sistema | API completa em execução, respostas HTTP contra o contrato e sessão | Node 24, Ajv 8 | `scripts/http-contract.mjs`, `scripts/http-session.mjs` | `scripts/test-contract.ps1 -HttpOnly` |
| Aceitação | Critérios Dado/Quando/Então pela interface e pela API | Playwright 1.63 | `frontend/e2e/session.spec.mjs` (login/logout); demais telas em QA-07 | `scripts/test-contract.ps1 -Frontend` |
| Regressão | Defeitos corrigidos não voltam | Tag `regression` + CI a cada push/PR | Testes `[Dxxx]` | CI GitHub Actions (`.github/workflows/ci.yml`) |

## 6. Ferramentas utilizadas

| Finalidade | Ferramenta (versão) |
| --- | --- |
| Build e testes Java | Maven Wrapper 3.9.11, JDK 25, JUnit 5, Mockito, AssertJ |
| Banco | PostgreSQL 18 (Docker Compose), Flyway |
| Contrato | OpenAPI 3.1, Springdoc 2.8.5, Ajv 8.20 + ajv-formats 3.0.1 |
| Interface | Next.js 16.3.8, React 19.3, Playwright 1.63 |
| Cobertura | JaCoCo 0.8.15 |
| Análise estática | PMD 7 (maven-pmd-plugin 3.28.0), CPD, SpotBugs 4.10 (plugin 4.10.4.1); ESLint (planejado, QA-12) |
| Métricas | `scripts/quality-metrics.mjs` (gera `metrics.json` e `resumo.md` a partir de `target/`) |
| CI | GitHub Actions |
| Gestão | Trello (quadro `ombHh0vo`), `docs/gestao/` |

## 7. Responsabilidades

| Integrante | Papel de qualidade | Cards principais |
| --- | --- | --- |
| Allan | Verificação: risco, catálogo, defeitos e reteste, inspeção, análise estática | 06, 08, 09, 10, 16 |
| Francisco | Documentação de qualidade, revisão e segurança/ética | 05, 19, 20, REV-01, DOC-01 |
| Gabriel | Automação, caixa-preta de contrato, caixa-branca e níveis | 11, 12, 13, 15 |
| Matheus | Métricas, regressão/melhoria contínua, banco e ambiente | 17, 18, BD-01/02, ENV-01 |

Regra transversal (Definição de Pronto): todo artefato entra na `main` por PR revisado por quem não é autor, e cada integrante explica na apresentação pelo menos dois testes ou defeitos.

## 8. Riscos

Riscos de produto: ver a [matriz probabilidade × impacto](analise-risco.md). Os críticos hoje são pagamento e total do pedido; estoque será crítico quando existir.

| Risco de projeto | Impacto na qualidade | Mitigação |
| --- | --- | --- |
| PRs sem revisão humana | Nenhum card cumpre a Definição de Pronto; defeitos passam | REV-02: revisão e merge na ordem #5 → #8 → #9 |
| Autoria concentrada em um integrante | Equipe não sabe explicar testes na demonstração | Revisão cruzada; DEMO-01 com 2 desafios por integrante |
| Ambiente de demonstração inexistente | Resultados não reproduzíveis ao vivo (seção 25) | ENV-01a até 01/11 e plano B gravado |
| Escopo do domínio maior que a capacidade | Artefatos da rubrica incompletos | Corte de escopo registrado em PL-01 |
| Segredos no histórico do Git | Exposição de credenciais | Senhas fora do código (PR #6); rotação (SEC-02) |

## 9. Critérios de entrada

Uma rodada formal de testes (R1, R2 ou a final) só começa quando:

1. O código está em um commit identificado na `main` (ou no PR em revisão, para reteste).
2. O CI desse commit terminou verde, ou as falhas estão classificadas.
3. O banco de teste é o descartável (`docker-compose.test.yml`), nunca o de desenvolvimento.
4. A massa é sintética e versionada (`OrderFixtures`, `TestData`).
5. Os casos selecionados estão no catálogo com RF/CA.

## 10. Critérios de saída

Uma rodada termina quando:

1. Todos os casos selecionados foram executados, e o relatório bruto (Surefire, JSON HTTP, Playwright) foi preservado.
2. Cada falha foi classificada como produto, teste, contrato ou ambiente.
3. Cada defeito de produto foi registrado com os 13 campos.
4. As métricas da rodada foram geradas com `scripts/quality-metrics.mjs` e interpretadas.
5. Existe um relatório `reports/execucoes/run-*.md` com comando, commit e data.

## 11. Critérios de aprovação e reprovação

| Objeto | Aprovado quando | Reprovado quando |
| --- | --- | --- |
| Caso de teste | Resultado obtido igual ao esperado, com evidência reproduzível | Diferença do esperado (gera defeito) ou ausência de evidência |
| Build / PR | CI verde (unitário, integração, contrato, E2E e validação Flyway) e revisão aprovada por não autor | Qualquer job falha, ou teste excluído sem ID de defeito |
| Correção de defeito | Teste falhou antes, passa depois, regressão completa verde e reteste por outro integrante | Teste passa antes da correção (não reproduziu) ou regressão quebra |
| Versão de entrega (`v1.0-a3`) | O1 = 0 e O2 = 100%; O3–O5 atingidos ou desvio justificado no relatório final; todos os números reproduzidos ao vivo | Defeito Crítico/Alto aberto, ou métrica sem origem reproduzível |

## Histórico

| Versão | Data | Autor | Mudança |
| --- | --- | --- | --- |
| 0.1 | 09/10/2026 | Matheus Siqueira | Rascunho inicial para revisão de Francisco e Allan. |