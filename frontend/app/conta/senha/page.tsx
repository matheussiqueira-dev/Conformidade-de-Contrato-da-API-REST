import type { Metadata } from 'next';
import { redirect } from 'next/navigation';
import { menuFor, PASSWORD_PATH } from '../../../lib/areas.ts';
import { currentUser } from '../../../lib/server/session.ts';
import PasswordForm from './password-form.tsx';
import LogoutButton from '../../../components/logout-button.tsx';

export const metadata: Metadata = { title: 'Minha senha' };

/** Fora do grupo (app): com senha provisoria o layout das areas redireciona para ca, sem laco. */
export default async function PasswordPage() {
  const user = await currentUser();
  if (!user) redirect(`/entrar?next=${encodeURIComponent(PASSWORD_PATH)}`);
  const required = user.areas.includes('TROCAR_SENHA');
  const back = menuFor(user.areas)[0]?.href;
  return (
    <main className="login">
      <section className="card" aria-labelledby="senha-titulo">
        <p className="wordmark" aria-label="SBI">SBI</p>
        <h1 id="senha-titulo">{required ? 'Troque a senha provisória' : 'Alterar minha senha'}</h1>
        <p className="muted">
          {required
            ? `Olá, ${user.name}. Para continuar, crie uma senha só sua.`
            : 'Ao trocar a senha, as suas outras sessões abertas são encerradas.'}
        </p>
        <PasswordForm email={user.email} />
        {back && <a className="btn btn-ghost btn-block" href={back}>Voltar sem alterar</a>}
        <div className="actions"><LogoutButton /></div>
      </section>
    </main>
  );
}
