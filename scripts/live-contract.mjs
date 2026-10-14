// CT-CONTRACT-102: o OpenAPI ao vivo e as respostas reais conferem com o contrato alvo (v5 = v4 + evolucao).
// 1) Estrutura: toda operacao implemented existe em /v3/api-docs com os mesmos parametros, corpo e codigos de sucesso.
//    Operacao planned ainda nao existe na API (C-02) e fica fora da comparacao.
// 2) Comportamento: cada operacao e chamada na API isolada e a resposta (sucesso e erro) e validada com Ajv.
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { randomUUID } from 'node:crypto';
import { sessionClient } from './session-client.mjs';
import { checkResponse } from './response-contract.mjs';

if (process.env.A3_CONTRACT_ISOLATED !== '1' || !process.env.A3_TEST_PASSWORD) {
  throw new Error('Run scripts/test-contract.ps1: live checks require the disposable database runner.');
}
const origin = 'http://127.0.0.1:18080';
const v4 = JSON.parse(readFileSync(new URL('../config/openapi/target-loja-gestao-openapi-2026-10-14-v5.json', import.meta.url), 'utf8'));
const METHODS = ['get', 'post', 'put', 'patch', 'delete'];
const findings = [];
const exercised = new Map();

function operations(document) {
  const list = [];
  for (const [path, item] of Object.entries(document.paths)) {
    for (const method of METHODS) if (item[method]) list.push({ path, method, operation: item[method] });
  }
  return list;
}
function resolve(document, node) {
  return node?.$ref ? node.$ref.slice(2).split('/').reduce((value, key) => value?.[key], document) : node;
}
function parameters(document, operation) {
  return (operation.parameters ?? []).map(p => resolve(document, p))
    .map(p => `${p.in}:${p.name}:${p.required === true}`).sort();
}

// ---------------- 1) estrutura ----------------
const anonymous = sessionClient(origin);
const live = (await anonymous.request('/v3/api-docs')).body;
for (const { path, method, operation } of operations(v4)) {
  if (operation['x-status'] !== 'implemented') continue;
  const name = `${method.toUpperCase()} ${path}`;
  const liveOperation = live.paths?.[path]?.[method];
  if (!liveOperation) { findings.push(`${name}: missing in live OpenAPI`); continue; }
  const expected = parameters(v4, operation);
  const actual = parameters(live, liveOperation);
  if (JSON.stringify(expected) !== JSON.stringify(actual)) {
    findings.push(`${name}: parameters differ (v4 ${expected.join(', ')} | live ${actual.join(', ')})`);
  }
  if (Boolean(operation.requestBody) !== Boolean(liveOperation.requestBody)) findings.push(`${name}: request body presence differs`);
  for (const status of Object.keys(operation.responses).filter(s => s.startsWith('2'))) {
    if (!liveOperation.responses?.[status]) findings.push(`${name}: success status ${status} not documented live`);
  }
}

// ---------------- 2) comportamento ----------------
function record(path, method, response, expected) {
  const name = `${method.toUpperCase()} ${path}`;
  if (response.status !== expected) {
    findings.push(`${name}: expected ${expected}, got ${response.status} ${JSON.stringify(response.body)?.slice(0, 300)}`);
    return response;
  }
  if (response.status !== 204) {
    const result = checkResponse(v4, path, method, response);
    if (!result.valid) findings.push(`${name} ${response.status}: ${JSON.stringify(result.errors).slice(0, 400)}`);
  }
  if (!v4.paths[path][method].responses[String(response.status)]) findings.push(`${name}: status ${response.status} not in the contract`);
  exercised.set(name, [...(exercised.get(name) ?? []), response.status]);
  return response;
}
const brl = amount => ({ amount, currency: 'BRL' });
const password = process.env.A3_TEST_PASSWORD;
const manager = sessionClient(origin);
const seller = sessionClient(origin);

record('/auth/me', 'get', await anonymous.request('/auth/me'), 401);
record('/auth/csrf', 'get', await manager.refreshCsrf(), 200);
record('/auth/login', 'post', await manager.request('/auth/login', 'POST', { email: 'manager@example.test', password }), 200);
await manager.refreshCsrf();
record('/auth/login', 'post', await seller.login('seller@example.test', 'deliberately-wrong'), 401);
record('/auth/login', 'post', await seller.login('seller@example.test', password), 200);
const sellerId = record('/auth/me', 'get', await seller.request('/auth/me'), 200).body.id;
record('/auth/me', 'get', await manager.request('/auth/me'), 200);

// Catalogo e estoque (GERENTE)
const stamp = Date.now();
const category = record('/categories', 'post', await manager.request('/categories', 'POST', { name: `Contrato ${stamp}` }), 201).body;
record('/categories', 'post', await manager.request('/categories', 'POST', { name: `Contrato ${stamp}` }), 409);
record('/categories', 'get', await manager.request('/categories'), 200);
record('/categories/{id}', 'put', await manager.request(`/categories/${category.id}`, 'PUT', { name: `Contrato ${stamp} B` }), 200);
const product = record('/products', 'post', await manager.request('/products', 'POST', { type: 'PHYSICAL', sku: `ct-${stamp}`,
  name: 'Produto do contrato', categoryId: category.id, sellable: true, price: brl(50), weightKg: 0.5, minLevel: 1 }), 201).body;
const digital = record('/products', 'post', await manager.request('/products', 'POST', { type: 'DIGITAL', sku: `dg-${stamp}`,
  name: 'Guia digital', sellable: true, price: brl(10), downloadLink: 'https://loja.test/guia.pdf' }), 201).body;
record('/products', 'post', await manager.request('/products', 'POST', { type: 'PHYSICAL' }), 400);
record('/products', 'get', await manager.request('/products?q=produto'), 200);
record('/products/{id}', 'get', await manager.request(`/products/${product.id}`), 200);
record('/products/{id}', 'get', await manager.request('/products/999999'), 404);
record('/products/{id}', 'put', await manager.request(`/products/${product.id}`, 'PUT', { sku: `ct-${stamp}`, name: 'Produto do contrato', sellable: true }), 200);
record('/products/{id}/price', 'patch', await manager.request(`/products/${product.id}/price`, 'PATCH', { newPrice: brl(55), reason: 'Reajuste do contrato' }), 200);
record('/products/{id}/status', 'patch', await manager.request(`/products/${product.id}/status`, 'PATCH', { active: true }), 200);
record('/products/{id}/price-history', 'get', await manager.request(`/products/${product.id}/price-history`), 200);
record('/products/{id}', 'delete', await manager.request(`/products/${digital.id}`, 'DELETE'), 204);
record('/stock/products/{id}/entries', 'post', await manager.request(`/stock/products/${product.id}/entries`, 'POST', { quantity: 20, unitCost: brl(30) }), 201);
record('/stock/products/{id}/adjustments', 'post', await manager.request(`/stock/products/${product.id}/adjustments`, 'POST', { direction: 'OUT', quantity: 999, reason: 'PERDA' }), 409);
record('/stock/products/{id}/adjustments', 'post', await manager.request(`/stock/products/${product.id}/adjustments`, 'POST', { direction: 'IN', quantity: 1, reason: 'INVENTARIO' }), 201);
record('/stock/products/{id}/min-level', 'patch', await manager.request(`/stock/products/${product.id}/min-level`, 'PATCH', { minLevel: 2 }), 200);
record('/stock/products', 'get', await manager.request('/stock/products'), 200);
record('/stock/movements', 'get', await manager.request(`/stock/movements?productId=${product.id}`), 200);
record('/products', 'get', await seller.request('/products'), 403);

// Promocoes
const now = Date.now();
const promotion = record('/promotions', 'post', await manager.request('/promotions', 'POST', { name: 'Promo do contrato', kind: 'PERCENT',
  value: 10, productId: product.id, startsAt: new Date(now - 86_400_000).toISOString(), endsAt: new Date(now + 86_400_000).toISOString() }), 201).body;
record('/promotions', 'get', await manager.request('/promotions'), 200);
record('/promotions/active', 'get', await seller.request(`/promotions/active?productId=${product.id}`), 200);

// Clientes (VENDEDOR)
const customer = record('/customers', 'post', await seller.request('/customers', 'POST', { type: 'INDIVIDUAL', name: 'Cliente do contrato',
  cpf: '529.982.247-25', email: 'cliente@contrato.test', phone: '85999990000' }), 201).body;
record('/customers', 'post', await seller.request('/customers', 'POST', { type: 'INDIVIDUAL', name: 'Duplicado',
  cpf: '52998224725', email: 'dup@contrato.test', phone: '85999990000' }), 409);
record('/customers', 'get', await seller.request('/customers?q=cliente'), 200);
record('/customers/{id}', 'get', await seller.request(`/customers/${customer.id}`), 200);
record('/customers/{id}', 'put', await seller.request(`/customers/${customer.id}`, 'PUT', { name: 'Cliente do contrato', email: 'cliente@contrato.test',
  phone: '85999990001', version: customer.version }), 200);
const company = record('/customers', 'post', await seller.request('/customers', 'POST', { type: 'CORPORATE', name: 'Empresa do contrato',
  companyName: 'Empresa LTDA', cnpj: '11.222.333/0001-81', email: 'empresa@contrato.test', phone: '8533334444' }), 201).body;
record('/customers/{id}', 'delete', await seller.request(`/customers/${company.id}`, 'DELETE'), 403);
record('/customers/{id}', 'delete', await manager.request(`/customers/${company.id}`, 'DELETE'), 204);

// PDV (VENDEDOR)
record('/sales/catalog', 'get', await seller.request('/sales/catalog?q=produto'), 200);
const items = [{ productId: product.id, quantity: 2, promotionId: promotion.id }];
record('/sales/quote', 'post', await seller.request('/sales/quote', 'POST', { items }), 200);
record('/sales/quote', 'post', await seller.request('/sales/quote', 'POST', { items: [] }), 400);
const key = randomUUID();
const sale = { customerId: customer.id, items, payment: { type: 'CARD', cardLast4: '4242', installments: 2 }, fulfillment: 'PICKUP' };
const order = record('/sales/orders', 'post', await seller.request('/sales/orders', 'POST', sale, { headers: { 'Idempotency-Key': key } }), 201).body;
record('/sales/orders', 'post', await seller.request('/sales/orders', 'POST', sale, { headers: { 'Idempotency-Key': key } }), 200);
record('/sales/orders', 'post', await seller.request('/sales/orders', 'POST', { ...sale, items: [{ productId: product.id, quantity: 999 }] },
  { headers: { 'Idempotency-Key': randomUUID() } }), 409);
record('/sales/orders', 'get', await seller.request('/sales/orders'), 200);
record('/sales/orders/{id}', 'get', await seller.request(`/sales/orders/${order.id}`), 200);
record('/sales/orders/{id}', 'get', await seller.request('/sales/orders/999999'), 404);
record('/sales/orders/{id}/payment', 'post', await seller.request(`/sales/orders/${order.id}/payment`, 'POST', { authorizationCode: 'NSU1' }), 200);
const second = record('/sales/orders', 'post', await seller.request('/sales/orders', 'POST', { ...sale, items: [{ productId: product.id, quantity: 1 }] },
  { headers: { 'Idempotency-Key': randomUUID() } }), 201).body;
record('/orders/{id}/cancel', 'post', await seller.request(`/orders/${second.id}/cancel`, 'POST'), 200);
record('/orders/{id}/cancel', 'post', await seller.request(`/orders/${order.id}/cancel`, 'POST'), 409);
record('/customers/{id}/orders', 'get', await seller.request(`/customers/${customer.id}/orders`), 200);

// Desconto manual com alcada (contrato v5, G-04): VENDEDOR 5% e GERENTE 20% (valores iniciais da V11)
const manualItems = [{ productId: product.id, quantity: 1 }];
record('/sales/quote', 'post', await seller.request('/sales/quote', 'POST', { items: manualItems, manualDiscount: { percent: 5, reason: 'Cliente fiel do contrato' } }), 200);
record('/sales/quote', 'post', await seller.request('/sales/quote', 'POST', { items: manualItems, manualDiscount: { percent: 9, reason: 'Acima da alcada' } }), 403);
const discounted = record('/sales/orders', 'post', await seller.request('/sales/orders', 'POST',
  { ...sale, items: manualItems, manualDiscount: { percent: 5, reason: 'Cliente fiel do contrato' } }, { headers: { 'Idempotency-Key': randomUUID() } }), 201).body;
record('/sales/orders/{id}/discount-approval', 'post', await manager.request(`/sales/orders/${discounted.id}/discount-approval`, 'POST',
  { percent: 12, reason: 'Aprovado no contrato' }), 200);
record('/sales/orders/{id}/discount-approval', 'post', await manager.request(`/sales/orders/${discounted.id}/discount-approval`, 'POST',
  { percent: 30, reason: 'Acima da gerencia' }), 403);
record('/sales/orders/{id}/discount-approval', 'post', await manager.request(`/sales/orders/${order.id}/discount-approval`, 'POST',
  { percent: 10, reason: 'Pedido ja pago' }), 409);
record('/orders/{id}/cancel', 'post', await seller.request(`/orders/${discounted.id}/cancel`, 'POST'), 200);
record('/discount-policy', 'get', await manager.request('/discount-policy'), 200);
record('/discount-policy', 'get', await seller.request('/discount-policy'), 403);
record('/discount-policy/{role}', 'put', await manager.request('/discount-policy/VENDEDOR', 'PUT', { maxPercent: 5, allowBelowCost: false, reason: 'Conferencia do contrato' }), 200);
record('/discount-policy/{role}', 'put', await manager.request('/discount-policy/VENDEDOR', 'PUT', { maxPercent: 101, allowBelowCost: false, reason: 'Invalido' }), 400);
record('/customers/{id}/summary', 'get', await seller.request(`/customers/${customer.id}/summary`), 200);
record('/customers/{id}/summary', 'get', await manager.request(`/customers/${customer.id}/summary`), 200);
record('/customers/{id}/summary', 'get', await seller.request('/customers/999999/summary'), 404);

// Gestao (GERENTE) e Meu desempenho
const month = new Date().toISOString().slice(0, 7);
record('/payment/{id}/refund', 'post', await manager.request(`/payment/${order.payment.id}/refund`, 'POST', { amount: brl(5), reason: 'Ajuste do contrato' }), 201);
record('/payment/{id}/refund', 'post', await manager.request(`/payment/${order.payment.id}/refund`, 'POST', { amount: brl(999999), reason: 'Ajuste do contrato' }), 400);
record('/goals/monthly', 'put', await manager.request('/goals/monthly', 'PUT', { period: month, storeGoal: brl(1000),
  sellers: [{ sellerId, goal: brl(1000) }] }), 200);
record('/goals/monthly', 'get', await manager.request(`/goals/monthly?period=${month}`), 200);
record('/goals/monthly', 'get', await manager.request('/goals/monthly?period=2000-01'), 404);
record('/performance/me', 'get', await seller.request(`/performance/me?period=${month}`), 200);
record('/management/summary', 'get', await manager.request(`/management/summary?period=${month}`), 200);
record('/management/revenue', 'get', await manager.request('/management/revenue?period=D30'), 200);
record('/analytics/sales', 'get', await manager.request(`/analytics/sales?month=${month}`), 200);
record('/analytics/stock', 'get', await manager.request('/analytics/stock'), 200);
record('/promotions/{id}/close', 'post', await manager.request(`/promotions/${promotion.id}/close`, 'POST'), 200);

// Usuarios (GERENTE) e troca da propria senha (contrato v5, G-01/G-03)
const managerId = (await manager.request('/auth/me')).body.id;
const email = `usuario${stamp}@contrato.test`;
const user = { name: 'Usuario do contrato', email, role: 'VENDEDOR', temporaryPassword: `Provisoria-${stamp}` };
const created = record('/users', 'post', await manager.request('/users', 'POST', user), 201).body;
record('/users', 'post', await manager.request('/users', 'POST', { ...user, email: email.toUpperCase() }), 409);
record('/users', 'post', await manager.request('/users', 'POST', { ...user, email: `fraca${stamp}@contrato.test`, temporaryPassword: 'curta-123' }), 400);
record('/users', 'get', await manager.request('/users?role=VENDEDOR&active=true'), 200);
record('/users', 'get', await seller.request('/users'), 403);
record('/users/{id}', 'get', await manager.request(`/users/${created.id}`), 200);
record('/users/{id}', 'get', await manager.request('/users/999999'), 404);
const edit = { name: 'Usuario do contrato B', email, version: created.version };
record('/users/{id}', 'put', await manager.request(`/users/${created.id}`, 'PUT', edit), 200);
record('/users/{id}', 'put', await manager.request(`/users/${created.id}`, 'PUT', edit), 409);
record('/users/{id}/role', 'patch', await manager.request(`/users/${created.id}/role`, 'PATCH', { role: 'GERENTE', reason: 'Promocao no contrato' }), 200);
record('/users/{id}/role', 'patch', await manager.request(`/users/${managerId}/role`, 'PATCH', { role: 'VENDEDOR', reason: 'Proprio papel' }), 409);
record('/users/{id}/status', 'patch', await manager.request(`/users/${created.id}/status`, 'PATCH', { active: false, reason: 'Pausa no contrato' }), 200);
record('/users/{id}/status', 'patch', await manager.request(`/users/${created.id}/status`, 'PATCH', { active: true, reason: 'Retorno no contrato' }), 200);
record('/users/{id}/unlock', 'post', await manager.request(`/users/${created.id}/unlock`, 'POST'), 204);
record('/users/{id}/unlock', 'post', await manager.request('/users/999999/unlock', 'POST'), 404);
record('/users/{id}/password-reset', 'post', await manager.request(`/users/${created.id}/password-reset`, 'POST', { temporaryPassword: 'curta-123' }), 400);
record('/users/{id}/password-reset', 'post', await manager.request(`/users/${created.id}/password-reset`, 'POST', { temporaryPassword: `Outra-${stamp}` }), 204);
const fresh = sessionClient(origin);
if ((await fresh.login(email, `Outra-${stamp}`)).status !== 200) findings.push('new user could not log in with the temporary password');
record('/auth/password', 'put', await fresh.request('/auth/password', 'PUT', { currentPassword: 'senha-errada', newPassword: `Definitiva-${stamp}` }), 401);
record('/auth/password', 'put', await fresh.request('/auth/password', 'PUT', { currentPassword: `Outra-${stamp}`, newPassword: 'curta-123' }), 400);
record('/auth/password', 'put', await fresh.request('/auth/password', 'PUT', { currentPassword: `Outra-${stamp}`, newPassword: `Definitiva-${stamp}` }), 204);
record('/auth/logout', 'post', await seller.request('/auth/logout', 'POST'), 204);

for (const { path, method, operation } of operations(v4)) {
  const name = `${method.toUpperCase()} ${path}`;
  if (operation['x-status'] === 'implemented' && !exercised.has(name)) findings.push(`${name}: implemented but not exercised`);
}

const directory = new URL('../reports/contrato/live-v5/', import.meta.url);
mkdirSync(directory, { recursive: true });
const report = { scope: 'live OpenAPI and responses versus target v5', timestamp: new Date().toISOString(),
  contract: 'target-loja-gestao-openapi-2026-10-14-v5.json', operations: operations(v4).length,
  exercised: Object.fromEntries([...exercised].sort()), divergences: findings,
  limits: ['Structural check covers path, method, parameters, request body presence and success status codes; schema bodies are checked on real responses.'] };
writeFileSync(new URL('live-contract.json', directory), JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify({ operations: report.operations, exercised: exercised.size, divergences: findings }, null, 2));
process.exitCode = findings.length ? 1 : 0;
