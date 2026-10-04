import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { auditContract, targetPath } from './audit-contract.mjs';
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
