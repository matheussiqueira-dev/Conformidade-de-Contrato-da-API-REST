// Formatacao pt-BR para a tela. Valores monetarios chegam como Money {amount, currency: 'BRL'} (C-05).
export interface Money { amount: number; currency: string }

const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const date = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeZone: 'America/Fortaleza' });
const time = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23', timeZone: 'America/Fortaleza' });
const dayMonth = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', timeZone: 'America/Fortaleza' });

/** Manual de marca v1.0: dado ausente e escrito por extenso, nunca zero inventado. */
export const MISSING = 'Não informado';
const brlCompact = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', notation: 'compact', maximumFractionDigits: 1 });
const monthName = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric', timeZone: 'America/Fortaleza' });
const percent = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 });

/** Espaco nao separavel do Intl vira espaco comum para leitores de tela e testes. */
function plain(text: string): string {
  return text.replace(/ /g, ' ');
}

export function money(value: Money | number | null | undefined): string {
  if (value === null || value === undefined) return MISSING;
  return plain(brl.format(typeof value === 'number' ? value : value.amount));
}

/** Valor curto para rotulo de grafico: "R$ 47 mil", "R$ 1,2 mi". */
export function compactMoney(value: Money | number): string {
  return plain(brlCompact.format(typeof value === 'number' ? value : value.amount));
}

/** Mes de referencia (YYYY-MM) por extenso: "outubro de 2026". */
export function formatMonth(period: string): string {
  return monthName.format(new Date(`${period}-15T12:00:00-03:00`));
}

/** Data e hora da loja: "14/10/2026 14h30". */
export function formatDateTime(iso: string): string {
  return `${date.format(new Date(iso))} ${formatTime(iso)}`;
}

/** Hora local da loja no formato do manual: "14h30" (usada em "Bloqueado até 10h15"). */
export function formatTime(iso: string): string {
  return time.format(new Date(iso)).replace(':', 'h');
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
  return value === null || value === undefined ? MISSING : `${percent.format(value)}%`;
}

/** Periodo de datas (YYYY-MM-DD): "01/09 a 30/09/2026"; com anos diferentes, as duas datas completas. */
export function formatPeriod(from: string, to: string): string {
  const start = new Date(`${from}T12:00:00-03:00`);
  if (from.slice(0, 4) !== to.slice(0, 4)) return `${formatDate(from)} a ${formatDate(to)}`;
  return `${dayMonth.format(start)} a ${formatDate(to)}`;
}

/** Iniciais do avatar: primeira letra do primeiro e do ultimo nome ("Allan Lima" -> "AL"). */
export function initials(name: string): string {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return '?';
  const first = parts[0]!.charAt(0);
  const last = parts.length > 1 ? parts[parts.length - 1]!.charAt(0) : '';
  return `${first}${last}`.toLocaleUpperCase('pt-BR');
}

export const ORDER_STATUS: Readonly<Record<string, string>> = {
  PENDING_PAYMENT: 'Aguardando pagamento',
  PROCESSING: 'Em processamento',
  PAID: 'Pago',
  SHIPPED: 'Enviado',
  DELIVERED: 'Entregue',
  CANCELED: 'Cancelado',
};

/** Selo de estado do pedido (sempre com texto): pago e entregue ok, aguardando em aviso, cancelado em erro, demais informativos. */
export function statusPill(status: string): string {
  if (status === 'PAID' || status === 'SHIPPED' || status === 'DELIVERED') return 'pill pill-ok';
  if (status === 'PENDING_PAYMENT') return 'pill pill-warn';
  if (status === 'CANCELED') return 'pill pill-err';
  return 'pill pill-info';
}

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
