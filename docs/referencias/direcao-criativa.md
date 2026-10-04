# Refinamento criativo — sistema de gestão da loja

Data: 01/10/2026. Proposta visual; sistema e permissões ainda não implementados.

## Direção escolhida

Preservar o mockup 1: interface clara, navegação lateral, métricas legíveis e tabelas simples. Dar identidade própria por proporção, tipografia e um degradê discreto de azul, pervinca e lavanda. Usar o nome provisório já existente, Loja • Gestão. Não criar marca, slogan ou vínculo oficial com a Apple.

As quatro telas são partes da mesma proposta: Gestão, Meu desempenho, Vender e Estoque. Não são quatro alternativas para escolher.

## Sistema visual

- Tela de referência: desktop, 1440 × 1024.
- Fundo branco; navegação em cinza muito claro; texto principal quase preto.
- Paleta proposta: texto #18202B, secundário #596474, base #FFFFFF, superfície #F7F8FA, linha #E6E9EF, destaque #5468E8.
- Degradê proposto: #DDF3FA → #E3E7FC → #ECE3FA. Aplicar em uma faixa curta ou em detalhe de destaque; manter números sobre superfícies claras.
- Títulos com 28–32 px; corpo com 14–16 px; valores com 32–44 px e algarismos alinhados. Evitar números gigantes e títulos promocionais.
- Ritmo de espaço de 8 px; margens de 32 px; navegação de aproximadamente 220 px; cantos de 10–12 px, sem arredondar tudo.
- Ícones de traço uniforme; botões com verbo claro; um botão principal por tarefa.
- Gráficos com eixo, período e legenda claros. A linha da meta precisa ser identificada.
- Estados positivos em verde discreto; alerta em âmbar; erros em vermelho. Não usar cores de status apenas como decoração.
- Reduzir excesso de cartões, sombras, bordas e degradês. Tabelas e agrupamentos devem ocupar a mesma superfície.

## Áreas e acesso

| Área | Acesso | Dados |
|---|---|---|
| Gestão | Gerente | Loja, equipe, metas, estoque, custo de compra e pedidos |
| Meu desempenho | Vendedor autenticado | Somente sua meta, seu realizado e suas próprias vendas |
| Vender | Toda a equipe autenticada | Clientes novos ou existentes, histórico para atendimento, produtos disponíveis e criação de pedido |
| Estoque administrativo | Gerente | Entrada, saldo, reservado, disponível, custo de compra e preço de venda |

A área de vendas é compartilhada pela equipe, não pública. Cada pessoa usa sua própria conta. O servidor atribui o vendedor pela identidade autenticada; não confiar em um campo livre enviado pelo navegador. Consultas e respostas da API devem limitar dados por perfil. Ocultar um menu não substitui autorização.

O histórico compartilhado pode mostrar produtos, datas e estado do pedido para atender o cliente. Não deve expor custos, margens ou resultados dos colegas. Definir limites de alteração de descontos e de pedidos de terceiros antes da implementação.

## Dados demonstrativos preservados

Período completo: setembro de 2026. Data atual da proposta: 01/10/2026, America/Fortaleza. Todos os valores, preços e clientes são fictícios.

| Vendedor | Meta | Realizado | Atingimento |
|---|---:|---:|---:|
| Ana Souza | R$ 80.000 | R$ 68.400 | 85,5% |
| Bruno Lima | R$ 80.000 | R$ 62.000 | 77,5% |
| Carla Alves | R$ 80.000 | R$ 56.000 | 70,0% |
| Loja | R$ 240.000 | R$ 186.400 | 77,7% |

Faltam R$ 53.600 para a loja e R$ 11.600 para Ana. Atingimento = realizado ÷ meta × 100, com uma casa decimal. Meta zero deve aparecer como Sem meta, sem divisão por zero.

Evolução da loja por intervalo: 1–6 set R$ 38.200; 7–13 set R$ 41.600; 14–20 set R$ 36.800; 21–27 set R$ 42.300; 28–30 set R$ 27.500. Total R$ 186.400.

Venda demonstrativa: Marina Costa, cliente já cadastrada; iPhone 128 GB Preto, quantidade 1, preço fictício R$ 4.500; AirPods Branco, quantidade 1, preço fictício R$ 1.200. Subtotal e total sem desconto: R$ 5.700. Custo de compra nunca aparece ao vendedor.

## Regras propostas para implementação

- Meta mensal distribuída aos vendedores; soma da distribuição deve corresponder à meta da loja.
- Realizado considera vendas com pagamento confirmado, descontando estornos. Pedido pendente não conta.
- Confirmação do pagamento atualiza o realizado e o atingimento; não altera a meta definida.
- Pedido reserva estoque; confirmação converte reserva em saída; cancelamento libera reserva; devolução física confirmada permite entrada.
- Disponível = saldo físico − reservado. Não permitir duas reservas da mesma última unidade.
- Valores monetários calculados no servidor com decimal, preço unitário preservado no item e quantidade multiplicada uma única vez.
- Registrar custo por entrada/lote e custo atribuído à venda para preservar o histórico. Método de custo e regras de devolução precisam ser definidos na implementação.
- Histórico do cliente deriva dos pedidos, sem cadastro duplicado de vendas.

## Critérios para o refinamento visual

Gestão: resumo compacto, evolução no período, tabela de vendedores e alerta curto de estoque. Meu desempenho: resumo individual, progresso da própria meta e próprias vendas. Vender: sequência Cliente → Produtos → Pagamento, resumo de total sempre visível e apenas Criar pedido como ação principal. Estoque: tabela operacional, disponível separado de reservado, custo visível apenas no perfil de gestão.

## Entrega e limites

Imagens geradas pelo recurso nativo de geração de imagens, com referências dos mockups escolhidos. São referências de design, não telas executáveis. Texto, dados e estados apresentados como referência devem ser reproduzidos com componentes e dados determinísticos durante a implementação. Pequenas diferenças de tipografia e texto em imagens geradas não constituem regras do sistema.

O quadro interativo do Creative Production não pode ser chamado diretamente com as ferramentas expostas nesta conversa. Os resultados são entregues em arquivos locais e no chat. Nenhuma alteração é feita no backend durante esta etapa criativa.

## Arquivos entregues e conferência

- `gestao.png`: painel do gerente, com valores da loja e da equipe conferidos visualmente.
- `vendedor.png`: painel individual de Ana, sem resultados da loja ou de colegas. Primeira versão preservada em `vendedor-v1.png`.
- `venda.png`: atendimento compartilhado com cliente existente, cadastro de novo cliente, total de R$ 5.700 e texto corrigido sobre atualização do realizado. Variante preservada em `venda-variante.png`.
- `estoque.png`: saldo, reserva e disponível separados; custo não informado, sem inventar preço de compra.
- `review-board.html`: galeria local gerada pelo renderer compartilhado do Creative Production; não é o sistema funcionando.
- Prompts finais: `prompt-gestao.txt`, `vendedor-prompt.txt`, `prompt-venda.txt`, `prompt-estoque.txt`.

As quatro imagens foram abertas para inspeção, e os arquivos existem e têm conteúdo. A dimensão nativa resultante foi 1487 × 1058, na proporção das referências. A resolução proposta de 1440 × 1024 orienta o layout; não foi exigida exportação exata pelo usuário.

Conferidos: valores e somas demonstrativas, nomes dos perfis, separação visível de áreas, ausência de custo nas telas do vendedor, total da venda e disponibilidade do estoque. Gráficos e barras são ilustrações de layout: suas alturas e preenchimentos aproximados não devem ser extraídos como dados ou reutilizados como imagem na aplicação. A implementação precisa calcular as proporções a partir dos valores, particularmente as barras de atingimento dos vendedores.

Antes de implementar a tela de venda, substituir a seleção de etapa ilustrativa por estados coerentes com o preenchimento de Cliente, Produtos e Pagamento. Os controles desenhados nos mockups ainda não são funcionais.
