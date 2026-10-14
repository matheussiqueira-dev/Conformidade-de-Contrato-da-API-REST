'use client';
import { useState, type FormEvent } from 'react';
import { useSearchParams } from 'next/navigation';
import { authApi } from '../../lib/api/auth.ts';
import { ApiError, api } from '../../lib/api/http.ts';
import { ERROR_MESSAGES, LOCKOUT_HINT, NETWORK_MESSAGE } from '../../lib/api/messages.ts';
import { landingPath } from '../../lib/areas.ts';

const auth = authApi(api);

export default function LoginForm() {
  const params = useSearchParams();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const revoked = params.get('sessao') === 'encerrada';

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setBusy(true);
    setError('');
    try {
      const user = await auth.login(String(data.get('email') ?? ''), String(data.get('password') ?? ''));
      // Navegacao completa: os layouts do servidor leem o cookie novo.
      window.location.assign(landingPath(user.areas, params.get('next')));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : NETWORK_MESSAGE);
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} aria-busy={busy} className="stack-sm">
      {revoked && !error && <p className="notice" role="status">{ERROR_MESSAGES.SESSION_REVOKED}</p>}
      {error && <p className="notice notice-error" role="alert">{error}</p>}
      <div className="field">
        <label htmlFor="email">E-mail</label>
        <input className="input" id="email" name="email" type="email" autoComplete="username" required maxLength={254}
               placeholder="voce@loja.com" disabled={busy} />
      </div>
      <div className="field">
        <label htmlFor="password">Senha</label>
        <input className="input" id="password" name="password" type="password" autoComplete="current-password" required
               minLength={8} maxLength={72} disabled={busy} />
      </div>
      <button type="submit" className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Entrando…' : 'Entrar'}</button>
      <p className="muted login-help">{LOCKOUT_HINT} Precisa de acesso? Fale com o gerente da loja.</p>
    </form>
  );
}
