# LINKS IMPORTANTES

Atualização: 04/10/2026. Card: https://trello.com/c/3MmXle4G/44-links-importantes

## Responsabilidades e crédito

- Responsável de continuidade: Matheus; membros nativos anteriores preservados: Matheus Siqueira, FR4NCISCO OFC, Gabriel Sousa, Allan Gabriel Almeida Barros.
- Revisor previsto no card: Equipe.
- Atualização, organização e documentação desta rodada: **Matheus Siqueira**, com assistência de ferramentas.
- **Implementador dos avanços técnicos descritos nesta rodada: Matheus Siqueira**, mesmo quando o card está atribuído a outro integrante. Responsabilidade futura/revisão não transfere autoria da implementação. Quando abaixo consta trabalho pendente, não há implementação a creditar desse escopo.

## Objetivo e resultado esperado

LINKS IMPORTANTES

Entrega de referência: README.md; docs/execucao-sessao.md

## O que foi feito, como e evidência

Matheus publicou código, decisões, relatórios HTTP e capturas da sessão na branch de revisão; o CI 37177644692 terminou com sucesso. Os documentos de referência serão incluídos em docs/referencias nesta sincronização.

Arquivos de referência: README.md; docs/execucao-sessao.md.

Os arquivos existentes podem ser consultados na [branch publicada](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/tree/a3-contract-validation). A comprovação técnica atual está no [CI aprovado](https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37177644692) e em [relatório de sessão](../../../reports/execucoes/run-2026-10-04-sessao-nextjs.md). O CI tem 12 Java, 23 contrato, 3 cliente, 12 HTTP e 2 E2E. Esses números são verificações executadas em escopos diferentes, não pontuação acadêmica, cobertura percentual ou prova de implementação de todo este card. O relatório de CT-MONEY-002 comprova a correção da quantidade multiplicada uma vez. Não foram implementadas metas, reserva/saída de estoque ou as quatro telas de negócio.

## Estado e motivo

Anterior: Referências. Estado definido nesta atualização: **Referências**.

Referência contínua; atualizar quando contrato, escopo ou evidência mudar.

## Briefing de continuidade

Usar o índice docs/gestao/README.md como entrada. Distinguir manual antigo de decisões aprovadas e conferir a branch antes de demonstrar.

### Entradas e dependências

Consultar os arquivos e próximos passos específicos abaixo; decisões de domínio em docs/decisoes/regras-loja.md.

Ler os arquivos citados antes da execução; conferir comportamento existente e registrar decisão ainda incerta como pendência. Reusar os cards de domínio/API/BD/QA correspondentes. Uma entrega futura precisa dos endpoints e dados reais de sua versão; não substituir integração por números fictícios.

### Passos executáveis

1. Preparar (até 2h, estimativa): verificar branch/commit, ler o objetivo, os arquivos e a entrega; confirmar os dados necessários e registrar dependência ausente.
2. Executar uma subentrega do briefing acima (até 4h por tarefa, estimativa). Se não couber, dividir em tarefas com entrada, saída e verificação próprias no checklist do card; não prometer a conclusão inteira nessa janela.
3. Verificar (até 2h por conjunto, estimativa): comparar com critérios de aceite originais abaixo e registrar esperado/obtido, versão, ambiente e comandos. Para documentação, conferir links e consistência com código; para código, executar unitário/integração/contrato pertinente.
4. Revisar (até 1h por rodada, estimativa): publicar arquivos/resultados, solicitar revisão de Equipe, registrar achados e reteste. Commit novo usa prefixo `autofix:` quando aplicável à correção/sincronização. Capacidade e datas devem ser combinadas com a equipe; estes tempos não são compromisso de sprint.

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

## Referências do projeto
- Google Drive da equipe: https://drive.google.com/drive/folders/13MA9KBPV3kl6OfPjHm4Dvj73PagR3Dj-?usp=sharing
- Repositório informado pela equipe: https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST
- API de origem: https://github.com/JkbSousa/order-management-api

## Fontes locais conferidas em 01/10/2026
- Projeto- A3.pdf: enunciado de Qualidade, 14 critérios/100 pontos e entrega até 30/11.
- Manual-Projeto-A3.pdf: plano v1.0 da API; exclusões antigas de frontend/auth foram superadas nesta revisão do quadro.
- direcao-criativa.md e Guia-Visual-Sistema-da-Loja.pdf: quatro áreas, perfis e regras propostas.
- order-management-api: backend original; novas funções ainda não implementadas.

Arquivos locais precisam ser publicados pela equipe no repositório/Drive para acesso compartilhado; não há link público confirmado para o novo guia. Os links acima foram preservados como referências informadas; acesso/commit remoto não foi auditado.

## Recursos opcionais para estudantes
Consultar elegibilidade e termos nas páginas oficiais: GitHub Education (https://github.com/education/students), JetBrains (https://www.jetbrains.com/academy/student-pack/), Azure (https://azure.microsoft.com/pt-br/free/students), Gemini (https://gemini.google/students/), AWS Educate (https://aws.amazon.com/pt/education/awseducate/), Oracle Academy (https://academy.oracle.com/pt-br/solutions-cloud-program.html) e Google Cloud Education (https://cloud.google.com/edu/students?hl=pt-BR).
Benefícios, valores, armazenamento e duração não foram confirmados; não tratá-los como garantidos nem como dependências do projeto.
