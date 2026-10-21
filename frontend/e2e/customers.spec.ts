import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { overflowing, paidSale, PASSWORD, seed } from './support.ts';

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

test('[CT-WEB-111] perfil do cliente com indicadores, abas e axe nos dois papeis; nova venda ja traz o cliente', async ({ page }) => {
  const data = await seed();
  await paidSale(data, 2);

  await login(page, 'manager@example.test');
  await page.goto(`/clientes/${data.customerId}`);
  await expect(page.getByRole('heading', { name: data.customerName, level: 1 })).toBeVisible();
  const strip = page.getByRole('region', { name: 'Indicadores do cliente' });
  await expect(strip.getByText('Total comprado')).toBeVisible();
  await expect(strip.locator('.kpi').filter({ hasText: 'Pedidos pagos' }).locator('.value')).toHaveText('1');
  await expect(strip.locator('.kpi').filter({ hasText: 'Última compra' }).locator('.value')).toHaveText('hoje');
  await axe(page, 'gerente /clientes/[id]');
  await page.getByRole('tab', { name: 'Produtos mais comprados' }).click();
  await expect(page.getByRole('tabpanel').getByText(data.productName)).toBeVisible();
  await page.getByRole('tab', { name: 'Produtos mais comprados' }).press('ArrowRight');
  await expect(page.getByRole('tab', { name: 'Dados cadastrais' })).toBeFocused();
  await expect(page.getByRole('tab', { name: 'Dados cadastrais' })).toHaveAttribute('aria-selected', 'true');
  await page.getByRole('button', { name: 'Sair' }).click();

  await login(page, 'seller@example.test');
  await page.goto(`/clientes/${data.customerId}`);
  await expect(page.getByText('Suas vendas para este cliente')).toBeVisible();
  await expect(page.getByText(/\*\*\*\.\d{3}\.\d{3}-\*\*/).first()).toBeVisible();
  await axe(page, 'vendedor /clientes/[id]');

  await page.getByRole('link', { name: 'Nova venda para este cliente' }).click();
  await expect(page).toHaveURL(new RegExp(`/vender\\?cliente=${data.customerId}$`));
  const search = page.getByRole('combobox', { name: /Nome ou SKU/ });
  await search.fill(data.sku);
  await expect(page.getByRole('option').first()).toContainText(data.sku);
  await search.press('Enter');
  await page.getByRole('button', { name: 'Continuar para cliente e pagamento' }).click();
  await expect(page.getByRole('region', { name: 'Cliente' }).getByText(data.customerName)).toBeVisible();
  await expect(page.getByRole('button', { name: 'Trocar cliente' })).toBeVisible();
});

/** CNPJ alfanumerico valido e unico por execucao (modulo 11 com valor ASCII - 48). */
function alphanumericCnpj(): string {
  const chars = '0123456789ABCDEFGHJKLMNPQRSTUVWXYZ';
  const base = ['1', '2', ...Array.from({ length: 10 }, (_, i) => (i % 3 === 0 ? String(Math.floor(Math.random() * 10)) : chars[Math.floor(Math.random() * chars.length)] ?? 'A'))];
  const dv = (values: string[]) => {
    const weights = values.length === 12 ? [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2] : [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
    const rest = values.reduce((sum, c, i) => sum + (c.charCodeAt(0) - 48) * (weights[i] ?? 0), 0) % 11;
    return String(rest < 2 ? 0 : 11 - rest);
  };
  const first = dv(base);
  return [...base, first, dv([...base, first])].join('');
}

test('[CT-WEB-113] vendedor cadastra empresa com CNPJ alfanumerico e ve o documento mascarado', async ({ page }) => {
  const cnpj = alphanumericCnpj();
  const masked = `**.${cnpj.slice(2, 5)}.${cnpj.slice(5, 8)}/${cnpj.slice(8, 12)}-**`;
  await login(page, 'seller@example.test');
  await page.goto('/clientes');
  await page.getByRole('button', { name: 'Cadastrar cliente' }).click();
  const form = page.getByRole('dialog');
  await form.getByText('Pessoa jurídica').click();
  await form.getByLabel('Nome fantasia').fill(`Loja Nova ${cnpj}`);
  await form.getByLabel('Razão social').fill('Loja Nova LTDA');
  await form.getByLabel('CNPJ').fill(cnpj.toLowerCase());
  await form.getByLabel('E-mail').fill(`contato${cnpj.toLowerCase()}@cliente.test`);
  await form.getByLabel('Telefone').fill('85999990000');
  await form.getByRole('button', { name: 'Cadastrar cliente' }).click();
  await expect(page.getByRole('heading', { name: `Loja Nova ${cnpj}`, level: 1 })).toBeVisible();
  await expect(page.getByText(masked).first()).toBeVisible();
  await page.goto('/clientes');
  await page.getByLabel('Buscar cliente').fill(cnpj.toLowerCase());
  await expect(page.getByText(masked).first()).toBeVisible();
});

test('[CT-WEB-128] documento ja cadastrado no PDV oferece o cliente existente em vez de duplicar (D-EVO-40)', async ({ page }) => {
  const data = await seed();
  await login(page, 'seller@example.test');
  await page.goto('/vender');
  const search = page.getByRole('combobox', { name: /Nome ou SKU/ });
  await search.fill(data.sku);
  await expect(page.getByRole('option').first()).toContainText(data.sku);
  await search.press('Enter');
  await page.getByRole('button', { name: 'Continuar para cliente e pagamento' }).click();
  await page.getByRole('button', { name: 'Cadastrar cliente' }).click();
  const form = page.getByRole('dialog');
  await form.getByLabel('Nome', { exact: true }).fill('Outro Nome');
  await form.getByLabel('CPF').fill(data.cpf);
  await form.getByLabel('E-mail').fill(`outro${Date.now()}@cliente.test`);
  await form.getByLabel('Telefone').fill('85999990000');
  await form.getByRole('button', { name: 'Cadastrar cliente' }).click();
  await expect(form.getByRole('alert')).toContainText('Este documento já pertence a um cliente cadastrado');
  await form.getByRole('button', { name: 'Usar o cliente já cadastrado' }).click();
  await expect(form).toBeHidden();
  await expect(page.getByRole('region', { name: 'Cliente' }).getByText(data.customerName)).toBeVisible();
  await expect(page.getByRole('button', { name: 'Trocar cliente' })).toBeVisible();
});
