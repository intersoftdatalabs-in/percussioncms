const { defineConfig } = require("@playwright/test");
const path = require("path");
const {
  resolveCmsBaseUrl,
  DEV_FALLBACK_URL,
} = require("./tests/helpers/resolve-cms-env");

// Load module-root .env for local human runs (QA mode typically injects env).
require("dotenv").config({
  path: path.resolve(__dirname, "../.env"),
});

const resolvedBase = resolveCmsBaseUrl(process.env, {
  fallbackUrl: DEV_FALLBACK_URL,
});

module.exports = defineConfig({
  // Resolved relative to this file (frontend/playwright.config.js). Specs
  // live in ./tests/ alongside this config; running from the parent
  // directory (e.g. `npx --prefix frontend playwright test`) breaks
  // resolution. Use `npx playwright test` from this directory.
  testDir: "./tests",
  // Node unit tests under tests/unit (no live CMS) — not Playwright specs.
  testIgnore: ["**/unit/**"],
  // Serial worker: the dev CMS login is contended and the form
  // session cookies are per-context but the server's login endpoint
  // has no rate-limit, so concurrent logins from multiple workers
  // can race. CI defaults to 1 worker; local runs can be parallel
  // when the dev CMS is dedicated.
  workers: 1,
  timeout: 30000,
  retries: 0,
  // Failure artifacts (paths for agents / operators). Full attach runbook:
  // docs/developer-module/playwright-failure-artifacts.md (#2066).
  outputDir: "test-results",
  reporter: [
    ["list"],
    ["html", { open: "never", outputFolder: "playwright-report" }],
  ],
  use: {
    // QA mode: set TEST_CMS_URL (from perc-devctl qa-up). Prefer freeport URL
    // over hardcoding :9993 — see #2005/#2014 and workbench-rest-and-qa-modes.md.
    baseURL: resolvedBase.url,
    headless: true,
    viewport: { width: 1280, height: 720 },
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
  projects: [
    {
      name: "chromium",
      use: { browserName: "chromium" },
    },
  ],
});
