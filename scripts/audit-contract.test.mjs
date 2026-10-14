import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { auditContract, parseMatrix, targetPath, v4Path, matrixPath } from './audit-contract.mjs';
const target = JSON.parse(readFileSync(targetPath, 'utf8'));
test('planned target has references and profile policies', () => {
  const result = auditContract(target);
  assert.deepEqual(result.errors, []);
  assert.equal(result.paths, 12);
  assert.equal(result.operations.length, 13);
});
test('broken reference is detected', () => {
  const doc = structuredClone(target);
  doc.components.schemas.SellerPerformance.properties.seller.$ref = '#/components/schemas/Missing';
  assert.ok(auditContract(doc).errors.some(e => e.includes('Unresolved')));
});
test('protected endpoint cannot declare public security', () => {
  const doc = structuredClone(target);
  doc.paths['/stock/products'].get.security = [];
  assert.ok(auditContract(doc).errors.some(e => e.includes('Missing session')));
});

test('anonymous alternative cannot bypass session policy', () => {
  const doc = structuredClone(target);
  doc.paths['/stock/products'].get.security = [{ sessionAuth: [] }, {}];
  assert.ok(auditContract(doc).errors.some(e => e.includes('Missing session')));
});

test('login and logout require CSRF and document rejection', () => {
  for (const path of ['/auth/login', '/auth/logout']) {
    const doc = structuredClone(target);
    doc.paths[path].post.parameters = [];
    assert.ok(auditContract(doc).errors.some(e => e.includes('Missing required CSRF')));
  }
});

test('session login rejects bearer response and malformed cookie scheme', () => {
  const doc = structuredClone(target);
  doc.components.schemas.AuthResponse.properties.accessToken = { type: 'string' };
  doc.components.securitySchemes.sessionAuth.in = 'header';
  const errors = auditContract(doc).errors;
  assert.ok(errors.some(e => e.includes('must not expose')));
  assert.ok(errors.some(e => e.includes('cookie security')));
});

test('archived v2 still passes its original bearer policies', () => {
  const doc = JSON.parse(readFileSync(new URL('../config/openapi/target-loja-gestao-openapi-2026-10-03-v2.json', import.meta.url)));
  assert.deepEqual(auditContract(doc).errors, []);
});
test('manager policy cannot admit a seller', () => {
  const doc = structuredClone(target);
  doc.paths['/management/summary'].get['x-roles'] = ['GERENTE', 'VENDEDOR'];
  assert.ok(auditContract(doc).errors.some(e => e.includes('Missing manager')));
});
test('seller projection cannot allow unknown root or nested fields', () => {
  for (const name of ['SellerPerformance', 'UserSession', 'Money']) {
    const doc = structuredClone(target);
    delete doc.components.schemas[name].additionalProperties;
    assert.ok(auditContract(doc).errors.some(e => e.includes('Open seller')));
  }
});
test('explicit sensitive field in seller projection is detected', () => {
  const doc = structuredClone(target);
  doc.components.schemas.SellerPerformance.properties.cost = { type: 'number' };
  assert.ok(auditContract(doc).errors.some(e => e.includes('Sensitive field')));
});
test('required path parameters cannot disappear', () => {
  const doc = structuredClone(target);
  doc.paths['/orders/{id}/cancel'].post.parameters = [];
  assert.ok(auditContract(doc).errors.some(e => e.includes('Missing required path')));
});

const v4 = JSON.parse(readFileSync(v4Path, 'utf8'));
const matrix = parseMatrix(readFileSync(matrixPath, 'utf8'));
const auditV4 = (doc, rows = matrix) => auditContract(doc, { matrix: rows }).errors;
test('v4 draft passes with the executable access matrix', () => {
  const result = auditContract(v4, { matrix });
  assert.deepEqual(result.errors, []);
  assert.equal(result.paths, 35);
  assert.equal(result.operations.length, 45);
});
test('v4 accepts implemented operations; v3 stays planning-only', () => {
  const doc = structuredClone(v4);
  doc.paths['/auth/me'].get['x-status'] = 'implemented';
  assert.deepEqual(auditV4(doc), []);
  doc.paths['/auth/me'].get['x-status'] = 'done';
  assert.ok(auditV4(doc).some(e => e.includes('Missing planned operation marker')));
  const v3 = structuredClone(target);
  v3.paths['/auth/me'].get['x-status'] = 'implemented';
  assert.ok(auditContract(v3).errors.some(e => e.includes('Missing planned operation marker')));
});
test('v4 x-roles must match the access matrix', () => {
  const rows = structuredClone(matrix);
  rows.find(r => r.method === 'GET' && r.path === '/stock/products').vendedor = 'PERMIT';
  assert.ok(auditV4(v4, rows).some(e => e.includes('x-roles differ') && e.includes('/stock/products')));
  const missing = matrix.filter(r => r.path !== '/sales/quote');
  assert.ok(auditV4(v4, missing).some(e => e.includes('missing from access matrix')));
  const anonymous = structuredClone(matrix);
  anonymous.find(r => r.method === 'GET' && r.path === '/auth/me').anonymous = 'PERMIT';
  assert.ok(auditV4(v4, anonymous).some(e => e.includes('Anonymous access differs')));
});
test('v4 seller responses stay closed and free of cost data', () => {
  const cost = structuredClone(v4);
  cost.components.schemas.CatalogItem.properties.avgCost = { $ref: '#/components/schemas/Money' };
  assert.ok(auditV4(cost).some(e => e.includes('Sensitive field') && e.includes('avgCost')));
  const open = structuredClone(v4);
  delete open.components.schemas.CustomerOrderSummary.additionalProperties;
  assert.ok(auditV4(open).some(e => e.includes('Open seller projection')));
});
test('v4 request schemas reject unknown properties', () => {
  const doc = structuredClone(v4);
  delete doc.components.schemas.StockAdjustmentRequest.additionalProperties;
  assert.ok(auditV4(doc).some(e => e.includes('Open v4 schema') && e.includes('StockAdjustmentRequest')));
});
test('v4 sale creation requires Idempotency-Key and protected operations keep session security', () => {
  const key = structuredClone(v4);
  key.paths['/sales/orders'].post.parameters = key.paths['/sales/orders'].post.parameters
    .filter(p => p.$ref !== '#/components/parameters/IdempotencyKey');
  assert.ok(auditV4(key).some(e => e.includes('Idempotency-Key')));
  const open = structuredClone(v4);
  open.paths['/sales/catalog'].get.security = [];
  const errors = auditV4(open);
  assert.ok(errors.some(e => e.includes('x-roles and security disagree')));
  assert.ok(errors.some(e => e.includes('Missing session')));
});
