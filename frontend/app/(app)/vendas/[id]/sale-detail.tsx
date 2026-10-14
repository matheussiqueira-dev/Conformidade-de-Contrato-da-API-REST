'use client';
import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CancelOrderResponse, ConfirmSalePaymentResponse, GetSalesOrderResponse } from '../../../../lib/api/generated/sbi.zod.ts';
import { ApiError, api } from '../../../../lib/api/http.ts';
import { formatDateTime, label, money, ORDER_STATUS, PAYMENT_TYPE } from '../../../../lib/format.ts';
import { ErrorNotice, Loading } from '../../../../components/feedback.tsx';

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
export default function SaleDetail({ orderId }: { orderId: number | null }) {
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
        <span className={`pill ${order.status === 'PAID' ? 'pill-ok' : order.status === 'CANCELED' ? 'pill-err' : 'pill-warn'}`}>{label(ORDER_STATUS, order.status)}</span>
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
        <ErrorNotice error={confirm.error ?? cancel.error} />
        {pending && <div className="actions">
          <button type="button" className="btn btn-primary" disabled={busy} onClick={() => confirm.mutate()}>
            {confirm.isPending ? 'Confirmando…' : 'Confirmar pagamento recebido'}</button>
          <button type="button" className="btn btn-danger" disabled={busy} onClick={() => cancel.mutate()}>
            {cancel.isPending ? 'Cancelando…' : 'Cancelar venda'}</button>
        </div>}
      </section>
    </div>
  );
}
