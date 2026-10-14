'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import type { MenuItem } from '../lib/areas.ts';

/** Item ativo marcado com aria-current; /gestao nao fica ativo dentro de /gestao/estoque. */
export default function NavLinks({ items }: { items: readonly MenuItem[] }) {
  const pathname = usePathname();
  const active = items.filter(item => pathname === item.href || pathname.startsWith(`${item.href}/`))
    .sort((a, b) => b.href.length - a.href.length)[0];
  return (
    <nav className="nav" aria-label="Áreas">
      {items.map(item => (
        <Link key={item.href} href={item.href} aria-current={active?.href === item.href ? 'page' : undefined}>{item.label}</Link>
      ))}
    </nav>
  );
}
