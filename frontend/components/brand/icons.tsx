// Icone de traco do SBI. Decorativo por padrao; passe label so quando o icone estiver sem texto ao lado.
import { ICON_PATHS, ICON_STROKE, ICON_VIEWBOX, iconA11y, type IconName } from './icons-data.ts';

export type { IconName } from './icons-data.ts';

export function Icon({ name, size = 20, label }: { name: IconName; size?: number; label?: string }) {
  return (
    <svg width={size} height={size} viewBox={ICON_VIEWBOX} fill="none" stroke="currentColor" strokeWidth={ICON_STROKE}
         strokeLinecap="round" strokeLinejoin="round" xmlns="http://www.w3.org/2000/svg" className="icon" {...iconA11y(label)}>
      {ICON_PATHS[name].map(d => <path key={d} d={d} />)}
    </svg>
  );
}
