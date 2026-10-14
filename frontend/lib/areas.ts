// Areas liberadas pelo servidor (UserSession.areas) -> rotas do menu. So entram telas que existem;
// a API continua sendo a autoridade (WP-3.4).
import type { IconName } from '../components/brand/icons-data.ts';

export type Area = 'VENDER' | 'VENDAS' | 'CLIENTES' | 'MEU_DESEMPENHO' | 'GESTAO' | 'PRODUTOS' | 'ESTOQUE' | 'PROMOCOES' | 'ANALISES'
  | 'USUARIOS' | 'TROCAR_SENHA';

/** Tela da propria senha: unica liberada enquanto a senha provisoria nao for trocada (area TROCAR_SENHA). */
export const PASSWORD_PATH = '/conta/senha';

export type MenuSection = 'GESTAO' | 'ATENDIMENTO' | 'MEU_ESPACO';

export interface MenuItem { area: Area; href: string; label: string; section: MenuSection; icon: IconName }

// A ordem do array decide a tela inicial (landingPath); a exibicao segue MENU_SECTIONS (manual de marca v1.0).
export const MENU: readonly MenuItem[] = [
  { area: 'VENDER', href: '/vender', label: 'Vender', section: 'ATENDIMENTO', icon: 'vender' },
  { area: 'VENDAS', href: '/vendas', label: 'Vendas', section: 'ATENDIMENTO', icon: 'vendas' },
  { area: 'CLIENTES', href: '/clientes', label: 'Clientes', section: 'ATENDIMENTO', icon: 'clientes' },
  { area: 'MEU_DESEMPENHO', href: '/meu-desempenho', label: 'Meu desempenho', section: 'MEU_ESPACO', icon: 'desempenho' },
  { area: 'GESTAO', href: '/gestao', label: 'Visão geral', section: 'GESTAO', icon: 'visao-geral' },
  { area: 'PRODUTOS', href: '/gestao/produtos', label: 'Produtos e preços', section: 'GESTAO', icon: 'preco' },
  { area: 'PROMOCOES', href: '/gestao/promocoes', label: 'Promoções', section: 'GESTAO', icon: 'promocao' },
  { area: 'ESTOQUE', href: '/gestao/estoque', label: 'Estoque', section: 'GESTAO', icon: 'estoque' },
  { area: 'ANALISES', href: '/gestao/analises', label: 'Análises', section: 'GESTAO', icon: 'analises' },
  { area: 'USUARIOS', href: '/gestao/usuarios', label: 'Usuários', section: 'GESTAO', icon: 'usuarios' },
];

export const MENU_SECTIONS: readonly { section: MenuSection; label: string }[] = [
  { section: 'GESTAO', label: 'Gestão' },
  { section: 'ATENDIMENTO', label: 'Atendimento' },
  { section: 'MEU_ESPACO', label: 'Meu espaço' },
];

/** Link fixo de MEU ESPACO: a troca da propria senha vale para qualquer conta autenticada. */
export const PASSWORD_LINK = { href: PASSWORD_PATH, label: 'Trocar senha', icon: 'senha' } as const satisfies { href: string; label: string; icon: IconName };

export interface MenuGroup { section: MenuSection; label: string; items: { href: string; label: string; icon: IconName }[] }

/** Itens liberados agrupados nas secoes do menu, sem secao vazia; Trocar senha sempre fecha MEU ESPACO. */
export function menuGroups(items: readonly MenuItem[]): MenuGroup[] {
  return MENU_SECTIONS.map(({ section, label }) => ({
    section, label,
    items: [
      ...items.filter(item => item.section === section).map(({ href, label: itemLabel, icon }) => ({ href, label: itemLabel, icon })),
      ...(section === 'MEU_ESPACO' ? [{ ...PASSWORD_LINK }] : []),
    ],
  })).filter(group => group.items.length > 0);
}

/** Menu na ordem fixa, filtrado pelas areas do usuario. */
export function menuFor(areas: readonly string[], available: ReadonlySet<Area> = new Set(MENU.map(item => item.area))): MenuItem[] {
  return MENU.filter(item => areas.includes(item.area) && available.has(item.area));
}

/** Primeira tela do usuario depois do login (respeita ?next= quando e uma rota interna). */
export function landingPath(areas: readonly string[], next?: string | null): string {
  if (areas.includes('TROCAR_SENHA')) return PASSWORD_PATH;
  if (next && next.startsWith('/') && !next.startsWith('//') && !next.startsWith('/api/') && !next.startsWith('/entrar')) return next;
  return menuFor(areas)[0]?.href ?? '/entrar';
}
