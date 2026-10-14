import test from 'node:test';
import assert from 'node:assert/strict';
import { landingPath, menuFor, PASSWORD_PATH } from '../lib/areas.ts';
import { ERROR_MESSAGES } from '../lib/api/messages.ts';
import { readFileSync } from 'node:fs';
import { passwordProblems } from '../lib/password.ts';
import { actionsFor, situation } from '../lib/users.ts';

const NOW = new Date('2026-10-14T13:00:00Z');

test('senha provisoria leva sempre para /conta/senha, mesmo com ?next=', () => {
  assert.equal(landingPath(['TROCAR_SENHA']), PASSWORD_PATH);
  assert.equal(landingPath(['TROCAR_SENHA'], '/vender'), PASSWORD_PATH);
  assert.deepEqual(menuFor(['TROCAR_SENHA']), []);
});

test('Usuarios aparece no menu so com a area USUARIOS', () => {
  assert.ok(menuFor(['GESTAO', 'USUARIOS']).some(item => item.href === '/gestao/usuarios'));
  assert.ok(!menuFor(['VENDER', 'GESTAO']).some(item => item.href === '/gestao/usuarios'));
});

test('situacao: inativo, bloqueado ate HH:MM no horario da loja, ativo', () => {
  assert.deepEqual(situation({ id: 1, active: false, lockedUntil: null }, NOW), { label: 'Inativo', tone: 'off' });
  assert.deepEqual(situation({ id: 1, active: true, lockedUntil: '2026-10-14T13:15:00Z' }, NOW), { label: 'Bloqueado até 10h15', tone: 'warn' });
  assert.deepEqual(situation({ id: 1, active: true, lockedUntil: '2026-10-14T12:00:00Z' }, NOW), { label: 'Ativo', tone: 'ok' });
});

test('linha do proprio usuario nao oferece papel, situacao nem redefinicao de senha (CT-USR-104)', () => {
  const locked = { id: 7, active: true, lockedUntil: '2026-10-14T13:15:00Z' };
  assert.deepEqual(actionsFor(locked, 7, NOW), ['edit', 'unlock']);
  assert.deepEqual(actionsFor(locked, 1, NOW), ['edit', 'role', 'status', 'unlock', 'reset']);
  assert.deepEqual(actionsFor({ id: 2, active: true, lockedUntil: null }, 1, NOW), ['edit', 'role', 'status', 'reset']);
});

test('troca de senha: confere tamanho, bytes, e-mail, igualdade e confirmacao antes de enviar', () => {
  const ok = { currentPassword: 'Provisoria-Loja-2026', newPassword: 'cavalo bateria grampo', confirmation: 'cavalo bateria grampo' };
  assert.deepEqual(passwordProblems(ok, 'ana@loja.test'), {});
  assert.ok(passwordProblems({ ...ok, newPassword: 'curta', confirmation: 'curta' }, 'ana@loja.test').newPassword);
  assert.ok(passwordProblems({ ...ok, newPassword: 'ç'.repeat(40), confirmation: 'ç'.repeat(40) }, 'ana@loja.test').newPassword);
  assert.ok(passwordProblems({ ...ok, newPassword: 'Ana@Loja.test', confirmation: 'Ana@Loja.test' }, 'ana@loja.test').newPassword);
  assert.ok(passwordProblems({ ...ok, newPassword: ok.currentPassword, confirmation: ok.currentPassword }, 'ana@loja.test').newPassword);
  assert.ok(passwordProblems({ ...ok, confirmation: 'outra coisa qualquer' }, 'ana@loja.test').confirmation);
  assert.ok(passwordProblems({ ...ok, currentPassword: '' }, 'ana@loja.test').currentPassword);
});

test('todo ErrorCode do contrato tem mensagem em pt-BR', () => {
  const contract = JSON.parse(readFileSync(new URL('../../config/openapi/target-loja-gestao-openapi-2026-10-14-v5.json', import.meta.url), 'utf8')) as
    { components: { schemas: { ErrorCode: { enum: string[] } } } };
  for (const code of contract.components.schemas.ErrorCode.enum) assert.ok(ERROR_MESSAGES[code], code);
});
