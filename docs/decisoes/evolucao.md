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
