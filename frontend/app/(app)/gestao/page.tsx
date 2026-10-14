import type { Metadata } from 'next';
import { sessionFor } from '../../../lib/server/session.ts';
import { NoAccess } from '../../../components/feedback.tsx';
import Dashboard from './dashboard.tsx';

export const metadata: Metadata = { title: 'Gestão' };

export default async function GestaoPage() {
  const { allowed } = await sessionFor('GESTAO', '/gestao');
  return allowed ? <Dashboard /> : <NoAccess />;
}
