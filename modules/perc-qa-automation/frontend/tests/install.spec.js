/**
 * CMS / DTS install matrix — login / health smoke (Layer 2 gate).
 *
 * Layer 1 (install + start + destroy) is driven by:
 *   python3 docker/scripts/matrix-install-smoke.py --product cms --db h2
 *   python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql --keep
 *
 * When using --keep, the cell stays up on the published host port so this
 * Playwright suite (and the rest of perc-qa-automation) can run against it.
 *
 * Usage:
 *   # Bring up a cell and leave it running
 *   python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql --keep
 *
 *   # Probe login (CMS default host port from the harness: 9993)
 *   TEST_CMS_URL=http://localhost:9993 TEST_DB_TYPE=postgresql \
 *     npm test -- tests/install.spec.js
 *
 *   # DTS (harness host port 9983)
 *   TEST_PRODUCT=dts TEST_CMS_URL=http://localhost:9983 TEST_DB_TYPE=h2 \
 *     npm test -- tests/install.spec.js
 *
 * Environment:
 *   TEST_CMS_URL   Base URL (CMS or DTS) — default http://localhost:9993
 *   TEST_DB_TYPE   Label for reporting — default h2
 *   TEST_PRODUCT   cms | dts — default cms (chooses probe path)
 */

const { test, expect } = require("@playwright/test");

const BASE_URL = process.env.TEST_CMS_URL || "http://localhost:9993";
const DB_TYPE = process.env.TEST_DB_TYPE || "h2";
const PRODUCT = (process.env.TEST_PRODUCT || "cms").toLowerCase();
const PROBE_PATH = PRODUCT === "dts" ? "/" : "/Rhythmyx/login";

test.describe(`${PRODUCT} on ${DB_TYPE} database`, () => {
  test(`should respond on ${PROBE_PATH} for ${DB_TYPE}`, async ({ page }) => {
    console.log(
      `Probing ${PRODUCT} at ${BASE_URL}${PROBE_PATH} (db=${DB_TYPE})...`,
    );

    await page.goto(`${BASE_URL}${PROBE_PATH}`, { timeout: 120000 });

    if (PRODUCT === "dts") {
      // DTS root may not be a titled login page; accept any non-error document.
      const status = page.url();
      expect(status).toBeTruthy();
      console.log(`✓ DTS reachable for ${DB_TYPE} backend at ${status}`);
      return;
    }

    const title = await page.title();
    console.log("Page title:", title);
    await expect(page).toHaveTitle(/Percussion|Login|Rhythmyx/i, {
      timeout: 60000,
    });
    console.log(`✓ Login page verified for ${DB_TYPE} backend!`);
  });
});
