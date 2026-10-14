'use client';
import { useEffect, useId, useRef, type ReactNode } from 'react';

/** Painel lateral com <dialog> nativo: foco preso, Esc fecha e o foco volta para quem abriu. */
export default function Drawer({ open, title, onClose, children }: { open: boolean; title: string; onClose: () => void; children: ReactNode }) {
  const ref = useRef<HTMLDialogElement>(null);
  const id = useId();
  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);
  return (
    <dialog ref={ref} className="drawer" aria-labelledby={id} onClose={onClose}>
      <div className="stack">
        <div className="page-head">
          <h2 id={id}>{title}</h2>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Fechar</button>
        </div>
        {open && children}
      </div>
    </dialog>
  );
}
