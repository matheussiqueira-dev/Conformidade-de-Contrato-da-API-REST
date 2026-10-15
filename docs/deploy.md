# Publicação da demonstração (Vercel + Render + Neon)

Ambiente público de demonstração: o frontend Next.js na Vercel, a API Spring Boot no Render (Docker) e o PostgreSQL no Neon. Nenhum valor real de senha, usuário ou URL de banco entra no repositório: tudo fica nas variáveis de ambiente de cada plataforma. Os dados são os do perfil `demo` (seed sintético) mais a massa Olist (pedidos anonimizados, nomes e documentos sintéticos).

```
navegador ──HTTPS──> Vercel (sbi-app, Next.js) ──/api/backend/* (rewrite)──> Render (API, Docker) ──JDBC + TLS──> Neon (PostgreSQL 18)
```

O navegador fala só com a Vercel. O cookie de sessão da API passa pelo rewrite do Next e fica associado ao domínio da Vercel, por isso `SESSION_COOKIE_SECURE=true` exige o frontend em HTTPS (a Vercel já serve assim).

Ordem: Neon → Render (o Flyway cria o esquema e as contas) → carga Olist → Vercel.

## Ambiente publicado

| Peça | Onde | Endereço / identificação | Região |
| --- | --- | --- | --- |
| Frontend | Vercel, projeto `sbi-app` (raiz `frontend`, produção a partir da `main`) | <https://sbi-app-alpha.vercel.app> (endereço oficial da demonstração) | funções em `iad1` (Washington, D.C.); a borda é a mais próxima de quem acessa (ex.: `gru1`, São Paulo) |
| API | Render, serviço `sbi-api` (Docker, plano gratuito, deploy automático a cada push na `main`, health check `/auth/csrf`) | <https://sbi-api-puk8.onrender.com> | Virginia (US East) |
| Banco | Neon, projeto `wispy-glade-88962628`, branch `production`, banco `neondb`, PostgreSQL 18 | host direto (sem `-pooler`) no painel do Neon | AWS US East 2 (Ohio) |

Variáveis, sem valores (os valores ficam só no painel de cada plataforma):

| Plataforma | Variáveis |
| --- | --- |
| Render | `SPRING_PROFILES_ACTIVE`, `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SBI_DEMO_PASSWORD`, `SESSION_COOKIE_SECURE`, `SPRING_JPA_SHOW_SQL`, `PORT`, `JAVA_TOOL_OPTIONS` (seção 2) |
| Vercel | `BACKEND_ORIGIN` = `https://sbi-api-puk8.onrender.com` (seção 4); as variáveis `DATABASE_URL*` que a integração do Neon cria não são usadas pelo frontend |
| GitHub | segredo `NEON_API_KEY` e variável `NEON_PROJECT_ID` (seções 3 e 5); segredos `SBI_SMOKE_*` do smoke (seção 7) |

No banco publicado, as quatro contas do seed foram renomeadas para os integrantes da equipe (seção 3); as metas da loja vão de 09/2026 a 12/2026. Aquecimento: seção 6. Smoke somente leitura: seção 7.

## 1. Neon (banco)

1. Crie um projeto com **PostgreSQL 18**, na região mais próxima do Render (AWS US East, Virgínia).
2. Use o banco `neondb`, criado com o projeto (é o que a demonstração publicada usa). Se preferir um banco `sbi`, crie-o e troque o fim da URL e a variável `NEON_DATABASE` dos workflows.
3. Em *Connection details*, desligue *Connection pooling* e copie o host **sem** `-pooler`. O Flyway e o contexto de sessão que os gatilhos usam (`set_config`) precisam de conexão direta; o pooler em modo transação quebra os dois.
4. O usuário dono do projeto basta: nenhuma migração precisa de superusuário (não há `CREATE ROLE`, `CREATE EXTENSION` nem `ALTER SYSTEM`).

URL JDBC para o Render: `jdbc:postgresql://<host-sem-pooler>/neondb?sslmode=require`.

## 2. Render (API)

*New → Web Service*, a partir deste repositório:

| Campo | Valor |
| --- | --- |
| Runtime | Docker (`Dockerfile` da raiz) |
| Região | Virginia (US East) |
| Health check path | `/auth/csrf` |
| Instância | a menor que comporte a JVM; as opções de memória abaixo cabem em 512 MB |

Variáveis de ambiente (os valores reais ficam só no painel do Render):

| Variável | Valor |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `demo` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host-do-neon-sem-pooler>/neondb?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `<usuário do Neon>` |
| `SPRING_DATASOURCE_PASSWORD` | `<senha do Neon>` |
| `SBI_DEMO_PASSWORD` | `<senha das contas demo, 12 a 72 bytes>` |
| `SESSION_COOKIE_SECURE` | `true` |
| `SPRING_JPA_SHOW_SQL` | `false` (o padrão do projeto registra cada consulta no log) |
| `PORT` | `8080` (opcional: `application.properties` usa `server.port=${PORT:8080}`, então a porta que o Render informar também funciona) |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k` |

**`SBI_DEMO_PASSWORD` só vale na primeira migração.** A `V6_1__DemoAccounts` grava o BCrypt das contas `rafael@loja.test`, `ana@loja.test`, `bruno@loja.test` e `carla@loja.test` uma única vez, quando o Flyway aplica o perfil `demo` num banco vazio. Trocar a variável depois não muda a senha: para isso, apague e recrie o banco `sbi` no Neon (e rode a carga Olist de novo) ou troque a senha pela tela de usuários. Como o ambiente é público e os e-mails do seed são conhecidos, use uma senha forte e exclusiva.

No primeiro deploy, o log do Render deve mostrar o Flyway aplicando V1 a V11 e as migrações `db/demo`. Conferência: `https://sbi-api-puk8.onrender.com/auth/csrf` responde 200 com o token. Cada chamada a `/auth/csrf` abre uma sessão de até 30 minutos (o token CSRF fica na sessão), então o health check mantém algumas centenas de sessões pequenas em memória, o que cabe na instância.

## 3. Carga Olist no Neon

Depois que a API subiu (esquema criado pelo Flyway), a forma mais simples é pelo GitHub: *Actions → Neon Olist dataset → Run workflow* (`.github/workflows/neon-olist.yml`). O workflow pega a URL sem pooler com a `NEON_API_KEY`, roda `07_carga_olist.sql` duas vezes e os testes `09_testes_olist.sql`, sem nada instalado na máquina de quem dispara. A carga é idempotente, então rodar de novo não duplica dados.

Pela máquina local, a massa entra por SQL puro com o `psql` do contêiner `postgres:18`, sem docker compose. A URL fica só numa variável de ambiente da sessão:

```powershell
$env:SBI_DATABASE_URL = 'postgresql://<usuário>:<senha>@<host-sem-pooler>/neondb?sslmode=require'
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\demo-olist.ps1 -DatabaseUrl $env:SBI_DATABASE_URL -Check
Remove-Item Env:SBI_DATABASE_URL
```

Em bash: `bash docs/bd/pacote-sql/run_demo_olist.sh --database-url "$SBI_DATABASE_URL" --check`.

O script confere que o esquema existe, roda `07_carga_olist.sql` (com `-Check`, duas vezes, para provar a idempotência) e os testes `09_testes_olist.sql` (CT-DEMO-01 a 04). A URL chega ao `psql` por variável de ambiente do contêiner, fora da linha de comando do `docker run`. O CT-DEMO-05 (máscara de documento pela API) pode ser conferido com `node scripts/demo-olist-api.mjs https://sbi-app-alpha.vercel.app/api/backend` e `SBI_DEMO_PASSWORD` definida.

**Contas do ambiente publicado.** No Neon, as quatro contas da `V6_1` foram renomeadas para os integrantes da equipe (nome, e-mail e senha próprios, combinados fora do repositório); papéis, vendas, metas e `db_role` continuam os mesmos. Por isso a carga `07_carga_olist.sql` acha o gerente e os vendedores pelo `db_role` (`gerente_rafael`, `vendedora_ana`, `vendedor_bruno`, `vendedora_carla`), e não pelo e-mail. No CT-DEMO-05 contra esse ambiente, informe as contas com `SBI_DEMO_SELLER_EMAIL`/`SBI_DEMO_SELLER_PASSWORD` e `SBI_DEMO_MANAGER_EMAIL`/`SBI_DEMO_MANAGER_PASSWORD`. As branches Neon criadas antes da troca (preview de PR) ainda têm as contas `@loja.test`.

Para o IntelliJ ou o DBeaver, use a mesma URL do Neon no formato JDBC com o usuário e a senha do painel; o roteiro `docs/bd/pacote-sql/08_crud_clientes_olist.sql` termina sempre em `ROLLBACK`.

## 4. Vercel (frontend)

Importe o repositório pelo painel da Vercel (não use `vercel link` na pasta que reúne os projetos):

| Campo | Valor |
| --- | --- |
| Nome do projeto | `sbi-app` |
| Root Directory | `frontend` |
| Framework | Next.js (detectado) |
| Build e instalação | padrão (`npm ci`, `next build`), pelo `package-lock.json` |

Variável de ambiente, marcada para **build** e execução em Production (e Preview, se usar):

| Variável | Valor |
| --- | --- |
| `BACKEND_ORIGIN` | `https://sbi-api-puk8.onrender.com` (só a origem: sem caminho, sem barra final, sem credenciais) |

O `next.config.mjs` grava o rewrite `/api/backend/*` → `BACKEND_ORIGIN` no `next build`, e os layouts do servidor usam a mesma variável na execução. Se a URL da API mudar, refaça o deploy (novo build), não basta reiniciar.

## 5. Branch Neon por pull request (opcional)

Se o projeto Neon foi ligado ao GitHub pela integração do Neon, `NEON_PROJECT_ID` e `NEON_API_KEY` já existem no repositório. O workflow `.github/workflows/neon-preview.yml` cria, para cada PR deste repositório, a branch Neon `preview/pr-<número>` a partir da branch padrão do projeto, aplica as migrações do PR com o Flyway (perfil `demo`, host sem pooler) e apaga a branch quando o PR fecha. A branch expira sozinha em 14 dias. Enquanto `NEON_PROJECT_ID` não estiver definida, os jobs são pulados; PRs de fork também, porque não recebem secrets.

Configuração em *Settings → Secrets and variables → Actions* do GitHub:

| Tipo | Nome | Valor |
| --- | --- | --- |
| Variable | `NEON_PROJECT_ID` | ID do projeto no Neon (*Settings → General*) |
| Secret | `NEON_API_KEY` | chave de API do Neon (*Account → API keys*) |
| Variable (opcional) | `NEON_DATABASE` | padrão `neondb` |
| Variable (opcional) | `NEON_ROLE` | padrão `neondb_owner` |
| Secret (opcional) | `SBI_DEMO_PASSWORD` | só é lida se a branch pai ainda não tiver as contas demo; sem ela, o job gera uma aleatória |

A branch pai precisa ter sido migrada com o perfil `demo` (é o caso depois do primeiro deploy no Render): com `out-of-order` desligado, aplicar o perfil `demo` sobre um banco migrado sem ele falha. Se a branch já tiver expirado quando o PR fechar, o job de exclusão falha sem efeito no banco.

## 6. Antes da demonstração: aquecer 5 minutos antes

O plano gratuito do Render dorme o serviço depois de um tempo sem acesso, e o Neon suspende o compute ocioso. A primeira requisição depois disso espera a JVM subir (perto de um minuto) e o banco acordar. Cinco minutos antes de apresentar:

1. abra `https://sbi-api-puk8.onrender.com/auth/csrf` até responder 200 (a primeira chamada pode levar de 1 a 3 minutos com o serviço dormindo);
2. abra `https://sbi-app-alpha.vercel.app/entrar`, entre como gerente e visite Visão geral e Clientes (aquece a JVM, o pool de conexões e o banco);
3. mantenha uma aba aberta em Visão geral: o painel consulta a API a cada 15 segundos com a aba visível, o que impede o serviço de dormir durante a apresentação.

Se algo falhar ao vivo, o plano B do [roteiro de demonstração](roteiro-demonstracao.md) usa a stack local (`docker-compose.demo.yml`) e as capturas de `reports/marca/`.

## 7. Smoke do ambiente publicado

O workflow `.github/workflows/published-smoke.yml` confere o site publicado sem escrever nada no banco: login do gerente e do vendedor, menu de cada papel, os quatro indicadores e o cartão da meta no `/gestao`, um cliente da massa Olist com documento mascarado para o vendedor e axe WCAG 2.2 AA em `/entrar`, `/gestao` e `/vender`. Os testes ficam em `frontend/e2e/published/`, fora da suíte normal, e falham se o navegador mandar à API qualquer requisição que não seja GET (fora login e logout).

Disparo manual, depois de aquecer o serviço:

```powershell
gh workflow run published-smoke.yml
gh workflow run published-smoke.yml -f base_url=https://sbi-app-alpha.vercel.app
gh run list --workflow published-smoke.yml --limit 1
```

O próprio workflow aquece a API (até 6 tentativas em `/api/backend/auth/csrf`, a cada 30 s) e guarda o relatório HTML e as capturas no artefato `published-smoke`. As contas vêm de quatro segredos do repositório, criados pelo responsável em *Settings → Secrets and variables → Actions*: `SBI_SMOKE_MANAGER_EMAIL`, `SBI_SMOKE_MANAGER_PASSWORD`, `SBI_SMOKE_SELLER_EMAIL` e `SBI_SMOKE_SELLER_PASSWORD`. Sem eles, o workflow para no primeiro passo com a lista do que falta.

Pela máquina local, com as mesmas variáveis definidas na sessão (lidas de um arquivo fora do repositório):

```powershell
cd frontend
npx playwright test -c playwright.published.config.mjs
```
