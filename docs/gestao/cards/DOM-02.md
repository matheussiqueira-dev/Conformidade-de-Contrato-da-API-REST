# [Gabriel] DOM-02 · Migrar dinheiro para BigDecimal e NUMERIC

Card: https://trello.com/c/HxWdN6Fv/61-gabriel-dom-02-migrar-dinheiro-para-bigdecimal-e-numeric. Membro nativo atribuído: Gabriel.

Criado/planejado por **Matheus Siqueira** em 04/10/2026. Execução futura: **Gabriel**; revisor: **Allan**. Não atribuir a Matheus implementação ainda pendente. Estado inicial: A fazer. Complexidade estimada 5 pontos, sujeita à capacidade da equipe; cada tarefa tem janela de até 4 h.

## História e por que criar este card

Como gerente, quero valores monetários exatos para que pedido, pagamento e custo não acumulem erro de ponto flutuante.

Quantidade já multiplica uma única vez, mas entidades/DTOs legados usam Double. Matheus aprovou duas casas decimais e HALF_UP; esta migração ainda não foi implementada.

Este card decompõe uma lacuna da implementação atual; complementa cards existentes sem substituir seus critérios nem duplicar resultados. Os demais domínios já têm API-02 a06, BD e UI próprios; não foram criados novos cards genéricos para os mesmos escopos.

## Entradas, arquivos e dependências

Caminhos Java relativos a `src/main/java/com/swee/ordermanagementspring/`, exceto caminhos completos iniciados por docs/src/test/src/main/resources.

entities/product/Product.java; entities/OrderItem.java; entities/payment/Payment.java; dto/; services/OrderService.java; src/main/resources/db/migration/; src/test/java/

DOM-01 aprovado; BD-02 para migration; QA-04 como regressão.

Ler docs/execucao-sessao.md e docs/decisoes/regras-loja.md. Branch: https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/tree/a3-contract-validation. Evidência atual (não aceite da tarefa futura): https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37177644692.

## Execução ordenada

- T1 (até 2h): inventariar todos os campos Double monetários com rg; registrar tipo/API/coluna e estratégia de conversão em docs/decisoes/dinheiro.md. Não converter quantidade ou percentual como dinheiro.
- T2 (até 4h): trocar entidade/DTO/serviço por BigDecimal, construir de texto e aplicar scale2/HALF_UP no limite definido. Preservar preço histórico e subtotal multiplicado uma vez; atualizar todos os consumidores.
- T3 (até 4h): criar migração NUMERIC com precisão escolhida por intervalo validado; testar instalação limpa e atualização de massa sintética. Comparar valores antes/depois; documentar backup/rollback.
- T4 (até 3h): validar casos abaixo e contrato/JSON numérico; anexar run e diff. Não alterar arbitrariamente preço antigo por nova entrada.

## Aceite — sucesso, erro e limite

1. Dado preço 100.00 e quantidade 2, quando criar pedido, então subtotal 200.00 e preço histórico 100.00.

2. Dado valor inválido/não numérico ou negativo, quando enviar, então resposta4xx documentada e nenhuma persistência parcial.

3. Dado valor1.005 no ponto de arredondamento definido, quando normalizar, então 1.01 com HALF_UP; testar0.10 + 0.20 = 0.30 e limite de coluna sem overflow silencioso.

## Verificação

```text
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\test.ps1 -Mode Integration; .\scripts\test-contract.ps1 -Frontend
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
