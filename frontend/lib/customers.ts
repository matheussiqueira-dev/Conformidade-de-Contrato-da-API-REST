// Regras de tela do cadastro de cliente que dependem da resposta da API.
import { ApiError } from './api/http.ts';

/** 409 DUPLICATE_DOCUMENT traz o id do cliente ja cadastrado em details ("customerId=12", D-EVO-40). */
export function existingCustomerId(error: unknown): number | null {
  if (!(error instanceof ApiError) || error.code !== 'DUPLICATE_DOCUMENT') return null;
  const match = error.details.map(detail => /^customerId=(\d+)$/.exec(detail)).find(Boolean);
  return match ? Number(match[1]) : null;
}
