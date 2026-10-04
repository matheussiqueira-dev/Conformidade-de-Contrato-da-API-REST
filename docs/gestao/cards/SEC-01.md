# [Matheus] SEC-01 · Vincular autoria do pedido à sessão

Card: https://trello.com/c/uBQH1dMy/62-matheus-sec-01-vincular-autoria-do-pedido-%C3%A0-sess%C3%A3o. Membro nativo atribuído: Matheus.

Criado/planejado por **Matheus Siqueira** em 04/10/2026. Execução futura: **Matheus**; revisor: **Gabriel**. Não atribuir a Matheus implementação ainda pendente. Estado inicial: A fazer. Complexidade estimada 5 pontos, sujeita à capacidade da equipe; cada tarefa tem janela de até 4 h.

## História e por que criar este card

Como vendedor, quero pedidos atribuídos à minha conta autenticada para impedir falsificação de autoria e acesso a vendas de colegas.

Sessão e perfis foram implementados por Matheus Siqueira. Order ainda não vincula o usuário autenticado; endpoints de vendedor são planejados, e rotas legadas permanecem só para gerente.

Este card decompõe uma lacuna da implementação atual; complementa cards existentes sem substituir seus critérios nem duplicar resultados. Os demais domínios já têm API-02 a06, BD e UI próprios; não foram criados novos cards genéricos para os mesmos escopos.

## Entradas, arquivos e dependências

Caminhos Java relativos a `src/main/java/com/swee/ordermanagementspring/`, exceto caminhos completos iniciados por docs/src/test/src/main/resources.

entities/Order.java; dto/OrderRequestDTO.java; services/OrderService.java; controllers/OrderController.java; config/SecurityConfig.java; repositories/OrderRepository.java; src/test/java/

API-01 base entregue; BD-01/BD-02 para FK/migração; DOM-01 para regras de pedido alheio.

Ler docs/execucao-sessao.md e docs/decisoes/regras-loja.md. Branch: https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/tree/a3-contract-validation. Evidência atual (não aceite da tarefa futura): https://github.com/matheussiqueira-dev/Conformidade-de-Contrato-da-API-REST/actions/runs/37177644692.

## Execução ordenada

- T1 (até 2h): desenhar FK/vendedor histórico e política para pedidos anteriores; não inventar vendedor para registros antigos. Documentar rota de vendedor e DTO restrito no alvoOpenAPI.
- T2 (até 4h): derivar AppPrincipal no servidor, resolver usuário e persistir autoria. Remover ou rejeitar sellerId controlado pelo cliente; impedir atribuição manual a outro usuário.
- T3 (até 4h): aplicar consulta/projeção por proprietário. Gerente cancela pedido alheio conforme regra aprovada; vendedor não altera pedido alheio. Edição alheia continua pendência de domínio.
- T4 (até 3h): testes com dois vendedores e gerente, tentativa de IDOR e rollback; confirmar ausência de custo/margem/colegas em JSON e regressão de acesso legado.

## Aceite — sucesso, erro e limite

1. Dado vendedor A autenticado, quando criar pedido válido, então autor persistido é A e reload mantém vínculo.

2. Dado vendedor A envia sellerId de B ou tenta alterar pedido de B, quando executar, então não obtém autoria de B nem alteração não autorizada; contrato define4xx.

3. Dado sessão expirada ou pedido antigo sem autor, quando acessar, então 401 ou tratamento legado explícito, sem autor inventado ou dados de terceiro.

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
- [ ] Revisão nominal de Gabriel com achados resolvidos ou limite aceito.
- [ ] Card movido para Em revisão quando entregue e Concluído após aceite.

## Falha, recuperação e limites

Se alterar persistência, testar com dados sintéticos e backup antes de migração; nunca executar create-drop no banco da loja. Se regressão quebrar, manter card em andamento, reduzir caso e retestar antes de concluir. Não usar force push nem reescrever commits da equipe. Novos commits desta sincronização usam `autofix:`; futuras correções também identificam problema e evidência.

Datas internas precisam de reestimativa com os integrantes; estes pontos/tempos são estimativas, não uma sprint aprovada. Nenhuma autorização docente, merge em main ou entrega final é presumida pelo briefing.
