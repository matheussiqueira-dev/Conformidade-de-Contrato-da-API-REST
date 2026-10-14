'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useForm } from 'react-hook-form';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { z } from 'zod';
import { GetCustomerResponse, ListCustomerOrdersResponse, UpdateCustomerBody, UpdateCustomerResponse } from '../../../../lib/api/generated/sbi.zod.ts';
import { ApiError, api } from '../../../../lib/api/http.ts';
import { formatDate, formatDateTime, label, money, ORDER_STATUS } from '../../../../lib/format.ts';
import { contractResolver, optional } from '../../../../lib/forms.ts';
import { ErrorNotice, Loading, errorMessage } from '../../../../components/feedback.tsx';
import { Field } from '../../../../components/customer-form.tsx';
import Pager from '../../../../components/pager.tsx';

type Customer = z.infer<typeof GetCustomerResponse>;

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
  const query = useQuery({
    queryKey: ['customer', customerId],
    queryFn: ({ signal }) => api.request(`/customers/${customerId}`, { signal, schema: GetCustomerResponse }),
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

  return (
    <div className="stack">
      <header className="page-head">
        <div className="stack-sm">
          <p><Link href="/clientes">Clientes</Link> / {customer.name}</p>
          <h1>{customer.name}</h1>
          <p className="mono">{customer.document}</p>
        </div>
        {!editing && <button type="button" className="btn" onClick={() => setEditing(true)}>Editar dados</button>}
      </header>

      <section className="card stack" aria-labelledby="dados-titulo">
        <h2 id="dados-titulo">Dados do cliente</h2>
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
