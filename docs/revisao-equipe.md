# Pacote de revisão da equipe

Data: 14/10/2026. Para: Gabriel, Allan e Francisco (Matheus revisa junto as áreas indicadas).

O objetivo é que cada integrante consiga revisar, rodar e aceitar a sua área sem depender de quem implementou. A revisão é sobre a `main` atual: todo o trabalho desde 13/10 já foi integrado por squash merge, um PR por etapa.

## 1. O que mudou desde 13/10

| Módulo | O que entrou | PR |
| --- | --- | --- |
| Banco | Migrações V5 (papéis, clientes PF/PJ, estoque com reserva, promoções, auditoria) e V6 (views de faturamento, metas e alertas), seed demo V6_1/V6_2 no perfil `demo`, Flyway como única fonte do esquema, V7 (cartão só com os 4 últimos dígitos) | [#13](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/pull/13), [#15](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/pull/15) |
| Segurança | Hierarquia GERENTE > VENDEDOR em URL e método, dono do dado com 404, bloqueio após 5 falhas por 15 min, `ErrorResponse` com `code`, matriz de acesso executável | [#14](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/pull/14) |
| Endpoints e contrato | As 45 operações do contrato v4 implementadas e verificadas ao vivo (PDV, clientes, backoffice, estoque, promoções, gestão, metas, análises) | [#15](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/pull/15) |
| Frontend | Next.js em TypeScript com schemas Zod gerados do contrato; telas Vender, Vendas, Clientes, Meu desempenho e Gestão (visão geral, estoque, produtos, promoções, análises); axe WCAG 2.2 AA | [#16](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/pull/16) |
| Testes e CI | CI com quatro jobs (`test (backend)`, `bd`, `contract`, `frontend`), testes de lacuna, métricas R1 e rastreabilidade requisito → teste → evidência | [#17](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/pull/17) |
| Entrega | Demonstração em um comando, README, arquitetura, roteiro de demonstração, relatório final | [#18](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/pull/18) |
| Registro | Registro da integração e exceções da varredura de termos | [#19](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/pull/19), [#20](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/pull/20) |

## 2. Como rodar

Requisitos: Docker Desktop, JDK 25 em `JAVA_HOME`, Node 20.9 ou mais novo. Comandos no PowerShell, na raiz do repositório.

| O quê | Comando | Resultado esperado (14/10) |
| --- | --- | --- |
| Demonstração publicada | Abrir <https://sbi-app-alpha.vercel.app> (aquecer antes: [deploy.md](deploy.md), seção 6) | Contas dos integrantes combinadas com a equipe, fora do repositório; roteiro em [roteiro-demonstracao.md](roteiro-demonstracao.md) |
| Demonstração | `$env:SBI_DEMO_PASSWORD='<senha>'; $env:SBI_DB_PASSWORD=[guid]::NewGuid().ToString('N'); docker compose -f docker-compose.demo.yml -p sbi-demo up --build -d --wait` e abrir <http://localhost:3000> | Contas no [README](../README.md); roteiro em [roteiro-demonstracao.md](roteiro-demonstracao.md) |
| Testes Java rápidos | `.\mvnw.cmd -q clean verify` | 233 testes, 0 falhas |
| Integração completa | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test.ps1 -Mode Integration` | 544 testes, 0 falhas |
| Contrato ao vivo | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test-contract.ps1 -HttpOnly` | 45/45 operações, `divergences: []` |
| Validadores Node | `npm test` | 36/36 |
| Frontend | `cd frontend; npm test; npm run build` | 20/20 e build ok |
| E2E com axe | `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test-contract.ps1 -HttpOnly -Frontend` | 14/14 |
| Banco por SQL | job `bd` da CI (`gh run list --branch main --limit 1`) | 51 PASS |

## 3. Quem revisa o quê

| Área | Revisores | Onde olhar |
| --- | --- | --- |
| Banco e SQL | Matheus, Gabriel | `src/main/resources/db/migration/V5*`–`V7*`, `db/demo/`, `docs/bd/modelo-erp-pdv.md`, `docs/bd/pacote-sql/` |
| Segurança | Allan | `config/SecurityConfig.java`, `security/*`, `src/test/resources/security/access-matrix.csv`, `docs/decisoes/autenticacao-sessao-rbac.md`, `docs/decisoes/matriz-acesso.md` |
| Endpoints e contrato | Gabriel, Francisco | `api/*`, `config/openapi/target-loja-gestao-openapi-2026-10-14-v4.json`, `reports/contrato/live-v4/live-contract.json`, `docs/decisoes/erp-pdv.md` |
| Frontend | Francisco, Matheus | `frontend/app`, `frontend/lib/api`, `frontend/e2e`, `docs/decisoes/arquitetura-frontend-sessao.md` |

## 4. Checklist de aceite por área

**Banco e SQL**
- [ ] Banco vazio + Flyway sobe V1 a V7 sem erro, e o Hibernate valida (`ddl-auto=validate`).
- [ ] Nenhum CHECK permite estoque negativo ou `reserved > on_hand`.
- [ ] Custo médio ponderado é calculado só no gatilho `stock_entry_apply`.
- [ ] O seed demo reproduz os números do Guia Visual (R$ 186.400,00 em setembro, 65 pedidos pagos).
- [ ] Job `bd` com 51 PASS na última execução da `main`.

**Segurança**
- [ ] Toda rota mapeada está na `access-matrix.csv` e o `AccessMatrixTest` passa.
- [ ] Mutação sem token CSRF responde 403 `CSRF_INVALID`.
- [ ] Cinco senhas erradas bloqueiam a conta por 15 min, com a mesma resposta para conta inexistente, senha errada, conta bloqueada e inativa.
- [ ] Pedido de outro vendedor responde 404 para o VENDEDOR.
- [ ] O VENDEDOR nunca recebe custo, margem, realizado de colegas nem documento completo do cliente.

**Endpoints e contrato**
- [ ] As 45 operações do v4 têm `x-status: implemented` e teste citado no plano de testes.
- [ ] O contrato ao vivo não tem divergência.
- [ ] Propriedade desconhecida no corpo responde 400; entrada inválida nunca responde 500.
- [ ] Preço, desconto e total são calculados só no servidor (HALF_UP, `Money {amount, currency}`).

**Frontend**
- [ ] O menu muda conforme o papel, e as telas do gerente mostram "Sem acesso a esta área" para o vendedor.
- [ ] Venda completa pelo teclado (busca com `/`, Enter, `+`, Finalizar).
- [ ] axe sem violação WCAG 2.2 AA e sem rolagem horizontal em 320 px.
- [ ] Erro de rede nunca aparece como zero nos indicadores.

## 5. Decisões que precisam de voto da equipe

| Decisão | Opções | Proposta atual | Onde está |
| --- | --- | --- | --- |
| Metas de qualidade | Aprovar, ajustar ou rejeitar MQ-01 a MQ-07 | Pisos de R1 e "nenhuma violação nova de prioridade alta" | [decisoes/metas-qualidade.md](decisoes/metas-qualidade.md) |
| Teto de desconto (DEC-01 / PEND-DOM-01) | Sem teto; teto único; teto por papel | Hoje só o gerente cria promoções, sem teto numérico | [decisoes/regras-loja.md](decisoes/regras-loja.md) |
| Arredondamento HALF_UP × HALF_EVEN | Manter HALF_UP (confirmado em 04/10); trocar para HALF_EVEN | Manter HALF_UP; a troca exige testes próprios em `Pricing` e no banco | [decisoes/integracao-fase0.md](decisoes/integracao-fase0.md#pendente) |
| Branch `a3-contract-validation` | Manter como histórico; apagar | Manter até o voto: tem 5 commits fora da `main`, todos já reaplicados | [decisoes/integracao-fase0.md](decisoes/integracao-fase0.md) |
