import type { Metadata } from 'next';
import { sessionFor } from '../../../lib/server/session.ts';
import { NoAccess } from '../../../components/feedback.tsx';
import SalePoint from './sale-point.tsx';

export const metadata: Metadata = { title: 'Nova venda' };

/** ?cliente=ID (vindo do perfil do cliente) deixa o cliente pre-selecionado na etapa de pagamento. */
export default async function VenderPage({ searchParams }: { searchParams: Promise<{ cliente?: string | string[] }> }) {
  const { user, allowed } = await sessionFor('VENDER', '/vender');
  const raw = (await searchParams).cliente;
  const customerId = typeof raw === 'string' && /^\d{1,18}$/.test(raw) ? Number(raw) : null;
  return allowed ? <SalePoint userId={user.id} userName={user.name} initialCustomerId={customerId} /> : <NoAccess />;
}
