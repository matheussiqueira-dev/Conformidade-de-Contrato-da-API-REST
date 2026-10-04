import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir:'./e2e', fullyParallel:false, workers:1, retries:0,
  reporter:[['list'], ['html', { open:'never' }]],
  use:{ baseURL:'http://127.0.0.1:3000', trace:'off' },
  webServer:{ command:'npm run start -- --hostname 127.0.0.1', url:'http://127.0.0.1:3000', reuseExistingServer:false, timeout:60000 },
});
