# Roteiro de demonstração (10 minutos)

## Antes de começar (5 minutos antes)

```powershell
$env:SBI_DEMO_PASSWORD = '<senha das contas demo>'
$env:SBI_DB_PASSWORD = [guid]::NewGuid().ToString('N')
docker compose -f docker-compose.demo.yml -p sbi-demo up --build -d --wait
```

Deixe abertas:
- duas janelas do navegador em <http://localhost:3000>, uma normal e uma anônima;
- um terminal na raiz do repositório.

Contas: `ana@loja.test` (vendedora) e `rafael@loja.test` (gerente), com a senha de `SBI_DEMO_PASSWORD`.

## Roteiro

| Min. | Passo | O que mostrar | O que dizer |
| --- | --- | --- | --- |
| 0:00 | 1. Login da vendedora | Janela normal: entrar como `ana@loja.test`. O menu mostra só Vender, Vendas, Clientes e Meu desempenho. | O menu vem das áreas que o servidor libera para a sessão; o cookie é HttpOnly. |
| 1:00 | 2. Venda com Pix | Em /vender, digitar "/" e um SKU, Enter, mais uma unidade com "+", e o total da cotação. Continuar, buscar o cliente pelo CPF, Pix, retirada, Finalizar venda, Confirmar pagamento recebido. | Preço, desconto e total vêm do servidor. A venda reserva o estoque e o pagamento dá a baixa. Um duplo clique não cria dois pedidos (Idempotency-Key). |
| 3:00 | 3. Acesso indevido | Abrir <http://localhost:3000/gestao> como Ana: aparece "Sem acesso a esta área". Abrir `/vendas/<id de um pedido de outro vendedor>`: aparece "Pedido não encontrado". | São três barreiras: URL, método e dono do dado. Pedido de outro vendedor responde 404, sem confirmar que existe. |
| 4:00 | 4. Gerente e baixa no estoque | Janela anônima: entrar como `rafael@loja.test`. Em /gestao/estoque, buscar o SKU vendido e mostrar saldo, reservado e disponível. | A saída aconteceu na confirmação do pagamento, com custo médio e movimento no extrato. |
| 5:00 | 5. Faturamento | Em /gestao, escolher 90 dias: faturamento líquido, lucro bruto (ou "—" com a nota de custo faltante), pedidos pagos, gráfico diário e "Atualizado às". | O painel atualiza a cada 15 s com a aba visível. Erro nunca vira zero. Lucro fica nulo quando falta custo. |
| 6:30 | 6. Preço com motivo | Em /gestao/produtos, clicar em Preço, informar o novo valor e um motivo, salvar e abrir o Histórico (autor e motivo). Abrir a venda do passo 2 em /vendas: o preço antigo continua. | Preço só muda pelo PATCH com motivo; o gatilho grava o histórico; a venda guarda o preço do momento. |
| 7:30 | 7. Contrato ao vivo | No terminal: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\test-contract.ps1 -HttpOnly`. Mostrar `reports/contrato/live-v4/live-contract.json` com `divergences: []`. | As 45 operações do v4 são chamadas de verdade, e cada resposta, inclusive de erro, é validada com Ajv contra o contrato. |
| 9:00 | 8. Job `bd` com 51 PASS | Abrir a última execução da CI (`gh run list --branch ci/full-pipeline`), job `bd`, e o artefato `resultado_bd.txt`. | Flyway V1–V7, seed demo, RBAC/RLS, CRUD e a corrida da última unidade (CC-01) passam por `psql`. |

## Plano B

- Sem rede no laboratório: rode o passo 7 antes da apresentação e mostre o relatório gerado. Para o passo 8, use o artefato baixado da CI.
- Se o Docker não subir: apresente as capturas `frontend/test-results/*.png` geradas pelos E2E e o relatório da CI.

## Encerrar

`docker compose -f docker-compose.demo.yml -p sbi-demo down`. O banco está em memória, então nada fica salvo.
