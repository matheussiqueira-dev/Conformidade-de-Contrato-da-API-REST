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

O workflow `.github/workflows/ci.yml` usa JDK 25, PostgreSQL 18 descartável na porta 15432 e `./mvnw -Pintegration verify`. Também executa os 18 testes Node, validação do contrato e seis casos HTTP com API isolada. Relatórios e log da API são preservados como artefatos. O CI remoto do commit antigo `ca79f02` foi confirmado verde; a execução deste workflow ampliado depende da publicação da rodada atual.

## Decisoes pendentes

- Stack do frontend.
- Estrategia de autenticacao/sessao.
- Unitários padrão e integração com perfil/banco isolados foram executados: seis testes Java aprovados em duas rodadas. Seis casos HTTP da documentação/erros também passaram após a correção de details null.
- Perfil de teste isolado foi adotado; Testcontainers não é necessário para a rodada atual.
