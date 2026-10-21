'use client';
import { useEffect, useRef } from 'react';

/**
 * <dialog> modal controlado por estado: showModal() ao abrir (foco preso, Esc e o gesto de voltar fecham) e close()
 * ao fechar. Usado pelo painel lateral, pela confirmacao e pelos cadastros de cliente.
 */
export function useModal(open: boolean, onOpen?: () => void) {
  const ref = useRef<HTMLDialogElement>(null);
  const opened = useRef(onOpen);
  opened.current = onOpen;
  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      dialog.showModal();
      opened.current?.();
    }
    if (!open && dialog.open) dialog.close();
  }, [open]);
  return ref;
}
