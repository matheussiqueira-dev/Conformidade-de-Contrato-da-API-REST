'use client';
// Graficos do painel de gestao. Duas series validadas (data-1 #3D4ED6 e data-3 #B26400: CVD ΔE 30, contraste >= 3:1);
// o teal data-2 nao passa no piso de croma e fica fora. Linha de 2px, grade hairline, legenda sempre, rotulo so no
// fim da serie, crosshair por ponteiro e teclado, e tabela com os mesmos dados.
import { useId, useState, type KeyboardEvent, type PointerEvent } from 'react';
import { compactMoney, formatDate, money } from '../lib/format.ts';

export interface DailyPoint { date: string; net: number; profit: number | null }

const W = 640;
const H = 240;
const PAD = { top: 16, right: 92, bottom: 28, left: 64 };
const SERIES = [
  { key: 'net', label: 'Faturamento líquido', color: 'var(--data-1)' },
  { key: 'profit', label: 'Lucro bruto', color: 'var(--data-3)' },
] as const;

function niceMax(value: number): number {
  if (value <= 0) return 1;
  const exp = 10 ** Math.floor(Math.log10(value));
  for (const step of [1, 2, 2.5, 5, 10]) if (step * exp >= value) return step * exp;
  return 10 * exp;
}

/** Caminho com quebra onde o valor e nulo (dia sem custo conhecido nao vira zero). */
function path(points: Array<[number, number] | null>): string {
  let d = '';
  let open = false;
  for (const point of points) {
    if (!point) { open = false; continue; }
    d += `${open ? 'L' : 'M'}${point[0].toFixed(1)},${point[1].toFixed(1)}`;
    open = true;
  }
  return d;
}

export function RevenueChart({ data, title }: { data: readonly DailyPoint[]; title: string }) {
  const [active, setActive] = useState<number | null>(null);
  const id = useId();
  if (data.length === 0) return <p className="empty">Sem dados no período.</p>;
  const values = data.flatMap(d => [d.net, d.profit ?? 0]);
  const min = Math.min(0, ...values);
  const max = niceMax(Math.max(...values));
  const plotW = W - PAD.left - PAD.right;
  const plotH = H - PAD.top - PAD.bottom;
  const x = (i: number) => PAD.left + (data.length === 1 ? plotW / 2 : (i * plotW) / (data.length - 1));
  const y = (v: number) => PAD.top + plotH - ((v - min) / (max - min)) * plotH;
  const ticks = [0, 0.25, 0.5, 0.75, 1].map(t => min + t * (max - min));
  const lines = {
    net: data.map((d, i) => [x(i), y(d.net)] as [number, number]),
    profit: data.map((d, i) => (d.profit === null ? null : [x(i), y(d.profit)] as [number, number])),
  };
  const last = data.length - 1;
  const xLabels = [...new Set([0, Math.floor(last / 2), last])];

  function fromPointer(event: PointerEvent<SVGSVGElement>) {
    const box = event.currentTarget.getBoundingClientRect();
    const px = ((event.clientX - box.left) / box.width) * W;
    const index = Math.round(((px - PAD.left) / plotW) * last);
    setActive(Math.min(Math.max(index, 0), last));
  }
  function onKey(event: KeyboardEvent<SVGSVGElement>) {
    if (event.key === 'ArrowRight') { event.preventDefault(); setActive(i => Math.min((i ?? -1) + 1, last)); }
    if (event.key === 'ArrowLeft') { event.preventDefault(); setActive(i => Math.max((i ?? last + 1) - 1, 0)); }
    if (event.key === 'Escape') setActive(null);
  }
  const point = active === null ? null : data[active];
  const lastNet = lines.net[last];
  const lastProfit = lines.profit[last];
  // Rotulos do fim das series nao se sobrepoem: com menos de 14 px entre eles, o de baixo desce.
  const labelY = { net: (lastNet?.[1] ?? 0) + 4, profit: (lastProfit?.[1] ?? 0) + 4 };
  if (lastNet && lastProfit && Math.abs(labelY.net - labelY.profit) < 14) {
    const top = Math.min(labelY.net, labelY.profit) - 7;
    if (labelY.net <= labelY.profit) { labelY.net = top; labelY.profit = top + 14; } else { labelY.profit = top; labelY.net = top + 14; }
  }

  return (
    <figure className="chart" aria-labelledby={`${id}-title`}>
      <figcaption id={`${id}-title`} className="chart-head">
        <span className="label">{title}</span>
        <ul className="legend" aria-label="Legenda">
          {SERIES.map(s => <li key={s.key}><span className="swatch" style={{ background: s.color }} aria-hidden="true" />{s.label}</li>)}
        </ul>
      </figcaption>
      <div className="chart-box">
        <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-describedby={`${id}-help`} tabIndex={0}
             aria-label={`${title}: use as setas para percorrer os dias`}
             onPointerMove={fromPointer} onPointerLeave={() => setActive(null)} onKeyDown={onKey} onBlur={() => setActive(null)}>
          {ticks.map(t => (
            <g key={t}>
              <line x1={PAD.left} x2={W - PAD.right} y1={y(t)} y2={y(t)} className="grid" />
              <text x={PAD.left - 8} y={y(t) + 4} textAnchor="end" className="tick">{money(t).replace(',00', '')}</text>
            </g>
          ))}
          {xLabels.map(i => (
            <text key={i} x={x(i)} y={H - 8} textAnchor={i === 0 ? 'start' : i === last ? 'end' : 'middle'} className="tick">
              {formatDate(data[i]?.date ?? '').slice(0, 5)}</text>
          ))}
          <path d={path(lines.net)} className="series" stroke="var(--data-1)" />
          <path d={path(lines.profit)} className="series" stroke="var(--data-3)" />
          {lastNet && <text x={lastNet[0] + 8} y={labelY.net} className="end-label">Líquido</text>}
          {lastProfit && <text x={lastProfit[0] + 8} y={labelY.profit} className="end-label">Lucro</text>}
          {active !== null && point && <>
            <line x1={x(active)} x2={x(active)} y1={PAD.top} y2={PAD.top + plotH} className="crosshair" />
            <circle cx={x(active)} cy={y(point.net)} r={4.5} fill="var(--data-1)" className="dot" />
            {point.profit !== null && <circle cx={x(active)} cy={y(point.profit)} r={4.5} fill="var(--data-3)" className="dot" />}
          </>}
        </svg>
        {active !== null && point && (
          <div className="tooltip" role="status" style={{ left: `${(x(active) / W) * 100}%` }}>
            <strong>{formatDate(point.date)}</strong>
            <span><strong className="num">{money(point.net)}</strong> líquido</span>
            <span><strong className="num">{point.profit === null ? '—' : money(point.profit)}</strong> lucro{point.profit === null ? ' (falta custo)' : ''}</span>
          </div>
        )}
      </div>
      <p id={`${id}-help`} className="sr-only">Os mesmos valores estão na tabela abaixo.</p>
      <details className="chart-table">
        <summary>Ver dados em tabela</summary>
        <div className="table-wrap"><table>
          <caption className="sr-only">{title}</caption>
          <thead><tr><th scope="col">Dia</th><th scope="col" className="right">Faturamento líquido</th><th scope="col" className="right">Lucro bruto</th></tr></thead>
          <tbody>{data.map(d => (
            <tr key={d.date}><td className="num">{formatDate(d.date)}</td><td className="num right">{money(d.net)}</td>
              <td className="num right">{d.profit === null ? '—' : money(d.profit)}</td></tr>
          ))}</tbody>
        </table></div>
      </details>
    </figure>
  );
}

export interface ShareItem { label: string; value: number; detail?: string }

/** Participacao de uma unica medida: uma cor, barras de ate 24px, valor e percentual escritos ao lado. */
export function ShareBars({ items, title }: { items: readonly ShareItem[]; title: string }) {
  const total = items.reduce((sum, item) => sum + Math.max(item.value, 0), 0);
  if (items.length === 0 || total === 0) return <p className="empty">Sem vendas pagas no período.</p>;
  return (
    <figure className="chart">
      <figcaption className="label">{title}</figcaption>
      <ul className="share">
        {items.map(item => {
          const share = Math.max(item.value, 0) / total;
          return (
            <li key={item.label}>
              <span className="share-label">{item.label}</span>
              <span className="share-track" aria-hidden="true"><span className="share-bar" style={{ width: `${(share * 100).toFixed(1)}%` }} /></span>
              <span className="num share-value">{money(item.value)} · {(share * 100).toLocaleString('pt-BR', { maximumFractionDigits: 1 })}%
                {item.detail ? <span className="muted"> · {item.detail}</span> : null}</span>
            </li>
          );
        })}
      </ul>
    </figure>
  );
}

/** Realizado contra a meta: barra Indigo e meta como linha ambar tracejada com o valor no rotulo ("Meta R$ 47 mil"). */
export function GoalBar({ realized, goal, title }: { realized: number; goal: number; title: string }) {
  const scale = Math.max(goal, realized, 1) * 1.15;
  const goalX = (goal / scale) * 600;
  return (
    <figure className="chart">
      <figcaption className="label">{title}</figcaption>
      <svg viewBox="0 0 600 72" role="img" aria-label={`Realizado ${money(realized)}${goal > 0 ? `, meta ${money(goal)}` : ', sem meta'}`}>
        <rect x="0" y="24" width="600" height="20" rx="4" fill="var(--line)" />
        <rect x="0" y="24" width={Math.max((realized / scale) * 600, 2)} height="20" rx="4" fill="var(--data-1)" />
        {goal > 0 && <>
          <line className="goal" x1={goalX} x2={goalX} y1="14" y2="54" />
          <text x={goalX} y="10" textAnchor={goalX > 520 ? 'end' : goalX < 80 ? 'start' : 'middle'} className="goal-label">Meta {compactMoney(goal)}</text>
        </>}
        <text x="0" y="68" className="tick">Realizado {compactMoney(realized)}</text>
      </svg>
    </figure>
  );
}
