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
 * Playwright surface: #2733 — Explorer preview polish + View refresh residual.
 *
 * <p>Verifies product shell chrome for Preview + Refresh. When H2 fixture
 * lacks a previewable content row, preview-open soft-skips (unit coverage
 * still applies).</p>
 *
 * <p>Tags: {@code @explorer-preview-view} {@code @preview} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-preview-view.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerEntryUrl,
  noPreviewableItemSkipMessage,
} = require("./helpers/explorer-preview-view");

async function listWaitReady(page) {
  await page.locator(`[data-testid="${TEST_IDS.list}"]`).waitFor({
    timeout: 15_000,
  });
}

test.describe("modern React Content Explorer — preview + view residual (#2733)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
    await page.goto(explorerEntryUrl(BASE_URL, { cacheBuster: Date.now() }));
    await page.waitForLoadState("networkidle");
  });

  test(
    "shell mounts preview action and refresh view control",
    { tag: ["@explorer-preview-view", "@preview", "@smoke"] },
    async ({ page }) => {
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });

      await expect(
        page.locator(`[data-testid="${TEST_IDS.reducedActions}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.preview}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.viewTools}"]`),
      ).toBeVisible();

      const refresh = page.locator(`[data-testid="${TEST_IDS.refresh}"]`);
      await expect(refresh).toBeVisible();
      await expect(refresh).toBeEnabled();
      await expect(refresh).toHaveAttribute("aria-label", /./);

      // Refresh is shell-state residual — click must not crash the shell.
      await refresh.click();
      await expect(shell).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.list}"]`),
      ).toBeVisible({ timeout: 15_000 });
    },
  );

  test(
    "preview enabled for a content row or soft-skip when H2 lacks item",
    { tag: ["@explorer-preview-view", "@preview"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });

      const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
      await expect(tree).toBeVisible({ timeout: 15_000 });
      const sitesNode = page
        .locator(
          '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"], [data-testid*="tree-node"][data-testid*="Sites"]',
        )
        .first();
      if ((await sitesNode.count()) > 0) {
        await sitesNode.click({ force: true, timeout: 10_000 }).catch(() => {});
        await listWaitReady(page);
        await page.waitForLoadState("networkidle").catch(() => {});
      }

      const list = page.locator(`[data-testid="${TEST_IDS.list}"]`);
      await expect(list).toBeVisible({ timeout: 15_000 });

      const rows = list.locator('tbody tr[data-testid^="detail-row-"]');
      const rowCount = await rows.count();
      if (rowCount === 0) {
        test.skip(true, noPreviewableItemSkipMessage());
        return;
      }

      // Prefer non-folder rows: force-click first few until Preview enables.
      let previewEnabled = false;
      const maxProbe = Math.min(rowCount, 8);
      for (let i = 0; i < maxProbe; i++) {
        const row = rows.nth(i);
        await row.click({ force: true, timeout: 10_000 }).catch(() => {});
        const preview = page.locator(`[data-testid="${TEST_IDS.preview}"]`);
        if (await preview.isEnabled().catch(() => false)) {
          previewEnabled = true;
          // Soft smoke: click opens a popup or navigates; do not assert body
          // (fixture-dependent). Popup may be blocked in CI — just ensure no throw.
          const popupPromise = page
            .waitForEvent("popup", { timeout: 5_000 })
            .catch(() => null);
          await preview.click();
          await popupPromise;
          break;
        }
      }

      if (!previewEnabled) {
        test.skip(true, noPreviewableItemSkipMessage());
      }
    },
  );
});
