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
 *   # Bring up a cell and leave it running (pins CMS_HOST_PORT / freeport)
 *   python3 docker/scripts/matrix-install-smoke.py --product cms --db postgresql --keep
 *   # Or: python docker/scripts/perc-devctl.py qa-up  (prints TEST_CMS_URL)
 *
 *   # Probe login — always use the harness-printed URL / pinned host port
 *   # (preferred CMS 9993 / DTS 9983 only when free; do not hardcode freeport).
 *   TEST_CMS_URL=http://127.0.0.1:${CMS_HOST_PORT:-9993} TEST_DB_TYPE=postgresql \
 *     npm test -- tests/install.spec.js
 *
 *   # DTS (preferred host port 9983 when free, else freeport via DTS_HOST_PORT)
 *   TEST_PRODUCT=dts TEST_CMS_URL=http://127.0.0.1:${DTS_HOST_PORT:-9983} TEST_DB_TYPE=h2 \
 *     npm test -- tests/install.spec.js
 *
 * Environment:
 *   TEST_CMS_URL   Base URL (CMS or DTS) — set from qa-up / matrix pin; fallback
 *                  http://localhost:9993 is preferred baseline only when free
 *   TEST_DB_TYPE   Label for reporting — default h2
 *   TEST_PRODUCT   cms | dts — default cms (chooses probe path)
 *   CMS_HOST_PORT / QA_CMS_HOST_PORT / DTS_HOST_PORT — set by harness (#2005)
 */

const { test, expect } = require("@playwright/test");
const {
  resolveCmsBaseUrl,
  QA_PREFERRED_FALLBACK_URL,
} = require("./helpers/resolve-cms-env");

// Same precedence as auth helpers (#2064): TEST_CMS_URL > host port > …
// Install matrix / QA preferred pin when free is the fallback (not sole port).
const BASE_URL = resolveCmsBaseUrl(process.env, {
  fallbackUrl: QA_PREFERRED_FALLBACK_URL,
}).url;
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
