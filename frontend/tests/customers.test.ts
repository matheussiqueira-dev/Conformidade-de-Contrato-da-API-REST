import test from 'node:test';
import assert from 'node:assert/strict';
import { daysSince } from '../lib/format.ts';

test('"há N dias" conta dias de calendario no horario da loja (America/Fortaleza)', () => {
  const now = new Date('2026-10-14T13:00:00Z'); // 10:00 em Fortaleza
  assert.equal(daysSince('2026-10-14T03:30:00Z', now), 'hoje'); // 00:30 do mesmo dia na loja
  assert.equal(daysSince('2026-10-14T02:30:00Z', now), 'há 1 dia'); // 23:30 do dia anterior na loja
  assert.equal(daysSince('2026-09-30T15:00:00-03:00', now), 'há 14 dias');
  assert.equal(daysSince('2026-10-15T10:00:00-03:00', now), 'hoje');
});
