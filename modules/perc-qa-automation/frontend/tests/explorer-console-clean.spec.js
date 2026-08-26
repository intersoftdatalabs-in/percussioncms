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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Playwright surface: #3468 / #3458 / parent #2745 / #3855 — Explorer shell
 * console-clean of product-path 404/400 (login → Explorer → sample site
 * folder → Refresh). Covers both /services and /Rhythmyx/services prefixes.
 * Find/types 400 + find/templates 500 after page select is asserted in
 * {@code explorer-preview-view.spec.js} (listed-page navigation).
 *
 * Does not blanket-skip 404/400 resource errors.
 *
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=... TEST_DB_TYPE=h2 \
 *     npm run test:surface -- --path tests/explorer-console-clean.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerSpaUrl,
  attachProductStatusCollector,
  formatHits,
} = require("./helpers/explorer-console-clean");

test.describe("Explorer shell console-clean (#3468 / #3458 / #2745)", () => {
  test(
    "login → Explorer → sample site → Refresh has no product 404/400",
    { tag: ["@explorer", "@console-clean", "@smoke"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const { hits, pageErrors } = attachProductStatusCollector(page, BASE_URL);

      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL), {
        waitUntil: "domcontentloaded",
      });

      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 30_000 });
      await expect(page.locator('[data-testid="explorer-tree"]')).toBeVisible({
        timeout: 15_000,
      });

      const siteNode = page.locator('[data-testid^="tree-node-/Sites/"]').first();
      if ((await siteNode.count()) > 0) {
        await siteNode.click();
        await expect(page.locator('[data-testid="detail-list"]')).toBeVisible({
          timeout: 10_000,
        });
      }

      const refresh = page.locator('[data-testid="explorer-refresh-list"]');
      await expect(refresh).toBeVisible();
      await refresh.click();
      await expect(page.locator('[data-testid="detail-list"]')).toBeVisible({
        timeout: 10_000,
      });

      expect(hits, `product 404/400:\n${formatHits(hits)}`).toEqual([]);
      const unexpected = pageErrors.filter(
        (t) =>
          !/ResizeObserver/i.test(t) &&
          !/Download the React DevTools/i.test(t),
      );
      expect(unexpected, unexpected.join("\n")).toEqual([]);
    },
  );
});
