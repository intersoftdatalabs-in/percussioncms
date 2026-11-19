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
 * Admin Auth & MFA Readiness tool (Milestone 1 / #48).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/admin-auth-readiness.spec.js
 *   npm run test:surface -- --tag auth-readiness
 *
 * @tag auth-readiness
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
async function openAuthReadinessPanel(page) {
  await page.goto(adminToolsUrl(), { waitUntil: "domcontentloaded" });
  await expect(page.getByTestId("perc-spa-app")).toBeVisible({
    timeout: 30_000,
  });
  await expect(page.getByTestId("perc-admin-shell")).toBeVisible({
    timeout: 30_000,
  });
  const toolsTab = page.getByTestId("tab-tools");
  if (await toolsTab.isVisible().catch(() => false)) {
    await toolsTab.click();
  }
  await expect(page.getByTestId("perc-tools-section")).toBeVisible({
    timeout: 15_000,
  });
  await page.getByTestId("tool-tab-auth-readiness").click();
  await expect(page.getByTestId("perc-auth-readiness")).toBeVisible({
    timeout: 20_000,
  });
}

test.describe("Admin Auth & MFA Readiness @auth-readiness @admin", () => {
  test("Admin can open readiness report with summary, infra checks, and mail test form", async ({
    page,
  }) => {
    await loginAsAdmin(page);
    await openAuthReadinessPanel(page);

    await expect(page.getByTestId("auth-readiness-summary")).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId("auth-readiness-infra-table")).toBeVisible();
    await expect(
      page.getByTestId("auth-readiness-mail-test-form"),
    ).toBeVisible();
    await expect(page.getByTestId("auth-readiness-error")).toHaveCount(0);
    await expect(page.getByTestId("admin-section-error")).toHaveCount(0);
  });

  test("non-Admin is denied Admin tools and auth readiness panel", async ({
    page,
  }) => {
    await loginAsEditor(page);
    await page.goto(adminToolsUrl(), { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("perc-spa-app")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("perc-auth-readiness")).toHaveCount(0, {
      timeout: 15_000,
    });
    await expect(page.getByTestId("perc-admin-shell")).toHaveCount(0);
  });
});
