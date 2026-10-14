import test from 'node:test';
import assert from 'node:assert/strict';
import { z } from 'zod';
import { ApiError, ContractError, createClient, type Fetcher } from '../lib/api/http.ts';
import { authApi } from '../lib/api/auth.ts';

interface Call { path: string; init: RequestInit }

function fake(responses: Array<{ status: number; body?: unknown }>): { fetcher: Fetcher; calls: Call[] } {
  const calls: Call[] = [];
  const fetcher: Fetcher = async (path, init) => {
    calls.push({ path, init });
    const next = responses.shift() ?? { status: 200, body: {} };
    return new Response(next.status === 204 ? null : JSON.stringify(next.body ?? {}), { status: next.status });
  };
  return { fetcher, calls };
}
const headers = (call: Call | undefined) => (call?.init.headers ?? {}) as Record<string, string>;
const CSRF = { status: 200, body: { headerName: 'X-CSRF-TOKEN', token: 'fresh-token' } };

test('mutacao busca CSRF uma vez, envia o cabecalho e usa cookies da mesma origem', async () => {
  const { fetcher, calls } = fake([CSRF, { status: 200, body: { ok: true } }, { status: 200, body: { ok: true } }]);
  const client = createClient({ fetcher });
  await client.request('/sales/quote', { method: 'POST', body: { items: [] } });
  await client.request('/sales/quote', { method: 'POST', body: { items: [] } });
  assert.deepEqual(calls.map(c => c.path), ['/api/backend/auth/csrf', '/api/backend/sales/quote', '/api/backend/sales/quote']);
  assert.equal(headers(calls[1])['X-CSRF-TOKEN'], 'fresh-token');
  for (const call of calls) { assert.equal(call.init.credentials, 'same-origin'); assert.equal(call.init.cache, 'no-store'); }
});

test('POST /sales/orders envia Idempotency-Key; GET nao envia CSRF', async () => {
  const { fetcher, calls } = fake([CSRF, { status: 201, body: { id: 1 } }, { status: 200, body: {} }]);
  const client = createClient({ fetcher });
  await client.request('/sales/orders', { method: 'POST', body: {}, idempotencyKey: 'key-123' });
  await client.request('/sales/catalog');
  assert.equal(headers(calls[1])['Idempotency-Key'], 'key-123');
  assert.equal(headers(calls[2])['X-CSRF-TOKEN'], undefined);
});

test('403 em mutacao renova o CSRF uma vez (token trocado no login)', async () => {
  const { fetcher, calls } = fake([CSRF, { status: 403, body: { code: 'FORBIDDEN' } }, CSRF, { status: 200, body: { ok: 1 } }]);
  await createClient({ fetcher }).request('/orders/1/cancel', { method: 'POST' });
  assert.equal(calls.length, 4);
});

test('ErrorResponse.code vira mensagem em pt-BR sem codigo HTTP; details preservados', async () => {
  const { fetcher } = fake([{ status: 409, body: { status: 409, code: 'STOCK_INSUFFICIENT', details: ['productId=7; available=2'] } }]);
  await assert.rejects(createClient({ fetcher }).request('/sales/catalog'), (error: unknown) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.code, 'STOCK_INSUFFICIENT');
    assert.deepEqual(error.details, ['productId=7; available=2']);
    assert.match(error.message, /Estoque insuficiente/);
    assert.doesNotMatch(error.message, /409/);
    return true;
  });
});

test('401 fora do login chama onUnauthorized; corpo do servidor nao vaza na mensagem', async () => {
  let redirected = 0;
  const { fetcher } = fake([{ status: 401, body: { private: 'database credentials' } }]);
  await assert.rejects(createClient({ fetcher, onUnauthorized: () => { redirected++; } }).request('/auth/me'),
    (error: unknown) => error instanceof ApiError && error.status === 401 && !error.message.includes('database'));
  assert.equal(redirected, 1);
});

test('resposta fora do contrato falha em teste (safeParse)', async () => {
  const { fetcher } = fake([{ status: 200, body: { id: 'x' } }]);
  await assert.rejects(createClient({ fetcher, validate: true }).request('/x', { schema: z.strictObject({ id: z.int() }) }),
    (error: unknown) => error instanceof ContractError);
});

test('falha de rede vira ApiError com mensagem de conexao', async () => {
  const client = createClient({ fetcher: async () => { throw new TypeError('fetch failed'); } });
  await assert.rejects(client.request('/auth/me'), (error: unknown) => error instanceof ApiError && error.status === 0 && /conexão/.test(error.message));
});

test('login: mesma mensagem generica no 401 e token renovado antes e depois', async () => {
  const { fetcher, calls } = fake([CSRF, { status: 401, body: { code: 'UNAUTHENTICATED' } }]);
  await assert.rejects(authApi(createClient({ fetcher })).login('ana@example.test', 'senha-errada'),
    (error: unknown) => error instanceof ApiError && error.message === 'E-mail ou senha inválidos.');
  assert.deepEqual(calls.map(c => c.path), ['/api/backend/auth/csrf', '/api/backend/auth/login']);
});
