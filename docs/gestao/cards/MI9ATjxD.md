# [Allan] 08 · Revisão técnica formal (inspeção)

Atualização: 04/10/2026. Card: https://trello.com/c/MI9ATjxD/11-allan-08-revis%C3%A3o-t%C3%A9cnica-formal-inspe%C3%A7%C3%A3o

## Responsabilidades e crédito

- Responsável de continuidade: Allan; membros nativos anteriores preservados: Allan Gabriel Almeida Barros.
- Revisor previsto no card: Matheus.
- Atualização, organização e documentação desta rodada: **Matheus Siqueira**, com assistência de ferramentas.
- **Implementador dos avanços técnicos descritos nesta rodada: Matheus Siqueira**, mesmo quando o card está atribuído a outro integrante. Responsabilidade futura/revisão não transfere autoria da implementação. Quando abaixo consta trabalho pendente, não há implementação a creditar desse escopo.

## Objetivo e resultado esperado

Realizar inspeção formal de requisitos, arquitetura, código, casos e documentação.

Entrega de referência: docs/atas/inspecao.md, checklist preenchido e links para achados.

## O que foi feito, como e evidência

Código de sessão e interface de acesso estão disponíveis para inspeção formal; aprovação de equipe não foi registrada nesta execução.

Arquivos de referência: docs/decisoes/arquitetura-frontend-sessao.md; src/main/java/com/swee/ordermanagementspring/config/SecurityConfig.java.

Os arquivos existentes podem ser consultados na [branch publicada](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/tree/a3-contract-validation). A comprovação técnica atual está no [CI aprovado](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37177644692) e em [relatório de sessão](../../../reports/execucoes/run-2026-10-04-sessao-nextjs.md). O CI tem 12 Java, 23 contrato, 3 cliente, 12 HTTP e 2 E2E. Esses números são verificações executadas em escopos diferentes, não pontuação acadêmica, cobertura percentual ou prova de implementação de todo este card. O relatório de CT-MONEY-002 comprova a correção da quantidade multiplicada uma vez. Não foram implementadas metas, reserva/saída de estoque ou as quatro telas de negócio.

## Estado e motivo

Anterior: Backlog. Estado definido nesta atualização: **A fazer**.

Tarefa preparada para continuidade documental/técnica. Não foi iniciado código deste escopo só por mudar a coluna.

## Briefing de continuidade

Allan: reunir requisitos, arquitetura, código, casos e docs; usar QA-08 para revisão da sessão, registrar participantes/achados em ata e retestar correções. Não confundir inspeção automática com reunião humana.

### Entradas e dependências

Rascunhos de requisitos, contrato e catálogo.

Fontes: enunciado A3 + direcao-criativa.md/Guia Visual (01/10). Novas funções são planejadas. Datas anteriores: reestimar em PL-01. Baseline e detalhes anteriores preservados no registro local desta revisão.

Ler os arquivos citados antes da execução; conferir comportamento existente e registrar decisão ainda incerta como pendência. Reusar os cards de domínio/API/BD/QA correspondentes. Uma entrega futura precisa dos endpoints e dados reais de sua versão; não substituir integração por números fictícios.

### Passos executáveis

1. Preparar (até 2h, estimativa): verificar branch/commit, ler o objetivo, os arquivos e a entrega; confirmar os dados necessários e registrar dependência ausente.
2. Executar uma subentrega do briefing acima (até 4h por tarefa, estimativa). Se não couber, dividir em tarefas com entrada, saída e verificação próprias no checklist do card; não prometer a conclusão inteira nessa janela.
3. Verificar (até 2h por conjunto, estimativa): comparar com critérios de aceite originais abaixo e registrar esperado/obtido, versão, ambiente e comandos. Para documentação, conferir links e consistência com código; para código, executar unitário/integração/contrato pertinente.
4. Revisar (até 1h por rodada, estimativa): publicar arquivos/resultados, solicitar revisão de Matheus, registrar achados e reteste. Commit novo usa prefixo `autofix:` quando aplicável à correção/sincronização. Capacidade e datas devem ser combinadas com a equipe; estes tempos não são compromisso de sprint.

### Critérios de aceite complementares

- Dado o artefato correto e as entradas válidas, quando o responsável executar a subentrega, então o resultado específico do briefing deve corresponder ao esperado, com arquivo e versão localizáveis.
- Dado erro, entrada inválida ou dependência ausente, quando executar, então a falha/pendência fica registrada; não se inventa resultado, nem se fecha o card sem evidência.
- Dado limite, versão divergente ou funcionalidade futura, quando avaliar, então o relatório separa o que foi executado do que permanece planejado. Para código de acesso usar perfis e massa sintética; para documentos conferir links quebrados/duplicações.

### Definição de pronto e verificação

- [ ] Entrega integral e critérios originais atendidos, com rastreabilidade.
- [ ] Evidência específica anexada, sanitizada e identificada por commit/data/ambiente.
- [ ] Documentação e contrato coerentes; links abrem a versão correta.
- [ ] Revisor nominal aprovou ou registrou pendências e reteste.
- [ ] Mover para Concluído apenas com aceite; execução parcial permanece visível.

Comandos de código, quando pertinentes, na raiz Git `order-management-api`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test.ps1 -Mode Integration
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test-contract.ps1 -Frontend
```

O banco de teste é descartável; nunca aplicar create-drop no banco normal. Relatórios não incluem senha, token, cookie ou dados reais. O preview Windows depende de QA-08; CI Linux não prova a execução interativa no Windows.

## Histórico preservado — descrição anterior

O texto abaixo registra o planejamento/contexto anterior e não substitui o estado de 04/10 acima. Frases antigas sobre inexistência de autenticação, ferramenta bloqueada ou regra proposta devem ser interpretadas pela atualização atual.

**Responsável principal:** Allan
**Apoio:** Gabriel como autor técnico; Matheus revisor; Francisco como escriba
**Revisor:** Matheus
**Referência anterior (provisória):** 2026-10-18

## Objetivo
Realizar inspeção formal de requisitos, arquitetura, código, casos e documentação.

## Execução — direção de 01/10/2026
Inspecionar matriz de permissões, DTOs por perfil, vendedor derivado da sessão, transações de estoque, cálculos de metas e quatro telas. Conferir carregamento/erro/vazio, teclado e mensagens. O código local ainda não contém essas funcionalidades; registrar ausências como trabalho planejado, sem inventar bugs executados.

## Entrega
docs/atas/inspecao.md, checklist preenchido e links para achados.

## Aceite
- [ ] Cinco tipos de artefato revisados.
- [ ] Ata e achados verificáveis.
- [ ] Sem registrar hipótese como bug reproduzido.
- [ ] Ampliação acima contemplada com evidência e revisão.

## Dependências
Rascunhos de requisitos, contrato e catálogo.

Fontes: enunciado A3 + direcao-criativa.md/Guia Visual (01/10). Novas funções são planejadas. Datas anteriores: reestimar em PL-01. Baseline e detalhes anteriores preservados no registro local desta revisão.
