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
 * 008 US1 (T009): action menus offer exactly the permitted workflow
 * transitions (FR-001/002). The transitions endpoint is stubbed with the
 * production wire shape ({@code ItemStateTransition} wrapper); an empty
 * trigger list must omit the Workflow group entirely.
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-workflow-offers.spec.js}
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

function transitionBody(triggers, commentRequired = []) {
  return {
    ItemStateTransition: {
      itemId: "42",
      stateName: "Draft",
      workflowId: "5",
      transitionTriggers: triggers,
      commentRequiredTriggers: commentRequired,
    },
  };
}

async function installRoutes(page, triggers) {
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(LISTING),
    });
  });
  await page.route("**/workflow/getTransitions/**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(transitionBody(triggers)),
    });
  });
}

async function openRowMenu(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
  const home = page.locator('[data-testid="detail-row-42"][data-row-kind="item"]');
  await expect(home).toBeVisible({ timeout: 20_000 });
  await home.click({ button: "right" });
  await expect(page.locator('[data-testid="context-menu"]')).toBeVisible({
    timeout: 15_000,
  });
}

test.describe("Explorer workflow transition offers", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "menu offers exactly the permitted transitions",
    { tag: ["@explorer", "@explorer-workflow", "@explorer-workflow-offers"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await installRoutes(page, ["Approve", "Reject"]);
      await openRowMenu(page);
      await page.locator('[data-testid="context-menu-item-workflow"]').click();
      await expect(
        page.locator('[data-testid="context-menu-submenu-workflow"]'),
      ).toBeVisible({ timeout: 15_000 });
      await expect(
        page.locator('[data-testid="context-menu-item-workflow-transition:Approve"]'),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="context-menu-item-workflow-transition:Reject"]'),
      ).toBeVisible();
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="context-menu"]',
      });
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "no Workflow group when nothing is permitted",
    { tag: ["@explorer", "@explorer-workflow", "@explorer-workflow-empty"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await installRoutes(page, []);
      await openRowMenu(page);
      await expect(page.locator('[data-testid="context-menu-item-workflow"]')).toHaveCount(0);
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
