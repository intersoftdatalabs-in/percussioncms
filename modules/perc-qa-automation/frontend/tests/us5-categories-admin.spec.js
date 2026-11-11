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
 * Workflow Administration - Categories Admin (993 US6 / P2).
 *
 * <p>Discoverable E2E for "Manage Categories": the unified Admin shell
 * ({@code view=workflow} &rarr; {@code /admin/*}, #3088) Categories tab
 * renders the hierarchy tree with lock controls, and the add-category
 * dialog opens with its inputs and cancels cleanly without mutations.</p>
 *
 * <p>Tags: {@code @workflow-admin} {@code @administration}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/us5-categories-admin.spec.js}</p>
 *
 * <p>Coverage map: {@code specs/993-workflow-admin-react-ui/e2e-coverage-map.md}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

test.describe("Workflow Administration - Categories Admin (993 US6)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  async function gotoCategoriesTab(page) {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        pageErrors.push(msg.text());
      }
    });
    await page.goto(`${BASE_URL}/cm/app/index.jsp?view=workflow`);
    await expect(page.locator("[data-testid='perc-admin-shell']")).toBeVisible({
      timeout: 30_000,
    });
    await page.locator("[data-testid='tab-categories']").click();
    await expect(
      page.locator("[data-testid='perc-categories-section']"),
    ).toBeVisible({ timeout: 30_000 });
    return pageErrors;
  }

  test(
    "categories tab renders the tree section with lock controls",
    { tag: ["@workflow-admin", "@administration"] },
    async ({ page }) => {
      const pageErrors = await gotoCategoriesTab(page);

      // #2959: wrapper payload must not trip RouteErrorBoundary (e.map is not a function)
      await expect(page.locator("[data-testid='route-error']")).toHaveCount(0);
      await expect(page.getByText(/Unable to load Admin/i)).toHaveCount(0);

      // Either the tab is unlocked (acquire action) or another session holds
      // the lock (banner); both are decided, rendered states.
      const acquireBtn = page.locator("[data-testid='acquire-lock-btn']");
      const lockBanner = page.locator("[data-testid='lock-banner']");
      if (await acquireBtn.isVisible()) {
        await expect(lockBanner).toHaveCount(0);
      } else {
        await expect(lockBanner).toBeVisible();
      }
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "category tree renders nodes or the empty state",
    { tag: ["@workflow-admin", "@administration"] },
    async ({ page }) => {
      const pageErrors = await gotoCategoriesTab(page);

      const nodes = page.locator("[data-testid^='category-node-']");
      const count = await nodes.count();
      if (count === 0) {
        await expect(page.locator("[data-testid='perc-categories-section']")).toContainText(
          /No categories available/i,
        );
      } else {
        await expect(nodes.first()).toBeVisible();
        const firstId = await nodes.first().getAttribute("data-testid");
        const nodeId = String(firstId).replace(/^category-node-/, "");
        // Lock-state indicator is always rendered for a node.
        await expect(
          page.locator(`[data-testid='lock-indicator-${nodeId}']`),
        ).toBeVisible();
      }
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "add root category dialog opens with inputs and cancels cleanly",
    { tag: ["@workflow-admin", "@administration"] },
    async ({ page }) => {
      const pageErrors = await gotoCategoriesTab(page);

      const addRootBtn = page.locator("[data-testid='add-root-category']");
      if (!(await addRootBtn.isVisible())) {
        // Locked by another session: adding is correctly unavailable.
        await expect(page.locator("[data-testid='lock-banner']")).toBeVisible();
        expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
          [],
        );
        return;
      }

      await addRootBtn.click();
      const dialog = page.locator("[data-testid='add-category-dialog']");
      await expect(dialog).toBeVisible({ timeout: 30_000 });
      await expect(
        page.locator("[data-testid='new-category-title-input']"),
      ).toBeVisible();
      await expect(
        page.locator("[data-testid='new-category-selectable-input']"),
      ).toBeVisible();
      await expect(page.locator("[data-testid='save-category-btn']")).toBeVisible();

      // Cancel closes the dialog without creating anything.
      await dialog.locator("button.perc-button-secondary").click();
      await expect(dialog).toHaveCount(0);
      await expect(
        page.locator("[data-testid='perc-categories-section']"),
      ).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
