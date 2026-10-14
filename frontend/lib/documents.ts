// Digito verificador de CPF e CNPJ na tela (a API confere de novo com @CPF/@CNPJ).
import { digits } from './format.ts';

export function isValidCpf(value: string): boolean {
  const d = digits(value);
  if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) return false;
  for (const size of [9, 10]) {
    let sum = 0;
    for (let i = 0; i < size; i++) sum += Number(d[i]) * (size + 1 - i);
    if (((sum * 10) % 11) % 10 !== Number(d[size])) return false;
  }
  return true;
}

/** CNPJ sem mascara e em maiusculas: numerico ou alfanumerico (12 posicoes + 2 digitos verificadores). */
export function cnpjKey(value: string): string {
  return value.replace(/[^0-9A-Za-z]/g, '').toUpperCase();
}

/** CNPJ numerico ou alfanumerico (Receita, a partir de 07/2026): modulo 11 com valor ASCII - 48 de cada posicao. */
export function isValidCnpj(value: string): boolean {
  const d = cnpjKey(value);
  if (!/^[0-9A-Z]{12}\d{2}$/.test(d) || /^(\d)\1{13}$/.test(d)) return false;
  const weights = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  for (const size of [12, 13]) {
    let sum = 0;
    for (let i = 0; i < size; i++) sum += (d.charCodeAt(i) - 48) * (weights[i + 13 - size] ?? 0);
    const rest = sum % 11;
    if ((rest < 2 ? 0 : 11 - rest) !== Number(d[size])) return false;
  }
  return true;
}
