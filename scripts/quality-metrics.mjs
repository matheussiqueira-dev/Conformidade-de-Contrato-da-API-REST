// Consolida as metricas de qualidade a partir dos relatorios gerados por
// `mvnw -Pintegration verify` (Surefire, JaCoCo, PMD, CPD e SpotBugs).
// Uso: node scripts/quality-metrics.mjs <diretorio-de-saida> [rotulo]
// Nao executa testes: apenas le target/ e grava metrics.json + resumo.md.
import { readFileSync, readdirSync, existsSync, mkdirSync, writeFileSync, cpSync } from 'node:fs';
import { join, resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const target = join(root, 'target');
const outDir = resolve(process.argv[2] ?? join(root, 'reports', 'metricas', 'ultima'));
const label = process.argv[3] ?? 'execucao';

const read = (file) => (existsSync(file) ? readFileSync(file, 'utf8') : null);
// SpotBugs escreve atributos com aspas simples; os demais relatorios, com aspas duplas.
const attr = (tag, name) => tag.match(new RegExp(`\\b${name}=(?:"([^"]*)"|'([^']*)')`))?.slice(1).find((v) => v !== undefined);
const pct = (covered, total) => (total === 0 ? 0 : Math.round((covered / total) * 1000) / 10);
const countBy = (items, key) => items.reduce((acc, item) => {
  acc[item[key]] = (acc[item[key]] ?? 0) + 1;
  return acc;
}, {});

function surefire() {
  const dir = join(target, 'surefire-reports');
  if (!existsSync(dir)) throw new Error('target/surefire-reports ausente: rode o build antes.');
  const suites = [];
  const cases = [];
  for (const file of readdirSync(dir).filter((f) => f.startsWith('TEST-') && f.endsWith('.xml'))) {
    const xml = readFileSync(join(dir, file), 'utf8');
    const suiteTag = xml.match(/<testsuite\b[^>]*>/)[0];
    suites.push({
      name: attr(suiteTag, 'name'),
      tests: Number(attr(suiteTag, 'tests')),
      failures: Number(attr(suiteTag, 'failures')),
      errors: Number(attr(suiteTag, 'errors')),
      skipped: Number(attr(suiteTag, 'skipped')),
    });
    for (const match of xml.matchAll(/<testcase\b([^>]*?)(\/>|>([\s\S]*?)<\/testcase>)/g)) {
      const body = match[3] ?? '';
      const status = /<(failure|error)\b/.test(body) ? 'FAIL' : /<skipped\b/.test(body) ? 'SKIP' : 'PASS';
      cases.push({ suite: attr(match[1], 'classname'), name: attr(match[1], 'name'), status });
    }
  }
  const total = suites.reduce((acc, s) => ({
    tests: acc.tests + s.tests,
    failures: acc.failures + s.failures,
    errors: acc.errors + s.errors,
    skipped: acc.skipped + s.skipped,
  }), { tests: 0, failures: 0, errors: 0, skipped: 0 });
  const executed = total.tests - total.skipped;
  const passed = executed - total.failures - total.errors;
  return {
    ...total,
    executed,
    passed,
    passRate: pct(passed, executed),
    failRate: pct(total.failures + total.errors, executed),
    suites: suites.sort((a, b) => a.name.localeCompare(b.name)),
    cases,
  };
}

function counters(fragment) {
  const result = {};
  for (const tag of fragment.matchAll(/<counter type="(\w+)" missed="(\d+)" covered="(\d+)"\/>/g)) {
    const missed = Number(tag[2]);
    const covered = Number(tag[3]);
    result[tag[1]] = { missed, covered, total: missed + covered, percent: pct(covered, missed + covered) };
  }
  return result;
}

function jacoco() {
  const xml = read(join(target, 'site', 'jacoco', 'jacoco.xml'));
  if (!xml) throw new Error('target/site/jacoco/jacoco.xml ausente.');
  const tail = xml.slice(xml.lastIndexOf('</package>'));
  const packages = [];
  const methods = [];
  for (const pkg of xml.matchAll(/<package name="([^"]+)">([\s\S]*?)<\/package>/g)) {
    const body = pkg[2];
    const own = body.slice(body.lastIndexOf('</sourcefile>'));
    packages.push({ name: pkg[1].replaceAll('/', '.'), ...pickCoverage(counters(own)) });
    for (const cls of body.matchAll(/<class name="([^"]+)"[^>]*>([\s\S]*?)<\/class>/g)) {
      for (const method of cls[2].matchAll(/<method name="([^"]+)" desc="[^"]*"(?: line="(\d+)")?>([\s\S]*?)<\/method>/g)) {
        const c = counters(method[3]);
        methods.push({
          class: cls[1].split('/').pop(),
          method: method[1],
          line: method[2] ? Number(method[2]) : null,
          complexity: c.COMPLEXITY?.total ?? 0,
          branchCoverage: c.BRANCH ? c.BRANCH.percent : null,
        });
      }
    }
  }
  const total = counters(tail);
  const complexities = methods.map((m) => m.complexity);
  return {
    total: pickCoverage(total),
    complexity: {
      total: total.COMPLEXITY?.total ?? 0,
      methods: methods.length,
      average: Math.round((complexities.reduce((a, b) => a + b, 0) / Math.max(methods.length, 1)) * 100) / 100,
      max: Math.max(0, ...complexities),
      above10: methods.filter((m) => m.complexity > 10).length,
      top: methods.sort((a, b) => b.complexity - a.complexity).slice(0, 10),
    },
    packages,
  };
}

function pickCoverage(c) {
  return {
    instructions: c.INSTRUCTION ?? null,
    branches: c.BRANCH ?? { missed: 0, covered: 0, total: 0, percent: 0 },
    lines: c.LINE ?? null,
    methods: c.METHOD ?? null,
  };
}

function pmd() {
  const xml = read(join(target, 'pmd.xml'));
  if (!xml) return null;
  const violations = [];
  for (const file of xml.matchAll(/<file name="([^"]+)">([\s\S]*?)<\/file>/g)) {
    const name = file[1].replaceAll('\\', '/').split('/src/main/java/').pop();
    for (const v of file[2].matchAll(/<violation\b([^>]*)>([\s\S]*?)<\/violation>/g)) {
      violations.push({
        file: name,
        line: Number(attr(v[1], 'beginline')),
        rule: attr(v[1], 'rule'),
        ruleset: attr(v[1], 'ruleset'),
        priority: Number(attr(v[1], 'priority')),
        message: v[2].trim(),
      });
    }
  }
  return { total: violations.length, byRule: countBy(violations, 'rule'), byRuleset: countBy(violations, 'ruleset'), byPriority: countBy(violations, 'priority'), violations };
}

function cpd() {
  const xml = read(join(target, 'cpd.xml'));
  if (!xml) return null;
  const duplications = [...xml.matchAll(/<duplication lines="(\d+)" tokens="(\d+)">([\s\S]*?)<\/duplication>/g)].map((d) => ({
    lines: Number(d[1]),
    tokens: Number(d[2]),
    files: [...d[3].matchAll(/<file\b([^>]*)\/>/g)].map((f) => `${attr(f[1], 'path').replaceAll('\\', '/').split('/src/main/java/').pop()}:${attr(f[1], 'line')}`),
  }));
  return {
    total: duplications.length,
    duplicatedLines: duplications.reduce((acc, d) => acc + d.lines * (d.files.length - 1), 0),
    duplications,
  };
}

function spotbugs() {
  const xml = read(join(target, 'spotbugsXml.xml'));
  if (!xml) return null;
  const bugs = [...xml.matchAll(/<BugInstance\b([^>]*)>([\s\S]*?)<\/BugInstance>/g)].map((b) => ({
    type: attr(b[1], 'type'),
    category: attr(b[1], 'category'),
    priority: Number(attr(b[1], 'priority')),
    rank: Number(attr(b[1], 'rank')),
    class: b[2].match(/<Class classname=['"]([^'"]+)['"]/)?.[1].split('.').pop(),
    line: Number(b[2].match(/<SourceLine\b[^>]*\bstart=['"](\d+)['"]/)?.[1] ?? 0),
  }));
  const size = xml.match(/<FindBugsSummary\b([^>]*)>/);
  return {
    total: bugs.length,
    byCategory: countBy(bugs, 'category'),
    byPriority: countBy(bugs, 'priority'),
    byType: countBy(bugs, 'type'),
    ncss: size ? Number(attr(size[1], 'total_size')) : null,
    bugs,
  };
}

const metrics = {
  label,
  generatedAt: new Date().toISOString(),
  tests: surefire(),
  coverage: jacoco(),
  pmd: pmd(),
  cpd: cpd(),
  spotbugs: spotbugs(),
};

mkdirSync(outDir, { recursive: true });
writeFileSync(join(outDir, 'metrics.json'), `${JSON.stringify(metrics, null, 2)}\n`);
for (const raw of ['pmd.xml', 'cpd.xml', 'spotbugsXml.xml']) {
  if (existsSync(join(target, raw))) cpSync(join(target, raw), join(outDir, raw));
}
cpSync(join(target, 'site', 'jacoco'), join(outDir, 'jacoco'), { recursive: true });
cpSync(join(target, 'surefire-reports'), join(outDir, 'surefire-reports'), {
  recursive: true,
  filter: (src) => !src.endsWith('.txt') || src.includes('surefire-reports'),
});

const t = metrics.tests;
const c = metrics.coverage.total;
const fmt = (x) => `${x.covered}/${x.total} (${x.percent}%)`;
const summary = [
  `# Metricas - ${label}`,
  '',
  `Gerado em ${metrics.generatedAt} por \`node scripts/quality-metrics.mjs\` a partir de \`target/\`.`,
  '',
  '| Indicador | Valor |',
  '| --- | --- |',
  `| Testes executados | ${t.executed} |`,
  `| Aprovados / reprovados | ${t.passed} / ${t.failures + t.errors} |`,
  `| Taxa de aprovacao | ${t.passRate}% |`,
  `| Cobertura de instrucoes | ${fmt(c.instructions)} |`,
  `| Cobertura de branches | ${fmt(c.branches)} |`,
  `| Cobertura de linhas | ${fmt(c.lines)} |`,
  `| Complexidade ciclomatica total / media / maxima | ${metrics.coverage.complexity.total} / ${metrics.coverage.complexity.average} / ${metrics.coverage.complexity.max} |`,
  `| Metodos com complexidade > 10 | ${metrics.coverage.complexity.above10} |`,
  `| Violacoes PMD | ${metrics.pmd?.total ?? 'n/d'} |`,
  `| Duplicacoes CPD (blocos / linhas) | ${metrics.cpd ? `${metrics.cpd.total} / ${metrics.cpd.duplicatedLines}` : 'n/d'} |`,
  `| Achados SpotBugs | ${metrics.spotbugs?.total ?? 'n/d'} |`,
  '',
  '## Suites',
  '',
  '| Suite | Testes | Falhas | Erros | Ignorados |',
  '| --- | ---: | ---: | ---: | ---: |',
  ...t.suites.map((s) => `| ${s.name.split('.').pop()} | ${s.tests} | ${s.failures} | ${s.errors} | ${s.skipped} |`),
  '',
  '## Cobertura por pacote',
  '',
  '| Pacote | Instrucoes | Branches |',
  '| --- | ---: | ---: |',
  ...metrics.coverage.packages.map((p) => `| ${p.name.replace('com.swee.ordermanagementspring', '~')} | ${p.instructions?.percent ?? 0}% | ${p.branches.total ? `${p.branches.percent}%` : '-'} |`),
  '',
  '## Metodos mais complexos',
  '',
  '| Classe.metodo | Complexidade | Branches cobertos |',
  '| --- | ---: | ---: |',
  ...metrics.coverage.complexity.top.map((m) => `| ${m.class}.${m.method} | ${m.complexity} | ${m.branchCoverage ?? '-'}${m.branchCoverage === null ? '' : '%'} |`),
  '',
  '## PMD por regra',
  '',
  ...Object.entries(metrics.pmd?.byRule ?? {}).sort((a, b) => b[1] - a[1]).map(([rule, n]) => `- ${rule}: ${n}`),
  '',
  '## SpotBugs por tipo',
  '',
  ...Object.entries(metrics.spotbugs?.byType ?? {}).sort((a, b) => b[1] - a[1]).map(([type, n]) => `- ${type}: ${n}`),
  '',
].join('\n');
writeFileSync(join(outDir, 'resumo.md'), summary);
console.log(summary);
