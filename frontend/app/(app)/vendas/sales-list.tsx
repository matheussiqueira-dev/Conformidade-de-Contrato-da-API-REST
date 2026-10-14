'use client';
import { useState } from 'react';
import Link from 'next/link';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { ListSalesOrdersResponse } from '../../../lib/api/generated/sbi.zod.ts';
import { api } from '../../../lib/api/http.ts';
import { formatDateTime, label, money, ORDER_STATUS, PAYMENT_TYPE } from '../../../lib/format.ts';
import { ErrorNotice, Loading } from '../../../components/feedback.tsx';
import Pager from '../../../components/pager.tsx';

/** VENDEDOR ve so as proprias vendas; GERENTE ve todas (C-10). */
export default function SalesList({ manager }: { manager: boolean }) {
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const query = useQuery({
    queryKey: ['sales', status, page],
    queryFn: ({ signal }) => {
      const params = new URLSearchParams({ page: String(page), size: '20' });
      if (status) params.set('status', status);
      return api.request(`/sales/orders?${params}`, { signal, schema: ListSalesOrdersResponse });
    },
    placeholderData: keepPreviousData,
  });
  const data = query.data;
  return (
    <div className="stack">
      <header className="page-head">
        <div className="stack-sm">
          <h1>Vendas</h1>
          <p>{manager ? 'Todas as vendas da loja.' : 'As vendas feitas por você.'}</p>
        </div>
        <div className="field">
          <label htmlFor="filtro-status">Situação</label>
          <select id="filtro-status" className="select" value={status} onChange={event => { setStatus(event.target.value); setPage(0); }}>
            <option value="">Todas</option>
            {Object.entries(ORDER_STATUS).map(([value, text]) => <option key={value} value={value}>{text}</option>)}
          </select>
        </div>
      </header>
      <section className="card stack-sm" aria-label="Lista de vendas">
        <ErrorNotice error={query.error} />
        {query.isLoading ? <Loading label="Carregando vendas…" /> : data && data.content.length === 0
          ? <p className="empty">Nenhuma venda encontrada.</p>
          : data && <div className="table-wrap"><table>
              <caption className="sr-only">Vendas</caption>
              <thead><tr>
                <th scope="col">Venda</th><th scope="col">Data</th><th scope="col">Cliente</th>
                {manager && <th scope="col">Vendedor</th>}<th scope="col">Pagamento</th><th scope="col">Situação</th><th scope="col" className="right">Total</th>
              </tr></thead>
              <tbody>{data.content.map(sale => (
                <tr key={sale.id}>
                  <td><Link href={`/vendas/${sale.id}`} className="mono">#{sale.id}</Link></td>
                  <td className="num">{formatDateTime(sale.createdAt)}</td>
                  <td>{sale.customer.name}</td>
                  {manager && <td>{sale.seller.name}</td>}
                  <td>{label(PAYMENT_TYPE, sale.paymentType)}</td>
                  <td>{label(ORDER_STATUS, sale.status)}</td>
                  <td className="num right">{money(sale.total)}</td>
                </tr>
              ))}</tbody>
            </table></div>}
        {data && <Pager page={data.page} size={data.size} total={data.totalElements} onPage={setPage} />}
      </section>
    </div>
  );
}
