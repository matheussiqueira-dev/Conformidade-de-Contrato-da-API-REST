// Conferencia da troca de senha antes do envio (politica 4.5). A lista de senhas comuns fica so no servidor,
// que responde 400 VALIDATION_FAILED; aqui ficam as regras que nao dependem dela.
export type PasswordField = 'currentPassword' | 'newPassword' | 'confirmation';

export interface PasswordValues { currentPassword: string; newPassword: string; confirmation: string }

const MIN = 12;
const MAX_BYTES = 72;

export function passwordProblems(values: PasswordValues, email: string): Partial<Record<PasswordField, string>> {
  const problems: Partial<Record<PasswordField, string>> = {};
  if (!values.currentPassword) problems.currentPassword = 'Informe a senha atual.';
  const length = [...values.newPassword].length;
  if (length < MIN) problems.newPassword = `Use pelo menos ${MIN} caracteres.`;
  else if (new TextEncoder().encode(values.newPassword).length > MAX_BYTES) problems.newPassword = 'Senha longa demais: use até 72 caracteres (letras acentuadas contam em dobro).';
  else if (values.newPassword.trim().toLowerCase() === email.trim().toLowerCase()) problems.newPassword = 'A senha não pode ser o seu e-mail.';
  else if (values.newPassword === values.currentPassword) problems.newPassword = 'A nova senha precisa ser diferente da atual.';
  if (values.confirmation !== values.newPassword) problems.confirmation = 'As duas senhas não conferem.';
  return problems;
}
