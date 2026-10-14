'use client';
// Gestao (WP-3.8): faturamento do periodo, lucro e margem (com "—" quando falta custo), pedidos pagos, serie diaria,
// formas de pagamento, vendedores, produtos, alerta de estoque e meta do mes. Atualiza a cada 15 s com a aba visivel;
// erro nunca vira zero.
import { useMemo, useState } from 'react';
import Link from 'next/link';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { GetManagementSummaryResponse, GetRevenueReportResponse, ListStockProductsResponse } from '../../../lib/api/generated/sbi.zod.ts';
import { api } from '../../../lib/api/http.ts';
import { formatPercent, label, money, PAYMENT_TYPE } from '../../../lib/format.ts';
import { ErrorNotice, Loading } from '../../../components/feedback.tsx';
import { RevenueChart, ShareBars } from '../../../components/charts.tsx';

const PERIODS = [['TODAY', 'Hoje'], ['D7', '7 dias'], ['D30', '30 dias'], ['D90', '90 dias']] as const;
type Period = (typeof PERIODS)[number][0];
const GOAL_STATE: Readonly<Record<string, string>> = { SEM_META: 'Sem meta', EM_ANDAMENTO: 'Em andamento', META_ATINGIDA: 'Meta atingida' };

function currentMonth(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Fortaleza', year: 'numeric', month: '2-digit' }).format(new Date());
}

export default function Dashboard() {
  const [period, setPeriod] = useState<Period>('D30');
  const [productFilter, setProductFilter] = useState('');
  const month = currentMonth();
  const live = { refetchInterval: 15_000, refetchIntervalInBackground: false } as const;

  const revenue = useQuery({
    queryKey: ['revenue', period],
    queryFn: ({ signal }) => api.request(`/management/revenue?period=${period}`, { signal, schema: GetRevenueReportResponse }),
    placeholderData: keepPreviousData,
    ...live,
  });
  const summary = useQuery({
    queryKey: ['summary', month],
    queryFn: ({ signal }) => api.request(`/management/summary?period=${month}`, { signal, schema: GetManagementSummaryResponse }),
    ...live,
  });
  const lowStock = useQuery({
    queryKey: ['low-stock'],
    queryFn: ({ signal }) => api.request('/stock/products?belowMin=true&sellable=true&size=5', { signal, schema: ListStockProductsResponse }),
    ...live,
  });

  const report = revenue.data;
  const products = useMemo(() => {
    const term = productFilter.trim().toLowerCase();
    return (report?.topProducts ?? []).filter(p => !term || p.name.toLowerCase().includes(term) || p.sku.toLowerCase().includes(term));
  }, [report, productFilter]);
  const updated = revenue.dataUpdatedAt
    ? new Intl.DateTimeFormat('pt-BR', { timeStyle: 'medium', timeZone: 'America/Fortaleza' }).format(revenue.dataUpdatedAt) : null;
  const missingCost = report !== undefined && report.grossProfit === null;

  return (
    <div className="stack">
      <header className="toolbar">
        <div className="stack-sm">
          <p className="muted">Gestão / Visão geral</p>
          <h1>Visão geral da loja</h1>
          <p className="muted">Faturamento confirmado, lucro e metas. {updated && <span aria-live="polite">Atualizado às {updated}.</span>}</p>
        </div>
        <fieldset className="segmented">
          <legend className="sr-only">Período</legend>
          {PERIODS.map(([value, text]) => (
            <label key={value}><input type="radio" name="periodo" value={value} checked={period === value} onChange={() => setPeriod(value)} />
              <span>{text}</span></label>
          ))}
        </fieldset>
      </header>

      <ErrorNotice error={revenue.error}>
        {report && <p>Os números abaixo são da última atualização bem-sucedida.</p>}
      </ErrorNotice>
      {revenue.isLoading && <Loading label="Carregando faturamento…" />}

      {report && <>
        <section className="kpis" aria-label="Indicadores do período">
          <div className="card kpi"><span className="label">Faturamento líquido</span><span className="value">{money(report.netRevenue)}</span>
            <span className="note">Bruto {money(report.grossRevenue)} · estornos {money(report.refunds)}</span></div>
          <div className="card kpi"><span className="label">Lucro bruto</span><span className="value">{money(report.grossProfit)}</span>
            <span className="note">{missingCost ? 'Há itens vendidos sem custo informado.' : `Cobertura de custo ${formatPercent(report.costCoveragePercent)}`}</span></div>
          <div className="card kpi"><span className="label">Margem bruta</span><span className="value">{formatPercent(report.grossMarginPercent)}</span>
            <span className="note">{missingCost ? 'Sem margem enquanto faltar custo.' : 'Lucro sobre o faturamento líquido.'}</span></div>
          <div className="card kpi"><span className="label">Pedidos pagos</span><span className="value">{report.ordersPaid}</span>
            <span className="note">Ticket médio {money(report.averageTicket)} · {report.unitsSold} unidades</span></div>
        </section>

        <section className="card">
          <RevenueChart title="Faturamento líquido e lucro bruto por dia"
                        data={report.daily.map(d => ({ date: d.date, net: d.netRevenue.amount, profit: d.grossProfit?.amount ?? null }))} />
        </section>

        <div className="grid-2">
          <section className="card">
            <ShareBars title="Formas de pagamento" items={report.byPaymentMethod.map(m => ({
              label: label(PAYMENT_TYPE, m.method), value: m.netRevenue.amount, detail: `${m.ordersPaid} pedidos` }))} />
          </section>
          <section className="card stack-sm" aria-labelledby="ranking-titulo">
            <h2 id="ranking-titulo" className="label">Vendedores</h2>
            {report.bySeller.length === 0 ? <p className="empty">Sem vendas pagas no período.</p> : <ol className="stack-sm">
              {report.bySeller.map(s => <li key={s.seller.id}><strong>{s.seller.name}</strong> · <span className="num">{money(s.netRevenue)}</span>
                <span className="muted"> · {s.ordersPaid} pedidos</span></li>)}
            </ol>}
          </section>
        </div>

        <div className="grid-2">
          <section className="card stack-sm" aria-labelledby="produtos-titulo">
            <div className="toolbar">
              <h2 id="produtos-titulo" className="label">Produtos mais vendidos</h2>
              <div className="field"><label htmlFor="filtro-produto" className="sr-only">Buscar produto</label>
                <input id="filtro-produto" className="input" type="search" placeholder="Buscar produto" value={productFilter}
                       onChange={event => setProductFilter(event.target.value)} /></div>
            </div>
            {products.length === 0 ? <p className="empty">Nenhum produto vendido no período.</p> : <div className="table-wrap"><table>
              <caption className="sr-only">Produtos mais vendidos</caption>
              <thead><tr><th scope="col">Produto</th><th scope="col" className="right">Unid.</th><th scope="col" className="right">Líquido</th>
                <th scope="col" className="right">Lucro</th></tr></thead>
              <tbody>{products.map(p => (
                <tr key={p.productId}><td>{p.name}<br /><span className="mono muted">{p.sku}</span></td><td className="num right">{p.unitsSold}</td>
                  <td className="num right">{money(p.netRevenue)}</td><td className="num right">{money(p.grossProfit)}</td></tr>
              ))}</tbody>
            </table></div>}
          </section>
          <div className="stack">
            <section className="card stack-sm" aria-labelledby="meta-titulo">
              <h2 id="meta-titulo" className="label">Meta do mês</h2>
              <ErrorNotice error={summary.error} />
              {summary.data && (summary.data.state === 'SEM_META'
                ? <p className="muted">Sem meta para este mês. Realizado: <span className="num">{money(summary.data.storeRealized)}</span>.</p>
                : <p><span className="num"><strong>{money(summary.data.storeRealized)}</strong> de {money(summary.data.storeGoal)}</span>
                    {' '}({formatPercent(summary.data.achievementPercent)}) · {GOAL_STATE[summary.data.state]}</p>)}
            </section>
            <section className="card stack-sm" aria-labelledby="alerta-titulo">
              <h2 id="alerta-titulo" className="label">Estoque abaixo do mínimo: <span className="num">{report.lowStockCount}</span></h2>
              <ErrorNotice error={lowStock.error} />
              {lowStock.data && (lowStock.data.content.length === 0 ? <p className="muted">Nenhum produto abaixo do mínimo.</p> : <ul className="stack-sm">
                {lowStock.data.content.map(p => <li key={p.productId}><span className="pill pill-warn">{p.available} disp.</span> {p.name}
                  <span className="muted"> · mínimo {p.minLevel}</span></li>)}
              </ul>)}
              <p><Link href="/gestao/estoque">Abrir estoque</Link></p>
            </section>
            <p className="muted">Pendentes de pagamento: <span className="num">{report.pendingPaymentCount}</span> · {money(report.pendingPaymentAmount)}</p>
          </div>
        </div>
      </>}
    </div>
  );
}
