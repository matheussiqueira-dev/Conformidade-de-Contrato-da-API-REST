# Validacao dos requisitos

Data: 03/10/2026.

Objetivo: eliminar ambiguidades antes de transformar requisitos em contrato, testes e implementacao. Esta validacao foi feita por leitura do backend local, README, direcao criativa, guia visual e revisao do Trello. Nao houve execucao HTTP ou validacao de banco em runtime nesta etapa.

## Resultado da revisao

| Tema | Decisao ou encaminhamento | Estado |
| --- | --- | --- |
| Frontend e autenticacao | Entram no escopo planejado da evolucao Loja Gestao, sem substituir as evidencias de qualidade da API. | Validado como alvo. |
| Baseline original | Deve ser preservado como referencia B0 antes das ampliacoes. | Validado. |
| Dados demonstrativos | Podem orientar fixtures, mas nao sao resultado medido. | Validado. |
| Vendedor do pedido | Deve vir da sessao autenticada, nunca de campo livre enviado pelo navegador. | Validado como regra. |
| Custo de compra | Visivel apenas para gerente e ausente tambem no JSON do vendedor. | Validado como regra. |
| Realizado | Considera pagamento confirmado menos estornos; pendente nao conta. | Validado como regra alvo. |
| Meta zero | Exibir "Sem meta", sem divisao por zero. | Validado como regra alvo. |
| Estoque | Disponivel = saldo fisico menos reservado; ultima unidade exige concorrencia controlada. | Validado como regra alvo. |
| Preco do item | Deve ser preco unitario historico; quantidade multiplica uma vez. | Validado como regra alvo e risco do baseline. |

## Ambiguidades resolvidas

| ID | Ambiguidade | Resolucao inicial |
| --- | --- | --- |
| AMB-01 | A interface substitui a API? | Nao. A interface e uma evolucao do SUT, mas autorizacao, dados e calculos permanecem no servidor. |
| AMB-02 | Vendedor pode ver resultado da loja? | Nao no fluxo de Meu desempenho. Vendedor ve apenas sua meta, realizado e vendas. |
| AMB-03 | Historico compartilhado revela custo/margem? | Nao. Historico de atendimento pode revelar produtos, datas e estado do pedido, sem custo, margem ou resultado de colegas. |
| AMB-04 | Pagamento pendente entra em meta realizada? | Nao. Somente pagamento confirmado, descontando estornos. |
| AMB-05 | Mockups sao telas prontas? | Nao. Sao referencia visual; a implementacao deve calcular dados e estados. |

## Decisoes ainda pendentes

| ID | Pergunta | Responsavel sugerido | Bloqueia |
| --- | --- | --- | --- |
| PEN-01 | Somente gerente concede desconto, confirmado em 04/10. Limite numerico e auditoria detalhada ainda pendentes. | Matheus + Gabriel | RF-07, RF-15, criterios de aceitacao. |
| PEN-02 | Somente gerente cancela pedido alheio, confirmado em 04/10. Edicao de pedido alheio ainda pendente. | Matheus + Allan | RF-06, RF-13, seguranca. |
| PEN-03 | Custo medio ponderado confirmado em 04/10. | Matheus + Gabriel | Implementar RF-14, BD, relatorio financeiro. |
| PEN-04 | Conferencia fisica obrigatoria antes de repor estoque, confirmada em 04/10. | Matheus + Allan | Implementar RF-08, RF-10, RF-13, RF-14. |
| PEN-05 | Dinheiro com 2 casas e HALF_UP confirmado em 04/10. | Matheus + Gabriel | Migracao monetaria de RF-07, RNF-03. |
| PEN-06 | Next.js + sessao HttpOnly: codigo de acesso, testes e runner E2E adicionados. Aceite depende de execucao Java/HTTP/browser. | Gabriel + Matheus | Ver `docs/execucao-sessao.md`; integrar UI-02 a UI-05 depois do dominio. |
| PEN-07 | Professores aceitam grupo de quatro, escopo ampliado e prazo interno? | Matheus | Planejamento e entrega final. |

## Validacao por requisito

| Requisito | Verificacao feita | Situacao |
| --- | --- | --- |
| RF-01 Autenticacao | Spring Security, usuarios persistidos, sessao e CSRF adicionados. | Codigo escrito; validar Java/HTTP/E2E da rodada. |
| RF-02 Autorizacao | Rotas legadas exigem gerente; usuario atual aceita ambos os perfis. | Parcial; projecoes dedicadas de vendedor pendentes. |
| RF-03 Clientes | CRUD e subtipos existem. | Requisito observado; precisa testes e contrato. |
| RF-04 Produtos | CRUD e subtipos existem. | Requisito observado; estoque/custo sao extensoes. |
| RF-05 Pedidos | Criacao com cliente novo/existente existe. | Requisito observado; validar total e erros. |
| RF-06 Vendedor por sessao | Identidade autenticada disponivel; autoria ainda nao integrada ao pedido. | Pendente de dominio. |
| RF-07 Calculo monetario | Risco estatico identificado em preco/quantidade. | Requer teste de reproducao antes de declarar defeito executado. |
| RF-08 Pagamento | Processamento simulado existe. | Parcial; falta estorno/idempotencia. |
| RF-09 Metas | Nao existe dominio de metas. | Alvo confirmado. |
| RF-10 Realizado | Nao existe realizado por vendedor. | Alvo confirmado. |
| RF-11 Meu desempenho | Nao existe endpoint por sessao. | Alvo confirmado. |
| RF-12 Gestao | Nao existe painel/API consolidada. | Alvo confirmado. |
| RF-13 Estoque | Produto nao possui saldo/reserva. | Alvo confirmado. |
| RF-14 Custos | Produto possui preco, mas nao custo por lote. | Alvo confirmado. |
| RF-15 Vender | Pedido existe na API; fluxo de interface nao existe. | Parcial. |
| RF-16 Interface | Next.js com login, recuperacao de sessao e logout adicionados. | Parcial; quatro areas de dominio pendentes. |

## Riscos de validacao

| Risco | Tratamento |
| --- | --- |
| Confundir proposta com entrega implementada. | Todos os docs usam status Observado/Alvo/Parcial/Pendente. |
| Encerrar tarefa sem revisao cruzada. | Cards devem ir para Em revisao, nao Concluido, apos estes documentos. |
| Declarar bug sem execucao. | Risco de total duplicado fica como hipotese estatica ate teste HTTP ou unitario reproduzir. |
| Deixar decisao de dominio implicita. | `docs/decisoes/regras-loja.md` registra decisoes propostas e pendentes. |
