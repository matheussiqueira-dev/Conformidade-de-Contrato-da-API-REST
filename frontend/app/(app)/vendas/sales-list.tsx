'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { ListSalesOrdersResponse } from '../../../lib/api/generated/sbi.zod.ts';
import { api } from '../../../lib/api/http.ts';
import { formatDateTime, label, money, ORDER_STATUS, PAYMENT_TYPE, statusPill } from '../../../lib/format.ts';
import { ErrorNotice, Loading } from '../../../components/feedback.tsx';
import Pager from '../../../components/pager.tsx';

/** Filtro e pagina vao para o endereco sem nova chamada ao servidor: voltar do detalhe reabre a mesma lista. */
function syncAddress(status: string, page: number) {
  const params = new URLSearchParams();
  if (status) params.set('situacao', status);
  if (page > 0) params.set('pagina', String(page + 1));
  const query = params.toString();
  window.history.replaceState(window.history.state, '', `${window.location.pathname}${query ? `?${query}` : ''}`);
}

/** ?situacao= so com um estado conhecido; ?pagina= comeca em 1 no endereco e em 0 na API. */
function fromAddress(params: Pick<URLSearchParams, 'get'>): { status: string; page: number } {
  const situacao = params.get('situacao') ?? '';
  const pagina = params.get('pagina') ?? '';
  return {
    status: Object.hasOwn(ORDER_STATUS, situacao) ? situacao : '',
    page: /^\d{1,4}$/.test(pagina) ? Math.max(Number(pagina) - 1, 0) : 0,
  };
}

/** VENDEDOR ve so as proprias vendas; GERENTE ve todas (C-10). O filtro sai do endereco atual, inclusive ao voltar do detalhe. */
export default function SalesList({ manager }: { manager: boolean }) {
  const params = useSearchParams();
  const [status, setStatus] = useState(() => fromAddress(params).status);
  const [page, setPage] = useState(() => fromAddress(params).page);
  useEffect(() => { syncAddress(status, page); }, [status, page]);
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
          ? <div className="empty stack-sm">
              {page > 0
                ? <><p>Esta página não tem vendas.</p>
                    <p><button type="button" className="btn" onClick={() => setPage(0)}>Ir para a primeira página</button></p></>
                : status
                ? <><p>Nenhuma venda com a situação “{label(ORDER_STATUS, status)}”.</p>
                    <p><button type="button" className="btn" onClick={() => { setStatus(''); setPage(0); }}>Ver todas as vendas</button></p></>
                : <><p>{manager ? 'Nenhuma venda registrada ainda.' : 'Você ainda não registrou vendas.'}</p>
                    <p><Link className="btn btn-primary" href="/vender">Nova venda</Link></p></>}
            </div>
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
                  <td><span className={statusPill(sale.status)}>{label(ORDER_STATUS, sale.status)}</span></td>
                  <td className="num right">{money(sale.total)}</td>
                </tr>
              ))}</tbody>
            </table></div>}
        {data && <Pager page={data.page} size={data.size} total={data.totalElements} onPage={setPage} />}
      </section>
    </div>
  );
}
