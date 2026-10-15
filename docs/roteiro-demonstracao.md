# Roteiro de demonstração (12 minutos)

A apresentação roda no ambiente publicado: <https://sbi-app-alpha.vercel.app> (frontend na Vercel, API no Render, banco no Neon), com a massa Olist já carregada. A stack local fica como [plano B](#plano-b-stack-local).

## Antes de começar (10 minutos antes)

1. Aqueça o ambiente, porque o plano gratuito do Render dorme o serviço e o Neon suspende o banco ocioso ([deploy.md, seção 6](deploy.md)):
   - abra <https://sbi-api-puk8.onrender.com/auth/csrf> até responder 200 (a primeira vez pode levar cerca de um minuto);
   - entre como Matheus e visite Visão geral e Clientes;
   - deixe uma aba em Visão geral: o painel consulta a API a cada 15 segundos e impede o serviço de dormir.
2. Confira, como Matheus:
   - em Usuários, as quatro contas ativas (se um ensaio anterior parou no passo 8, ative o Allan);
   - em Promoções, "Acessorios 10% - outubro a dezembro" vigente;
   - em Estoque, `CG-20W` com saldo para duas unidades (cada ensaio do passo 2 baixa duas).

Deixe abertas:
- três janelas do navegador em <https://sbi-app-alpha.vercel.app>: uma normal, uma anônima e uma de outro perfil do navegador;
- o IntelliJ com o painel Database ligado ao Neon (URL JDBC do painel do Neon com o host **sem** `-pooler` e `sslmode=require`, usuário e senha do painel) e o arquivo `docs/bd/pacote-sql/08_crud_clientes_olist.sql` aberto;
- um terminal na raiz do repositório, com o `gh` autenticado.

Contas: Matheus Siqueira (gerente, o único da loja) e Gabriel Sousa, Allan Barros e Francisco Ferreira (vendedores). E-mails e senhas são combinados com a equipe e não ficam no repositório.

## Roteiro

| Min. | Passo | O que mostrar | O que dizer |
| --- | --- | --- | --- |
| 0:00 | 1. Login do vendedor | Janela normal: tela "Entrar no SBI", entrar como Gabriel. O menu mostra ATENDIMENTO (Vender, Vendas, Clientes) e MEU ESPAÇO (Meu desempenho, Trocar senha), sem GESTÃO. | O menu vem das áreas que o servidor libera para a sessão; o cookie é HttpOnly. A interface segue o manual de marca SBI v1.0. |
| 1:00 | 2. Venda com Pix e desconto | Em Vender ("Nova venda", responsável Gabriel), digitar "/" e o SKU `CG-20W`, Enter, mais uma unidade com "+": 2 × R$ 200,00 com a promoção de 10% em acessórios = R$ 360,00. Tentar desconto manual de 10%: "Desconto acima do seu limite". Aplicar 5% com motivo. Continuar, buscar o cliente pelo CPF, Pix, retirada, Criar pedido, Confirmar pagamento recebido. | Preço, desconto e total vêm do servidor. A alçada do vendedor é 5% e a do gerente 20%; acima disso, o gerente aprova no pedido. A venda reserva o estoque e o pagamento dá a baixa. Um duplo clique não cria dois pedidos (Idempotency-Key). |
| 2:30 | 3. Acesso indevido | Abrir <https://sbi-app-alpha.vercel.app/gestao> como Gabriel: aparece "Sem acesso a esta área". Abrir `/vendas/<id de um pedido de outro vendedor>`: aparece "Pedido não encontrado". | São três barreiras: URL, método e dono do dado. Pedido de outro vendedor responde 404, sem confirmar que existe. |
| 3:15 | 4. Cliente recorrente | Em Clientes, buscar "Larissa Esteves" e abrir a da massa Olist (e-mail `larissa.cli-001@example.com`, 14 pedidos): indicadores, abas Pedidos e Produtos mais comprados com o teclado, documento mascarado. | Para o vendedor, total e ticket médio contam só as vendas dele; o gerente vê a loja toda. O histórico vem de pedidos reais anonimizados da Olist, com nomes e documentos sintéticos. |
| 4:15 | 5. Gerente e baixa no estoque | Janela anônima: entrar como Matheus. Em Estoque, buscar `CG-20W` e mostrar saldo, reservado e disponível. | A saída aconteceu na confirmação do pagamento, com custo médio e movimento no extrato. |
| 5:00 | 6. Visão geral | Em Visão geral, escolher 90 dias: faturamento líquido, lucro bruto (ou "Não informado" com a nota de custo faltante), pedidos pagos, gráfico diário e "Atualizado às". O cartão da meta diz "Meta do mês não definida" quando o mês não tem meta cadastrada (o seed tem só a de setembro); com meta, mostra o realizado e a linha tracejada. | Somente pagamentos confirmados, descontados os estornos. O painel atualiza a cada 15 s com a aba visível; erro nunca vira zero; lucro fica nulo quando falta custo; mês sem meta aparece como tal, sem divisão por zero. |
| 6:00 | 7. Preço com motivo | Em Produtos e preços, clicar em Preço, informar o novo valor e um motivo, salvar e abrir o Histórico (autor e motivo). Abrir a venda do passo 2 em Vendas: o preço antigo continua. | Preço só muda pelo PATCH com motivo; o gatilho grava o histórico; a venda guarda o preço do momento. |
| 6:45 | 8. Desativar vendedor e ver a sessão cair | Na terceira janela, entrar como Allan. Como Matheus, em Usuários, Desativar o Allan com motivo. Na janela do Allan, clicar em qualquer item do menu: volta ao login com "Sua sessão foi encerrada porque o seu acesso mudou. Entre novamente." **Ativar o Allan de novo.** | Mudar papel, situação ou senha incrementa a versão da sessão do usuário; o filtro da API recusa a próxima requisição com 401 `SESSION_REVOKED`. Não é preciso esperar a sessão expirar. |
| 7:45 | 9. Tentar rebaixar o último gerente | Em Usuários, mostrar que a linha "Matheus Siqueira (você)" não tem ação de papel nem de situação. No IntelliJ (Neon), executar `BEGIN; SET CONSTRAINTS ALL IMMEDIATE; UPDATE app_users SET role = 'VENDEDOR' WHERE db_role = 'gerente_matheus'; ROLLBACK;`: o banco responde `LAST_MANAGER: at least one active manager is required`. | Duas proteções: a API proíbe alterar a própria conta (409 `SELF_CHANGE_FORBIDDEN`) e o gatilho `keep_one_active_manager` garante um gerente ativo mesmo com dois gerentes se rebaixando ao mesmo tempo (409 `LAST_MANAGER`). O `db_role` é a chave fixa da conta de gerente do seed. |
| 8:30 | 10. SQL e CRUD no banco | No IntelliJ (Neon), executar `08_crud_clientes_olist.sql` por blocos: cadastro de PF e de PJ com CNPJ alfanumérico, as recusas do banco, histórico da Larissa, `v_cliente_resumo`, ranking de clientes e exclusão recusada pela FK. Tudo termina em `ROLLBACK`. | É o mesmo banco que atende o site. A massa entrou por SQL puro (`\copy`, `INSERT ... SELECT`, uma transação) e o banco aceita CRUD de qualquer cliente SQL; CHECKs, FKs e gatilhos barram o que a API também barra. |
| 10:00 | 11. Contrato | No terminal: `gh run list --branch main --limit 1`, abrir a execução e mostrar o job `contract`. Mostrar `reports/contrato/live-v5/live-contract.json` com `divergences: []`. | As 58 operações do contrato v5 são chamadas de verdade contra uma API isolada (a CI e o `scripts\test-contract.ps1` não tocam no banco publicado), e cada resposta, inclusive de erro, é validada com Ajv contra o contrato. |
| 11:15 | 12. Job `bd` | Na mesma execução, job `bd`, e o artefato `bd-reports/resultado_bd.txt` (`gh run download <id> -n bd-reports`): 60 PASS e, na etapa Olist, 10 PASS. | Flyway V1–V11, seed demo, RBAC/RLS, CRUD, a corrida da última unidade (CC-01), o último gerente (BD-30), as alçadas (BD-32) e a massa Olist (CT-DEMO) passam por `psql`. Cada push na `main` publica sozinho a API e o frontend. |

## Depois da apresentação ou do ensaio

O banco publicado guarda o que foi feito ao vivo:
- confira que o Allan está ativo;
- volte o preço alterado no passo 7, com o motivo "restaurar após ensaio";
- a venda do passo 2 fica no histórico e baixa duas unidades de `CG-20W`. Antes do estoque acabar, registre uma entrada em Estoque.

## Plano B: stack local

Use quando o ambiente publicado não responder (rede do laboratório, Render fora do ar).

```powershell
$env:SBI_DEMO_PASSWORD = '<senha das contas demo>'
$env:SBI_DB_PASSWORD = [guid]::NewGuid().ToString('N')
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\demo-olist.ps1 -Check
```

O script sobe a stack (`docker-compose.demo.yml`), espera a API aplicar o Flyway, carrega a massa Olist por SQL puro e imprime as contagens (60 clientes, 220 produtos, 219 pedidos). Detalhes em [bd/massa-demonstracao.md](bd/massa-demonstracao.md).

O roteiro é o mesmo, com estas trocas:
- **Endereço:** <http://localhost:3000> no lugar do site publicado.
- **Contas:** as do seed, com a senha de `SBI_DEMO_PASSWORD`: `allanbarros@sbi.tech` no lugar do Gabriel, `matheussiqueira@sbi.tech` (Matheus Siqueira) no lugar do Matheus e `franciscoferreira@sbi.tech` no lugar do Allan.
- **Banco:** IntelliJ ligado a `jdbc:postgresql://127.0.0.1:5433/sbi_demo` (usuário `sbi`, senha de `SBI_DB_PASSWORD`).
- **Passo 11:** pode rodar o contrato ao vivo com `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test-contract.ps1 -HttpOnly`.

Para encerrar: `docker compose -f docker-compose.demo.yml -p sbi-demo down`. O banco está em memória, então nada fica salvo.

Outros imprevistos:
- **Sem rede no laboratório:** para os passos 11 e 12, use o relatório gerado antes e o artefato baixado da CI.
- **Docker também não sobe:** apresente as capturas de `reports/marca/` (todas as telas dos dois papéis) e o relatório da CI.
- **Massa Olist não carregou:** os passos 4 e 10 usam um cliente do seed demo (por exemplo, Marina Costa) e os blocos de `docs/bd/pacote-sql/05_crud_exemplos.sql`.
