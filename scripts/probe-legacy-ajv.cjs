// Diagnostic only: Ajv 6 cannot certify OpenAPI 3.1 / JSON Schema 2020-12.
const fs = require('node:fs');
const path = require('node:path');
const modulePath = process.argv[2];
if (!modulePath) throw new Error('Pass the module directory of an already installed Ajv 6 (diagnostic only).');
const Ajv = require(path.resolve(modulePath));
const version = require(path.join(path.resolve(modulePath), 'package.json')).version;
if (!version.startsWith('6.')) throw new Error('This probe specifically evaluates Ajv 6; use Ajv2020 for adoption.');
const fixture = {
  period: '2026-09',
  seller: { id: 1, name: 'Ana Fixture', email: 'ana@loja.test', role: 'VENDEDOR' },
  goal: { amount: 80000, currency: 'BRL' },
  realized: { amount: 68400, currency: 'BRL' },
  achievementPercent: 85.5
};
const result = { tool: `Ajv ${version}`, scope: 'draft-07-compatible-subset-diagnostic-no-http', dialect202012: false, contracts: [] };
try {
  new Ajv().compile({ $schema: 'https://json-schema.org/draft/2020-12/schema', type: 'object' });
  throw new Error('Unexpected 2020-12 support; investigate probe.');
} catch (error) {
  if (!error.message.includes('no schema with key or ref')) throw error;
  result.dialectError = error.message;
}
for (const name of ['target-loja-gestao-openapi-2026-10-03.json', 'target-loja-gestao-openapi-2026-10-03-v2.json']) {
  const document = JSON.parse(fs.readFileSync(path.join(__dirname, '../config/openapi', name), 'utf8'));
  const ajv = new Ajv({ allErrors: true, unknownFormats: 'ignore', logger: false });
  // Register the original document only as a reference container, then validate
  // this compatible subset. This is NOT OpenAPI document validation.
  ajv.addSchema(document, 'target');
  const validate = ajv.compile({ $ref: 'target#/components/schemas/SellerPerformance' });
  const validFixture = validate(fixture);
  const invalid = { ...fixture, cost: 100, team: [] };
  const leakAccepted = validate(invalid);
  const errors = structuredClone(validate.errors);
  const nestedLeak = structuredClone(fixture);
  nestedLeak.seller.margin = 100;
  const nestedLeakAccepted = validate(nestedLeak);
  result.contracts.push({ file: name, validFixture, leakAccepted, nestedLeakAccepted, negativeErrors: errors });
  if (!validFixture) throw new Error(`Positive diagnostic failed: ${name}`);
  if (name.includes('-v2') && (leakAccepted || nestedLeakAccepted)) throw new Error('Closed v2 projections failed the diagnostic.');
}
console.log(JSON.stringify(result, null, 2));
