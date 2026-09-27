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
 * Where-used / Impact Analysis from the Explorer server-action catalog (#4944).
 *
 * The catalog URL is still Data Flow {@code sys_cxDependencyTree/dependencytree.html}.
 * Selecting a page or asset and running the action must list dependencies or the
 * empty state. A folder must stay selected and show a named error. HTTP 404
 * stays on Explorer as a not-found panel, not an empty graph.
 *
 * Run (QA mode after perc-devctl qa-up):
 *   npm run test:surface -- --path tests/explorer-where-used.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { paginatedFolderUrl } = require("./helpers/pathmanagement-url");
const {
  isKnownExplorerTransientNetworkConsoleNoise,
} = require("./helpers/explorer-console-clean");

const WHERE_USED = '[data-testid="action-toolbar-item-Item_ViewDependents"]';

function attachConsoleCleanGate(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (isKnownExplorerTransientNetworkConsoleNoise(text)) {
      return;
    }
    consoleErrors.push(text);
  });
  return { pageErrors, consoleErrors };
}

async function folderWithContent(page) {
  async function fetchChildren(folderPath) {
    const url = paginatedFolderUrl(BASE_URL, folderPath);
    const res = await page.request.get(url, {
      headers: { Accept: "application/json" },
    });
    if (!res.ok()) {
      return [];
    }
    const body = await res.json();
    return body?.PagedItemList?.childrenInPage ?? [];
  }

  async function walk(startPath, depth) {
    const children = await fetchChildren(startPath);
    const items = children.filter((child) => {
      const category = String(child?.category ?? "").toLowerCase();
      return (
        category === "page" ||
        category === "asset" ||
        category === "landing_page"
      );
    });
    if (items.length > 0) {
      return startPath;
    }
    if (depth <= 0) {
      return null;
    }
    for (const child of children) {
      const next = child.folderPath || child.path;
      if (!next) continue;
      const found = await walk(next, depth - 1);
      if (found) return found;
    }
    return null;
  }

  return walk("/Sites", 4);
}

test.describe("Explorer where-used for the selected item (#4944)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "page or asset lists where-used; folder stays selected with a named error",
    { tag: ["@explorer"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = attachConsoleCleanGate(page);
      const folderPath = await folderWithContent(page);
      expect(folderPath, "H2 sample content under /Sites").toBeTruthy();

      await page.goto(
        `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&path=${encodeURIComponent(folderPath)}&_=${Date.now()}`,
      );
      await page.waitForLoadState("networkidle");
      const list = page.locator('[data-testid="detail-list"]');
      await expect(list).toBeVisible({ timeout: 15_000 });

      const itemRow = list.locator(
        'tbody tr[data-testid^="detail-row-"][data-row-kind="item"]:not([aria-disabled="true"])',
      );
      await itemRow.first().click({ force: true, timeout: 10_000 });
      const whereUsed = page.locator(WHERE_USED);
      await expect(whereUsed).toBeVisible({ timeout: 15_000 });
      await whereUsed.click();

      const panel = page.locator('[data-testid="dependency-viewer"]');
      await expect(panel).toBeVisible({ timeout: 20_000 });
      await expect(panel).toHaveAttribute("data-testid-state", "ok", {
        timeout: 20_000,
      });
      await expect(
        page
          .locator('[data-testid="dependency-edges"]')
          .or(page.locator('[data-testid="dependency-empty"]'))
          .first(),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toHaveCount(0);

      const selectedBefore = await itemRow.first().getAttribute("data-testid");
      const folderRow = list.locator(
        'tbody tr[data-row-kind="folder"], tbody tr[data-testid^="detail-row-"][data-row-kind="site"]',
      );
      if ((await folderRow.count()) > 0) {
        await folderRow.first().click({ force: true, timeout: 10_000 });
        const onFolder = page.locator(WHERE_USED);
        if ((await onFolder.count()) > 0) {
          await onFolder.click();
          await expect(
            page.locator('[data-testid="explorer-server-actions-error"]'),
          ).toContainText(/select a content item first/i);
          await expect(page.locator('[data-testid="dependency-viewer"]')).toHaveCount(
            0,
          );
        }
      }
      expect(selectedBefore).toBeTruthy();
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "HTTP 404 stays on Explorer with a named not-found state",
    { tag: ["@explorer"] },
    async ({ page }) => {
      const { pageErrors, consoleErrors } = attachConsoleCleanGate(page);
      const folderPath = await folderWithContent(page);
      expect(folderPath).toBeTruthy();
      await page.route("**/content-explorer/relationships/**/summary", (route) =>
        route.fulfill({
          status: 404,
          contentType: "text/plain",
          body: "missing",
        }),
      );
      await page.goto(
        `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&path=${encodeURIComponent(folderPath)}&_=${Date.now()}`,
      );
      await page.waitForLoadState("networkidle");
      const list = page.locator('[data-testid="detail-list"]');
      await list
        .locator(
          'tbody tr[data-testid^="detail-row-"][data-row-kind="item"]:not([aria-disabled="true"])',
        )
        .first()
        .click({ force: true, timeout: 10_000 });
      await page.locator(WHERE_USED).click();
      const panel = page.locator('[data-testid="dependency-viewer"]');
      await expect(panel).toHaveAttribute("data-testid-state", "missing", {
        timeout: 20_000,
      });
      await expect(panel).toContainText(/not found/i);
      await expect(page).toHaveURL(/entry=explorer|\/explorer/);
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
    },
  );
});
