# Template — caso de teste

Copiar o bloco abaixo para cada caso. Os 10 primeiros campos são exigidos pelo enunciado (seção 11). Os 3 últimos ligam o caso à estratégia (técnica, nível e risco).

```markdown
### CT-XXX-000 — título curto

| Campo | Valor |
| --- | --- |
| ID | CT-XXX-000 |
| Requisito relacionado | RF-00 / CA-00-00 |
| Objetivo | O que o caso prova, em uma frase. |
| Pré-condições | Estado do sistema e dados existentes antes do passo 1. |
| Dados de entrada | Valores exatos (massa sintética). Para valor-limite, listar cada fronteira. |
| Passos | 1. ... 2. ... |
| Resultado esperado | Comportamento observável (status HTTP, estado gravado, mensagem). |
| Resultado obtido | O que aconteceu na última execução, com a data. |
| Status | Passou / Falhou / Bloqueado / Não executado |
| Evidência | Teste automatizado (`Classe.metodo`) + run ou CI (link/commit). |
| Técnica | Particionamento / Valor-limite / Tabela de decisão / Transição de estados / Caminhos (caixa-branca) / Baseado em risco |
| Nível | Unitário / Integração / Sistema / Aceitação / Regressão |
| Risco coberto | ID da matriz em `docs/analise-risco.md` |
```

Regras:

- Caso que falhou aponta um defeito (`Dxxx`) no campo Resultado obtido.
- "Passou" só com evidência reproduzível (comando + commit). Execução manual sem registro não conta (seção 25 do enunciado).
- Valor-limite lista as fronteiras dos dois lados (ex.: 0,004 / 0,005 / 0,01), não valores do meio da faixa.