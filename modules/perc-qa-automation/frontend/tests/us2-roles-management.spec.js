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
 * Workflow Administration - Roles Management (993 US3 / P1).
 *
 * <p>Discoverable E2E for "Manage Roles": the unified Admin shell
 * ({@code view=workflow} &rarr; {@code /admin/*}, #3088) Roles tab renders
 * the role catalog, the create-role editor enforces the name-required
 * validation, and cancelling returns to the list without mutations.</p>
 *
 * <p>Tags: {@code @workflow-admin} {@code @administration}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/us2-roles-management.spec.js}</p>
 *
 * <p>Coverage map: {@code specs/993-workflow-admin-react-ui/e2e-coverage-map.md}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

test.describe("Workflow Administration - Roles Management (993 US3)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  async function gotoRolesTab(page) {
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
    await page.locator("[data-testid='tab-roles']").click();
    await expect(page.locator("[data-testid='perc-roles-section']")).toBeVisible({
      timeout: 30_000,
    });
    return pageErrors;
  }

  test(
    "roles tab renders the roles section with a create action",
    { tag: ["@workflow-admin", "@administration"] },
    async ({ page }) => {
      const pageErrors = await gotoRolesTab(page);

      // #2959: wrapper payload must not trip RouteErrorBoundary (e.map is not a function)
      await expect(page.locator("[data-testid='route-error']")).toHaveCount(0);
      await expect(page.getByText(/Unable to load Admin/i)).toHaveCount(0);

      await expect(
        page.locator("[data-testid='create-role-button']"),
      ).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "role catalog lists roles with edit and delete actions",
    { tag: ["@workflow-admin", "@administration"] },
    async ({ page }) => {
      const pageErrors = await gotoRolesTab(page);

      const cards = page.locator("[data-testid^='role-card-']");
      await expect(cards.first()).toBeVisible({ timeout: 30_000 });
      const count = await cards.count();
      expect(count).toBeGreaterThan(0);

      const firstId = await cards.first().getAttribute("data-testid");
      const roleName = String(firstId).replace(/^role-card-/, "");
      await expect(
        page.locator(`[data-testid='edit-role-${roleName}']`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid='delete-role-${roleName}']`),
      ).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "create role editor requires a name and cancels back to the list",
    { tag: ["@workflow-admin", "@administration"] },
    async ({ page }) => {
      const pageErrors = await gotoRolesTab(page);

      await page.locator("[data-testid='create-role-button']").click();
      await expect(page.locator("[data-testid='perc-role-editor']")).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.locator("[data-testid='role-name-input']")).toBeVisible();
      await expect(
        page.locator("[data-testid='role-description-input']"),
      ).toBeVisible();
      await expect(
        page.locator("[data-testid='available-users-list']"),
      ).toBeVisible();
      await expect(
        page.locator("[data-testid='assigned-users-list']"),
      ).toBeVisible();

      // Empty name must be rejected client-side; nothing is persisted.
      await page.locator("[data-testid='save-role-button']").click();
      await expect(
        page.locator("[data-testid='role-editor-error']"),
      ).toBeVisible();

      // Cancel returns to the list without creating anything.
      await page.locator("[data-testid='cancel-role-button']").click();
      await expect(page.locator("[data-testid='perc-role-editor']")).toHaveCount(0);
      await expect(page.locator("[data-testid='perc-roles-section']")).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
