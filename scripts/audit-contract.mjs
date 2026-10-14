import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

export const targetPath = new URL('../config/openapi/target-loja-gestao-openapi-2026-10-04-v3.json', import.meta.url);
export const v4Path = new URL('../config/openapi/target-loja-gestao-openapi-2026-10-14-v4.json', import.meta.url);
export const matrixPath = new URL('../src/test/resources/security/access-matrix.csv', import.meta.url);
// Contract marker -> accepted per-operation x-status values. v2/v3 stay planning-only documents.
export const contractMarkers = {
  'target-planned-not-implemented': ['planned'],
  'target-v4': ['planned', 'implemented'],
};
const roleNames = ['GERENTE', 'VENDEDOR'];
const granted = new Set(['PERMIT', 'OWN']);
export function parseMatrix(text) {
  const [header, ...lines] = text.split(/\r?\n/).filter(line => line.trim() !== '');
  if (header !== 'method,path,anonymous,vendedor,gerente') throw new Error('Unexpected access matrix header');
  return lines.map(line => {
    const [method, path, anonymous, vendedor, gerente] = line.split(',');
    return { method, path, anonymous, vendedor, gerente };
  });
}
export function auditContract(document, { matrix } = {}) {
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
  const session = Boolean(document.components?.securitySchemes?.sessionAuth);
  const securityName = session ? 'sessionAuth' : 'bearerAuth';
  if (session) {
    const scheme = document.components.securitySchemes.sessionAuth;
    if (scheme.type !== 'apiKey' || scheme.in !== 'cookie' || scheme.name !== 'JSESSIONID') errors.push('Invalid session cookie security scheme');
    if (!document.paths?.['/auth/csrf']?.get || !document.paths?.['/auth/logout']?.post) errors.push('Missing CSRF or logout operation');
    const response = document.components.schemas?.AuthResponse;
    if (response?.additionalProperties !== false || response?.properties?.accessToken || response?.properties?.tokenType) errors.push('Session login must not expose bearer tokens');
  }
  if (document.openapi !== '3.1.0') errors.push('Expected OpenAPI 3.1.0');
  const statuses = contractMarkers[document['x-contract-status']];
  const v4 = document['x-contract-status'] === 'target-v4';
  if (!statuses) errors.push('Missing planned contract marker');
  for (const [path, item] of Object.entries(document.paths ?? {})) {
    for (const [method, operation] of Object.entries(item)) {
      if (!methods.has(method)) continue;
      const name = `${method.toUpperCase()} ${path}`;
      if (!operation.operationId || operationIds.has(operation.operationId)) errors.push(`Missing or duplicate operationId: ${name}`);
      operationIds.add(operation.operationId);
      if (!statuses?.includes(operation['x-status'])) errors.push(`Missing planned operation marker: ${name}`);
      if (!operation['x-rf']?.length) errors.push(`Missing RF traceability: ${name}`);
      const security = operation.security ?? item.security ?? document.security;
      const isPublic = path === '/auth/login' || (session && ['/auth/csrf', '/auth/logout'].includes(path));
      if (!isPublic && (!security?.length || security.some(s => !Object.hasOwn(s, securityName)))) errors.push(`Missing ${session ? 'session' : 'bearer'} security: ${name}`);
      if (isPublic && security?.length !== 0) errors.push(`Auth bootstrap/logout must be public: ${name}`);
      if ((path.startsWith('/stock/') || path === '/management/summary' || path === '/goals/monthly') &&
          JSON.stringify(operation['x-roles']) !== JSON.stringify(['GERENTE'])) errors.push(`Missing manager policy: ${name}`);
      const parameters = [...(item.parameters ?? []), ...(operation.parameters ?? [])].map(p => p.$ref ? resolveRef(p.$ref) : p);
      if (session && ['post', 'put', 'patch', 'delete'].includes(method)) {
        if (!parameters.some(p => p?.in === 'header' && p.name === 'X-CSRF-TOKEN' && p.required === true)) errors.push(`Missing required CSRF header: ${name}`);
        if (!operation.responses?.['403']) errors.push(`Missing CSRF rejection response: ${name}`);
      }
      if (v4) auditV4Operation(path, method, name, operation, parameters, security);
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
  function auditV4Operation(path, method, name, operation, parameters, security) {
    const roles = operation['x-roles'];
    if (!Array.isArray(roles) || roles.some(r => !roleNames.includes(r))) errors.push(`Invalid x-roles: ${name}`);
    const isPublic = Array.isArray(security) && security.length === 0;
    if (isPublic !== (roles?.length === 0)) errors.push(`x-roles and security disagree: ${name}`);
    if (method === 'post' && path === '/sales/orders' && !parameters.some(p =>
      p?.in === 'header' && p.name === 'Idempotency-Key' && p.required === true && p.schema?.format === 'uuid')) {
      errors.push(`Missing required Idempotency-Key: ${name}`);
    }
    if (roles?.includes('VENDEDOR')) {
      for (const [status, response] of Object.entries(operation.responses ?? {})) {
        if (!status.startsWith('2')) continue;
        const resolved = response.$ref ? resolveRef(response.$ref) : response;
        for (const media of Object.values(resolved?.content ?? {})) checkSellerSafe(media.schema, `${name} ${status}`);
      }
    }
    if (!matrix) return;
    const row = matrix.find(r => r.method === method.toUpperCase() && r.path === path);
    if (!row) { errors.push(`Operation missing from access matrix: ${name}`); return; }
    const matrixRoles = roleNames.filter(role => granted.has(row[role.toLowerCase()]));
    // Public operations declare x-roles: [] and must be open to everyone in the matrix.
    const expected = isPublic ? (matrixRoles.length === roleNames.length ? [] : matrixRoles) : matrixRoles;
    if (JSON.stringify([...(roles ?? [])].sort()) !== JSON.stringify(expected)) errors.push(`x-roles differ from access matrix: ${name}`);
    if ((row.anonymous === 'PERMIT') !== isPublic) errors.push(`Anonymous access differs from access matrix: ${name}`);
  }
  // v4: every success response a seller can reach is closed and has no cost, margin, profit or team field.
  function checkSellerSafe(schema, location, visited = new Set()) {
    if (!schema || typeof schema !== 'object') return;
    if (schema.$ref) {
      if (!visited.has(schema.$ref)) {
        visited.add(schema.$ref);
        checkSellerSafe(resolveRef(schema.$ref), `${location} -> ${schema.$ref}`, visited);
      }
      return;
    }
    if (schema.properties && schema.additionalProperties !== false) errors.push(`Open seller projection: ${location}`);
    for (const [key, child] of Object.entries(schema.properties ?? {})) {
      if (/cost|margin|profit|team/i.test(key)) errors.push(`Sensitive field in seller projection: ${location}/${key}`);
      checkSellerSafe(child, `${location}/${key}`, visited);
    }
    for (const key of ['oneOf', 'anyOf', 'allOf']) for (const child of schema[key] ?? []) checkSellerSafe(child, location, visited);
    if (schema.items) checkSellerSafe(schema.items, `${location}/items`, visited);
  }
  // v4: every object schema, request or response, rejects unknown properties.
  function checkClosed(schema, location) {
    if (!schema || typeof schema !== 'object') return;
    if (schema.properties && schema.additionalProperties !== false) errors.push(`Open v4 schema: ${location}`);
    for (const [prop, child] of Object.entries(schema.properties ?? {})) checkClosed(child, `${location}/${prop}`);
    for (const key of ['oneOf', 'anyOf', 'allOf']) (schema[key] ?? []).forEach((child, i) => checkClosed(child, `${location}/${key}/${i}`));
    if (schema.items) checkClosed(schema.items, `${location}/items`);
  }
  if (v4) for (const [name, schema] of Object.entries(document.components?.schemas ?? {})) checkClosed(schema, `#/components/schemas/${name}`);
  return { scope: 'structural-policy-audit-only', paths: Object.keys(document.paths ?? {}).length, operations, errors };
}

if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const [file = fileURLToPath(targetPath), matrixFile = fileURLToPath(matrixPath)] = process.argv.slice(2).map(arg => resolve(arg));
  const document = JSON.parse(readFileSync(file, 'utf8'));
  const matrix = document['x-contract-status'] === 'target-v4' ? parseMatrix(readFileSync(matrixFile, 'utf8')) : undefined;
  const result = auditContract(document, { matrix });
  console.log(JSON.stringify(result, null, 2));
  process.exitCode = result.errors.length ? 1 : 0;
}
