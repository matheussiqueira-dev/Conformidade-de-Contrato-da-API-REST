'use client';
// Usuarios (WP-EVO-2): cadastro com senha provisoria, edicao com versao, papel e situacao com motivo,
// desbloqueio e redefinicao de senha. Papel, situacao, desbloqueio e senha encerram as sessoes do usuario.
import { useState, type FormEvent } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { core, z } from 'zod';
import {
  ChangeUserRoleBody, ChangeUserRoleResponse, ChangeUserStatusBody, ChangeUserStatusResponse, CreateUserBody, CreateUserResponse,
  GetUserResponse, ListUsersResponse, ResetUserPasswordBody, UpdateUserBody, UpdateUserResponse,
} from '../../../../lib/api/generated/sbi.zod.ts';
import { api } from '../../../../lib/api/http.ts';
import { formatDateTime } from '../../../../lib/format.ts';
import { issueMessage } from '../../../../lib/forms.ts';
import { actionsFor, situation, type UserAction } from '../../../../lib/users.ts';
import { ErrorNotice, Loading } from '../../../../components/feedback.tsx';
import { Field } from '../../../../components/customer-form.tsx';
import Drawer from '../../../../components/drawer.tsx';
import Pager from '../../../../components/pager.tsx';

type User = z.infer<typeof ListUsersResponse>['content'][number];
type Panel = { kind: 'new' } | { kind: UserAction; user: User } | null;

const ROLE_LABEL: Record<string, string> = { GERENTE: 'Gerente', VENDEDOR: 'Vendedor' };
const ACTION_LABEL: Record<UserAction, string> = {
  edit: 'Editar', role: 'Papel', status: 'Situação', unlock: 'Desbloquear', reset: 'Redefinir senha',
};
const PASSWORD_HINT = 'De 12 a 72 caracteres; sem senhas comuns nem o próprio e-mail. A pessoa troca no primeiro acesso.';

function problemsOf(result: { success: boolean; error?: { issues: core.$ZodIssue[] } | undefined }): string[] {
  return result.success || !result.error ? [] : result.error.issues.map(issue => `${issue.path.join('.') || 'formulário'}: ${issueMessage(issue)}`);
}

function Problems({ items }: { items: string[] }) {
  if (items.length === 0) return null;
  return <div className="notice notice-error" role="alert">{items.map(p => <p key={p}>{p}</p>)}</div>;
}

export default function Users({ currentUserId }: { currentUserId: number }) {
  const client = useQueryClient();
  const [q, setQ] = useState('');
  const [role, setRole] = useState('');
  const [active, setActive] = useState('');
  const [page, setPage] = useState(0);
  const [panel, setPanel] = useState<Panel>(null);
  const list = useQuery({
    queryKey: ['users', q, role, active, page],
    queryFn: ({ signal }) => {
      const params = new URLSearchParams({ q, page: String(page), size: '20' });
      if (role) params.set('role', role);
      if (active) params.set('active', active);
      return api.request(`/users?${params}`, { signal, schema: ListUsersResponse });
    },
    placeholderData: keepPreviousData,
  });
  const done = () => { void client.invalidateQueries({ queryKey: ['users'] }); setPanel(null); };
  const data = list.data;
  const title = panel === null ? '' : panel.kind === 'new' ? 'Cadastrar usuário' : `${ACTION_LABEL[panel.kind]} · ${panel.user.name}`;

  return (
    <div className="stack">
      <header className="toolbar">
        <div className="stack-sm"><p className="muted">Gestão / Usuários</p><h1>Usuários da equipe</h1></div>
        <div className="actions"><button type="button" className="btn btn-primary" onClick={() => setPanel({ kind: 'new' })}>Cadastrar usuário</button></div>
      </header>
      <section className="card stack" aria-label="Lista de usuários">
        <div className="toolbar">
          <div className="field"><label htmlFor="busca-usuarios">Nome ou e-mail</label>
            <input id="busca-usuarios" className="input" type="search" value={q} onChange={event => { setQ(event.target.value); setPage(0); }} /></div>
          <div className="field"><label htmlFor="filtro-papel">Papel</label>
            <select id="filtro-papel" className="select" value={role} onChange={event => { setRole(event.target.value); setPage(0); }}>
              <option value="">Todos</option><option value="GERENTE">Gerente</option><option value="VENDEDOR">Vendedor</option></select></div>
          <div className="field"><label htmlFor="filtro-situacao">Situação</label>
            <select id="filtro-situacao" className="select" value={active} onChange={event => { setActive(event.target.value); setPage(0); }}>
              <option value="">Todas</option><option value="true">Ativos</option><option value="false">Inativos</option></select></div>
        </div>
        <ErrorNotice error={list.error} />
        {list.isLoading ? <Loading label="Carregando usuários…" /> : data && data.content.length === 0
          ? <p className="empty">Nenhum usuário encontrado.</p>
          : data && <div className="table-wrap"><table>
              <caption className="sr-only">Usuários</caption>
              <thead><tr><th scope="col">Nome</th><th scope="col">E-mail</th><th scope="col">Papel</th><th scope="col">Situação</th>
                <th scope="col">Último acesso</th><th scope="col"><span className="sr-only">Ações</span></th></tr></thead>
              <tbody>{data.content.map(u => {
                const state = situation(u);
                return (
                  <tr key={u.id}>
                    <td>{u.name}{u.id === currentUserId && <span className="muted"> (você)</span>}</td>
                    <td className="mono">{u.email}</td>
                    <td>{ROLE_LABEL[u.role] ?? u.role}</td>
                    <td><span className={`pill ${state.tone === 'ok' ? 'pill-ok' : state.tone === 'warn' ? 'pill-warn' : ''}`}>{state.label}</span></td>
                    <td>{u.lastLoginAt ? formatDateTime(u.lastLoginAt) : '—'}</td>
                    <td><div className="actions">
                      {actionsFor(u, currentUserId).map(action => {
                        const label = action === 'status' ? (u.active ? 'Desativar' : 'Ativar') : ACTION_LABEL[action];
                        return (
                          <button key={action} type="button" className={action === 'edit' ? 'btn' : 'btn btn-ghost'}
                                  aria-label={`${label}: ${u.name}`} onClick={() => setPanel({ kind: action, user: u })}>{label}</button>
                        );
                      })}
                    </div></td>
                  </tr>
                );
              })}</tbody>
            </table></div>}
        {data && <Pager page={data.page} size={data.size} total={data.totalElements} onPage={setPage} />}
      </section>
      <Drawer open={panel !== null} title={title} onClose={() => setPanel(null)}>
        {panel?.kind === 'new' && <CreateForm onDone={done} />}
        {panel?.kind === 'edit' && <EditForm user={panel.user} onDone={done} />}
        {panel?.kind === 'role' && <RoleForm user={panel.user} onDone={done} />}
        {panel?.kind === 'status' && <StatusForm user={panel.user} onDone={done} />}
        {panel?.kind === 'unlock' && <UnlockForm user={panel.user} onDone={done} />}
        {panel?.kind === 'reset' && <ResetForm user={panel.user} onDone={done} />}
      </Drawer>
    </div>
  );
}

function SubmitButton({ busy, label }: { busy: boolean; label: string }) {
  return <div className="actions"><button type="submit" className="btn btn-primary" disabled={busy}>{busy ? 'Salvando…' : label}</button></div>;
}

function CreateForm({ onDone }: { onDone: () => void }) {
  const [problems, setProblems] = useState<string[]>([]);
  const save = useMutation({
    mutationFn: (body: unknown) => api.request('/users', { method: 'POST', body, schema: CreateUserResponse }),
    onSuccess: onDone,
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const body = { name: String(form.get('name') ?? '').trim(), email: String(form.get('email') ?? '').trim(),
      role: String(form.get('role') ?? ''), temporaryPassword: String(form.get('temporaryPassword') ?? '') };
    const check = CreateUserBody.safeParse(body);
    setProblems(problemsOf(check));
    if (check.success) save.mutate(body);
  }
  return (
    <form className="stack" onSubmit={submit} aria-busy={save.isPending}>
      <div className="form-grid">
        <Field id="usuario-nome" label="Nome"><input id="usuario-nome" name="name" className="input" required minLength={3} maxLength={120} /></Field>
        <Field id="usuario-email" label="E-mail"><input id="usuario-email" name="email" className="input" type="email" required maxLength={254} autoComplete="off" /></Field>
        <Field id="usuario-papel" label="Papel"><select id="usuario-papel" name="role" className="select" defaultValue="VENDEDOR">
          <option value="VENDEDOR">Vendedor</option><option value="GERENTE">Gerente</option></select></Field>
        <Field id="usuario-senha" label="Senha provisória" hint={PASSWORD_HINT}>
          <input id="usuario-senha" name="temporaryPassword" className="input" type="password" required minLength={12} maxLength={72} autoComplete="new-password" /></Field>
      </div>
      <Problems items={problems} />
      <ErrorNotice error={save.error} />
      <SubmitButton busy={save.isPending} label="Cadastrar" />
    </form>
  );
}

function EditForm({ user, onDone }: { user: User; onDone: () => void }) {
  const detail = useQuery({ queryKey: ['user', user.id], queryFn: ({ signal }) => api.request(`/users/${user.id}`, { signal, schema: GetUserResponse }) });
  const [problems, setProblems] = useState<string[]>([]);
  const save = useMutation({
    mutationFn: (body: unknown) => api.request(`/users/${user.id}`, { method: 'PUT', body, schema: UpdateUserResponse }),
    onSuccess: onDone,
  });
  if (detail.isLoading) return <Loading label="Carregando usuário…" />;
  if (!detail.data) return <ErrorNotice error={detail.error} />;
  const version = detail.data.version;
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const body = { name: String(form.get('name') ?? '').trim(), email: String(form.get('email') ?? '').trim(), version };
    const check = UpdateUserBody.safeParse(body);
    setProblems(problemsOf(check));
    if (check.success) save.mutate(body);
  }
  return (
    <form className="stack" onSubmit={submit} aria-busy={save.isPending}>
      <p className="muted">Papel, situação e senha têm ações próprias.</p>
      <div className="form-grid">
        <Field id="editar-nome" label="Nome"><input id="editar-nome" name="name" className="input" defaultValue={detail.data.name} required minLength={3} maxLength={120} /></Field>
        <Field id="editar-email" label="E-mail"><input id="editar-email" name="email" className="input" type="email" defaultValue={detail.data.email} required maxLength={254} /></Field>
      </div>
      <Problems items={problems} />
      <ErrorNotice error={save.error} />
      <SubmitButton busy={save.isPending} label="Salvar" />
    </form>
  );
}

function ReasonField({ id }: { id: string }) {
  return (
    <Field id={id} label="Motivo" hint="De 5 a 200 caracteres; fica registrado na auditoria.">
      <input id={id} name="reason" className="input" required minLength={5} maxLength={200} />
    </Field>
  );
}

const REVOKES = 'As sessões abertas desta pessoa serão encerradas no próximo acesso.';

function RoleForm({ user, onDone }: { user: User; onDone: () => void }) {
  const [problems, setProblems] = useState<string[]>([]);
  const save = useMutation({
    mutationFn: (body: unknown) => api.request(`/users/${user.id}/role`, { method: 'PATCH', body, schema: ChangeUserRoleResponse }),
    onSuccess: onDone,
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const body = { role: String(form.get('role') ?? ''), reason: String(form.get('reason') ?? '').trim() };
    const check = ChangeUserRoleBody.safeParse(body);
    setProblems(problemsOf(check));
    if (check.success) save.mutate(body);
  }
  return (
    <form className="stack" onSubmit={submit} aria-busy={save.isPending}>
      <p className="muted">Papel atual: {ROLE_LABEL[user.role]}. {REVOKES}</p>
      <div className="form-grid">
        <Field id="papel-novo" label="Novo papel"><select id="papel-novo" name="role" className="select" defaultValue={user.role === 'GERENTE' ? 'VENDEDOR' : 'GERENTE'}>
          <option value="VENDEDOR">Vendedor</option><option value="GERENTE">Gerente</option></select></Field>
        <ReasonField id="papel-motivo" />
      </div>
      <Problems items={problems} />
      <ErrorNotice error={save.error} />
      <SubmitButton busy={save.isPending} label="Alterar papel" />
    </form>
  );
}

function StatusForm({ user, onDone }: { user: User; onDone: () => void }) {
  const [problems, setProblems] = useState<string[]>([]);
  const next = !user.active;
  const save = useMutation({
    mutationFn: (body: unknown) => api.request(`/users/${user.id}/status`, { method: 'PATCH', body, schema: ChangeUserStatusResponse }),
    onSuccess: onDone,
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const body = { active: next, reason: String(form.get('reason') ?? '').trim() };
    const check = ChangeUserStatusBody.safeParse(body);
    setProblems(problemsOf(check));
    if (check.success) save.mutate(body);
  }
  return (
    <form className="stack" onSubmit={submit} aria-busy={save.isPending}>
      <p className="muted">{next ? 'A pessoa volta a poder entrar.' : `A pessoa não consegue mais entrar. ${REVOKES}`} O histórico de vendas é mantido.</p>
      <ReasonField id="situacao-motivo" />
      <Problems items={problems} />
      <ErrorNotice error={save.error} />
      <SubmitButton busy={save.isPending} label={next ? 'Ativar' : 'Desativar'} />
    </form>
  );
}

function UnlockForm({ user, onDone }: { user: User; onDone: () => void }) {
  const unlock = useMutation({ mutationFn: () => api.request(`/users/${user.id}/unlock`, { method: 'POST' }), onSuccess: onDone });
  return (
    <form className="stack" onSubmit={event => { event.preventDefault(); unlock.mutate(); }} aria-busy={unlock.isPending}>
      <p className="muted">{situation(user).label}. Desbloquear zera as tentativas e permite entrar agora. {REVOKES}</p>
      <ErrorNotice error={unlock.error} />
      <SubmitButton busy={unlock.isPending} label="Desbloquear" />
    </form>
  );
}

function ResetForm({ user, onDone }: { user: User; onDone: () => void }) {
  const [problems, setProblems] = useState<string[]>([]);
  const reset = useMutation({
    mutationFn: (body: unknown) => api.request(`/users/${user.id}/password-reset`, { method: 'POST', body }),
    onSuccess: onDone,
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const body = { temporaryPassword: String(form.get('temporaryPassword') ?? '') };
    const check = ResetUserPasswordBody.safeParse(body);
    setProblems(problemsOf(check));
    if (check.success) reset.mutate(body);
  }
  return (
    <form className="stack" onSubmit={submit} aria-busy={reset.isPending}>
      <p className="muted">Entregue a senha provisória pessoalmente. {REVOKES}</p>
      <Field id="redefinir-senha" label="Senha provisória" hint={PASSWORD_HINT}>
        <input id="redefinir-senha" name="temporaryPassword" className="input" type="password" required minLength={12} maxLength={72} autoComplete="new-password" /></Field>
      <Problems items={problems} />
      <ErrorNotice error={reset.error} />
      <SubmitButton busy={reset.isPending} label="Redefinir senha" />
    </form>
  );
}
