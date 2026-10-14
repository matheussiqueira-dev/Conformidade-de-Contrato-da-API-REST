// Formatacao pt-BR para a tela. Valores monetarios chegam como Money {amount, currency: 'BRL'} (C-05).
export interface Money { amount: number; currency: string }

const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dateTime = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short', timeZone: 'America/Fortaleza' });
const date = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeZone: 'America/Fortaleza' });
const time = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit', timeZone: 'America/Fortaleza' });
const percent = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });

/** Espaco nao separavel do Intl vira espaco comum para leitores de tela e testes. */
function plain(text: string): string {
  return text.replace(/ /g, ' ');
}

export function money(value: Money | number | null | undefined): string {
  if (value === null || value === undefined) return '—';
  return plain(brl.format(typeof value === 'number' ? value : value.amount));
}

export function formatDateTime(iso: string): string {
  return dateTime.format(new Date(iso));
}

/** Hora local da loja (HH:MM), usada em "Bloqueado até 10:15". */
export function formatTime(iso: string): string {
  return time.format(new Date(iso));
}

const storeDay = new Intl.DateTimeFormat('en-CA', { year: 'numeric', month: '2-digit', day: '2-digit', timeZone: 'America/Fortaleza' });

/** Dias de calendario da loja desde o instante: "hoje", "há 1 dia", "há N dias". */
export function daysSince(iso: string, now: Date = new Date()): string {
  const day = (value: Date) => Date.parse(`${storeDay.format(value)}T00:00:00Z`);
  const days = Math.max(0, Math.round((day(now) - day(new Date(iso))) / 86_400_000));
  return days === 0 ? 'hoje' : days === 1 ? 'há 1 dia' : `há ${days} dias`;
}

export function formatDate(isoDate: string): string {
  // Data sem hora (YYYY-MM-DD) e lida ao meio-dia para nao mudar de dia com o fuso.
  return date.format(new Date(`${isoDate}T12:00:00-03:00`));
}

export function formatPercent(value: number | null | undefined): string {
  return value === null || value === undefined ? '—' : `${percent.format(value)}%`;
}

export const ORDER_STATUS: Readonly<Record<string, string>> = {
  PENDING_PAYMENT: 'Aguardando pagamento',
  PROCESSING: 'Em processamento',
  PAID: 'Pago',
  SHIPPED: 'Enviado',
  DELIVERED: 'Entregue',
  CANCELED: 'Cancelado',
};

export const PAYMENT_TYPE: Readonly<Record<string, string>> = { PIX: 'Pix', CARD: 'Cartão', BOLETO: 'Boleto' };

export function label(map: Readonly<Record<string, string>>, key: string): string {
  return map[key] ?? key;
}

/** Remove tudo que nao e digito (CPF, CNPJ, CEP, telefone). */
export function digits(value: string): string {
  return value.replace(/\D/g, '');
}

/** "1.234,56", "1234,56" e "1234.56" -> 1234.56. Com virgula, os pontos sao separadores de milhar. */
export function parseDecimal(value: string): number {
  const text = value.trim().replace(/\s/g, '');
  return Number(text.includes(',') ? text.replace(/\./g, '').replace(',', '.') : text);
}
