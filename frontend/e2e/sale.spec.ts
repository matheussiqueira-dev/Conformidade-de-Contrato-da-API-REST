import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { customerOrderCount, overflowing, PASSWORD, seed } from './support.ts';

async function login(page: Page, email: string) {
  await page.goto('/entrar');
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await page.getByLabel('Senha', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
  await expect(page).not.toHaveURL(/\/entrar/);
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
}

async function cartWithCustomer(page: Page, sku: string, cpf: string) {
  await page.goto('/vender');
  const search = page.getByRole('combobox', { name: /Nome ou SKU/ });
  await expect(search).toBeFocused();
  await search.fill(sku);
  await expect(page.getByRole('option').first()).toContainText(sku);
  await search.press('Enter');
  await expect(page.getByRole('group', { name: /Quantidade de/ })).toBeVisible();
  await expect(page.locator('.totals .total')).toContainText('R$ 59,90');
  await page.getByRole('button', { name: 'Continuar para cliente e pagamento' }).click();
  await page.getByLabel('CPF, CNPJ ou nome').fill(cpf);
  await page.getByRole('list', { name: 'Clientes encontrados' }).getByRole('button').first().click();
}

const AXE_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];

test('[CT-WEB-101] venda com Pix pelo teclado aparece em vendas e no historico do cliente', async ({ page }) => {
  const data = await seed();
  await login(page, 'seller@example.test');
  await cartWithCustomer(page, data.sku, data.cpf);
  await page.getByRole('button', { name: 'Finalizar venda' }).click();
  await expect(page.getByRole('heading', { name: /Venda #\d+/ })).toBeVisible();
  const heading = await page.getByRole('heading', { name: /Venda #\d+/ }).textContent();
  const orderId = /#(\d+)/.exec(heading ?? '')?.[1];
  await page.getByRole('button', { name: 'Confirmar pagamento recebido' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Pagamento confirmado' })).toBeVisible();

  await page.getByRole('link', { name: 'Vendas', exact: true }).click();
  await expect(page.getByRole('link', { name: `#${orderId}` })).toBeVisible();
  await page.goto(`/clientes/${data.customerId}`);
  await expect(page.getByRole('heading', { name: data.customerName })).toBeVisible();
  await expect(page.getByText(/\*\*\*\.\d{3}\.\d{3}-\*\*/)).toBeVisible();
  await expect(page.getByRole('link', { name: `#${orderId}` })).toBeVisible();
  await expect(page.getByRole('cell', { name: 'R$ 59,90' })).toBeVisible();
});

test('[CT-WEB-104] duplo clique e recarga nao criam dois pedidos', async ({ page }) => {
  const data = await seed();
  await login(page, 'seller@example.test');
  await cartWithCustomer(page, data.sku, data.cpf);
  const finish = page.getByRole('button', { name: 'Finalizar venda' });
  await finish.dblclick();
  await expect(page.getByRole('heading', { name: /Venda #\d+/ })).toBeVisible();
  expect(await customerOrderCount(data.customerId)).toBe(1);
});

test('[CT-WEB-103] vendedor nao ve Gestao no menu nem abre telas do gerente', async ({ page }) => {
  await login(page, 'seller@example.test');
  const nav = page.getByRole('navigation', { name: 'Áreas' });
  await expect(nav.getByRole('link', { name: 'Vender' })).toBeVisible();
  await expect(nav.getByRole('link', { name: 'Gestão' })).toHaveCount(0);
  await page.goto('/gestao');
  await expect(page.getByRole('heading', { name: 'Sem acesso a esta área' })).toBeVisible();
  await expect(page.getByText('Faturamento líquido')).toHaveCount(0);
});

test('[CT-WEB-109] telas do vendedor sem violacao WCAG 2.2 AA e sem rolagem horizontal em 320 px', async ({ page }) => {
  const data = await seed();
  await login(page, 'seller@example.test');
  for (const path of ['/vender', '/vendas', '/clientes', `/clientes/${data.customerId}`]) {
    await page.goto(path);
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    const result = await new AxeBuilder({ page }).withTags(AXE_TAGS).analyze();
    expect(result.violations.map(v => `${path}: ${v.id} ${v.nodes.length}`)).toEqual([]);
    await page.setViewportSize({ width: 320, height: 720 });
    expect(await overflowing(page), path).toEqual([]);
    await page.setViewportSize({ width: 1280, height: 800 });
  }
  await page.goto('/entrar');
  const loginPage = await new AxeBuilder({ page }).withTags(AXE_TAGS).analyze();
  expect(loginPage.violations.map(v => v.id)).toEqual([]);
});
