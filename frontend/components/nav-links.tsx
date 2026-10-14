'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import type { MenuGroup } from '../lib/areas.ts';
import { Icon } from './brand/icons.tsx';

/** Menu em secoes, icone + texto; item ativo com aria-current e /gestao nao fica ativo dentro de /gestao/estoque. */
export default function NavLinks({ groups }: { groups: readonly MenuGroup[] }) {
  const pathname = usePathname();
  const active = groups.flatMap(group => group.items)
    .filter(item => pathname === item.href || pathname.startsWith(`${item.href}/`))
    .sort((a, b) => b.href.length - a.href.length)[0];
  return (
    <nav className="nav" aria-label="Áreas">
      {groups.map(group => (
        <div key={group.section} className="nav-group" role="group" aria-labelledby={`menu-${group.section}`}>
          <span className="nav-group-label" id={`menu-${group.section}`}>{group.label}</span>
          {group.items.map(item => (
            <Link key={item.href} href={item.href} aria-current={active?.href === item.href ? 'page' : undefined}>
              <Icon name={item.icon} />{item.label}
            </Link>
          ))}
        </div>
      ))}
    </nav>
  );
}
