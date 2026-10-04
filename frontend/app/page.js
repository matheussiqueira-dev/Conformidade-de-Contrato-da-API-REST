import AccessPanel from './access-panel.js';

export default function Home() {
  return <main className="access-layout">
    <section className="store-context" aria-labelledby="brand-title">
      <p className="brand">Loja <span>•</span> Gestão</p>
      <div className="context-copy"><p className="eyebrow">O DIA A DIA DA LOJA</p>
        <h1 id="brand-title">Cada venda começa<br />com a sua equipe.</h1>
        <p>Um lugar para acompanhar o trabalho e cuidar de cada atendimento.</p>
      </div>
      <p className="context-footer">Acesso individual · Gerente e vendedor</p>
    </section>
    <section className="access-surface" aria-label="Acesso ao sistema"><AccessPanel /></section>
  </main>;
}
