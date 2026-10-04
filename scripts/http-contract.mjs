import assert from 'node:assert/strict';
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { checkResponse } from './response-contract.mjs';
import { loadOfficialSchema, validateDocument } from './validate-contract.mjs';

if (process.env.A3_CONTRACT_ISOLATED !== '1') {
  throw new Error('Run scripts/test-contract.ps1: HTTP writes require the disposable database runner.');
}

// Fixed loopback port; the PowerShell runner owns an isolated API and database.
const origin = 'http://127.0.0.1:18080';
const baseline = JSON.parse(readFileSync(new URL('../config/openapi/baseline-errors-openapi-2026-10-04.json', import.meta.url), 'utf8'));
const results = [];
let liveContract;
const directory = new URL('../reports/contrato/spike-2026-10-04/', import.meta.url);
mkdirSync(directory, { recursive: true });
async function request(path, method = 'GET', body) {
  const response = await fetch(origin + path, { method, redirect: 'error', signal: AbortSignal.timeout(10000), headers: { 'Content-Type': 'application/json' }, ...(body === undefined ? {} : { body: JSON.stringify(body) }) });
  return { status: response.status, body: await response.json() };
}
function validateResponse(path, method, response) {
  const result = checkResponse(baseline, path, method, response);
  assert.ok(result.valid, JSON.stringify(result.errors));
}
async function run(name, work) {
  try { await work(); results.push({ name, passed: true }); }
  catch (error) { results.push({ name, passed: false, error: error.message }); }
}
await run('Live OpenAPI is 3.1 and includes baseline routes', async () => {
  const response = await request('/v3/api-docs');
  assert.equal(response.status, 200);
  assert.match(response.body.openapi, /^3\.1\./);
  for (const path of Object.keys(baseline.paths)) assert.ok(response.body.paths[path], path);
  liveContract = response.body;
});
await run('Live OpenAPI documents 400/404 error schemas and validates with official document schema', async () => {
  assert.ok(liveContract, 'Live contract unavailable');
  const validation = validateDocument(liveContract, await loadOfficialSchema());
  assert.deepEqual(validation.findings, []);
  for (const [path, method, status] of [['/products', 'post', '400'], ['/products/{id}', 'get', '404']]) {
    const schema = liveContract.paths[path][method].responses[status]?.content?.['application/json']?.schema;
    assert.ok(schema, `Missing documented ${status}`);
    const resolved = schema.$ref ? schema.$ref.slice(2).split('/').reduce((value, key) => value[key], liveContract) : schema;
    for (const field of ['timestamp', 'status', 'error', 'message']) assert.ok(resolved.required?.includes(field), `Missing required error field ${field}`);
    assert.equal(resolved.properties.timestamp.format, 'local-date-time');
  }
  writeFileSync(new URL('openapi-runtime-errors.json', directory), JSON.stringify(liveContract, null, 2) + '\n');
});
await run('GET products matches baseline response schema', async () => {
  const response = await request('/products'); assert.equal(response.status, 200);
  validateResponse('/products', 'get', response);
});
await run('Synthetic product creation and retrieval match baseline', async () => {
  const response = await request('/products', 'POST', { type: 'PHYSICAL', name: 'HTTP contract synthetic product', price: 4500, weight: 1 });
  assert.equal(response.status, 200); validateResponse('/products', 'post', response);
  assert.ok(Number.isSafeInteger(response.body.id));
  const found = await request(`/products/${response.body.id}`);
  assert.equal(found.status, 200); validateResponse('/products/{id}', 'get', found);
  assert.equal(found.body.price, 4500);
});
await run('Invalid product returns 400 validation error', async () => {
  const response = await request('/products', 'POST', {});
  assert.equal(response.status, 400); assert.equal(response.body.status, 400);
  validateResponse('/products', 'post', response);
  const live = checkResponse(liveContract, '/products', 'post', response);
  assert.ok(live.valid, JSON.stringify(live.errors));
  assert.ok(Array.isArray(response.body.details) && response.body.details.length > 0);
});
await run('Missing product returns 404', async () => {
  const response = await request('/products/9223372036854775807');
  assert.equal(response.status, 404); assert.equal(response.body.status, 404);
  validateResponse('/products/{id}', 'get', response);
  const live = checkResponse(liveContract, '/products/{id}', 'get', response);
  assert.ok(live.valid, JSON.stringify(live.errors));
});
const report = { scope: 'baseline-http-isolated-database', timestamp: new Date().toISOString(), results,
  contract: 'baseline-errors-openapi-2026-10-04.json',
  contractGaps: ['Error documentation scope: POST /products 400 and GET /products/{id} 404 only.'],
  excluded: ['Planned login, 401/403 and role-specific projections require API-01 implementation.'] };
writeFileSync(new URL('http-swagger-errors-v1.json', directory), JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify(report, null, 2));
process.exitCode = results.some(result => !result.passed) ? 1 : 0;
