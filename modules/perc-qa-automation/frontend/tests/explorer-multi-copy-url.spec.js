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
 * Explorer multi-select Copy URL to Clipboard (#4885 / parent #4530).
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-multi-copy-url.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const LISTING = {
  PagedItemList: {
    childrenInPage: [
      {
        id: "42",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
      {
        id: "9",
        name: "Logo",
        path: "/Assets/uploads/logo.png",
        type: "percImageAsset",
        category: "asset",
        accessLevel: "WRITE",
        leaf: true,
      },
      {
        id: "7",
        name: "News",
        path: "/Sites/Demo/News",
        type: "folder",
        category: "folder",
        accessLevel: "WRITE",
        leaf: false,
      },
    ],
    childrenCount: 3,
    startIndex: 0,
  },
};

const EMPTY_LISTING = {
  PagedItemList: {
    childrenInPage: [
      {
        id: "42",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
      {
        id: "44",
        name: "Orphan",
        path: " ",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
    ],
    childrenCount: 2,
    startIndex: 0,
  },
};

/**
 * @param {import('@playwright/test').Page} page
 * @param {object} listing
 */
async function installListing(page, listing) {
  await page.addInitScript(() => {
    const writes = [];
    window.__copiedUrls = writes;
    const writeText = async (text) => {
      writes.push(String(text));
    };
    try {
      Object.defineProperty(navigator, "clipboard", {
        configurable: true,
        value: { writeText },
      });
    } catch {
      navigator.clipboard = { writeText };
    }
  });
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(listing),
    });
  });
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string[]} ids
 */
async function openAndCheck(page, ids) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  const first = page.locator(`[data-testid="detail-row-${ids[0]}"][data-row-kind="item"]`);
  await expect(first).toBeVisible({ timeout: 20_000 });
  await first.click();
  for (const id of ids) {
    await page.locator(`[data-testid="detail-select-${id}"]`).check();
  }
}

test.describe("Explorer multi-select Copy URL (#4885)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "copies page and asset URLs and names the skipped folder",
    { tag: ["@explorer", "@explorer-copy-url", "@explorer-multi-copy-url"] },
    async ({ page }) => {
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });
      await installListing(page, LISTING);
      await openAndCheck(page, ["42", "9", "7"]);
      const copy = page.locator(
        '[data-testid="action-toolbar-item-Copy_URL_to_Clipboard"]',
      );
      await expect(copy).toBeVisible({ timeout: 15_000 });
      await copy.click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/Folders are not copied: News/i, { timeout: 10_000 });
      const written = await page.evaluate(() => window.__copiedUrls || []);
      expect(written).toHaveLength(1);
      const lines = String(written[0]).split("\n");
      expect(lines).toHaveLength(2);
      expect(lines[0].toLowerCase()).toContain("/sites/demo/home");
      expect(lines[1].toLowerCase()).toContain("/assets/uploads/logo.png");
      expect(String(written[0]).toLowerCase()).not.toContain("/sites/demo/news");
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "an empty URL is named and is not full success",
    { tag: ["@explorer", "@explorer-copy-url", "@explorer-multi-copy-url"] },
    async ({ page }) => {
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });
      await installListing(page, EMPTY_LISTING);
      await openAndCheck(page, ["42", "44"]);
      await page
        .locator('[data-testid="action-toolbar-item-Copy_URL_to_Clipboard"]')
        .click();
      const error = page.locator('[data-testid="explorer-server-actions-error"]');
      await expect(error).toBeVisible({ timeout: 10_000 });
      await expect(error).toContainText(/Orphan/i);
      await expect(error).toContainText(/Not every selected item URL was copied/i);
      const written = await page.evaluate(() => window.__copiedUrls || []);
      expect(written).toHaveLength(1);
      expect(String(written[0]).toLowerCase()).toContain("/sites/demo/home");
      expect(String(written[0]).split("\n")).toHaveLength(1);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
      expect(consoleErrors, consoleErrors.join(" | ")).toEqual([]);
    },
  );
});
