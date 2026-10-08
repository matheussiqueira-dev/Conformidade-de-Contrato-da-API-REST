# Diagnostico de inicializacao — 05/10/2026

Analise e ajuste por Matheus Siqueira.

## Causas confirmadas

1. Na tentativa anterior, o ambiente restrito negou acesso ao executavel Docker e aos downloads npm (`EACCES`). Com acesso liberado, Docker foi encontrado no PATH e o build Java passou. Isso nao era uma falha de compilacao da aplicacao.
2. A configuracao normal aponta para `localhost:5432/order_management`. Nenhum banco estava ouvindo nessa porta. Uma execucao diagnostica, com `ddl-auto=validate` para preservar o schema, reproduziu `Connection to localhost:5432 refused`.
3. `docker compose up -d --wait postgres` falhou porque o nome `postgres-order` pertence a um container anterior, criado fora desse projeto Compose. O container estava parado, usa a porta 5432 e o database `order_management`. `docker start postgres-order` retomou esse banco; `pg_isready` confirmou que aceita conexoes. Nenhum container com dados foi removido.
4. A instalacao da interface falhou no `npm ci`: downloads do registry retornaram `ECONNRESET`. Curl, Node fetch e Invoke-WebRequest tambem nao conseguiram baixar tarballs do registry dentro dos respectivos timeouts. DNS resolveu o dominio, mas isso nao comprova que HTTPS/download esteja funcionando. A origem externa da interrupcao (proxy, firewall, rede ou registry) nao foi determinada. Nao houve resposta HTTP 404 que comprovasse versoes de pacotes inexistentes.

## Schema e acesso

Depois de reiniciar o banco normal, a verificacao com `ddl-auto=validate` encontrou `Schema validation: missing table [app_users]`. A configuracao normal do projeto usa `ddl-auto=update`; portanto esse resultado e um diagnostico de divergencia de schema, nao uma reproducao da inicializacao normal com update. Nenhum update de schema ou Flyway foi executado nesse banco nesta analise. Contas sinteticas so existem no perfil contract isolado; ausencia de contas no banco normal tambem impede login, mesmo com servicos ativos.

## Evidencias executadas

- `scripts/test-contract.ps1 -Preview`: 23 testes Node de contrato passaram; schema OpenAPI oficial e schemas de dados sem achados.
- Maven `-DskipTests package`: BUILD SUCCESS. Nao equivale a executar testes Java.
- API contract iniciou no banco descartavel da porta 15432 e respondeu na porta 18080.
- Seis casos HTTP de baseline e seis de sessao passaram. Evidencias em `reports/contrato/spike-2026-10-04/http-swagger-errors-v1.json` e `reports/contrato/session-2026-10-04/http-session.json`.
- `node --test frontend/tests/*.test.mjs`: tres testes passaram.
- Build Next e E2E nao foram executados: instalacao das dependencias falhou antes dessas etapas.

## Ajuste realizado

`scripts/test-contract.ps1` agora instala o frontend antes de iniciar os servicos descartaveis, informa explicitamente a falha de instalacao/rede, limita downloads npm a uma repeticao com timeout de 20 segundos por tentativa e verifica a porta 3000. Removeu-se a instalacao duplicada da etapa posterior. O ajuste melhora o diagnostico e o tempo de falha; nao corrige a conectividade externa.

Reexecucao do runner ajustado confirmou a mensagem especifica de ECONNRESET e a saida antes do build Java e de criar API/banco descartavel. Os 23 testes de contrato passaram novamente nessa reexecucao. `git diff --check` sem erros de whitespace.

## Estado e reproducao

PostgreSQL normal reiniciado; banco normal preservado. A API diagnostica com validate encerrou por tabela faltante. O primeiro runner encerrou API/banco de teste ao falhar no npm. A interface nao esta disponivel, e o sistema completo nao foi validado nesta rodada.

Na pasta `order-management-api`, depois de restabelecer o download HTTPS do registry:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-contract.ps1 -Preview
```

O runner usa banco descartavel, informa as contas e a senha temporaria, e publica a interface em `http://127.0.0.1:3000` somente depois de build e testes aprovados. Nao rodar em paralelo com os testes de integracao que usam o mesmo banco descartavel.

## Confirmacao apos a tentativa pelo terminal do usuario

O log npm `2026-10-05T23_04_42_904Z-debug-0.log`, em `%LOCALAPPDATA%/npm-cache/_logs`, confirma `read ECONNRESET` no tarball `source-map-js-1.2.2.tgz`. npm registry esta configurado como `https://registry.npmjs.org/`; proxy e https-proxy do npm retornam null. WinHTTP indica acesso direto e o proxy explicito do Windows esta desativado.

DNS resolveu registry.npmjs.org e o teste TCP da porta 443 passou. Curl reproduziu timeout ao estabelecer HTTPS; uma segunda sondagem, mantendo o dominio e usando outro IP retornado pelo mesmo DNS, falhou com `Recv failure: Connection was reset`. Assim, o problema nao se restringe ao npm nem ao script PowerShell. A camada HTTPS esta falhando apesar da conectividade TCP; nao ha evidencia suficiente para atribuir a interrupcao a firewall, VPN, filtro da rede ou problema do provedor. Nao foram alterados proxy, DNS, certificados, versoes de dependencias ou configuracoes de seguranca.
