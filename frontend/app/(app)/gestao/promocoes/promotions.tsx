'use client';
// Promocoes (WP-3.11, RF-23): liberacao por produto, categoria ou loja toda; PERCENT de 1 a 100 ou valor fixo;
// encerrar tira do PDV. A escolha no pedido continua explicita (C-08).
import { useState, type FormEvent } from 'react';
import type { z } from 'zod';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ClosePromotionResponse, CreatePromotionBody, CreatePromotionResponse, ListCategoriesResponse, ListProductsResponse, ListPromotionsResponse,
} from '../../../../lib/api/generated/sbi.zod.ts';
import { api } from '../../../../lib/api/http.ts';
import { formatDateTime, money, parseDecimal } from '../../../../lib/format.ts';
import { labeledProblems } from '../../../../lib/forms.ts';
import { ErrorNotice, Loading } from '../../../../components/feedback.tsx';
import { Field } from '../../../../components/customer-form.tsx';
import ConfirmDialog from '../../../../components/confirm-dialog.tsx';
import Drawer from '../../../../components/drawer.tsx';
import DiscountPolicies from './discount-policies.tsx';
import Pager from '../../../../components/pager.tsx';
import { StatusMessage, useStatusMessage } from '../../../../components/status-message.tsx';

type Promotion = z.infer<typeof ListPromotionsResponse>['content'][number];

const PROMOTION_FIELDS: Readonly<Record<string, string>> = {
  name: 'Nome', kind: 'Tipo', value: 'Valor', productId: 'Produto', categoryId: 'Categoria', startsAt: 'Início', endsAt: 'Fim',
};

/** datetime-local (hora da loja) -> ISO com offset de Fortaleza (-03:00). */
function storeInstant(value: string): string {
  return `${value.length === 16 ? `${value}:00` : value}-03:00`;
}

export default function Promotions() {
  const client = useQueryClient();
  const [active, setActive] = useState<'true' | 'false' | ''>('true');
  const [page, setPage] = useState(0);
  const [open, setOpen] = useState(false);
  const [closing, setClosing] = useState<Promotion | null>(null);
  const message = useStatusMessage();
  const list = useQuery({
    queryKey: ['promotions', active, page],
    queryFn: ({ signal }) => {
      const params = new URLSearchParams({ page: String(page), size: '20' });
      if (active) params.set('active', active);
      return api.request(`/promotions?${params}`, { signal, schema: ListPromotionsResponse });
    },
    placeholderData: keepPreviousData,
  });
  const close = useMutation({
    mutationFn: (promotion: Promotion) => api.request(`/promotions/${promotion.id}/close`, { method: 'POST', schema: ClosePromotionResponse }),
    onSuccess: (_, promotion) => {
      message.show(`Promoção “${promotion.name}” encerrada. Ela não aparece mais no PDV.`);
      return client.invalidateQueries({ queryKey: ['promotions'] });
    },
  });
  const data = list.data;
  return (
    <div className="stack">
      <header className="toolbar">
        <div className="stack-sm"><p className="muted">Gestão / Promoções</p><h1>Promoções</h1></div>
        <button type="button" className="btn btn-primary" onClick={() => setOpen(true)}>Liberar promoção</button>
      </header>
      <StatusMessage message={message.message} onClose={message.clear} />
      <section className="card stack" aria-label="Lista de promoções">
        <fieldset className="segmented"><legend className="sr-only">Situação</legend>
          {([['true', 'Ativas'], ['false', 'Encerradas'], ['', 'Todas']] as const).map(([value, text]) => (
            <label key={text}><input type="radio" name="situacao" checked={active === value} onChange={() => { setActive(value); setPage(0); }} /><span>{text}</span></label>
          ))}
        </fieldset>
        <ErrorNotice error={list.error ?? close.error} />
        {list.isLoading ? <Loading /> : data && data.content.length === 0
          ? <p className="empty">{active === 'true' ? 'Nenhuma promoção ativa. Use “Liberar promoção” para criar uma.' : 'Nenhuma promoção nesta lista.'}</p>
          : data && <div className="table-wrap"><table>
              <caption className="sr-only">Promoções</caption>
              <thead><tr><th scope="col">Promoção</th><th scope="col">Desconto</th><th scope="col">Vale para</th><th scope="col">Vigência</th>
                <th scope="col"><span className="sr-only">Ações</span></th></tr></thead>
              <tbody>{data.content.map(p => (
                <tr key={p.id}>
                  <td>{p.name}<br /><span className="muted">por {p.createdBy.name}</span></td>
                  <td className="num">{p.kind === 'PERCENT' ? `${p.value.toLocaleString('pt-BR')}%` : money(p.value)}</td>
                  <td>{p.product ? p.product.name : p.category ? `Categoria ${p.category.name}` : 'Loja toda'}</td>
                  <td className="num">{formatDateTime(p.startsAt)} a {formatDateTime(p.endsAt)}</td>
                  <td>{p.active ? <button type="button" className="btn btn-danger" disabled={close.isPending} aria-label={`Encerrar ${p.name}`}
                                          onClick={() => setClosing(p)}>Encerrar</button>
                    : <span className="pill">Encerrada</span>}</td>
                </tr>
              ))}</tbody>
            </table></div>}
        {data && <Pager page={data.page} size={data.size} total={data.totalElements} onPage={setPage} />}
      </section>
      <DiscountPolicies onSaved={message.show} />
      <Drawer open={open} title="Liberar promoção" onClose={() => setOpen(false)}>
        <PromotionForm onDone={name => {
          setOpen(false);
          message.show(`Promoção “${name}” liberada. Ela aparece no PDV durante a vigência.`);
          void client.invalidateQueries({ queryKey: ['promotions'] });
        }} />
      </Drawer>
      <ConfirmDialog open={closing !== null} title={`Encerrar a promoção “${closing?.name ?? ''}”?`} confirmLabel="Encerrar promoção"
                     cancelLabel="Manter ativa" onCancel={() => setClosing(null)}
                     onConfirm={() => { if (closing) close.mutate(closing); setClosing(null); }}>
        <p>Ela sai do PDV agora e não pode ser reaberta. Vendas já feitas mantêm o desconto. Para voltar a oferecer, libere uma nova promoção.</p>
      </ConfirmDialog>
    </div>
  );
}

function PromotionForm({ onDone }: { onDone: (name: string) => void }) {
  const [scope, setScope] = useState<'store' | 'product' | 'category'>('product');
  const [problems, setProblems] = useState<string[]>([]);
  const products = useQuery({ queryKey: ['promo-products'], queryFn: ({ signal }) => api.request('/products?sellable=true&active=true&size=100', { signal, schema: ListProductsResponse }) });
  const categories = useQuery({ queryKey: ['categories'], queryFn: ({ signal }) => api.request('/categories', { signal, schema: ListCategoriesResponse }) });
  const create = useMutation({
    mutationFn: (body: unknown) => api.request('/promotions', { method: 'POST', body, schema: CreatePromotionResponse }),
    onSuccess: created => onDone(created.name),
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const text = (name: string) => String(form.get(name) ?? '').trim();
    const body = {
      name: text('name'), kind: text('kind'), value: parseDecimal(text('value')),
      ...(scope === 'product' && text('productId') ? { productId: Number(text('productId')) } : {}),
      ...(scope === 'category' && text('categoryId') ? { categoryId: Number(text('categoryId')) } : {}),
      startsAt: text('startsAt') ? storeInstant(text('startsAt')) : '', endsAt: text('endsAt') ? storeInstant(text('endsAt')) : '',
    };
    const check = CreatePromotionBody.safeParse(body);
    const local: string[] = check.success ? [] : labeledProblems(check.error.issues, PROMOTION_FIELDS);
    if (body.kind === 'PERCENT' && (body.value < 1 || body.value > 100)) local.push('Percentual deve ficar entre 1 e 100.');
    if (body.startsAt && body.endsAt && body.endsAt <= body.startsAt) local.push('O fim precisa ser depois do início.');
    setProblems(local);
    if (local.length === 0) create.mutate(body);
  }
  return (
    <form className="stack" onSubmit={submit} aria-busy={create.isPending}>
      <div className="form-grid">
        <Field id="promo-nome" label="Nome"><input id="promo-nome" name="name" className="input" minLength={3} maxLength={120} required /></Field>
        <Field id="promo-tipo" label="Tipo"><select id="promo-tipo" name="kind" className="select" defaultValue="PERCENT">
          <option value="PERCENT">Percentual</option><option value="AMOUNT">Valor por unidade</option></select></Field>
        <Field id="promo-valor" label="Valor"><input id="promo-valor" name="value" className="input" inputMode="decimal" required /></Field>
        <Field id="promo-inicio" label="Início"><input id="promo-inicio" name="startsAt" className="input" type="datetime-local" required /></Field>
        <Field id="promo-fim" label="Fim"><input id="promo-fim" name="endsAt" className="input" type="datetime-local" required /></Field>
      </div>
      <fieldset className="segmented"><legend className="sr-only">Vale para</legend>
        {([['product', 'Um produto'], ['category', 'Uma categoria'], ['store', 'Loja toda']] as const).map(([value, text]) => (
          <label key={value}><input type="radio" name="escopo" checked={scope === value} onChange={() => setScope(value)} /><span>{text}</span></label>
        ))}
      </fieldset>
      {scope === 'product' && <Field id="promo-produto" label="Produto"><select id="promo-produto" name="productId" className="select" required>
        {products.data?.content.map(p => <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>)}</select></Field>}
      {scope === 'category' && <Field id="promo-categoria" label="Categoria"><select id="promo-categoria" name="categoryId" className="select" required>
        {categories.data?.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></Field>}
      {problems.length > 0 && <div className="notice notice-error" role="alert">{problems.map(p => <p key={p}>{p}</p>)}</div>}
      <ErrorNotice error={create.error} />
      <div className="actions"><button type="submit" className="btn btn-primary" disabled={create.isPending}>{create.isPending ? 'Salvando…' : 'Liberar'}</button></div>
    </form>
  );
}
