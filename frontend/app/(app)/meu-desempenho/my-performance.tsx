'use client';
// Meu desempenho (WP-3.11, DEC-ERP-01): so os dados da sessao. Meta zero = "Sem meta", sem percentual (DOM-08).
// A meta aparece como linha ambar tracejada e nomeada sobre a barra do realizado.
import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { GetMyPerformanceResponse } from '../../../lib/api/generated/sbi.zod.ts';
import { api } from '../../../lib/api/http.ts';
import { formatPercent, money } from '../../../lib/format.ts';
import { ErrorNotice, Loading } from '../../../components/feedback.tsx';

const STATE: Readonly<Record<string, string>> = { SEM_META: 'Sem meta', EM_ANDAMENTO: 'Em andamento', META_ATINGIDA: 'Meta atingida' };

function thisMonth(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Fortaleza', year: 'numeric', month: '2-digit' }).format(new Date());
}

export default function MyPerformance() {
  const [month, setMonth] = useState(thisMonth());
  const query = useQuery({
    queryKey: ['performance', month],
    queryFn: ({ signal }) => api.request(`/performance/me?period=${month}`, { signal, schema: GetMyPerformanceResponse }),
    enabled: /^\d{4}-\d{2}$/.test(month),
  });
  const data = query.data;
  const goal = data?.goal.amount ?? 0;
  const realized = data?.realized.amount ?? 0;
  const scale = Math.max(goal, realized, 1) * 1.1;
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
        <h2 id="meta-titulo">{STATE[data.state] ?? data.state}</h2>
        <div className="kpis">
          <div className="kpi"><span className="label">Realizado</span><span className="value">{money(data.realized)}</span></div>
          <div className="kpi"><span className="label">Meta</span><span className="value">{data.state === 'SEM_META' ? 'Sem meta' : money(data.goal)}</span></div>
          <div className="kpi"><span className="label">Atingimento</span><span className="value">{formatPercent(data.achievementPercent)}</span></div>
          <div className="kpi"><span className="label">Falta</span><span className="value">{data.state === 'SEM_META' ? '—' : money(data.remaining)}</span></div>
        </div>
        <figure className="chart">
          <figcaption className="label">Realizado em relação à meta</figcaption>
          <svg viewBox="0 0 600 64" role="img" aria-label={`Realizado ${money(data.realized)}${goal > 0 ? `, meta ${money(data.goal)}` : ', sem meta'}`}>
            <rect x="0" y="20" width="600" height="20" rx="4" fill="var(--line)" />
            <rect x="0" y="20" width={Math.max((realized / scale) * 600, 2)} height="20" rx="4" fill="var(--data-1)" />
            {goal > 0 && <>
              <line x1={(goal / scale) * 600} x2={(goal / scale) * 600} y1="8" y2="52" stroke="var(--data-3)" strokeWidth="2" strokeDasharray="6 4" />
              <text x={(goal / scale) * 600} y="62" textAnchor="middle" className="tick">Meta</text>
            </>}
          </svg>
        </figure>
      </section>}
    </div>
  );
}
