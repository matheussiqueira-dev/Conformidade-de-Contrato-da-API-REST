// Depois de criar, pagar ou cancelar uma venda, as telas que mostram vendas, metas, estoque e cliente leem de novo ao
// abrir (ou ja, se estiverem na tela). Sem isto, voltar para Vendas em ate 10 s mostrava a situacao antiga.
import type { QueryClient } from '@tanstack/react-query';

const AFTER_SALE = new Set([
  'sales', 'summary', 'revenue', 'low-stock', 'performance', 'catalog', 'stock', 'stock-below-count', 'movements',
  'customers-list', 'customer-orders', 'customer-summary', 'analytics-sales', 'analytics-stock',
]);

export function invalidateAfterSale(client: QueryClient): Promise<void> {
  return client.invalidateQueries({ predicate: query => AFTER_SALE.has(String(query.queryKey[0])) });
}
