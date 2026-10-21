'use client';
import BrandScreen from '../components/brand-screen.tsx';

/** Falha inesperada ao montar a tela: mensagem da voz SBI, sem codigo tecnico, com nova tentativa. */
export default function ErrorPage({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return (
    <main>
      <BrandScreen>
        <h1>Não foi possível carregar esta tela.</h1>
        <p className="muted">Se o sistema acabou de ser aberto, o servidor pode estar iniciando: aguarde um minuto e tente de novo.</p>
        <div className="actions">
          <button type="button" className="btn btn-primary" onClick={reset}>Tentar de novo</button>
          <a className="btn" href="/">Voltar ao início</a>
        </div>
      </BrandScreen>
    </main>
  );
}
