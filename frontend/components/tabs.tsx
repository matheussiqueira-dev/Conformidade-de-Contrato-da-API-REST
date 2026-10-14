'use client';
import { useId, useRef, type KeyboardEvent, type ReactNode } from 'react';

export interface Tab { id: string; label: string; content: ReactNode }

/** Abas acessiveis (padrao WAI-ARIA): setas, Home e End movem o foco; so o painel ativo e renderizado. */
export default function Tabs({ label, tabs, active, onChange }: {
  label: string; tabs: readonly Tab[]; active: string; onChange: (id: string) => void;
}) {
  const base = useId();
  const refs = useRef<Record<string, HTMLButtonElement | null>>({});
  const index = Math.max(0, tabs.findIndex(tab => tab.id === active));
  const current = tabs[index];

  function onKey(event: KeyboardEvent<HTMLButtonElement>) {
    const moves: Record<string, number> = { ArrowRight: index + 1, ArrowLeft: index - 1, Home: 0, End: tabs.length - 1 };
    const target = moves[event.key];
    if (target === undefined) return;
    event.preventDefault();
    const next = tabs[(target + tabs.length) % tabs.length];
    if (!next) return;
    onChange(next.id);
    refs.current[next.id]?.focus();
  }

  return (
    <div className="stack">
      <div className="tabs" role="tablist" aria-label={label}>
        {tabs.map(tab => {
          const selected = tab.id === current?.id;
          return (
            <button key={tab.id} ref={node => { refs.current[tab.id] = node; }} type="button" role="tab"
                    id={`${base}-${tab.id}-tab`} aria-controls={`${base}-${tab.id}-panel`} aria-selected={selected}
                    tabIndex={selected ? 0 : -1} className="tab" onClick={() => onChange(tab.id)} onKeyDown={onKey}>
              {tab.label}
            </button>
          );
        })}
      </div>
      {current && (
        <div role="tabpanel" id={`${base}-${current.id}-panel`} aria-labelledby={`${base}-${current.id}-tab`} tabIndex={0}>
          {current.content}
        </div>
      )}
    </div>
  );
}
