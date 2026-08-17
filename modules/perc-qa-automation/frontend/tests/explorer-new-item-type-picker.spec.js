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
 * Explorer New Item content-type picker (#3513).
 *
 * <p>Tags: {@code @explorer-new-item} {@code @explorer}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-new-item-type-picker.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

function isFeatureUrl(url) {
  const u = String(url || "");
  return (
    /\/actions\/find/i.test(u) ||
    /\/itemmanagement\/item\/create/i.test(u) ||
    /\/services\/contenttypes/i.test(u)
  );
}

function collectFeatureErrors(page) {
  const pageErrors = [];
  const featureHttpErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("response", (res) => {
    if (res.status() < 500) {
      return;
    }
    const url = res.url();
    if (isFeatureUrl(url)) {
      featureHttpErrors.push(`${res.status()} ${res.request().method()} ${url}`);
    }
  });
  return { pageErrors, featureHttpErrors };
}

async function stubNewItemHostLeaf(page, createBodies) {
  await page.route("**/actions/find**", async (route) => {
    const url = route.request().url();
    if (url.includes("/actions/find/types")) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          ActionMenuList: [
            { name: "percFile", label: "File", sortRank: 1, menuType: "MENUITEM" },
            { name: "rffEvent", label: "Event", sortRank: 2, menuType: "MENUITEM" },
          ],
        }),
      });
      return;
    }
    if (route.request().method() !== "GET") {
      await route.continue();
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ActionMenu: [
          {
            name: "New",
            label: "New Item",
            sortRank: 0,
            menuType: "MENUITEM",
          },
        ],
      }),
    });
  });
  await page.route("**/services/contenttypes**", async (route) => {
    if (route.request().method() !== "GET") {
      await route.continue();
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ContentType: [
          { name: "percFile", label: "File" },
          { name: "rffEvent", label: "Event" },
        ],
      }),
    });
  });
  await page.route("**/itemmanagement/item/create**", async (route) => {
    createBodies.push(route.request().postData() || "");
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        ItemCreateResult: {
          itemId: "1-101-88",
          folderPath: "//Sites/Demo",
          name: "New-rffEvent",
          contentType: "rffEvent",
        },
      }),
    });
  });
}

async function openExplorerOnSitesFolder(page) {
  await page.goto(`${explorerSpaUrl(BASE_URL)}&path=/Sites`, {
    waitUntil: "networkidle",
  });
  await expect(page.locator('[data-testid="action-toolbar"]')).toBeVisible({
    timeout: 20_000,
  });
  const tree = page.locator('[data-testid="explorer-tree"]');
  if ((await tree.count()) > 0) {
    const sitesNode = page
      .locator(
        '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"], [data-testid*="tree-node"][data-testid*="Sites"]',
      )
      .first();
    if ((await sitesNode.count()) > 0) {
      await sitesNode.click({ force: true, timeout: 10_000 }).catch(() => {});
    }
  }
}

test.describe("Explorer New Item content-type picker (#3513)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test(
    "New Item host opens a type picker instead of an error toast",
    { tag: ["@explorer-new-item", "@explorer"] },
    async ({ page }) => {
      const { pageErrors, featureHttpErrors } = collectFeatureErrors(page);
      const createBodies = [];
      await stubNewItemHostLeaf(page, createBodies);
      await openExplorerOnSitesFolder(page);

      const neu = page.locator('[data-testid="action-toolbar-item-New"]');
      await expect(neu).toBeVisible({ timeout: 15_000 });
      await neu.click();

      const picker = page.locator('[data-testid="explorer-type-picker"]');
      await expect(picker).toBeVisible({ timeout: 10_000 });
      await expect(page.getByText("Choose a content type from New Item")).toHaveCount(0);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="explorer-type-picker"]',
      });
      await page.locator('[data-testid="explorer-type-picker-cancel"]').click();
      await expect(picker).toHaveCount(0);
      expect(createBodies, "Cancel must not POST create").toEqual([]);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      expect(
        featureHttpErrors,
        `feature HTTP 5xx: ${featureHttpErrors.join(" | ")}`,
      ).toEqual([]);
    },
  );

  test(
    "New Item picker creates the selected type and does not open leftover CE HTML",
    { tag: ["@explorer-new-item", "@explorer"] },
    async ({ page }) => {
      const { pageErrors, featureHttpErrors } = collectFeatureErrors(page);
      const blocked = [];
      page.on("request", (req) => {
        const u = req.url();
        if (
          u.includes("rx_ce") ||
          u.includes("contenteditorurls.html") ||
          u.includes("checkoutedit.xml")
        ) {
          blocked.push(u);
        }
      });
      const createBodies = [];
      await stubNewItemHostLeaf(page, createBodies);
      await openExplorerOnSitesFolder(page);

      await page.locator('[data-testid="action-toolbar-item-New"]').click();
      const picker = page.locator('[data-testid="explorer-type-picker"]');
      await expect(picker).toBeVisible({ timeout: 10_000 });
      await page.locator('[data-testid="explorer-type-picker-select"]').selectOption("rffEvent");
      await page.locator('[data-testid="explorer-type-picker-ok"]').click();
      await expect.poll(() => createBodies.length).toBe(1);
      expect(createBodies[0]).toMatch(/"contentType"\s*:\s*"rffEvent"/);
      expect(blocked, `Data Flow CE HTML must not be requested: ${blocked.join(" ")}`).toEqual([]);
      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      expect(
        featureHttpErrors,
        `feature HTTP 5xx: ${featureHttpErrors.join(" | ")}`,
      ).toEqual([]);
    },
  );
});
