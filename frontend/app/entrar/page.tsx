import type { Metadata } from 'next';
import { Suspense } from 'react';
import LoginForm from './login-form.tsx';

export const metadata: Metadata = { title: 'Entrar' };

export default function LoginPage() {
  return (
    <main className="login">
      <section className="card" aria-labelledby="login-title">
        <p className="wordmark" aria-label="SBI">SBI</p>
        <h1 id="login-title">Entre para continuar.</h1>
        <p className="muted">Use o e-mail e a senha da sua conta de equipe.</p>
        <Suspense fallback={<p role="status">Carregando…</p>}>
          <LoginForm />
        </Suspense>
      </section>
    </main>
  );
}
