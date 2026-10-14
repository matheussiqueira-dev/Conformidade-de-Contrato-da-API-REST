import { redirect } from 'next/navigation';
import { landingPath } from '../lib/areas.ts';
import { currentUser } from '../lib/server/session.ts';

/** Inicio: leva cada perfil para a primeira area liberada pelo servidor. */
export default async function Home() {
  const user = await currentUser();
  redirect(user ? landingPath(user.areas) : '/entrar');
}
