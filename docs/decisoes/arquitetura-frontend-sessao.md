# ARQ-01 — Next.js e sessão no Spring

Data: 04/10/2026. Estado: escolha de frontend/autenticação confirmada por Matheus nesta sessão; detalhes abaixo são direção técnica para implementação, não entrega concluída.

## Decisão confirmada

Frontend em Next.js. Sessão mantida no Spring, identificada por cookie HttpOnly. A API permanece responsável por autenticação, autorização por perfil e autoria dos pedidos. A continuação adicionou código de autenticação e interface de acesso; aceite e pendências estão em [execucao-sessao.md](../execucao-sessao.md). Autoria de pedidos e áreas de domínio seguem pendentes.

## Direção de integração

- Preferir uma origem pública: navegador chama o backend por caminho encaminhado ao Spring. Avaliar rewrites/proxy de Next.js e comprovar Set-Cookie, Cookie, logout e CSRF no ambiente local e na hospedagem escolhida.
- Cookie de sessão HttpOnly, SameSite=Lax e Secure no ambiente HTTPS; caminho que funcione para o proxy. Não configurar domínio público sem definir a hospedagem. A sessão deve renovar seu identificador no login e ser invalidada no logout.
- Manter proteção CSRF no Spring para métodos que alteram dados. Planejar endpoint para obtenção do token e header no cliente; HttpOnly refere-se ao cookie da sessão e não dispensa o token CSRF acessível ao frontend.
- Login e logout com respostas JSON; 401 para ausência de sessão, 403 para acesso sem permissão. Vendedor deriva da sessão, sem aceitar autoria enviada pelo cliente.
- Credenciais de teste sintéticas e passwords com hash no servidor. Não criar contas produtivas ou guardar passwords no frontend.
- E2E proposto: Playwright; confirmar versão junto ao setup do frontend. Não há decisão sobre hospedagem, timeout de sessão ou armazenamento distribuído.

## Impacto no contrato

A v2 planejada usa Bearer como hipótese anterior e foi preservada. A v3 de 04/10 usa cookie/sessão e documenta CSRF, login, usuário atual e logout. A auditoria verifica sessão/CSRF na v3 e ainda valida a v2 arquivada. As rotas de domínio continuam planejadas.

## Aceite da implementação futura

Login válido estabelece sessão; inválido retorna 401; consulta sem sessão retorna 401; perfil errado retorna 403. Escrita sem token CSRF deve ser rejeitada. Logout invalida sessão. Cookie e token precisam funcionar via Next.js. Testes devem comprovar ausência de custo/margem e acesso a dados de colega para vendedor, além de autoria derivada da sessão.

## Pendências independentes

Desconto, alteração de pedido alheio, custeio, devolução e arredondamento continuam em DOM-01. A escolha arquitetural não aprova automaticamente essas regras nem o escopo acadêmico com professores.

Referências oficiais: [rewrites Next.js](https://nextjs.org/docs/app/api-reference/config/next-config-js/rewrites) e [CSRF Spring Security](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).
