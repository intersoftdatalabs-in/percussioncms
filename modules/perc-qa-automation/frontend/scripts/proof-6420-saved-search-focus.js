/* Live proof for #6420: Explorer saved-search execute with focus on results.
 *
 * Run: TEST_CMS_URL=http://127.0.0.1:<port> ADMIN_USERNAME=Admin \
 *   ADMIN_PASSWORD=<pw> node scripts/proof-6420-saved-search-focus.js
 */
"use strict";
const { chromium } = require("playwright");
const {
  loginAsAdmin,
  BASE_URL,
} = require("../tests/helpers/auth");
const {
  TEST_IDS,
  explorerEntryUrl,
  pickRunnableSelectOption,
} = require("../tests/helpers/explorer-saved-search");

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1400, height: 900 } });
  const base = process.env.TEST_CMS_URL || BASE_URL;
  await loginAsAdmin(page);
  await page.goto(explorerEntryUrl(base), { waitUntil: "networkidle" });
  await page
    .locator(`[data-testid="${TEST_IDS.shell}"]`)
    .waitFor({ timeout: 20000 });
  await page.locator(`[data-testid="${TEST_IDS.menuView}"]`).click();
  await page.locator(`[data-testid="${TEST_IDS.toggleSearch}"]`).click();
  await page
    .locator(`[data-testid="${TEST_IDS.searchPanel}"]`)
    .waitFor({ timeout: 10000 });
  const { catalogSettledSelector } = require("../tests/helpers/explorer-saved-search");
  await page.locator(catalogSettledSelector()).first().waitFor({ timeout: 20000 });
  const select = page.locator(`[data-testid="${TEST_IDS.savedSelect}"]`);
  const options = await select
    .locator("option")
    .evaluateAll((opts) =>
      opts.map((o) => ({ value: o.value, text: String(o.textContent || "") })),
    );
  const runnable = pickRunnableSelectOption(options);
  if (runnable) {
    await select.selectOption(runnable.value);
    await page.locator(`[data-testid="${TEST_IDS.savedRun}"]`).click();
    await page
      .locator(`[data-testid="${TEST_IDS.resultsList}"]`)
      .waitFor({ timeout: 30000 });
  }
  await page
    .locator(`[data-testid="${TEST_IDS.searchPanel}"]`)
    .screenshot({
      path: "../../../docs/ai-generated/proofs/issue-6420-saved-search-escape-clear-focus.png",
    });
  const focused = await page.evaluate(
    () =>
      document.activeElement &&
      document.activeElement.getAttribute("data-testid"),
  );
  console.log("FOCUSED_TESTID=" + focused);
  await browser.close();
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
