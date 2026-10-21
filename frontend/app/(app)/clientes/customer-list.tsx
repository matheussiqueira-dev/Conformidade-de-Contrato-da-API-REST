'use client';
import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { ListCustomersResponse } from '../../../lib/api/generated/sbi.zod.ts';
import { api } from '../../../lib/api/http.ts';
import { formatDateTime } from '../../../lib/format.ts';
import { ErrorNotice, Loading } from '../../../components/feedback.tsx';
import CustomerForm from '../../../components/customer-form.tsx';
import { useModal } from '../../../components/use-modal.ts';
import Pager from '../../../components/pager.tsx';

/** Busca por CPF/CNPJ (so digitos) ou prefixo do nome (C-11); documento mascarado para o VENDEDOR (C-12). */
export default function CustomerList() {
  const router = useRouter();
  const [term, setTerm] = useState('');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [drawer, setDrawer] = useState(false);
  const dialog = useModal(drawer);

  useEffect(() => {
    const timer = setTimeout(() => { setQ(term.trim()); setPage(0); }, 250);
    return () => clearTimeout(timer);
  }, [term]);

  const query = useQuery({
    queryKey: ['customers-list', q, page],
    queryFn: ({ signal }) => api.request(`/customers?${new URLSearchParams({ q, page: String(page), size: '20' })}`,
      { signal, schema: ListCustomersResponse }),
    placeholderData: keepPreviousData,
  });
  const data = query.data;

  return (
    <div className="stack">
      <header className="page-head">
        <div className="stack-sm">
          <h1>Clientes</h1>
          <p>Busque por CPF, CNPJ ou nome.</p>
        </div>
        <button type="button" className="btn btn-primary" onClick={() => setDrawer(true)}>Cadastrar cliente</button>
      </header>
      <section className="card stack" aria-label="Lista de clientes">
        <div className="field">
          <label htmlFor="busca-clientes">Buscar cliente</label>
          <input id="busca-clientes" className="input" type="search" autoComplete="off" value={term} onChange={event => setTerm(event.target.value)} />
        </div>
        <ErrorNotice error={query.error} />
        {query.isLoading ? <Loading label="Carregando clientes…" /> : data && data.content.length === 0
          ? <p className="empty">{q ? 'Nenhum cliente encontrado para esta busca.' : 'Nenhum cliente cadastrado ainda.'}</p>
          : data && <div className="table-wrap"><table>
              <caption className="sr-only">Clientes</caption>
              <thead><tr><th scope="col">Nome</th><th scope="col">Documento</th><th scope="col">Contato</th>
                <th scope="col" className="right">Pedidos</th><th scope="col">Último pedido</th></tr></thead>
              <tbody>{data.content.map(customer => (
                <tr key={customer.id}>
                  <td><Link href={`/clientes/${customer.id}`}>{customer.name}</Link></td>
                  <td className="mono">{customer.document}</td>
                  <td>{customer.email ?? '—'}<br /><span className="muted">{customer.phone ?? ''}</span></td>
                  <td className="num right">{customer.ordersCount}</td>
                  <td className="num">{customer.lastOrderAt ? formatDateTime(customer.lastOrderAt) : '—'}</td>
                </tr>
              ))}</tbody>
            </table></div>}
        {data && <Pager page={data.page} size={data.size} total={data.totalElements} onPage={setPage} />}
      </section>
      <dialog ref={dialog} className="drawer" aria-labelledby="cadastro-cliente" onClose={() => setDrawer(false)}>
        <div className="stack">
          <h2 id="cadastro-cliente">Cadastrar cliente</h2>
          {drawer && <CustomerForm idPrefix="novo-cliente" onCancel={() => setDrawer(false)}
                                   onCreated={created => { setDrawer(false); router.push(`/clientes/${created.id}`); }}
                                   onExisting={id => { setDrawer(false); router.push(`/clientes/${id}`); }} />}
        </div>
      </dialog>
    </div>
  );
}
