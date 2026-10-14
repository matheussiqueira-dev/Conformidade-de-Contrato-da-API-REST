import type { Metadata } from 'next';
import { sessionFor } from '../../../lib/server/session.ts';
import { NoAccess } from '../../../components/feedback.tsx';
import MyPerformance from './my-performance.tsx';

export const metadata: Metadata = { title: 'Meu desempenho' };

export default async function MeuDesempenhoPage() {
  const { allowed } = await sessionFor('MEU_DESEMPENHO', '/meu-desempenho');
  return allowed ? <MyPerformance /> : <NoAccess />;
}
