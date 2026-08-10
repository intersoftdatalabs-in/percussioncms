/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Golden unattended smoke (#2065 / #1928 slice B, epic #1827).
 *
 * <p>One reference path for overnight / agent QA mode against the H2 Docker
 * stack from {@code perc-devctl qa-up}: Admin login + one stable product
 * screen (modern Content Explorer shell). Env-only — no
 * {@code DEV_PERCUSSION_INSTALL}.</p>
 *
 * <p>From repo root (Unix):</p>
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   # capture TEST_CMS_URL + ADMIN_PASSWORD from stdout
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; TEST_DB_TYPE=h2 \
 *     npm run test:golden
 *   # optional extended (#2490 / #2498): golden + @folder-recycle + @profile multi-path
 *   # npm run test:golden-extended
 *   python docker/scripts/perc-devctl.py qa-down
 * </pre>
 *
 * <p>Windows (cmd) one-shot: see module README → Golden unattended smoke.
 * Extended set inventory: {@code helpers/golden-unattended-smoke-set.js}
 * (folder-recycle #2490; profile-shell #2498).</p>
 *
 * <p>Failure artifacts: {@code test-results/}, {@code playwright-report/}
 * under this frontend directory. Attach runbook:
 * docs/developer-module/playwright-failure-artifacts.md</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  BASE_URL_SOURCE,
  ADMIN_USERNAME,
} = require("./helpers/auth");

/** Modern Content Explorer SPA entry (stable product screen for golden). */
function explorerUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;
}

test.describe("golden unattended smoke @smoke @golden", () => {
  test("QA env resolves without host install", async () => {
    // Documents the unattended contract: freeport URL from TEST_CMS_URL or
    // host-port env, never hard-coded :9993 alone (#2005/#2014).
    expect(BASE_URL).toMatch(/^https?:\/\/[^/]+:\d+$/);
    expect(ADMIN_USERNAME).toBeTruthy();
    // Prefer QA sources when operators set them; fallback is allowed only for
    // local accidental runs (will fail login without a live CMS).
    if (
      process.env.TEST_CMS_URL ||
      process.env.QA_CMS_URL ||
      process.env.CMS_BASE_URL
    ) {
      expect(BASE_URL_SOURCE).toBe("TEST_CMS_URL");
    } else if (process.env.QA_CMS_HOST_PORT || process.env.CMS_HOST_PORT) {
      expect(BASE_URL_SOURCE).toBe("CMS_HOST_PORT");
    }
  });

  test("Admin login + Content Explorer shell @smoke @golden", async ({
    page,
  }) => {
    test.setTimeout(90_000);

    await loginAsAdmin(page);

    const postLoginUrl = page.url();
    expect(postLoginUrl).not.toMatch(/\/Rhythmyx\/login(\?|$)/);
    expect(postLoginUrl).toMatch(/\/Rhythmyx\/|\/cm\//);

    await page.goto(explorerUrl(), { waitUntil: "networkidle" });

    const shell = page.locator('[data-testid="content-explorer-shell"]');
    await expect(shell).toBeVisible({ timeout: 30_000 });
    await expect(page.locator('[data-testid="explorer-tree"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="detail-list"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="reduced-actions"]')).toBeVisible({
      timeout: 15_000,
    });
  });
});
