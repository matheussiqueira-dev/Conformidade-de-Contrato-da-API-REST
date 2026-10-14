# Decisões da evolução (G-01 a G-06)

Data: 14/10/2026. Registro das decisões tomadas na evolução de autenticação e sessão, cadastro de usuários, resumo do cliente, validação de documento, desconto com alçada e endurecimento da sessão. Uma linha por decisão; o motivo fica na própria linha.

## Lacunas tratadas

| ID | Lacuna | Pacote |
| --- | --- | --- |
| G-01 | Desativar usuário ou trocar papel não derruba as sessões abertas | WP-EVO-2 |
| G-02 | Contador de falhas de login gravado por leitura-modificação-gravação, perdendo incremento em tentativas paralelas | WP-EVO-1 |
| G-03 | Não há cadastro de usuários, desbloqueio manual nem troca de senha | WP-EVO-2 |
| G-04 | Desconto manual com alçada por papel | WP-EVO-5 |
| G-05 | Resumo do cliente (indicadores) para a tela de perfil | WP-EVO-3 |
| G-06 | A documentação citava Testcontainers, mas a integração usa PostgreSQL descartável do Docker Compose | WP-EVO-7 |

## Decisões

| ID | Decisão |
| --- | --- |
| D-EVO-01 | Os pacotes formam uma pilha linear (WP-EVO-1 → 2 → … → 7): todos mexem no mesmo contrato v5, na sequência de migrações, na matriz de acesso e neste arquivo. Cada PR aponta para a `main` e diz de qual PR depende; enquanto o anterior não entra, o diff inclui o dele. |
| D-EVO-02 | G-02: o contador continua zerando quando a conta é bloqueada (comportamento do CT-AUTH-101). O UPDATE atômico só incrementa se a conta não estiver bloqueada no instante do `Clock`, então tentativas durante o bloqueio não o prolongam e o bloqueio dispara uma única vez. O CT-SEC-120 verifica isso: 10 falhas paralelas resultam em conta bloqueada, 1 `ACCOUNT_LOCKED` e 10 `LOGIN_FAILED`. |
| D-EVO-03 | G-02: o login de sucesso também grava por UPDATE só das colunas de contador, bloqueio e último acesso, em vez de salvar a entidade inteira. Assim um login simultâneo não desfaz uma troca de papel ou uma desativação feita por outra requisição. |
| D-EVO-04 | G-01: o `SessionVersionFilter` confere a conta em toda requisição autenticada que tenha sessão HTTP; a API não tem outro mecanismo de login (sem token, sem Basic, sem "lembrar-me"). O `user()` do MockMvc também grava o contexto numa sessão, então os testes que usam principal sintético (`AccessMatrixTest`, `ErrorContractTest`) criam as duas contas no banco com os mesmos ids (`support/SyntheticAccounts`). |
| D-EVO-05 | Política de senha: máximo de 72 caracteres e 72 bytes UTF-8, no lugar dos 128 do plano, porque o BCrypt usa só os primeiros 72 bytes (o login já limita a 72). O mínimo de 12 vale para senha nova; o login continua aceitando 8, para não trancar contas existentes. |
| D-EVO-06 | O GERENTE não redefine a própria senha por `/users/{id}/password-reset` (409 `SELF_CHANGE_FORBIDDEN`): sem a senha atual, uma sessão roubada tomaria a conta. A própria senha muda em `PUT /auth/password`. A redefinição também desbloqueia a conta. |
| D-EVO-07 | Senha atual errada em `PUT /auth/password` conta para o bloqueio, como no login, e o serviço devolve vazio em vez de lançar exceção, para o incremento ser gravado. Se essa falha bloquear a conta, as sessões do usuário são encerradas (quem tenta adivinhar a senha atual numa sessão aberta perde a sessão). O bloqueio pelo login não encerra sessões, para que uma pessoa de fora não derrube a sessão de outra. |
| D-EVO-08 | O último GERENTE ativo é protegido só pelo gatilho `keep_one_active_manager`, sem pré-verificação duplicada no serviço. O serviço roda `SET CONSTRAINTS ... IMMEDIATE` para o 409 `LAST_MANAGER` sair na própria requisição, e o gatilho usa trava consultiva para serializar a verificação. Pela API isso só acontece em corrida (dois gerentes se rebaixando ao mesmo tempo), coberta pelo CT-USR-103. |
| D-EVO-09 | Fluxo do contrato v5: operação `planned` fica fora da comparação estrutural ao vivo e da conferência com a matriz, porque ainda não tem rota; a linha da matriz entra no mesmo commit da rota. Todo o v4 é `implemented`, então nada afrouxa para o que já existia. O relatório ao vivo passa a `reports/contrato/live-v5/`. |
| D-EVO-10 | Áreas: o GERENTE ganha USUARIOS (menu Usuários); com senha provisória, `areas` traz só TROCAR_SENHA e o frontend leva para `/conta/senha`, que fica fora do grupo das áreas para não entrar em laço de redirecionamento. |
| D-EVO-11 | As leituras para alteração de usuário travam a linha com `FOR NO KEY UPDATE`: o `audit_event` é gravado em transação própria e sua FK para `app_users` pede trava de chave compartilhada, que `FOR UPDATE` bloquearia (impasse entre a transação externa e a da auditoria). |
| D-EVO-12 | Conforme o plano: editar nome e e-mail não encerra sessões; desbloqueio não pede motivo; não existe DELETE de usuário (desativar substitui apagar). O 403 por CSRF continua com `code` `FORBIDDEN` (o plano citava `CSRF_INVALID`, que não existe no contrato). |
| D-EVO-13 | DEC-ERP-07 ("gestão de usuários fora do escopo; contas do seed") deixa de valer: o cadastro de usuários entra no contrato v5 (RF-24). O primeiro GERENTE continua vindo do seed demo ou da configuração; não há autocadastro. |
