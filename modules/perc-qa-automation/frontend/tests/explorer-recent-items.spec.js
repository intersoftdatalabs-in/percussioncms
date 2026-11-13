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
 * Playwright: #6256 / parent #4530 — Explorer recent-items quick access.
 *
 * Tags: @explorer-recent-items @explorer @smoke
 *
 * npm run test:surface -- --path tests/explorer-recent-items.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  RECENT_KEY_PREFIX,
  explorerRecentItemsUrl,
  isKnownExplorerRecentConsoleNoise,
} = require("./helpers/explorer-recent-items");

const TAGS = ["@explorer-recent-items", "@explorer", "@smoke"];

async function openExplorer(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error" && !isKnownExplorerRecentConsoleNoise(msg.text())) {
      jsErrors.push(msg.text());
    }
  });
  await loginAsAdmin(page);
  await page.goto(explorerRecentItemsUrl(BASE_URL), { waitUntil: "domcontentloaded" });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 30_000,
  });
  return jsErrors;
}

async function listReady(page) {
  await page.locator(`[data-testid="${TEST_IDS.detailList}"]`).waitFor({ timeout: 20_000 });
}

async function clearRecents(page) {
  await page.evaluate((prefix) => {
    const drop = [];
    for (let i = 0; i < window.localStorage.length; i += 1) {
      const key = window.localStorage.key(i);
      if (key && key.startsWith(prefix)) {
        drop.push(key);
      }
    }
    for (const key of drop) {
      window.localStorage.removeItem(key);
    }
  }, RECENT_KEY_PREFIX);
}

async function openRecentPanel(page) {
  await page.locator(`[data-testid="${TEST_IDS.recentToggle}"]`).click();
  await expect(page.locator(`[data-testid="${TEST_IDS.recentPanel}"]`)).toBeVisible({
    timeout: 10_000,
  });
}

test.describe("Explorer recent-items quick access (#6256 / #4530)", () => {
  test(
    "UI: browsing a folder records it and the panel re-browses to it",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
      await clearRecents(page);
      await openRecentPanel(page);
      await expect(page.locator(`[data-testid="${TEST_IDS.recentEmpty}"]`)).toBeVisible();

      const root = page.locator(
        `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites"]`,
      );
      await expect(root.first()).toBeVisible({ timeout: 30_000 });
      await root.first().click();
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toHaveAttribute("data-selected-folder-path", /\/Sites\/?$/);
      // Pin the row by seed name: the /Sites list must have refreshed past
      // the previous folder's rows before the double-click (#6256 — a stale
      // first-row dblclick recorded the wrong entry and never browsed).
      const folderRow = page.locator(
        '[data-testid="detail-list"] tbody tr[data-item-name="Corporate_Investments"]',
      );
      await expect(folderRow).toBeVisible({ timeout: 20_000 });
      await expect(folderRow).toHaveAttribute("data-row-kind", "folder");
      await folderRow.dblclick({ force: true });
      await expect(shell).toHaveAttribute(
        "data-selected-folder-path",
        /Corporate_?Investments/,
        { timeout: 20_000 },
      );

      const row = page.locator(`[data-testid="${TEST_IDS.recentRow}"]`, {
        hasText: "Corporate_Investments",
      });
      await expect(row).toBeVisible({ timeout: 10_000 });
      await expect(row).toHaveAttribute("data-recent-kind", "folder");

      // Browse away, then re-open from recents — folders browse in place.
      // The shell resolves the canonical folder path on browse, so match
      // the folder family rather than the recorded display string.
      await root.first().click();
      await expect(shell).toHaveAttribute("data-selected-folder-path", /\/Sites\/?$/);
      await listReady(page);
      await row.locator(`[data-testid="${TEST_IDS.recentOpen}"]`).click();
      await expect(shell).toHaveAttribute("data-selected-folder-path", /Corporate_?Investments/, {
        timeout: 20_000,
      });
      await expectNoSeriousA11yViolations(page, `[data-testid="${TEST_IDS.shell}"]`);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
      // Live Proof (#6305): pass-run capture of the Recent panel with the
      // re-browsed folder row; committed copy under docs/ai-generated/proofs/.
      await page.screenshot({ path: "test-results/live-proof-6256-recent-items.png" });
    },
  );

  test(
    "UI: a deleted entry is dropped with a status notice, not an error",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
      await page.evaluate(
        ({ prefix, payload }) => {
          window.localStorage.setItem(`${prefix}Admin`, JSON.stringify(payload));
        },
        {
          prefix: RECENT_KEY_PREFIX,
          payload: [
            {
              id: "99999999-999-999",
              name: "Deleted item",
              path: "/Sites/Demo/Deleted",
              kind: "item",
              openedAt: new Date(0).toISOString(),
            },
          ],
        },
      );
      // Fresh navigation (not reload) so the shell boots with the seeded entry.
      await page.goto(explorerRecentItemsUrl(BASE_URL), { waitUntil: "domcontentloaded" });
      await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
        timeout: 30_000,
      });
      await openRecentPanel(page);
      const row = page.locator(`[data-testid="${TEST_IDS.recentRow}"]`);
      await expect(row).toHaveCount(1);
      await page.locator(`[data-testid="${TEST_IDS.recentOpen}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.recentRow}"]`)).toHaveCount(0, {
        timeout: 20_000,
      });
      const status = page.locator(`[data-testid="${TEST_IDS.recentStatus}"]`);
      await expect(status).toBeVisible();
      await expect(status).toContainText("no longer available");
      await expect(page.locator(`[data-testid="${TEST_IDS.recentEmpty}"]`)).toBeVisible();
      await expect(page.getByRole("alert")).toHaveCount(0);
      await clearRecents(page);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );
});
