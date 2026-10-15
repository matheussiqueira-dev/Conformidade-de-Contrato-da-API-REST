import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { overflowing, paidSale, PASSWORD, seed } from './support.ts';

async function login(page: Page) {
  await page.goto('/entrar');
  await page.getByLabel('E-mail', { exact: true }).fill('manager@example.test');
  await page.getByLabel('Senha', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  await expect(page).not.toHaveURL(/\/entrar/);
}

function ordersPaid(page: Page) {
  return page.locator('.kpi').filter({ hasText: 'Pedidos pagos' }).locator('.value');
}

test('[CT-WEB-102] venda paga no balcao aparece no faturamento e baixa o estoque para o gerente em ate 15 s', async ({ page }) => {
  const data = await seed(10);
  await login(page);
  await page.goto('/gestao');
  await page.getByText('Hoje', { exact: true }).click();
  await expect(ordersPaid(page)).toBeVisible();
  const before = Number(await ordersPaid(page).textContent());
  await paidSale(data, 2);
  await expect.poll(async () => Number(await ordersPaid(page).textContent()), { timeout: 20_000, intervals: [1_000] }).toBe(before + 1);
  await expect(page.getByText(/Atualizado às/)).toBeVisible();

  await page.goto('/gestao/estoque');
  await page.getByLabel('Nome ou SKU').fill(data.sku);
  const row = page.getByRole('row').filter({ hasText: data.sku });
  await expect(row.getByRole('cell').nth(1)).toHaveText('8');
});

test('[CT-WEB-109] telas do gerente sem violacao WCAG 2.2 AA e sem rolagem horizontal em 320 px', async ({ page }) => {
  await seed();
  await login(page);
  for (const path of ['/gestao', '/gestao/estoque', '/gestao/produtos', '/gestao/promocoes', '/gestao/analises', '/meu-desempenho']) {
    await page.setViewportSize({ width: 1280, height: 800 });
    await page.goto(path);
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
    await page.waitForLoadState('networkidle');
    const result = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']).analyze();
    expect(result.violations.map(v => `${path}: ${v.id} ${v.nodes.map(n => n.target.join(' ')).join(' | ')}`)).toEqual([]);
    await page.setViewportSize({ width: 320, height: 720 });
    expect(await overflowing(page), path).toEqual([]);
  }
  await page.setViewportSize({ width: 1280, height: 800 });
  await page.goto('/gestao');
  await page.screenshot({ path: 'test-results/gestao-desktop.png', fullPage: true });
});

test('[CT-UI-001] menu do gerente traz as areas de venda e de gestao', async ({ page }) => {
  await login(page);
  const nav = page.getByRole('navigation', { name: 'Áreas' });
  for (const name of ['Vender', 'Vendas', 'Clientes', 'Meu desempenho', 'Visão geral', 'Estoque', 'Produtos e preços', 'Promoções', 'Análises', 'Trocar senha']) {
    await expect(nav.getByRole('link', { name, exact: true })).toBeVisible();
  }
});

test('[CT-WEB-126] Produtos mais vendidos mostra 10 linhas e leva a Analises pelo "Ver todos"', async ({ page }) => {
  for (let i = 0; i < 11; i++) await paidSale(await seed(5));
  await login(page);
  await page.goto('/gestao');
  await page.getByText('Hoje', { exact: true }).click();
  const section = page.getByRole('region', { name: 'Produtos mais vendidos' });
  await expect(section.getByRole('table')).toBeVisible();
  await expect(section.locator('tbody tr')).toHaveCount(10);
  await expect(section.getByText(/^Mostrando 10 de \d+\./)).toBeVisible();
  await section.getByRole('link', { name: 'Ver todos' }).click();
  await expect(page).toHaveURL(/\/gestao\/analises$/);
});
