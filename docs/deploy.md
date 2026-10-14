# Publicação da demonstração (Vercel + Render + Neon)

Ambiente público de demonstração: o frontend Next.js na Vercel, a API Spring Boot no Render (Docker) e o PostgreSQL no Neon. Nenhum valor real de senha, usuário ou URL de banco entra no repositório: tudo fica nas variáveis de ambiente de cada plataforma. Os dados são os do perfil `demo` (seed sintético) mais a massa Olist (pedidos anonimizados, nomes e documentos sintéticos).

```
navegador ──HTTPS──> Vercel (sbi-app, Next.js) ──/api/backend/* (rewrite)──> Render (API, Docker) ──JDBC + TLS──> Neon (PostgreSQL 18)
```

O navegador fala só com a Vercel. O cookie de sessão da API passa pelo rewrite do Next e fica associado ao domínio da Vercel, por isso `SESSION_COOKIE_SECURE=true` exige o frontend em HTTPS (a Vercel já serve assim).

Ordem: Neon → Render (o Flyway cria o esquema e as contas) → carga Olist → Vercel.

## 1. Neon (banco)

1. Crie um projeto com **PostgreSQL 18**, na região mais próxima do Render (AWS US East, Virgínia).
2. Crie o banco `sbi` (o padrão do Neon é `neondb`; se preferir usá-lo, troque o fim da URL).
3. Em *Connection details*, desligue *Connection pooling* e copie o host **sem** `-pooler`. O Flyway e o contexto de sessão que os gatilhos usam (`set_config`) precisam de conexão direta; o pooler em modo transação quebra os dois.
4. O usuário dono do projeto basta: nenhuma migração precisa de superusuário (não há `CREATE ROLE`, `CREATE EXTENSION` nem `ALTER SYSTEM`).

URL JDBC para o Render: `jdbc:postgresql://<host-sem-pooler>/sbi?sslmode=require`.

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
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<host-do-neon-sem-pooler>/sbi?sslmode=require` |
| `SPRING_DATASOURCE_USERNAME` | `<usuário do Neon>` |
| `SPRING_DATASOURCE_PASSWORD` | `<senha do Neon>` |
| `SBI_DEMO_PASSWORD` | `<senha das contas demo, 12 a 72 bytes>` |
| `SESSION_COOKIE_SECURE` | `true` |
| `SPRING_JPA_SHOW_SQL` | `false` (o padrão do projeto registra cada consulta no log) |
| `PORT` | `8080` (opcional: `application.properties` usa `server.port=${PORT:8080}`, então a porta que o Render informar também funciona) |
| `JAVA_TOOL_OPTIONS` | `-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k` |

**`SBI_DEMO_PASSWORD` só vale na primeira migração.** A `V6_1__DemoAccounts` grava o BCrypt das contas `matheussiqueira@sbi.tech`, `allanbarros@sbi.tech`, `franciscoferreira@sbi.tech` e `gabrielsousa@sbi.tech` uma única vez, quando o Flyway aplica o perfil `demo` num banco vazio. Trocar a variável depois não muda a senha: para isso, apague e recrie o banco `sbi` no Neon (e rode a carga Olist de novo) ou troque a senha pela tela de usuários. Como o ambiente é público e os e-mails do seed são conhecidos, use uma senha forte e exclusiva.

No primeiro deploy, o log do Render deve mostrar o Flyway aplicando V1 a V11 e as migrações `db/demo`. Conferência: `https://<serviço>.onrender.com/auth/csrf` responde 200 com o token. Cada chamada a `/auth/csrf` abre uma sessão de até 30 minutos (o token CSRF fica na sessão), então o health check mantém algumas centenas de sessões pequenas em memória, o que cabe na instância.

## 3. Carga Olist no Neon

Depois que a API subiu (esquema criado pelo Flyway), carregue a massa por SQL puro com o `psql` do contêiner `postgres:18`, sem docker compose. A URL fica só numa variável de ambiente da sessão:

```powershell
$env:SBI_DATABASE_URL = 'postgresql://<usuário>:<senha>@<host-sem-pooler>/sbi?sslmode=require'
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\demo-olist.ps1 -DatabaseUrl $env:SBI_DATABASE_URL -Check
Remove-Item Env:SBI_DATABASE_URL
```

Em bash: `bash docs/bd/pacote-sql/run_demo_olist.sh --database-url "$SBI_DATABASE_URL" --check`.

O script confere que o esquema existe, roda `07_carga_olist.sql` (com `-Check`, duas vezes, para provar a idempotência) e os testes `09_testes_olist.sql` (CT-DEMO-01 a 04). A URL chega ao `psql` por variável de ambiente do contêiner, fora da linha de comando do `docker run`. O CT-DEMO-05 (máscara de documento pela API) pode ser conferido com `node scripts/demo-olist-api.mjs https://<sbi-app>.vercel.app/api/backend` e `SBI_DEMO_PASSWORD` definida.

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
| `BACKEND_ORIGIN` | `https://<serviço>.onrender.com` (só a origem: sem caminho, sem barra final, sem credenciais) |

O `next.config.mjs` grava o rewrite `/api/backend/*` → `BACKEND_ORIGIN` no `next build`, e os layouts do servidor usam a mesma variável na execução. Se a URL da API mudar, refaça o deploy (novo build), não basta reiniciar.

## 5. Antes da demonstração: aquecer 5 minutos antes

O plano gratuito do Render dorme o serviço depois de um tempo sem acesso, e o Neon suspende o compute ocioso. A primeira requisição depois disso espera a JVM subir (perto de um minuto) e o banco acordar. Cinco minutos antes de apresentar:

1. abra `https://<serviço>.onrender.com/auth/csrf` até responder 200;
2. abra `https://<sbi-app>.vercel.app/entrar`, entre como gerente e visite Visão geral e Clientes (aquece a JVM, o pool de conexões e o banco);
3. mantenha uma aba aberta em Visão geral: o painel consulta a API a cada 15 segundos com a aba visível, o que impede o serviço de dormir durante a apresentação.

Se algo falhar ao vivo, o plano B do [roteiro de demonstração](roteiro-demonstracao.md) usa a stack local (`docker-compose.demo.yml`) e as capturas de `reports/marca/`.
