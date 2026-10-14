'use client';
// Alcada de desconto manual por papel (G-04): o GERENTE altera com motivo; a mudanca e auditada.
import { useState, type FormEvent } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { z } from 'zod';
import { ListDiscountPoliciesResponse, UpdateDiscountPolicyBody, UpdateDiscountPolicyResponse } from '../../../../lib/api/generated/sbi.zod.ts';
import { api } from '../../../../lib/api/http.ts';
import { formatDateTime } from '../../../../lib/format.ts';
import { ErrorNotice, Loading } from '../../../../components/feedback.tsx';
import { Field } from '../../../../components/customer-form.tsx';
import Drawer from '../../../../components/drawer.tsx';

type Policy = z.infer<typeof ListDiscountPoliciesResponse>[number];
const ROLE_LABEL: Record<string, string> = { GERENTE: 'Gerente', VENDEDOR: 'Vendedor' };
const percent = (value: number) => `${value.toLocaleString('pt-BR', { maximumFractionDigits: 2 })}%`;

export default function DiscountPolicies() {
  const client = useQueryClient();
  const [editing, setEditing] = useState<Policy | null>(null);
  const list = useQuery({ queryKey: ['discount-policy'], queryFn: ({ signal }) => api.request('/discount-policy', { signal, schema: ListDiscountPoliciesResponse }) });
  return (
    <section className="card stack" aria-labelledby="alcada-titulo">
      <div className="stack-sm">
        <h2 id="alcada-titulo">Alçada de desconto manual</h2>
        <p className="muted">Percentual máximo que cada papel concede no balcão. Acima disso, o gerente aprova no pedido pendente.</p>
      </div>
      <ErrorNotice error={list.error} />
      {list.isLoading ? <Loading /> : list.data && <div className="table-wrap"><table>
        <caption className="sr-only">Alçadas por papel</caption>
        <thead><tr><th scope="col">Papel</th><th scope="col" className="right">Máximo</th><th scope="col">Abaixo do custo</th>
          <th scope="col">Alterado</th><th scope="col"><span className="sr-only">Ações</span></th></tr></thead>
        <tbody>{list.data.map(p => (
          <tr key={p.role}>
            <td>{ROLE_LABEL[p.role] ?? p.role}</td>
            <td className="num right">{percent(p.maxPercent)}</td>
            <td>{p.allowBelowCost ? 'Permitido' : 'Bloqueado'}</td>
            <td className="num">{formatDateTime(p.updatedAt)}{p.updatedBy ? ` por ${p.updatedBy.name}` : ''}</td>
            <td><button type="button" className="btn" aria-label={`Alterar alçada: ${ROLE_LABEL[p.role] ?? p.role}`} onClick={() => setEditing(p)}>Alterar</button></td>
          </tr>
        ))}</tbody>
      </table></div>}
      <Drawer open={editing !== null} title={editing ? `Alçada · ${ROLE_LABEL[editing.role] ?? editing.role}` : ''} onClose={() => setEditing(null)}>
        {editing && <PolicyForm policy={editing} onDone={() => { setEditing(null); void client.invalidateQueries({ queryKey: ['discount-policy'] }); }} />}
      </Drawer>
    </section>
  );
}

function PolicyForm({ policy, onDone }: { policy: Policy; onDone: () => void }) {
  const [problem, setProblem] = useState('');
  const save = useMutation({
    mutationFn: (body: unknown) => api.request(`/discount-policy/${policy.role}`, { method: 'PUT', body, schema: UpdateDiscountPolicyResponse }),
    onSuccess: onDone,
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const body = { maxPercent: Number(String(form.get('maxPercent') ?? '').replace(',', '.')), allowBelowCost: form.get('allowBelowCost') === 'on',
      reason: String(form.get('reason') ?? '').trim() };
    const check = UpdateDiscountPolicyBody.safeParse(body);
    setProblem(check.success ? '' : 'Informe um percentual de 0 a 100 com até 2 casas e um motivo de 5 a 200 caracteres.');
    if (check.success) save.mutate(body);
  }
  return (
    <form className="stack" onSubmit={submit} aria-busy={save.isPending}>
      <div className="form-grid">
        <Field id="alcada-maximo" label="Máximo (%)"><input id="alcada-maximo" name="maxPercent" className="input" inputMode="decimal" defaultValue={String(policy.maxPercent).replace('.', ',')} required /></Field>
        <Field id="alcada-motivo" label="Motivo" hint="De 5 a 200 caracteres; fica na auditoria."><input id="alcada-motivo" name="reason" className="input" required minLength={5} maxLength={200} /></Field>
      </div>
      <label className="actions"><input type="checkbox" name="allowBelowCost" defaultChecked={policy.allowBelowCost} /> Permitir vender abaixo do custo médio</label>
      {problem && <p className="notice notice-error" role="alert">{problem}</p>}
      <ErrorNotice error={save.error} />
      <div className="actions"><button type="submit" className="btn btn-primary" disabled={save.isPending}>{save.isPending ? 'Salvando…' : 'Salvar alçada'}</button></div>
    </form>
  );
}
