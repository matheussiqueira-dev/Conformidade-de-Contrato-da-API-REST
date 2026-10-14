import { test, expect, type Browser, type Page } from '@playwright/test';
import { paidSale, PASSWORD, seed, session } from './support.ts';

async function login(page: Page, email: string) {
  await page.goto('/entrar');
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await page.getByLabel('Senha', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
  await expect(page).not.toHaveURL(/\/entrar/);
}

async function sellerPage(browser: Browser): Promise<Page> {
  const page = await (await browser.newContext()).newPage();
  await login(page, 'seller@example.test');
  return page;
}

async function searchCatalog(page: Page, sku: string) {
  await page.goto('/vender');
  await page.getByRole('combobox', { name: /Nome ou SKU/ }).fill(sku);
  const option = page.getByRole('option').filter({ hasText: sku });
  await expect(option).toBeVisible();
  return option;
}

test('[CT-WEB-105] entrada de estoque do gerente aumenta o disponivel no PDV', async ({ page, browser }) => {
  const data = await seed(2);
  await login(page, 'manager@example.test');
  await page.goto('/gestao/estoque');
  await page.getByLabel('Nome ou SKU').fill(data.sku);
  const row = page.getByRole('row').filter({ hasText: data.sku });
  await row.getByRole('button', { name: 'Entrada' }).click();
  const panel = page.getByRole('dialog');
  await panel.getByLabel('Quantidade').fill('5');
  await panel.getByLabel(/Custo unitário/).fill('21,50');
  await panel.getByRole('button', { name: 'Salvar' }).click();
  await expect(panel).toBeHidden();
  await expect(row.getByRole('cell').nth(1)).toHaveText('7');

  const seller = await sellerPage(browser);
  await expect(await searchCatalog(seller, data.sku)).toContainText('7 disp.');
});

test('[CT-WEB-106] preco alterado com motivo aparece no historico com autor; venda antiga mantem o preco', async ({ page }) => {
  const data = await seed();
  const oldOrder = await paidSale(data);
  await login(page, 'manager@example.test');
  await page.goto('/gestao/produtos');
  await page.getByLabel('Nome ou SKU').fill(data.sku);
  const row = page.getByRole('row').filter({ hasText: data.sku });
  await row.getByRole('button', { name: 'Preço' }).click();
  const panel = page.getByRole('dialog');
  await panel.getByLabel('Novo preço (R$)').fill('64,90');
  await panel.getByLabel('Motivo').fill('Reajuste do fornecedor');
  await panel.getByRole('button', { name: 'Alterar preço' }).click();
  await expect(panel).toBeHidden();
  await expect(row).toContainText('R$ 64,90');

  await row.getByRole('button', { name: 'Histórico' }).click();
  const history = page.getByRole('dialog').getByRole('row').filter({ hasText: 'Reajuste do fornecedor' });
  await expect(history).toContainText('R$ 59,90');
  await expect(history).toContainText('R$ 64,90');
  await expect(history).toContainText(/Gerente/i);
  await page.keyboard.press('Escape');

  await page.goto(`/vendas/${oldOrder}`);
  await expect(page.getByRole('row').filter({ hasText: data.productName })).toContainText('R$ 59,90');
});

test('[CT-WEB-107] promocao liberada aparece no PDV e entra na cotacao', async ({ page, browser }) => {
  const data = await seed();
  await login(page, 'manager@example.test');
  await page.goto('/gestao/promocoes');
  await page.getByRole('button', { name: 'Liberar promoção' }).click();
  const panel = page.getByRole('dialog');
  const name = `Promo E2E ${Date.now()}`;
  await panel.getByLabel('Nome', { exact: true }).fill(name);
  await panel.getByLabel('Tipo', { exact: true }).selectOption('PERCENT');
  await panel.getByLabel('Valor', { exact: true }).fill('10');
  const now = new Date(Date.now() - 3 * 3_600_000);
  const later = new Date(Date.now() + 48 * 3_600_000);
  const local = (date: Date) => new Date(date.getTime() - 3 * 3_600_000).toISOString().slice(0, 16);
  await panel.getByLabel('Início').fill(local(now));
  await panel.getByLabel('Fim').fill(local(later));
  await panel.getByLabel('Produto', { exact: true }).selectOption({ label: `${data.productName} (${data.sku})` });
  await panel.getByRole('button', { name: 'Liberar' }).click();
  await expect(panel).toBeHidden();
  await expect(page.getByRole('row').filter({ hasText: name })).toBeVisible();

  const seller = await sellerPage(browser);
  const option = await searchCatalog(seller, data.sku);
  await expect(option).toContainText(name);
  await seller.getByRole('combobox', { name: /Nome ou SKU/ }).press('Enter');
  await expect(seller.locator('.totals')).toContainText('R$ 5,99');
  await expect(seller.locator('.totals .total')).toContainText('R$ 53,91');
});

test('[CT-WEB-108] vendedor nao ve pedido de colega na lista nem pelo endereco', async ({ browser }) => {
  const data = await seed();
  const { api, csrf } = await session('manager@example.test');
  const created = await api.post('/sales/orders', { headers: { ...csrf, 'Idempotency-Key': crypto.randomUUID() }, data: {
    customerId: data.customerId, items: [{ productId: data.productId, quantity: 1 }], payment: { type: 'PIX' }, fulfillment: 'PICKUP' } });
  expect(created.status()).toBe(201);
  const colleagueOrder = (await created.json() as { id: number }).id;
  await api.dispose();

  const seller = await sellerPage(browser);
  await seller.goto('/vendas');
  await expect(seller.getByRole('heading', { name: 'Vendas', level: 1 })).toBeVisible();
  await expect(seller.getByRole('link', { name: `#${colleagueOrder}` })).toHaveCount(0);
  await seller.goto(`/vendas/${colleagueOrder}`);
  await expect(seller.getByRole('heading', { name: 'Pedido não encontrado' })).toBeVisible();
});
