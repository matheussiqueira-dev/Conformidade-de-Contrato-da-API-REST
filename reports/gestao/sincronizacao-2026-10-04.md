# Sincronização GitHub e Trello — 04/10/2026

Executada por **Matheus Siqueira**. Quadro: https://trello.com/b/ombHh0vo

## Resultado conferido no Trello

60 cards existentes tiveram descrições revisadas; 4 novos criados; 28 movidos entre listas. Na conferência final, 26 itens técnicos de checklists existentes permaneceram completos conforme artefatos/testes; o item que exigia relatórios Surefire/Failsafe foi desmarcado porque Failsafe ainda não está configurado. Quatro checklists novas têm 32 itens pendentes. Antes/depois da sincronização inicial estão preservados em JSON; a correção posterior do item Failsafe está registrada neste relatório. A releitura dos 64 cards confirmou descrição/coluna esperadas sem divergências; quatro novos cards possuem o membro nativo correto.

| Lista | Quantidade |
| --- | ---: |
| Referências | 4 |
| Backlog | 17 |
| A fazer | 16 |
| Em andamento | 10 |
| Em revisão | 15 |
| Concluído | 2 |

Crédito de implementação técnica desta rodada: Matheus Siqueira, inclusive em cards originalmente de outro responsável. Continuidade/revisão e membro original preservados. Trabalho futuro não é apresentado como implementação de Matheus. DoisConcluído encontrados foram preservados como estado humano anterior; confirmação docente ainda carece de artefato no Git.

## Novos cards e distribuição

- [DOM-02: [Gabriel] DOM-02 · Migrar dinheiro para BigDecimal e NUMERIC](https://trello.com/c/HxWdN6Fv/61-gabriel-dom-02-migrar-dinheiro-para-bigdecimal-e-numeric) — membro nativo Gabriel, revisão Allan; complexidade estimada5 pontos. Briefing: [arquivo](../../docs/gestao/cards/DOM-02.md).
- [SEC-01: [Matheus] SEC-01 · Vincular autoria do pedido à sessão](https://trello.com/c/uBQH1dMy/62-matheus-sec-01-vincular-autoria-do-pedido-%C3%A0-sess%C3%A3o) — membro nativo Matheus, revisão Gabriel; complexidade estimada5 pontos. Briefing: [arquivo](../../docs/gestao/cards/SEC-01.md).
- [QA-08: [Allan] QA-08 · Revisar sessão e reproduzir preview no Windows](https://trello.com/c/PQ5Aikvp/63-allan-qa-08-revisar-sess%C3%A3o-e-reproduzir-preview-no-windows) — membro nativo Allan, revisão Matheus; complexidade estimada3 pontos. Briefing: [arquivo](../../docs/gestao/cards/QA-08.md).
- [DOC-01: [Francisco] DOC-01 · Consolidar rubrica, rastreabilidade e autoria](https://trello.com/c/qT7awxOR/64-francisco-doc-01-consolidar-rubrica-rastreabilidade-e-autoria) — membro nativo Francisco, revisão Allan; complexidade estimada3 pontos. Briefing: [arquivo](../../docs/gestao/cards/DOC-01.md).

Os briefings têm arquivos, entradas, dependências, tarefas de até 4 h, cenários Dado/Quando/Então de sucesso/erro/limite, verificação, recuperação e definição de pronto. Sem prazo novo inventado ou sprint comprometida. Demais escopos já possuem cards e não foram duplicados.

## GitHub e documentos

Branch de revisão `a3-contract-validation`, repositório https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST. Base publicada antes desta rodada:`4f72c8bfbfd9014b4bfa19519133d2892e58803b`, tree`ee2899027d9cfb7298b2364cfc9197ab2aff7c40`. Nenhum merge em main nesta sincronização. Commit novo prefixado`autofix:`; histórico anterior preservado. Autoria dos commits já existentes não foi reescrita.

64 registros por card e índice, planejamento/requisitos/regras corrigidos e documentos de referência publicados. PDFs originais copiados sem alteração (comparar hashes Git no manifesto): enunciado, plano original, manual e guia visual. Fonte Markdown do manual e direção criativa também publicadas. [Índice da equipe](../../docs/gestao/README.md).

A referência histórica pode conter decisão antiga de FIFO ou frontend fora do escopo. Regra atual aprovada por Matheus: Next/sessãoHttpOnly; gerente no desconto/cancelamento alheio; dinheiro com 2 casas e HALF_UP; custo médio ponderado; reposição após conferência física. A aprovação define o alvo; implementação de domínio permanece pendente.

## Evidência e limite de revisão

Código da sessão e UI já passou no [CI 37177644692](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37177644692), com 52 verificações e build. Esta rodada altera documentação/gestão; não amplia a comprovação para metas/estoque/telas futuras. Preview Windows, reflow a 200%, aceite dos integrantes e rubrica final estão nos novos cards. Não há deploy público ou envio aos professores.

O conector Trello permite descrições de até2.048 caracteres; o detalhamento completo está no Git e ligado em cada card. Atribuições nativas dos novos cards realizadas pela interface do Trello e relidas no conector. Não foram trocados os membros dos cards anteriores.

## Itens técnicos comprovados e pendentes

- [x] Card aGnCboeK: Criar workflow no push e pull request; mínimo funcional desde S1.
- [x] Card aGnCboeK: Configurar JDK compatível e Maven Wrapper; registrar versões do build.
- [ ] Card aGnCboeK: Publicar relatórios Surefire/Failsafe e evidências também quando houver falhas. Surefire entregue; Failsafe pendente. Item desmarcado no Trello em 04/10.
- [x] Card aGnCboeK: Reproduzir comandos de API/BD/frontend e suíte/E2E em ambiente limpo; conferir CI.
- [x] Card aGnCboeK: Isolar PostgreSQL, fixtures e portas entre casos; registrar política de limpeza.
- [x] Card aGnCboeK: Documentar comandos, pré-requisitos e localização dos relatórios no README.
- [x] Card ige94AvZ: Não autenticado rejeitado
- [x] Card ige94AvZ: vendedor não acessa colega/custos
- [x] Card ige94AvZ: gerente autorizado
- [x] Card ige94AvZ: testes HTTP positivos/negativos.
- [x] Card vhUnNujA: ADR com versões e justificativa
- [x] Card vhUnNujA: integração mínima real
- [x] Card vhUnNujA: execução de API/BD/UI documentada.
- [x] Card MT77ib4r: Organizar src/main, src/test, docs, reports e config conforme Maven e documentar no README.
- [x] Card MT77ib4r: Executar a API e verificar Swagger UI e /v3/api-docs; registrar versões e falhas reais.
- [x] Card MT77ib4r: Exportar OpenAPI em config/ com commit, gerador e hash; vincular ao card de baseline.
- [x] Card MT77ib4r: Preservar B0 antes de alterações; separar B1 executável e documentar ajustes mínimos.
- [x] Card UCbFIFXr: Resultado real registrado.
- [x] Card UCbFIFXr: Defeito confirmado corrigido ou risco justificado.
- [x] Card UCbFIFXr: Reteste e regressão se houver correção.
- [x] Card a9wQuRO0: Enviar requests inválidos até a API, sem bloqueio pelo validador de request; validar resposta 400.
- [x] Card a9wQuRO0: Verificar recurso inexistente e resposta 404 conforme contrato aprovado.
- [x] Card a9wQuRO0: Provocar 500 de forma controlada em teste; conferir schema e ausência de stack trace na resposta.
- [x] Card a9wQuRO0: Validar respostas de sucesso e erro contra contrato revisado, congelado e identificado por versão.
- [x] Card a9wQuRO0: Conferir coerência entre status HTTP e campo status do corpo, além de timestamp/error/message/details.
- [x] Card lUE8z32P: Sem resíduos entre casos.
- [x] Card jf7xU6cz: pendências bloqueiam apenas a implementação dependente.

Failsafe e guard contra suíte vazia não foram marcados como concluídos: integração atual usa perfil Surefire. JaCoCo/estática, autoria na sessão, estoque concorrente e revisão nominal continuam pendentes. Não se confunde32 tarefas de checklist com testes executados.
