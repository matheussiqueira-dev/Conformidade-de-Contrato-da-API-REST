'use client';
// Meu desempenho (WP-3.11, DEC-ERP-01): so os dados da sessao. Meta zero = "Sem meta", sem percentual (DOM-08).
// A meta aparece como linha ambar tracejada e nomeada sobre a barra do realizado.
import { useState } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { GetMyPerformanceResponse } from '../../../lib/api/generated/sbi.zod.ts';
import { api } from '../../../lib/api/http.ts';
import { formatMonth, formatPercent, money } from '../../../lib/format.ts';
import { ErrorNotice, Loading } from '../../../components/feedback.tsx';
import { GoalBar } from '../../../components/charts.tsx';

const STATE: Readonly<Record<string, string>> = { SEM_META: 'Sem meta', EM_ANDAMENTO: 'Em andamento', META_ATINGIDA: 'Meta atingida' };
const STATE_PILL: Readonly<Record<string, string>> = { SEM_META: 'pill-info', EM_ANDAMENTO: 'pill-warn', META_ATINGIDA: 'pill-ok' };

/** Titulo que diz a conclusao (manual v1.0): quanto falta, meta batida ou mes sem meta. */
function conclusion(state: string, remaining: string): string {
  if (state === 'META_ATINGIDA') return 'Você bateu a sua meta do mês.';
  if (state === 'SEM_META') return 'Sem meta definida para este mês.';
  return `Faltam ${remaining} para a sua meta.`;
}

function thisMonth(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Fortaleza', year: 'numeric', month: '2-digit' }).format(new Date());
}

export default function MyPerformance({ manager = false }: { manager?: boolean }) {
  const [month, setMonth] = useState(thisMonth());
  const query = useQuery({
    queryKey: ['performance', month],
    queryFn: ({ signal }) => api.request(`/performance/me?period=${month}`, { signal, schema: GetMyPerformanceResponse }),
    enabled: /^\d{4}-\d{2}$/.test(month),
  });
  const data = query.data;
  const goal = data?.goal.amount ?? 0;
  const realized = data?.realized.amount ?? 0;
  return (
    <div className="stack">
      <header className="toolbar">
        <div className="stack-sm"><h1>Meu desempenho</h1><p className="muted">Vendas pagas menos estornos no mês.</p></div>
        <div className="field"><label htmlFor="mes">Mês</label>
          <input id="mes" className="input" type="month" value={month} onChange={event => setMonth(event.target.value)} /></div>
      </header>
      <ErrorNotice error={query.error} />
      {query.isLoading && <Loading />}
      {data && <section className="card stack" aria-labelledby="meta-titulo">
        <div className="stack-sm">
          <h2 id="meta-titulo">{conclusion(data.state, money(data.remaining))}</h2>
          <p><span className={`pill ${STATE_PILL[data.state] ?? 'pill-info'}`}>{STATE[data.state] ?? data.state}</span>
            <span className="muted"> · {formatMonth(month)}</span></p>
        </div>
        <div className="kpis">
          <div className="kpi"><span className="label">Realizado</span><span className="value">{money(data.realized)}</span></div>
          <div className="kpi"><span className="label">Meta</span><span className={`value${data.state === 'SEM_META' ? ' text' : ''}`}>{data.state === 'SEM_META' ? 'Sem meta' : money(data.goal)}</span></div>
          <div className="kpi"><span className="label">Atingimento</span><span className={`value${data.state === 'SEM_META' ? ' text' : ''}`}>{data.state === 'SEM_META' ? 'Sem meta' : formatPercent(data.achievementPercent)}</span></div>
          <div className="kpi"><span className="label">Falta</span><span className={`value${data.state === 'SEM_META' ? ' text' : ''}`}>{data.state === 'SEM_META' ? 'Sem meta' : money(data.remaining)}</span></div>
        </div>
        <GoalBar title="Realizado em relação à meta" realized={realized} goal={goal} />
        <p><Link href="/vendas">{manager ? 'Ver as vendas da loja' : 'Ver as minhas vendas'}</Link>
          {data.state !== 'META_ATINGIDA' && <> · <Link href="/vender">Nova venda</Link></>}</p>
      </section>}
    </div>
  );
}
