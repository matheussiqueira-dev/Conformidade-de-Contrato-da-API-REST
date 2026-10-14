'use client';
// Troca da propria senha (PUT /auth/password). Sucesso renova a sessao e o token CSRF; a tela busca as areas de
// novo e vai para a primeira area liberada. Senha atual errada responde 401 sem encerrar a sessao e conta para o
// bloqueio de 5 tentativas.
import { useEffect, useRef, useState, type FormEvent } from 'react';
import { authApi } from '../../../lib/api/auth.ts';
import { ApiError, api } from '../../../lib/api/http.ts';
import { LOCKOUT_HINT, WRONG_CURRENT_PASSWORD } from '../../../lib/api/messages.ts';
import { landingPath } from '../../../lib/areas.ts';
import { errorMessage } from '../../../components/feedback.tsx';
import { passwordProblems, type PasswordField } from '../../../lib/password.ts';

const auth = authApi(api);
const FIELDS: readonly PasswordField[] = ['currentPassword', 'newPassword', 'confirmation'];

export default function PasswordForm({ email }: { email: string }) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [problems, setProblems] = useState<Partial<Record<PasswordField, string>>>({});
  const [focusTarget, setFocusTarget] = useState<PasswordField | null>(null);
  const form = useRef<HTMLFormElement>(null);

  // O foco vai para o primeiro campo com erro depois da renderizacao: durante o envio os campos ficam desabilitados.
  useEffect(() => {
    if (!focusTarget || busy) return;
    form.current?.querySelector<HTMLInputElement>(`[name="${focusTarget}"]`)?.focus();
    setFocusTarget(null);
  }, [focusTarget, busy]);

  function focusFirst(found: Partial<Record<PasswordField, string>>) {
    setFocusTarget(FIELDS.find(field => found[field]) ?? null);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const values = { currentPassword: String(data.get('currentPassword') ?? ''), newPassword: String(data.get('newPassword') ?? ''),
      confirmation: String(data.get('confirmation') ?? '') };
    const found = passwordProblems(values, email);
    setProblems(found);
    setError('');
    if (Object.keys(found).length > 0) { focusFirst(found); return; }
    setBusy(true);
    try {
      await api.request('/auth/password', { method: 'PUT', body: { currentPassword: values.currentPassword, newPassword: values.newPassword } });
      api.resetCsrf();
      const user = await auth.me();
      window.location.assign(landingPath(user.areas));
    } catch (err) {
      setBusy(false);
      if (err instanceof ApiError && err.status === 401 && err.code === 'UNAUTHENTICATED') {
        setProblems({ currentPassword: WRONG_CURRENT_PASSWORD });
        focusFirst({ currentPassword: WRONG_CURRENT_PASSWORD });
        return;
      }
      setError(err instanceof ApiError && err.details.length > 0 ? `${err.message} ${err.details.join(' ')}` : errorMessage(err));
    }
  }

  const described = (field: PasswordField, hint?: string) => [problems[field] ? `${field}-erro` : '', hint ?? ''].filter(Boolean).join(' ') || undefined;
  return (
    <form ref={form} onSubmit={submit} aria-busy={busy} className="stack-sm" noValidate>
      <div aria-live="polite">{error && <p className="notice notice-error" role="alert">{error}</p>}</div>
      <div className="field">
        <label htmlFor="currentPassword">Senha atual</label>
        <input className="input" id="currentPassword" name="currentPassword" type="password" autoComplete="current-password"
               required maxLength={72} disabled={busy} aria-invalid={Boolean(problems.currentPassword)} aria-describedby={described('currentPassword', 'senha-bloqueio')} />
        <span className="hint" id="senha-bloqueio">{LOCKOUT_HINT}</span>
        {problems.currentPassword && <span className="field-error" id="currentPassword-erro">{problems.currentPassword}</span>}
      </div>
      <div className="field">
        <label htmlFor="newPassword">Nova senha</label>
        <input className="input" id="newPassword" name="newPassword" type="password" autoComplete="new-password"
               required minLength={12} maxLength={72} disabled={busy} aria-invalid={Boolean(problems.newPassword)} aria-describedby={described('newPassword', 'senha-regra')} />
        <span className="hint" id="senha-regra">De 12 a 72 caracteres. Uma frase longa é mais segura e fácil de lembrar.</span>
        {problems.newPassword && <span className="field-error" id="newPassword-erro">{problems.newPassword}</span>}
      </div>
      <div className="field">
        <label htmlFor="confirmation">Repita a nova senha</label>
        <input className="input" id="confirmation" name="confirmation" type="password" autoComplete="new-password"
               required maxLength={72} disabled={busy} aria-invalid={Boolean(problems.confirmation)} aria-describedby={described('confirmation')} />
        {problems.confirmation && <span className="field-error" id="confirmation-erro">{problems.confirmation}</span>}
      </div>
      <button type="submit" className="btn btn-primary btn-block" disabled={busy}>{busy ? 'Salvando…' : 'Salvar nova senha'}</button>
    </form>
  );
}
