'use client';
import { useCallback, useState } from 'react';

/** Confirmacao do que acabou de ser salvo; a proxima acao substitui a mensagem. */
export function useStatusMessage() {
  const [message, setMessage] = useState('');
  const clear = useCallback(() => setMessage(''), []);
  return { message, show: setMessage, clear };
}

/** Regiao viva sempre montada: o leitor de tela anuncia a mensagem quando ela muda (WCAG 4.1.3). */
export function StatusMessage({ message, onClose }: { message: string; onClose: () => void }) {
  return (
    <div role="status" className="status-slot">
      {message && <p className="notice notice-ok status-message">
        <span>{message}</span>
        <button type="button" className="btn btn-ghost btn-compact" onClick={onClose}>Fechar aviso</button>
      </p>}
    </div>
  );
}
