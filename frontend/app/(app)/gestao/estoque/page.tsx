import type { Metadata } from 'next';
import { sessionFor } from '../../../../lib/server/session.ts';
import { NoAccess } from '../../../../components/feedback.tsx';
import Stock from './stock.tsx';

export const metadata: Metadata = { title: 'Estoque' };

export default async function EstoquePage() {
  const { allowed } = await sessionFor('ESTOQUE', '/gestao/estoque');
  return allowed ? <Stock /> : <NoAccess />;
}
