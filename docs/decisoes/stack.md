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

Foi criado `.github/workflows/ci.yml` com JDK 25, cache Maven, PostgreSQL 18 como service e comando `./mvnw test`. A execucao remota depende de o workflow ser publicado no GitHub.

## Decisoes pendentes

- Stack do frontend.
- Estrategia de autenticacao/sessao.
- Separacao futura entre testes unitarios rapidos e testes que exigem PostgreSQL.
- Testcontainers ou perfil de teste isolado para reduzir dependencia de porta local 5432.
