# Template — relatório de execução

Salvar como `reports/execucoes/run-AAAA-MM-DD-<assunto>.md`. Um relatório por rodada formal. Resultados que não puderem ser reproduzidos com o comando registrado não valem (seção 25 do enunciado).

```markdown
# Execução <assunto>

Data: DD/MM/AAAA. Branch/commit: `branch` @ `abc1234`.

## Identificação

| Campo | Valor |
| --- | --- |
| Run ID | run-AAAA-MM-DD-assunto |
| Casos / defeitos | CT-..., D... |
| Requisitos | RF-..., RNF-... |
| Tipo | Unitário / Integração / Contrato HTTP / E2E / Métricas |
| Ambiente | SO, JDK, Maven, PostgreSQL, Node, navegador |

## Comandos

(blocos de código exatamente como executados)

## Resultado

| Suíte | Testes | Falhas | Erros | Ignorados |
| --- | ---: | ---: | ---: | ---: |

Falhas classificadas como: produto / teste / contrato / ambiente.

## Evidências

Caminho dos relatórios brutos (`target/surefire-reports`, `reports/...`), artefato do CI ou captura sanitizada.

## Limites

O que não foi executado e por quê.
```