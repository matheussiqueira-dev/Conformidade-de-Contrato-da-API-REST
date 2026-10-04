# Decisao tecnica - contrato da API

Data: 03/10/2026.

## Baseline preservado

O contrato observado do backend atual foi exportado em:

```text
config/openapi/baseline-openapi-2026-10-03.json
```

Hash SHA-256:

```text
4CF78EA43C7A24490F5B53F4E4BB11FCBBC4689EFDC1DB57BB9FA5EB9FE8F180
```

Rotas presentes no baseline exportado:

| Grupo | Rotas |
| --- | --- |
| Clientes | `/clients`, `/clients/{id}`, `/clients/{id}/address` |
| Produtos | `/products`, `/products/{id}` |
| Enderecos | `/address`, `/address/{id}` |
| Pedidos | `/orders`, `/orders/{id}`, `/orders/{id}/address`, `/orders/{id}/status` |
| Pagamentos | `/payment`, `/payment/{id}`, `/payment/{id}/process` |

## Como reexportar

Com PostgreSQL ativo:

```powershell
.\mvnw.cmd spring-boot:run
```

Em outro terminal:

```powershell
Invoke-WebRequest -Uri 'http://localhost:8080/v3/api-docs' -OutFile 'config/openapi/baseline-openapi-2026-10-03.json'
Get-FileHash 'config/openapi/baseline-openapi-2026-10-03.json' -Algorithm SHA256
```

## Contrato alvo

O primeiro contrato alvo planejado da evolucao Loja Gestao foi versionado em:

```text
config/openapi/target-loja-gestao-openapi-2026-10-03.json
```

Hash SHA-256:

```text
2B028507ACE72F17540155C52C60ED09D8F7BFAAA1E35CAE1022646A4C51B571
```

Este contrato possui 10 rotas planejadas para autenticacao, gestao, meu desempenho, metas, venda, cancelamento, estorno e estoque. Ele usa `x-contract-status: target-planned-not-implemented` e `x-status: planned` nas operacoes para nao confundir proposta com implementacao existente.

Rotas planejadas:

| Grupo | Rotas |
| --- | --- |
| Autenticacao | `/auth/login`, `/auth/me` |
| Gestao e desempenho | `/management/summary`, `/performance/me` |
| Metas | `/goals/monthly` |
| Venda e pedido | `/sales/orders`, `/orders/{id}/cancel` |
| Pagamento | `/payment/{id}/refund` |
| Estoque | `/stock/products`, `/stock/products/{id}/entries` |

O contrato alvo ainda precisa de revisao da equipe antes de virar compromisso de implementacao.

## Revisão do spike

A rodada seguinte criou `config/openapi/target-loja-gestao-openapi-2026-10-03-v2.json`, sem sobrescrever os contratos acima. SHA-256: `16A77FD221546B1A77A5D703E81AD3AAC863F41D25D61390FBC5FB130D4D33C7`. São 10 paths e 11 operações.

A v2 fecha campos extras nas projeções de vendedor e declara papéis propostos via `x-roles`. Sete testes da auditoria estrutural passaram; o diagnóstico legado rejeitou custo/equipe e margem aninhada. A biblioteca disponível não suporta 2020-12, e não houve teste HTTP. Consulte `docs/decisoes/contract-testing.md` para decisão, limitações e pendências antes de adotar o contrato.

## Regras de mudanca

### Revisão documentada de erros — 04/10/2026

`config/openapi/baseline-errors-openapi-2026-10-04.json` deriva do baseline original sem sobrescrevê-lo. SHA-256: `6FDB2A09F3378F1C5E38BD79511FE39770D703ECFF27A367461C07F56229EAB9`. Documenta apenas os casos HTTP já comprovados: POST `/products` 400 e GET `/products/{id}` 404, com `ApiErrorResponse` e timestamp local sem offset. A exportação Springdoc do backend permanece inalterada. Validação executada: 18 testes Node e 71 pontos de schema da revisão sem achados; a reexecução HTTP com validação ampliada dos corpos ainda está pendente. Ver `reports/execucoes/run-2026-10-04-erros-contrato.md`.

- Nao sobrescrever o baseline sem registrar novo arquivo e hash.
- Cada rota nova deve apontar para RF, CA e CT correspondentes.
- Alteracoes incompatíveis no baseline precisam de justificativa academica e registro em `docs/validacao-requisitos.md`.
- Rotas do contrato alvo so devem ser implementadas apos revisao de perfil, banco, transacao e regra de dominio correspondente.
