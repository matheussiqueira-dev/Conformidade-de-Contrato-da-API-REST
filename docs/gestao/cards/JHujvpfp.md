# [Matheus] UI-01 · Implementar base visual e navegação por perfil

Atualização: 04/10/2026. Card: https://trello.com/c/JHujvpfp/55-matheus-ui-01-implementar-base-visual-e-navega%C3%A7%C3%A3o-por-perfil

## Responsabilidades e crédito

- Responsável de continuidade: Matheus; membros nativos anteriores preservados: Matheus Siqueira.
- Revisor previsto no card: Gabriel.
- Atualização, organização e documentação desta rodada: **Matheus Siqueira**, com assistência de ferramentas.
- **Implementador dos avanços técnicos descritos nesta rodada: Matheus Siqueira**, mesmo quando o card está atribuído a outro integrante. Responsabilidade futura/revisão não transfere autoria da implementação. Quando abaixo consta trabalho pendente, não há implementação a creditar desse escopo.

## Objetivo e resultado esperado

[Matheus] UI-01 · Implementar base visual e navegação por perfil

Entrega de referência: Frontend/componentes + evidências de revisão

## O que foi feito, como e evidência

Matheus implementou base Next e painel real de acesso: login, recuperação de sessão, logout, erro acessível e layout desktop/mobile. Dois E2E e build passaram.

Arquivos de referência: frontend/app/access-panel.js; frontend/app/globals.css; frontend/lib/auth-client.mjs.

Os arquivos existentes podem ser consultados na [branch publicada](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/tree/a3-contract-validation). A comprovação técnica atual está no [CI aprovado](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37177644692) e em [relatório de sessão](../../../reports/execucoes/run-2026-10-04-sessao-nextjs.md). O CI tem 12 Java, 23 contrato, 3 cliente, 12 HTTP e 2 E2E. Esses números são verificações executadas em escopos diferentes, não pontuação acadêmica, cobertura percentual ou prova de implementação de todo este card. O relatório de CT-MONEY-002 comprova a correção da quantidade multiplicada uma vez. Não foram implementadas metas, reserva/saída de estoque ou as quatro telas de negócio.

## Estado e motivo

Anterior: Backlog. Estado definido nesta atualização: **Em andamento**.

Há contribuição concreta parcial; os próximos passos ainda são necessários para cumprir o escopo completo.

## Briefing de continuidade

Matheus: finalizar navegação por perfil ao integrar telas de domínio; UX-01/QA-08 revisam teclado e 200% de zoom. Acesso entregue não equivale a Gestão/Vender/Estoque completos.

### Entradas e dependências

UX-01, ARQ-01 e API-01

Planejado em 01/10/2026 conforme direcao-criativa.md e Guia Visual. Não implementado/certificado por esta revisão. Concluir apenas com execução real e revisão. Responsáveis propostos precisam ser validados no acordo da equipe.

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

**Responsável proposto:** Matheus
**Apoio:** Francisco
**Revisor:** Gabriel
**Prioridade/sequência:** Implementar após dependências; prazo a reestimar em PL-01.

## Objetivo e execução
Criar shell claro com navegação lateral e componentes reutilizáveis seguindo Guia Visual: branco/cinza, destaque #5468E8, degradê discreto, espaço de 8 px e números legíveis. Referência desktop 1440×1024; prever telas menores. Menu por perfil, sessão e estados globais. API deve autorizar independentemente da navegação.

## Entrega
Frontend/componentes + evidências de revisão

## Critérios de conclusão
- [ ] Navegação coerente para gerente/vendedor
- [ ] teclado/foco/contraste e overflow conferidos
- [ ] carregamento/erro/vazio implementados.

## Dependências
UX-01, ARQ-01 e API-01

Planejado em 01/10/2026 conforme direcao-criativa.md e Guia Visual. Não implementado/certificado por esta revisão. Concluir apenas com execução real e revisão. Responsáveis propostos precisam ser validados no acordo da equipe.
