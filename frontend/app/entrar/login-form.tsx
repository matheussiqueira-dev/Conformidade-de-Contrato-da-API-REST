'use client';
import { useEffect, useState, type FormEvent } from 'react';
import { useSearchParams } from 'next/navigation';
import { authApi } from '../../lib/api/auth.ts';
import { API_PREFIX, ApiError, api } from '../../lib/api/http.ts';
import { ERROR_MESSAGES, LOCKOUT_HINT, NETWORK_MESSAGE, SLOW_START_HINT } from '../../lib/api/messages.ts';
import { landingPath } from '../../lib/areas.ts';
import ShowPassword from '../../components/show-password.tsx';

const auth = authApi(api);
/** Depois disso a espera deixa de ser normal: no ambiente publicado o servidor costuma estar acordando. */
const SLOW_AFTER_MS = 8_000;
/** D-EVO-41: so com NEXT_PUBLIC_SBI_AQUECER=1 no build (dia de apresentacao); desligado nao gasta cota. */
const WARM_UP = process.env.NEXT_PUBLIC_SBI_AQUECER === '1';

export default function LoginForm() {
  const params = useSearchParams();
  const [busy, setBusy] = useState(false);
  const [slow, setSlow] = useState(false);
  const [error, setError] = useState('');
  const [visible, setVisible] = useState(false);
  const revoked = params.get('sessao') === 'encerrada';

  useEffect(() => {
    if (!WARM_UP) return;
    // So acorda a API e o banco enquanto a pessoa digita; o resultado nao importa (o login busca o proprio token).
    void fetch(`${API_PREFIX}/auth/csrf`, { credentials: 'same-origin', cache: 'no-store' }).catch(() => undefined);
  }, []);

  useEffect(() => {
    if (!busy) { setSlow(false); return; }
    const timer = setTimeout(() => setSlow(true), SLOW_AFTER_MS);
    return () => clearTimeout(timer);
  }, [busy]);

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
        <input className="input" id="email" name="email" type="email" autoComplete="username" spellCheck={false} required maxLength={254}
               placeholder="nome@sbi.tech" disabled={busy} />
      </div>
      <div className="field">
        <label htmlFor="password">Senha</label>
        <input className="input" id="password" name="password" type={visible ? 'text' : 'password'} autoComplete="current-password" required
               minLength={8} maxLength={72} disabled={busy} />
        <ShowPassword checked={visible} onChange={setVisible} disabled={busy} />
      </div>
      <button type="submit" className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Entrando…' : 'Entrar'}</button>
      <div role="status" className="status-slot">{slow && <p className="notice notice-warn">{SLOW_START_HINT}</p>}</div>
      <p className="muted login-help">{LOCKOUT_HINT} Precisa de acesso? Fale com o gerente da loja.</p>
    </form>
  );
}
