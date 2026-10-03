# Regras de dominio - Loja Gestao

Data: 03/10/2026.

Este documento inicia o card DOM-01. As decisoes abaixo sao propostas para revisao da equipe. Nenhuma regra deve ser tratada como implementada enquanto nao houver codigo, testes e contrato correspondentes.

## Decisoes propostas

| ID | Regra | Estado | Impacto |
| --- | --- | --- | --- |
| DOM-01 | Cada pessoa da equipe usa conta propria com perfil Gerente ou Vendedor. | Proposta | Autenticacao, auditoria e autorizacao. |
| DOM-02 | O vendedor de um pedido e sempre derivado da sessao autenticada. | Proposta aprovada para alvo | Evita falsificacao pelo navegador. |
| DOM-03 | Vendedor nao acessa custo, margem, realizado de colegas ou dados consolidados da loja. | Proposta aprovada para alvo | API deve omitir esses campos no JSON. |
| DOM-04 | Gerente acessa Gestao e Estoque administrativo. | Proposta | Define escopo da autorizacao. |
| DOM-05 | A area Vender e compartilhada por equipe autenticada, mas a autoria do pedido permanece individual. | Proposta | Permite atendimento sem expor dados sensiveis. |
| DOM-06 | Realizado considera pagamentos confirmados menos estornos. | Proposta aprovada para alvo | Base de metas e desempenho. |
| DOM-07 | Pedido pendente nao altera realizado. | Proposta aprovada para alvo | Evita inflar meta antes de pagamento. |
| DOM-08 | Meta zero exibe "Sem meta" e nao calcula porcentagem. | Proposta aprovada para alvo | Evita divisao por zero. |
| DOM-09 | Disponivel = saldo fisico menos reservado. | Proposta aprovada para alvo | Base de estoque. |
| DOM-10 | Criacao de pedido reserva estoque; pagamento confirmado converte reserva em saida; cancelamento libera reserva. | Proposta | Depende de API-03. |
| DOM-11 | Preco unitario do item deve ser historico; quantidade multiplica uma unica vez. | Proposta aprovada para alvo | Corrige risco de total duplicado. |
| DOM-12 | Valores monetarios usam tipo decimal adequado no servidor e no banco. | Proposta | Evita erro de ponto flutuante. |

## Decisoes pendentes

| ID | Decisao | Opcao inicial recomendada | Motivo |
| --- | --- | --- | --- |
| PEND-DOM-01 | Quem altera desconto? | Somente gerente; vendedor apenas sem desconto ou ate limite zero inicial. | Reduz risco de margem e auditoria no MVP. |
| PEND-DOM-02 | Pedido de outro vendedor pode ser alterado? | Gerente pode alterar/cancelar; vendedor pode consultar historico permitido, mas nao alterar pedido alheio. | Protege autoria e evita conflito de comissao/meta. |
| PEND-DOM-03 | Como registrar auditoria? | Registrar usuario, data/hora, operacao e antes/depois para desconto, cancelamento, estorno, entrada e preco. | Necessario para rastreabilidade e relatorio de qualidade. |
| PEND-DOM-04 | Metodo de custo por venda. | FIFO por lote como primeira escolha; media ponderada apenas se a equipe preferir simplicidade contabil. | FIFO preserva lote e custo historico com clareza para testes. |
| PEND-DOM-05 | Devolucao fisica. | Devolucao confirmada cria entrada vinculada ao pedido original e estorno reduz realizado. | Mantem estoque e meta consistentes. |
| PEND-DOM-06 | Arredondamento. | Arredondar dinheiro em 2 casas no limite de persistencia/resposta; porcentagem com 1 casa. | Alinha com guia visual e testes. |
| PEND-DOM-07 | Idempotencia de pagamento/estorno. | Usar identificador de evento ou impedir transicao repetida do mesmo pagamento. | Evita duplicar realizado e saida de estoque. |
| PEND-DOM-08 | Exclusao de pedidos/produtos. | Preferir cancelamento/inativacao em vez de delete fisico para dados que impactam historico. | Preserva evidencias e integridade. |

## Transicoes alvo

| Entidade | Estado inicial | Evento | Estado/efeito esperado |
| --- | --- | --- | --- |
| Pedido | `PENDING_PAYMENT` | pagamento confirmado | `PAID`; reserva vira saida; realizado aumenta. |
| Pedido | `PENDING_PAYMENT` | cancelamento | `CANCELED` futuro ou estado equivalente; reserva liberada. |
| Pagamento | `PENDING` | processamento aprovado | `APPROVED`; data de pagamento preenchida. |
| Pagamento | `APPROVED` | estorno aprovado | `REFUNDED` futuro ou registro de estorno; realizado diminui. |
| Estoque | saldo com disponivel | pedido criado | reservado aumenta e disponivel diminui. |
| Estoque | reservado | pagamento confirmado | reservado diminui e saldo fisico diminui. |
| Estoque | reservado | cancelamento | reservado diminui e disponivel aumenta. |

O enum atual de pedido nao possui `CANCELED` e o enum atual de pagamento nao possui `REFUNDED`. A equipe deve decidir se adiciona novos estados ou cria registros/eventos separados antes da implementacao.

## Impacto em contrato e banco

| Area | Mudanca esperada |
| --- | --- |
| Usuarios/perfis | Tabelas ou seed de usuarios, perfil e credenciais de teste. |
| Pedidos | Vendedor historico, possivel status de cancelamento e campos de auditoria. |
| Itens | Preco unitario historico, subtotal calculado e possivel custo atribuido. |
| Metas | Periodo, loja, vendedor, valor de meta e validacao da soma. |
| Pagamentos | Eventos ou status para confirmacao, estorno e idempotencia. |
| Estoque | Saldo fisico, reservado, entradas/lotes, custo, preco vigente e auditoria. |
| API | Respostas diferentes por perfil; campos sensiveis ausentes para vendedor. |

## Perguntas para revisao

- A equipe aceita gerente como unico perfil com permissao de desconto no MVP?
- A equipe prefere FIFO por lote ou media ponderada para custo?
- Sera criado status `CANCELED` para pedido e `REFUNDED` para pagamento?
- O professor espera modelagem relacional detalhada de estoque/custo para a rubrica de Banco de Dados?
- As decisoes de dominio serao aplicadas antes ou depois da exportacao do contrato baseline?
