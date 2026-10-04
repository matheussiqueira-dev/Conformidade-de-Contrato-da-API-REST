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

## Decisoes pendentes

- Frontend Next.js e sessao HttpOnly confirmados por Matheus em 04/10/2026. Codigo de acesso adicionado; as quatro areas de dominio e detalhes de hospedagem seguem pendentes. Ver `execucao-sessao.md`.
- Unitários padrão e integração com perfil/banco isolados foram executados: seis testes Java aprovados em duas rodadas. Seis casos HTTP da documentação/erros também passaram após a correção de details null.
- Perfil de teste isolado foi adotado; Testcontainers não é necessário para a rodada atual.
