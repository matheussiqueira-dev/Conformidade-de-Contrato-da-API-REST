'use client';
import { useEffect, useState, type ReactNode } from 'react';
import { usePathname } from 'next/navigation';
import { SbiLogo } from './brand/logo.tsx';

/** Barra lateral; abaixo de 760 px vira faixa superior com o logo e o menu recolhivel, que fecha ao trocar de tela. */
export default function Sidebar({ home, children }: { home: string; children: ReactNode }) {
  const [open, setOpen] = useState(false);
  const pathname = usePathname();
  useEffect(() => { setOpen(false); }, [pathname]);
  return (
    <aside className="sidebar" aria-label="Navegação principal" data-open={open}>
      <div className="sidebar-top">
        <a className="brand-link" href={home}><SbiLogo size={36} /></a>
        <button type="button" className="btn btn-ghost nav-toggle" aria-expanded={open} aria-controls="menu-principal"
                onClick={() => setOpen(value => !value)}>
          {open ? 'Fechar menu' : 'Menu'}
        </button>
      </div>
      <div className="sidebar-body" id="menu-principal">{children}</div>
    </aside>
  );
}
