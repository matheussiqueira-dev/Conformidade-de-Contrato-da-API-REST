import { defineConfig } from '@playwright/test';
// Smoke do ambiente publicado (somente leitura), fora da suite normal: npx playwright test -c playwright.published.config.mjs
// Endereco em SBI_SMOKE_BASE_URL (padrao: o site publicado); contas em SBI_SMOKE_* (segredos do workflow).
export default defineConfig({
  testDir:'./e2e/published', testMatch:'**/*.spec.ts', fullyParallel:false, workers:1, retries:1, timeout:120000,
  outputDir:'test-results-published',
  reporter:[['list'], ['html', { open:'never', outputFolder:'playwright-report-published' }]],
  use:{ baseURL: process.env.SBI_SMOKE_BASE_URL || 'https://sbi-app-alpha.vercel.app', screenshot:'on', trace:'retain-on-failure',
        navigationTimeout:90000 },
});
