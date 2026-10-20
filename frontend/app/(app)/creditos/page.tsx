import type { Metadata } from 'next';
import { PRODUCT_PHOTOS } from '../../../lib/product-photos.ts';

export const metadata: Metadata = { title: 'Créditos das imagens' };

/** Atribuicao das fotos de produto (Wikimedia Commons), exigida pelas licencas CC BY e CC BY-SA. */
export default function CreditosPage() {
  return (
    <div className="stack">
      <header className="page-head">
        <div>
          <h1>Créditos das imagens</h1>
          <p>Fotos de produto do Wikimedia Commons, reduzidas e recortadas para miniatura. Produto sem foto livre mostra o ícone da linha.</p>
        </div>
      </header>
      <section className="card">
        <div className="table-wrap">
          <table>
            <thead><tr><th scope="col">Imagem</th><th scope="col">Usada em</th><th scope="col">Autor</th><th scope="col">Licença</th></tr></thead>
            <tbody>{PRODUCT_PHOTOS.map(photo => (
              <tr key={photo.file}>
                <td><div className="product-cell">
                  <img className="thumb" src={`/produtos/${photo.file}`} alt="" width={40} height={40} loading="lazy" decoding="async" />
                  <a href={photo.page} rel="noreferrer">{photo.title}</a></div></td>
                <td>{photo.names.join(', ')}</td>
                <td>{photo.author}</td>
                <td>{photo.licenseUrl ? <a href={photo.licenseUrl} rel="noreferrer">{photo.license}</a> : photo.license}</td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      </section>
    </div>
  );
}
