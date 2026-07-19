const { defineConfig } = require('@playwright/test');

module.exports = defineConfig({
  // Resolved relative to this file (frontend/playwright.config.js). Specs
  // live in ./tests/ alongside this config; running from the parent
  // directory (e.g. `npx --prefix frontend playwright test`) breaks
  // resolution. Use `npx playwright test` from this directory.
  testDir: './tests',
  timeout: 30000,
  retries: 0,
  use: {
    headless: true,
    viewport: { width: 1280, height: 720 },
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' },
    },
  ],
});
