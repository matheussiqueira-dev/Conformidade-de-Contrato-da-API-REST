'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { z } from 'zod';
import {
  GetCustomerResponse, GetCustomerSummaryResponse, ListCustomerOrdersResponse, UpdateCustomerBody, UpdateCustomerResponse,
} from '../../../../lib/api/generated/sbi.zod.ts';
import { ApiError, api } from '../../../../lib/api/http.ts';
import { daysSince, formatDate, formatDateTime, label, money, ORDER_STATUS, PAYMENT_TYPE } from '../../../../lib/format.ts';
import { contractResolver, optional } from '../../../../lib/forms.ts';
import { ErrorNotice, Loading, errorMessage } from '../../../../components/feedback.tsx';
import { Field } from '../../../../components/customer-form.tsx';
import Pager from '../../../../components/pager.tsx';
import Tabs from '../../../../components/tabs.tsx';

type Customer = z.infer<typeof GetCustomerResponse>;
type Summary = z.infer<typeof GetCustomerSummaryResponse>;

interface EditValues { name: string; companyName: string; email: string; phone: string; birthDate: string;
  street: string; number: string; complement: string; neighborhood: string; city: string; state: string; zipCode: string }

function toBody(values: EditValues, customer: Customer): unknown {
  const birthDate = optional(values.birthDate);
  const companyName = optional(values.companyName);
  const street = optional(values.street);
  const address = street ? {
    street, number: values.number.trim(), neighborhood: values.neighborhood.trim(), city: values.city.trim(),
    state: values.state.trim().toUpperCase(), zipCode: values.zipCode.replace(/\D/g, ''),
    ...(optional(values.complement) ? { complement: values.complement.trim() } : {}),
  } : undefined;
  return {
    name: values.name.trim(), email: values.email.trim(), phone: values.phone.trim(), version: customer.version,
    ...(customer.type === 'CORPORATE' && companyName ? { companyName } : {}),
    ...(customer.type === 'INDIVIDUAL' && birthDate ? { birthDate } : {}),
    ...(address ? { address } : {}),
  };
}

/** Perfil com documento mascarado para o VENDEDOR; edicao sem documento, com versao (CT-CRM-104). */
export default function CustomerProfile({ customerId, manager }: { customerId: number | null; manager: boolean }) {
  const client = useQueryClient();
  const [editing, setEditing] = useState(false);
  const [page, setPage] = useState(0);
  const [tab, setTab] = useState('pedidos');
  const query = useQuery({
    queryKey: ['customer', customerId],
    queryFn: ({ signal }) => api.request(`/customers/${customerId}`, { signal, schema: GetCustomerResponse }),
    enabled: customerId !== null,
  });
  const summary = useQuery({
    queryKey: ['customer-summary', customerId],
    queryFn: ({ signal }) => api.request(`/customers/${customerId}/summary`, { signal, schema: GetCustomerSummaryResponse }),
    enabled: customerId !== null,
  });
  const history = useQuery({
    queryKey: ['customer-orders', customerId, page],
    queryFn: ({ signal }) => api.request(`/customers/${customerId}/orders?${new URLSearchParams({ page: String(page), size: '10' })}`,
      { signal, schema: ListCustomerOrdersResponse }),
    enabled: customerId !== null,
    placeholderData: keepPreviousData,
  });

  if (customerId === null || (query.error instanceof ApiError && query.error.status === 404)) {
    return <section className="card stack-sm"><h1>Cliente não encontrado</h1><p><Link href="/clientes">Voltar para clientes</Link></p></section>;
  }
  if (query.isLoading) return <Loading label="Carregando cliente…" />;
  if (query.error || !query.data) return <ErrorNotice error={query.error} />;
  const customer = query.data;

  const orders = (
    <section className="card stack-sm" aria-labelledby="historico-titulo">
      <h2 id="historico-titulo">Histórico de pedidos</h2>
      <ErrorNotice error={history.error} />
      {history.isLoading ? <Loading label="Carregando histórico…" /> : history.data && history.data.content.length === 0
        ? <p className="empty">Este cliente ainda não tem pedidos.</p>
        : history.data && <div className="table-wrap"><table>
            <caption className="sr-only">Pedidos do cliente, do mais recente ao mais antigo</caption>
            <thead><tr><th scope="col">Pedido</th><th scope="col">Data</th><th scope="col">Itens</th><th scope="col">Situação</th>
              <th scope="col">Vendedor</th><th scope="col" className="right">Total</th></tr></thead>
            <tbody>{history.data.content.map(order => {
              // DEC-ERP-02: total, vendedor e link so nos pedidos proprios ou para o GERENTE.
              const visible = manager || order.own;
              return (
                <tr key={order.id}>
                  <td>{visible ? <Link className="mono" href={`/vendas/${order.id}`}>#{order.id}</Link> : <span className="mono">#{order.id}</span>}</td>
                  <td className="num">{formatDateTime(order.moment)}</td>
                  <td>{order.items.map(item => `${item.quantity}× ${item.name}`).join(', ')}</td>
                  <td>{label(ORDER_STATUS, order.status)}</td>
                  <td>{visible && order.sellerFirstName ? order.sellerFirstName : '—'}</td>
                  <td className="num right">{visible && order.total ? money(order.total) : '—'}</td>
                </tr>
              );
            })}</tbody>
          </table></div>}
      {history.data && <Pager page={history.data.page} size={history.data.size} total={history.data.totalElements} onPage={setPage} />}
    </section>
  );

  const details = (
    <section className="card stack" aria-labelledby="dados-titulo">
      <div className="page-head">
        <h2 id="dados-titulo">Dados cadastrais</h2>
        {!editing && <button type="button" className="btn" onClick={() => setEditing(true)}>Editar dados</button>}
      </div>
      {editing
        ? <EditForm customer={customer} onCancel={() => setEditing(false)}
                    onSaved={saved => { client.setQueryData(['customer', customerId], saved); setEditing(false); }}
                    onReload={() => { void query.refetch(); setEditing(false); }} />
        : <dl className="facts">
            <div><dt>Tipo</dt><dd>{customer.type === 'CORPORATE' ? 'Pessoa jurídica' : 'Pessoa física'}</dd></div>
            {customer.companyName && <div><dt>Razão social</dt><dd>{customer.companyName}</dd></div>}
            <div><dt>E-mail</dt><dd>{customer.email ?? '—'}</dd></div>
            <div><dt>Telefone</dt><dd>{customer.phone ?? '—'}</dd></div>
            {customer.birthDate && <div><dt>Nascimento</dt><dd className="num">{formatDate(customer.birthDate)}</dd></div>}
            <div><dt>Endereço</dt><dd>{customer.address
              ? `${customer.address.street}, ${customer.address.number} · ${customer.address.neighborhood} · ${customer.address.city}/${customer.address.state}`
              : '—'}</dd></div>
            <div><dt>Cadastrado em</dt><dd className="num">{formatDateTime(customer.createdAt)}{customer.createdBy ? ` por ${customer.createdBy.name}` : ''}</dd></div>
          </dl>}
    </section>
  );

  return (
    <div className="stack">
      <header className="page-head">
        <div className="stack-sm">
          <p><Link href="/clientes">Clientes</Link> / {customer.name}</p>
          <h1>{customer.name}</h1>
          <p><span className="pill">{customer.type === 'CORPORATE' ? 'PJ' : 'PF'}</span> <span className="mono">{customer.document}</span>
            {customer.phone && <> · {customer.phone}</>} · <span className="muted">cliente desde {formatDate(customer.createdAt.slice(0, 10))}</span></p>
        </div>
        <Link className="btn btn-primary" href={`/vender?cliente=${customer.id}`}>Nova venda para este cliente</Link>
      </header>

      <SummaryStrip summary={summary.data} error={summary.error} loading={summary.isLoading} />

      <Tabs label="Seções do cliente" active={tab} onChange={setTab} tabs={[
        { id: 'pedidos', label: 'Pedidos', content: orders },
        { id: 'produtos', label: 'Produtos mais comprados', content: <TopProducts summary={summary.data} /> },
        { id: 'dados', label: 'Dados cadastrais', content: details },
      ]} />
    </div>
  );
}

/** Faixa de indicadores (G-05): para o VENDEDOR, valores so das vendas dele (scope OWN). */
function SummaryStrip({ summary, error, loading }: { summary: Summary | undefined; error: unknown; loading: boolean }) {
  if (loading) return <Loading label="Carregando indicadores…" />;
  if (!summary) return <ErrorNotice error={error} />;
  const own = summary.scope === 'OWN';
  return (
    <section className="stack-sm" aria-labelledby="indicadores-titulo">
      <h2 id="indicadores-titulo" className="sr-only">Indicadores do cliente</h2>
      {own && <p className="muted">Suas vendas para este cliente</p>}
      <div className="kpis">
        <div className="card kpi"><span className="label">{own ? 'Total das suas vendas' : 'Total comprado'}</span>
          <span className="value">{money(summary.totalSpent)}</span>
          <span className="note">{summary.refundsTotal && summary.refundsTotal.amount > 0 ? `Já descontados ${money(summary.refundsTotal)} em estornos` : 'Pagamentos confirmados menos estornos'}</span></div>
        <div className="card kpi"><span className="label">Ticket médio</span><span className="value">{money(summary.averageTicket)}</span>
          <span className="note">{own ? 'Nos seus pedidos pagos' : 'Por pedido pago'}</span></div>
        <div className="card kpi"><span className="label">Pedidos pagos</span><span className="value">{summary.ordersPaid}</span>
          <span className="note">{summary.pendingOrders} aguardando pagamento · {summary.canceledOrders} cancelados</span></div>
        <div className="card kpi"><span className="label">Última compra</span>
          <span className="value">{summary.lastPurchaseAt ? daysSince(summary.lastPurchaseAt) : '—'}</span>
          <span className="note">{summary.lastPurchaseAt ? formatDateTime(summary.lastPurchaseAt) : 'Nenhuma compra paga ainda'}</span></div>
      </div>
    </section>
  );
}

function TopProducts({ summary }: { summary: Summary | undefined }) {
  if (!summary) return <Loading label="Carregando produtos…" />;
  return (
    <div className="grid-2">
      <section className="card stack-sm" aria-labelledby="produtos-titulo">
        <h2 id="produtos-titulo">Produtos mais comprados</h2>
        {summary.topProducts.length === 0 ? <p className="empty">Nenhuma compra paga ainda.</p>
          : <ol className="rank">{summary.topProducts.map(product => (
              <li key={product.productId}>{product.name} <span className="muted">· {product.quantity} {product.quantity === 1 ? 'unidade' : 'unidades'}</span></li>
            ))}</ol>}
      </section>
      <section className="card stack-sm" aria-labelledby="formas-titulo">
        <h2 id="formas-titulo">{summary.scope === 'OWN' ? 'Formas de pagamento nas suas vendas' : 'Formas de pagamento'}</h2>
        {summary.paymentMix.length === 0 ? <p className="empty">Nenhum pagamento confirmado.</p>
          : <dl className="facts">{summary.paymentMix.map(mix => (
              <div key={mix.method}><dt>{label(PAYMENT_TYPE, mix.method)}</dt><dd className="num">{money(mix.amount)} · {mix.orders} {mix.orders === 1 ? 'pedido' : 'pedidos'}</dd></div>
            ))}</dl>}
      </section>
    </div>
  );
}

function EditForm({ customer, onSaved, onCancel, onReload }: {
  customer: Customer; onSaved: (customer: Customer) => void; onCancel: () => void; onReload: () => void;
}) {
  const { register, handleSubmit, formState: { errors } } = useForm<EditValues>({
    defaultValues: {
      name: customer.name, companyName: customer.companyName ?? '', email: customer.email ?? '', phone: customer.phone ?? '',
      birthDate: customer.birthDate ?? '', street: customer.address?.street ?? '', number: customer.address?.number ?? '',
      complement: customer.address?.complement ?? '', neighborhood: customer.address?.neighborhood ?? '',
      city: customer.address?.city ?? '', state: customer.address?.state ?? '', zipCode: customer.address?.zipCode ?? '',
    },
    resolver: contractResolver<EditValues>(UpdateCustomerBody, values => toBody(values, customer)),
  });
  const save = useMutation({
    mutationFn: (values: EditValues) => api.request(`/customers/${customer.id}`, { method: 'PUT', body: toBody(values, customer), schema: UpdateCustomerResponse }),
    onSuccess: onSaved,
  });
  const conflict = save.error instanceof ApiError && save.error.code === 'CONCURRENT_UPDATE';
  const err = (path: string) => {
    const parts = path.split('.');
    let node: unknown = errors;
    for (const part of parts) node = (node as Record<string, unknown> | undefined)?.[part];
    return (node as { message?: string } | undefined)?.message;
  };
  const text = (name: keyof EditValues, title: string, extra: Record<string, unknown> = {}) => (
    <Field id={`editar-${name}`} label={title} error={err(name) ?? err(`address.${name}`)}>
      <input id={`editar-${name}`} className="input" {...extra} {...register(name)} />
    </Field>
  );

  return (
    <form className="stack" onSubmit={handleSubmit(values => save.mutate(values))} noValidate aria-busy={save.isPending}>
      <p className="muted">O documento não pode ser alterado.</p>
      <div className="form-grid">
        {text('name', customer.type === 'CORPORATE' ? 'Nome fantasia' : 'Nome')}
        {customer.type === 'CORPORATE' && text('companyName', 'Razão social')}
        {text('email', 'E-mail', { type: 'email', autoComplete: 'email' })}
        {text('phone', 'Telefone', { type: 'tel', autoComplete: 'tel' })}
        {customer.type === 'INDIVIDUAL' && text('birthDate', 'Data de nascimento', { type: 'date' })}
      </div>
      <fieldset className="stack-sm plain-fieldset">
        <legend className="label">Endereço (opcional)</legend>
        <div className="form-grid">
          {text('street', 'Rua')}{text('number', 'Número')}{text('complement', 'Complemento')}{text('neighborhood', 'Bairro')}
          {text('city', 'Cidade')}{text('state', 'UF', { maxLength: 2 })}{text('zipCode', 'CEP', { inputMode: 'numeric' })}
        </div>
      </fieldset>
      {save.error && <div className="notice notice-error" role="alert">
        <p>{conflict ? 'Os dados mudaram; recarregue.' : errorMessage(save.error)}</p>
        {conflict && <button type="button" className="btn" onClick={onReload}>Recarregar</button>}
      </div>}
      <div className="actions">
        <button type="submit" className="btn btn-primary" disabled={save.isPending}>{save.isPending ? 'Salvando…' : 'Salvar alterações'}</button>
        <button type="button" className="btn" onClick={onCancel} disabled={save.isPending}>Cancelar</button>
      </div>
    </form>
  );
}
