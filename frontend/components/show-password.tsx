'use client';
import { useId } from 'react';

/** Caixa "Mostrar senha": deixa conferir o que foi digitado antes de enviar; desmarcada por padrao. */
export default function ShowPassword({ checked, onChange, label = 'Mostrar senha', disabled = false }: {
  checked: boolean; onChange: (checked: boolean) => void; label?: string; disabled?: boolean;
}) {
  const id = useId();
  return (
    <label className="check" htmlFor={id}>
      <input id={id} type="checkbox" checked={checked} disabled={disabled} onChange={event => onChange(event.target.checked)} />
      <span>{label}</span>
    </label>
  );
}
