# Decisao tecnica - stack executavel

Data: 03/10/2026.

## Stack observada no backend

| Camada | Decisao atual | Evidencia |
| --- | --- | --- |
| Linguagem | Java 25 | `pom.xml` com `java.version=25`; suite executada com Temurin 25.0.4.1. |
| Framework | Spring Boot 4.1.0 | Parent Maven em `pom.xml`. |
| API REST | Spring WebMVC | Dependencia `spring-boot-starter-webmvc`. |
| Persistencia | Spring Data JPA / Hibernate | Dependencia `spring-boot-starter-data-jpa`; entidades JPA no pacote `entities`. |
| Banco | PostgreSQL | `application.properties` e `docker-compose.yml`. |
| Contrato | Springdoc OpenAPI 2.8.5 | Dependencia `springdoc-openapi-starter-webmvc-ui`. |
| Build/teste | Maven Wrapper | `.mvn/wrapper/maven-wrapper.properties` aponta para Maven 3.9.11. |

## Ambiente local padrao

Subir banco:

```powershell
docker compose up -d postgres
```

Executar testes:

```powershell
.\mvnw.cmd test
```

Executar API:

```powershell
.\mvnw.cmd spring-boot:run
```

## CI minimo

O workflow `.github/workflows/ci.yml` usa JDK 25, PostgreSQL 18 descartável na porta 15432 e `./mvnw -Pintegration verify`. Executa 23 testes de contrato, tres testes do cliente web, seis casos HTTP legados autenticados, seis casos HTTP de sessao, build Next e dois E2E Chromium. Relatorios, log e screenshots ficam como artefatos. A evidencia da rodada atual e registrada em `docs/execucao-sessao.md`.

## Frontend na ETAPA 3 (14/10/2026)

Versões exatas, verificadas com `npm view` em 14/10/2026 e fixadas em `frontend/package.json` (`npm install -E`).

| Pacote | Versão | Uso |
| --- | --- | --- |
| `typescript` | 6.0.3 | TypeScript estrito (`strict`, `noUncheckedIndexedAccess`). A 7.0.2 é a mais nova, mas o pacote nativo não expõe a API de compilador (`createProgram`), que o `next build` usa na checagem de tipos; a 6.0.3 é a última com essa API (D-EXEC-08). |
| `@types/react`, `@types/react-dom` | 19.3.0 | Tipos do React 19.3. |
| `@types/node` | 24.19.2 | Tipos do Node 24, a versão da CI e do ambiente local (a 26.x descreve o Node 26). |
| `zod` | 4.6.7 | Schemas gerados do contrato v4 e validação de formulários e respostas. |
| `orval` | 8.42.0 | `npm run api:generate` gera `lib/api/generated/sbi.zod.ts` (`client: 'zod'`, Zod 4); `npm run api:check` falha se houver diff. |
| `@tanstack/react-query` | 5.104.2 | Cache e estado das consultas. |
| `react-hook-form` | 7.90.0 | Formulários. O resolver fica em `lib/forms.ts`: monta o corpo da requisição e aplica o schema gerado; por isso `@hookform/resolvers` não entrou (D-EXEC-09). |
| `@axe-core/playwright` | 4.13.0 | Verificação WCAG 2.2 AA nos E2E (CT-WEB-109). |

O `postinstall` do `esbuild` (dependência do `orval`) não foi aprovado no `npm approve-scripts`; a geração funciona com o binário opcional da plataforma.

## Decisoes pendentes

- Frontend Next.js e sessao HttpOnly confirmados por Matheus em 04/10/2026. Codigo de acesso adicionado; as quatro areas de dominio e detalhes de hospedagem seguem pendentes. Ver `execucao-sessao.md`.
- Unitários padrão e integração com perfil/banco isolados foram executados: seis testes Java aprovados em duas rodadas. Seis casos HTTP da documentação/erros também passaram após a correção de details null.
- Perfil de teste isolado foi adotado, com o PostgreSQL 18 descartável via Docker Compose (`docker-compose.test.yml`, porta 15432); Testcontainers não é usado (decisão da equipe, D-EVO-38).
