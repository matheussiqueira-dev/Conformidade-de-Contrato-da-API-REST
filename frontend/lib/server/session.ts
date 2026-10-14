// Sessao no servidor Next: o layout de cada area chama /auth/me repassando o cookie (WP-3.4).
// A API continua sendo a autoridade; isto so decide o que renderizar e para onde redirecionar.
import { cookies } from 'next/headers';
import { redirect } from 'next/navigation';
import { GetCurrentUserResponse } from '../api/generated/sbi.zod.ts';
import type { UserSession } from '../api/auth.ts';
import type { Area } from '../areas.ts';

function backendOrigin(): string {
  return new URL(process.env.BACKEND_ORIGIN ?? 'http://127.0.0.1:8080').origin;
}

export async function currentUser(): Promise<UserSession | null> {
  const session = (await cookies()).get('JSESSIONID');
  if (!session) return null;
  const response = await fetch(`${backendOrigin()}/auth/me`, {
    headers: { Accept: 'application/json', Cookie: `JSESSIONID=${session.value}` },
    cache: 'no-store',
  });
  if (response.status === 401) return null;
  if (!response.ok) throw new Error(`Falha ao consultar a sessão (${response.status})`);
  return GetCurrentUserResponse.parse(await response.json());
}

/** Sem sessao vai para /entrar?next=...; com sessao, informa se a area foi liberada pelo servidor. */
export async function sessionFor(area: Area, path: string): Promise<{ user: UserSession; allowed: boolean }> {
  const user = await currentUser();
  if (!user) redirect(`/entrar?next=${encodeURIComponent(path)}`);
  return { user, allowed: user.areas.includes(area) };
}
