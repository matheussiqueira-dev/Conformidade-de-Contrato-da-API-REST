import Ajv2020 from 'ajv/dist/2020.js';
import addFormats from 'ajv-formats';
import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';

export const schemaUrl = 'https://spec.openapis.org/oas/3.1/schema/2025-09-15';
export const officialSchemaPath = new URL('../config/openapi/validation/openapi-3.1-schema.json', import.meta.url);
export const targetPath = new URL('../config/openapi/target-loja-gestao-openapi-2026-10-03-v2.json', import.meta.url);
export function createAjv() {
  // OpenAPI annotations/extensions are not JSON Schema validation keywords.
  const ajv = new Ajv2020({ strict: false, allErrors: true, validateFormats: true });
  addFormats(ajv);
  const dateTime = ajv.compile({ type: 'string', format: 'date-time' });
  ajv.addFormat('local-date-time', value => /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?$/.test(value) && dateTime(value + 'Z'));
  const token = "[!#$%&'*+.^_`|~0-9A-Za-z-]+";
  const quoted = String.raw`"(?:[\t\x20\x21\x23-\x5b\x5d-\x7e\x80-\xff]|\\[\t\x20-\x7e\x80-\xff])*"`;
  const mediaRange = new RegExp(`^(?:\\*/\\*|${token}/${token})(?:[ \\t]*;[ \\t]*${token}=(?:${token}|${quoted}))*$`);
  ajv.addFormat('media-range', value => mediaRange.test(value) && (!value.startsWith('*/') || value.startsWith('*/*')));
  ajv.addFormat('decimal', { type: 'number', validate: Number.isFinite });
  ajv.addFormat('double', { type: 'number', validate: Number.isFinite });
  ajv.addFormat('float', { type: 'number', validate: Number.isFinite });
  ajv.addFormat('int32', { type: 'number', validate: n => Number.isInteger(n) && n >= -2147483648 && n <= 2147483647 });
  ajv.addFormat('int64', { type: 'number', validate: Number.isSafeInteger });
  return ajv;
}
export function compileProjection(document, name) {
  return createAjv().compile({
    $schema: 'https://json-schema.org/draft/2020-12/schema',
    components: document.components,
    $ref: `#/components/schemas/${name}`,
  });
}
export function validateDocument(document, officialSchema) {
  const ajv = createAjv();
  const findings = [];
  if (officialSchema) {
    const validate = ajv.compile(adaptOfficialSchema(officialSchema));
    if (!validate(document)) findings.push({ scope: 'openapi-document', errors: validate.errors });
  }
  let count = 0;
  function check(schema, location) {
    count++;
    if (!ajv.validateSchema(schema)) findings.push({ scope: location, errors: structuredClone(ajv.errors) });
    try { ajv.compile({ components: document.components, ...schema }); }
    catch (error) { findings.push({ scope: location, error: error.message }); }
  }
  for (const [name, schema] of Object.entries(document.components?.schemas ?? {})) check(schema, `components/schemas/${name}`);
  function walk(value, location) {
    if (!value || typeof value !== 'object') return;
    for (const [key, child] of Object.entries(value)) {
      if (key === 'schema') check(child, `${location}/schema`);
      else if (key !== 'schemas') walk(child, `${location}/${key}`);
    }
  }
  walk(document, '#');
  return { dialect: '2020-12', officialDocumentSchema: Boolean(officialSchema),
    ...(officialSchema ? { officialSchemaCompatibility: 'Ajv #1745: four #meta dynamic refs resolved statically; data schemas validated separately' } : {}),
    schemasChecked: count, findings };
}
export function adaptOfficialSchema(officialSchema) {
  // Ajv issue #1745: nested dynamic anchors can resolve to the wrong scope.
  // This pinned, "without Schema Object validation" schema uses #meta only
  // as an object/boolean placeholder. Preserve every document constraint,
  // resolve that placeholder statically, and validate data schemas separately.
  if (officialSchema.$id !== schemaUrl ||
      JSON.stringify(officialSchema.$defs?.schema?.type) !== JSON.stringify(['object', 'boolean']) ||
      officialSchema.$defs?.schema?.$dynamicAnchor !== 'meta') {
    throw new Error('Unexpected official schema: compatibility adapter requires review.');
  }
  const adapted = structuredClone(officialSchema);
  let replacements = 0;
  function walk(value) {
    if (!value || typeof value !== 'object') return;
    if (Object.hasOwn(value, '$dynamicRef')) {
      if (value.$dynamicRef !== '#meta') throw new Error('Unexpected dynamic reference in official schema.');
      value.$ref = '#/$defs/schema';
      delete value.$dynamicRef;
      replacements++;
    }
    for (const child of Object.values(value)) walk(child);
  }
  walk(adapted);
  if (replacements !== 4) throw new Error('Unexpected official schema reference count.');
  delete adapted.$defs.schema.$dynamicAnchor;
  return adapted;
}
export async function loadOfficialSchema() {
  if (!existsSync(officialSchemaPath)) {
    const response = await fetch(schemaUrl, { signal: AbortSignal.timeout(15000) });
    if (!response.ok) throw new Error(`OpenAPI schema download: HTTP ${response.status}`);
    const schema = await response.json();
    mkdirSync(new URL('.', officialSchemaPath), { recursive: true });
    writeFileSync(officialSchemaPath, JSON.stringify(schema, null, 2) + '\n');
  }
  return JSON.parse(readFileSync(officialSchemaPath, 'utf8'));
}
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const document = JSON.parse(readFileSync(targetPath, 'utf8'));
  let officialSchema, blocked;
  try { officialSchema = await loadOfficialSchema(); }
  catch (error) { blocked = `Official OpenAPI document validation blocked: ${error.message}`; }
  const revisedBaseline = JSON.parse(readFileSync(new URL('../config/openapi/baseline-errors-openapi-2026-10-04.json', import.meta.url), 'utf8'));
  const result = { ...validateDocument(document, officialSchema),
    revisedBaseline: validateDocument(revisedBaseline, officialSchema), ...(blocked ? { blocked } : {}) };
  const reportDirectory = new URL('../reports/contrato/spike-2026-10-04/', import.meta.url);
  mkdirSync(reportDirectory, { recursive: true });
  writeFileSync(new URL('validation.json', reportDirectory), JSON.stringify(result, null, 2) + '\n');
  console.log(JSON.stringify(result, null, 2));
  process.exitCode = result.findings.length || result.revisedBaseline.findings.length || blocked ? 1 : 0;
}
