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
 * Explorer multi-select workflow transition (#4833 / parent #4530).
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-multi-workflow.spec.js}
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
        id: "43",
        name: "About",
        path: "/Sites/Demo/About",
        type: "percPage",
        category: "page",
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

function transitionBody(triggers) {
  return JSON.stringify({
    ItemStateTransition: {
      transitionTriggers: triggers,
      commentRequiredTriggers: [],
    },
  });
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {{ aboutStatus?: number }} [opts]
 */
async function installWorkflowRoutes(page, opts = {}) {
  const aboutStatus = opts.aboutStatus ?? 200;
  /** @type {string[]} */
  const invoked = [];
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/itemmanagement/workflow/getTransitions/**", async (route) => {
    const url = route.request().url();
    let triggers = ["Submit"];
    if (url.includes("/getTransitions/42")) {
      triggers = ["Submit", "Approve"];
    } else if (url.includes("/getTransitions/43")) {
      triggers = ["Submit"];
    } else {
      triggers = [];
    }
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: transitionBody(triggers),
    });
  });
  await page.route("**/itemmanagement/workflow/transitionWithComments/**", async (route) => {
    const url = route.request().url();
    invoked.push(url);
    const status = url.includes("/43/") ? aboutStatus : 200;
    await route.fulfill({
      status,
      contentType: "application/json",
      body: status === 200 ? "{}" : "denied",
    });
  });
  return invoked;
}

async function openListing(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
  await expect(home).toBeVisible({ timeout: 20_000 });
  await home.click();
  await page.locator('[data-testid="detail-select-42"]').check();
  await page.locator('[data-testid="detail-select-43"]').check();
  await page.locator('[data-testid="detail-select-7"]').check();
}

function submitButton(page) {
  return page.locator('[data-testid="action-toolbar-item-workflow-transition:Submit"]');
}

test.describe("Explorer multi-select workflow transition", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "one confirm transitions shared trigger and names skipped folders",
    { tag: ["@explorer", "@explorer-workflow", "@explorer-multi-workflow"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        expect(dialog.message()).toMatch(/Apply Submit to 2 selected items/i);
        void dialog.accept();
      });
      const invoked = await installWorkflowRoutes(page);
      await openListing(page);
      await expect(submitButton(page)).toBeVisible({ timeout: 15_000 });
      await expect(
        page.locator('[data-testid="action-toolbar-item-workflow-transition:Approve"]'),
      ).toHaveCount(0);
      await submitButton(page).click();
      await expect(
        page.locator('[data-testid="explorer-server-actions-error"]'),
      ).toContainText(/Folders are not transitioned: News/i, { timeout: 10_000 });
      await expect.poll(() => invoked.length).toBe(2);
      expect(invoked.some((url) => url.includes("/42/") && url.includes("Submit"))).toBe(true);
      expect(invoked.some((url) => url.includes("/43/") && url.includes("Submit"))).toBe(true);
      expect(invoked.some((url) => url.includes("/7/"))).toBe(false);
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "1",
      );
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "cancel transitions nothing",
    { tag: ["@explorer", "@explorer-workflow", "@explorer-multi-workflow"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.dismiss();
      });
      const invoked = await installWorkflowRoutes(page);
      await openListing(page);
      await expect(submitButton(page)).toBeVisible({ timeout: 15_000 });
      await submitButton(page).click();
      await expect(page.locator('[data-testid="explorer-nav"]')).toHaveAttribute(
        "data-list-epoch",
        "0",
      );
      expect(invoked).toEqual([]);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "HTTP 403 on one item is not a full-batch success",
    { tag: ["@explorer", "@explorer-workflow", "@explorer-multi-workflow"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      const invoked = await installWorkflowRoutes(page, { aboutStatus: 403 });
      await openListing(page);
      await expect(submitButton(page)).toBeVisible({ timeout: 15_000 });
      await submitButton(page).click();
      const error = page.locator('[data-testid="explorer-server-actions-error"]');
      await expect(error).toBeVisible({ timeout: 10_000 });
      await expect(error).toContainText(/About \(HTTP 403\)/i);
      await expect(error).toContainText(/Not every selected item was transitioned/i);
      await expect.poll(() => invoked.length).toBe(2);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
