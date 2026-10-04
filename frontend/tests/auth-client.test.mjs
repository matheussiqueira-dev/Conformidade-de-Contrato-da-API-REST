import test from 'node:test';
import assert from 'node:assert/strict';
import { authClient, ApiError } from '../lib/auth-client.mjs';

test('login sends CSRF header, uses same origin cookies and refreshes token after authentication', async () => {
  const calls = [];
  const api = authClient(async (path, options) => {
    calls.push({ path, options });
    return { ok: true, status: 200, json: async () => path.endsWith('/csrf') ? { headerName:'X-CSRF-TOKEN', token:'masked-test-token' } : { user: { id:1, role:'VENDEDOR' } } };
  });
  assert.equal((await api.login('ana@example.test', 'synthetic-password')).id, 1);
  assert.deepEqual(calls.map(c => c.path), ['/api/backend/auth/csrf', '/api/backend/auth/login', '/api/backend/auth/csrf']);
  assert.equal(calls[1].options.headers['X-CSRF-TOKEN'], 'masked-test-token');
  for (const { options } of calls) { assert.equal(options.credentials, 'same-origin'); assert.equal(options.cache, 'no-store'); }
});
test('logout obtains a new token and accepts an empty 204 body', async () => {
  const calls = [];
  const api = authClient(async (path, options) => {
    calls.push({ path, options });
    return { ok:true, status:path.endsWith('/logout') ? 204 : 200, json: async () => ({ headerName:'X-CSRF-TOKEN', token:'fresh' }) };
  });
  await api.logout();
  assert.equal(calls[1].options.headers['X-CSRF-TOKEN'], 'fresh');
  assert.equal(calls[1].options.method, 'POST');
});
test('401 and 403 surface errors without retrying credentials or leaking server response', async () => {
  for (const status of [401, 403]) {
    const api = authClient(async () => ({ ok:false, status, json: async () => ({ private:'database credentials' }) }));
    await assert.rejects(api.me(), error => error instanceof ApiError && error.status === status && !error.message.includes('database'));
  }
});
