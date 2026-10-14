import type { ReactNode } from 'react';
import { redirect } from 'next/navigation';
import Link from 'next/link';
import { menuFor, PASSWORD_PATH } from '../../lib/areas.ts';
import { currentUser } from '../../lib/server/session.ts';
import NavLinks from '../../components/nav-links.tsx';
import LogoutButton from '../../components/logout-button.tsx';

/** Shell das areas internas: menu montado a partir de UserSession.areas, so com telas existentes. */
export default async function AppLayout({ children }: { children: ReactNode }) {
  const user = await currentUser();
  if (!user) redirect('/entrar');
  // Senha provisoria: nenhuma area de trabalho ate a troca (a API responde 403 PASSWORD_CHANGE_REQUIRED).
  if (user.areas.includes('TROCAR_SENHA')) redirect(PASSWORD_PATH);
  const menu = menuFor(user.areas);
  return (
    <div className="shell">
      <a className="skip-link" href="#conteudo">Pular para o conteúdo</a>
      <aside className="sidebar" aria-label="Navegação principal">
        <a className="wordmark" href={menu[0]?.href ?? '/'}>SBI</a>
        <NavLinks items={menu} />
        <div className="account-box">
          <span><strong>{user.name}</strong><br /><span className="muted">{user.role === 'GERENTE' ? 'Gerente' : 'Vendedor'}</span></span>
          <Link href={PASSWORD_PATH} className="btn btn-ghost">Minha senha</Link>
          <LogoutButton />
        </div>
      </aside>
      <main id="conteudo" className="main" tabIndex={-1}>{children}</main>
    </div>
  );
}
