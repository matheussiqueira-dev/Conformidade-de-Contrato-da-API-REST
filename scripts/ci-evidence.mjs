import { readFileSync } from 'node:fs';
// Only dependency metadata and two screenshots of synthetic fixtures are exported.
// No cookies, passwords, environment, traces or API logs are included.
for (const path of ['frontend/package-lock.json', 'frontend/test-results/session-desktop.png', 'frontend/test-results/login-mobile.png']) {
  console.log('A3_EVIDENCE ' + JSON.stringify({ path, base64:readFileSync(path).toString('base64') }));
}
