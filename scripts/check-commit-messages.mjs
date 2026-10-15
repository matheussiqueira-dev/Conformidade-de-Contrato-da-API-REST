// Barreira de higiene: mensagens de commit sem atribuicao a ferramentas ou servicos externos.
// Uso: node scripts/check-commit-messages.mjs <arquivo com as mensagens>
//   CI (pull_request): git log --format=%B "origin/${GITHUB_BASE_REF}..HEAD" > msgs.txt
//   hook commit-msg local: o Git passa o arquivo da mensagem como primeiro argumento.
// Linhas que comecam com "#" sao comentarios do Git e ficam de fora. Sai com 1 se encontrar algo.
// Os nomes de produto ficam em base64 para que o proprio verificador passe na varredura de termos do
// repositorio (decisao D-PUB-02); a mensagem de erro cita so a linha e a regra, sem repetir o termo.
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const PLAIN_TERMS = ['Co-Authored-By', 'Generated with', 'AI-generated', 'assistant'];
const ENCODED_TERMS = ['Q2xhdWRl', 'QW50aHJvcGlj', 'Q2hhdEdQVA==', 'T3BlbkFJ', 'Q29kZXg=', 'Q29waWxvdA=='];

export const FORBIDDEN_TERMS = [...PLAIN_TERMS, ...ENCODED_TERMS.map(term => Buffer.from(term, 'base64').toString('utf8'))];

const escape = text => text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
const TERM_RULES = FORBIDDEN_TERMS.map((term, index) => ({
  rule: `termo ${index + 1}`,
  pattern: new RegExp(`(?<![A-Za-z0-9])${escape(term)}(?![A-Za-z0-9])`, 'i'),
}));
const SESSION_RULES = [
  { rule: 'link de sessao', pattern: /https?:\/\/\S*session/i },
  { rule: 'identificador de sessao', pattern: /\bsession_[A-Za-z0-9]{6,}/i },
];
const RULES = [...TERM_RULES, ...SESSION_RULES];

/** Linhas violadas: [{ line, rule }], na ordem do texto. Texto vazio passa. */
export function findViolations(text) {
  const violations = [];
  text.split(/\r?\n/).forEach((content, index) => {
    if (content.startsWith('#')) return;
    for (const { rule, pattern } of RULES) {
      if (pattern.test(content)) violations.push({ line: index + 1, rule });
    }
  });
  return violations;
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const file = process.argv[2];
  if (!file) {
    console.error('Uso: node scripts/check-commit-messages.mjs <arquivo com as mensagens>');
    process.exit(2);
  }
  const violations = findViolations(readFileSync(file, 'utf8'));
  for (const { line, rule } of violations) {
    console.error(`check-commit-messages: linha ${line} tem atribuicao proibida (${rule}).`);
  }
  if (violations.length) {
    console.error('Mensagens de commit seguem Conventional Commits, sem rodape de coautoria nem credito a ferramenta.');
    process.exit(1);
  }
  console.log('check-commit-messages: mensagens limpas.');
}
