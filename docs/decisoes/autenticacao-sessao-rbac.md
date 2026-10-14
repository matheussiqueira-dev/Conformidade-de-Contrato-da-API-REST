# Autenticação, sessão e RBAC (ERP/PDV)

Data: 14/10/2026. Etapa: ETAPA 1 (desenho); implementado na ETAPA 2B (`feat/security-error-contract`): `SecurityConfig`, `LockoutAuthenticationProvider`, `LoginAttemptService`, `AuditService`, `AreaResolver`, `GlobalExceptionHandler` e `ApiErrorController`. Testes: `AccessMatrixTest` (CT-SEC-101..103), `ErrorContractTest` (CT-SEC-104, CT-SEC-115), `SessionSecurityTest` (CT-SEC-105), `LoginLockoutPolicyTest` (CT-AUTH-101), `LoginLockoutIntegrationTest` (CT-AUTH-102), `MethodSecurityHierarchyTest` (CT-SEC-103). Complementa [arquitetura-frontend-sessao.md](arquitetura-frontend-sessao.md) (ARQ-01) e a [matriz de acesso](matriz-acesso.md).

## 1. Sessão no servidor, não JWT

A sessão Spring (`HttpSession`) continua sendo o mecanismo de autenticação (ARQ-01). Motivos:

- **Revogação imediata no logout.** `POST /auth/logout` invalida a sessão no servidor; o cookie roubado deixa de valer no mesmo instante. Com JWT, o token continuaria válido até expirar, a menos que se mantivesse uma lista de tokens revogados, que é uma sessão com outro nome.
- **Mesma origem.** O Next.js encaminha `/api/backend/:path*` para `BACKEND_ORIGIN`; o navegador só conversa com uma origem. O cookie `HttpOnly` vai sozinho, e o JavaScript da página nunca guarda credencial. Um token no navegador exigiria `localStorage` (exposto a XSS) ou cookie, o que traz de volta CSRF sem os ganhos da sessão.
- **Já implementado e testado.** Login com `ChangeSessionIdAuthenticationStrategy`, CSRF com `HttpSessionCsrfTokenRepository`, BCrypt(12) e `SessionSecurityTest` estão na `main`.

Configuração mantida ou reforçada:

| Item | Valor |
| --- | --- |
| Cookie | `JSESSIONID`, `HttpOnly`, `SameSite=Lax`, `Secure` fora de dev (`SESSION_COOKIE_SECURE`, padrão `true`), `path=/` |
| Duração | `server.servlet.session.timeout=30m` |
| Fixação de sessão | `ChangeSessionIdAuthenticationStrategy` no login |
| CSRF | Token de `GET /auth/csrf`, enviado em `X-CSRF-TOKEN` em toda mutação, inclusive login e logout; renovado após login (`CsrfAuthenticationStrategy`) e após logout |
| Logout | Invalida a sessão, apaga o cookie, grava `LOGOUT` em `audit_event`, responde 204 |
| Senha | BCrypt(12); senha de 8 a 72 bytes UTF-8 (limite do BCrypt) |

## 2. Hierarquia de papéis

```java
@Bean
static RoleHierarchy roleHierarchy() {
    return RoleHierarchyImpl.fromHierarchy("ROLE_GERENTE > ROLE_VENDEDOR");
}

@Bean
static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
    var handler = new DefaultMethodSecurityExpressionHandler();
    handler.setRoleHierarchy(roleHierarchy);
    return handler;
}
```

- O mesmo bean atende as regras de URL (`authorizeHttpRequests` usa o `RoleHierarchy` publicado no contexto) e o `@PreAuthorize`, com `@EnableMethodSecurity`. Os dois beans são `static` para não antecipar a criação da configuração de segurança.
- GERENTE herda tudo do VENDEDOR; nada do GERENTE alcança o VENDEDOR (RF-17). As regras de URL e de método escrevem só o papel mínimo: `hasRole("VENDEDOR")` para operações de balcão e `hasRole("GERENTE")` para o backoffice.
- `GET /auth/me` devolve `areas`, calculadas no servidor a partir de `roleHierarchy.getReachableGrantedAuthorities(...)`:

| Autoridade alcançável | Áreas |
| --- | --- |
| `ROLE_VENDEDOR` | `VENDER`, `VENDAS`, `CLIENTES`, `MEU_DESEMPENHO` |
| `ROLE_GERENTE` | as do VENDEDOR + `GESTAO`, `PRODUTOS`, `ESTOQUE`, `PROMOCOES`, `ANALISES` |

- Prova exigida (CT-SEC-103): para cada linha da matriz em que o VENDEDOR tem `PERMIT` ou `OWN`, o GERENTE passa na regra de URL **e** no `@PreAuthorize` do caso de uso.

## 3. Três barreiras de autorização

1. **URL**, negação por padrão: cada linha da [matriz](matriz-acesso.md) vira um `requestMatchers(...)`; o restante cai em `anyRequest().denyAll()`. `dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()` deixa o `/error` responder no formato `ErrorResponse`.
2. **Método**: `@PreAuthorize` em cada caso de uso, com o mesmo papel mínimo da URL. Protege contra uma regra de URL esquecida ou um novo controlador que reutilize o serviço.
3. **Dono do dado** no serviço: pedido de venda só é lido, confirmado ou cancelado por `seller_id == usuário da sessão` ou por GERENTE. Objeto alheio responde **404**, sem diferenciar de inexistente. Listas do VENDEDOR são filtradas por `seller_id`.

## 4. Bloqueio por tentativas (RF-18)

`LoginAttemptService`, com `java.time.Clock` injetado, e um `AuthenticationProvider` próprio no lugar do `DaoAuthenticationProvider`:

1. Carrega o usuário pelo e-mail normalizado (minúsculas, sem espaços).
2. **Sempre** executa `passwordEncoder.matches(senha, hash)`: com o hash do usuário, se existir, ou com um hash BCrypt(12) fixo gerado na inicialização, se não existir. É a mesma mitigação de tempo do `DaoAuthenticationProvider` para usuário inexistente.
3. Só **depois** da comparação avalia o estado: inexistente, inativo (`active = false`), bloqueado (`locked_until > agora`) ou senha errada → mesma `BadCredentialsException`.
4. Senha errada em conta ativa e não bloqueada: `failed_login_attempts + 1`. Na 5ª falha seguida: `locked_until = agora + 15 min`, `failed_login_attempts = 0` e `audit_event ACCOUNT_LOCKED`.
5. Sucesso: zera o contador, grava `last_login_at`, `LOGIN_SUCCEEDED`.
6. Tentativas durante o bloqueio, inclusive com a senha correta, respondem 401, gravam `LOGIN_FAILED` e **não** prolongam o bloqueio.

Os quatro casos (senha errada, inexistente, bloqueada, inativa) respondem **401** com o mesmo corpo `ErrorResponse` (`code = UNAUTHENTICATED`, mesma mensagem), exceto `timestamp`. Todo caminho de falha grava um `audit_event LOGIN_FAILED` (o e-mail tentado só como SHA-256 em `detail`), para que o número de escritas não varie com a existência da conta.

## 5. Erros e firewall

- `RequestRejectedHandler` próprio: requisição barrada pelo `StrictHttpFirewall` (`;`, `%2e%2e`, `//`, caracteres de controle) responde **400** `MALFORMED_REQUEST` no formato `ErrorResponse`, nunca 500.
- `AuthenticationEntryPoint` e `AccessDeniedHandler` usam o mesmo formato (`UNAUTHENTICATED` e `FORBIDDEN`). CSRF ausente ou inválido é 403 `FORBIDDEN`.
- `X-HTTP-Method-Override` não é honrado: o `HiddenHttpMethodFilter` fica desligado (padrão do Spring Boot) e nenhum filtro lê esse header. Coberto por CT-SEC-115.
- Variação de caminho (barra final, maiúsculas, `;jsessionid=`) não casa com regra permissiva: o `PathPatternRequestMatcher` compara o caminho exato e o restante cai em `denyAll`.

## 6. Ameaças cobertas

| Ameaça | Controle |
| --- | --- |
| Acesso anônimo a dados | Negação por padrão; 401 |
| Vendedor acessando backoffice | URL + `@PreAuthorize`; 403 |
| Vendedor lendo, pagando ou cancelando venda alheia | Dono no serviço; 404 |
| Vendedor forjando autoria, preço, desconto ou total | DTO sem esses campos; propriedade desconhecida → 400; vendedor = sessão |
| Força bruta numa conta | 5 falhas → 15 min de bloqueio |
| Enumeração de contas pelo login | Corpo, status e custo de BCrypt iguais nos quatro casos |
| Fixação de sessão | Novo ID no login |
| CSRF | Token por sessão em toda mutação; `SameSite=Lax` |
| Roubo de cookie por JavaScript | `HttpOnly` |
| Cookie em texto claro | `Secure` fora de dev |
| Sessão esquecida aberta | Expira em 30 min; logout invalida |
| Clickjacking | `X-Frame-Options: DENY` padrão do Spring Security |
| Truques de caminho e de método | `StrictHttpFirewall` + 400; override de método ignorado |
| Repetição de venda (duplo clique, recarga) | `Idempotency-Key` por vendedor |
| Vazamento de custo e margem ao vendedor | Projeções fechadas (`additionalProperties: false`) auditadas no contrato v4 |

## 7. Ameaças não cobertas (riscos aceitos ou fora do escopo)

| Ameaça | Situação |
| --- | --- |
| Força bruta distribuída (muitas contas, um IP; ou muitos IPs, uma conta) | Sem limitação por IP nem CAPTCHA; o bloqueio é só por conta. |
| Bloqueio proposital de conta alheia (negação de serviço) | Quem sabe o e-mail pode manter a conta bloqueada errando a senha a cada 15 min. Não há desbloqueio manual porque a gestão de usuários está fora do escopo (DEC-ERP-07). |
| Diferença residual de tempo | Contas existentes fazem um `UPDATE` a mais na falha; o BCrypt domina o tempo, mas a diferença não foi medida. |
| Inativação com sessão aberta | Conta inativada direto no banco só é barrada no próximo login; sessões já abertas expiram em até 30 min. Não há `SessionRegistry` nesta etapa. |
| XSS no frontend | Um script injetado pode ler o token CSRF e agir na sessão; a defesa é não renderizar HTML não confiável (React escapa por padrão). |
| Cookie sem `Secure` em dev | `SESSION_COOKIE_SECURE=false` só para HTTP local. |
| Documentação OpenAPI pública | Revela a superfície da API; decisão registrada na [matriz](matriz-acesso.md#documentação-pública). |
| Abuso por GERENTE | Mitigado só por auditoria (`audit_event`, histórico de preço, autor de entradas). |
| RLS na conexão da aplicação | A aplicação usa conexão única; a RLS do `03_rbac.sql` é demonstrada só via `psql` (A4). |

## 8. Frontend

`frontend/proxy.ts` (convenção do Next.js 16 que substitui `middleware`) redireciona para `/entrar` quando não há cookie de sessão nas rotas protegidas; o matcher exclui `/entrar`, `/api/backend`, `_next` e arquivos estáticos. O layout de servidor de cada área chama `/auth/me` repassando o cookie para `BACKEND_ORIGIN` e redireciona quem não tem a área. Essas guardas só melhoram a experiência; a autoridade continua sendo a API (403).
