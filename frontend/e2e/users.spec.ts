import { test, expect, type Page } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';
import { overflowing, PASSWORD } from './support.ts';

const AXE_TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'];
const BASE = 'http://127.0.0.1:3000';

async function fillLogin(page: Page, email: string, password: string) {
  await page.getByLabel('E-mail', { exact: true }).fill(email);
  await page.getByLabel('Senha', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Entrar na minha conta' }).click();
}

async function axe(page: Page, label: string) {
  const result = await new AxeBuilder({ page }).withTags(AXE_TAGS).analyze();
  expect(result.violations.map(v => `${label}: ${v.id} ${v.nodes.map(n => n.target.join(' ')).join(' | ')}`)).toEqual([]);
  await page.setViewportSize({ width: 320, height: 720 });
  expect(await overflowing(page), label).toEqual([]);
  await page.setViewportSize({ width: 1280, height: 800 });
}

test('[CT-WEB-110] login: mensagem unica, aviso de bloqueio e ?next= externo ignorado', async ({ page }) => {
  await page.goto('/entrar');
  await expect(page.getByText('Após 5 tentativas incorretas, o acesso fica bloqueado por 15 minutos.')).toBeVisible();
  await fillLogin(page, 'ninguem@example.test', 'senha-qualquer-errada');
  const unknown = await page.locator('form').getByRole('alert').textContent();
  await fillLogin(page, 'seller@example.test', 'senha-qualquer-errada');
  await expect(page.locator('form').getByRole('alert')).toHaveText(unknown ?? '');
  expect(unknown).toContain('E-mail ou senha inválidos.');

  for (const next of ['https://evil.test/roubo', '//evil.test']) {
    await page.context().clearCookies();
    await page.goto(`/entrar?next=${encodeURIComponent(next)}`);
    await fillLogin(page, 'seller@example.test', PASSWORD);
    await expect(page).toHaveURL(`${BASE}/vender`);
  }
});

test('[CT-WEB-112] gerente cadastra usuario; senha provisoria obriga troca; desativar derruba a sessao', async ({ page, browser }) => {
  const stamp = `${Date.now()}`;
  const email = `novo${stamp}@example.test`;
  const temporary = `Provisoria-${stamp}`;
  const definitive = `Senha definitiva ${stamp}`;

  await page.goto('/entrar');
  await fillLogin(page, 'manager@example.test', PASSWORD);
  await page.getByRole('navigation', { name: 'Áreas' }).getByRole('link', { name: 'Usuários' }).click();
  await expect(page.getByRole('heading', { name: 'Usuários da equipe', level: 1 })).toBeVisible();
  await page.waitForLoadState('networkidle');
  await axe(page, '/gestao/usuarios');

  await page.getByRole('button', { name: 'Cadastrar usuário' }).click();
  const drawer = page.getByRole('dialog');
  await drawer.getByLabel('Nome').fill('Bruno Novato');
  await drawer.getByLabel('E-mail').fill(email);
  await drawer.getByLabel('Senha provisória').fill(temporary);
  await drawer.getByRole('button', { name: 'Cadastrar' }).click();
  await expect(drawer).toBeHidden();
  await page.getByLabel('Nome ou e-mail').fill(`novo${stamp}`);
  const row = page.getByRole('row').filter({ hasText: email });
  await expect(row).toContainText('Ativo');
  await expect(page.getByRole('row').filter({ hasText: 'manager@example.test' }).getByRole('button', { name: /^Papel/ })).toHaveCount(0);

  const other = await browser.newContext({ baseURL: BASE });
  const user = await other.newPage();
  await user.goto('/entrar');
  await fillLogin(user, email, temporary);
  await expect(user).toHaveURL(`${BASE}/conta/senha`);
  await expect(user.getByRole('heading', { name: 'Troque a senha provisória' })).toBeVisible();
  await user.goto('/vender');
  await expect(user).toHaveURL(`${BASE}/conta/senha`);
  await axe(user, '/conta/senha');

  await user.getByLabel('Senha atual').fill('senha-atual-errada');
  await user.getByLabel('Nova senha', { exact: true }).fill(definitive);
  await user.getByLabel('Repita a nova senha').fill(definitive);
  await user.getByRole('button', { name: 'Salvar nova senha' }).click();
  await expect(user.getByText('Senha atual incorreta.')).toBeVisible();
  await expect(user.getByLabel('Senha atual')).toBeFocused();

  await user.getByLabel('Senha atual').fill(temporary);
  await user.getByRole('button', { name: 'Salvar nova senha' }).click();
  await expect(user).toHaveURL(`${BASE}/vender`);
  await expect(user.getByRole('heading', { name: 'Vender', level: 1 })).toBeVisible();

  await row.getByRole('button', { name: /^Desativar/ }).click();
  await drawer.getByLabel('Motivo').fill('Fim do periodo de testes');
  await drawer.getByRole('button', { name: 'Desativar' }).click();
  await expect(drawer).toBeHidden();
  await expect(row).toContainText('Inativo');

  await user.getByRole('link', { name: 'Vendas', exact: true }).click();
  await expect(user).toHaveURL(/\/entrar\?.*sessao=encerrada/);
  await expect(user.getByRole('status').filter({ hasText: 'Sua sessão foi encerrada' })).toBeVisible();
  await other.close();
});
