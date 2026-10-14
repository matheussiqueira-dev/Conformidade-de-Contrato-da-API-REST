import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { checkResponse } from './response-contract.mjs';
import { createAjv, compileProjection, targetPath, validateDocument, officialSchemaPath, adaptOfficialSchema } from './validate-contract.mjs';

const document = JSON.parse(readFileSync(targetPath, 'utf8'));
const officialSchema = JSON.parse(readFileSync(officialSchemaPath, 'utf8'));
const revisedBaseline = JSON.parse(readFileSync(new URL('../config/openapi/baseline-errors-openapi-2026-10-04.json', import.meta.url), 'utf8'));
const money = amount => ({ amount, currency: 'BRL' });
const performance = () => ({ period: '2026-10', seller: { id: 1, name: 'Allan', email: 'allan@example.test', role: 'VENDEDOR' }, goal: money(80000), realized: money(68400), achievementPercent: 85.5 });
test('2020-12 tuple keywords are executed', () => {
  const validate = createAjv().compile({ $schema: 'https://json-schema.org/draft/2020-12/schema', type: 'array', prefixItems: [{ type: 'string' }], items: false });
  assert.equal(validate(['ok']), true);
  assert.equal(validate([42]), false);
  assert.equal(validate(['ok', 'extra']), false);
});
test('all target schema locations compile and validate against 2020-12', () => {
  assert.deepEqual(validateDocument(document).findings, []);
});

test('session login response rejects token fields and CSRF response requires the exact header', () => {
  const login = compileProjection(document, 'AuthResponse');
  const user = performance().seller;
  assert.equal(login({ user }), true);
  assert.equal(login({ user, accessToken: 'forbidden' }), false);
  const csrf = compileProjection(document, 'CsrfResponse');
  assert.equal(csrf({ headerName: 'X-CSRF-TOKEN', token: 'masked' }), true);
  assert.equal(csrf({ headerName: 'Authorization', token: 'masked' }), false);
  assert.equal(csrf({ headerName: 'X-CSRF-TOKEN', token: '' }), false);
});
test('seller projection accepts valid sample and rejects sensitive fields at both levels', () => {
  const validate = compileProjection(document, 'SellerPerformance');
  assert.equal(validate(performance()), true);
  const extra = performance(); extra.cost = money(1);
  assert.equal(validate(extra), false);
  const nested = performance(); nested.seller.margin = 10;
  assert.equal(validate(nested), false);
});
test('formats reject invalid email, wrong currency and non numeric money', () => {
  const validate = compileProjection(document, 'SellerPerformance');
  const email = performance(); email.seller.email = 'invalid';
  assert.equal(validate(email), false);
  const currency = performance(); currency.goal.currency = 'USD';
  assert.equal(validate(currency), false);
  const amount = performance(); amount.goal.amount = '80000';
  assert.equal(validate(amount), false);
});
test('sales order rejects zero quantity and empty items', () => {
  const validate = compileProjection(document, 'SalesOrderRequest');
  const order = { items: [{ productId: 1, quantity: 1 }], payment: {}, shippingAddress: {} };
  assert.equal(validate(order), true);
  assert.equal(validate({ ...order, items: [{ productId: 1, quantity: 0 }] }), false);
  assert.equal(validate({ ...order, items: [] }), false);
});
test('invalid JSON Schema and broken references fail', () => {
  const badType = structuredClone(document); badType.components.schemas.Money.type = 'money';
  assert.ok(validateDocument(badType).findings.length);
  const badRef = structuredClone(document); badRef.components.schemas.Money.$ref = '#/components/schemas/Missing';
  assert.ok(validateDocument(badRef).findings.length);
});
test('official OpenAPI document validates refs and schemas without mutating downloaded schema', () => {
  const before = JSON.stringify(officialSchema);
  assert.deepEqual(validateDocument(document, officialSchema).findings, []);
  assert.equal(JSON.stringify(officialSchema), before);
  const changed = structuredClone(officialSchema); changed.$defs.schema.type = 'object';
  assert.throws(() => adaptOfficialSchema(changed), /requires review/);
});
test('official document checks reject missing info, malformed parameter and extra operation field', () => {
  const missingInfo = structuredClone(document); delete missingInfo.info;
  const parameter = structuredClone(document); delete parameter.components.parameters.Id.name;
  const extra = structuredClone(document); extra.paths['/auth/login'].post.invalidField = true;
  for (const invalid of [missingInfo, parameter, extra]) {
    assert.ok(validateDocument(invalid, officialSchema).findings.some(f => f.scope === 'openapi-document'));
  }
});
test('media-range format accepts JSON and wildcards, rejects malformed content keys', () => {
  const validate = createAjv().compile({ type: 'string', format: 'media-range' });
  for (const valid of ['application/json', 'application/*', '*/*', '*/*; q=0.5', 'text/plain; charset=utf-8']) assert.equal(validate(valid), true, valid);
  for (const invalid of ['invalid', '*/json', 'text /plain', 'text/plain; charset=']) assert.equal(validate(invalid), false, invalid);
  const changed = structuredClone(document);
  changed.paths['/auth/login'].post.requestBody.content.invalid = { schema: { type: 'string' } };
  assert.ok(validateDocument(changed, officialSchema).findings.some(f => f.scope === 'openapi-document'));
});
test('error revision validates and preserves original baseline and successful responses', () => {
  const originalBytes = readFileSync(new URL('../config/openapi/baseline-openapi-2026-10-03.json', import.meta.url));
  assert.equal(createHash('sha256').update(originalBytes).digest('hex'), '4cf78ea43c7a24490f5b53f4e4bb11fcbbc4689efdc1db57bb9fa5eb9fe8f180');
  const original = JSON.parse(originalBytes);
  assert.deepEqual(validateDocument(revisedBaseline, officialSchema).findings, []);
  for (const [path, item] of Object.entries(original.paths)) {
    for (const [method, operation] of Object.entries(item)) {
      for (const [status, response] of Object.entries(operation.responses ?? {})) {
        assert.deepEqual(revisedBaseline.paths[path][method].responses[status], response);
      }
    }
  }
});
test('400 and 404 error payloads validate with nullable details and local timestamp', () => {
  const sample = { timestamp: '2026-10-04T00:19:48.123456', status: 400, error: 'Validation Error', message: 'Invalid product', details: ['Name required.'] };
  assert.equal(checkResponse(revisedBaseline, '/products', 'post', { status: 400, body: sample }).valid, true);
  assert.equal(checkResponse(revisedBaseline, '/products/{id}', 'get', { status: 404, body: { ...sample, status: 404, details: null } }).valid, true);
  for (const body of [{ ...sample, status: 404 }, { ...sample, details: [123] }, { ...sample, timestamp: '2026-02-30T00:00:00' }, { ...sample, timestamp: '2026-10-04T00:00:00Z' }]) {
    assert.equal(checkResponse(revisedBaseline, '/products', 'post', { status: 400, body }).valid, false);
  }
  const missingMessage = { ...sample }; delete missingMessage.message;
  assert.equal(checkResponse(revisedBaseline, '/products', 'post', { status: 400, body: missingMessage }).valid, false);
});

const v4 = JSON.parse(readFileSync(new URL('../config/openapi/target-loja-gestao-openapi-2026-10-14-v4.json', import.meta.url), 'utf8'));
const brl = amount => ({ amount, currency: 'BRL' });
const sale = () => ({ customerId: 1, items: [{ productId: 7, quantity: 2 }], payment: { type: 'PIX' }, fulfillment: 'PICKUP' });
test('SignedMoney accepts negative results while Money stays non-negative', () => {
  const day = { date: '2026-10-14', netRevenue: brl(-50), grossProfit: brl(-50) };
  const report = { period: { from: '2026-10-14', to: '2026-10-14' }, grossRevenue: brl(0), refunds: brl(50), netRevenue: brl(-50),
    ordersPaid: 0, unitsSold: 0, averageTicket: null, grossProfit: brl(-50), grossMarginPercent: null, costCoveragePercent: 0,
    daily: [day], byPaymentMethod: [{ method: 'PIX', ordersPaid: 0, netRevenue: brl(-50) }],
    bySeller: [{ seller: { id: 2, name: 'Allan' }, ordersPaid: 0, netRevenue: brl(-50) }], topProducts: [], lowStockCount: 0,
    pendingPaymentCount: 0, pendingPaymentAmount: brl(0), generatedAt: '2026-10-14T10:00:00-03:00' };
  assert.equal(checkResponse(v4, '/management/revenue', 'get', { status: 200, body: report }).valid, true);
  assert.equal(checkResponse(v4, '/management/revenue', 'get', { status: 200, body: { ...report, refunds: brl(-1) } }).valid, false);
});
test('checkResponse resolves v4 shared responses such as NotFound', () => {
  const error = { timestamp: '2026-10-14T05:00:00-03:00', status: 404, error: 'Not Found', code: 'NOT_FOUND', message: 'Product not found', details: null };
  assert.equal(checkResponse(v4, '/products/{id}', 'get', { status: 404, body: error }).valid, true);
  assert.equal(checkResponse(v4, '/products/{id}', 'get', { status: 404, body: { ...error, code: undefined } }).valid, false);
});
test('v4 document validates against the official OpenAPI 3.1 schema', () => {
  assert.deepEqual(validateDocument(v4, officialSchema).findings, []);
});
test('v4 sale request rejects seller, price and total fields', () => {
  const validate = compileProjection(v4, 'SalesOrderRequest');
  assert.equal(validate(sale()), true);
  for (const forged of [{ sellerId: 2 }, { total: brl(1) }, { discount: brl(1) }]) assert.equal(validate({ ...sale(), ...forged }), false);
  const priced = sale(); priced.items[0].unitPrice = brl(1);
  assert.equal(validate(priced), false);
});
test('v4 sale request enforces quantity bounds and payment variants', () => {
  const validate = compileProjection(v4, 'SalesOrderRequest');
  for (const quantity of [0, -1, 1.5, 1000]) {
    const order = sale(); order.items[0].quantity = quantity;
    assert.equal(validate(order), false, String(quantity));
  }
  assert.equal(validate({ ...sale(), payment: { type: 'CARD', installments: 1 } }), false);
  assert.equal(validate({ ...sale(), payment: { type: 'CARD', cardLast4: '1234', installments: 3 } }), true);
  assert.equal(validate({ ...sale(), payment: { type: 'CARD', cardLast4: '4111111111111111', installments: 1 } }), false);
  assert.equal(validate({ ...sale(), payment: { type: 'BOLETO' } }), false);
  assert.equal(validate({ ...sale(), items: Array.from({ length: 51 }, (_, i) => ({ productId: i + 1, quantity: 1 })) }), false);
});
test('v4 error response requires code and keeps details as strings or null', () => {
  const validate = compileProjection(v4, 'ErrorResponse');
  const error = { timestamp: '2026-10-14T10:00:00-03:00', status: 409, error: 'Conflict', code: 'STOCK_INSUFFICIENT', message: 'Insufficient stock', details: null };
  assert.equal(validate(error), true);
  assert.equal(validate({ ...error, details: ['quantity: must be at most 999'] }), true);
  assert.equal(validate({ ...error, code: 'OUT_OF_STOCK' }), false);
  const noCode = { ...error }; delete noCode.code;
  assert.equal(validate(noCode), false);
  assert.equal(validate({ ...error, details: { field: 'quantity' } }), false);
});
test('v4 customer projections mask documents and keep the document immutable', () => {
  const summary = compileProjection(v4, 'CustomerSummary');
  const customer = { id: 1, type: 'INDIVIDUAL', name: 'Marina Costa', document: '***.456.789-**', email: null, phone: null, ordersCount: 0, lastOrderAt: null };
  assert.equal(summary(customer), true);
  assert.equal(summary({ ...customer, document: '**.345.678/0001-**', type: 'CORPORATE' }), true);
  assert.equal(summary({ ...customer, document: '12345678909' }), false);
  const update = compileProjection(v4, 'CustomerUpdateRequest');
  const body = { name: 'Marina Costa', email: 'marina@cliente.test', phone: '81999990000', version: 0 };
  assert.equal(update(body), true);
  assert.equal(update({ ...body, cpf: '12345678909' }), false);
  const history = compileProjection(v4, 'CustomerOrderSummary');
  const order = { id: 9, moment: '2026-10-01T09:40:00-03:00', status: 'PAID', fulfillment: 'PICKUP', items: [{ name: 'AirPods Branco', quantity: 1 }], own: false };
  assert.equal(history(order), true);
  assert.equal(history({ ...order, unitCost: brl(10) }), false);
});
const v5 = JSON.parse(readFileSync(new URL('../config/openapi/target-loja-gestao-openapi-2026-10-14-v5.json', import.meta.url), 'utf8'));
test('v5 accepts numeric and alphanumeric CNPJ, masked the same way, and keeps letters uppercase', () => {
  const create = compileProjection(v5, 'CorporateCustomerCreate');
  const company = { type: 'CORPORATE', name: 'Empresa Nova', companyName: 'Empresa Nova LTDA', email: 'empresa@cliente.test', phone: '8533334444' };
  for (const cnpj of ['12.ABC.345/01DE-35', '12ABC34501DE35', '11.222.333/0001-81']) assert.equal(create({ ...company, cnpj }), true, cnpj);
  for (const cnpj of ['12.abc.345/01de-35', '12ABC34501DE3X', '12.ABC.345/01DE']) assert.equal(create({ ...company, cnpj }), false, cnpj);
  const summary = compileProjection(v5, 'CustomerSummary');
  const customer = { id: 1, type: 'CORPORATE', name: 'Empresa Nova', document: '**.ABC.345/01DE-**', email: null, phone: null, ordersCount: 0, lastOrderAt: null };
  assert.equal(summary(customer), true);
  assert.equal(summary({ ...customer, document: '12.ABC.345/01DE-35' }), true);
  assert.equal(summary({ ...customer, document: '12ABC34501DE35' }), false);
});
