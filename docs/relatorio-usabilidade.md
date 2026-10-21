# Relatório de usabilidade — SBI

Situação em 20/10/2026, na branch `feat/ux-usabilidade` (ainda sem commit). O relatório cobre o frontend (`frontend/`), que é onde vendedor e gerente trabalham; a API só entrou quando uma mensagem ou regra da tela dependia dela.

## 1. Escopo e método

- **Quem usa e para quê.** Três perfis, descritos como trabalho a ser feito:
  - *Vendedor no balcão*: fechar a venda depressa e sem erro, com o cliente na frente. Precisa ver o total antes de cobrar e não pode perder o que já digitou.
  - *Gerente*: acompanhar a meta e agir sobre exceções (estoque abaixo do mínimo, pedidos pendentes, descontos acima da alçada, contas bloqueadas). Precisa saber que cada alteração foi salva.
  - *Quem abre o site publicado pela primeira vez* (avaliador da banca, colega de equipe): entrar sem instalar nada. O servidor gratuito dorme sem acesso e a primeira entrada pode levar de 1 a 3 minutos.
- **Como foi feito.** Avaliação heurística e auditoria de fluxos em cinco camadas, adaptada a Next.js: inventário dos pontos de entrada (botões, links, painéis, diálogos), rastreio dos fluxos críticos (venda, confirmação de pagamento, estoque, preço, promoção, usuários), detecção de problemas por categoria (ação destrutiva sem confirmação, estado perdido, falta de retorno, promessa que leva ao lugar errado, estado vazio sem saída), avaliação pelo ponto de vista de cada perfil e conferência de que cada tela usa os dados reais da API. A revisão de texto seguiu quatro critérios (útil, conciso, natural, claro) e o padrão "o que houve + por quê + o que fazer" nas mensagens de erro.
- **Limite.** Não houve sessão com usuários reais. Os achados vêm da leitura do código e da execução das telas; a seção 8 traz o roteiro para validar com pessoas.

## 2. Resumo

Antes das mudanças, as telas já tinham boa base: marca aplicada, WCAG 2.2 AA verificado pelo axe, teclado no PDV, erro de API nunca exibido como zero e mensagens por código de erro. Os problemas encontrados estavam nas bordas dos fluxos:

1. três ações sem volta executavam no primeiro clique (cancelar pedido, encerrar promoção, inativar produto);
2. a etapa de pagamento do PDV não mostrava o total, e voltar aos itens apagava cliente, pagamento e endereço;
3. recarregar o PDV apagava o desconto manual digitado;
4. painéis do gerente fechavam sem dizer que a alteração foi salva;
5. mensagens de validação mostravam nomes internos (`temporaryPassword: Mínimo: 12.`);
6. no ambiente publicado, a espera do servidor acordando não tinha explicação, e o 502/504 virava "Algo deu errado do nosso lado";
7. na demonstração local por Docker (plano B da apresentação), todas as fotos de produto e ícones apareciam quebrados: a imagem do frontend não levava a pasta `public/`.

Foram corrigidos 24 achados e 5 recomendações da primeira rodada (U-25 a U-29); 6 ficam como recomendação (seção 6). A partida a frio da API também caiu de cerca de 280 s para 67 s em 0,1 CPU com o PR #43 (cache AOT), que vale para o Render e para a stack local.

## 3. Achados e situação

Escala: urgência, risco da correção (chance de quebrar algo), risco sem correção (efeito para quem usa), retorno, arquivos tocados e esforço.

| # | Achado | Urgência | Risco da correção | Risco sem correção | Retorno | Arquivos | Esforço | Situação |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| U-01 | "Cancelar pedido" cancelava no primeiro clique, no PDV e no detalhe da venda; o pedido não pode ser reaberto | Crítica | Baixo | Crítico | Excelente | 3 | Pequeno | Corrigido |
| U-02 | "Encerrar" promoção sem confirmação; encerrada não volta | Crítica | Baixo | Alto | Excelente | 2 | Pequeno | Corrigido |
| U-03 | "Inativar" produto sem confirmação; o produto some da busca do PDV | Alta | Baixo | Alto | Excelente | 1 | Pequeno | Corrigido |
| U-04 | Etapa "Cliente e pagamento" sem itens nem total: o vendedor criava o pedido sem ver o valor | Alta | Baixo | Alto | Excelente | 2 | Pequeno | Corrigido |
| U-05 | "Voltar aos itens" apagava cliente, forma de pagamento, parcelas e endereço já preenchidos | Alta | Médio | Alto | Excelente | 1 | Pequeno | Corrigido |
| U-06 | Recarregar o PDV apagava o desconto manual (o carrinho salvo guardava, a leitura ignorava) | Alta | Baixo | Médio | Excelente | 2 | Trivial | Corrigido, com teste |
| U-07 | Nenhum aviso de sucesso em estoque, produtos, promoções, alçada, usuários, edição de cliente e detalhe da venda | Média | Baixo | Médio | Bom | 9 | Médio | Corrigido |
| U-08 | Validação com nome técnico do campo (`price.amount`, `temporaryPassword`) em produtos, promoções e usuários | Alta | Baixo | Médio | Excelente | 4 | Pequeno | Corrigido, com teste |
| U-09 | Mensagens genéricas "Mínimo: 2." e "Formato inválido." sem dizer a unidade | Média | Baixo | Baixo | Bom | 1 | Trivial | Corrigido |
| U-10 | Site publicado: login parado em "Entrando…" por até 3 min sem explicação; 502/503/504 viravam "Algo deu errado do nosso lado" | Alta | Baixo | Alto | Excelente | 4 | Pequeno | Corrigido, com teste |
| U-11 | Filtro e página de Vendas se perdiam ao abrir uma venda e voltar | Média | Baixo | Médio | Bom | 2 | Pequeno | Corrigido |
| U-12 | Painel: "Pendentes de pagamento" sem caminho para os pedidos; "Abrir estoque" abria a lista inteira, não os itens abaixo do mínimo | Média | Baixo | Médio | Bom | 3 | Pequeno | Corrigido |
| U-13 | Depois de criar, pagar ou cancelar, Vendas, painel e estoque mostravam dados antigos por até 10 s | Média | Baixo | Médio | Bom | 3 | Pequeno | Corrigido |
| U-14 | Sem como conferir a senha digitada (login, troca de senha, senha provisória entregue pelo gerente) | Média | Baixo | Médio | Bom | 4 | Pequeno | Corrigido |
| U-15 | Botão genérico "Salvar" em entrada, ajuste e mínimo do estoque | Baixa | Baixo | Baixo | Bom | 2 | Trivial | Corrigido |
| U-16 | Ajuste de entrada exigia escolher "Inventário" à mão, o único motivo aceito | Baixa | Baixo | Baixo | Bom | 1 | Trivial | Corrigido |
| U-17 | Produto sem estoque aparecia como "0 disp." no PDV | Baixa | Baixo | Baixo | Bom | 1 | Trivial | Corrigido |
| U-18 | Busca de cliente no PDV sem orientação (mínimo de 2 letras; documento só completo) | Baixa | Baixo | Baixo | Bom | 1 | Trivial | Corrigido |
| U-19 | Estados vazios sem próxima ação ("Nenhuma venda encontrada.", "Nenhuma promoção.") | Baixa | Baixo | Baixo | Bom | 2 | Trivial | Corrigido |
| U-20 | Conflito de edição do cliente: "Os dados mudaram; recarregue." sem dizer o que aconteceu | Baixa | Baixo | Baixo | Bom | 1 | Trivial | Corrigido |
| U-21 | Pedido Pix ou cartão pendente: ao clicar "Nova venda", nada dizia que ele continua reservado em Vendas | Baixa | Baixo | Baixo | Bom | 1 | Trivial | Corrigido |
| U-22 | Meu desempenho sem caminho para as vendas nem para uma nova venda | Baixa | Baixo | Baixo | Bom | 2 | Trivial | Corrigido |
| U-23 | Exemplo do e-mail no login com domínio diferente das contas (`voce@loja.com`) | Baixa | Baixo | Baixo | Bom | 1 | Trivial | Corrigido |
| U-25 | Trocar de etapa no PDV deixava o foco no corpo da página; o leitor de tela não anunciava a etapa nova (antes P-03) | Média | Baixo | Médio | Bom | 2 | Pequeno | Corrigido, com E2E |
| U-26 | Atalhos da busca do PDV (setas, Enter, leitor de código) não apareciam na tela (antes P-05) | Baixa | Baixo | Baixo | Bom | 1 | Trivial | Corrigido |
| U-27 | Abrir e fechar `<dialog>` repetido em quatro lugares (antes P-10) | Baixa | Baixo | Baixo | Bom | 5 | Pequeno | Corrigido: `components/use-modal.ts` |
| U-28 | Cadastro com documento já existente só dizia "Já existe um cliente com este documento." e o vendedor tinha de buscar o cliente de novo (antes P-02) | Média | Baixo | Médio | Bom | 6 | Pequeno | Corrigido: o 409 traz `customerId` (D-EVO-40) e a tela oferece "Usar o cliente já cadastrado"; integração e E2E CT-WEB-128 |
| U-29 | Sem como acordar o servidor publicado antes de a pessoa terminar de digitar (antes P-04) | Média | Baixo | Médio | Bom | 2 | Trivial | Corrigido atrás de `NEXT_PUBLIC_SBI_AQUECER=1`, desligado por padrão (D-EVO-41, `docs/deploy.md` seção 6) |
| U-24 | Stack `docker-compose.demo.yml`: `frontend/Dockerfile` não copiava `public/`, e `next start` respondia 404 para fotos, ícones das linhas e marca | Alta | Baixo | Alto | Excelente | 1 | Trivial | Corrigido |

## 4. O que mudou, por tela

| Tela | Mudança | Arquivos |
| --- | --- | --- |
| Entrar | "Mostrar senha"; depois de 8 s esperando aparece "O servidor pode levar até 3 minutos para acordar na primeira entrada. Aguarde nesta página."; e-mail sem corretor ortográfico | `app/entrar/login-form.tsx` |
| Falha ao carregar | Explica que o servidor pode estar iniciando e oferece nova tentativa | `app/error.tsx` |
| Nova venda | Resumo com itens, descontos e total ao lado de "Criar pedido"; a etapa de pagamento continua montada ao voltar aos itens; "Sem estoque" no resultado; dica na busca de cliente; confirmação antes de cancelar; nota sobre o pedido pendente | `app/(app)/vender/sale-point.tsx` |
| Venda (detalhe) | Confirmação antes de cancelar; aviso depois de confirmar pagamento, cancelar ou aplicar desconto | `app/(app)/vendas/[id]/sale-detail.tsx` |
| Vendas | Situação e página no endereço (`?situacao=PENDING_PAYMENT&pagina=2`), sem nova chamada ao servidor; estados vazios com ação | `app/(app)/vendas/page.tsx`, `sales-list.tsx` |
| Visão geral | "Ver pedidos pendentes" e "Ver produtos abaixo do mínimo" levam às listas já filtradas | `app/(app)/gestao/dashboard.tsx` |
| Estoque | `?abaixo=1` abre filtrado; botões "Registrar entrada", "Registrar ajuste", "Salvar mínimo"; Inventário escolhido sozinho no ajuste de entrada; aviso de sucesso | `app/(app)/gestao/estoque/` |
| Produtos | Confirmação antes de inativar; erros com o rótulo do campo; aviso depois de cadastrar, editar, mudar preço, ativar e inativar | `app/(app)/gestao/produtos/products.tsx` |
| Promoções | Confirmação antes de encerrar; erros com rótulo; aviso depois de liberar, encerrar e mudar alçada | `app/(app)/gestao/promocoes/` |
| Usuários | Erros com rótulo; "Mostrar senha" na senha provisória; aviso depois de cada ação | `app/(app)/gestao/usuarios/users.tsx` |
| Cliente | Aviso depois de salvar; texto do conflito de edição; lista de clientes atualizada | `app/(app)/clientes/[id]/customer-profile.tsx` |
| Meu desempenho | Links para as vendas e para nova venda | `app/(app)/meu-desempenho/` |
| Troca de senha | "Mostrar as senhas" para os três campos | `app/conta/senha/password-form.tsx` |

Peças compartilhadas novas: `components/confirm-dialog.tsx`, `components/status-message.tsx`, `components/show-password.tsx`, `lib/api/invalidate.ts`, `labeledProblems` em `lib/forms.ts` e `STARTING_MESSAGE` e `SLOW_START_HINT` em `lib/api/messages.ts`.

## 5. Texto da interface: antes e depois

| Onde | Antes | Depois |
| --- | --- | --- |
| Erro de produto | `sku: Preencha este campo.` e `sku: Formato inválido.` | `SKU: preencha este campo.` (um problema por campo) |
| Erro de usuário | `temporaryPassword: Mínimo: 12.` | `Senha provisória: use pelo menos 12 caracteres.` |
| Campo de texto curto | `Mínimo: 2.` | `Use pelo menos 2 caracteres.` |
| Formato | `Formato inválido.` | `Confira o formato deste campo.` |
| Servidor fora do ar (502/503/504) | `Algo deu errado do nosso lado. Tente novamente.` | `O sistema está iniciando ou indisponível no momento. Aguarde um minuto e tente de novo.` |
| Falha ao montar a tela | `Não foi possível carregar. Tente de novo.` | `Não foi possível carregar esta tela.` + `Se o sistema acabou de ser aberto, o servidor pode estar iniciando: aguarde um minuto e tente de novo.` |
| Conflito de edição | `Os dados mudaram; recarregue.` | `Outra pessoa alterou este cliente enquanto você editava. Recarregue para ver os dados atuais e edite de novo.` |
| Botões do estoque | `Salvar` | `Registrar entrada`, `Registrar ajuste`, `Salvar mínimo` |
| Vendas vazia | `Nenhuma venda encontrada.` | Sem filtro: `Você ainda não registrou vendas.` + `Nova venda`. Com filtro: `Nenhuma venda com a situação "Pago".` + `Ver todas as vendas` |
| Confirmação de cancelamento | (não existia) | `Cancelar o pedido #123?` · `A reserva de estoque é liberada e o pedido não pode ser reaberto.` · `Manter pedido` / `Cancelar pedido` |

Regras usadas: botão com verbo e objeto; confirmação com a ação no título e o efeito no texto, e botões com o nome da ação (nunca "OK"); depois dos dois-pontos a frase segue em minúscula; mensagens de status no passado ("Entrada registrada"); avisos sobre pessoas sem marcar gênero ("Acesso de Allan Barros desativado").

## 6. Recomendações não implementadas

| # | Recomendação | Por que ficou para depois |
| --- | --- | --- |
| P-01 | Filtrar Vendas por vendedor, período e cliente | Precisa de parâmetros novos em `GET /sales/orders` (contrato v5, verificação ao vivo e matriz de acesso) |
| P-06 | Linha inteira da tabela como área de clique | Exige cuidado com os botões da própria linha |
| P-07 | Voltar ao preço anterior direto do histórico | Regra de negócio: toda mudança de preço exige motivo |
| P-08 | Tema escuro | O manual de marca v1.0 define só o tema claro |
| P-09 | Teste com pessoas reais | Roteiro na seção 8 |
| P-11 | Invalidação depois da venda por lista de chaves (`lib/api/invalidate.ts`) | Chave nova de tela precisa entrar na lista; alternativa é prefixar as chaves por domínio |

## 7. Especificação dos componentes novos

Tokens: `brand-tokens.css` (manual v1.0). Nenhuma animação: o manual pede telas sem movimento.

### 7.1 Diálogo de confirmação (`ConfirmDialog`)

| Item | Especificação |
| --- | --- |
| Uso | Antes de ação sem volta: cancelar pedido, encerrar promoção, inativar produto |
| Estrutura | `<dialog role="alertdialog">` aberto com `showModal()`; título (`h2`, 20/28), texto, botões à direita |
| Medidas | Largura `min(440px, 100% - 32px)`, padding `--space-3`, cantos `--radius-card`, borda `--line`, fundo escurecido `rgba(20, 27, 38, 0.4)` |
| Botões | "Manter …" (`.btn`, recebe o foco ao abrir) e a ação (`.btn-danger-solid`, fundo `--err-ink`, texto branco, contraste 6,4:1; hover mais escuro) |
| Teclado | Esc e o gesto de voltar fecham sem executar; Tab fica preso no diálogo; o foco volta para quem abriu |
| Leitor de tela | Nome pelo título (`aria-labelledby`) e descrição pelo texto (`aria-describedby`) |
| Depois de confirmar | O diálogo fecha, o botão original mostra "Cancelando…" e o resultado aparece como aviso ou erro na tela |
| Texto | Título com a ação e o objeto ("Encerrar a promoção "Acessórios 10%"?"); texto com o efeito e se dá para desfazer |

### 7.2 Aviso de sucesso (`StatusMessage` + `useStatusMessage`)

| Item | Especificação |
| --- | --- |
| Uso | Depois de salvar em painel lateral ou de mudar a situação de um pedido |
| Estrutura | Região `role="status"` sempre montada (o leitor anuncia quando o texto muda); dentro, `.notice.notice-ok` com o texto e "Fechar aviso" |
| Cores | `--ok-bg` / `--ok-ink` (verde do manual, sempre com texto) |
| Vida útil | Fica até a próxima ação ou até "Fechar aviso"; não some sozinho (WCAG 2.2.1) |
| Texto | Passado + o que mudou + efeito quando importa ("Preço de Capa alterado para R$ 320,00. Vendas já feitas mantêm o preço antigo.") |
| Vazio | `.status-slot:empty` não ocupa espaço na grade |

### 7.3 Mostrar senha (`ShowPassword`)

| Item | Especificação |
| --- | --- |
| Uso | Login, troca de senha (uma caixa para os três campos) e senha provisória do gerente |
| Estrutura | Caixa de seleção com rótulo "Mostrar senha" abaixo do campo; marcada, o campo vira `type="text"` |
| Padrão | Desmarcada a cada abertura; desabilitada durante o envio |
| Alvo | 44 px de altura; caixa de 18 px com `accent-color: --primary` |

### 7.4 Resumo da venda (PDV, etapa 2)

| Item | Especificação |
| --- | --- |
| Posição | No cartão "Pagamento e entrega", logo acima de "Criar pedido", separado por `--line` |
| Conteúdo | Uma linha por item ("2× Carregador 20W" e o subtotal), descontos e total (22 px, Plex Mono) |
| Dados | Mesma cotação da etapa de itens (mesma chave de cache: abre sem nova chamada) |
| Carregando | "Calculando o total…"; com erro, a mensagem da cotação no lugar do total |
| Celular | O cartão ocupa a largura toda; o total continua acima do botão |

## 8. Roteiro para validar com pessoas

Formato: teste de usabilidade moderado, 5 participantes (2 vendedores, 2 gerentes, 1 pessoa que nunca viu o sistema), 30 minutos cada, no ambiente local (`docker-compose.demo.yml`) para não gastar a cota do Render e do Neon.

| Tarefa | Perfil | Sucesso quando |
| --- | --- | --- |
| Vender 2 Carregadores 20W para Marina Costa no Pix, retirada | Vendedor | Pedido pago, sem ajuda, e a pessoa diz o total antes de criar o pedido |
| Voltar aos itens no meio do pagamento, trocar a quantidade e concluir | Vendedor | Cliente e pagamento continuam escolhidos |
| Cancelar um pedido pendente e explicar o que aconteceu com o estoque | Vendedor | Cancela pela confirmação e cita a reserva liberada |
| Achar os pedidos aguardando pagamento a partir da Visão geral | Gerente | Chega à lista filtrada em até 2 cliques |
| Registrar entrada de 5 unidades com custo e conferir o saldo | Gerente | Vê o aviso "Entrada registrada" e o saldo novo |
| Encerrar uma promoção | Gerente | Lê a confirmação e diz que a promoção não volta |
| Entrar no site publicado com o servidor dormindo | Novo | Espera sem recarregar a página |

Medidas por tarefa: concluiu ou não, tempo, erros e pedidos de ajuda; ao fim, nota de 1 a 7 para "foi fácil". Para consolidar: agrupar as observações por tema, contar quantos participantes passaram por cada problema ("3 de 5 não acharam…"), separar o que foi observado do que foi interpretado e transformar cada tema numa linha nova da seção 3.

## 9. Validação executada

Rodada de 20/10/2026, na branch `feat/ux-usabilidade`, antes de qualquer commit. Nada foi executado no Render nem no Neon.

| Verificação | Resultado | Como reproduzir |
| --- | --- | --- |
| Tipos | `tsc --noEmit` sem erros | `cd frontend; npm run typecheck` |
| Lógica do frontend | 41/41 (3 novos: desconto manual depois da recarga, 502/503/504, rótulo nos erros) | `cd frontend; npm test` |
| Build de produção | 18 rotas, sem erro | `cd frontend; npm run build` |
| Contrato ao vivo + E2E + axe | 58 de 58 operações, `divergences: []`; Playwright 26/26 (rodado de novo em 21/10 com U-25 a U-27 e o foco das etapas conferido no CT-WEB-101), com WCAG 2.2 AA e 320 px nas telas dos dois perfis | `scripts	est-contract.ps1 -HttpOnly -Frontend` (PowerShell 7, JDK 25) |
| Validadores do contrato (Node) | 47/47 | mesmo runner |
| Fluxos alterados no navegador | 36/36 verificações na stack `docker-compose.demo.yml`: mostrar senha, aviso de espera e 504 no login, total igual nas duas etapas, cliente e cartão mantidos ao voltar, desconto mantido na recarga, confirmação com foco em "Manter pedido" e Esc sem cancelar, filtro de Vendas no endereço e na volta do detalhe, links do painel, `?abaixo=1`, Inventário automático, avisos de sucesso, confirmação de inativar, mostrar senha provisória; axe sem violação em `/entrar`, PDV (pagamento e confirmação), `/vendas`, `/gestao`, `/gestao/estoque` e `/gestao/usuarios`; sem rolagem horizontal em 320 px no pagamento | roteiro Playwright fora do repositório |
| Fotos e ícones na stack Docker | antes 404; depois 200 (`/produtos/airpods.webp`, `/produtos/linhas/acessorio.svg`, `/brand/sbi-512.png`) | `curl -I http://127.0.0.1:3000/produtos/airpods.webp` |

Rodada de 21/10 (U-28 e U-29): integração Java 655 testes, 0 falhas (`mvnw -Pintegration clean verify`); contrato 58/58 sem divergência; E2E 27/27, incluindo CT-WEB-128.

Não validado: leitores de tela reais (NVDA, VoiceOver) e o site publicado.
