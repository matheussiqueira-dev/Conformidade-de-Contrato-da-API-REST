import type { Metadata } from 'next';
import { sessionFor } from '../../../lib/server/session.ts';
import { NoAccess } from '../../../components/feedback.tsx';
import SalePoint from './sale-point.tsx';

export const metadata: Metadata = { title: 'Vender' };

export default async function VenderPage() {
  const { user, allowed } = await sessionFor('VENDER', '/vender');
  return allowed ? <SalePoint userId={user.id} /> : <NoAccess />;
}
