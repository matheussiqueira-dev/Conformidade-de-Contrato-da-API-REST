-- Perfil demo: a carga inicial de iPhone e AirPods da V6_2 passa a ter custo informado (60% do preco de
-- venda: 2.700,00 e 720,00), para o painel mostrar lucro e margem. Uma entrada nova nao serviria: com saldo e
-- custo medio desconhecido, o gatilho stock_entry_apply mantem o custo medio desconhecido (regra CA-22-02).
-- Por isso o custo e gravado onde a carga inicial o teria gravado: na entrada de 25/08, no custo medio do
-- saldo e no custo historico dos itens ja vendidos. Saldos, reservas e precos nao mudam (numeros do Guia Visual).
-- So altera linhas ainda sem custo; o teste de margem nula usa produto proprio (BD-07).
UPDATE stock_entry e SET unit_cost = c.custo
  FROM (VALUES ('IP-128-PT', 2700.00), ('AP-BR', 720.00)) c(sku, custo)
  JOIN product p ON p.sku = c.sku
 WHERE e.product_id = p.id AND e.unit_cost IS NULL AND e.note = 'Carga inicial da demonstracao';

UPDATE stock_balance b SET avg_cost = c.custo, updated_at = now()
  FROM (VALUES ('IP-128-PT', 2700.00), ('AP-BR', 720.00)) c(sku, custo)
  JOIN product p ON p.sku = c.sku
 WHERE b.product_id = p.id AND b.avg_cost IS NULL;

UPDATE order_item oi SET unit_cost = c.custo
  FROM (VALUES ('IP-128-PT', 2700.00), ('AP-BR', 720.00)) c(sku, custo)
  JOIN product p ON p.sku = c.sku
 WHERE oi.product_id = p.id AND oi.unit_cost IS NULL;
