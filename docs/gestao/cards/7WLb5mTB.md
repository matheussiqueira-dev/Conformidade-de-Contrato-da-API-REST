# [Gabriel] Spike: escolher ferramenta de contract testing

Atualização: 04/10/2026. Card: https://trello.com/c/7WLb5mTB/28-gabriel-spike-escolher-ferramenta-de-contract-testing

## Responsabilidades e crédito

- Responsável de continuidade: Gabriel; membros nativos anteriores preservados: Gabriel Sousa.
- Revisor previsto no card: Allan.
- Atualização, organização e documentação desta rodada: **Matheus Siqueira**, com assistência de ferramentas.
- **Implementador dos avanços técnicos descritos nesta rodada: Matheus Siqueira**, mesmo quando o card está atribuído a outro integrante. Responsabilidade futura/revisão não transfere autoria da implementação. Quando abaixo consta trabalho pendente, não há implementação a creditar desse escopo.

## Objetivo e resultado esperado

[Gabriel] Spike: escolher ferramenta de contract testing

Entrega de referência: docs/decisoes/contract-testing.md; scripts/validate-contract.mjs

## O que foi feito, como e evidência

Matheus escolheu Node/Ajv 2020-12 determinístico, oficial OAS3.1 vendorizado e adapter limitado Ajv#1745; 23 testes e validações baseline/alvo passaram. Schemathesis é complemento futuro.

Arquivos de referência: docs/decisoes/contract-testing.md; scripts/validate-contract.mjs.

Os arquivos existentes podem ser consultados na [branch publicada](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/tree/a3-contract-validation). A comprovação técnica atual está no [CI aprovado](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37177644692) e em [relatório de sessão](../../../reports/execucoes/run-2026-10-04-sessao-nextjs.md). O CI tem 12 Java, 23 contrato, 3 cliente, 12 HTTP e 2 E2E. Esses números são verificações executadas em escopos diferentes, não pontuação acadêmica, cobertura percentual ou prova de implementação de todo este card. O relatório de CT-MONEY-002 comprova a correção da quantidade multiplicada uma vez. Não foram implementadas metas, reserva/saída de estoque ou as quatro telas de negócio.

## Estado e motivo

Anterior: Em andamento. Estado definido nesta atualização: **Em revisão**.

Artefato técnico entregue e testado dentro do escopo descrito. Falta revisão nominal da equipe; não existe aceite humano presumido.

## Briefing de continuidade

Gabriel: revisar decisão, adapter e fixtures negativas, garantir referência quebrada e bypass de auth/CSRF falham. Não desabilitar unevaluatedProperties para mascarar erros.

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

## Execução — continuação de 03/10/2026
Auditei o alvo (10 paths/11 operações), referências, perfis, segurança declarada, RF e projeções. Identifiquei schemas abertos que aceitavam campos sensíveis extras; criei alvo v2 preservando o baseline e alvo inicial.
Como: scripts/audit-contract.mjs + sete testes Node; probe-legacy-ajv.cjs compara exemplos sintéticos antes/depois. V2 rejeitou custo/equipe e margem aninhada. Ajv 6.15.0 local falhou ao compilar metaschema 2020-12: não foi adotado.
Utilizado: Node 24.19.0, node:test, fs/assert e Ajv legado apenas diagnóstico de subconjunto draft-07. Proposta: avaliar Ajv 8/ajv/dist/2020; instalação npm bloqueada por EACCES.

## Entrega e evidência
docs/decisoes/contract-testing.md; reports/contrato/spike-2026-10-03/; reports/execucoes/run-2026-10-03-continuacao.md.
7 testes Node aprovados. Auditoria estrutural não é validação completa OpenAPI nem teste HTTP.

## Pendências
Validador 2020-12, positivo/negativo HTTP real, sessão e 401/403 após API-01; revisão Allan/Gabriel. Em andamento; sem declarar conformidade ou autenticação implementada.
