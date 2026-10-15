// CT-DEMO-05: pela API, o VENDEDOR ve o documento mascarado tambem na massa Olist; o GERENTE ve completo.
// Uso: SBI_DEMO_PASSWORD=<senha das contas demo> node scripts/demo-olist-api.mjs <origem da API>
//   ex.: http://127.0.0.1:3000/api/backend (stack demo) ou http://127.0.0.1:18081 (API isolada da CI)
// Ambiente com contas renomeadas: SBI_DEMO_SELLER_EMAIL/_PASSWORD e SBI_DEMO_MANAGER_EMAIL/_PASSWORD
// substituem ana@loja.test, rafael@loja.test e SBI_DEMO_PASSWORD.
// Saida no formato dos testes SQL (PASS/FAIL por linha); termina com codigo 1 se algum caso falhar.
import { readFileSync } from 'node:fs';
import { sessionClient } from './session-client.mjs';

const origin = process.argv[2] ?? 'http://127.0.0.1:3000/api/backend';
const env = process.env;
const sellerAccount = [env.SBI_DEMO_SELLER_EMAIL ?? 'ana@loja.test', env.SBI_DEMO_SELLER_PASSWORD ?? env.SBI_DEMO_PASSWORD];
const managerAccount = [env.SBI_DEMO_MANAGER_EMAIL ?? 'rafael@loja.test', env.SBI_DEMO_MANAGER_PASSWORD ?? env.SBI_DEMO_PASSWORD];
if (!sellerAccount[1] || !managerAccount[1]) throw new Error('Defina SBI_DEMO_PASSWORD (senha das contas de demonstracao).');

const [header, ...rows] = readFileSync(new URL('../docs/bd/pacote-sql/olist/clientes.csv', import.meta.url), 'utf8').trim().split(/\r?\n/);
const columns = header.split(',');
const record = line => Object.fromEntries(line.split(',').map((value, i) => [columns[i], value]));
const recurring = record(rows.find(line => line.startsWith('CLI-001,')));
const company = rows.map(record).find(c => c.tipo === 'CORPORATE');
const results = [];
const check = (id, ok, info = '') => { results.push(ok); console.log(`${ok ? 'PASS' : 'FAIL'} ${id}${ok ? '' : ` ${info}`}`); };

async function login([email, password]) {
  const client = sessionClient(origin);
  const response = await client.login(email, password);
  if (response.status !== 200) throw new Error(`login ${email}: ${response.status}`);
  return client;
}

const seller = await login(sellerAccount);
const manager = await login(managerAccount);
const digits = recurring.cpf;
const formatted = `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`;

const found = await seller.request(`/customers?q=${digits}`);
const customer = found.body?.content?.[0];
check('CT-DEMO-05a VENDEDOR acha o cliente Olist pelo CPF completo e recebe ***.XXX.XXX-**',
  found.status === 200 && found.body.totalElements === 1 && /^\*{3}\.\d{3}\.\d{3}-\*{2}$/.test(customer?.document ?? ''),
  JSON.stringify(found.body).slice(0, 200));

const id = customer?.id;
const sellerBodies = [found, await seller.request(`/customers/${id}`), await seller.request(`/customers/${id}/orders?size=100`),
  await seller.request(`/customers/${id}/summary`)];
const leaked = sellerBodies.map(r => JSON.stringify(r.body)).filter(text => text.includes(digits) || text.includes(formatted));
check('CT-DEMO-05b nenhuma resposta do VENDEDOR (lista, perfil, historico, resumo) traz o CPF completo',
  sellerBodies.every(r => r.status === 200) && leaked.length === 0, `${leaked.length} resposta(s) com o documento`);

const companyFound = await seller.request(`/customers?q=${company.cnpj}`);
check('CT-DEMO-05c VENDEDOR ve o CNPJ da PJ Olist como **.XXX.XXX/XXXX-**',
  companyFound.status === 200 && /^\*{2}\.\d{3}\.\d{3}\/\d{4}-\*{2}$/.test(companyFound.body?.content?.[0]?.document ?? ''),
  JSON.stringify(companyFound.body).slice(0, 200));

const full = await manager.request(`/customers/${id}`);
const summary = await manager.request(`/customers/${id}/summary`);
check('CT-DEMO-05d GERENTE ve o CPF completo e o resumo com 14 pedidos pagos da loja',
  full.status === 200 && full.body.document === formatted && summary.status === 200 && summary.body.ordersPaid === 14
    && summary.body.scope === 'STORE', `${full.body?.document} / ${summary.body?.ordersPaid}`);

const passed = results.filter(Boolean).length;
console.log(`PASS: ${passed}  FAIL: ${results.length - passed}`);
process.exitCode = passed === results.length ? 0 : 1;
