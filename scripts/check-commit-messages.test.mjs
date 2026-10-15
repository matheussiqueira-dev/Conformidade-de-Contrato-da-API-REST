import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import { mkdtempSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { test } from 'node:test';
import { fileURLToPath } from 'node:url';
import { FORBIDDEN_TERMS, OLD_SITE_TERMS, findOldSiteReferences, findViolations } from './check-commit-messages.mjs';

const script = fileURLToPath(new URL('./check-commit-messages.mjs', import.meta.url));

function run(message) {
  const file = join(mkdtempSync(join(tmpdir(), 'msgs-')), 'msgs.txt');
  writeFileSync(file, message);
  try {
    execFileSync(process.execPath, [script, file], { stdio: 'pipe' });
    return 0;
  } catch (error) {
    return error.status;
  }
}

test('mensagem limpa em Conventional Commits passa', () => {
  const clean = 'fix(security): count failed logins with an atomic update\n\nfeat(users): revoke the session on role change\n';
  assert.deepEqual(findViolations(clean), []);
  assert.equal(run(clean), 0);
});

test('corpo vazio passa', () => {
  assert.deepEqual(findViolations(''), []);
  assert.equal(run(''), 0);
});

test('cada termo proibido falha, em qualquer caixa', () => {
  assert.equal(FORBIDDEN_TERMS.length, 11);
  for (const term of FORBIDDEN_TERMS) {
    for (const variant of [term, term.toLowerCase(), term.toUpperCase()]) {
      const message = `docs: update the guide\n\n${variant} line\n`;
      assert.equal(findViolations(message).length, 1, variant);
      assert.equal(findViolations(message)[0].line, 3);
      assert.equal(run(message), 1, variant);
    }
  }
});

test('linha de sessao falha; a palavra session em texto comum passa', () => {
  assert.equal(findViolations('chore: x\n\nhttps://example.test/code/session_abc123XYZ').length >= 1, true);
  assert.equal(findViolations('chore: x\n\nsession_0123456789abcdef').length, 1);
  assert.deepEqual(findViolations('feat(security): end the session after a role change'), []);
});

test('termo dentro de outra palavra nao conta; comentario do Git fica de fora', () => {
  assert.deepEqual(findViolations('refactor: rename the assistants table to helpers'), []);
  assert.deepEqual(findViolations(`chore: x\n# ${FORBIDDEN_TERMS[4]} comment from git\n`), []);
});

test('endereco antigo do site falha na mensagem e no arquivo; o endereco oficial passa', () => {
  const [old] = OLD_SITE_TERMS;
  const url = `https://${old}.vercel.app/entrar`;
  assert.equal(findViolations(`docs: link the demo

See ${url}
`).length, 1);
  assert.equal(run(`docs: link the demo

See ${url}
`), 1);
  assert.deepEqual(findOldSiteReferences(`# Demo

Abra ${url}.
`), [{ line: 3, rule: 'endereco antigo do site 1' }]);
  assert.deepEqual(findOldSiteReferences('Abra https://sbi-app-alpha.vercel.app/entrar.'), []);
  assert.deepEqual(findViolations('docs: point the demo to https://sbi-app-alpha.vercel.app'), []);
});

test('--tracked: nenhum arquivo versionado tem endereco antigo do site', () => {
  const output = execFileSync(process.execPath, [script, '--tracked'], { encoding: 'utf8' });
  assert.match(output, /arquivos versionados sem endereco antigo do site/);
});
