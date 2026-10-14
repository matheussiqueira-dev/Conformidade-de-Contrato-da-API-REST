import test from 'node:test';
import assert from 'node:assert/strict';
import { landingPath, menuFor } from '../lib/areas.ts';
import { isValidCnpj, isValidCpf } from '../lib/documents.ts';
import { digits, formatPercent, money, parseDecimal } from '../lib/format.ts';
import { messageFor } from '../lib/api/messages.ts';
import { contractResolver } from '../lib/forms.ts';
import { CreateCustomerBody } from '../lib/api/generated/sbi.zod.ts';

const SELLER = ['VENDER', 'VENDAS', 'CLIENTES', 'MEU_DESEMPENHO'];
const MANAGER = [...SELLER, 'GESTAO', 'PRODUTOS', 'ESTOQUE', 'PROMOCOES', 'ANALISES'];

test('menu vem das areas do servidor; vendedor nao ve Gestao (CT-WEB-103)', () => {
  assert.deepEqual(menuFor(SELLER).map(i => i.href), ['/vender', '/vendas', '/clientes', '/meu-desempenho']);
  assert.equal(menuFor(MANAGER).length, 9);
  assert.ok(menuFor(MANAGER).some(i => i.href === '/gestao'));
  assert.deepEqual(menuFor([]), []);
});

test('destino depois do login respeita ?next= interno e ignora destino externo', () => {
  assert.equal(landingPath(SELLER), '/vender');
  assert.equal(landingPath(SELLER, '/clientes/3'), '/clientes/3');
  for (const next of ['//evil.test', 'https://evil.test', '/api/backend/auth/me', '/entrar']) assert.equal(landingPath(SELLER, next), '/vender');
});

test('CPF e CNPJ conferem digito verificador', () => {
  assert.ok(isValidCpf('529.982.247-25'));
  assert.ok(!isValidCpf('529.982.247-26'));
  assert.ok(!isValidCpf('111.111.111-11'));
  assert.ok(isValidCnpj('11.222.333/0001-81'));
  assert.ok(!isValidCnpj('11.222.333/0001-82'));
  assert.equal(digits('(85) 99999-0000'), '85999990000');
});

test('dinheiro e percentual em pt-BR; ausente vira travessao', () => {
  assert.equal(money({ amount: 1234.5, currency: 'BRL' }), 'R$ 1.234,50');
  assert.equal(money(null), '—');
  assert.equal(formatPercent(85.5), '85,5%');
  assert.equal(formatPercent(null), '—');
});

test('mensagens por codigo, com fallback por status e sem codigo HTTP', () => {
  assert.match(messageFor('CONCURRENT_UPDATE', 409), /recarregue/i);
  assert.match(messageFor(undefined, 403), /Sem acesso/);
  assert.doesNotMatch(messageFor('INTERNAL_ERROR', 500), /500/);
});

test('formulario de cliente validado pelo schema gerado do contrato', async () => {
  const resolver = contractResolver<{ type: string; name: string; cpf: string; email: string; phone: string }>(CreateCustomerBody, v => v);
  const invalid = await resolver({ type: 'INDIVIDUAL', name: 'A', cpf: '123', email: 'x', phone: '1' }, undefined, { fields: {}, shouldUseNativeValidation: false });
  assert.ok(Object.keys(invalid.errors).length > 0);
  const valid = await resolver({ type: 'INDIVIDUAL', name: 'Allan Barros', cpf: '529.982.247-25', email: 'allan@cliente.test', phone: '85999990000' },
    undefined, { fields: {}, shouldUseNativeValidation: false });
  assert.deepEqual(valid.errors, {});
});

test('valor digitado aceita virgula ou ponto decimal', () => {
  assert.equal(parseDecimal('59.90'), 59.9);
  assert.equal(parseDecimal('59,90'), 59.9);
  assert.equal(parseDecimal('1.234,56'), 1234.56);
  assert.ok(Number.isNaN(parseDecimal('abc')));
});
