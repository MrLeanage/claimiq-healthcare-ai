import { defineConfig, devices } from '@playwright/test';
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  use: { baseURL: 'http://127.0.0.1:5174', trace: 'retain-on-failure' },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: 'node scripts/start-e2e-ai.mjs',
      url: 'http://127.0.0.1:8092/health',
      reuseExistingServer: false,
      timeout: 15000,
    },
    {
      command: 'node scripts/start-e2e-backend.mjs',
      url: 'http://127.0.0.1:8091/actuator/health',
      reuseExistingServer: false,
      timeout: 60000,
    },
    {
      command: 'node node_modules/vite/bin/vite.js --port 5174',
      url: 'http://127.0.0.1:5174',
      env: { BACKEND_URL: 'http://127.0.0.1:8091' },
      reuseExistingServer: false,
      timeout: 60000,
    },
  ],
});
