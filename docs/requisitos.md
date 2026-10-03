# Requisitos - Loja Gestao

Data: 03/10/2026.

Este documento separa o que foi observado no backend atual da Order Management API e o que passa a ser alvo do produto Loja Gestao. O baseline atual possui API REST para clientes, produtos, enderecos, pedidos e pagamentos simulados. Ainda nao existem autenticacao, frontend, metas, estoque operacional, reserva, custo historico ou autorizacao por perfil.

## Escopo

O trabalho academico continua tendo como eixo a conformidade de contrato da API REST e suas evidencias de qualidade. A evolucao planejada adiciona uma camada de loja com quatro areas: Gestao, Meu desempenho, Vender e Estoque administrativo.

Status usados neste documento:

| Status | Significado |
| --- | --- |
| Observado | Existe no codigo local ou em documento de referencia. |
| Alvo | Deve ser implementado ou especificado para a evolucao Loja Gestao. |
| Parcial | Existe parte do comportamento, mas falta regra relevante. |
| Pendente | Depende de decisao da equipe, professor, stack ou card anterior. |

## Baseline observado

| Area | Comportamento atual |
| --- | --- |
| Clientes | CRUD em `/clients`, com pessoa fisica ou juridica. |
| Produtos | CRUD em `/products`, com produto fisico ou digital. |
| Enderecos | CRUD em `/address`. |
| Pedidos | Criacao de pedido em `/orders`, com cliente existente ou novo, itens, endereco e pagamento. |
| Pagamentos | CRUD em `/payment`, processamento em `/payment/{id}/process` e status simulado. |
| Erros | Handler global padroniza erros de validacao, negocio, nao encontrado, conflito e erro inesperado. |

## Requisitos funcionais

| ID | Requisito | Status | Origem / observacao |
| --- | --- | --- | --- |
| RF-01 | O sistema deve autenticar usuarios da equipe com perfil Gerente ou Vendedor. | Alvo | Necessario para as quatro areas e para impedir acesso publico. |
| RF-02 | O servidor deve autorizar cada operacao por perfil e identidade da sessao. | Alvo | Ocultar menu no frontend nao substitui autorizacao HTTP. |
| RF-03 | O sistema deve manter cadastro de clientes pessoas fisicas e juridicas. | Observado | CRUD atual em `/clients`; validar campos especificos por tipo. |
| RF-04 | O sistema deve manter catalogo de produtos fisicos e digitais. | Observado | CRUD atual em `/products`; estoque/custo ficam em RF especificos. |
| RF-05 | O sistema deve criar pedidos para cliente existente ou cliente novo, com itens, endereco e pagamento. | Observado | Criacao atual em `/orders`. |
| RF-06 | O sistema deve atribuir o vendedor do pedido pela sessao autenticada. | Alvo | O navegador nao deve enviar vendedor livremente. |
| RF-07 | O sistema deve calcular valores monetarios no servidor, preservando preco unitario historico e multiplicando quantidade uma unica vez. | Parcial | Defeito de quantidade duplicada foi reproduzido e corrigido em 03/10/2026 com teste unitario. Ainda faltam decimal adequado para dinheiro e cobertura de historico de preco. |
| RF-08 | O sistema deve processar pagamento simulado e refletir o estado do pedido. | Parcial | Processamento existe; faltam estorno, idempotencia e regras para realizado. |
| RF-09 | O sistema deve registrar meta mensal da loja e distribuicao por vendedor, com soma igual a meta da loja. | Alvo | Base do painel de Gestao e Meu desempenho. |
| RF-10 | O sistema deve calcular realizado como vendas com pagamento confirmado menos estornos. | Alvo | Pedido pendente nao conta; meta zero deve exibir "Sem meta". |
| RF-11 | O vendedor deve acessar somente sua meta, seu realizado e suas proprias vendas. | Alvo | Inclui bloqueio por ID de colega tambem via HTTP. |
| RF-12 | O gerente deve acessar visao consolidada da loja, equipe, metas e alertas de estoque. | Alvo | Dados devem derivar da API, nao de numeros fixos de interface. |
| RF-13 | O sistema deve reservar estoque ao criar pedido, converter reserva em saida no pagamento confirmado e liberar reserva no cancelamento. | Alvo | Disponivel = saldo fisico menos reservado. |
| RF-14 | O gerente deve registrar entradas/lotes com quantidade, custo e preco de venda, preservando custo historico da venda. | Alvo | Custo/margem nao devem aparecer para vendedor nem no JSON. |
| RF-15 | A area Vender deve permitir atendimento de cliente novo ou existente, selecao de produtos disponiveis e criacao do pedido. | Alvo | Total sempre calculado no servidor. |
| RF-16 | A interface deve oferecer as areas Gestao, Meu desempenho, Vender e Estoque conforme perfil autenticado. | Alvo | Depende de stack e API. |

## Requisitos nao funcionais

| ID | Requisito | Status | Criterio verificavel inicial |
| --- | --- | --- | --- |
| RNF-01 | Acesso minimo por perfil. | Alvo | Rotas restritas retornam 401 sem sessao e 403 para perfil indevido. |
| RNF-02 | Integridade transacional. | Alvo | Reserva, saida, cancelamento e pagamento nao deixam estoque ou pedido em estado parcial. |
| RNF-03 | Precisao monetaria. | Alvo | Valores usam decimal adequado para dinheiro e preservam historico de preco/custo. |
| RNF-04 | Reprodutibilidade de qualidade. | Alvo | Ambiente, seed, versoes, comandos e relatorios ficam versionados. |
| RNF-05 | Usabilidade e acessibilidade basica. | Alvo | Estados vazio, carregando, erro e sucesso; teclado, foco e contraste conferidos. |
| RNF-06 | Desempenho local sob protocolo definido. | Pendente | Medicao p95 somente apos endpoints, seed e ambiente estaveis. |
| RNF-07 | Rastreabilidade academica. | Alvo | Cada RF liga requisito, criterio de aceitacao, caso de teste, execucao e defeito. |

## Dados demonstrativos preservados

Periodo demonstrativo: setembro de 2026, fuso America/Fortaleza. Valores sao ficticios.

| Vendedor | Meta | Realizado | Atingimento |
| --- | ---: | ---: | ---: |
| Ana Souza | R$ 80.000 | R$ 68.400 | 85,5% |
| Bruno Lima | R$ 80.000 | R$ 62.000 | 77,5% |
| Carla Alves | R$ 80.000 | R$ 56.000 | 70,0% |
| Loja | R$ 240.000 | R$ 186.400 | 77,7% |

Venda demonstrativa: Marina Costa, iPhone 128 GB Preto por R$ 4.500 e AirPods Branco por R$ 1.200, total R$ 5.700 sem desconto.

## Pendencias

| Pendencia | Impacto |
| --- | --- |
| Confirmar grupo de quatro, rubrica de Banco de Dados e prazo com professores. | Pode alterar escopo e criterios finais. |
| Definir stack de frontend, sessao, CORS e ferramenta E2E. | Bloqueia implementacao UI-01 a UI-05. |
| Decidir regras de desconto, pedido de terceiros, custeio, estorno e devolucao. | Bloqueia regras de pedido, metas, estoque e aceitacao. |
| Definir modelo de banco evolutivo. | Bloqueia API-02, API-04 e trilha BD. |
| Automatizar banco isolado para testes. | Hoje a suite completa depende do PostgreSQL local/container `postgres-order`; Testcontainers ou Compose versionado reduzem atrito. |
