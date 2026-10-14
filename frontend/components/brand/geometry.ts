// Geometria do simbolo SBI (manual de marca v1.0): quadrado de 48 com raio 12 em Indigo, tres barras brancas
// crescentes com raio 2 e o ponto Menta no alto da terceira barra. E a mesma de app/icon.svg (favicon).
// Fica em .ts para os testes unitarios (node --test) lerem sem JSX.

export const BRAND_NAME = 'SBI';
export const BRAND_DESCRIPTOR = 'Super Business Intelligence';
export const BRAND_ACCESSIBLE_NAME = 'SBI — Super Business Intelligence';

export const COLORS = {
  indigo: '#3D4ED6',
  indigoDeep: '#2B39A8',
  ink: '#141B26',
  mint: '#5BD6B8',
  white: '#FFFFFF',
  slate: '#556070',
} as const;

export const SYMBOL_SIZE = 48;
export const SYMBOL_RADIUS = 12;

export interface Bar { x: number; y: number; width: number; height: number; rx: number }

export const BARS: readonly Bar[] = [
  { x: 11, y: 27, width: 7, height: 10, rx: 2 },
  { x: 20.5, y: 21, width: 7, height: 16, rx: 2 },
  { x: 30, y: 17, width: 7, height: 20, rx: 2 },
];

export const DOT = { cx: 33.5, cy: 10.5, r: 3.5 } as const;

export type LogoTone = 'principal' | 'negativa' | 'indigo' | 'mono';

/** Cores do simbolo e do texto por variante: principal (fundo claro), negativa (sobre Tinta), sobre Indigo, monocromatica. */
export function toneColors(tone: LogoTone): { square: string; bars: string; dot: string; text: string; descriptor: string } {
  switch (tone) {
    case 'negativa': return { square: COLORS.indigo, bars: COLORS.white, dot: COLORS.mint, text: COLORS.white, descriptor: '#C9CEDA' };
    case 'indigo': return { square: COLORS.white, bars: COLORS.indigo, dot: COLORS.mint, text: COLORS.white, descriptor: COLORS.white };
    case 'mono': return { square: 'currentColor', bars: COLORS.white, dot: COLORS.white, text: 'currentColor', descriptor: 'currentColor' };
    default: return { square: COLORS.indigo, bars: COLORS.white, dot: COLORS.mint, text: COLORS.ink, descriptor: COLORS.slate };
  }
}
