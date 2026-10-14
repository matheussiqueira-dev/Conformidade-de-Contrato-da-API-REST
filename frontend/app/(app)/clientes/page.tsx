import type { Metadata } from 'next';
import { sessionFor } from '../../../lib/server/session.ts';
import { NoAccess } from '../../../components/feedback.tsx';
import CustomerList from './customer-list.tsx';

export const metadata: Metadata = { title: 'Clientes' };

export default async function ClientesPage() {
  const { allowed } = await sessionFor('CLIENTES', '/clientes');
  return allowed ? <CustomerList /> : <NoAccess />;
}
