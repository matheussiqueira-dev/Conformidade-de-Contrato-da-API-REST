import type { Metadata } from 'next';
import { sessionFor } from '../../../../lib/server/session.ts';
import { NoAccess } from '../../../../components/feedback.tsx';
import Analytics from './analytics.tsx';

export const metadata: Metadata = { title: 'Análises' };

export default async function AnalisesPage() {
  const { allowed } = await sessionFor('ANALISES', '/gestao/analises');
  return allowed ? <Analytics /> : <NoAccess />;
}
