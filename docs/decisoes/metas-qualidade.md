# Metas de qualidade do backend

Estado: **Proposta, aguardando aprovação da equipe.** Data: 14/10/2026.

As metas partem dos números reais das rodadas R0 (`reports/metricas/r0-baseline/`, 06/10) e R1 (`reports/metricas/r1/`, 14/10), gerados por `scripts/quality-metrics.mjs` a partir de `mvnw -Pintegration verify`. Nenhuma regra de CI foi ativada: cada meta traz a regra que a aplicaria, para a equipe ligar depois do voto.

## Ponto de partida

| Indicador | R0 | R1 |
| --- | ---: | ---: |
| Testes (aprovados / total) | 17 / 17 | 544 / 544 |
| Cobertura de instruções | 33,3% (1385/4160) | 93,1% (13320/14310) |
| Cobertura de branches | 10,0% (22/221) | 80,5% (598/743) |
| Cobertura de linhas | 32,6% (349/1070) | 90,4% (2168/2398) |
| PMD (total; prioridade 1 / 2) | 44; 0 / 0 | 105; 1 / 1 |
| CPD (blocos / linhas duplicadas) | 9 / 241 | 18 / 253 |
| SpotBugs (total; prioridade alta; rank 1–9) | 46; 0; 0 | 83; 0; 0 |

Achados de prioridade alta do PMD no R1:
- prioridade 1, `ClassNamingConventions` em `db/demo/V6_1__DemoAccounts.java`: o nome segue a convenção de versão do Flyway e não pode mudar;
- prioridade 2, `GuardLogStatement` em `config/SecurityConfig.java`, linha 109.

## Metas propostas

| ID | Métrica | Meta | Justificativa | Regra de CI que a aplicaria (não ativada) |
| --- | --- | --- | --- | --- |
| MQ-01 | Cobertura de instruções | ≥ 93% | Piso = R1 arredondado para baixo; impede regressão sem exigir esforço novo. | `jacoco-maven-plugin`, goal `check` na fase `verify` do perfil `integration`, regra `BUNDLE` com `INSTRUCTION COVEREDRATIO minimum 0.93`. |
| MQ-02 | Cobertura de branches | ≥ 80% | Piso = R1 (80,5%) arredondado para baixo. É a métrica que mais cai quando entra regra de negócio sem teste de caminho alternativo. | Mesma execução, limite `BRANCH COVEREDRATIO minimum 0.80`. |
| MQ-03 | Cobertura de linhas | ≥ 90% | Piso = R1 (90,4%) arredondado para baixo. | Mesma execução, limite `LINE COVEREDRATIO minimum 0.90`. |
| MQ-04 | PMD | Nenhuma violação nova de prioridade 1 ou 2 | As duas existentes ficam registradas acima: uma é imposta pelo Flyway; a outra é de estilo de log. O restante (prioridades 3 e 4) é estilo, com 44 casos de `ControlStatementBraces`. | `maven-pmd-plugin` com `failurePriority=2` e `failOnViolation=true`, mais exclusão do arquivo `V6_1__DemoAccounts.java` no ruleset e correção ou supressão justificada do `GuardLogStatement`. |
| MQ-05 | CPD | Linhas duplicadas não aumentam (≤ 253) | Com 3,4 vezes mais código, as linhas duplicadas subiram só de 241 para 253. | Passo no job `test (backend)` que lê `target/cpd.xml`, soma `lines` das duplicações e falha acima de 253. |
| MQ-06 | SpotBugs | Nenhum achado de prioridade alta nem de rank 1 a 9 | R0 e R1 têm zero nos dois. Os 72 `EI_EXPOSE_REP*` do R1 vêm de `record`s com listas e de serviços injetados por construtor, padrão aceito no projeto. | `spotbugs-maven-plugin` com `threshold=High` e `failOnError=true` numa execução separada, mantendo a atual (`threshold=Low`) para o relatório. |
| MQ-07 | Testes | 0 reprovados, 0 `@Disabled`, 0 `known-defect` | Já vale na prática (DoD 6.5 do plano). | Já aplicada pelo Maven (falha de teste quebra o build); o `@Disabled` pode ser vigiado com `git grep -n "@Disabled" -- src/test` no job. |

## Como votar

Cada integrante marca Aprovo, Aprovo com ajuste (dizendo qual) ou Rejeito em cada linha. Uma meta aprovada vira regra de CI num PR próprio, com a rodada que comprova que a `main` já cumpre o limite. Até lá, os números continuam só no relatório.
