// Smoke do ambiente publicado (CT-PUB-01 a 05). Somente leitura: nenhuma venda, pagamento ou cadastro.
// Roda com playwright.published.config.mjs; contas e senhas vem do ambiente (segredos do GitHub no workflow).
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { test, expect, type Page, type Request } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const AXE_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];
const MANAGER = { email: process.env.SBI_SMOKE_MANAGER_EMAIL ?? '', password: process.env.SBI_SMOKE_MANAGER_PASSWORD ?? '' };
const SELLER = { email: process.env.SBI_SMOKE_SELLER_EMAIL ?? '', password: process.env.SBI_SMOKE_SELLER_PASSWORD ?? '' };
if (!MANAGER.email || !MANAGER.password || !SELLER.email || !SELLER.password) {
  throw new Error('Defina SBI_SMOKE_MANAGER_EMAIL/_PASSWORD e SBI_SMOKE_SELLER_EMAIL/_PASSWORD.');
}

/** CPF do cliente recorrente da massa Olist (CLI-001, 14 pedidos), lido do CSV versionado. */
function recurringCpf(): string {
  const csv = readFileSync(join(process.cwd(), '..', 'docs', 'bd', 'pacote-sql', 'olist', 'clientes.csv'), 'utf8');
  const [header = '', ...rows] = csv.trim().split(/\r?\n/);
  const column = header.split(',').indexOf('cpf');
  const cpf = rows.find(line => line.startsWith('CLI-001,'))?.split(',')[column];
  if (!cpf) throw new Error('CLI-001 sem CPF em clientes.csv');
  return cpf;
}

/** Somente leitura: fora login, logout e token CSRF, a API publicada so recebe GET. */
function guardReadOnly(page: Page): Request[] {
  const writes: Request[] = [];
  page.on('request', request => {
    const url = request.url();
    if (url.includes('/api/backend/') && request.method() !== 'GET' && !/\/auth\/(login|logout)$/.test(new URL(url).pathname)) {
      writes.push(request);
    }
  });
  return writes;
}

async function login(page: Page, account: { email: string; password: string }) {
  await page.goto('/entrar');
  await page.getByLabel('E-mail', { exact: true }).fill(account.email);
  await page.getByLabel('Senha', { exact: true }).fill(account.password);
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  await expect(page).not.toHaveURL(/\/entrar/, { timeout: 60_000 });
}

async function axe(page: Page, label: string) {
  await page.waitForLoadState('networkidle');
  const result = await new AxeBuilder({ page }).withTags(AXE_TAGS).analyze();
  expect(result.violations.map(v => `${label}: ${v.id} ${v.nodes.map(n => n.target.join(' ')).join(' | ')}`)).toEqual([]);
}

test('[CT-PUB-01] login sem sessao e axe em /entrar', async ({ page }) => {
  await page.goto('/entrar');
  await expect(page.getByRole('heading', { name: 'Entrar no SBI', level: 1 })).toBeVisible();
  await axe(page, '/entrar');
});

test('[CT-PUB-02] gerente: menu completo, 4 indicadores, meta do mes e axe em /gestao e /vender', async ({ page }) => {
  const writes = guardReadOnly(page);
  await login(page, MANAGER);
  const nav = page.getByRole('navigation', { name: 'Áreas' });
  for (const item of ['Visão geral', 'Produtos e preços', 'Promoções', 'Estoque', 'Análises', 'Usuários',
                      'Vender', 'Vendas', 'Clientes', 'Meu desempenho', 'Trocar senha']) {
    await expect(nav.getByRole('link', { name: item, exact: true })).toBeVisible();
  }
  await page.goto('/gestao');
  const kpis = page.getByRole('region', { name: 'Indicadores do período' });
  for (const kpi of ['Faturamento líquido', 'Lucro bruto', 'Margem bruta', 'Pedidos pagos']) {
    await expect(kpis.locator('.kpi').filter({ hasText: kpi })).toBeVisible({ timeout: 60_000 });
  }
  await expect(page.locator('#meta-titulo')).toBeVisible();
  await axe(page, 'gerente /gestao');
  await page.goto('/vender');
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  await axe(page, 'gerente /vender');
  expect(writes.map(r => `${r.method()} ${r.url()}`)).toEqual([]);
});

test('[CT-PUB-03] vendedor: menu sem gestao e /gestao sem acesso', async ({ page }) => {
  const writes = guardReadOnly(page);
  await login(page, SELLER);
  const nav = page.getByRole('navigation', { name: 'Áreas' });
  for (const item of ['Vender', 'Vendas', 'Clientes', 'Meu desempenho', 'Trocar senha']) {
    await expect(nav.getByRole('link', { name: item, exact: true })).toBeVisible();
  }
  for (const item of ['Visão geral', 'Estoque', 'Usuários']) {
    await expect(nav.getByRole('link', { name: item, exact: true })).toHaveCount(0);
  }
  await page.goto('/gestao');
  await expect(page.getByText('Sem acesso a esta área')).toBeVisible();
  expect(writes.map(r => `${r.method()} ${r.url()}`)).toEqual([]);
});

test('[CT-PUB-04] vendedor: cliente Olist recorrente com historico e documento mascarado', async ({ page }) => {
  const writes = guardReadOnly(page);
  const cpf = recurringCpf();
  await login(page, SELLER);
  await page.goto('/clientes');
  await page.getByLabel('Buscar cliente').fill(cpf);
  const rows = page.getByRole('table', { name: 'Clientes' }).locator('tbody tr');
  await expect(rows).toHaveCount(1, { timeout: 30_000 });
  await expect(rows.first().getByRole('cell').nth(1)).toHaveText(/^\*{3}\.\d{3}\.\d{3}-\*{2}$/);
  await rows.first().getByRole('link').click();
  await expect(page).toHaveURL(/\/clientes\/\d+$/);
  await expect(page.getByRole('heading', { level: 1 })).not.toHaveText('Clientes');
  await expect(page.getByText(/\*\*\*\.\d{3}\.\d{3}-\*\*/).first()).toBeVisible();
  // O perfil do vendedor nao traz o CPF completo em lugar nenhum da pagina (so mascarado).
  expect(await page.locator('main').innerText()).not.toContain(cpf);
  await expect(page.getByRole('tab', { name: 'Pedidos' })).toBeVisible();
  await page.getByRole('tab', { name: 'Pedidos' }).click();
  await expect(page.getByRole('tabpanel').locator('tbody tr').first()).toBeVisible();
  expect(writes.map(r => `${r.method()} ${r.url()}`)).toEqual([]);
});

test('[CT-PUB-05] axe em /vender do vendedor', async ({ page }) => {
  const writes = guardReadOnly(page);
  await login(page, SELLER);
  await page.goto('/vender');
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  await axe(page, 'vendedor /vender');
  expect(writes.map(r => `${r.method()} ${r.url()}`)).toEqual([]);
});
