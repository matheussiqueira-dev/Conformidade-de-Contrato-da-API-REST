import type { Metadata } from 'next';
import { sessionFor } from '../../../lib/server/session.ts';
import { NoAccess } from '../../../components/feedback.tsx';
import SalesList from './sales-list.tsx';

export const metadata: Metadata = { title: 'Vendas' };

/** ?situacao=PENDING_PAYMENT&pagina=2 abre a lista ja filtrada (link do painel e volta do detalhe da venda). */
export default async function VendasPage() {
  const { user, allowed } = await sessionFor('VENDAS', '/vendas');
  return allowed ? <SalesList manager={user.role === 'GERENTE'} /> : <NoAccess />;
}
