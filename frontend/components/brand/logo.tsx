// Simbolo e logo do SBI (manual de marca v1.0). Nome acessivel unico: "SBI — Super Business Intelligence".
import { BARS, BRAND_ACCESSIBLE_NAME, BRAND_DESCRIPTOR, BRAND_NAME, DOT, SYMBOL_RADIUS, SYMBOL_SIZE, toneColors, type LogoTone } from './geometry.ts';

interface SymbolProps { size?: number; tone?: LogoTone; decorative?: boolean }

/** Simbolo isolado. Decorativo quando acompanha o nome por extenso; senao, imagem com o nome acessivel. */
export function SbiSymbol({ size = 36, tone = 'principal', decorative = false }: SymbolProps) {
  const c = toneColors(tone);
  const a11y = decorative ? { 'aria-hidden': true as const, focusable: false as const } : { role: 'img' as const, 'aria-label': BRAND_ACCESSIBLE_NAME };
  return (
    <svg width={size} height={size} viewBox={`0 0 ${SYMBOL_SIZE} ${SYMBOL_SIZE}`} xmlns="http://www.w3.org/2000/svg" className="sbi-symbol" {...a11y}>
      <rect width={SYMBOL_SIZE} height={SYMBOL_SIZE} rx={SYMBOL_RADIUS} fill={c.square} />
      {BARS.map(bar => <rect key={bar.x} x={bar.x} y={bar.y} width={bar.width} height={bar.height} rx={bar.rx} fill={c.bars} />)}
      <circle cx={DOT.cx} cy={DOT.cy} r={DOT.r} fill={c.dot} />
    </svg>
  );
}

type Layout = 'horizontal' | 'descritor' | 'vertical';

/** Logo: horizontal (simbolo + SBI), com descritor (Super Business Intelligence abaixo do nome) ou vertical. */
export function SbiLogo({ layout = 'horizontal', tone = 'principal', size = 36 }: { layout?: Layout; tone?: LogoTone; size?: number }) {
  const c = toneColors(tone);
  return (
    <span className={`sbi-logo sbi-logo-${layout}`} role="img" aria-label={BRAND_ACCESSIBLE_NAME} style={{ color: c.text }}>
      <SbiSymbol size={size} tone={tone} decorative />
      <span className="sbi-logo-text" aria-hidden="true">
        <span className="sbi-logo-name" style={{ fontSize: Math.round(size * 0.72) }}>{BRAND_NAME}</span>
        {layout !== 'horizontal' && <span className="sbi-logo-descriptor" style={{ color: c.descriptor }}>{BRAND_DESCRIPTOR}</span>}
      </span>
    </span>
  );
}
