'use client';
// Analises (WP-3.11): views do V6 por mes (semanas, formas de pagamento, margem, promocoes) e posicao de estoque.
// Margem e custo ficam "—" quando falta custo (C-20); nunca zero inventado.
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { GetSalesAnalyticsResponse, GetStockAnalyticsResponse } from '../../../../lib/api/generated/sbi.zod.ts';
import { api } from '../../../../lib/api/http.ts';
import { formatDate, formatPercent, label, money, PAYMENT_TYPE } from '../../../../lib/format.ts';
import { ErrorNotice, Loading } from '../../../../components/feedback.tsx';
import { ShareBars } from '../../../../components/charts.tsx';

function thisMonth(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Fortaleza', year: 'numeric', month: '2-digit' }).format(new Date());
}

export default function Analytics() {
  const [month, setMonth] = useState(thisMonth());
  const sales = useQuery({
    queryKey: ['analytics-sales', month],
    queryFn: ({ signal }) => api.request(`/analytics/sales?month=${month}`, { signal, schema: GetSalesAnalyticsResponse }),
    enabled: /^\d{4}-\d{2}$/.test(month),
  });
  const stock = useQuery({
    queryKey: ['analytics-stock'],
    queryFn: ({ signal }) => api.request('/analytics/stock', { signal, schema: GetStockAnalyticsResponse }),
  });
  const data = sales.data;
  return (
    <div className="stack">
      <header className="toolbar">
        <div className="stack-sm"><p className="muted">Gestão / Análises</p><h1>Análises</h1></div>
        <div className="field"><label htmlFor="mes-analise">Mês</label>
          <input id="mes-analise" className="input" type="month" value={month} onChange={event => setMonth(event.target.value)} /></div>
      </header>
      <ErrorNotice error={sales.error} />
      {sales.isLoading && <Loading />}
      {data && <>
        <div className="grid-2">
          <section className="card">
            <ShareBars title="Realizado por semana" items={data.weekly.map(w => ({ label: `${formatDate(w.start).slice(0, 5)}–${formatDate(w.end).slice(0, 5)}`, value: w.realized.amount }))} />
          </section>
          <section className="card">
            <ShareBars title="Formas de pagamento" items={data.byPaymentMethod.map(m => ({ label: label(PAYMENT_TYPE, m.method), value: m.netRevenue.amount, detail: `${m.ordersPaid} pedidos` }))} />
          </section>
        </div>
        <section className="card stack-sm" aria-labelledby="margem-titulo">
          <h2 id="margem-titulo" className="label">Margem por produto</h2>
          {data.productMargins.length === 0 ? <p className="empty">Sem vendas pagas no mês.</p> : <div className="table-wrap"><table>
            <caption className="sr-only">Margem por produto</caption>
            <thead><tr><th scope="col">Produto</th><th scope="col" className="right">Unid.</th><th scope="col" className="right">Líquido</th>
              <th scope="col" className="right">Custo</th><th scope="col" className="right">Margem</th><th scope="col" className="right">%</th></tr></thead>
            <tbody>{data.productMargins.map(m => (
              <tr key={m.productId}><td>{m.name}{m.itemsWithoutCost > 0 && <><br /><span className="pill pill-warn">{m.itemsWithoutCost} sem custo</span></>}</td>
                <td className="num right">{m.units}</td><td className="num right">{money(m.netRevenue)}</td><td className="num right">{money(m.cost)}</td>
                <td className="num right">{money(m.grossMargin)}</td><td className="num right">{formatPercent(m.marginPercent)}</td></tr>
            ))}</tbody>
          </table></div>}
        </section>
        <section className="card stack-sm" aria-labelledby="promo-uso-titulo">
          <h2 id="promo-uso-titulo" className="label">Uso de promoções</h2>
          {data.promotionUsage.length === 0 ? <p className="empty">Nenhuma promoção usada no mês.</p> : <div className="table-wrap"><table>
            <caption className="sr-only">Uso de promoções</caption>
            <thead><tr><th scope="col">Promoção</th><th scope="col" className="right">Itens</th><th scope="col" className="right">Desconto</th><th scope="col" className="right">Líquido</th></tr></thead>
            <tbody>{data.promotionUsage.map(u => (
              <tr key={u.promotionId}><td>{u.name}</td><td className="num right">{u.items}</td><td className="num right">{money(u.discountTotal)}</td><td className="num right">{money(u.netRevenue)}</td></tr>
            ))}</tbody>
          </table></div>}
        </section>
        <section className="card stack-sm" aria-labelledby="desconto-manual-titulo">
          <h2 id="desconto-manual-titulo" className="label">Descontos manuais</h2>
          {data.manualDiscounts.length === 0 ? <p className="empty">Nenhum desconto manual em pedidos pagos no mês.</p> : <div className="table-wrap"><table>
            <caption className="sr-only">Descontos manuais por vendedor</caption>
            <thead><tr><th scope="col">Vendedor</th><th scope="col" className="right">Pedidos</th><th scope="col" className="right">Concedido</th><th scope="col" className="right">Aprovados pelo gerente</th></tr></thead>
            <tbody>{data.manualDiscounts.map(d => (
              <tr key={d.seller.id}><td>{d.seller.name}</td><td className="num right">{d.orders}</td><td className="num right">{money(d.amount)}</td><td className="num right">{d.approvedByManager}</td></tr>
            ))}</tbody>
          </table></div>}
        </section>
      </>}
      <section className="card stack-sm" aria-labelledby="estoque-analise">
        <h2 id="estoque-analise" className="label">Estoque</h2>
        <ErrorNotice error={stock.error} />
        {stock.data && <>
          <dl className="facts">
            <div><dt>Produtos ativos</dt><dd className="num">{stock.data.products}</dd></div>
            <div><dt>Unidades</dt><dd className="num">{stock.data.units}</dd></div>
            <div><dt>Valor a custo</dt><dd className="num">{money(stock.data.valueAtCost)}{stock.data.valueAtCost === null ? ' (há produto sem custo)' : ''}</dd></div>
            <div><dt>Valor a preço de venda</dt><dd className="num">{money(stock.data.valueAtPrice)}</dd></div>
          </dl>
          <p>{stock.data.belowMin.length} abaixo do mínimo{stock.data.belowMin.length > 0 ? `: ${stock.data.belowMin.map(p => p.name).join(', ')}` : '.'}</p>
        </>}
      </section>
    </div>
  );
}
