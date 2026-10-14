// Regras de exibicao da tela de usuarios (WP-EVO-2). O servidor continua sendo a autoridade: estas funcoes so
// decidem rotulos e quais acoes aparecem.
import { formatTime } from './format.ts';

export interface UserRow {
  id: number;
  active: boolean;
  lockedUntil: string | null;
}

export type Situation = { label: string; tone: 'ok' | 'warn' | 'off' };

/** Inativo, bloqueado (com a hora do fim do bloqueio) ou ativo. */
export function situation(user: UserRow, now: Date = new Date()): Situation {
  if (!user.active) return { label: 'Inativo', tone: 'off' };
  if (user.lockedUntil && new Date(user.lockedUntil).getTime() > now.getTime()) {
    return { label: `Bloqueado até ${formatTime(user.lockedUntil)}`, tone: 'warn' };
  }
  return { label: 'Ativo', tone: 'ok' };
}

export type UserAction = 'edit' | 'role' | 'status' | 'unlock' | 'reset';

/** Na linha do proprio usuario nao aparecem papel, situacao nem redefinicao de senha (409 no servidor). */
export function actionsFor(user: UserRow, currentUserId: number, now: Date = new Date()): UserAction[] {
  const self = user.id === currentUserId;
  const actions: UserAction[] = ['edit'];
  if (!self) actions.push('role', 'status');
  if (situation(user, now).tone === 'warn') actions.push('unlock');
  if (!self) actions.push('reset');
  return actions;
}
