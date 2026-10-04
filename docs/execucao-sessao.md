# API-01 e UI-01: acesso com sessao

Data: 04/10/2026. Codigo escrito; o aceite Java, HTTP e browser depende de execucao dos testes abaixo. O contrato v3 continua marcando as rotas de dominio como planejadas.

## Implementacao desta rodada

Spring Security autentica usuarios persistidos em `app_users`, com hash BCrypt e perfis GERENTE/VENDEDOR. GET `/auth/csrf` inicia uma sessao anonima e retorna token/header; POST `/auth/login` exige esse token, renova o identificador, limpa CSRF e salva explicitamente o contexto; GET `/auth/me` retorna id, nome, email e perfil; POST `/auth/logout` invalida sessao e limpa cookie. Obter novo token depois de login/logout. Sessao expira apos 30 minutos de inatividade.

Cookie JSESSIONID HttpOnly, SameSite=Lax, path=/; Secure por padrao. Para desenvolvimento HTTP, definir `SESSION_COOKIE_SECURE=false`. O perfil `contract` desabilita Secure somente no runner isolado. Nenhuma conta padrao e criada no ambiente normal. As contas sinteticas do perfil contract so iniciam com flag de isolamento, datasource de teste exato, create-drop e senha temporaria fornecida pelo runner.

Frontend em `frontend/`, Next.js 16.3.8 e React 19.3.0: login, recuperacao de sessao ao recarregar e logout. Navegador chama `/api/backend/*`; rewrite encaminha ao Spring. Cookie nao vai para JavaScript/localStorage. `BACKEND_ORIGIN` e configurado no build; nao ha CORS permissivo. Telas de metas, vendas e estoque ainda dependem dos endpoints de dominio.

As rotas legadas de clientes, produtos, pedidos, pagamento e endereco agora exigem gerente. Isso altera o acesso publico anterior, preservando os formatos de sucesso do baseline. Vendedor nao usa esses DTOs, que nao foram desenhados como projecoes restritas. Rotas desconhecidas sao negadas. Swagger permanece publico no ambiente local; restringir exposicao na hospedagem.

Corrigido erro generico que devolvia status HTTP 500 com status 400 no JSON. Incluido teste de regressao. Passwords com mais de 72 bytes UTF-8 sao rejeitados antes do BCrypt.

## Verificacao executavel

Na pasta `order-management-api`, com JDK 25 e Docker Desktop:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test.ps1 -Mode Integration
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-contract.ps1 -Frontend
```

O segundo runner cria senha aleatoria somente em memoria, banco descartavel e API em 18080. Verifica seis casos legados autenticados, seis casos de sessao e dois E2E Chromium via Next na porta 3000; encerra os processos que iniciou. O E2E prova Set-Cookie/Cookie, HttpOnly, recarregamento, logout, erro de senha e largura mobile. Screenshots locais ficam em `frontend/test-results`; nao habilitar traces que possam capturar credenciais.

Para rodar apenas testes Node sem dependencias do frontend: `node --test frontend/tests/*.test.mjs`. A primeira instalacao do frontend gera package-lock; preservar o lock antes de um release. O CI gera e arquiva esse arquivo junto aos resultados E2E.

Para visualizar depois dos testes: `powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-contract.ps1 -Preview`. O runner informa a URL e senha temporaria das contas sinteticas; mantem API/Next/banco ativos ate pressionar Enter, quando encerra os recursos. Nao usa o banco normal da loja.

## Aceite e limites

Aceite: sessao real persiste; ID muda no login; login invalido=401; escrita sem CSRF=403; vendedor nas rotas antigas=403; logout torna cookie anterior inutil; cookie transita pelo proxy do Next. Os testes atuais nao comprovam autoria de pedidos por sessao ou ausencia de margem em projecoes futuras: essas rotas ainda nao existem.

Antes de uso publico: provisionamento de contas pela equipe, limite de tentativas de login, migracao versionada de usuarios, HTTPS, politica de senha e armazenamento de sessao para multiplas instancias. Nao existem endpoints de cadastro publico ou contas produtivas nesta entrega. Reverter a rodada devolve o acesso publico legado: qualquer rollback deve ficar em ambiente isolado.

## Proxima implementacao de dominio

1. Migrar dinheiro para BigDecimal/NUMERIC com HALF_UP e preservar preco historico por item; testar limites e compatibilidade do baseline.
2. Introduzir autoria por sessao, cancelamento e estorno com auditoria/idempotencia; vendedor altera apenas pedido proprio conforme regra a detalhar.
3. Estoque com reserva atomica, custo medio ponderado e conferencia de devolucao; teste concorrente da ultima unidade.
4. Metas e realizado por periodo, projecoes distintas e testes contra acesso a colega/custo/margem.
5. Integrar as quatro areas Next com dados reais; validar teclado, estados e reflow.

Referencias: [persistencia da sessao](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html), [CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html), [rewrites Next](https://nextjs.org/docs/app/api-reference/config/next-config-js/rewrites).
