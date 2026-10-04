# [Gabriel] Extrair e versionar o contrato OpenAPI (baseline)

Atualização: 04/10/2026. Card: https://trello.com/c/haebO3FA/27-gabriel-extrair-e-versionar-o-contrato-openapi-baseline

## Responsabilidades e crédito

- Responsável de continuidade: Gabriel; membros nativos anteriores preservados: Gabriel Sousa.
- Revisor previsto no card: Allan.
- Atualização, organização e documentação desta rodada: **Matheus Siqueira**, com assistência de ferramentas.
- **Implementador dos avanços técnicos descritos nesta rodada: Matheus Siqueira**, mesmo quando o card está atribuído a outro integrante. Responsabilidade futura/revisão não transfere autoria da implementação. Quando abaixo consta trabalho pendente, não há implementação a creditar desse escopo.

## Objetivo e resultado esperado

[Gabriel] Extrair e versionar o contrato OpenAPI (baseline)

Entrega de referência: config/openapi/; src/main/java/com/swee/ordermanagementspring/config/OpenApiConfig.java

## O que foi feito, como e evidência

Matheus preservou baseline original, adicionou revisão de erros e alvo v2/v3; OAS3.1 oficial e 2020-12 validados, schemas 41/71 sem findings. Swagger declara cookie/CSRF/logout e erros reais.

Arquivos de referência: config/openapi/; src/main/java/com/swee/ordermanagementspring/config/OpenApiConfig.java.

Os arquivos existentes podem ser consultados na [branch publicada](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/tree/a3-contract-validation). A comprovação técnica atual está no [CI aprovado](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37177644692) e em [relatório de sessão](../../../reports/execucoes/run-2026-10-04-sessao-nextjs.md). O CI tem 12 Java, 23 contrato, 3 cliente, 12 HTTP e 2 E2E. Esses números são verificações executadas em escopos diferentes, não pontuação acadêmica, cobertura percentual ou prova de implementação de todo este card. O relatório de CT-MONEY-002 comprova a correção da quantidade multiplicada uma vez. Não foram implementadas metas, reserva/saída de estoque ou as quatro telas de negócio.

## Estado e motivo

Anterior: Em andamento. Estado definido nesta atualização: **Em revisão**.

Artefato técnico entregue e testado dentro do escopo descrito. Falta revisão nominal da equipe; não existe aceite humano presumido.

## Briefing de continuidade

Gabriel: conferir exportação runtime e diffs; manter original imutável, marcar rotas planejadas e revisar contratos de erro. Comparar comportamento HTTP autenticado à versão implementada.

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
Baseline exportado em config/openapi/baseline-openapi-2026-10-03.json: 14 paths, SHA-256 4CF78EA43C7A24490F5B53F4E4BB11FCBBC4689EFDC1DB57BB9FA5EB9FE8F180.
Alvo inicial de 10 paths/11 operações foi preservado. Nova revisão: config/openapi/target-loja-gestao-openapi-2026-10-03-v2.json, SHA-256 16A77FD221546B1A77A5D703E81AD3AAC863F41D25D61390FBC5FB130D4D33C7.
Como: fechamento de campos extras nas projeções do vendedor e objetos aninhados, itens obrigatórios e extensão x-roles por operação. Marcadores planned mantidos; permissões são propostas, sem enforcement no servidor.
Utilizado: exportação Springdoc anterior; Node.js/fs/crypto para inspeção e hashes; auditoria estrutural e exemplos sintéticos no spike.

## Evidências
docs/decisoes/contrato.md e contract-testing.md; reports/contrato/spike-2026-10-03/. Sete testes da auditoria aprovados; v2 rejeita exemplos de custo/equipe e margem extra.

## Pendências
Validação integral OpenAPI 3.1/2020-12 e execução HTTP/autorização; revisão do alvo. Ajv legado incompatível; novas rotas ainda não implementadas. Mantido Em andamento.
