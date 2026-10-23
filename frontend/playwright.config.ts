import { defineConfig } from '@playwright/test'
export default defineConfig({
  testDir: './tests', fullyParallel: false, workers: 1, timeout: 45000,
  reporter: [['list'], ['json', { outputFile: 'test-results/results.json' }]],
  use: { baseURL: process.env.I01_URL ?? 'https://localhost:8443', ignoreHTTPSErrors: true, screenshot: 'only-on-failure', trace: 'retain-on-failure' },
  projects: [{ name: 'edge', use: { browserName: 'chromium', channel: 'msedge' } }],
})
