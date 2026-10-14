'use client';

/** Paginacao do contrato v4 (page comeca em 0, C-18). */
export default function Pager({ page, size, total, onPage }: { page: number; size: number; total: number; onPage: (page: number) => void }) {
  const pages = Math.max(1, Math.ceil(total / size));
  if (total <= size) return null;
  return (
    <nav className="pager" aria-label="Paginação">
      <button type="button" className="btn" disabled={page === 0} onClick={() => onPage(page - 1)}>Anterior</button>
      <span className="num" aria-live="polite">Página {page + 1} de {pages}</span>
      <button type="button" className="btn" disabled={page + 1 >= pages} onClick={() => onPage(page + 1)}>Próxima</button>
    </nav>
  );
}
