import { createAjv } from './validate-contract.mjs';

export function checkResponse(document, path, method, response) {
  const content = document.paths[path]?.[method]?.responses[String(response.status)]?.content;
  const schema = (content?.['application/json'] ?? content?.['*/*'])?.schema;
  if (!schema) return { valid: false, errors: [`No response schema for ${method} ${path} status ${response.status}`] };
  const validate = createAjv().compile({ components: document.components, ...schema });
  const valid = validate(response.body);
  return { valid, errors: validate.errors ?? [] };
}
