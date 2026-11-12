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
 * Workflow Administration - Users Management (993 US4 / P1).
 *
 * <p>Discoverable E2E for "Manage Users": the unified Admin shell
 * ({@code view=workflow} &rarr; {@code /admin/*}, #3088) Users tab renders
 * the user catalog, the create-user editor enforces the name-required
 * validation, and the LDAP import dialog opens for directory search.</p>
 *
 * <p>Tags: {@code @workflow-admin} {@code @administration}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/us3-users-management.spec.js}</p>
 *
 * <p>Coverage map: {@code specs/993-workflow-admin-react-ui/e2e-coverage-map.md}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

test.describe("Workflow Administration - Users Management (993 US4)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  async function gotoUsersTab(page) {
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
    await page.locator("[data-testid='tab-users']").click();
    await expect(page.locator("[data-testid='perc-users-section']")).toBeVisible({
      timeout: 30_000,
    });
    return pageErrors;
  }

  test(
    "users tab renders the users section with create and LDAP import actions",
    { tag: ["@workflow-admin", "@administration"] },
    async ({ page }) => {
      const pageErrors = await gotoUsersTab(page);

      // #2959: wrapper payload must not trip RouteErrorBoundary (e.map is not a function)
      await expect(page.locator("[data-testid='route-error']")).toHaveCount(0);
      await expect(page.getByText(/Unable to load Admin/i)).toHaveCount(0);

      await expect(
        page.locator("[data-testid='create-user-button']"),
      ).toBeVisible();
      await expect(
        page.locator("[data-testid='ldap-import-button']"),
      ).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "user catalog lists users with edit and delete actions",
    { tag: ["@workflow-admin", "@administration"] },
    async ({ page }) => {
      const pageErrors = await gotoUsersTab(page);

      const cards = page.locator("[data-testid^='user-card-']");
      await expect(cards.first()).toBeVisible({ timeout: 30_000 });
      const count = await cards.count();
      expect(count).toBeGreaterThan(0);

      const firstId = await cards.first().getAttribute("data-testid");
      const userName = String(firstId).replace(/^user-card-/, "");
      await expect(
        page.locator(`[data-testid='edit-user-${userName}']`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid='delete-user-${userName}']`),
      ).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "create user editor requires a name and cancels back to the list",
    { tag: ["@workflow-admin", "@administration"] },
    async ({ page }) => {
      const pageErrors = await gotoUsersTab(page);

      await page.locator("[data-testid='create-user-button']").click();
      await expect(page.locator("[data-testid='perc-user-editor']")).toBeVisible({
        timeout: 30_000,
      });
      await expect(page.locator("[data-testid='user-name-input']")).toBeVisible();
      await expect(page.locator("[data-testid='user-email-input']")).toBeVisible();
      await expect(
        page.locator("[data-testid='save-user-button']"),
      ).toBeVisible();

      // Empty name must be rejected client-side; nothing is persisted.
      await page.locator("[data-testid='save-user-button']").click();
      await expect(
        page.locator("[data-testid='user-editor-error']"),
      ).toBeVisible();

      // Cancel (header cancel button) returns to the list without creating anything.
      await page
        .locator("[data-testid='perc-user-editor'] button[type='button']")
        .first()
        .click();
      await expect(page.locator("[data-testid='perc-user-editor']")).toHaveCount(0);
      await expect(page.locator("[data-testid='perc-users-section']")).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "LDAP import dialog opens for directory search and closes cleanly",
    { tag: ["@workflow-admin", "@administration"] },
    async ({ page }) => {
      const pageErrors = await gotoUsersTab(page);

      await page.locator("[data-testid='ldap-import-button']").click();
      const dialog = page.locator("[data-testid='perc-ldap-dialog-overlay']");
      await expect(dialog).toBeVisible({ timeout: 30_000 });

      // LDAP may be disabled in QA H2; either the search form or the
      // disabled notice proves the dialog reached its decided state.
      const searchInput = page.locator("[data-testid='ldap-search-input']");
      if (await searchInput.isVisible()) {
        await expect(
          page.locator("[data-testid='ldap-search-submit']"),
        ).toBeVisible();
      } else {
        await expect(dialog).toContainText(/LDAP|disabled|not (enabled|configured)/i);
      }

      // Header close button is the first button in the overlay.
      await dialog.locator("button").first().click();
      await expect(dialog).toHaveCount(0);
      await expect(page.locator("[data-testid='perc-users-section']")).toBeVisible();
      expect(pageErrors, `console/page errors: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
