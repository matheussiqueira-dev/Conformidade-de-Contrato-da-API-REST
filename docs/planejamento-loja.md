# Planejamento Loja Gestao

Data: 04/10/2026. Atualização e implementação técnica desta rodada: Matheus Siqueira.

## Progresso confirmado em 04/10

Baseline e contrato preservados/validados; runners, CI, sessão Spring e painel de acesso Next executados com sucesso. O CI 37177644692 aprovou 52 verificações e build. Pendentes: BigDecimal/NUMERIC, autoria do pedido, estoque, metas, pagamentos consistentes e quatro telas. A tabela histórica de esforço/marcos abaixo é proposta a reestimar, não capacidade confirmada. Distribuição e estado atual de cada card: [índice de gestão](gestao/README.md). QA-08 documenta revisão humana e preview Windows; DOC-01 consolida rubrica/entregáveis.

Este planejamento inicia o card PL-01 e organiza o caminho para as atividades do Matheus. As estimativas sao propostas de trabalho, sujeitas a revisao da equipe e dos professores.

## Linhas de trabalho

| Linha | Objetivo | Estado |
| --- | --- | --- |
| B0 - baseline | Preservar API original e registrar contrato/ambiente. | Entregue para revisão; contrato e HTTP validados. |
| Documentacao de qualidade | Requisitos, validacao, criterios de aceitacao, plano de testes e matriz. | Iniciado nesta revisao. |
| Decisoes de dominio | Descontos, pedidos de terceiros, custo, devolucao, dinheiro e idempotencia. | Iniciado em `docs/decisoes/regras-loja.md`. |
| Evolucao API/BD | Autenticacao, metas, estoque, custo historico e pagamento consistente. | Sessão entregue; demais domínios pendentes de modelo/decimal/autoria. |
| Interface | Gestao, Meu desempenho, Vender e Estoque. | Acesso entregue; telas dependem de APIs de domínio e UX. |
| Evidencias | Testes, analise estatica, defeitos, metricas e relatorio final. | Testes/build executados; análise estática, métricas e relatório final pendentes. |

## Prioridade dos cards do Matheus

| Ordem | Card | Entrega | Situacao sugerida apos esta rodada |
| ---: | --- | --- | --- |
| 1 | 02 - Definicao do sistema e requisitos | `docs/requisitos.md` | Em revisao. |
| 2 | 03 - Validacao dos requisitos | `docs/validacao-requisitos.md` | Em revisao. |
| 3 | 04 - Criterios de aceitacao | `docs/criterios-aceitacao.md` | Em revisao. |
| 4 | PL-01 - Reestimar escopo e capacidade | `docs/planejamento-loja.md` | Em revisao. |
| 5 | DOM-01 - Regras de descontos, terceiros e custos | `docs/decisoes/regras-loja.md` | Em revisao. |
| 6 | 07 - Plano de testes | `docs/plano-testes.md` | Em andamento; rascunho inicial criado com bloqueios de risco/stack destacados. |
| 7 | API-02 - Metas mensais | API/DTOs/metas + testes | Iniciar depois de DOM-01 e API-01. |
| 8 | API-04 - Entradas de estoque e custo | Entrada/lote/custo + testes | Iniciar depois de API-03 e BD. |
| 9 | UI-01 a UI-05 | Frontend e E2E | Iniciar depois de UX-01, ARQ-01 e API minima. |

## Marcos propostos

| Marco | Data alvo interna | Criterio de saida |
| --- | --- | --- |
| M0 - alinhamento | 04/10/2026 | Professores/equipe: grupo, BD, prazo e responsabilidades registrados ou pendentes explicitas. |
| M1 - base documental e contrato | 11/10/2026 | Requisitos, validacao, criterios, stack, contrato baseline e plano de qualidade revisados. |
| M2 - arquitetura executavel minima | 18/10/2026 | API sobe com banco, contrato exportado, autenticacao definida ou bloqueio reproduzivel. |
| M3 - dominio minimo | 01/11/2026 | Metas, estoque/reserva, custo e pagamento consistente com testes principais. |
| M4 - interface integrada | 15/11/2026 | Fluxos Gestao, Meu desempenho, Vender e Estoque exercitados por perfil. |
| M5 - evidencias finais | 29/11/2026 | Catalogo, execucoes, defeitos, metricas, relatorio e demo ensaiada. |
| Entrega | 30/11/2026 | Pacote final submetido no canal docente, salvo mudanca formal de prazo. |

## Estimativa inicial por frente do Matheus

| Frente | Esforco inicial | Observacao |
| --- | ---: | --- |
| Requisitos, validacao e criterios | 6 h | Parte iniciada nesta rodada; falta revisao cruzada. |
| Planejamento e regras de dominio | 5 h | Depende de decisao da equipe para fechar. |
| Plano de testes | 4 h | Depende de risco, stack e contrato. |
| Metas mensais/API | 10 h | Inclui modelo, DTOs, servicos e testes. |
| Estoque/custo/API | 14 h | Maior risco por transacao, concorrencia e historico. |
| Base visual e telas | 22 h | Pode variar muito conforme stack e escopo minimo. |
| Revisoes, evidencias e ajustes | 9 h | Contempla rastreabilidade e retestes. |

Total inicial da carga Matheus: 70 h. Esta estimativa deve ser comparada com a disponibilidade real e com apoio de Gabriel/Allan/Francisco.

## Dependencias principais

| Dependencia | Necessaria para |
| --- | --- |
| ARQ-01 - stack e integracao | UI-01 a UI-05, E2E e CORS. |
| API-01 - autenticacao/autorizacao | RF de perfil, Meu desempenho, Estoque e seguranca. |
| API-03 - estoque reserva/saida/cancelamento | API-04, UI-04 e UI-05. |
| UX-01 - guia visual publicado | UI-01 a UI-05. |
| Risco e catalogo inicial | Plano de testes e priorizacao dos cenarios. |
| Alinhamento docente | Escopo final, BD e janela de entrega. |

## Politica para mover cards

| Movimento | Regra |
| --- | --- |
| Backlog -> A fazer | Dependencias principais aceitas e card pronto para execucao. |
| A fazer -> Em andamento | Trabalho iniciado com entrega concreta no repositorio. |
| Em andamento -> Em revisao | Artefato entregue e pronto para revisor designado. |
| Em revisao -> Concluido | Revisor aprovou ou pendencias foram resolvidas com evidencia. |

Cards de implementacao dependentes devem permanecer no Backlog ate existir stack/API minima. Mover tudo para Em andamento agora reduziria a visibilidade do caminho critico.
