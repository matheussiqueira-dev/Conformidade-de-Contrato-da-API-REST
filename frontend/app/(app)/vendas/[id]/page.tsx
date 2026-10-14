import type { Metadata } from 'next';
import { sessionFor } from '../../../../lib/server/session.ts';
import { NoAccess } from '../../../../components/feedback.tsx';
import SaleDetail from './sale-detail.tsx';

export const metadata: Metadata = { title: 'Venda' };

export default async function VendaPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const { allowed } = await sessionFor('VENDAS', `/vendas/${id}`);
  if (!allowed) return <NoAccess />;
  const orderId = /^\d{1,18}$/.test(id) ? Number(id) : null;
  return <SaleDetail orderId={orderId} />;
}
