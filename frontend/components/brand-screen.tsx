import type { ReactNode } from 'react';
import { SbiLogo } from './brand/logo.tsx';

/** Assinatura vertical sobre Nevoa, sem animacao (manual v1.0): carregamento, pagina nao encontrada e falha. */
export default function BrandScreen({ inline = false, children }: { inline?: boolean; children: ReactNode }) {
  return (
    <div className={`brand-screen${inline ? ' brand-screen-inline' : ''}`}>
      <SbiLogo layout="vertical" size={56} />
      {children}
    </div>
  );
}
