# Métricas R0 × R1

R0 (`r0-baseline/`) foi gerado em 06/10/2026, antes dos novos testes e correções. R1 (`r1/`) foi gerado em 14/10/2026 na branch `ci/full-pipeline`, depois das etapas 2A a 4. Os dois saíram de `scripts/quality-metrics.mjs` a partir do `target/` de `mvnw -Pintegration verify` (Surefire, JaCoCo, PMD, CPD e SpotBugs). Os números são da suíte Java. Os testes do frontend ficam fora: `npm test` com 20 testes de lógica e Playwright com 14 E2E.

| Indicador | R0 | R1 | Leitura |
| --- | ---: | ---: | --- |
| Testes executados | 17 | 544 | Suíte de integração completa (PDV, clientes, backoffice, gestão, segurança, matriz de acesso, Flyway). |
| Aprovados / reprovados | 17 / 0 | 544 / 0 | Nenhum teste vermelho herdado. |
| Instruções cobertas | 1385/4160 (33,3%) | 13320/14310 (93,1%) | O código cresceu 3,4 vezes; a cobertura subiu junto. |
| Branches cobertos | 22/221 (10%) | 598/743 (80,5%) | |
| Linhas cobertas | 349/1070 (32,6%) | 2168/2398 (90,4%) | |
| Complexidade ciclomática total / média / máxima | 566 / 1,26 / 18 | 1300 / 1,42 / 18 | A máxima continua a mesma: o método legado não mudou. |
| Métodos com complexidade > 10 | 1 | 2 | |
| Violações PMD | 44 | 105 | 44 são `ControlStatementBraces` (if de uma linha sem chaves, estilo usado nos módulos novos). |
| Duplicações CPD (blocos / linhas) | 9 / 241 | 18 / 253 | Linhas duplicadas quase estáveis, apesar de 3,4 vezes mais código. |
| Achados SpotBugs | 46 | 83 | 72 são `EI_EXPOSE_REP`/`EI_EXPOSE_REP2`: `record`s com listas e serviços injetados por construtor. |

## Observações

- O PMD e o SpotBugs rodam sem quebrar o build (`failOnViolation=false`, `failOnError=false`), como no R0.
- Nenhuma meta foi fixada. Uma proposta de meta (por exemplo, branches ≥ 80% ou SpotBugs sem achado fora de `EI_EXPOSE_REP*`) precisa ser decidida pela equipe em `docs/decisoes/`.
- Os relatórios brutos de cada rodada estão nas pastas `r0-baseline/` e `r1/`: `jacoco/`, `pmd.xml`, `cpd.xml`, `spotbugsXml.xml`, `surefire-reports/`, `metrics.json` e `resumo.md`.
