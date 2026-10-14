// Icones de traco do SBI: grade de 24, traco de 1,75 com pontas arredondadas, cor do texto (currentColor).
// Os seis do manual v1.0 e sete derivados na mesma construcao. Fica em .ts para os testes unitarios.

export type IconName =
  | 'visao-geral' | 'vender' | 'estoque' | 'preco' | 'promocao' | 'analises'
  | 'clientes' | 'vendas' | 'desempenho' | 'usuarios' | 'senha' | 'sair' | 'check';

export const MANUAL_ICONS: readonly IconName[] = ['visao-geral', 'vender', 'estoque', 'preco', 'promocao', 'analises'];
export const DERIVED_ICONS: readonly IconName[] = ['clientes', 'vendas', 'desempenho', 'usuarios', 'senha', 'sair', 'check'];

export const ICON_VIEWBOX = '0 0 24 24';
export const ICON_STROKE = 1.75;

export const ICON_PATHS: Readonly<Record<IconName, readonly string[]>> = {
  'visao-geral': ['M4 4h7v7H4z', 'M13 4h7v4h-7z', 'M13 10h7v10h-7z', 'M4 13h7v7H4z'],
  vender: ['M4 5h2l2 10h10l2-7H7.2', 'M10 19.5a1 1 0 1 0 0 .01', 'M17 19.5a1 1 0 1 0 0 .01'],
  estoque: ['M12 3l8 4.5v9L12 21l-8-4.5v-9z', 'M4 7.5l8 4.5 8-4.5', 'M12 12v9'],
  preco: ['M3.5 12.5l8-8H20v8.5l-8 8z', 'M15.5 8.5a1 1 0 1 0 0 .01'],
  promocao: ['M5 19L19 5', 'M7.5 9a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z', 'M16.5 18a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z'],
  analises: ['M4 20h16', 'M6 16l4-5 3 3 5-7', 'M18 7h-3', 'M18 7v3'],
  clientes: ['M9 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z', 'M3 20c.6-3.4 3-5.5 6-5.5s5.4 2.1 6 5.5', 'M16 4.5a3.5 3.5 0 0 1 0 6.5', 'M18 14.8c1.6.8 2.7 2.6 3 5.2'],
  vendas: ['M6 3h12v18l-3-2-3 2-3-2-3 2z', 'M9 8h6', 'M9 12h6', 'M9 16h3'],
  desempenho: ['M12 20a8 8 0 1 0-8-8', 'M12 12l4-4', 'M4 20l3-3'],
  usuarios: ['M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z', 'M4 21c.8-4 4-6.5 8-6.5s7.2 2.5 8 6.5'],
  senha: ['M6 11h12v9H6z', 'M8.5 11V8a3.5 3.5 0 0 1 7 0v3', 'M12 15v2'],
  sair: ['M14 4h5v16h-5', 'M10 8l-4 4 4 4', 'M6 12h10'],
  check: ['M5 12.5l4.5 4.5L19 7.5'],
};

/** Icone decorativo por padrao (o texto ao lado ja nomeia a acao); com rotulo, vira imagem nomeada. */
export function iconA11y(label?: string): Record<string, string | boolean> {
  return label ? { role: 'img', 'aria-label': label } : { 'aria-hidden': true, focusable: false };
}
