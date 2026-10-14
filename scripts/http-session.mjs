import assert from 'node:assert/strict';
import { readFileSync, mkdirSync, writeFileSync } from 'node:fs';
import { sessionClient } from './session-client.mjs';
import { checkResponse } from './response-contract.mjs';
if (process.env.A3_CONTRACT_ISOLATED !== '1' || !process.env.A3_TEST_PASSWORD) throw new Error('Use the disposable database runner.');
const origin = 'http://127.0.0.1:18080';
const target = JSON.parse(readFileSync(new URL('../config/openapi/target-loja-gestao-openapi-2026-10-04-v3.json', import.meta.url)));
const results = [];
async function run(name, work) {
  try { await work(); results.push({ name, passed: true }); }
  catch (error) { results.push({ name, passed: false, error: error.message }); }
}
function schema(path, method, response) {
  const result = checkResponse(target, path, method, response);
  assert.ok(result.valid, JSON.stringify(result.errors));
}
const seller = sessionClient(origin);
await run('Anonymous me returns 401; login without CSRF returns 403', async () => {
  const response = await seller.request('/auth/me');
  assert.equal(response.status, 401);
  const login = await seller.request('/auth/login', 'POST', { email: 'seller@example.test', password: process.env.A3_TEST_PASSWORD }, { sendCsrf: false });
  assert.equal(login.status, 403);
});
await run('Wrong credentials return generic 401', async () => {
  const response = await seller.login('seller@example.test', 'deliberately-wrong-password');
  assert.equal(response.status, 401);
});
await run('Login rotates session cookie and exposes only user projection', async () => {
  await seller.refreshCsrf();
  const original = seller.cookie();
  const response = await seller.request('/auth/login', 'POST', { email: 'seller@example.test', password: process.env.A3_TEST_PASSWORD });
  assert.equal(response.status, 200); schema('/auth/login', 'post', response);
  assert.ok(seller.cookie()); assert.notEqual(seller.cookie(), original);
  assert.ok(seller.setCookies().some(cookie => /JSESSIONID=/.test(cookie) && /HttpOnly/i.test(cookie) && /SameSite=Lax/i.test(cookie)));
  assert.equal(response.body.user.role, 'VENDEDOR');
  await seller.refreshCsrf();
  const me = await seller.request('/auth/me');
  assert.equal(me.status, 200); schema('/auth/me', 'get', me);
  assert.equal(me.body.id, response.body.user.id);
});
await run('Seller cannot read legacy customer, payment, product or order data', async () => {
  for (const path of ['/products', '/customers', '/orders', '/payment', '/address']) assert.equal((await seller.request(path)).status, 403, path);
});
await run('Mutations without CSRF fail even with manager session', async () => {
  const manager = sessionClient(origin);
  assert.equal((await manager.login('manager@example.test', process.env.A3_TEST_PASSWORD)).status, 200);
  assert.equal((await manager.request('/products', 'POST', {}, { sendCsrf: false })).status, 403);
});
await run('Logout requires CSRF and invalidates the previous cookie', async () => {
  assert.equal((await seller.request('/auth/logout', 'POST', undefined, { sendCsrf: false })).status, 403);
  const original = seller.cookie();
  assert.equal((await seller.request('/auth/logout', 'POST')).status, 204);
  const stale = await fetch(origin + '/auth/me', { headers: { Cookie: `JSESSIONID=${original}` }, signal: AbortSignal.timeout(10000) });
  assert.equal(stale.status, 401);
  await seller.refreshCsrf();
  assert.equal((await seller.request('/auth/logout', 'POST')).status, 204);
});
const directory = new URL('../reports/contrato/session-2026-10-04/', import.meta.url);
mkdirSync(directory, { recursive: true });
const report = { scope: 'real-session-http-disposable-database', timestamp: new Date().toISOString(), results };
writeFileSync(new URL('http-session.json', directory), JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify(report, null, 2));
process.exitCode = results.some(result => !result.passed) ? 1 : 0;
