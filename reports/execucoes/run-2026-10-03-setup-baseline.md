# Execucao - setup e baseline

Data/hora local: 03/10/2026 20:00, America/Fortaleza.

## Objetivo

Desbloquear a execucao local do projeto, rodar a suite minima e exportar o contrato OpenAPI baseline.

## Ambiente

| Item | Valor |
| --- | --- |
| Sistema | Windows 11 |
| JDK | Temurin 25.0.4.1, portatil em `tmp/tools/jdk-25` |
| Maven | Apache Maven 3.9.11 via Maven Wrapper |
| Banco | PostgreSQL Docker, container `postgres-order`, database `order_management` |
| URL JDBC | `jdbc:postgresql://localhost:5432/order_management` |
| Aplicacao | Spring Boot 4.1.0 |

## Ajustes realizados

- Criado `.mvn/wrapper/maven-wrapper.properties` apontando para Maven 3.9.11.
- Baixado JDK 25 portatil para o workspace, sem instalacao global no sistema.
- Iniciado Docker Desktop instalado no perfil do usuario.
- Criado container PostgreSQL `postgres-order` com `POSTGRES_DB=order_management` e senha `senha123`, conforme README.
- Criado `docker-compose.yml` para versionar o PostgreSQL local com healthcheck.
- Criado `.github/workflows/ci.yml` com JDK 25, PostgreSQL 18 e `./mvnw test`.
- Registradas as decisoes `docs/decisoes/stack.md` e `docs/decisoes/contrato.md`.

## Comandos executados

```powershell
.\mvnw.cmd -version
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
Invoke-WebRequest -Uri 'http://localhost:8080/v3/api-docs' -OutFile 'config/openapi/baseline-openapi-2026-10-03.json'
```

## Resultados

| Verificacao | Resultado |
| --- | --- |
| Maven Wrapper | Funcionou com Maven 3.9.11. |
| Suite minima inicial | `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`. |
| Suite apos correcao CT-MONEY-002 | `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`. |
| Aplicacao | Subiu em `http://localhost:8080`. |
| OpenAPI baseline | Exportado para `config/openapi/baseline-openapi-2026-10-03.json`. |
| SHA-256 do contrato | `4CF78EA43C7A24490F5B53F4E4BB11FCBBC4689EFDC1DB57BB9FA5EB9FE8F180`. |
| Paths no contrato | 14. |
| CI minimo | Workflow criado; execucao remota depende de publicacao no GitHub. |

## Paths exportados

- `/address`
- `/address/{id}`
- `/clients`
- `/clients/{id}`
- `/clients/{id}/address`
- `/orders`
- `/orders/{id}`
- `/orders/{id}/address`
- `/orders/{id}/status`
- `/payment`
- `/payment/{id}`
- `/payment/{id}/process`
- `/products`
- `/products/{id}`

## Observacoes

- A primeira tentativa de teste falhou porque nao havia PostgreSQL aceitando conexao em `localhost:5432`.
- O Docker exigiu `DOCKER_API_VERSION=1.51` para alguns comandos neste ambiente.
- A suite agora inclui um teste unitario de regra monetaria para CT-MONEY-002.
- O container PostgreSQL ficou disponivel para proximas execucoes locais.
