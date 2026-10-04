import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { auditContract, targetPath } from './audit-contract.mjs';
const target = JSON.parse(readFileSync(targetPath, 'utf8'));
test('planned target has references and profile policies', () => {
  const result = auditContract(target);
  assert.deepEqual(result.errors, []);
  assert.equal(result.paths, 10);
  assert.equal(result.operations.length, 11);
});
test('broken reference is detected', () => {
  const doc = structuredClone(target);
  doc.components.schemas.SellerPerformance.properties.seller.$ref = '#/components/schemas/Missing';
  assert.ok(auditContract(doc).errors.some(e => e.includes('Unresolved')));
});
test('protected endpoint cannot declare public security', () => {
  const doc = structuredClone(target);
  doc.paths['/stock/products'].get.security = [];
  assert.ok(auditContract(doc).errors.some(e => e.includes('Missing bearer')));
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
