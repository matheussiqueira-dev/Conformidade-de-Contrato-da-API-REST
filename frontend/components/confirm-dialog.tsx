'use client';
import { useId, useRef, type ReactNode } from 'react';
import { useModal } from './use-modal.ts';

/**
 * Confirmacao antes de acao sem volta (cancelar pedido, encerrar promocao, inativar produto): <dialog> nativo em
 * modo modal, foco inicial em "manter" (o caminho seguro), Esc e o gesto de voltar fecham sem executar.
 */
export default function ConfirmDialog({ open, title, confirmLabel, cancelLabel, onConfirm, onCancel, children }: {
  open: boolean; title: string; confirmLabel: string; cancelLabel: string;
  onConfirm: () => void; onCancel: () => void; children: ReactNode;
}) {
  const keep = useRef<HTMLButtonElement>(null);
  const ref = useModal(open, () => keep.current?.focus());
  const id = useId();
  return (
    <dialog ref={ref} className="confirm" role="alertdialog" aria-labelledby={`${id}-titulo`} aria-describedby={`${id}-texto`} onClose={onCancel}>
      <div className="stack-sm">
        <h2 id={`${id}-titulo`}>{title}</h2>
        <div id={`${id}-texto`} className="stack-sm">{children}</div>
        <div className="actions confirm-actions">
          <button ref={keep} type="button" className="btn" onClick={onCancel}>{cancelLabel}</button>
          <button type="button" className="btn btn-danger-solid" onClick={onConfirm}>{confirmLabel}</button>
        </div>
      </div>
    </dialog>
  );
}
