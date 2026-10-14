import type { Metadata } from 'next';
import { sessionFor } from '../../../../lib/server/session.ts';
import { NoAccess } from '../../../../components/feedback.tsx';
import Promotions from './promotions.tsx';

export const metadata: Metadata = { title: 'Promoções' };

export default async function PromocoesPage() {
  const { allowed } = await sessionFor('PROMOCOES', '/gestao/promocoes');
  return allowed ? <Promotions /> : <NoAccess />;
}
