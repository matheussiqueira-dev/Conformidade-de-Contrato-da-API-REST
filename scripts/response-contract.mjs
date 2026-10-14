import { createAjv } from './validate-contract.mjs';

export function checkResponse(document, path, method, response) {
  let entry = document.paths[path]?.[method]?.responses[String(response.status)];
  // v4 reuses responses through components.responses (BadRequest, NotFound, ...).
  if (entry?.$ref) entry = entry.$ref.slice(2).split('/').reduce((value, key) => value?.[key], document);
  const content = entry?.content;
  const schema = (content?.['application/json'] ?? content?.['*/*'])?.schema;
  if (!schema) return { valid: false, errors: [`No response schema for ${method} ${path} status ${response.status}`] };
  const validate = createAjv().compile({ components: document.components, ...schema });
  const valid = validate(response.body);
  return { valid, errors: validate.errors ?? [] };
}
