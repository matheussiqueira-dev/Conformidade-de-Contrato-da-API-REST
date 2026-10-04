# [Gabriel] 01 · Setup do repositório e ambiente

Atualização: 04/10/2026. Card: https://trello.com/c/MT77ib4r/4-gabriel-01-setup-do-reposit%C3%B3rio-e-ambiente

## Responsabilidades e crédito

- Responsável de continuidade: Gabriel; membros nativos anteriores preservados: Gabriel Sousa.
- Revisor previsto no card: Allan.
- Atualização, organização e documentação desta rodada: **Matheus Siqueira**, com assistência de ferramentas.
- **Implementador dos avanços técnicos descritos nesta rodada: Matheus Siqueira**, mesmo quando o card está atribuído a outro integrante. Responsabilidade futura/revisão não transfere autoria da implementação. Quando abaixo consta trabalho pendente, não há implementação a creditar desse escopo.

## Objetivo e resultado esperado

[Gabriel] 01 · Setup do repositório e ambiente

Entrega de referência: README.md; pom.xml; reports/execucoes/

## O que foi feito, como e evidência

Matheus preparou wrapper, ambiente e workflow executáveis. Build Maven/Next e testes passaram no runner remoto; sandbox local tinha bloqueios de Java/rede documentados.

Arquivos de referência: README.md; pom.xml; reports/execucoes/.

Os arquivos existentes podem ser consultados na [branch publicada](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/tree/a3-contract-validation). A comprovação técnica atual está no [CI aprovado](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37177644692) e em [relatório de sessão](../../../reports/execucoes/run-2026-10-04-sessao-nextjs.md). O CI tem 12 Java, 23 contrato, 3 cliente, 12 HTTP e 2 E2E. Esses números são verificações executadas em escopos diferentes, não pontuação acadêmica, cobertura percentual ou prova de implementação de todo este card. O relatório de CT-MONEY-002 comprova a correção da quantidade multiplicada uma vez. Não foram implementadas metas, reserva/saída de estoque ou as quatro telas de negócio.

## Estado e motivo

Anterior: Em revisão. Estado definido nesta atualização: **Em revisão**.

Artefato técnico entregue e testado dentro do escopo descrito. Falta revisão nominal da equipe; não existe aceite humano presumido.

## Briefing de continuidade

Gabriel: validar setup Windows com JDK 25/Node/Docker e atualizar passos em QA-08. Não afirmar teste local completo com base no CI; revisar logs e branch publicados.

### Entradas e dependências

Consultar os arquivos e próximos passos específicos abaixo; decisões de domínio em docs/decisoes/regras-loja.md.

Ler os arquivos citados antes da execução; conferir comportamento existente e registrar decisão ainda incerta como pendência. Reusar os cards de domínio/API/BD/QA correspondentes. Uma entrega futura precisa dos endpoints e dados reais de sua versão; não substituir integração por números fictícios.

### Passos executáveis

1. Preparar (até 2h, estimativa): verificar branch/commit, ler o objetivo, os arquivos e a entrega; confirmar os dados necessários e registrar dependência ausente.
2. Executar uma subentrega do briefing acima (até 4h por tarefa, estimativa). Se não couber, dividir em tarefas com entrada, saída e verificação próprias no checklist do card; não prometer a conclusão inteira nessa janela.
3. Verificar (até 2h por conjunto, estimativa): comparar com critérios de aceite originais abaixo e registrar esperado/obtido, versão, ambiente e comandos. Para documentação, conferir links e consistência com código; para código, executar unitário/integração/contrato pertinente.
4. Revisar (até 1h por rodada, estimativa): publicar arquivos/resultados, solicitar revisão de Allan, registrar achados e reteste. Commit novo usa prefixo `autofix:` quando aplicável à correção/sincronização. Capacidade e datas devem ser combinadas com a equipe; estes tempos não são compromisso de sprint.

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

**Responsável principal:** Gabriel
**Apoio:** Matheus
**Revisor:** Allan

## Execução — 03/10/2026
Rodada anterior restaurou Maven Wrapper3.9.11, JDK25 portátil, PostgreSQL18/Compose, CI mínimo e exportação OpenAPI; commit ca79f02. Dois testes Java aprovados naquele ambiente.
Continuação: corrigi mvnw.cmd para atributo Target nulo em diretório comum no PowerShell (antes falhava em indexação de matriz nula). Separei unitários sem banco de contexto/persistência anotação JUnit Tag (integration); criei perfil Spring/banco descartável separado e runner PowerShell.
Utilizado: Java25/Boot4.1, Maven Wrapper/Surefire, PowerShell, Compose/PostgreSQL18, JUnit tags. CI preparado para -Pintegration verify e artefatos Surefire.

## Evidências
mvnw.cmd; pom.xml; docker-compose.test.yml; scripts/test.ps1; .github/workflows/ci.yml; docs/decisoes/stack.md; reports/execucoes/run-2026-10-03-continuacao.md.
Wrapper passou a iniciar Maven. Nesta sessão, JVM falhou em java.security (AccessDeniedException), Docker sem acesso e npm com EACCES. Não contar como falha do produto nem sucesso dos testes novos.

## Pendências
Reteste Java/runner/CI em ambiente liberado; revisão Gabriel. Mantido Em revisão.

## Checklist técnico atualizado em 04/10

Itens abaixo marcados tecnicamente executados por Matheus Siqueira, com evidência nos relatórios/CI citados. Revisão humana e demais itens permanecem pendentes.

- [x] Organizar src/main, src/test, docs, reports e config conforme Maven e documentar no README.
- [x] Executar a API e verificar Swagger UI e /v3/api-docs; registrar versões e falhas reais.
- [x] Exportar OpenAPI em config/ com commit, gerador e hash; vincular ao card de baseline.
- [x] Preservar B0 antes de alterações; separar B1 executável e documentar ajustes mínimos.
