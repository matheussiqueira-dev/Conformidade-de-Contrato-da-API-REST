import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { overflowing, PASSWORD, seed } from './support.ts';

const AXE_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];

async function login(page: Page, email: string) {
  await page.goto('/entrar');
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await page.getByLabel('Senha', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  await expect(page).not.toHaveURL(/\/entrar/);
}

async function axe(page: Page, label: string) {
  await page.waitForLoadState('networkidle');
  const result = await new AxeBuilder({ page }).withTags(AXE_TAGS).analyze();
  expect(result.violations.map(v => `${label}: ${v.id} ${v.nodes.map(n => n.target.join(' ')).join(' | ')}`)).toEqual([]);
  await page.setViewportSize({ width: 320, height: 720 });
  expect(await overflowing(page), label).toEqual([]);
  await page.setViewportSize({ width: 1280, height: 800 });
}

test('[CT-WEB-114] vendedor da desconto dentro da alcada; acima e barrado; gerente aprova no pedido pendente', async ({ page }) => {
  const data = await seed();
  await login(page, 'seller@example.test');
  await page.goto('/vender');
  const search = page.getByRole('combobox', { name: /Nome ou SKU/ });
  await search.fill(data.sku);
  await expect(page.getByRole('option').first()).toContainText(data.sku);
  await search.press('Enter');
  await expect(page.locator('.totals .total')).toContainText('R$ 59,90');

  await page.getByLabel('Motivo').fill('Cliente fiel da loja');
  await page.getByLabel('Percentual (%)').fill('10');
  await expect(page.getByRole('alert').filter({ hasText: 'Desconto acima do seu limite de 5' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Continuar para cliente e pagamento' })).toBeDisabled();

  await page.getByLabel('Percentual (%)').fill('5');
  await expect(page.locator('.totals')).toContainText('Desconto manual');
  await expect(page.locator('.totals .total')).toContainText('R$ 56,90');
  await expect(page.getByText('Seu limite: 5%')).toBeVisible();
  await axe(page, 'vendedor /vender com desconto');
  await page.getByRole('button', { name: 'Continuar para cliente e pagamento' }).click();
  await page.getByLabel('CPF, CNPJ ou nome').fill(data.cpf);
  await page.getByRole('list', { name: 'Clientes encontrados' }).getByRole('button').first().click();
  await page.getByRole('button', { name: 'Criar pedido' }).click();
  const heading = page.getByRole('heading', { name: /Venda #\d+/ });
  await expect(heading).toBeVisible();
  const orderId = /#(\d+)/.exec((await heading.textContent()) ?? '')?.[1];
  await page.goto(`/vendas/${orderId}`);
  await expect(page.getByText(/Desconto manual de 5%/)).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Aprovar desconto (gerente)' })).toHaveCount(0);
  await page.getByRole('button', { name: 'Sair' }).click();

  await login(page, 'manager@example.test');
  await page.goto(`/vendas/${orderId}`);
  await page.getByLabel('Percentual (%)').fill('12');
  await page.getByLabel('Motivo').fill('Aprovado pela gerencia');
  await page.getByRole('button', { name: 'Aplicar desconto' }).click();
  await expect(page.locator('.totals .total')).toContainText('R$ 52,71');
  await expect(page.getByText(/Aprovado por/)).toBeVisible();
  await axe(page, 'gerente /vendas/[id] pendente');

  await page.goto('/gestao/promocoes');
  const policies = page.getByRole('region', { name: 'Alçada de desconto manual' });
  await expect(policies.getByRole('row').filter({ hasText: 'Vendedor' })).toContainText('5%');
  await axe(page, 'gerente /gestao/promocoes');
});
