import type { Metadata } from 'next';
import BrandScreen from '../components/brand-screen.tsx';

export const metadata: Metadata = { title: 'Página não encontrada' };

export default function NotFound() {
  return (
    <main>
      <BrandScreen>
        <h1>Página não encontrada.</h1>
        <p className="muted">O endereço pode ter mudado. Volte para o início e siga pelo menu.</p>
        <a className="btn btn-primary" href="/">Voltar ao início</a>
      </BrandScreen>
    </main>
  );
}
