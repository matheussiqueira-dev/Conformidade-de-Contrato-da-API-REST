'use client';
import { useState } from 'react';
import { authApi } from '../lib/api/auth.ts';
import { api } from '../lib/api/http.ts';
import { Icon } from './brand/icons.tsx';

const auth = authApi(api);

export default function LogoutButton() {
  const [busy, setBusy] = useState(false);
  async function logout() {
    setBusy(true);
    try {
      await auth.logout();
    } finally {
      window.location.assign('/entrar');
    }
  }
  return <button type="button" className="btn btn-ghost" onClick={logout} disabled={busy}><Icon name="sair" />{busy ? 'Saindo…' : 'Sair'}</button>;
}
