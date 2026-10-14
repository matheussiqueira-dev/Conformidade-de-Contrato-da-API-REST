import type { ReactNode } from 'react';
import { ApiError, ContractError } from '../lib/api/http.ts';
import { NETWORK_MESSAGE } from '../lib/api/messages.ts';

/** Mensagem de erro em pt-BR, sem codigo HTTP; erro de contrato aparece igual ao usuario e vai para o console. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof ContractError) {
    console.error(error);
    return 'Recebemos dados inesperados do sistema. Atualize a página.';
  }
  return NETWORK_MESSAGE;
}

export function ErrorNotice({ error, children }: { error: unknown; children?: ReactNode }) {
  if (!error) return null;
  return <div className="notice notice-error" role="alert"><p>{errorMessage(error)}</p>{children}</div>;
}

export function Loading({ label = 'Carregando…' }: { label?: string }) {
  return <p role="status" className="muted">{label}</p>;
}

export function NoAccess() {
  return (
    <section className="card stack-sm" aria-labelledby="no-access">
      <h1 id="no-access">Sem acesso a esta área</h1>
      <p className="muted">Seu perfil não tem permissão para esta tela. Use o menu para ir a uma área liberada.</p>
    </section>
  );
}
