# Spike de contrato — 03/10/2026

Atualizacao de 04/10: sessao HttpOnly/CSRF confirmada e contrato v3 adicionado, preservando a v2. Schema oficial agora e versionado e validado com adaptacao explicita Ajv #1745; codigo de autenticacao, Next e suites HTTP/E2E adicionados. Estado e evidencia atual em [execucao-sessao.md](../execucao-sessao.md). As secoes abaixo documentam o diagnostico historico da v2.

## Resultado

O contrato-alvo inicial possui 10 paths e 11 operações (GET e PUT em metas).
A auditoria estrutural identificou schemas abertos nas projeções do vendedor e ausência de uma política explícita de perfis para gestão, metas e estoque. Isso é uma lacuna do contrato planejado, não um vazamento reproduzido na API: essas rotas ainda não existem.

Foi criado `config/openapi/target-loja-gestao-openapi-2026-10-03-v2.json`, preservando o baseline e o alvo inicial. A revisão fecha campos extras em `SellerPerformance`, `SalesOrderResponse`, seus itens, `UserSession` e `Money`; declara `x-roles` nas operações. Essas extensões documentam uma proposta e não executam autorização. O controle de autoria no cancelamento e as permissões de estorno continuam sujeitos a DOM-01/API-01. Bearer permanece uma hipótese de contrato até ARQ-01; não houve escolha final de sessão.

## Ferramentas e execução

- Node.js nativo: `scripts/audit-contract.mjs`, sem dependências externas. Confere referências locais, IDs de operação, RF, marcadores de planejamento, parâmetros, declaração de segurança e fechamento das projeções. Não valida toda a especificação OpenAPI nem todo JSON Schema.
- `node --test scripts/audit-contract.test.mjs`: sete testes positivos/negativos, incluindo referência quebrada, rota pública indevida, perfil indevido, projeção aberta e campo sensível.
- `scripts/probe-legacy-ajv.cjs`: diagnóstico com Ajv 6.15.0 já instalado em outro projeto local, sem alteração desse projeto. A execução recebe explicitamente o diretório do módulo. Não é dependência adotada pelo A3.
- Exemplo sintético de desempenho da Ana: meta 80.000, realizado 68.400, atingimento 85,5%. Sem HTTP ou conta real.

O Ajv disponível aceitou o exemplo válido nos dois alvos; o alvo inicial também aceitou custo/equipe e margem aninhada. A v2 rejeitou esses campos extras. O diagnóstico usa apenas o subconjunto compatível com draft-07 e não prova conformidade 3.1.

## Decisão

Rejeitar Ajv 6 como validador definitivo do contrato 3.1: a compilação com metaschema 2020-12 falhou (`no schema with key or ref`). Avaliar Ajv 8 com `ajv/dist/2020` e formatos explícitos ou outro validador OpenAPI 3.1 após liberar a instalação. A documentação oficial explica os exports por dialeto: https://ajv.js.org/json-schema.html e https://ajv.js.org/v6-to-v8-migration.html.

Não reduzir silenciosamente o dialeto para fazer os testes passarem. O formato `decimal` de dinheiro também precisa de política explícita; não foi validado pelo diagnóstico legado.

## Aceite ainda pendente

Atualização verificada após execução do usuário em 04/10/2026 às 00:19:48: os cinco casos HTTP preparados passaram, e a validação do documento com adaptação explícita Ajv #1745 mais 37 pontos de schema terminou sem achados. `http.json` confirma lacuna de documentação das respostas 400/404 no baseline. Os itens históricos abaixo sobre instalação, download e execução dos casos HTTP preparados foram superados; autorização real, autoria, revisão da equipe e declaração de erros permanecem pendentes. Evidências: `reports/contrato/spike-2026-10-04/` e `reports/execucoes/run-2026-10-04-contrato.md`.

### Continuação em 04/10/2026

Ajv 8.20.0/ajv-formats 3.0.1 foram adotados com lockfile. Os 13 testes Node passaram e 37 pontos de schema do alvo v2 compilaram/validaram com 2020-12. A validação do documento OpenAPI completo permanece bloqueada pelo download do schema oficial nesta sessão. O runner isolado `scripts/test-contract.ps1` prepara cinco casos HTTP; execução ainda pendente. Política de formatos, instruções e limites: `reports/execucoes/run-2026-10-04-contrato.md`. Os itens abaixo devem ser lidos com essa atualização: instalar Ajv 8 já foi resolvido; documento completo e HTTP ainda não.

- Instalar e provar o validador 2020-12; validar o documento OpenAPI completo.
- Executar positivo e negativo HTTP contra a API, com PostgreSQL ativo.
- Após API-01, executar sessão real e 401/403; verificar JSON por perfil e autoria.
- Revisão de Allan/Gabriel das permissões propostas e do contrato v2.

Evidências: `reports/contrato/spike-2026-10-03/` e `reports/execucoes/run-2026-10-03-continuacao.md`.
