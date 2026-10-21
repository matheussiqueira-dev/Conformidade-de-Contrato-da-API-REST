import type { Metadata } from 'next';
import { sessionFor } from '../../../../lib/server/session.ts';
import { NoAccess } from '../../../../components/feedback.tsx';
import Stock from './stock.tsx';

export const metadata: Metadata = { title: 'Estoque' };

/** ?abaixo=1 (alerta do painel) abre a posicao ja filtrada pelos produtos abaixo do minimo. */
export default async function EstoquePage({ searchParams }: { searchParams: Promise<{ abaixo?: string | string[] }> }) {
  const { allowed } = await sessionFor('ESTOQUE', '/gestao/estoque');
  const belowMin = (await searchParams).abaixo === '1';
  return allowed ? <Stock initialBelowMin={belowMin} /> : <NoAccess />;
}
