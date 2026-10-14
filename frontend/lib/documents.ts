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

export function isValidCnpj(value: string): boolean {
  const d = digits(value);
  if (d.length !== 14 || /^(\d)\1{13}$/.test(d)) return false;
  const weights = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  for (const size of [12, 13]) {
    let sum = 0;
    for (let i = 0; i < size; i++) sum += Number(d[i]) * (weights[i + 13 - size] ?? 0);
    const rest = sum % 11;
    if ((rest < 2 ? 0 : 11 - rest) !== Number(d[size])) return false;
  }
  return true;
}
