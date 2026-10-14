import type { Metadata, Viewport } from 'next';
import type { ReactNode } from 'react';
import { IBM_Plex_Mono, IBM_Plex_Sans, Space_Grotesk } from 'next/font/google';
import Providers from './providers.tsx';
import './brand-tokens.css';
import './globals.css';

// Manual de marca v1.0: Space Grotesk nos titulos e no logo, Plex Sans no texto, Plex Mono 500 nos valores e IDs.
const spaceGrotesk = Space_Grotesk({ subsets: ['latin'], weight: ['500', '600', '700'], variable: '--font-space-grotesk', display: 'swap' });
const plexSans = IBM_Plex_Sans({ subsets: ['latin'], weight: ['400', '500', '600'], variable: '--font-plex-sans', display: 'swap' });
const plexMono = IBM_Plex_Mono({ subsets: ['latin'], weight: ['500'], variable: '--font-plex-mono', display: 'swap' });

export const metadata: Metadata = {
  title: { default: 'SBI — Super Business Intelligence', template: '%s · SBI' },
  description: 'Gestão de loja com vendas, estoque e metas em um só lugar.',
  robots: { index: false, follow: false },
};

export const viewport: Viewport = { themeColor: '#141B26' };

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="pt-BR" className={`${spaceGrotesk.variable} ${plexSans.variable} ${plexMono.variable}`}>
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
