'use client';
import { useEffect, useState } from 'react';
import { ApiError, authClient } from '../lib/auth-client.mjs';

const api = authClient();
export default function AccessPanel() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  useEffect(() => {
    const abort = new AbortController();
    api.me(abort.signal).then(setUser).catch(err => {
      if (err.name !== 'AbortError' && !(err instanceof ApiError && err.status === 401)) setError('Não conseguimos conectar ao sistema. Tente novamente.');
    }).finally(() => { if (!abort.signal.aborted) setLoading(false); });
    return () => abort.abort();
  }, []);
  async function login(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    setBusy(true); setError('');
    try { setUser(await api.login(String(data.get('email')), String(data.get('password')))); form.reset(); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Sem conexão com o sistema. Tente novamente.'); }
    finally { setBusy(false); }
  }
  async function logout() {
    setBusy(true); setError('');
    try { await api.logout(); setUser(null); }
    catch (err) { setError(err instanceof ApiError ? err.message : 'Não conseguimos encerrar a sessão. Tente novamente.'); }
    finally { setBusy(false); }
  }
  if (loading) return <p role="status">Verificando seu acesso…</p>;
  return <div className="access-panel">
    <p className="eyebrow">{user ? 'SUA CONTA' : 'BEM-VINDO À LOJA'}</p>
    <h2>{user ? `Olá, ${user.name}.` : 'Entre para continuar.'}</h2>
    <p className="intro">{user ? 'Você está conectado com sua conta individual.' : 'Use o e-mail e a senha da sua conta de equipe.'}</p>
    {error && <p className="error" role="alert">{error}</p>}
    {user ? <>
      <dl className="account"><div><dt>E-mail</dt><dd>{user.email}</dd></div><div><dt>Perfil</dt><dd>{user.role === 'GERENTE' ? 'Gerente' : 'Vendedor'}</dd></div></dl>
      <p className="availability">As áreas de vendas e acompanhamento estarão disponíveis após a conclusão da integração.</p>
      <button type="button" onClick={logout} disabled={busy}>{busy ? 'Saindo…' : 'Sair da conta'}</button>
    </> : <form onSubmit={login} aria-busy={busy}>
      <label htmlFor="email">E-mail</label>
      <input id="email" name="email" type="email" autoComplete="username" required maxLength={254} placeholder="voce@loja.com" disabled={busy} />
      <label htmlFor="password">Senha</label>
      <input id="password" name="password" type="password" autoComplete="current-password" required minLength={8} maxLength={72} disabled={busy} />
      <button type="submit" disabled={busy}>{busy ? 'Entrando…' : 'Entrar na minha conta'}<span aria-hidden="true"> →</span></button>
      <p className="access-help">Precisa de acesso? Fale com o gerente da loja.</p>
    </form>}
  </div>;
}
