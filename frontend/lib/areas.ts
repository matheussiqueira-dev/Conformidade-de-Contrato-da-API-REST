// Areas liberadas pelo servidor (UserSession.areas) -> rotas do menu. So entram telas que existem;
// a API continua sendo a autoridade (WP-3.4).
export type Area = 'VENDER' | 'VENDAS' | 'CLIENTES' | 'MEU_DESEMPENHO' | 'GESTAO' | 'PRODUTOS' | 'ESTOQUE' | 'PROMOCOES' | 'ANALISES';

export interface MenuItem { area: Area; href: string; label: string }

export const MENU: readonly MenuItem[] = [
  { area: 'VENDER', href: '/vender', label: 'Vender' },
  { area: 'VENDAS', href: '/vendas', label: 'Vendas' },
  { area: 'CLIENTES', href: '/clientes', label: 'Clientes' },
  { area: 'GESTAO', href: '/gestao', label: 'Gestão' },
  { area: 'ESTOQUE', href: '/gestao/estoque', label: 'Estoque' },
  { area: 'PRODUTOS', href: '/gestao/produtos', label: 'Produtos' },
];

/** Menu na ordem fixa, filtrado pelas areas do usuario. */
export function menuFor(areas: readonly string[], available: ReadonlySet<Area> = new Set(MENU.map(item => item.area))): MenuItem[] {
  return MENU.filter(item => areas.includes(item.area) && available.has(item.area));
}

/** Primeira tela do usuario depois do login (respeita ?next= quando e uma rota interna). */
export function landingPath(areas: readonly string[], next?: string | null): string {
  if (next && next.startsWith('/') && !next.startsWith('//') && !next.startsWith('/api/') && !next.startsWith('/entrar')) return next;
  return menuFor(areas)[0]?.href ?? '/entrar';
}
