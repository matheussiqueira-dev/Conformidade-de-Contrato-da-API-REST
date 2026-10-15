// Barreira de higiene: mensagens de commit sem atribuicao a ferramentas ou servicos externos e nenhum
// endereco antigo do site no repositorio (o unico endereco oficial e https://sbi-app-alpha.vercel.app).
// Uso: node scripts/check-commit-messages.mjs <arquivo com as mensagens>
//   CI (pull_request): git log --format=%B "origin/${GITHUB_BASE_REF}..HEAD" > msgs.txt
//   hook commit-msg local: o Git passa o arquivo da mensagem como primeiro argumento.
// Uso: node scripts/check-commit-messages.mjs --tracked
//   varre os arquivos de texto versionados (git ls-files) atras de endereco antigo do site.
// Linhas que comecam com "#" sao comentarios do Git e ficam de fora. Sai com 1 se encontrar algo.
// Os nomes proibidos ficam em base64 para que o proprio verificador passe na varredura de termos do
// repositorio (decisoes D-PUB-02 e D-PUB-11); a mensagem de erro cita so a linha e a regra, sem repetir o termo.
import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

const decode = term => Buffer.from(term, 'base64').toString('utf8');
const PLAIN_TERMS = ['Co-Authored-By', 'Generated with', 'AI-generated', 'assistant'];
const ENCODED_TERMS = ['Q2xhdWRl', 'QW50aHJvcGlj', 'Q2hhdEdQVA==', 'T3BlbkFJ', 'Q29kZXg=', 'Q29waWxvdA=='];
const ENCODED_OLD_SITES = ['YnVzaW5lc3MtaW50ZWxsaWdlbmNlLWVpZ2h0'];

export const OLD_SITE_TERMS = ENCODED_OLD_SITES.map(decode);
export const FORBIDDEN_TERMS = [...PLAIN_TERMS, ...ENCODED_TERMS.map(decode), ...OLD_SITE_TERMS];

const escape = text => text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
const termRule = (term, rule) => ({ rule, pattern: new RegExp(`(?<![A-Za-z0-9])${escape(term)}(?![A-Za-z0-9])`, 'i') });
const OLD_SITE_RULES = OLD_SITE_TERMS.map((term, index) => termRule(term, `endereco antigo do site ${index + 1}`));
const TERM_RULES = [...PLAIN_TERMS, ...ENCODED_TERMS.map(decode)].map((term, index) => termRule(term, `termo ${index + 1}`));
const SESSION_RULES = [
  { rule: 'link de sessao', pattern: /https?:\/\/\S*session/i },
  { rule: 'identificador de sessao', pattern: /\bsession_[A-Za-z0-9]{6,}/i },
];
const MESSAGE_RULES = [...TERM_RULES, ...OLD_SITE_RULES, ...SESSION_RULES];

function scan(text, rules) {
  const violations = [];
  text.split(/\r?\n/).forEach((content, index) => {
    if (content.startsWith('#')) return;
    for (const { rule, pattern } of rules) {
      if (pattern.test(content)) violations.push({ line: index + 1, rule });
    }
  });
  return violations;
}

/** Linhas violadas numa mensagem de commit: [{ line, rule }], na ordem do texto. Texto vazio passa. */
export function findViolations(text) {
  return scan(text, MESSAGE_RULES);
}

/** Linhas de um arquivo versionado com endereco antigo do site (comentarios incluidos). */
export function findOldSiteReferences(text) {
  const violations = [];
  text.split(/\r?\n/).forEach((content, index) => {
    for (const { rule, pattern } of OLD_SITE_RULES) {
      if (pattern.test(content)) violations.push({ line: index + 1, rule });
    }
  });
  return violations;
}

function checkTrackedFiles() {
  const files = execFileSync('git', ['ls-files', '-z'], { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 }).split('\0').filter(Boolean);
  let found = 0;
  for (const file of files) {
    let buffer;
    try { buffer = readFileSync(file); } catch { continue; }
    if (buffer.includes(0)) continue;
    for (const { line, rule } of findOldSiteReferences(buffer.toString('utf8'))) {
      console.error(`check-commit-messages: ${file}:${line} tem ${rule}.`);
      found++;
    }
  }
  if (found) {
    console.error('O unico endereco do site e https://sbi-app-alpha.vercel.app.');
    process.exit(1);
  }
  console.log(`check-commit-messages: ${files.length} arquivos versionados sem endereco antigo do site.`);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]) {
  const argument = process.argv[2];
  if (argument === '--tracked') {
    checkTrackedFiles();
  } else if (!argument) {
    console.error('Uso: node scripts/check-commit-messages.mjs <arquivo com as mensagens> | --tracked');
    process.exit(2);
  } else {
    const violations = findViolations(readFileSync(argument, 'utf8'));
    for (const { line, rule } of violations) {
      console.error(`check-commit-messages: linha ${line} tem termo proibido (${rule}).`);
    }
    if (violations.length) {
      console.error('Mensagens de commit seguem Conventional Commits, sem rodape de coautoria, credito a ferramenta nem endereco antigo do site.');
      process.exit(1);
    }
    console.log('check-commit-messages: mensagens limpas.');
  }
}
