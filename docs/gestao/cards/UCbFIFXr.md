# [Allan] QA-04 · Reproduzir e tratar cálculo de total do pedido

Atualização: 04/10/2026. Card: https://trello.com/c/UCbFIFXr/41-allan-qa-04-reproduzir-e-tratar-c%C3%A1lculo-de-total-do-pedido

## Responsabilidades e crédito

- Responsável de continuidade: Allan; membros nativos anteriores preservados: Allan Gabriel Almeida Barros.
- Revisor previsto no card: Matheus.
- Atualização, organização e documentação desta rodada: **Matheus Siqueira**, com assistência de ferramentas.
- **Implementador dos avanços técnicos descritos nesta rodada: Matheus Siqueira**, mesmo quando o card está atribuído a outro integrante. Responsabilidade futura/revisão não transfere autoria da implementação. Quando abaixo consta trabalho pendente, não há implementação a creditar desse escopo.

## Objetivo e resultado esperado

[Allan] QA-04 · Reproduzir e tratar cálculo de total do pedido

Entrega de referência: reports/execucoes/run-2026-10-03-ct-money-002.md; src/main/java/com/swee/ordermanagementspring/services/OrderService.java

## O que foi feito, como e evidência

Matheus reproduziu quantidade 2×preço100 com item erroneamente200; corrigiu buildItems para guardar100 unitário, subtotal200. Regressões de preço histórico/quantidade passaram.

Arquivos de referência: reports/execucoes/run-2026-10-03-ct-money-002.md; src/main/java/com/swee/ordermanagementspring/services/OrderService.java.

Os arquivos existentes podem ser consultados na [branch publicada](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/tree/a3-contract-validation). A comprovação técnica atual está no [CI aprovado](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37177644692) e em [relatório de sessão](../../../reports/execucoes/run-2026-10-04-sessao-nextjs.md). O CI tem 12 Java, 23 contrato, 3 cliente, 12 HTTP e 2 E2E. Esses números são verificações executadas em escopos diferentes, não pontuação acadêmica, cobertura percentual ou prova de implementação de todo este card. O relatório de CT-MONEY-002 comprova a correção da quantidade multiplicada uma vez. Não foram implementadas metas, reserva/saída de estoque ou as quatro telas de negócio.

## Estado e motivo

Anterior: Em revisão. Estado definido nesta atualização: **Em revisão**.

Artefato técnico entregue e testado dentro do escopo descrito. Falta revisão nominal da equipe; não existe aceite humano presumido.

## Briefing de continuidade

Allan: revisar CT-MONEY-002 e histórico de persistência. DOM-02 tratará decimal separado; não confundir correção da multiplicação com migração Double→BigDecimal.

### Entradas e dependências

Consultar os arquivos e próximos passos específicos abaixo; decisões de domínio em docs/decisoes/regras-loja.md.

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
**Apoio:** Gabriel corrige se confirmado; Matheus valida regra monetária
**Revisor:** Matheus

## Execução — 03/10/2026
Rodada anterior: confirmou multiplicação duplicada (100 × qtd 2 gravado como preço 200), corrigiu OrderService.buildItems para preço unitário e publicou ca79f02. Reteste anterior: 2 testes Java aprovados.
Continuação: ampliei CT-MONEY-001 (4500+1200=5700); teste de preço histórico em memória; dois testes PostgreSQL cobrem recarga após catálogo mudar 100→150 mantendo item 100/total200 e persistência da venda demonstrativa.
Como: OrderFixtures compartilhada; JUnit/Mockito para serviço; Spring Boot Test + JPA/EntityManager.flush/clear para recarga real; transação com rollback.
Utilizado: Java25, JUnit, Mockito, AssertJ, Spring/JPA, PostgreSQL de teste isolado.

## Evidências
OrderServiceTest.java e OrderPersistenceTest.java; reports/execucoes/run-2026-10-03-ct-money-002.md e run-2026-10-03-continuacao.md.
Novos testes implementados, ainda sem aprovação: JVM bloqueada em java.security e Docker indisponível. Resultado anterior não representa suíte ampliada.

## Pendências
Executar CT-MONEY-001/003 e regressão completa; revisar resultados. Mantido Em revisão; não marcar concluído sem evidências novas.

## Checklist técnico atualizado em 04/10

Itens abaixo marcados tecnicamente executados por Matheus Siqueira, com evidência nos relatórios/CI citados. Revisão humana e demais itens permanecem pendentes.

- [x] Resultado real registrado.
- [x] Defeito confirmado corrigido ou risco justificado.
- [x] Reteste e regressão se houver correção.
