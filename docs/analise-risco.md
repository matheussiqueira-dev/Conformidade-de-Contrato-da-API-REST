# Análise de risco por funcionalidade

Versão 0.1 (rascunho), 09/10/2026. Preparado por Matheus Siqueira para o card 06. **Responsável: Allan. Revisor: Francisco.** A classificação precisa ser discutida e aprovada pela equipe; os números de testes abaixo são medidos, as notas de probabilidade e impacto são julgamento e podem mudar.

Atende à seção 10 do enunciado: classificar funcionalidades por risco e justificar por que algumas recebem mais testes que outras.

## Escalas

**Probabilidade de falha** (evidência, não intuição):

| Nota | Nível | Critério |
| --- | --- | --- |
| 3 | Alta | Já teve 2 ou mais defeitos registrados, **ou** tem método com complexidade ciclomática > 10, **ou** depende de concorrência. |
| 2 | Média | Teve 1 defeito, ou tem regras condicionais por tipo/status, ou é código novo sem uso real. |
| 1 | Baixa | CRUD simples, sem defeito registrado e com validação declarativa. |

**Impacto da falha**:

| Nota | Nível | Critério |
| --- | --- | --- |
| 3 | Alto | Dinheiro errado, pedido em estado inválido, perda de dados ou exposição de dados entre perfis. |
| 2 | Médio | Erro de contrato (status/código), operação bloqueada com contorno, ou falha que derruba a demonstração. |
| 1 | Baixo | Mensagem, cosmético ou dado auxiliar. |

**Risco = Probabilidade × Impacto**: 9 Crítico · 6 Alto · 3–4 Médio · 1–2 Baixo.

## Matriz

| ID | Funcionalidade | RF / RNF | P | Justificativa de P | I | Justificativa de I | Risco |
| --- | --- | --- | :-: | --- | :-: | --- | --- |
| R01 | Registrar e processar pagamento | RF-08 | 3 | 8 defeitos (D003, D004, D006, D012, D016, D017, D018, D019); `PaymentService.update` com CC 18 e 0% de branches no R0 | 3 | Confirma o pedido como pago; base do realizado e das metas | **9 Crítico** |
| R02 | Criar pedido e calcular o total | RF-05, RF-07 | 3 | Defeito de quantidade multiplicada duas vezes (03/10), D012, D017; `OrderService.buildPayment` com CC 10 | 3 | Valor cobrado errado | **9 Crítico** |
| R03 | Reserva e saída de estoque (planejado) | RF-13, RNF-02 | 3 | Concorrência na última unidade; código ainda não existe | 3 | Venda sem estoque ou estoque negativo | **9 Crítico** (reavaliar quando API-03 existir) |
| R04 | Transição de status do pedido | RF-08 | 3 | D010 (regressão de status) e D019 (atualizações simultâneas) | 2 | Pedido enviado volta a "pago"; afeta logística e relatórios, com correção manual possível | **6 Alto** |
| R05 | Autenticação e autorização por perfil | RF-01, RF-02, RNF-01 | 2 | Código novo (sessão, CSRF, perfis); nenhum defeito registrado até agora | 3 | Vendedor lendo dados de colega ou do gerente | **6 Alto** |
| R06 | Precisão monetária (BigDecimal, 2 casas, HALF_UP) | RNF-03 | 2 | Migração de `Double` recente (DOM-02); D017 na fronteira de meio centavo | 3 | Arredondamento acumulado ou valor zero gravado | **6 Alto** |
| R07 | Migração do banco (Flyway V1–V3) | RNF-02, RNF-04 | 2 | D020 (arredonda antes de checar o limite) | 3 | Alteração silenciosa de dados históricos | **6 Alto** |
| R08 | Metas e realizado (planejado) | RF-09, RF-10 | 2 | Cálculo agregado por mês e fuso; meta zero | 2 | Painel errado, sem perda de dados | **4 Médio** |
| R09 | Cadastro de clientes PF/PJ | RF-03 | 2 | D008 (exceção de tipo errado) e D011 (exclusão inexistente) | 2 | Erro de contrato; cliente sem documento | **4 Médio** |
| R10 | Configuração e credenciais do ambiente | RNF-04 | 2 | D021 (senha ausente gera erro confuso) | 2 | Equipe ou banca não consegue subir o sistema | **4 Médio** |
| R11 | Catálogo de produtos físico/digital | RF-04 | 1 | Só D011 (comum aos 5 serviços); regras por tipo cobertas por tabela de decisão | 2 | Produto sem peso/link; frete errado | **2 Baixo** |
| R12 | Endereço de entrega | RF-05 | 1 | CRUD simples; regra única (não alterar após envio) | 2 | Entrega no endereço antigo | **2 Baixo** |
| R13 | Tela de acesso (login/logout) | RF-16, RNF-05 | 1 | 2 E2E e 3 testes do cliente aprovados; sem defeito | 2 | Demo não entra no sistema | **2 Baixo** |

Mapa de calor (quantidade de funcionalidades por célula):

| P \ I | 1 Baixo | 2 Médio | 3 Alto |
| --- | :-: | :-: | :-: |
| **3 Alta** | — | R04 | R01, R02, R03 |
| **2 Média** | — | R08, R09, R10 | R05, R06, R07 |
| **1 Baixa** | — | R11, R12, R13 | — |

## Intensidade de teste proporcional ao risco

Contagem medida em 09/10/2026 na branch `fix/FIX-03-pagamento-dinheiro` (`529ca49`), que contém os PRs #5, #8 e #9. O comando foi `scripts/test.ps1 -Mode Integration`, com **176 testes Java**, e a contagem saiu dos `TEST-*.xml` do Surefire. Os casos HTTP, Node e E2E aparecem à parte.

| Risco | Funcionalidade | Testes Java | Outros níveis | Técnicas aplicadas |
| --- | --- | ---: | --- | --- |
| R01 Crítico | Pagamento | 55 | Contrato HTTP de erros | Tabela de decisão por tipo, valor-limite (HALF_UP 99,995/100,004/100,005; vencimento hoje ± 1), transição de estados, 7 caminhos independentes de `PixPayment.processPayment`, concorrência com banco real |
| R02 Crítico | Pedido e total | 40 | 2 integração (preço histórico, venda demonstrativa) | Valor-limite (frete em 2 kg, quantidade, meio centavo, máximo NUMERIC(12,2)), particionamento de cliente novo/existente |
| R04 Alto | Status do pedido | 35 | — | Tabela de transição 5 × 5 (25 combinações), regressão D010, concorrência D019 |
| R05 Alto | Autenticação/autorização | 10 | 6 HTTP de sessão, 3 do cliente, 2 E2E | Tabela de decisão perfil × rota, CSRF, sessão removida |
| R07 Alto | Migração | 5 | Validação Hibernate no CI | Banco vazio, legado com baseline, conversão HALF_UP, limite antes do arredondamento |
| R09 Médio | Clientes | 9 | — | Tabela de decisão tipo × documento |
| R11 Baixo | Produtos | 11 | 3 HTTP (listagem, criação, 400/404) | Tabela de decisão tipo × peso × link |
| R12 Baixo | Endereço | 8 | — | Tabela de decisão status × alteração |
| — | Exclusão de pedido e carga do contexto | 3 | — | — |

**Por que pagamento tem 55 testes e endereço 8?** Pagamento combina probabilidade alta (8 defeitos encontrados, o método mais complexo do sistema e acesso concorrente) com impacto alto (dinheiro e status do pedido). Cada regra ali tem testes de fronteira, de transição e de concorrência. Endereço é um CRUD com uma regra só. Uma tabela de decisão de 5 linhas cobre todos os status, e uma falha tem contorno manual.

**Por que autenticação tem só 10 testes Java, se o risco é Alto?** Esse risco é verificado principalmente no nível de sistema: 6 casos HTTP com sessão real, CSRF e perfis, e 2 E2E no navegador. É ali que um vazamento entre perfis apareceria. Ainda falta o caso de vendedor tentando ler o recurso de um colega por ID (CA-11-02), que depende de SEC-01.

## Ações derivadas

| Risco | Lacuna | Ação | Card |
| --- | --- | --- | --- |
| R01 | `PaymentService.update` (CC 18) tinha 0% de branches no R0 | Grafo de fluxo e caminhos independentes, um teste por caminho | 12 |
| R01, R02 | Inspeção formal ainda não feita | Inspecionar `PaymentService` com checklist e ata | 08 |
| R03 | Estoque não existe | Teste de concorrência da última unidade antes da implementação (teste primeiro) | API-03 |
| R05 | Falta caso de ID de colega | CA-11-02 por HTTP quando SEC-01 expuser "minhas vendas" | SEC-01, CT-02 |
| R06 | Fronteiras monetárias espalhadas | Teste parametrizado único das 6 fronteiras (0,00; 0,004; 0,005; 0,01; máximo; máximo + 0,01) | QA-11 |
| Todos | Cobertura real desconhecida após os testes novos | Medição R1 logo depois dos merges | QA-10 |

## Revisão desta matriz

Reavaliar após cada medição (R1, R2), quando surgir defeito novo, e quando API-02/API-03 forem implementadas. Registrar a mudança de nota com o motivo.