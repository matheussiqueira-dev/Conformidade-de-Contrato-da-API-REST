# Roteiro de demonstração (12 minutos)

## Antes de começar (10 minutos antes)

```powershell
$env:SBI_DEMO_PASSWORD = '<senha das contas demo>'
$env:SBI_DB_PASSWORD = [guid]::NewGuid().ToString('N')
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\demo-olist.ps1 -Check
```

O script sobe a stack (`docker-compose.demo.yml`), espera a API aplicar o Flyway, carrega a massa Olist por SQL puro e imprime as contagens (60 clientes, 220 produtos, 219 pedidos). Sem a massa, `docker compose -f docker-compose.demo.yml -p sbi-demo up --build -d --wait` sobe só o seed demo. Detalhes em [bd/massa-demonstracao.md](bd/massa-demonstracao.md).

Deixe abertas:
- três janelas do navegador em <http://localhost:3000>: uma normal, uma anônima e uma de outro perfil do navegador;
- o IntelliJ com o painel Database ligado a `jdbc:postgresql://127.0.0.1:5433/sbi_demo` (usuário `sbi`, senha de `SBI_DB_PASSWORD`) e o arquivo `docs/bd/pacote-sql/08_crud_clientes_olist.sql` aberto;
- um terminal na raiz do repositório.

Contas: `ana@loja.test` e `bruno@loja.test` (vendedores) e `rafael@loja.test` (gerente, o único da loja), com a senha de `SBI_DEMO_PASSWORD`.

## Roteiro

| Min. | Passo | O que mostrar | O que dizer |
| --- | --- | --- | --- |
| 0:00 | 1. Login da vendedora | Janela normal: tela "Entrar no SBI", entrar como `ana@loja.test`. O menu mostra ATENDIMENTO (Vender, Vendas, Clientes) e MEU ESPAÇO (Meu desempenho, Trocar senha), sem GESTÃO. | O menu vem das áreas que o servidor libera para a sessão; o cookie é HttpOnly. A interface segue o manual de marca SBI v1.0. |
| 1:00 | 2. Venda com Pix e desconto | Em Vender ("Nova venda", responsável Ana), digitar "/" e o SKU `CG-20W`, Enter, mais uma unidade com "+": 2 × R$ 200,00 com a promoção de 10% em acessórios = R$ 360,00. Tentar desconto manual de 10%: "Desconto acima do seu limite". Aplicar 5% com motivo. Continuar, buscar o cliente pelo CPF, Pix, retirada, Criar pedido, Confirmar pagamento recebido. | Preço, desconto e total vêm do servidor. A alçada do vendedor é 5% e a do gerente 20%; acima disso, o gerente aprova no pedido. A venda reserva o estoque e o pagamento dá a baixa. Um duplo clique não cria dois pedidos (Idempotency-Key). |
| 2:30 | 3. Acesso indevido | Abrir <http://localhost:3000/gestao> como Ana: aparece "Sem acesso a esta área". Abrir `/vendas/<id de um pedido de outro vendedor>`: aparece "Pedido não encontrado". | São três barreiras: URL, método e dono do dado. Pedido de outro vendedor responde 404, sem confirmar que existe. |
| 3:15 | 4. Cliente recorrente | Em Clientes, buscar "Larissa Esteves" e abrir a da massa Olist (e-mail `larissa.cli-001@example.com`, 14 pedidos): indicadores, abas Pedidos e Produtos mais comprados com o teclado, documento mascarado. | Para a vendedora, total e ticket médio contam só as vendas dela; o gerente vê a loja toda. O histórico vem de pedidos reais anonimizados da Olist, com nomes e documentos sintéticos. |
| 4:15 | 5. Gerente e baixa no estoque | Janela anônima: entrar como `rafael@loja.test`. Em Estoque, buscar `CG-20W` e mostrar saldo, reservado e disponível. | A saída aconteceu na confirmação do pagamento, com custo médio e movimento no extrato. |
| 5:00 | 6. Visão geral | Em Visão geral, escolher 90 dias: faturamento líquido, lucro bruto (ou "Não informado" com a nota de custo faltante), pedidos pagos, gráfico diário, "Atualizado às" e o cartão da meta com a linha tracejada. | Somente pagamentos confirmados, descontados os estornos. O painel atualiza a cada 15 s com a aba visível; erro nunca vira zero; lucro fica nulo quando falta custo. |
| 6:00 | 7. Preço com motivo | Em Produtos e preços, clicar em Preço, informar o novo valor e um motivo, salvar e abrir o Histórico (autor e motivo). Abrir a venda do passo 2 em Vendas: o preço antigo continua. | Preço só muda pelo PATCH com motivo; o gatilho grava o histórico; a venda guarda o preço do momento. |
| 6:45 | 8. Desativar vendedor e ver a sessão cair | Na terceira janela, entrar como `bruno@loja.test`. Como Rafael, em Usuários, Desativar o Bruno com motivo. Na janela do Bruno, clicar em qualquer item do menu: volta ao login com "Sua sessão foi encerrada porque o seu acesso mudou. Entre novamente." Ativar o Bruno de novo. | Mudar papel, situação ou senha incrementa a versão da sessão do usuário; o filtro da API recusa a próxima requisição com 401 `SESSION_REVOKED`. Não é preciso esperar a sessão expirar. |
| 7:45 | 9. Tentar rebaixar o último gerente | Em Usuários, mostrar que a linha "Rafael Mendes (você)" não tem ação de papel nem de situação. No IntelliJ, executar `BEGIN; SET CONSTRAINTS ALL IMMEDIATE; UPDATE app_users SET role = 'VENDEDOR' WHERE email = 'rafael@loja.test'; ROLLBACK;`: o banco responde `LAST_MANAGER: at least one active manager is required`. | Duas proteções: a API proíbe alterar a própria conta (409 `SELF_CHANGE_FORBIDDEN`) e o gatilho `keep_one_active_manager` garante um gerente ativo mesmo com dois gerentes se rebaixando ao mesmo tempo (409 `LAST_MANAGER`). |
| 8:30 | 10. SQL e CRUD no banco | No IntelliJ, executar `08_crud_clientes_olist.sql` por blocos: cadastro de PF e de PJ com CNPJ alfanumérico, as recusas do banco, histórico da Larissa, `v_cliente_resumo`, ranking de clientes e exclusão recusada pela FK. Tudo termina em `ROLLBACK`. | A massa entrou por SQL puro (`\copy`, `INSERT ... SELECT`, uma transação) e o banco aceita CRUD de qualquer cliente SQL; CHECKs, FKs e gatilhos barram o que a API também barra. |
| 10:00 | 11. Contrato ao vivo | No terminal: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test-contract.ps1 -HttpOnly`. Mostrar `reports/contrato/live-v5/live-contract.json` com `divergences: []`. | As 58 operações do contrato v5 são chamadas de verdade, e cada resposta, inclusive de erro, é validada com Ajv contra o contrato. |
| 11:15 | 12. Job `bd` | Abrir a última execução da CI da `main` (`gh run list --branch main --limit 1`), job `bd`, e o artefato `bd-reports/resultado_bd.txt` (`gh run download <id> -n bd-reports`): 60 PASS e, na etapa Olist, 10 PASS. | Flyway V1–V11, seed demo, RBAC/RLS, CRUD, a corrida da última unidade (CC-01), o último gerente (BD-30), as alçadas (BD-32) e a massa Olist (CT-DEMO) passam por `psql`. |

## Plano B

- Sem rede no laboratório: rode o passo 11 antes da apresentação e mostre o relatório gerado. Para o passo 12, use o artefato baixado da CI.
- Se o Docker não subir: apresente as capturas de `reports/marca/` (todas as telas dos dois papéis) e o relatório da CI.
- Se a massa Olist não carregar: os passos 4 e 10 usam um cliente do seed demo (por exemplo, Marina Costa) e os blocos de `docs/bd/pacote-sql/05_crud_exemplos.sql`.

## Encerrar

`docker compose -f docker-compose.demo.yml -p sbi-demo down`. O banco está em memória, então nada fica salvo.
