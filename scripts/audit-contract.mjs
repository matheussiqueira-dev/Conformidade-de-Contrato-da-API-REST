import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

export const targetPath = new URL('../config/openapi/target-loja-gestao-openapi-2026-10-03-v2.json', import.meta.url);
export function auditContract(document) {
  const errors = [];
  const operations = [];
  const operationIds = new Set();
  const methods = new Set(['get', 'put', 'post', 'delete', 'patch', 'head', 'options', 'trace']);
  const resolveRef = (ref) => ref.slice(2).split('/').reduce((value, key) =>
    value?.[key.replace(/~1/g, '/').replace(/~0/g, '~')], document);
  function walk(value, location = '#') {
    if (!value || typeof value !== 'object') return;
    if (value.$ref && (!value.$ref.startsWith('#/') || resolveRef(value.$ref) === undefined)) {
      errors.push(`Unresolved or external reference at ${location}: ${value.$ref}`);
    }
    for (const [key, child] of Object.entries(value)) walk(child, `${location}/${key}`);
  }
  walk(document);
  if (document.openapi !== '3.1.0') errors.push('Expected OpenAPI 3.1.0');
  if (document['x-contract-status'] !== 'target-planned-not-implemented') errors.push('Missing planned contract marker');
  for (const [path, item] of Object.entries(document.paths ?? {})) {
    for (const [method, operation] of Object.entries(item)) {
      if (!methods.has(method)) continue;
      const name = `${method.toUpperCase()} ${path}`;
      if (!operation.operationId || operationIds.has(operation.operationId)) errors.push(`Missing or duplicate operationId: ${name}`);
      operationIds.add(operation.operationId);
      if (operation['x-status'] !== 'planned') errors.push(`Missing planned operation marker: ${name}`);
      if (!operation['x-rf']?.length) errors.push(`Missing RF traceability: ${name}`);
      const security = operation.security ?? item.security ?? document.security;
      if (path !== '/auth/login' && !security?.some(s => Object.hasOwn(s, 'bearerAuth'))) errors.push(`Missing bearer security: ${name}`);
      if (path === '/auth/login' && security?.length !== 0) errors.push('Login must be public');
      if ((path.startsWith('/stock/') || path === '/management/summary' || path === '/goals/monthly') &&
          JSON.stringify(operation['x-roles']) !== JSON.stringify(['GERENTE'])) errors.push(`Missing manager policy: ${name}`);
      const parameters = [...(item.parameters ?? []), ...(operation.parameters ?? [])].map(p => p.$ref ? resolveRef(p.$ref) : p);
      for (const match of path.matchAll(/\{([^}]+)\}/g)) {
        if (!parameters.some(p => p?.in === 'path' && p.name === match[1] && p.required === true)) errors.push(`Missing required path parameter: ${name} / ${match[1]}`);
      }
      operations.push({ method: method.toUpperCase(), path, operationId: operation.operationId, roles: operation['x-roles'] ?? [] });
    }
  }
  function checkClosedProjection(schema, location, visited = new Set()) {
    if (!schema || typeof schema !== 'object') return;
    if (schema.$ref) {
      if (!visited.has(schema.$ref)) {
        visited.add(schema.$ref);
        checkClosedProjection(resolveRef(schema.$ref), schema.$ref, visited);
      }
      return;
    }
    if (schema.type === 'object' || schema.properties) {
      if (schema.additionalProperties !== false) errors.push(`Open seller projection: ${location}`);
      for (const [key, child] of Object.entries(schema.properties ?? {})) {
        if (/cost|margin|team/i.test(key)) errors.push(`Sensitive field in seller projection: ${location}/${key}`);
        checkClosedProjection(child, `${location}/${key}`, visited);
      }
    }
    if (schema.items) checkClosedProjection(schema.items, `${location}/items`, visited);
  }
  for (const name of ['SellerPerformance', 'SalesOrderResponse']) {
    checkClosedProjection(document.components?.schemas?.[name], `#/components/schemas/${name}`);
  }
  return { scope: 'structural-policy-audit-only', paths: Object.keys(document.paths ?? {}).length, operations, errors };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const file = process.argv[2] ? resolve(process.argv[2]) : targetPath;
  const result = auditContract(JSON.parse(readFileSync(file, 'utf8')));
  console.log(JSON.stringify(result, null, 2));
  process.exitCode = result.errors.length ? 1 : 0;
}
