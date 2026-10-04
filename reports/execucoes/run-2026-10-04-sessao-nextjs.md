# Sessao Spring e acesso Next.js — 04/10/2026

Codigo verificado: commit remoto `fa321ae172be00252b017e85e068997d7d544323`, [CI 37177302914](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37177302914), job `111362493235`, concluido com sucesso. Branch `a3-contract-validation`; nenhuma integracao em main nesta rodada.

## Resultado

| Verificacao | Resultado |
| --- | --- |
| Java/JPA/contexto/sessao/regressao de erro | 12 testes, zero falhas ou erros |
| Contrato/politicas/JSON Schema | 23 testes aprovados; alvo v3 com 41 locais de schema e baseline com 71, sem findings |
| Cliente frontend | 3 testes aprovados |
| HTTP legado com sessao de gerente | 6 casos aprovados |
| HTTP real de sessao | 6 casos aprovados |
| Next.js 16.3.8/React 19.3.0 | Build aprovado |
| Chromium via Next/proxy/Spring | 2 E2E aprovados |

Os E2E comprovam cookie HttpOnly/SameSite, login, persistencia apos reload, logout e erro de senha. HTTP verifica renovacao do ID, CSRF ausente rejeitado, cookie anterior invalidado e vendedor impedido de ler DTOs legados. As contas e o banco sao sinteticos/descartaveis; nenhuma conta produtiva foi provisionada.

## QA visual

Capturas do CI copiadas sem alteracao e abertas para inspeção: [desktop](../contrato/session-2026-10-04/session-desktop.png), [mobile](../contrato/session-2026-10-04/login-mobile.png). Conferidos hierarquia, contraste visual, campos rotulados, botao de saida e alerta de senha. O teste mobile em 390 px comprova ausencia de overflow horizontal; scroll vertical permanece natural. Isso nao substitui uma auditoria completa de acessibilidade ou teste de reflow em 200%.

## Correcoes validadas

- Erro generico agora combina HTTP 500 com status 500 no corpo.
- Contexto MVC carrega explicitamente Web Security.
- Alerta E2E fica limitado ao formulario, evitando colisao com anunciador do Next.
- BCrypt recebe apenas passwords dentro do limite de 72 bytes UTF-8.
- Auditoria rejeita alternativa anonima em security e ausencia de CSRF nas escritas do alvo v3.

Package-lock do frontend recuperado do mesmo build aprovado e versionado; CI passa a usar npm ci. Essa mudanca preserva as versoes que acabaram de passar. Relatorios HTTP completos em `reports/contrato/session-2026-10-04/`.

## Pendencias

API-01 entregue para acesso/sessao; RF-06 ainda exige autoria no pedido. UI-01 entregue para acesso; Gestao, Meu desempenho, Vender e Estoque dependem dos endpoints de dominio. Dinheiro decimal, custo medio, desconto/cancelamento, reserva concorrente, estorno, devolucao e metas continuam pendentes. As regras aprovadas por Matheus foram registradas em `docs/decisoes/regras-loja.md`.

O sandbox local bloqueou Maven em java.security e npm em rede. A comprovacao Java/build/browser veio do runner remoto; Node e validacao estrutural tambem passaram localmente. Sem deploy publico nesta rodada.
