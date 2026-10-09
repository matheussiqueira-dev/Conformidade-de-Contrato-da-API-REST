# [Francisco] 19 · Segurança, privacidade e ética

Atualização: 04/10/2026. Card: https://trello.com/c/VQvMapdo/22-francisco-19-seguran%C3%A7a-privacidade-e-%C3%A9tica

## Responsabilidades e crédito

- Responsável de continuidade: Francisco; membros nativos anteriores preservados: FR4NCISCO OFC.
- Revisor previsto no card: Allan.
- Atualização, organização e documentação desta rodada: **Matheus Siqueira**, com assistência de ferramentas.
- **Implementador dos avanços técnicos descritos nesta rodada: Matheus Siqueira**, mesmo quando o card está atribuído a outro integrante. Responsabilidade futura/revisão não transfere autoria da implementação. Quando abaixo consta trabalho pendente, não há implementação a creditar desse escopo.

## Objetivo e resultado esperado

Analisar segurança, privacidade, ética e impacto de falhas (seção 18, 2 pontos).

Entrega de referência: docs/etica-seguranca.md + referências para achados/evidências sanitizadas.

## O que foi feito, como e evidência

Matheus implementou BCrypt, cookie HttpOnly/SameSite, CSRF e bloqueio do vendedor nos DTOs legados. Contas CI sintéticas; traces desabilitados.

Arquivos de referência: docs/execucao-sessao.md; frontend/playwright.config.mjs.

Os arquivos existentes podem ser consultados na [branch publicada](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/tree/a3-contract-validation). A comprovação técnica atual está no [CI aprovado](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37177644692) e em [relatório de sessão](../../../reports/execucoes/run-2026-10-04-sessao-nextjs.md). O CI tem 12 Java, 23 contrato, 3 cliente, 12 HTTP e 2 E2E. Esses números são verificações executadas em escopos diferentes, não pontuação acadêmica, cobertura percentual ou prova de implementação de todo este card. O relatório de CT-MONEY-002 comprova a correção da quantidade multiplicada uma vez. Não foram implementadas metas, reserva/saída de estoque ou as quatro telas de negócio.

## Estado e motivo

Anterior: Backlog. Estado definido nesta atualização: **A fazer**.

Tarefa preparada para continuidade documental/técnica. Não foi iniciado código deste escopo só por mudar a coluna.

## Briefing de continuidade

Francisco: documentar finalidade dos dados, retenção, massa sintética e riscos residuais. Tratar provisionamento, limitação de login e ambiente HTTPS como pendências, sem declarar segurança completa.

### Entradas e dependências

Inventário do sistema, defeitos e evidências; execução local pode ocorrer em dupla.

Fontes: enunciado A3 + direcao-criativa.md/Guia Visual (01/10). Novas funções são planejadas. Datas anteriores: reestimar em PL-01. Baseline e detalhes anteriores preservados no registro local desta revisão.

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

**Responsável principal:** Francisco
**Apoio:** Matheus e Gabriel fornecem inspeções técnicas
**Revisor:** Allan
**Referência anterior (provisória):** 2026-11-20

## Objetivo
Analisar segurança, privacidade, ética e impacto de falhas (seção 18, 2 pontos).

## Execução — direção de 01/10/2026
Acrescentar acesso mínimo por perfil, histórico compartilhado limitado, custo/margem/resultados de colegas protegidos e auditoria de alterações críticas. Examinar impacto de metas/rankings sem exposição indevida. Pagamentos e dados são sintéticos; autenticação agora é requisito funcional.

## Entrega
docs/etica-seguranca.md + referências para achados/evidências sanitizadas.

## Aceite
- [ ] Pergunta sobre falha grave respondida.
- [ ] Riscos ligados ao sistema.
- [ ] Texto revisado tecnicamente.
- [ ] Ampliação acima contemplada com evidência e revisão.

## Dependências
Inventário do sistema, defeitos e evidências; execução local pode ocorrer em dupla.

Fontes: enunciado A3 + direcao-criativa.md/Guia Visual (01/10). Novas funções são planejadas. Datas anteriores: reestimar em PL-01. Baseline e detalhes anteriores preservados no registro local desta revisão.

## Atualização 08/10/2026 — credenciais
- Senhas removidas do código: o banco de teste usa `TEST_DB_PASSWORD` (gerada por execução nos runners e no CI; secret opcional no GitHub) e o banco de desenvolvimento usa `SPRING_DATASOURCE_PASSWORD` (`application.properties`, `docker-compose.yml`, README).
- Decisão: as senhas antigas permanecem no histórico do Git. Elas valem apenas para bancos locais ou descartáveis, e reescrever o histórico obrigaria a equipe a clonar de novo e quebraria links de commits nos relatórios. Mitigação: cada integrante troca a senha do próprio banco local.
- Usar este registro como evidência no `docs/etica-seguranca.md`.
