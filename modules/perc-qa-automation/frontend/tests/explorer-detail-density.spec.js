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
 * Explorer detail-list row-density switch (#5883 / parent #4530).
 *
 * <p>A Comfortable/Compact switch above the list changes row metrics per
 * operator, persisted client-side in {@code localStorage} with zero new
 * REST traffic. Density is styling only: row identity, arrow-key row
 * navigation, and screen-reader row positions are identical in both
 * densities.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-detail-density.spec.js}
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
        id: "c-3",
        name: "Cherry",
        path: "/Sites/Demo/Cherry",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
      {
        id: "a-1",
        name: "apple",
        path: "/Sites/Demo/apple",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
      {
        id: "b-2",
        name: "Banana",
        path: "/Sites/Demo/Banana",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
    ],
    childrenCount: 3,
    startIndex: 0,
  },
};

/**
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<{ listingHits: () => number }>}
 */
async function installDensityRoutes(page) {
  let listingHits = 0;
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    listingHits += 1;
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  return { listingHits: () => listingHits };
}

async function openListing(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(page.locator('[data-testid="detail-row-c-3"]')).toBeVisible({
    timeout: 20_000,
  });
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function focusedRowTestId(page) {
  return page.evaluate(() => {
    const el = document.activeElement;
    return el ? el.getAttribute("data-testid") : null;
  });
}

test.describe("Explorer detail-list row density", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "compact tightens rows and the choice persists across loads",
    {
      tag: ["@explorer", "@explorer-detail-density"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      const routes = await installDensityRoutes(page);
      await openListing(page);

      const list = page.locator('[data-testid="detail-list"]');
      await expect(list).toHaveAttribute("data-density", "comfortable");
      await expect(
        page.locator('[data-testid="detail-density-comfortable"]'),
      ).toHaveAttribute("aria-pressed", "true");
      await expect(
        page.locator('[data-testid="detail-density-compact"]'),
      ).toHaveAttribute("aria-pressed", "false");

      const comfortableBox = await page
        .locator('[data-testid="detail-row-c-3"]')
        .boundingBox();

      await page.locator('[data-testid="detail-density-compact"]').click();
      await expect(list).toHaveAttribute("data-density", "compact");
      await expect(
        page.locator('[data-testid="detail-density-compact"]'),
      ).toHaveAttribute("aria-pressed", "true");
      await expect(
        page.locator('[data-testid="detail-density-comfortable"]'),
      ).toHaveAttribute("aria-pressed", "false");

      // Compact rows render shorter than comfortable rows.
      const compactBox = await page
        .locator('[data-testid="detail-row-c-3"]')
        .boundingBox();
      expect(comfortableBox).not.toBeNull();
      expect(compactBox).not.toBeNull();
      expect(compactBox.height).toBeLessThan(comfortableBox.height);

      // Persisted per operator in localStorage.
      const stored = await page.evaluate(() =>
        window.localStorage.getItem("perc.explorer.detailDensity.v1"),
      );
      expect(JSON.parse(stored)).toBe("compact");

      // Fresh page load restores compact (localStorage is origin-scoped,
      // so re-navigation keeps it; a bare reload drops the shell's
      // selected folder and renders no listing).
      await page.goto(explorerSpaUrl(BASE_URL));
      await page.waitForLoadState("networkidle");
      await expect(page.locator('[data-testid="detail-row-c-3"]')).toBeVisible({
        timeout: 20_000,
      });
      await expect(
        page.locator('[data-testid="detail-list"]'),
      ).toHaveAttribute("data-density", "compact");
      await expect(
        page.locator('[data-testid="detail-density-compact"]'),
      ).toHaveAttribute("aria-pressed", "true");

      // Client-side only: one fetch per page load, no density traffic.
      expect(routes.listingHits()).toBe(2);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "keyboard nav and row positions work in compact density",
    {
      tag: ["@explorer", "@explorer-detail-density"],
    },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await installDensityRoutes(page);
      await openListing(page);

      await page.locator('[data-testid="detail-density-compact"]').click();
      await expect(page.locator('[data-testid="detail-list"]')).toHaveAttribute(
        "data-density",
        "compact",
      );

      // Arrow-key row navigation still moves focus and selection.
      await page.locator('[data-testid="detail-row-c-3"]').focus();
      await page.keyboard.press("ArrowDown");
      expect(await focusedRowTestId(page)).toBe("detail-row-a-1");
      await expect(
        page.locator('[data-testid="content-explorer-shell"][data-selected-item-id="a-1"]'),
      ).toBeVisible({ timeout: 10_000 });

      // Home/End jump to the ends.
      await page.keyboard.press("End");
      expect(await focusedRowTestId(page)).toBe("detail-row-b-2");
      await page.keyboard.press("Home");
      expect(await focusedRowTestId(page)).toBe("detail-row-c-3");

      // Screen-reader row positions are identical in compact density.
      await expect(page.locator('[data-testid="detail-row-c-3"]')).toHaveAttribute(
        "aria-rowindex",
        "1",
      );
      await expect(page.locator('[data-testid="detail-row-b-2"]')).toHaveAttribute(
        "aria-rowindex",
        "3",
      );
      await expect(
        page.locator('[data-testid="detail-list"] table'),
      ).toHaveAttribute("aria-rowcount", "3");

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
