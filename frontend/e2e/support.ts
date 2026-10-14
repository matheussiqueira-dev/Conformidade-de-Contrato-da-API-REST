// Massa propria de cada teste E2E, criada pela API isolada (porta 18080) com a conta do gerente e a do vendedor.
import { request, type APIRequestContext, type Page } from '@playwright/test';

export const API = 'http://127.0.0.1:18080';
export const PASSWORD = process.env.A3_TEST_PASSWORD ?? '';

if (process.env.A3_CONTRACT_ISOLATED !== '1' || !PASSWORD) throw new Error('E2E requires the disposable database runner.');

export async function session(email: string): Promise<{ api: APIRequestContext; csrf: Record<string, string> }> {
  const api = await request.newContext({ baseURL: API });
  const token = async () => {
    const body = await (await api.get('/auth/csrf')).json() as { headerName: string; token: string };
    return { [body.headerName]: body.token };
  };
  const login = await api.post('/auth/login', { data: { email, password: PASSWORD }, headers: await token() });
  if (login.status() !== 200) throw new Error(`login ${email}: ${login.status()}`);
  return { api, csrf: await token() };
}

/** CPF sintetico valido e unico por execucao. */
export function syntheticCpf(): string {
  const base = Array.from({ length: 9 }, (_, i) => (i === 0 ? 1 + Math.floor(Math.random() * 8) : Math.floor(Math.random() * 10)));
  for (const size of [9, 10]) {
    const sum = base.slice(0, size).reduce((total, digit, i) => total + digit * (size + 1 - i), 0);
    base.push(((sum * 10) % 11) % 10);
  }
  return base.join('');
}

export interface Seed { sku: string; productName: string; productId: number; customerId: number; customerName: string; cpf: string }

/** Produto vendavel com estoque e um cliente novo. */
export async function seed(stock = 10): Promise<Seed> {
  const { api, csrf } = await session('manager@example.test');
  const stamp = `${Date.now()}${Math.floor(Math.random() * 1000)}`;
  const sku = `E2E-${stamp}`;
  const productName = `Fone E2E ${stamp}`;
  const product = await api.post('/products', { headers: csrf, data: {
    type: 'PHYSICAL', sku, name: productName, sellable: true, price: { amount: 59.9, currency: 'BRL' }, weightKg: 0.2 } });
  if (product.status() !== 201) throw new Error(`produto: ${product.status()} ${await product.text()}`);
  const productId = (await product.json() as { id: number }).id;
  const entry = await api.post(`/stock/products/${productId}/entries`, { headers: csrf, data: { quantity: stock, unitCost: { amount: 20, currency: 'BRL' } } });
  if (entry.status() !== 201) throw new Error(`entrada: ${entry.status()}`);
  const cpf = syntheticCpf();
  const customerName = `Cliente E2E ${stamp}`;
  const customer = await api.post('/customers', { headers: csrf, data: {
    type: 'INDIVIDUAL', name: customerName, cpf, email: `e2e${stamp}@cliente.test`, phone: '85999990000' } });
  if (customer.status() !== 201) throw new Error(`cliente: ${customer.status()} ${await customer.text()}`);
  const customerId = (await customer.json() as { id: number }).id;
  await api.dispose();
  return { sku, productName, productId, customerId, customerName, cpf };
}

export async function customerOrderCount(customerId: number): Promise<number> {
  const { api } = await session('manager@example.test');
  const response = await api.get(`/customers/${customerId}/orders`);
  const total = (await response.json() as { totalElements: number }).totalElements;
  await api.dispose();
  return total;
}

/** Venda paga feita por outra sessao (o painel do gerente precisa enxergar em ate 15 s). */
export async function paidSale(data: Seed, quantity = 1): Promise<number> {
  const { api, csrf } = await session('seller@example.test');
  const created = await api.post('/sales/orders', { headers: { ...csrf, 'Idempotency-Key': crypto.randomUUID() }, data: {
    customerId: data.customerId, items: [{ productId: data.productId, quantity }], payment: { type: 'PIX' }, fulfillment: 'PICKUP' } });
  if (created.status() !== 201) throw new Error(`venda: ${created.status()} ${await created.text()}`);
  const id = (await created.json() as { id: number }).id;
  const paid = await api.post(`/sales/orders/${id}/payment`, { headers: csrf });
  if (paid.status() !== 200) throw new Error(`pagamento: ${paid.status()}`);
  await api.dispose();
  return id;
}

/** Elementos que passam da largura da janela (vazio = sem rolagem horizontal). */
export async function overflowing(page: Page): Promise<string[]> {
  return page.evaluate(() => {
    if (document.documentElement.scrollWidth <= window.innerWidth) return [];
    const culprits = [...document.querySelectorAll('body *')]
      .filter(el => {
        if (el.getBoundingClientRect().right <= window.innerWidth + 1 || el.closest('dialog:not([open])')) return false;
        // Conteudo dentro de um conteiner com rolagem propria (tabela, menu) nao alarga a pagina.
        for (let node = el.parentElement; node && node !== document.body; node = node.parentElement) {
          if (['auto', 'scroll', 'hidden'].includes(getComputedStyle(node).overflowX)) return false;
        }
        return true;
      })
      .slice(0, 8).map(el => `${el.tagName.toLowerCase()}.${(el.className || '').toString().replace(/\s+/g, '.')} ${Math.round(el.getBoundingClientRect().right)}px`);
    if (culprits.length) return culprits;
    const name = (el: Element) => `${el.tagName.toLowerCase()}.${(el.className || '').toString().trim().split(/\s+/).join('.')}`;
    const wide = [...document.querySelectorAll('body *')].filter(el => el.getBoundingClientRect().right > window.innerWidth + 1)
      .filter(el => !el.parentElement || el.parentElement.getBoundingClientRect().right <= window.innerWidth + 1).slice(0, 6)
      .map(el => `${name(el)} ${Math.round(el.getBoundingClientRect().right)}px em ${el.parentElement ? name(el.parentElement) : '-'}`);
    return [`documento com ${document.documentElement.scrollWidth}px em ${window.innerWidth}px`, ...wide];
  });
}
