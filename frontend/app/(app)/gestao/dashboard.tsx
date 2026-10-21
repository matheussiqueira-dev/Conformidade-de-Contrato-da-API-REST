'use client';
// Gestao (WP-3.8): faturamento do periodo, lucro e margem (com "—" quando falta custo), pedidos pagos, serie diaria,
// formas de pagamento, vendedores, produtos, alerta de estoque e meta do mes. Atualiza a cada 15 s com a aba visivel;
// erro nunca vira zero.
import { useMemo, useState } from 'react';
import type { z } from 'zod';
import Link from 'next/link';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { GetManagementSummaryResponse, GetRevenueReportResponse, ListStockProductsResponse } from '../../../lib/api/generated/sbi.zod.ts';
import { api } from '../../../lib/api/http.ts';
import { formatMonth, formatPercent, formatTime, label, money, PAYMENT_TYPE } from '../../../lib/format.ts';
import { ErrorNotice, Loading } from '../../../components/feedback.tsx';
import { GoalBar, RevenueChart, ShareBars } from '../../../components/charts.tsx';
import { Icon } from '../../../components/brand/icons.tsx';
import ProductThumb from '../../../components/product-thumb.tsx';

const PERIODS = [['TODAY', 'Hoje'], ['D7', '7 dias'], ['D30', '30 dias'], ['D90', '90 dias']] as const;
/** Linhas da tabela "Produtos mais vendidos"; a lista completa fica em Análises. */
const TOP_PRODUCTS = 10;
type Period = (typeof PERIODS)[number][0];
const GOAL_STATE: Readonly<Record<string, string>> = { SEM_META: 'Sem meta', EM_ANDAMENTO: 'Em andamento', META_ATINGIDA: 'Meta atingida' };

/** Valor ausente ("Não informado") sai em texto, nao na fonte de numeros. */
function valueClass(value: unknown): string {
  return value === null || value === undefined ? 'value text' : 'value';
}

/** Titulo do cartao da meta dizendo a conclusao (manual v1.0). */
function goalTitle(summary: z.infer<typeof GetManagementSummaryResponse> | undefined): string {
  if (!summary) return 'Meta do mês';
  if (summary.state === 'META_ATINGIDA') return 'A loja bateu a meta do mês';
  if (summary.state === 'SEM_META') return 'Meta do mês não definida';
  return `Faltam ${money(summary.remaining)} para a meta da loja`;
}

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
  const matching = useMemo(() => {
    const term = productFilter.trim().toLowerCase();
    return (report?.topProducts ?? []).filter(p => !term || p.name.toLowerCase().includes(term) || p.sku.toLowerCase().includes(term));
  }, [report, productFilter]);
  const products = matching.slice(0, TOP_PRODUCTS);
  const updated = revenue.dataUpdatedAt ? formatTime(new Date(revenue.dataUpdatedAt).toISOString()) : null;
  const missingCost = report !== undefined && report.grossProfit === null;

  return (
    <div className="stack">
      <header className="toolbar">
        <div className="stack-sm">
          <h1>Visão geral da loja</h1>
          <p className="muted">{formatMonth(month)} · somente pagamentos confirmados, descontados os estornos.
            {updated && <span aria-live="polite"> Atualizado às {updated}.</span>}</p>
        </div>
        <Link className="btn btn-primary" href="/vender"><Icon name="vender" />Nova venda</Link>
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
          <div className="card kpi"><span className="label">Lucro bruto</span><span className={valueClass(report.grossProfit)}>{money(report.grossProfit)}</span>
            <span className="note">{missingCost ? 'Há itens vendidos sem custo informado.' : `Cobertura de custo ${formatPercent(report.costCoveragePercent)}`}</span></div>
          <div className="card kpi"><span className="label">Margem bruta</span><span className={valueClass(report.grossMarginPercent)}>{formatPercent(report.grossMarginPercent)}</span>
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
            {products.length === 0 ? <p className="empty">Nenhum produto vendido no período.</p>
              : <div className="table-wrap" tabIndex={0} role="group" aria-labelledby="produtos-titulo"><table>
              <caption className="sr-only">Produtos mais vendidos</caption>
              <thead><tr><th scope="col">Produto</th><th scope="col" className="right">Unid.</th><th scope="col" className="right">Líquido</th>
                <th scope="col" className="right">Lucro</th></tr></thead>
              <tbody>{products.map(p => (
                <tr key={p.productId}><td><div className="product-cell"><ProductThumb sku={p.sku} name={p.name} /><span>{p.name}<br /><span className="mono muted">{p.sku}</span></span></div></td><td className="num right">{p.unitsSold}</td>
                  <td className="num right">{money(p.netRevenue)}</td><td className="num right">{money(p.grossProfit)}</td></tr>
              ))}</tbody>
            </table></div>}
            {matching.length > TOP_PRODUCTS && <p className="muted">
              Mostrando {TOP_PRODUCTS} de {matching.length}. <Link href="/gestao/analises">Ver todos</Link></p>}
          </section>
          <div className="stack">
            <section className="card stack-sm" aria-labelledby="meta-titulo">
              <h2 id="meta-titulo" className="label">{goalTitle(summary.data)}</h2>
              <ErrorNotice error={summary.error} />
              {summary.data && (summary.data.state === 'SEM_META'
                ? <p className="muted">Sem meta para este mês. Realizado: <span className="money">{money(summary.data.storeRealized)}</span>.</p>
                : <>
                    <p><span className="money"><strong>{money(summary.data.storeRealized)}</strong> de {money(summary.data.storeGoal)}</span>
                      {' '}({formatPercent(summary.data.achievementPercent)}) · {GOAL_STATE[summary.data.state]}</p>
                    <GoalBar title="Realizado da loja em relação à meta" realized={summary.data.storeRealized.amount} goal={summary.data.storeGoal.amount} />
                  </>)}
            </section>
            <section className="card stack-sm" aria-labelledby="alerta-titulo">
              <h2 id="alerta-titulo" className="label">Estoque abaixo do mínimo: <span className="num">{report.lowStockCount}</span></h2>
              <ErrorNotice error={lowStock.error} />
              {lowStock.data && (lowStock.data.content.length === 0 ? <p className="muted">Nenhum produto abaixo do mínimo.</p> : <ul className="stack-sm">
                {lowStock.data.content.map(p => <li key={p.productId}><span className="pill pill-warn">{p.available} disp.</span> {p.name}
                  <span className="muted"> · mínimo {p.minLevel}</span></li>)}
              </ul>)}
              <p><Link href={report.lowStockCount > 0 ? '/gestao/estoque?abaixo=1' : '/gestao/estoque'}>
                {report.lowStockCount > 0 ? 'Ver produtos abaixo do mínimo' : 'Abrir estoque'}</Link></p>
            </section>
            <p className="muted">Pendentes de pagamento: <span className="num">{report.pendingPaymentCount}</span> · {money(report.pendingPaymentAmount)}
              {report.pendingPaymentCount > 0 && <> · <Link href="/vendas?situacao=PENDING_PAYMENT">Ver pedidos pendentes</Link></>}</p>
          </div>
        </div>
      </>}
    </div>
  );
}
