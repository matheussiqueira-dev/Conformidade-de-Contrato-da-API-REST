# [Matheus] QA-02 · Integração, constraints e rollback real

Atualização: 04/10/2026. Card: https://trello.com/c/lUE8z32P/39-matheus-qa-02-integra%C3%A7%C3%A3o-constraints-e-rollback-real

## Responsabilidades e crédito

- Responsável de continuidade: Matheus; membros nativos anteriores preservados: Matheus Siqueira.
- Revisor previsto no card: Gabriel.
- Atualização, organização e documentação desta rodada: **Matheus Siqueira**, com assistência de ferramentas.
- **Implementador dos avanços técnicos descritos nesta rodada: Matheus Siqueira**, mesmo quando o card está atribuído a outro integrante. Responsabilidade futura/revisão não transfere autoria da implementação. Quando abaixo consta trabalho pendente, não há implementação a creditar desse escopo.

## Objetivo e resultado esperado

[Matheus] QA-02 · Integração, constraints e rollback real

Entrega de referência: src/test/java/; docker-compose.test.yml

## O que foi feito, como e evidência

Matheus entregou testes JPA de total/preço histórico e contexto isolado; suíte Java 12 passou no CI. Constraints/rollback de estoque/autoria futura ainda faltam.

Arquivos de referência: src/test/java/; docker-compose.test.yml.

Os arquivos existentes podem ser consultados na [branch publicada](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/tree/a3-contract-validation). A comprovação técnica atual está no [CI aprovado](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37177644692) e em [relatório de sessão](../../../reports/execucoes/run-2026-10-04-sessao-nextjs.md). O CI tem 12 Java, 23 contrato, 3 cliente, 12 HTTP e 2 E2E. Esses números são verificações executadas em escopos diferentes, não pontuação acadêmica, cobertura percentual ou prova de implementação de todo este card. O relatório de CT-MONEY-002 comprova a correção da quantidade multiplicada uma vez. Não foram implementadas metas, reserva/saída de estoque ou as quatro telas de negócio.

## Estado e motivo

Anterior: Em andamento. Estado definido nesta atualização: **Em andamento**.

Há contribuição concreta parcial; os próximos passos ainda são necessários para cumprir o escopo completo.

## Briefing de continuidade

Matheus/Allan: revisar casos atuais e ampliar migrações e domínio conforme existirem; provar rollback em PostgreSQL real, persistência histórica e FKs. Não marcar todo QA-02 completo pela suíte atual.

### Entradas e dependências

Consultar os arquivos e próximos passos específicos abaixo; decisões de domínio em docs/decisoes/regras-loja.md.

Ler os arquivos citados antes da execução; conferir comportamento existente e registrar decisão ainda incerta como pendência. Reusar os cards de domínio/API/BD/QA correspondentes. Uma entrega futura precisa dos endpoints e dados reais de sua versão; não substituir integração por números fictícios.

### Passos executáveis

1. Preparar (até 2h, estimativa): verificar branch/commit, ler o objetivo, os arquivos e a entrega; confirmar os dados necessários e registrar dependência ausente.
2. Executar uma subentrega do briefing acima (até 4h por tarefa, estimativa). Se não couber, dividir em tarefas com entrada, saída e verificação próprias no checklist do card; não prometer a conclusão inteira nessa janela.
3. Verificar (até 2h por conjunto, estimativa): comparar com critérios de aceite originais abaixo e registrar esperado/obtido, versão, ambiente e comandos. Para documentação, conferir links e consistência com código; para código, executar unitário/integração/contrato pertinente.
4. Revisar (até 1h por rodada, estimativa): publicar arquivos/resultados, solicitar revisão de Gabriel, registrar achados e reteste. Commit novo usa prefixo `autofix:` quando aplicável à correção/sincronização. Capacidade e datas devem ser combinadas com a equipe; estes tempos não são compromisso de sprint.

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

**Responsável principal:** Matheus
**Apoio:** Allan
**Revisor:** Gabriel

## Execução — 03/10/2026
Iniciei integração real do baseline com OrderPersistenceTest: cria produtos sintéticos e pedidos pelo OrderService; flush/clear obriga recarga do banco.
Cenários: preço unitário histórico persiste após mudança do catálogo; venda iPhone4500+AirPods1200 mantém total5700, cliente/endereço/pagamento persistidos.
Como: Spring Boot Test, perfil integration, banco PostgreSQL exclusivo porta15432, transação Spring com rollback e massa OrderFixtures.
Utilizado: Java25, JUnit, AssertJ, Spring/JPA/EntityManager, Maven tags e Compose descartável.

## Evidência
OrderPersistenceTest.java; OrderFixtures.java; application-integration.properties; docker-compose.test.yml; reports/execucoes/run-2026-10-03-continuacao.md.
Código implementado, sem execução aprovada: bloqueios JDK/Docker nesta sessão.

## Pendências
Executar e confirmar persistência/rollback; ainda faltam cenários de constraints, erro parcial, concorrência e domínio estoque. Não afirmar rollback comprovado antes do run. Em andamento.

## Checklist técnico atualizado em 04/10

Itens abaixo marcados tecnicamente executados por Matheus Siqueira, com evidência nos relatórios/CI citados. Revisão humana e demais itens permanecem pendentes.

- [x] Sem resíduos entre casos.
