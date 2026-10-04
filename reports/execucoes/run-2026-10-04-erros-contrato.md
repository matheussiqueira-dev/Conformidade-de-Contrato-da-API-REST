# Revisão dos erros do contrato — 04/10/2026

## Alteração

Criado `config/openapi/baseline-errors-openapi-2026-10-04.json`, derivado do baseline original preservado. SHA-256 da revisão: `6FDB2A09F3378F1C5E38BD79511FE39770D703ECFF27A367461C07F56229EAB9`.

Escopo comprovado na rodada HTTP anterior: resposta 400 de `POST /products` por validação/regra de produto e resposta 404 de `GET /products/{id}` por recurso inexistente. Não foram acrescentadas respostas por suposição em outras operações. Schemas e respostas de sucesso do baseline foram mantidos.

O schema `ApiErrorResponse` exige timestamp, status, error e message; details pode ser array de strings, null ou ausente. Cada resposta restringe o status do corpo ao status HTTP correspondente. O timestamp é LocalDateTime sem offset, conforme a entidade ErrorResponse atual. O formato `local-date-time` executa validação de data/hora sem converter o valor ou atribuir fuso.

O artefato é uma revisão documentada do contrato. Não altera o Java nem a exportação Springdoc ao vivo: esta ainda precisa incorporar a documentação dos erros numa futura mudança do backend.

## Validação executada

- 18 testes Node aprovados, zero falhas.
- Nova revisão: documento OpenAPI validado com adaptação explícita Ajv #1745 e 71 pontos de schema verificados, zero achados.
- Contrato-alvo v2: 37 pontos de schema, zero achados.
- Hash do baseline original conferido: `4CF78EA43C7A24490F5B53F4E4BB11FCBBC4689EFDC1DB57BB9FA5EB9FE8F180`.
- Respostas de sucesso originais comparadas com a revisão, sem alterações.
- Casos negativos rejeitam status incompatível, detalhe numérico, data impossível, timestamp com offset e mensagem ausente.

## Reexecução HTTP pendente

### Atualização: execução conferida

Em 04/10/2026 às 00:25:58 (America/Fortaleza), o relatório `http-errors-v1.json` registrou os cinco casos aprovados contra `baseline-errors-openapi-2026-10-04.json`, incluindo validação de schema dos corpos 400 e 404. A leitura de `validation.json` confirmou novamente 37 pontos do alvo e 71 da revisão, com zero achados. A reexecução descrita abaixo foi concluída; permanecem a documentação Springdoc ao vivo e as decisões ARQ-01/DOM-01, além da revisão/publicação e CI remoto.

O runner passou a validar os corpos 400/404 com o novo contrato, usando a mesma função exercitada nos testes Node. O resultado será gravado em `reports/contrato/spike-2026-10-04/http-errors-v1.json`, preservando o `http.json` da rodada anterior. Os cinco casos anteriores passaram, mas não comprovaram esta validação ampliada dos corpos de erro.

Com Docker Desktop ativo, executar na pasta `order-management-api`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-contract.ps1
```

O runner continua usando apenas a API isolada e o banco descartável. Após conferir `http-errors-v1.json`, revisar/publicar as alterações e fechar ARQ-01/DOM-01 com a equipe antes da evolução dependente de autenticação.
