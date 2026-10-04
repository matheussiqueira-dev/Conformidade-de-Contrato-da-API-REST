# Gestão do projeto e registros por card

Atualizado em 04/10/2026 por **Matheus Siqueira**. Quadro: https://trello.com/b/ombHh0vo

## O que já foi entregue

Matheus implementou correção do preço unitário do pedido, validação OpenAPI 3.1/2020-12, contrato de erros, runners isolados e CI, autenticação Spring com sessão/CSRF/perfis e interface Next de acesso. A primeira fatia de autoria do pedido também está em andamento. Evidência: [sessão](../execucao-sessao.md), [relatório e capturas](../../reports/execucoes/run-2026-10-04-sessao-nextjs.md), [CI da autoria aprovado](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37198467104). Resultado atual: 15 Java + 23 contrato + 3 cliente + 12 HTTP + 2 E2E = 55 verificações; build Next aprovado. Essa soma não é catálogo acadêmico ou cobertura percentual.

Publicação na branch `a3-contract-validation`; não foi feito merge em main. Revisão dos integrantes continua necessária. Dinheiro decimal, migração e acesso por vendedor, metas, estoque, pagamentos consistentes e quatro telas de negócio permanecem futuros.

## Como usar os registros

Cada card tem objetivo, responsável, revisor, crédito, entrega, evidência, estado/motivo, dependências, passos e critérios de aceite. O histórico anterior foi preservado no final do arquivo e pode conter informações superadas. Usar primeiro a atualização atual. Descrições do Trello têm limite do conector de2.048 caracteres; os briefings completos são mantidos aqui, com link no card.

Matheus Siqueira recebe crédito pela implementação técnica desta rodada, inclusive nos cards de outros responsáveis. Quem permanece atribuído revisará ou continuará trabalho; isso não transfere autoria. Novos cards: criação/planejamento de Matheus, implementação futura do responsável indicado. Estados Concluído anteriores preservados como informação humana do quadro; falta de ata docente é pendência documental explícita.

## Distribuição e ordem de execução

| Integrante | Nova tarefa | Continuidade |
| --- | --- | --- |
| Gabriel Sousa | DOM-02: BigDecimal/NUMERIC | Revisar sessão/ADR/CI; estoque e pagamento depois das dependências. |
| Matheus Siqueira | SEC-01: autoria derivada da sessão | Modelo/migrações, metas/custo e interface conforme APIs prontas. |
| Allan Gabriel Almeida Barros | QA-08: revisão do acesso e Windows | Risco, catálogo, defeitos, concorrência e evidências. |
| Francisco (FR4NCISCO OFC) | DOC-01: rubrica/rastreabilidade/crédito | Plano de qualidade, guias, relatórios e apoio à revisão. |

Sequência: QA-08 e DOC-01 podem avançar com os artefatos atuais. DOM-02 depende do desenho/migração BD-02; SEC-01 depende da FK e política de dados antigos. Depois: reserva/entrada/pagamento→metas→quatro telas→aceitação completa. Revisor nominal valida antes de Concluído. Estimativas nos briefings não significam capacidade/sprint confirmada; datas históricas do Trello foram preservadas para reestimativa, não renovadas arbitrariamente.

## Índice dos 64 cards

| Card | Responsável de continuidade | Estado | Registro detalhado |
| --- | --- | --- | --- |
| [LINKS IMPORTANTES](https://trello.com/c/3MmXle4G/44-links-importantes) | Matheus | Referências | [Briefing](cards/3MmXle4G.md) |
| [[Matheus] Escopo do projeto e SUT](https://trello.com/c/XBAVHAk6/1-matheus-escopo-do-projeto-e-sut) | Matheus | Referências | [Briefing](cards/XBAVHAk6.md) |
| [[Francisco] Critérios de avaliação (100 pts)](https://trello.com/c/JRbuRtZg/2-francisco-crit%C3%A9rios-de-avalia%C3%A7%C3%A3o-100-pts) | Francisco | Referências | [Briefing](cards/JRbuRtZg.md) |
| [[Gabriel] Stack técnica proposta](https://trello.com/c/X9UQOOgN/3-gabriel-stack-t%C3%A9cnica-proposta) | Gabriel | Referências | [Briefing](cards/X9UQOOgN.md) |
| [[Francisco] 05 · Plano de Qualidade de Software](https://trello.com/c/LSD2pMwn/8-francisco-05-plano-de-qualidade-de-software) | Francisco | A fazer | [Briefing](cards/LSD2pMwn.md) |
| [[Allan] 06 · Análise de risco das funcionalidades](https://trello.com/c/a5gxnQvi/9-allan-06-an%C3%A1lise-de-risco-das-funcionalidades) | Allan | A fazer | [Briefing](cards/a5gxnQvi.md) |
| [[Allan] 08 · Revisão técnica formal (inspeção)](https://trello.com/c/MI9ATjxD/11-allan-08-revis%C3%A3o-t%C3%A9cnica-formal-inspe%C3%A7%C3%A3o) | Allan | A fazer | [Briefing](cards/MI9ATjxD.md) |
| [[Allan] 09 · Análise estática (antes × depois)](https://trello.com/c/VXXKixWS/12-allan-09-an%C3%A1lise-est%C3%A1tica-antes-%C3%97-depois) | Allan | A fazer | [Briefing](cards/VXXKixWS.md) |
| [[Allan] 10 · Catálogo de casos de teste (≥30)](https://trello.com/c/Z8XcjCam/13-allan-10-cat%C3%A1logo-de-casos-de-teste-%E2%89%A530) | Allan | A fazer | [Briefing](cards/Z8XcjCam.md) |
| [[Gabriel] 11 · Testes caixa-preta de conformidade de contrato](https://trello.com/c/a9wQuRO0/14-gabriel-11-testes-caixa-preta-de-conformidade-de-contrato) | Gabriel | Em andamento | [Briefing](cards/a9wQuRO0.md) |
| [[Gabriel] 12 · Testes caixa-branca e cobertura (JaCoCo)](https://trello.com/c/9EKvdzHs/15-gabriel-12-testes-caixa-branca-e-cobertura-jacoco) | Gabriel | A fazer | [Briefing](cards/9EKvdzHs.md) |
| [[Gabriel] 13 · Níveis de teste: unitário, integração, sistema, aceitação](https://trello.com/c/CmRstOih/16-gabriel-13-n%C3%ADveis-de-teste-unit%C3%A1rio-integra%C3%A7%C3%A3o-sistema-aceita%C3%A7%C3%A3o) | Gabriel | Em andamento | [Briefing](cards/CmRstOih.md) |
| [[Matheus] 14 · Banco de Dados: modelo, integridade e persistência](https://trello.com/c/I4BYlFWv/17-matheus-14-banco-de-dados-modelo-integridade-e-persist%C3%AAncia) | Matheus | Em andamento | [Briefing](cards/I4BYlFWv.md) |
| [[Allan] 16 · Gestão de defeitos](https://trello.com/c/rXZu5aql/19-allan-16-gest%C3%A3o-de-defeitos) | Allan | Em andamento | [Briefing](cards/rXZu5aql.md) |
| [[Matheus] 17 · Regressão e ciclo de melhoria contínua](https://trello.com/c/cYHvKIHo/20-matheus-17-regress%C3%A3o-e-ciclo-de-melhoria-cont%C3%ADnua) | Matheus | Em andamento | [Briefing](cards/cYHvKIHo.md) |
| [[Matheus] 18 · Métricas e avaliação dos atributos de qualidade](https://trello.com/c/zIsf4wjO/21-matheus-18-m%C3%A9tricas-e-avalia%C3%A7%C3%A3o-dos-atributos-de-qualidade) | Matheus | A fazer | [Briefing](cards/zIsf4wjO.md) |
| [[Francisco] 19 · Segurança, privacidade e ética](https://trello.com/c/VQvMapdo/22-francisco-19-seguran%C3%A7a-privacidade-e-%C3%A9tica) | Francisco | A fazer | [Briefing](cards/VQvMapdo.md) |
| [[Francisco] 20 · Relatórios e relatório técnico final](https://trello.com/c/tWeVAYyZ/23-francisco-20-relat%C3%B3rios-e-relat%C3%B3rio-t%C3%A9cnico-final) | Francisco | A fazer | [Briefing](cards/tWeVAYyZ.md) |
| [[Gabriel] 21 · Apresentação e demonstração ao vivo](https://trello.com/c/PJe5BNT8/24-gabriel-21-apresenta%C3%A7%C3%A3o-e-demonstra%C3%A7%C3%A3o-ao-vivo) | Gabriel | Backlog | [Briefing](cards/PJe5BNT8.md) |
| [[Matheus] Templates padronizados (caso de teste, defeito, relatório)](https://trello.com/c/9DYSzP9j/29-matheus-templates-padronizados-caso-de-teste-defeito-relat%C3%B3rio) | Matheus | A fazer | [Briefing](cards/9DYSzP9j.md) |
| [[Francisco] Slides da apresentação](https://trello.com/c/qQvIlw5q/32-francisco-slides-da-apresenta%C3%A7%C3%A3o) | Francisco | Backlog | [Briefing](cards/qQvIlw5q.md) |
| [[Francisco] Plano B da demo (gravação de contingência)](https://trello.com/c/JTx8I4eB/33-francisco-plano-b-da-demo-grava%C3%A7%C3%A3o-de-conting%C3%AAncia) | Francisco | Backlog | [Briefing](cards/JTx8I4eB.md) |
| [[Matheus] Checklist de entrega final (conferência dos 14 critérios)](https://trello.com/c/1mNWyLTq/34-matheus-checklist-de-entrega-final-confer%C3%AAncia-dos-14-crit%C3%A9rios) | Matheus | Backlog | [Briefing](cards/1mNWyLTq.md) |
| [[Matheus] BD-01 · DER, dicionário e análise de normalização](https://trello.com/c/YPqTTPbz/35-matheus-bd-01-der-dicion%C3%A1rio-e-an%C3%A1lise-de-normaliza%C3%A7%C3%A3o) | Matheus | A fazer | [Briefing](cards/YPqTTPbz.md) |
| [[Matheus] BD-02 · Migrações Flyway e integridade no PostgreSQL](https://trello.com/c/FwvkkAdG/36-matheus-bd-02-migra%C3%A7%C3%B5es-flyway-e-integridade-no-postgresql) | Matheus | A fazer | [Briefing](cards/FwvkkAdG.md) |
| [[Allan] BD-03 · Consultas SQL, views e análise de índices](https://trello.com/c/9luJhlJy/37-allan-bd-03-consultas-sql-views-e-an%C3%A1lise-de-%C3%ADndices) | Allan | Backlog | [Briefing](cards/9luJhlJy.md) |
| [[Gabriel] QA-01 · Testes unitários das regras de negócio](https://trello.com/c/eQ7WLyPR/38-gabriel-qa-01-testes-unit%C3%A1rios-das-regras-de-neg%C3%B3cio) | Gabriel | Em andamento | [Briefing](cards/eQ7WLyPR.md) |
| [[Allan] QA-03 · Testes de sistema HTTP e aceitação](https://trello.com/c/pY0i7PQQ/40-allan-qa-03-testes-de-sistema-http-e-aceita%C3%A7%C3%A3o) | Allan | Em andamento | [Briefing](cards/pY0i7PQQ.md) |
| [[Allan] QA-05 · Medição de desempenho RNF-03](https://trello.com/c/Ob9F07Rn/42-allan-qa-05-medi%C3%A7%C3%A3o-de-desempenho-rnf-03) | Allan | Backlog | [Briefing](cards/Ob9F07Rn.md) |
| [[Gabriel] QA-06 · Geração de testes de contrato com Schemathesis](https://trello.com/c/71ixgS6n/43-gabriel-qa-06-gera%C3%A7%C3%A3o-de-testes-de-contrato-com-schemathesis) | Gabriel | Backlog | [Briefing](cards/71ixgS6n.md) |
| [[Gabriel] API-01 · Autenticação e autorização por gerente/vendedor](https://trello.com/c/ige94AvZ/49-gabriel-api-01-autentica%C3%A7%C3%A3o-e-autoriza%C3%A7%C3%A3o-por-gerente-vendedor) | Gabriel | Em revisão | [Briefing](cards/ige94AvZ.md) |
| [[Matheus] API-02 · Metas mensais, distribuição e realizado](https://trello.com/c/9OrNNT1y/50-matheus-api-02-metas-mensais-distribui%C3%A7%C3%A3o-e-realizado) | Matheus | Backlog | [Briefing](cards/9OrNNT1y.md) |
| [[Gabriel] API-03 · Estoque: reserva, saída e cancelamento atômicos](https://trello.com/c/nEt4hNMG/51-gabriel-api-03-estoque-reserva-sa%C3%ADda-e-cancelamento-at%C3%B4micos) | Gabriel | Backlog | [Briefing](cards/nEt4hNMG.md) |
| [[Matheus] API-04 · Entradas de estoque e custo histórico do gerente](https://trello.com/c/Bm4Z8HVX/52-matheus-api-04-entradas-de-estoque-e-custo-hist%C3%B3rico-do-gerente) | Matheus | Backlog | [Briefing](cards/Bm4Z8HVX.md) |
| [[Gabriel] API-05 · Histórico do cliente e atendimento compartilhado](https://trello.com/c/FrFkwHnJ/53-gabriel-api-05-hist%C3%B3rico-do-cliente-e-atendimento-compartilhado) | Gabriel | Backlog | [Briefing](cards/FrFkwHnJ.md) |
| [[Gabriel] API-06 · Confirmar pagamento, estorno e efeitos consistentes](https://trello.com/c/MUxkT9NS/54-gabriel-api-06-confirmar-pagamento-estorno-e-efeitos-consistentes) | Gabriel | Backlog | [Briefing](cards/MUxkT9NS.md) |
| [[Matheus] UI-01 · Implementar base visual e navegação por perfil](https://trello.com/c/JHujvpfp/55-matheus-ui-01-implementar-base-visual-e-navega%C3%A7%C3%A3o-por-perfil) | Matheus | Em andamento | [Briefing](cards/JHujvpfp.md) |
| [[Matheus] UI-02 · Gestão: loja, equipe, metas e alerta de estoque](https://trello.com/c/1nef0V1n/56-matheus-ui-02-gest%C3%A3o-loja-equipe-metas-e-alerta-de-estoque) | Matheus | Backlog | [Briefing](cards/1nef0V1n.md) |
| [[Matheus] UI-03 · Meu desempenho: meta e vendas do próprio vendedor](https://trello.com/c/vSFg74kp/57-matheus-ui-03-meu-desempenho-meta-e-vendas-do-pr%C3%B3prio-vendedor) | Matheus | Backlog | [Briefing](cards/vSFg74kp.md) |
| [[Matheus] UI-04 · Vender: Cliente → Produtos → Pagamento](https://trello.com/c/MlqruKBN/58-matheus-ui-04-vender-cliente-%E2%86%92-produtos-%E2%86%92-pagamento) | Matheus | Backlog | [Briefing](cards/MlqruKBN.md) |
| [[Matheus] UI-05 · Estoque administrativo: saldo, reserva, custo e preço](https://trello.com/c/wfIF8udO/59-matheus-ui-05-estoque-administrativo-saldo-reserva-custo-e-pre%C3%A7o) | Matheus | Backlog | [Briefing](cards/wfIF8udO.md) |
| [[Allan] QA-07 · Aceitação das quatro telas e regressão por perfil](https://trello.com/c/vctQv8R5/60-allan-qa-07-aceita%C3%A7%C3%A3o-das-quatro-telas-e-regress%C3%A3o-por-perfil) | Allan | Backlog | [Briefing](cards/vctQv8R5.md) |
| [[Francisco] UX-01 · Fonte visual, quatro áreas e estados da interface](https://trello.com/c/5uQbmzmm/46-francisco-ux-01-fonte-visual-quatro-%C3%A1reas-e-estados-da-interface) | Francisco | Em andamento | [Briefing](cards/5uQbmzmm.md) |
| [[Gabriel] ARQ-01 · Decisão de stack e integração frontend/API](https://trello.com/c/vhUnNujA/47-gabriel-arq-01-decis%C3%A3o-de-stack-e-integra%C3%A7%C3%A3o-frontend-api) | Gabriel | Em revisão | [Briefing](cards/vhUnNujA.md) |
| [[Gabriel] 15 · Automação da suíte e pipeline de CI](https://trello.com/c/aGnCboeK/18-gabriel-15-automa%C3%A7%C3%A3o-da-su%C3%ADte-e-pipeline-de-ci) | Gabriel | Em revisão | [Briefing](cards/aGnCboeK.md) |
| [[Gabriel] Spike: escolher ferramenta de contract testing](https://trello.com/c/7WLb5mTB/28-gabriel-spike-escolher-ferramenta-de-contract-testing) | Gabriel | Em revisão | [Briefing](cards/7WLb5mTB.md) |
| [[Matheus] Ambiente reprodutível + seed de dados](https://trello.com/c/YBJ5YBeV/30-matheus-ambiente-reprodut%C3%ADvel-seed-de-dados) | Matheus | Em revisão | [Briefing](cards/YBJ5YBeV.md) |
| [[Gabriel] README + execução em um comando](https://trello.com/c/TKUhRGgt/31-gabriel-readme-execu%C3%A7%C3%A3o-em-um-comando) | Gabriel | Em revisão | [Briefing](cards/TKUhRGgt.md) |
| [[Matheus] QA-02 · Integração, constraints e rollback real](https://trello.com/c/lUE8z32P/39-matheus-qa-02-integra%C3%A7%C3%A3o-constraints-e-rollback-real) | Matheus | Em andamento | [Briefing](cards/lUE8z32P.md) |
| [[Gabriel] Extrair e versionar o contrato OpenAPI (baseline)](https://trello.com/c/haebO3FA/27-gabriel-extrair-e-versionar-o-contrato-openapi-baseline) | Gabriel | Em revisão | [Briefing](cards/haebO3FA.md) |
| [[Gabriel] 01 · Setup do repositório e ambiente](https://trello.com/c/MT77ib4r/4-gabriel-01-setup-do-reposit%C3%B3rio-e-ambiente) | Gabriel | Em revisão | [Briefing](cards/MT77ib4r.md) |
| [[Allan] QA-04 · Reproduzir e tratar cálculo de total do pedido](https://trello.com/c/UCbFIFXr/41-allan-qa-04-reproduzir-e-tratar-c%C3%A1lculo-de-total-do-pedido) | Allan | Em revisão | [Briefing](cards/UCbFIFXr.md) |
| [[Matheus] 02 · Definição do sistema e requisitos (≥10 RF, ≥5 RNF)](https://trello.com/c/FlbPAXNE/5-matheus-02-defini%C3%A7%C3%A3o-do-sistema-e-requisitos-%E2%89%A510-rf-%E2%89%A55-rnf) | Matheus | Em revisão | [Briefing](cards/FlbPAXNE.md) |
| [[Matheus] 03 · Validação dos requisitos](https://trello.com/c/rfWqWzVR/6-matheus-03-valida%C3%A7%C3%A3o-dos-requisitos) | Matheus | Em revisão | [Briefing](cards/rfWqWzVR.md) |
| [[Matheus] 04 · Critérios de aceitação por requisito](https://trello.com/c/NsbhF1uK/7-matheus-04-crit%C3%A9rios-de-aceita%C3%A7%C3%A3o-por-requisito) | Matheus | Em revisão | [Briefing](cards/NsbhF1uK.md) |
| [[Matheus] PL-01 · Reestimar escopo, marcos e capacidade](https://trello.com/c/T3zkhwHk/45-matheus-pl-01-reestimar-escopo-marcos-e-capacidade) | Matheus | Em revisão | [Briefing](cards/T3zkhwHk.md) |
| [[Matheus] DOM-01 · Decidir descontos, pedidos de terceiros e custos](https://trello.com/c/jf7xU6cz/48-matheus-dom-01-decidir-descontos-pedidos-de-terceiros-e-custos) | Matheus | Em revisão | [Briefing](cards/jf7xU6cz.md) |
| [[Matheus] 07 · Plano de testes](https://trello.com/c/1ZC4pKWK/10-matheus-07-plano-de-testes) | Matheus | Em revisão | [Briefing](cards/1ZC4pKWK.md) |
| [[Matheus] 00 · Divisão de responsabilidades e acordo de trabalho](https://trello.com/c/KKPfn6ES/26-matheus-00-divis%C3%A3o-de-responsabilidades-e-acordo-de-trabalho) | Matheus | Concluído | [Briefing](cards/KKPfn6ES.md) |
| [[Matheus] 00 · Alinhamento com os professores (BLOQUEADOR)](https://trello.com/c/3lLw8iW9/25-matheus-00-alinhamento-com-os-professores-bloqueador) | Matheus | Concluído | [Briefing](cards/3lLw8iW9.md) |
| [[Gabriel] DOM-02 · Migrar dinheiro para BigDecimal e NUMERIC](https://trello.com/c/HxWdN6Fv/61-gabriel-dom-02-migrar-dinheiro-para-bigdecimal-e-numeric) | Gabriel | A fazer | [Briefing](cards/DOM-02.md) |
| [[Matheus] SEC-01 · Vincular autoria do pedido à sessão](https://trello.com/c/uBQH1dMy/62-matheus-sec-01-vincular-autoria-do-pedido-%C3%A0-sess%C3%A3o) | Matheus | Em andamento | [Briefing](cards/SEC-01.md) |
| [[Allan] QA-08 · Revisar sessão e reproduzir preview no Windows](https://trello.com/c/PQ5Aikvp/63-allan-qa-08-revisar-sess%C3%A3o-e-reproduzir-preview-no-windows) | Allan | A fazer | [Briefing](cards/QA-08.md) |
| [[Francisco] DOC-01 · Consolidar rubrica, rastreabilidade e autoria](https://trello.com/c/qT7awxOR/64-francisco-doc-01-consolidar-rubrica-rastreabilidade-e-autoria) | Francisco | A fazer | [Briefing](cards/DOC-01.md) |

## Referências publicadas

- [Relatório da sincronização](../../reports/gestao/sincronizacao-2026-10-04.md), snapshots antes/depois e [comprovante visual do card](../../reports/gestao/trello-doc-01-2026-10-04.png).
- [Revisão histórica de 01/10](../../reports/gestao/historico/revisao-2026-10-01/relatorio.md), preservada com snapshots; suas afirmações de estado descrevem aquela data.

- [Enunciado acadêmico](../referencias/Projeto-A3.pdf).
- [Plano original recebido](../referencias/Plano-A3-original.pdf).
- [Manual original](../referencias/Manual-Projeto-A3.pdf) e [fonteMarkdown do manual](../referencias/Manual-Projeto-A3.md).
- [Guia visual](../referencias/Guia-Visual-Sistema-da-Loja.pdf) e [direção criativa](../referencias/direcao-criativa.md).

Manual/plano originais são referências históricas e podem excluir frontend/auth ou recomendar FIFO. Decisões atuais em [regras](../decisoes/regras-loja.md) e [arquitetura](../decisoes/arquitetura-frontend-sessao.md) prevalecem: Next/sessão, dinheiro com 2 casas e HALF_UP, custo médio e conferência física aprovados pelo usuário; não são aprovação docente ou implementação do domínio.

## Comandos e limites

Executar na raiz Git `order-management-api`. Pré-requisitos: JDK 25, DockerDesktop, Node compatível e portas de teste livres.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test.ps1 -Mode Integration
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-contract.ps1 -Frontend
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-contract.ps1 -Preview
```

Preview usa conta sintética/senha temporária e limpa recursos próprios ao Enter. Execução interativa Windows e reflow a 200% ficam em QA-08. Não publicar credenciais nem aplicar create-drop ao banco normal. Não há deploy público desta rodada.
