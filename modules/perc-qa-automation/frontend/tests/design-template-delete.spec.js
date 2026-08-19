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
 * Design SPA delete template without Widget XML (#3580 / parent #2631).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/design-template-delete.spec.js
 *
 * QA mode: perc-devctl qa-up → TEST_CMS_URL + ADMIN_* → test:surface → qa-down.
 *
 * Entry: spa.jsp?entry=design&section=templates
 * Flow: create (or use fixture) then delete with confirm; list refreshes.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  SKIP,
  designTemplatesUrl,
  filterConsoleNoise,
} = require("./helpers/design-spa-surface");

test.describe("Design template delete (#3580)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("creates then deletes a modern template and refreshes the list @smoke @ui", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });

    const name = `qa.delete.tpl.${Date.now()}`;

    await page.goto(designTemplatesUrl(BASE_URL), { waitUntil: "networkidle" });

    await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
      timeout: 20_000,
    });

    const createBtn = page.locator(`[data-testid="${TEST_IDS.create}"]`);
    await expect(createBtn).toBeVisible({ timeout: 30_000 });

    const error = page.locator(`[data-testid="${TEST_IDS.error}"]`);
    if (await error.isVisible()) {
      const msg = (await error.innerText()).trim();
      throw new Error(`Design templates catalog error: ${msg}`);
    }

    await createBtn.click();
    await expect(
      page.locator(`[data-testid="${TEST_IDS.createDialog}"]`),
    ).toBeVisible({ timeout: 10_000 });
    await page.locator(`[data-testid="${TEST_IDS.createName}"]`).fill(name);
    await page.locator(`[data-testid="${TEST_IDS.createSubmit}"]`).click();

    await expect(
      page.locator(`[data-testid="${TEST_IDS.createDialog}"]`),
    ).toHaveCount(0, { timeout: 20_000 });
    await expect(page.locator(`[data-testid="${TEST_IDS.table}"]`)).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(`[data-testid="${TEST_IDS.table}"]`)).toContainText(
      name,
    );

    const namedRow = page.locator(`[data-testid^="design-tpl-row"]`).filter({
      hasText: name,
    });
    await expect(namedRow).toHaveCount(1, { timeout: 10_000 });
    const deleteBtn = namedRow.locator('[data-testid^="design-tpl-delete-"]');
    if (!(await deleteBtn.isVisible().catch(() => false))) {
      test.skip(true, SKIP.DELETE);
      return;
    }
    await deleteBtn.click();

    await expect(
      page.locator(`[data-testid="${TEST_IDS.deleteDialog}"]`),
    ).toBeVisible({ timeout: 10_000 });
    await expect(
      page.locator(`[data-testid="${TEST_IDS.deleteConfirm}"]`),
    ).toContainText(name);
    await page.locator(`[data-testid="${TEST_IDS.deleteSubmit}"]`).click();

    await expect(
      page.locator(`[data-testid="${TEST_IDS.deleteDialog}"]`),
    ).toHaveCount(0, { timeout: 20_000 });
    await expect(
      page.locator(`[data-testid="${TEST_IDS.panel}"]`),
    ).not.toContainText(name, { timeout: 20_000 });

    expect(filterConsoleNoise(consoleErrors)).toEqual([]);
  });
});
