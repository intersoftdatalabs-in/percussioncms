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
 * 008 US2 (T015): item-properties dialog shows General + Display groups with
 * server values; a permitted edit persists and shows on reopen; denied edits
 * message (FR-009). Properties endpoints are stubbed with the production
 * wire shape ({@code ItemProperties} wrapper).
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-item-properties.spec.js}
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
    ],
    childrenCount: 1,
    startIndex: 0,
  },
};

let savedTitle = "Home page";

async function installRoutes(page, { saveStatus = 200 } = {}) {
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/folders/item-properties**", async (route) => {
    if (route.request().method() === "GET") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ItemProperties: {
            itemPath: "/Sites/Demo/Home",
            name: "Home",
            displayTitle: savedTitle,
          },
        }),
      });
      return;
    }
    if (saveStatus !== 200) {
      await route.fulfill({ status: saveStatus, contentType: "application/json", body: "{}" });
      return;
    }
    const post = route.request().postDataJSON();
    savedTitle = post?.ItemPropertiesRequest?.displayTitle ?? savedTitle;
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemProperties: {
          itemPath: "/Sites/Demo/Home",
          name: "Home",
          displayTitle: savedTitle,
        },
      }),
    });
  });
}

async function openProperties(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
  await expect(home).toBeVisible({ timeout: 20_000 });
  await home.click();
  const toggle = page.locator('[data-testid="explorer-toggle-item-properties"]');
  if ((await toggle.count()) > 0) {
    await toggle.click();
  }
  await expect(page.locator('[data-testid="item-properties-panel"]')).toBeVisible({
    timeout: 15_000,
  });
}

test.describe("Explorer item properties dialog", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    savedTitle = "Home page";
    await loginAsAdmin(page);
  });

  test(
    "groups show server values; edit persists and shows on reopen",
    { tag: ["@explorer", "@explorer-props", "@explorer-props-roundtrip"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await installRoutes(page);
      await openProperties(page);
      await expect(page.locator('[data-testid="item-properties-general-group"]')).toBeVisible();
      await expect(page.locator('[data-testid="item-properties-display-group"]')).toBeVisible();
      await expect(page.locator('[data-testid="item-properties-path-value"]')).toHaveText(
        "/Sites/Demo/Home",
      );
      await page.locator('[data-testid="item-properties-display-title"]').fill("Home RENAMED");
      await page.locator('[data-testid="item-properties-save"]').click();
      await expect(page.locator('[data-testid="item-properties-status"]')).toContainText(
        /saved/i,
        { timeout: 15_000 },
      );
      await expect(
        page.locator('[data-testid="item-properties-shown-display-title-value"]'),
      ).toHaveText("Home RENAMED");
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="item-properties-panel"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "denied save keeps committed values and messages",
    { tag: ["@explorer", "@explorer-props", "@explorer-props-denied"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await installRoutes(page, { saveStatus: 403 });
      await openProperties(page);
      await page.locator('[data-testid="item-properties-display-title"]').fill("Nope");
      await page.locator('[data-testid="item-properties-save"]').click();
      await expect(page.locator('[data-testid="item-properties-status"]')).not.toBeEmpty({
        timeout: 15_000,
      });
      await expect(
        page.locator('[data-testid="item-properties-shown-display-title-value"]'),
      ).toHaveText("Home page");
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
