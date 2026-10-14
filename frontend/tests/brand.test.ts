import test from 'node:test';
import assert from 'node:assert/strict';
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { BARS, BRAND_ACCESSIBLE_NAME, DOT, SYMBOL_RADIUS, SYMBOL_SIZE, toneColors } from '../components/brand/geometry.ts';
import { DERIVED_ICONS, ICON_PATHS, MANUAL_ICONS, iconA11y } from '../components/brand/icons-data.ts';
import { menuFor, menuGroups, PASSWORD_PATH } from '../lib/areas.ts';
import { compactMoney, formatDateTime, formatMonth, formatPeriod, formatTime, initials, money, MISSING, statusPill } from '../lib/format.ts';

const ROOT = fileURLToPath(new URL('..', import.meta.url));

test('[CT-WEB-122] nome acessivel unico da marca', () => {
  assert.equal(BRAND_ACCESSIBLE_NAME, 'SBI — Super Business Intelligence');
});

test('[CT-WEB-122] simbolo do componente tem a mesma geometria do favicon app/icon.svg', () => {
  const svg = readFileSync(join(ROOT, 'app', 'icon.svg'), 'utf8');
  assert.match(svg, new RegExp(`<rect width="${SYMBOL_SIZE}" height="${SYMBOL_SIZE}" rx="${SYMBOL_RADIUS}" fill="#3D4ED6"/>`));
  const bars = [...svg.matchAll(/<rect x="([\d.]+)" y="([\d.]+)" width="([\d.]+)" height="([\d.]+)" rx="([\d.]+)" fill="#FFFFFF"\/>/g)]
    .map(m => ({ x: Number(m[1]), y: Number(m[2]), width: Number(m[3]), height: Number(m[4]), rx: Number(m[5]) }));
  assert.deepEqual(bars, BARS);
  assert.match(svg, new RegExp(`<circle cx="${DOT.cx}" cy="${DOT.cy}" r="${DOT.r}" fill="#5BD6B8"/>`));
  // Barras crescentes e o ponto Menta no alto da terceira barra.
  assert.ok(BARS[0]!.height < BARS[1]!.height && BARS[1]!.height < BARS[2]!.height);
  assert.ok(DOT.cx > BARS[2]!.x && DOT.cx < BARS[2]!.x + BARS[2]!.width && DOT.cy + DOT.r < BARS[2]!.y);
});

test('variantes do logo: negativa sobre Tinta e principal sobre fundo claro', () => {
  assert.equal(toneColors('principal').text, '#141B26');
  assert.equal(toneColors('negativa').text, '#FFFFFF');
  assert.equal(toneColors('negativa').dot, '#5BD6B8');
});

test('[CT-WEB-123] icones: os seis do manual e os derivados, decorativos por padrao', () => {
  assert.equal(MANUAL_ICONS.length, 6);
  for (const name of [...MANUAL_ICONS, ...DERIVED_ICONS]) assert.ok(ICON_PATHS[name].length > 0, name);
  assert.deepEqual(iconA11y(), { 'aria-hidden': true, focusable: false });
  assert.deepEqual(iconA11y('Concluída'), { role: 'img', 'aria-label': 'Concluída' });
});

test('menu em secoes GESTAO, ATENDIMENTO e MEU ESPACO; Trocar senha sempre presente', () => {
  const manager = menuGroups(menuFor(['VENDER', 'VENDAS', 'CLIENTES', 'MEU_DESEMPENHO', 'GESTAO', 'PRODUTOS', 'ESTOQUE', 'PROMOCOES', 'ANALISES', 'USUARIOS']));
  assert.deepEqual(manager.map(g => g.label), ['Gestão', 'Atendimento', 'Meu espaço']);
  assert.deepEqual(manager[0]!.items.map(i => i.label), ['Visão geral', 'Produtos e preços', 'Promoções', 'Estoque', 'Análises', 'Usuários']);
  assert.deepEqual(manager[1]!.items.map(i => i.label), ['Vender', 'Vendas', 'Clientes']);
  assert.deepEqual(manager[2]!.items.map(i => i.href), ['/meu-desempenho', PASSWORD_PATH]);
  const seller = menuGroups(menuFor(['VENDER', 'VENDAS', 'CLIENTES', 'MEU_DESEMPENHO']));
  assert.deepEqual(seller.map(g => g.label), ['Atendimento', 'Meu espaço']);
});

test('[CT-WEB-124] formatos do manual: hora 14h30, periodo, ausente e valor curto', () => {
  assert.equal(formatTime('2026-10-14T17:30:00Z'), '14h30');
  assert.equal(formatTime('2026-10-14T12:05:00Z'), '09h05');
  assert.equal(formatDateTime('2026-10-14T17:30:00Z'), '14/10/2026 14h30');
  assert.equal(formatPeriod('2026-09-01', '2026-09-30'), '01/09 a 30/09/2026');
  assert.equal(formatPeriod('2025-12-15', '2026-01-14'), '15/12/2025 a 14/01/2026');
  assert.equal(formatMonth('2026-10'), 'outubro de 2026');
  assert.equal(MISSING, 'Não informado');
  assert.equal(money(undefined), MISSING);
  assert.equal(compactMoney(47_000), 'R$ 47 mil');
  assert.equal(money(1234.5), 'R$ 1.234,50');
  assert.equal(initials('Allan Paula Lima'), 'AL');
  assert.equal(initials('marta'), 'M');
  assert.equal(statusPill('PAID'), 'pill pill-ok');
  assert.equal(statusPill('PENDING_PAYMENT'), 'pill pill-warn');
  assert.equal(statusPill('CANCELED'), 'pill pill-err');
  assert.equal(statusPill('PROCESSING'), 'pill pill-info');
});

/** Formas proibidas pelo manual: so "SBI" (maiusculas, sem pontos) e "o SBI". */
const FORBIDDEN: readonly [RegExp, string][] = [
  [/S\.B\.I\./, 'S.B.I.'],
  [/\bSbi\b/, 'Sbi'],
  [/Super BI\b/i, 'Super BI'],
  [/SuperBI/i, 'SuperBI'],
  [/\ba SBI\b/, 'a SBI'],
];

function files(dir: string): string[] {
  return readdirSync(dir).flatMap(name => {
    const path = join(dir, name);
    if (name === 'node_modules' || name === '.next' || name === 'generated') return [];
    return statSync(path).isDirectory() ? files(path) : /\.(tsx?|css|md)$/.test(name) ? [path] : [];
  });
}

test('[CT-WEB-125] nenhum nome proibido da marca na interface nem na documentacao', () => {
  const targets = [join(ROOT, 'app'), join(ROOT, 'components'), join(ROOT, 'lib'), join(ROOT, '..', 'docs')];
  const found = targets.flatMap(files).flatMap(path => {
    const text = readFileSync(path, 'utf8');
    return FORBIDDEN.filter(([pattern]) => pattern.test(text)).map(([, name]) => `${path}: ${name}`);
  });
  assert.deepEqual(found, []);
});
