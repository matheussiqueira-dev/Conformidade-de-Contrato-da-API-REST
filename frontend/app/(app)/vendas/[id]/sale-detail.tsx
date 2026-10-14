'use client';
import Link from 'next/link';
import { useState, type FormEvent } from 'react';
import type { z } from 'zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  ApproveOrderDiscountBody, ApproveOrderDiscountResponse, CancelOrderResponse, ConfirmSalePaymentResponse, GetSalesOrderResponse,
} from '../../../../lib/api/generated/sbi.zod.ts';
import { ApiError, api } from '../../../../lib/api/http.ts';
import { formatDateTime, label, money, ORDER_STATUS, statusPill, PAYMENT_TYPE } from '../../../../lib/format.ts';
import { ErrorNotice, Loading } from '../../../../components/feedback.tsx';
import { Field } from '../../../../components/customer-form.tsx';

function NotFound() {
  return (
    <section className="card stack-sm">
      <h1>Pedido não encontrado</h1>
      <p className="muted">Ele não existe ou pertence a outro vendedor.</p>
      <p><Link href="/vendas">Voltar para vendas</Link></p>
    </section>
  );
}

/** Pedido de outro vendedor responde 404 na API e aparece como "Pedido nao encontrado" (WP-3.6). */
export default function SaleDetail({ orderId, manager = false }: { orderId: number | null; manager?: boolean }) {
  const client = useQueryClient();
  const key = ['sale', orderId];
  const query = useQuery({
    queryKey: key,
    queryFn: ({ signal }) => api.request(`/sales/orders/${orderId}`, { signal, schema: GetSalesOrderResponse }),
    enabled: orderId !== null,
  });
  const confirm = useMutation({
    mutationFn: () => api.request(`/sales/orders/${orderId}/payment`, { method: 'POST', schema: ConfirmSalePaymentResponse }),
    onSuccess: order => client.setQueryData(key, order),
  });
  const cancel = useMutation({
    mutationFn: () => api.request(`/orders/${orderId}/cancel`, { method: 'POST', schema: CancelOrderResponse }),
    onSuccess: order => client.setQueryData(key, order),
  });

  if (orderId === null || (query.error instanceof ApiError && query.error.status === 404)) return <NotFound />;
  if (query.isLoading) return <Loading label="Carregando venda…" />;
  if (query.error || !query.data) return <ErrorNotice error={query.error} />;
  const order = query.data;
  const pending = order.status === 'PENDING_PAYMENT';
  const busy = confirm.isPending || cancel.isPending;
  return (
    <div className="stack">
      <header className="page-head">
        <div className="stack-sm">
          <p><Link href="/vendas">Vendas</Link> / <span className="mono">#{order.id}</span></p>
          <h1>Venda <span className="mono">#{order.id}</span></h1>
        </div>
        <span className={statusPill(order.status)}>{label(ORDER_STATUS, order.status)}</span>
      </header>
      <section className="card stack">
        <dl className="facts">
          <div><dt>Data</dt><dd className="num">{formatDateTime(order.createdAt)}</dd></div>
          <div><dt>Cliente</dt><dd>{order.customer.name}</dd></div>
          <div><dt>Vendedor</dt><dd>{order.seller.name}</dd></div>
          <div><dt>Pagamento</dt><dd>{label(PAYMENT_TYPE, order.payment.type)}</dd></div>
          <div><dt>Entrega</dt><dd>{order.fulfillment === 'DELIVERY' && order.shippingAddress
            ? `${order.shippingAddress.street}, ${order.shippingAddress.number} · ${order.shippingAddress.city}/${order.shippingAddress.state}`
            : 'Retirada na loja'}</dd></div>
        </dl>
        <div className="table-wrap"><table>
          <caption className="sr-only">Itens da venda</caption>
          <thead><tr><th scope="col">Produto</th><th scope="col" className="right">Qtd.</th><th scope="col" className="right">Unitário</th>
            <th scope="col" className="right">Desconto</th><th scope="col" className="right">Subtotal</th></tr></thead>
          <tbody>{order.items.map(item => (
            <tr key={item.productId}>
              <td>{item.name}<br /><span className="mono muted">{item.sku}</span></td>
              <td className="num right">{item.quantity}</td>
              <td className="num right">{money(item.unitPrice)}</td>
              <td className="num right">{money(item.discount)}</td>
              <td className="num right">{money(item.subtotal)}</td>
            </tr>
          ))}</tbody>
        </table></div>
        <dl className="totals">
          <div><dt>Subtotal</dt><dd className="num">{money(order.subtotal)}</dd></div>
          <div><dt>Descontos</dt><dd className="num">{money(order.discountTotal)}</dd></div>
          <div className="total"><dt>Total</dt><dd className="num">{money(order.total)}</dd></div>
        </dl>
        {order.manualDiscount && <p className="muted">
          Desconto manual de {order.manualDiscount.percent.toString().replace('.', ',')}% ({money(order.manualDiscount.amount)}), já incluído nos itens.
          Motivo: {order.manualDiscount.reason}.{order.manualDiscount.approvedBy ? ` Aprovado por ${order.manualDiscount.approvedBy.name}.` : ''}</p>}
        <ErrorNotice error={confirm.error ?? cancel.error} />
        {pending && <div className="actions">
          <button type="button" className="btn btn-primary" disabled={busy} onClick={() => confirm.mutate()}>
            {confirm.isPending ? 'Confirmando…' : 'Confirmar pagamento recebido'}</button>
          <button type="button" className="btn btn-danger" disabled={busy} onClick={() => cancel.mutate()}>
            {cancel.isPending ? 'Cancelando…' : 'Cancelar pedido'}</button>
        </div>}
      </section>
      {manager && pending && <DiscountApproval orderId={order.id} onDone={updated => client.setQueryData(key, updated)} />}
    </div>
  );
}

/** G-04: o GERENTE aplica no pedido pendente o desconto que passou da alcada do vendedor. */
function DiscountApproval({ orderId, onDone }: { orderId: number; onDone: (order: z.infer<typeof ApproveOrderDiscountResponse>) => void }) {
  const [problem, setProblem] = useState('');
  const approve = useMutation({
    mutationFn: (body: unknown) => api.request(`/sales/orders/${orderId}/discount-approval`, { method: 'POST', body, schema: ApproveOrderDiscountResponse }),
    onSuccess: onDone,
  });
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const body = { percent: Number(String(form.get('percent') ?? '').replace(',', '.')), reason: String(form.get('reason') ?? '').trim() };
    const check = ApproveOrderDiscountBody.safeParse(body);
    setProblem(check.success ? '' : 'Informe um percentual de 0,01 a 100 com até 2 casas e um motivo de 5 a 200 caracteres.');
    if (check.success) approve.mutate(body);
  }
  return (
    <section className="card stack-sm" aria-labelledby="aprovar-desconto">
      <h2 id="aprovar-desconto">Aprovar desconto (gerente)</h2>
      <p className="muted">Substitui o desconto manual do pedido; o total e o valor do pagamento são recalculados.</p>
      <form className="stack-sm" onSubmit={submit} aria-busy={approve.isPending}>
        <div className="form-grid">
          <Field id="aprovar-percentual" label="Percentual (%)"><input id="aprovar-percentual" name="percent" className="input" inputMode="decimal" required /></Field>
          <Field id="aprovar-motivo" label="Motivo" hint="De 5 a 200 caracteres"><input id="aprovar-motivo" name="reason" className="input" required minLength={5} maxLength={200} /></Field>
        </div>
        {problem && <p className="notice notice-error" role="alert">{problem}</p>}
        <ErrorNotice error={approve.error} />
        <div className="actions"><button type="submit" className="btn btn-primary" disabled={approve.isPending}>{approve.isPending ? 'Aplicando…' : 'Aplicar desconto'}</button></div>
      </form>
    </section>
  );
}
