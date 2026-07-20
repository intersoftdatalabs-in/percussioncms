const { defineConfig } = require('@playwright/test');

module.exports = defineConfig({
  // Resolved relative to this file (frontend/playwright.config.js). Specs
  // live in ./tests/ alongside this config; running from the parent
  // directory (e.g. `npx --prefix frontend playwright test`) breaks
  // resolution. Use `npx playwright test` from this directory.
  testDir: './tests',
  // Serial worker: the dev CMS login is contended and the form
  // session cookies are per-context but the server's login endpoint
  // has no rate-limit, so concurrent logins from multiple workers
  // can race. CI defaults to 1 worker; local runs can be parallel
  // when the dev CMS is dedicated.
  workers: 1,
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
