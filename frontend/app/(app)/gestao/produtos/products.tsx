'use client';
// Produtos e insumos (WP-3.10): preco so pelo PATCH /price com motivo (historico com autor), situacao so pelo
// PATCH /status, tipo fisico/digital no cadastro (C-17), SKU em maiusculas e unico (C-16), categorias.
import { useState, type FormEvent } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { z } from 'zod';
import {
  ChangeProductPriceResponse, ChangeProductStatusResponse, CreateCategoryResponse, CreateProductBody, CreateProductResponse,
  ListCategoriesResponse, ListPriceHistoryResponse, ListProductsResponse, UpdateCategoryResponse, UpdateProductBody, UpdateProductResponse,
} from '../../../../lib/api/generated/sbi.zod.ts';
import { api } from '../../../../lib/api/http.ts';
import { formatDateTime, money, parseDecimal as decimal } from '../../../../lib/format.ts';
import { issueMessage } from '../../../../lib/forms.ts';
import { ErrorNotice, Loading, errorMessage } from '../../../../components/feedback.tsx';
import { Field } from '../../../../components/customer-form.tsx';
import Drawer from '../../../../components/drawer.tsx';
import Pager from '../../../../components/pager.tsx';

type Product = z.infer<typeof ListProductsResponse>['content'][number];
type Panel = { kind: 'new' } | { kind: 'edit' | 'price' | 'history'; product: Product } | { kind: 'categories' } | null;



export default function Products() {
  const client = useQueryClient();
  const [tab, setTab] = useState<'true' | 'false'>('true');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [panel, setPanel] = useState<Panel>(null);
  const list = useQuery({
    queryKey: ['products', tab, q, page],
    queryFn: ({ signal }) => api.request(`/products?${new URLSearchParams({ sellable: tab, q, page: String(page), size: '20' })}`,
      { signal, schema: ListProductsResponse }),
    placeholderData: keepPreviousData,
  });
  const status = useMutation({
    mutationFn: (product: Product) => api.request(`/products/${product.id}/status`, { method: 'PATCH', body: { active: !product.active }, schema: ChangeProductStatusResponse }),
    onSuccess: () => client.invalidateQueries({ queryKey: ['products'] }),
  });
  const done = () => { void client.invalidateQueries({ queryKey: ['products'] }); setPanel(null); };
  const data = list.data;
  const title = panel === null ? '' : panel.kind === 'new' ? 'Novo produto' : panel.kind === 'categories' ? 'Categorias'
    : `${panel.kind === 'edit' ? 'Editar' : panel.kind === 'price' ? 'Alterar preço' : 'Histórico de preço'} · ${panel.product.name}`;

  return (
    <div className="stack">
      <header className="toolbar">
        <div className="stack-sm"><p className="muted">Gestão / Produtos</p><h1>Produtos e insumos</h1></div>
        <div className="actions">
          <button type="button" className="btn" onClick={() => setPanel({ kind: 'categories' })}>Categorias</button>
          <button type="button" className="btn btn-primary" onClick={() => setPanel({ kind: 'new' })}>Novo produto</button>
        </div>
      </header>
      <section className="card stack" aria-label="Lista de produtos">
        <div className="toolbar">
          <fieldset className="segmented"><legend className="sr-only">Mostrar</legend>
            <label><input type="radio" name="tipo-lista" checked={tab === 'true'} onChange={() => { setTab('true'); setPage(0); }} /><span>Produtos</span></label>
            <label><input type="radio" name="tipo-lista" checked={tab === 'false'} onChange={() => { setTab('false'); setPage(0); }} /><span>Insumos</span></label>
          </fieldset>
          <div className="field"><label htmlFor="busca-produtos">Nome ou SKU</label>
            <input id="busca-produtos" className="input" type="search" value={q} onChange={event => { setQ(event.target.value); setPage(0); }} /></div>
        </div>
        <ErrorNotice error={list.error ?? status.error} />
        {list.isLoading ? <Loading label="Carregando produtos…" /> : data && data.content.length === 0
          ? <p className="empty">Nenhum item encontrado.</p>
          : data && <div className="table-wrap"><table>
              <caption className="sr-only">{tab === 'true' ? 'Produtos' : 'Insumos'}</caption>
              <thead><tr><th scope="col">Item</th><th scope="col">Categoria</th><th scope="col" className="right">Preço</th>
                <th scope="col">Situação</th><th scope="col"><span className="sr-only">Ações</span></th></tr></thead>
              <tbody>{data.content.map(p => (
                <tr key={p.id}>
                  <td>{p.name}<br /><span className="mono muted">{p.sku}</span> · <span className="muted">{p.type === 'DIGITAL' ? 'Digital' : 'Físico'}</span></td>
                  <td>{p.category?.name ?? '—'}</td>
                  <td className="num right">{money(p.price)}</td>
                  <td>{p.active ? <span className="pill pill-ok">Ativo</span> : <span className="pill">Inativo</span>}</td>
                  <td><div className="actions">
                    <button type="button" className="btn" onClick={() => setPanel({ kind: 'edit', product: p })}>Editar</button>
                    <button type="button" className="btn" onClick={() => setPanel({ kind: 'price', product: p })}>Preço</button>
                    <button type="button" className="btn btn-ghost" onClick={() => setPanel({ kind: 'history', product: p })}>Histórico</button>
                    <button type="button" className="btn btn-ghost" disabled={status.isPending} onClick={() => status.mutate(p)}>{p.active ? 'Inativar' : 'Ativar'}</button>
                  </div></td>
                </tr>
              ))}</tbody>
            </table></div>}
        {data && <Pager page={data.page} size={data.size} total={data.totalElements} onPage={setPage} />}
      </section>
      <Drawer open={panel !== null} title={title} onClose={() => setPanel(null)}>
        {panel?.kind === 'new' && <ProductForm onDone={done} />}
        {panel?.kind === 'edit' && <ProductForm product={panel.product} onDone={done} />}
        {panel?.kind === 'price' && <PriceForm product={panel.product} onDone={done} />}
        {panel?.kind === 'history' && <PriceHistory product={panel.product} />}
        {panel?.kind === 'categories' && <Categories />}
      </Drawer>
    </div>
  );
}

function useCategories() {
  return useQuery({ queryKey: ['categories'], queryFn: ({ signal }) => api.request('/categories', { signal, schema: ListCategoriesResponse }) });
}

function ProductForm({ product, onDone }: { product?: Product; onDone: () => void }) {
  const categories = useCategories();
  const [type, setType] = useState<'PHYSICAL' | 'DIGITAL'>(product?.type ?? 'PHYSICAL');
  const [problems, setProblems] = useState<string[]>([]);
  const save = useMutation({
    mutationFn: (body: unknown) => (product
      ? api.request(`/products/${product.id}`, { method: 'PUT', body, schema: UpdateProductResponse })
      : api.request('/products', { method: 'POST', body, schema: CreateProductResponse })),
    onSuccess: onDone,
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const text = (name: string) => String(form.get(name) ?? '').trim();
    const categoryId = text('categoryId') ? Number(text('categoryId')) : undefined;
    const common = { sku: text('sku'), name: text('name'), sellable: form.get('sellable') === 'on',
      ...(text('description') ? { description: text('description') } : {}), ...(categoryId ? { categoryId } : {}) };
    const specific = type === 'PHYSICAL'
      ? (text('weightKg') ? { weightKg: decimal(text('weightKg')) } : {})
      : (text('downloadLink') ? { downloadLink: text('downloadLink') } : {});
    const body = product ? { ...common, ...specific }
      : { type, ...common, ...specific, price: { amount: decimal(text('price')), currency: 'BRL' }, ...(text('minLevel') ? { minLevel: Number(text('minLevel')) } : {}) };
    const check = (product ? UpdateProductBody : CreateProductBody).safeParse(body);
    if (!check.success) {
      setProblems(check.error.issues.map(issue => `${issue.path.join('.') || 'formulário'}: ${issueMessage(issue)}`));
      return;
    }
    setProblems([]);
    save.mutate(body);
  }
  return (
    <form className="stack" onSubmit={submit} aria-busy={save.isPending}>
      {!product && <fieldset className="segmented"><legend className="sr-only">Tipo</legend>
        <label><input type="radio" name="tipo" checked={type === 'PHYSICAL'} onChange={() => setType('PHYSICAL')} /><span>Físico</span></label>
        <label><input type="radio" name="tipo" checked={type === 'DIGITAL'} onChange={() => setType('DIGITAL')} /><span>Digital</span></label>
      </fieldset>}
      <div className="form-grid">
        <Field id="prod-sku" label="SKU"><input id="prod-sku" name="sku" className="input mono" defaultValue={product?.sku} required maxLength={40} /></Field>
        <Field id="prod-nome" label="Nome"><input id="prod-nome" name="name" className="input" defaultValue={product?.name} required maxLength={120} /></Field>
        <Field id="prod-cat" label="Categoria"><select id="prod-cat" name="categoryId" className="select" defaultValue={product?.category?.id ?? ''}>
          <option value="">Sem categoria</option>{categories.data?.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></Field>
        {!product && <Field id="prod-preco" label="Preço de venda (R$)"><input id="prod-preco" name="price" className="input" inputMode="decimal" required /></Field>}
        {type === 'PHYSICAL'
          ? <Field id="prod-peso" label="Peso (kg)"><input id="prod-peso" name="weightKg" className="input" inputMode="decimal" defaultValue={product?.weightKg ?? ''} required={!product} /></Field>
          : <Field id="prod-link" label="Link de download"><input id="prod-link" name="downloadLink" className="input" type="url" defaultValue={product?.downloadLink ?? ''} required={!product} /></Field>}
        {!product && <Field id="prod-min" label="Estoque mínimo (opcional)"><input id="prod-min" name="minLevel" className="input" type="number" min={0} /></Field>}
        <Field id="prod-desc" label="Descrição (opcional)"><input id="prod-desc" name="description" className="input" defaultValue={product?.description ?? ''} maxLength={255} /></Field>
      </div>
      <label className="actions"><input type="checkbox" name="sellable" defaultChecked={product?.sellable ?? true} /> Vendável no PDV (desmarcado = insumo)</label>
      {problems.length > 0 && <div className="notice notice-error" role="alert">{problems.map(p => <p key={p}>{p}</p>)}</div>}
      <ErrorNotice error={save.error} />
      <div className="actions"><button type="submit" className="btn btn-primary" disabled={save.isPending}>{save.isPending ? 'Salvando…' : 'Salvar'}</button></div>
    </form>
  );
}

function PriceForm({ product, onDone }: { product: Product; onDone: () => void }) {
  const change = useMutation({
    mutationFn: (body: unknown) => api.request(`/products/${product.id}/price`, { method: 'PATCH', body, schema: ChangeProductPriceResponse }),
    onSuccess: onDone,
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    change.mutate({ newPrice: { amount: decimal(String(form.get('price') ?? '')), currency: 'BRL' }, reason: String(form.get('reason') ?? '').trim() });
  }
  return (
    <form className="stack" onSubmit={submit} aria-busy={change.isPending}>
      <p className="muted">Preço atual {money(product.price)}. Vendas já feitas mantêm o preço antigo.</p>
      <div className="form-grid">
        <Field id="novo-preco" label="Novo preço (R$)"><input id="novo-preco" name="price" className="input" inputMode="decimal" required /></Field>
        <Field id="motivo-preco" label="Motivo" hint="De 5 a 200 caracteres"><input id="motivo-preco" name="reason" className="input" minLength={5} maxLength={200} required /></Field>
      </div>
      {change.error && <p className="notice notice-error" role="alert">{errorMessage(change.error)}</p>}
      <div className="actions"><button type="submit" className="btn btn-primary" disabled={change.isPending}>{change.isPending ? 'Salvando…' : 'Alterar preço'}</button></div>
    </form>
  );
}

function PriceHistory({ product }: { product: Product }) {
  const history = useQuery({
    queryKey: ['price-history', product.id],
    queryFn: ({ signal }) => api.request(`/products/${product.id}/price-history?size=50`, { signal, schema: ListPriceHistoryResponse }),
  });
  if (history.isLoading) return <Loading />;
  if (history.error || !history.data) return <ErrorNotice error={history.error} />;
  return history.data.content.length === 0 ? <p className="empty">Sem alterações de preço.</p> : (
    <div className="table-wrap"><table>
      <caption className="sr-only">Histórico de preço</caption>
      <thead><tr><th scope="col">Quando</th><th scope="col" className="right">De</th><th scope="col" className="right">Para</th><th scope="col">Motivo</th><th scope="col">Por</th></tr></thead>
      <tbody>{history.data.content.map(h => (
        <tr key={h.id}><td className="num">{formatDateTime(h.changedAt)}</td><td className="num right">{money(h.oldPrice)}</td>
          <td className="num right">{money(h.newPrice)}</td><td>{h.reason ?? '—'}</td><td>{h.changedBy?.name ?? '—'}</td></tr>
      ))}</tbody>
    </table></div>
  );
}

function Categories() {
  const client = useQueryClient();
  const categories = useCategories();
  const [editing, setEditing] = useState<number | null>(null);
  const save = useMutation({
    mutationFn: ({ id, name }: { id: number | null; name: string }) => (id === null
      ? api.request('/categories', { method: 'POST', body: { name }, schema: CreateCategoryResponse })
      : api.request(`/categories/${id}`, { method: 'PUT', body: { name }, schema: UpdateCategoryResponse })),
    onSuccess: () => { setEditing(null); void client.invalidateQueries({ queryKey: ['categories'] }); },
  });
  function submit(event: FormEvent<HTMLFormElement>, id: number | null) {
    event.preventDefault();
    const form = event.currentTarget;
    save.mutate({ id, name: String(new FormData(form).get('name') ?? '').trim() }, { onSuccess: () => form.reset() });
  }
  return (
    <div className="stack">
      <form className="actions" onSubmit={event => submit(event, null)}>
        <Field id="nova-categoria" label="Nova categoria"><input id="nova-categoria" name="name" className="input" minLength={2} maxLength={80} required /></Field>
        <button type="submit" className="btn btn-primary" disabled={save.isPending}>Criar</button>
      </form>
      <ErrorNotice error={categories.error ?? save.error} />
      <ul className="stack-sm">{categories.data?.map(c => (
        <li key={c.id}>{editing === c.id
          ? <form className="actions" onSubmit={event => submit(event, c.id)}>
              <label htmlFor={`cat-${c.id}`} className="sr-only">Nome da categoria</label>
              <input id={`cat-${c.id}`} name="name" className="input" defaultValue={c.name} minLength={2} maxLength={80} required />
              <button type="submit" className="btn">Salvar</button></form>
          : <span className="actions">{c.name}<button type="button" className="btn btn-ghost" onClick={() => setEditing(c.id)}>Renomear</button></span>}
        </li>
      ))}</ul>
    </div>
  );
}
