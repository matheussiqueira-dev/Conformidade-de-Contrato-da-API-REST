# Contrato e preparação HTTP — 04/10/2026

## Evidência executada

- Ajv 8.20.0 com export `ajv/dist/2020` e ajv-formats 3.0.1, dependências fixadas em package.json/package-lock.json.
- 13 testes Node aprovados: 7 da auditoria anterior e 6 de dialeto, schemas, projeções, formatos e casos negativos.
- 37 pontos de schema do contrato-alvo v2 validados/compilados, sem achados. Os testes demonstram execução de `prefixItems`/`items: false` de 2020-12; não houve redução a draft-07.
- Relatório: `reports/contrato/spike-2026-10-04/validation.json`.

## Limites e bloqueios atuais

O download do schema oficial do documento OpenAPI falhou por acesso de rede nesta sessão. `officialDocumentSchema: false` e saída 1 deixam explícito que os schemas dos dados passaram, mas o documento OpenAPI completo ainda não foi aprovado. O runner baixa o schema de https://spec.openapis.org/oas/3.1/schema/2025-09-15 e preserva uma cópia em `config/openapi/validation/` quando a rede está disponível.

A API local estava desligada e Docker não estava disponível nesta sessão do agente. O runner HTTP foi preparado, mas os cinco casos HTTP não foram executados aqui. Não há aprovação HTTP, 401/403 ou execução remota do CI nesta rodada.

## Política do validador

`decimal`, `float` e `double` verificam números finitos. `decimal` não prova escala, arredondamento em duas casas ou exatidão financeira; essas decisões continuam em DOM-01. `int32` verifica seus limites e `int64` restringe ao intervalo seguro de inteiros JavaScript, explicitamente mais estreito que Long do Java. Tipos, campos obrigatórios, referências e formatos email/date/date-time são verificados sem coerção. Extensões/annotations OpenAPI são permitidas via `strict: false`; isso não desliga validação de schemas, formatos ou conteúdo.

## Runner pronto para o terminal do usuário

Na pasta `order-management-api`, com JDK 25, Node e Docker Desktop:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-contract.ps1
```

O runner instala as dependências se necessário, executa os 13 testes, valida o documento com schema oficial, gera o jar e inicia uma API exclusiva em 127.0.0.1:18080 com banco descartável `order_management_test` na porta 15432. Os parâmetros do datasource são explícitos, pois o perfil integration é um recurso de teste e não está no jar. Encerra a API iniciada pelo runner e o Compose de teste ao terminar. Não executá-lo ao mesmo tempo que a suíte de integração, pois ambos usam o mesmo projeto Compose de testes.

Se apenas o download oficial falhar, a etapa HTTP pode ser executada separadamente:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-contract.ps1 -HttpOnly
```

Casos preparados: OpenAPI ao vivo com rotas do baseline; listagem de produtos; criação/consulta de produto sintético; POST inválido com 400; produto inexistente com 404. Respostas positivas são comparadas aos schemas do baseline. Os negativos verificam status e conteúdo básico, mas não recebem aprovação de conformidade contra o baseline: ele não declara os schemas de erro 400/404. Esse é um achado de documentação observado nos arquivos, sem sobrescrever o baseline preservado.

## Próximo passo

### Execução HTTP conferida após retomada

Em 04/10/2026, às 00:19:48 (America/Fortaleza), `http.json` registrou os cinco casos preparados como aprovados: OpenAPI ao vivo com rotas do baseline, listagem, criação/consulta sintética, rejeição 400 e recurso inexistente 404. O relatório de validação das 00:19:33 registra schema oficial com adaptação explícita Ajv #1745, 37 pontos de schema e zero achados. Os dois relatórios foram lidos na retomada; o stderr da API está vazio.

A etapa HTTP preparada está executada. O achado confirmado é a ausência de declaração das respostas 400/404 no baseline, embora os negativos tenham retornado os status esperados. A cobertura HTTP desta rodada é limitada aos casos descritos; login, 401/403 e projeções por perfil ainda dependem de API-01. Não há comprovação de CI remoto nem aprovação da equipe para ARQ-01/DOM-01.

### Correção após download do schema oficial

A primeira execução do usuário baixou o schema oficial, mas expôs a limitação Ajv #1745 com âncoras dinâmicas aninhadas: os erros sobre `$ref`, `type` e `required` foram reproduzidos no validador. A evidência anterior foi preservada em `validation-before-ajv-fix.json`. A cópia oficial baixada permanece intacta e está disponível no repositório, também para execução sem rede.

Foi criado um adaptador restrito ao schema oficial fixado: suas quatro referências dinâmicas `#meta` passam a referências estáticas para o placeholder object/boolean já definido no próprio schema. As restrições do documento, inclusive `unevaluatedProperties`, são preservadas; schemas de dados continuam verificados separadamente pelo metaschema 2020-12. O adaptador rejeita alterações inesperadas de ID, placeholder ou contagem de referências. Fonte da limitação: https://github.com/ajv-validator/ajv/issues/1745.

O formato `media-range` agora é validado explicitamente. Resultado executado após a correção: 16 testes aprovados, documento OpenAPI validado com essa adaptação explícita, 37 pontos de schema, zero achados e nenhum aviso de formato ignorado. Os novos testes rejeitam documento sem info, parâmetro sem nome, campo extra em operação e media range inválido. A etapa HTTP permanece pendente; o comando do runner completo continua o mesmo.

Executar o runner no terminal do usuário e conferir `http.json`/`validation.json`. Revisar o contrato baseline para documentar erros em uma nova versão. Os testes das rotas planejadas de login e perfis dependem da implementação API-01; as decisões ARQ-01/DOM-01 permanecem abertas.
