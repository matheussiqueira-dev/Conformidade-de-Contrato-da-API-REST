import type { Metadata } from 'next';
import { sessionFor } from '../../../../lib/server/session.ts';
import { NoAccess } from '../../../../components/feedback.tsx';
import Users from './users.tsx';

export const metadata: Metadata = { title: 'Usuários' };

export default async function UsuariosPage() {
  const { user, allowed } = await sessionFor('USUARIOS', '/gestao/usuarios');
  return allowed ? <Users currentUserId={user.id} /> : <NoAccess />;
}
