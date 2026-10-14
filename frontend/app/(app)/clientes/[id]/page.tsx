import type { Metadata } from 'next';
import { sessionFor } from '../../../../lib/server/session.ts';
import { NoAccess } from '../../../../components/feedback.tsx';
import CustomerProfile from './customer-profile.tsx';

export const metadata: Metadata = { title: 'Cliente' };

export default async function ClientePage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  const { user, allowed } = await sessionFor('CLIENTES', `/clientes/${id}`);
  if (!allowed) return <NoAccess />;
  const customerId = /^\d{1,18}$/.test(id) ? Number(id) : null;
  return <CustomerProfile customerId={customerId} manager={user.role === 'GERENTE'} />;
}
