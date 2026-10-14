import { test, expect } from '@playwright/test';
import { PASSWORD } from './support.ts';

test('login real, cookie HttpOnly, recarga e saida pelo proxy do Next', async ({ page, context }) => {
  await page.goto('/vender');
  await expect(page).toHaveURL(/\/entrar\?next=%2Fvender/);
  await expect(page.getByRole('heading', { name: 'Entre para continuar.' })).toBeVisible();
  await page.getByLabel('E-mail', { exact: true }).fill('seller@example.test');
  await page.getByLabel('Senha', { exact: true }).fill(PASSWORD);
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
  await expect(page).toHaveURL(/\/vender$/);
  await expect(page.getByRole('heading', { name: 'Vender', level: 1 })).toBeVisible();
  const cookie = (await context.cookies()).find(c => c.name === 'JSESSIONID');
  expect(cookie?.httpOnly).toBe(true);
  expect(cookie?.sameSite).toBe('Lax');
  expect(await page.evaluate(() => document.cookie.includes('JSESSIONID'))).toBe(false);
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Vender', level: 1 })).toBeVisible();
  await page.screenshot({ path: 'test-results/vender-desktop.png', fullPage: true });
  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page).toHaveURL(/\/entrar/);
  await page.goto('/vender');
  await expect(page).toHaveURL(/\/entrar/);
});

test('senha errada mostra erro acessivel; formulario cabe em 320 px', async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 720 });
  await page.goto('/entrar');
  await page.getByLabel('E-mail', { exact: true }).fill('manager@example.test');
  await page.getByLabel('Senha', { exact: true }).fill('intentionally-wrong-password');
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
  await expect(page.locator('form').getByRole('alert')).toContainText('E-mail ou senha inválidos');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.screenshot({ path: 'test-results/login-mobile.png', fullPage: true });
});
