# [Francisco] DOC-01 · Consolidar rubrica, rastreabilidade e autoria

Card: https://trello.com/c/qT7awxOR/64-francisco-doc-01-consolidar-rubrica-rastreabilidade-e-autoria. Membro nativo atribuído: Francisco.

Criado/planejado por **Matheus Siqueira** em 04/10/2026. Execução futura: **Francisco**; revisor: **Allan**. Não atribuir a Matheus implementação ainda pendente. Estado inicial: A fazer. Complexidade estimada 3 pontos, sujeita à capacidade da equipe; cada tarefa tem janela de até 4 h.

## História e por que criar este card

Como integrante, quero evidências ligadas aos critérios acadêmicos para entender o que foi entregue, quem implementou e o que ainda falta.

Matheus publicou documentação técnica e nesta rodada prepara briefings dos 64 cards. Rubrica completa, catálogo de 30 casos, atas e confirmação docente ainda exigem consolidação humana.

Este card decompõe uma lacuna da implementação atual; complementa cards existentes sem substituir seus critérios nem duplicar resultados. Os demais domínios já têm API-02 a06, BD e UI próprios; não foram criados novos cards genéricos para os mesmos escopos.

## Entradas, arquivos e dependências

Caminhos Java relativos a `src/main/java/com/swee/ordermanagementspring/`, exceto caminhos completos iniciados por docs/src/test/src/main/resources.

docs/matriz-rubrica.md; docs/catalogo-casos.md; docs/decisoes/alinhamento-docente.md; docs/gestao/README.md; docs/checklist-entrega.md

Critérios de 100 pontos; Catálogo de pelo menos 30 casos; cards 00; relatórios publicados. Reusar cards existentes, sem duplicar seus entregáveis.

Ler docs/execucao-sessao.md e docs/decisoes/regras-loja.md. Branch: https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/tree/a3-contract-validation. Evidência atual (não aceite da tarefa futura): https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37177644692.

## Execução ordenada

- T1 (até 2h): mapear 14 critérios do enunciado a card/arquivo/evidência/estado; marcar ausente e não substituir prova por descrição.
- T2 (até 3h): integrar catálogo e testes com ID estável RF→CA→CT→run→defeito. Explicar 52 testes versus pelo menos 30 casos acadêmicos e separar baseline/evolução.
- T3 (até 2h): conferir crédito Matheus Siqueira nas implementações desta rodada, mantendo autoria anterior e revisores. Anexar confirmação docente apenas se recebida, sem dados privados.
- T4 (até 2h): testar links GitHub/Trello, abrir PDFs, conferir os dez entregáveis e registrar revisão nominal. Atualizar cards existentes com evidência, sem fechar tudo por este índice.

## Aceite — sucesso, erro e limite

1. Dado critério com artefato/run, quando clicar a referência, então arquivo certo abre e identifica versão/autor/resultado.

2. Dado critério sem prova ou rota apenas planejada, quando consolidar, então aparece pendente e não concluído.

3. Dado card originalmente Gabriel/Allan com código de Matheus, quando revisar créditos, então implementador Matheus e responsável/revisor original aparecem distintos; duplicação de CT não infla contagem.

## Verificação

```text
Conferir links e matriz manualmente; usar git diff --check antes do commit; anexar aceite de Allan.
```

Registrar esperado/obtido, commit, ferramenta, ambiente e nome do teste ou documento. Usar somente massa sintética e banco descartável. Não salvar senha/token/cookie. Falha é resultado a documentar e corrigir, não motivo para inventar aprovação. O runner CI anterior prova a base, não a nova entrega.

## Definição de pronto

- [ ] Entradas e dependências verificadas; dúvidas resolvidas/documentadas antes de codificar.
- [ ] Tarefas entregues e todos os cenários acima verificados.
- [ ] Código/contrato/migração/documentação consistentes, quando aplicáveis.
- [ ] Evidências sanitizadas publicadas com commit e links.
- [ ] Revisão nominal de Allan com achados resolvidos ou limite aceito.
- [ ] Card movido para Em revisão quando entregue e Concluído após aceite.

## Falha, recuperação e limites

Se alterar persistência, testar com dados sintéticos e backup antes de migração; nunca executar create-drop no banco da loja. Se regressão quebrar, manter card em andamento, reduzir caso e retestar antes de concluir. Não usar force push nem reescrever commits da equipe. Novos commits desta sincronização usam `autofix:`; futuras correções também identificam problema e evidência.

Datas internas precisam de reestimativa com os integrantes; estes pontos/tempos são estimativas, não uma sprint aprovada. Nenhuma autorização docente, merge em main ou entrega final é presumida pelo briefing.
