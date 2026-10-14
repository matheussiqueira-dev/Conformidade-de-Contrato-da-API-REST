import type { Metadata } from 'next';
import { sessionFor } from '../../../../lib/server/session.ts';
import { NoAccess } from '../../../../components/feedback.tsx';
import Products from './products.tsx';

export const metadata: Metadata = { title: 'Produtos' };

export default async function ProdutosPage() {
  const { allowed } = await sessionFor('PRODUTOS', '/gestao/produtos');
  return allowed ? <Products /> : <NoAccess />;
}
