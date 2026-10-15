# SBI — Super Business Intelligence

Sistema de loja com **venda no balcão** (vendedor) e **gestão** (gerente), feito no projeto A3 de Garantia da Qualidade de Software e Banco de Dados (UNIFG). O tema é a conformidade de contrato da API REST: o que o OpenAPI promete é o que a API entrega. A conformidade é verificada ao vivo, com Bean Validation, um formato único de erro e testes em todas as camadas.

Equipe: Matheus Siqueira, Gabriel Sousa, Allan Barros e Francisco Ferreira.

## O que tem

- **PDV**: busca por SKU ou nome, carrinho com preço e desconto calculados no servidor, Pix, cartão (só os 4 últimos dígitos) ou boleto, retirada ou entrega, reserva de estoque na venda e baixa na confirmação do pagamento.
- **Clientes**: pessoa física e jurídica com dígito verificador (CNPJ numérico ou alfanumérico), documento mascarado para o vendedor, histórico de pedidos e perfil com indicadores de compra.
- **Backoffice do gerente**: produtos e insumos, categorias, preço com motivo e histórico, entradas com custo médio, ajustes com motivo, estoque mínimo e extrato.
- **Gestão**: faturamento, lucro e margem ("Não informado" quando falta custo), metas da loja e da equipe, Meu desempenho, estornos, promoções, desconto manual com alçada (vendedor 5%, gerente 20%) e análises.
- **Usuários**: cadastro pelo gerente com senha provisória, papel, situação, desbloqueio e redefinição de senha; a sessão do usuário cai na requisição seguinte a qualquer dessas mudanças, e sempre existe um gerente ativo.
- **Segurança**: sessão HttpOnly + CSRF, hierarquia GERENTE > VENDEDOR em três barreiras (URL, método e dono do dado), bloqueio após 5 falhas de login (contador atômico) e matriz de acesso executável.

## Stack

| Camada | Tecnologia |
| --- | --- |
| API | Java 25, Spring Boot 4.1, Spring Security 7, `JdbcClient` nos módulos novos e JPA no legado, Springdoc |
| Banco | PostgreSQL 18, Flyway V1–V11 (única fonte do esquema, Hibernate em `validate`), views do V6 e do V9 para os painéis e o resumo do cliente |
| Frontend | Next.js 16, React 19, TypeScript estrito, Zod 4 gerado do contrato (orval), TanStack Query, marca SBI v1.0 |
| Testes | JUnit 5, MockMvc, PostgreSQL 18 descartável via Docker Compose (`docker-compose.test.yml`, porta 15432), Playwright + axe, Ajv para o contrato, pacote SQL por `psql` |

Detalhes em [docs/arquitetura.md](docs/arquitetura.md) e versões em [docs/decisoes/stack.md](docs/decisoes/stack.md).

## Acesso online (sem instalar nada)

A demonstração publicada roda em <https://sbi-app-alpha.vercel.app> (frontend na Vercel, API no Render, banco no Neon) e abre de qualquer navegador. Lá as contas são as dos integrantes da equipe (um gerente e três vendedores), com e-mail e senha combinados com a equipe, fora do repositório; a tabela da seção seguinte vale para a stack local. O plano gratuito do Render dorme o serviço sem acesso: a primeira requisição pode levar cerca de um minuto. Publicação, variáveis e manutenção: [docs/deploy.md](docs/deploy.md).

Cada push na `main` publica sozinho a API (Render) e o frontend (Vercel). Cada pull request ganha uma branch própria do banco no Neon, migrada pelo Flyway, que some quando o PR fecha.

## Subir tudo com Docker (um comando)

Requisito: Docker Desktop. O ambiente de demonstração usa um banco descartável em memória, separado do banco de desenvolvimento `postgres-order`, e já entra com o seed demo (contas, catálogo e vendas).

```powershell
$env:SBI_DEMO_PASSWORD = 'escolha-uma-senha'   # senha das contas demo, de 8 a 72 caracteres
$env:SBI_DB_PASSWORD = [guid]::NewGuid().ToString('N')
docker compose -f docker-compose.demo.yml -p sbi-demo up --build
```

No Linux ou no macOS:

```bash
export SBI_DEMO_PASSWORD='escolha-uma-senha'
export SBI_DB_PASSWORD="$(openssl rand -hex 16)"
docker compose -f docker-compose.demo.yml -p sbi-demo up --build
```

Abra <http://localhost:3000> e entre com uma das contas abaixo, todas com a senha definida em `SBI_DEMO_PASSWORD`:

| Conta | Perfil |
| --- | --- |
| `matheussiqueira@sbi.tech` | Gerente |
| `allanbarros@sbi.tech`, `franciscoferreira@sbi.tech`, `gabrielsousa@sbi.tech` | Vendedor |

Para encerrar e apagar tudo: `docker compose -f docker-compose.demo.yml -p sbi-demo down`.

### Massa de demonstração (Olist) e acesso ao banco por SQL

Para mostrar um histórico de clientes volumoso (60 clientes, 219 pedidos, recorte do conjunto público da Olist, CC BY-NC-SA 4.0), carregue a massa por SQL depois de subir a stack:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\demo-olist.ps1 -Check
```

O banco de demonstração fica acessível só nesta máquina, para o painel Database do IntelliJ, o DBeaver ou o `psql`: `jdbc:postgresql://127.0.0.1:5433/sbi_demo`, usuário `sbi`, senha de `SBI_DB_PASSWORD`. O roteiro de CRUD da apresentação é `docs/bd/pacote-sql/08_crud_clientes_olist.sql`. Detalhes em [docs/bd/massa-demonstracao.md](docs/bd/massa-demonstracao.md).

## Desenvolvimento no VS Code (um atalho)

Requisitos: Docker Desktop aberto, JDK 25 e Node. Com **Ctrl+Shift+B** (tarefa "SBI: iniciar tudo", em `.vscode/tasks.json`), o `scripts/dev.ps1` sobe um PostgreSQL descartável em `127.0.0.1:5434`, a API no perfil `demo` em <http://localhost:8080> e o frontend em <http://localhost:3000>, cada um no seu terminal. O banco é recriado a cada início, e o stack `sbi-demo` é parado se estiver ocupando a porta 3000.

As senhas ficam em `.env.local` (fora do git), gerado na primeira execução: `SBI_DEMO_PASSWORD` é a senha das contas demo da tabela acima. Para encerrar, feche os terminais da tarefa e rode a tarefa "SBI: parar banco".

## Perfis da API

| Perfil | Uso |
| --- | --- |
| padrão | Desenvolvimento com o banco `postgres-order` (`docker compose up -d postgres`) e a senha em `SPRING_DATASOURCE_PASSWORD`. |
| `demo` | Aplica também `db/demo` (V6_1, contas com BCrypt de `SBI_DEMO_PASSWORD`; V6_2, massa sintética). Precisa estar ativo desde a primeira migração do banco. |
| `integration` (testes) | PostgreSQL descartável da porta 15432 (`docker-compose.test.yml`), com as tabelas de negócio esvaziadas por `TRUNCATE`. |
| `contract` | API isolada dos testes HTTP de contrato, com contas sintéticas e senha gerada por execução. |

Variáveis: `SPRING_DATASOURCE_PASSWORD` é obrigatória, e a API não sobe sem ela. `SESSION_COOKIE_SECURE=false` só vale para HTTP local. `BACKEND_ORIGIN` é a origem da API usada pelo frontend (padrão `http://127.0.0.1:8080`). Nenhuma senha fica no repositório.

## Testes

Execute no PowerShell, na raiz do repositório, com JDK 25 em `JAVA_HOME` e Docker ativo:

```powershell
.\mvnw.cmd -q verify                                                                        # unitários (233)
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test.ps1 -Mode Integration     # integração (649), JaCoCo, PMD, CPD, SpotBugs
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test-contract.ps1 -HttpOnly -Frontend  # contrato ao vivo + E2E e axe
npm test; node scripts\audit-contract.mjs                                                  # auditoria estática do contrato
cd frontend; npm run typecheck; npm run api:check; npm test; npm run build
node scripts\quality-metrics.mjs reports\metricas\r1 "R1"                                  # métricas a partir de target/
```

A CI (`.github/workflows/ci.yml`) roda quatro jobs, cada um com seus relatórios como artefato:
- `test (backend)`;
- `bd`: Flyway, seed demo e pacote SQL com 60 PASS, mais 10 PASS da massa Olist;
- `contract`: auditoria estática e verificação ao vivo das 58 operações do contrato v5;
- `frontend`: typecheck, schemas sem diff, testes unitários, build, E2E e axe.

## Documentação

| Assunto | Documento |
| --- | --- |
| Arquitetura, fluxo de venda e segurança | [docs/arquitetura.md](docs/arquitetura.md) |
| Roteiro de demonstração (12 minutos) | [docs/roteiro-demonstracao.md](docs/roteiro-demonstracao.md) |
| Relatório final com números reais | [docs/relatorio-final.md](docs/relatorio-final.md) |
| Requisitos, critérios e rastreabilidade | [docs/requisitos.md](docs/requisitos.md), [docs/criterios-aceitacao.md](docs/criterios-aceitacao.md), [docs/rastreabilidade.md](docs/rastreabilidade.md) |
| Plano de testes, defeitos e métricas | [docs/plano-testes.md](docs/plano-testes.md), [docs/defeitos.md](docs/defeitos.md), [reports/metricas/comparativo-r0-r1.md](reports/metricas/comparativo-r0-r1.md) |
| Modelo de dados e pacote SQL | [docs/bd/modelo-erp-pdv.md](docs/bd/modelo-erp-pdv.md), [docs/bd/pacote-sql/](docs/bd/pacote-sql/) |
| Decisões | [docs/decisoes/](docs/decisoes/) (ERP/PDV, evolução D-EVO, matriz de acesso, autenticação, dinheiro, stack) |
| Marca SBI v1.0 aplicada | [reports/marca/](reports/marca/) (capturas de todas as telas nos dois papéis) |
| Contrato | [config/openapi/target-loja-gestao-openapi-2026-10-14-v5.json](config/openapi/target-loja-gestao-openapi-2026-10-14-v5.json) e Swagger em `/swagger-ui/index.html` |
| Rotas legadas (`/orders`, `/payment`, `/address`) | [docs/api-legada.md](docs/api-legada.md) |
