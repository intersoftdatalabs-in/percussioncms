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
 * Admin Security Audit Log viewer (Phase 4 / #2619).
 *
 * Surface-filtered only — not full suite:
 *   npm run test:surface -- --path tests/admin-security-audit-log.spec.js
 *   npm run test:surface -- --tag security-audit-log
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * @tag security-audit-log
 * @tag admin
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, loginAsEditor, BASE_URL } = require("./helpers/auth");

function adminToolsUrl() {
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=admin&tab=tools&_=${Date.now()}`;
}

/**
 * @param {import('@playwright/test').Page} page
 */
async function openSecurityAuditLog(page) {
  await page.goto(adminToolsUrl(), { waitUntil: "domcontentloaded" });
  await expect(page.getByTestId("perc-spa-app")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-admin-shell")).toBeVisible({
    timeout: 30_000,
  });
  // Deep link may land on tools; ensure tools tab is active.
  const toolsTab = page.getByTestId("tab-tools");
  if (await toolsTab.isVisible().catch(() => false)) {
    await toolsTab.click();
  }
  await expect(page.getByTestId("perc-tools-section")).toBeVisible({
    timeout: 15_000,
  });
  const auditToolTab = page.getByTestId("tool-tab-security-audit");
  if (await auditToolTab.isVisible().catch(() => false)) {
    await auditToolTab.click();
  }
  await expect(page.getByTestId("perc-security-audit-log")).toBeVisible({
    timeout: 20_000,
  });
}

test.describe("Admin Security Audit Log @security-audit-log @admin", () => {
  test("Admin can open viewer with filters and table", async ({ page }) => {
    await loginAsAdmin(page);
    await openSecurityAuditLog(page);

    await expect(page.getByTestId("audit-log-filters")).toBeVisible();
    await expect(page.getByTestId("audit-filter-module")).toBeVisible();
    await expect(page.getByTestId("audit-filter-apply")).toBeVisible();
    await expect(page.getByTestId("audit-log-table")).toBeVisible();

    // Either empty state or at least one row after load settles.
    await expect(
      page
        .getByTestId("audit-log-empty")
        .or(page.locator("[data-testid^='audit-log-row-']").first()),
    ).toBeVisible({ timeout: 20_000 });
  });

  test("Admin can apply module filter without error chrome", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await openSecurityAuditLog(page);

    await page.getByTestId("audit-filter-module").fill("AUTH");
    await page.getByTestId("audit-filter-apply").click();

    // Wait for load cycle — error banner must not appear for Admin.
    await page.waitForTimeout(500);
    await expect(page.getByTestId("audit-log-error")).toHaveCount(0);
    await expect(page.getByTestId("audit-log-table")).toBeVisible();
  });

  test("non-Admin is denied Admin tools (redirect away from shell)", async ({
    page,
  }) => {
    await loginAsEditor(page);
    await page.goto(adminToolsUrl(), { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("perc-spa-app")).toBeVisible({
      timeout: 30_000,
    });
    // RequireRole gate → navigate home; security audit shell must not mount.
    await expect(page.getByTestId("perc-security-audit-log")).toHaveCount(0, {
      timeout: 15_000,
    });
    await expect(page.getByTestId("perc-admin-shell")).toHaveCount(0);
  });
});
