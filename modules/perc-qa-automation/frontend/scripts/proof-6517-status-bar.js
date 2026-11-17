/* Live proof for #6517: Explorer status bar footer with a live selection.
 *
 * Run: TEST_CMS_URL=http://127.0.0.1:<port> ADMIN_USERNAME=Admin \
 *   ADMIN_PASSWORD=<pw> node scripts/proof-6517-status-bar.js
 */
"use strict";
const path = require("path");
const { chromium } = require("playwright");
const { loginAsAdmin } = require("../tests/helpers/auth");
const { explorerStatusBarUrl } = require("../tests/helpers/explorer-status-bar");
const {
  sitesTreeRootLocator,
} = require("../tests/helpers/explorer-sites-list-create");

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 900 } });
  const base = process.env.TEST_CMS_URL;
  await loginAsAdmin(page);
  await page.goto(explorerStatusBarUrl(base), { waitUntil: "domcontentloaded" });
  const shell = page.locator('[data-testid="content-explorer-shell"]');
  await shell.waitFor({ timeout: 30000 });
  const sites = sitesTreeRootLocator(page);
  await sites.first().waitFor({ timeout: 30000 });
  await sites.first().click();
  await shell.waitFor({ timeout: 15000 });
  const firstRow = page.locator('[data-testid^="detail-row-"]').first();
  await firstRow.waitFor({ timeout: 20000 });
  await firstRow.click();
  await page
    .locator('[data-testid="explorer-status-bar-selection"]')
    .getByText(/Selected:|1 item selected/i)
    .waitFor({ timeout: 10000 });
  const out = path.resolve(
    __dirname,
    "..",
    "..",
    "..",
    "..",
    "docs",
    "ai-generated",
    "proofs",
    "issue-6517-status-bar.png",
  );
  await shell.screenshot({ path: out });
  console.log(`proof written: ${out}`);
  await browser.close();
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
