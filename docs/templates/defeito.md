# Template — defeito

Os 13 campos são exigidos pelo enunciado (seção 15). Severidade mede o **impacto** da falha. Prioridade mede a **urgência** da correção. As duas são classificadas separadamente.

```markdown
### D000 — título curto

| Campo | Valor |
| --- | --- |
| ID | D000 |
| Título | Frase que descreve a falha, não a correção. |
| Descrição | Contexto, regra violada e RF/CA afetado. |
| Passos para reprodução | 1. ... 2. ... (com dados sintéticos exatos) |
| Resultado esperado | Conforme requisito/critério. |
| Resultado obtido | O que o sistema fez (mensagem, status, valor gravado). |
| Severidade | Crítica / Alta / Média / Baixa |
| Prioridade | Alta / Média / Baixa |
| Evidência | Teste que reproduz (`Classe.metodo`), commit em que falhou, relatório. |
| Responsável | Correção: card e integrante. Reteste: integrante. |
| Status | Aberto / Em correção / Corrigido (aguardando merge) / Retestado / Encerrado |
| Correção | Arquivo(s), commit e PR. |
| Reteste | Commit, comando, data e resultado; regressão da suíte completa. |
```

## Escalas

| Severidade | Critério |
| --- | --- |
| Crítica | Perda ou corrupção de dinheiro/dados, ou falha de segurança explorável sem pré-condição. |
| Alta | Regra de negócio central violada (pagamento, status, total), com contorno difícil. |
| Média | Comportamento incorreto com contorno, ou erro de contrato (status/código errado). |
| Baixa | Mensagem, tipo de exceção, documentação ou configuração local. |

| Prioridade | Critério |
| --- | --- |
| Alta | Corrigir antes do próximo merge na `main` ou antes da demonstração. |
| Média | Corrigir na rodada de correção corrente (até a próxima medição Rn). |
| Baixa | Pode ficar para depois, com risco residual registrado. |

## Ciclo exigido

Defeito → Registro → Análise → Correção → Reteste → Regressão → Encerramento.

- **Encerrado** exige: correção mesclada na `main`, teste de regressão com `@Tag("regression")` passando e reteste registrado por quem não implementou a correção.
- Separar defeito de produto, erro do teste e funcionalidade não implementada (requisito "Alvo" não é defeito).