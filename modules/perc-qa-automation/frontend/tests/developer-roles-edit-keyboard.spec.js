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
 * Developer Roles edit-form keyboard path (#6905 / parent #1690; mirrors
 * slot #6713, display-format #6630/#5404, content-type/locale #6747).
 *
 * The description input autofocuses when the edit form opens, and Escape
 * cancels the edit without writing (same as Cancel): no PUT fires and the
 * stored description is kept.
 *
 * Focus return (#6733 pattern): when the form closes (Escape, Cancel, or a
 * successful save) focus returns to the catalog row that opened it instead
 * of falling back to <body>.
 *
 *   npm run test:surface -- --path tests/developer-roles-edit-keyboard.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

function developerRolesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "roles",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

function uniqueRoleName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  return `qa6905${`${a}${b}`.slice(0, 8) || "role"}`;
}

function attachConsole(page) {
  const consoleErrors = [];
  const pageErrors = [];
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  return { consoleErrors, pageErrors };
}

function assertClean(consoleErrors, pageErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpected = consoleErrors.filter((text) => {
    if (
      /Failed to load resource: the server responded with a status of (400|403|404|409)/.test(
        text,
      )
    ) {
      return false;
    }
    if (/rx_resources\/css\/(?:bootstrap|googleFonts)/.test(text)) {
      return false;
    }
    if (/favicon/i.test(text)) {
      return false;
    }
    return true;
  });
  expect(unexpected, `console error: ${unexpected.join(" | ")}`).toEqual([]);
}

function isRolePut(request) {
  return (
    request.method() === "PUT" &&
    /\/services\/roles\/?(?:\?|$)/.test(request.url()) &&
    !/\/services\/roles\/catalog/.test(request.url())
  );
}

function isRoleDelete(request, roleName) {
  return (
    request.method() === "DELETE" &&
    request.url().includes(`/services/roles/${encodeURIComponent(roleName)}`)
  );
}

async function openRoles(page) {
  await page.goto(developerRolesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="tab-developer-roles"]')).toBeVisible({
    timeout: 20_000,
  });
  const error = page.locator('[data-testid="developer-roles-error"]');
  const panel = page.locator('[data-testid="developer-roles-panel"]');
  const empty = page.locator('[data-testid="developer-roles-empty"]');
  await expect(panel.or(empty).or(error).first()).toBeVisible({ timeout: 30_000 });
  if (await error.isVisible()) {
    throw new Error(`Roles catalog error: ${(await error.innerText()).trim()}`);
  }
  await expect(page.locator('[data-testid="developer-roles-create"]')).toBeVisible();
}

async function createRole(page, roleName) {
  await page.locator('[data-testid="developer-roles-create"]').click();
  await page.locator('[data-testid="developer-roles-create-name"]').fill(roleName);
  await page.locator('[data-testid="developer-roles-create-description"]').fill("Keyboard path");
  const putDone = page.waitForResponse(
    (response) => isRolePut(response.request()) && response.url().includes("create=true"),
  );
  await page.locator('[data-testid="developer-roles-create-save"]').click();
  const response = await putDone;
  expect(response.status(), await response.text()).toBe(200);
  await expect(page.locator(`[data-role-name="${roleName}"]`).first()).toBeVisible({
    timeout: 30_000,
  });
}

async function deleteRole(page, roleName) {
  const deleted = page.waitForResponse((response) => isRoleDelete(response.request(), roleName));
  await page
    .locator(`[data-testid="developer-roles-delete"][data-role-name="${roleName}"]`)
    .first()
    .click();
  await confirmDeveloperCatalogDelete(page);
  const deleteResponse = await deleted;
  expect(deleteResponse.status(), await deleteResponse.text()).toBe(200);
  await expect(page.locator(`[data-role-name="${roleName}"]`)).toHaveCount(0);
}

function roleRow(page, roleName) {
  return page.locator(`[data-role-name="${roleName}"]`).first();
}

test.describe("Developer roles edit-form keyboard path (#6905)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("description input autofocuses and Escape cancels without writing", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openRoles(page);
    const roleName = uniqueRoleName();
    await createRole(page, roleName);

    const writes = [];
    page.on("request", (request) => {
      if (isRolePut(request) && !request.url().includes("create=true")) {
        writes.push(request.url());
      }
    });

    await roleRow(page, roleName).click();
    const descriptionInput = page.locator('[data-testid="developer-roles-edit-description"]');
    await expect(descriptionInput).toBeFocused();
    await descriptionInput.fill("not-saved");
    await descriptionInput.press("Escape");
    await expect(page.locator('[data-testid="developer-roles-edit-form"]')).toHaveCount(0);
    expect(writes, "Escape must not update the role").toEqual([]);
    await expect(page.locator(`[data-role-description="${roleName}"]`).first()).toHaveText(
      "Keyboard path",
    );
    await expect(roleRow(page, roleName)).toBeFocused();
    await page.screenshot({ path: `test-results/live-proof-6905-roles-edit-keyboard.png` });

    await deleteRole(page, roleName);
    assertClean(consoleErrors, pageErrors);
  });

  test("Cancel and Save return focus to the row; save notice is status", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openRoles(page);
    const roleName = uniqueRoleName();
    await createRole(page, roleName);

    // Cancel closes the form and returns focus without writing.
    await roleRow(page, roleName).click();
    await expect(page.locator('[data-testid="developer-roles-edit-description"]')).toBeFocused();
    await page.locator('[data-testid="developer-roles-edit-description"]').fill("not-saved");
    await page.locator('[data-testid="developer-roles-edit-cancel"]').click();
    await expect(page.locator('[data-testid="developer-roles-edit-form"]')).toHaveCount(0);
    await expect(roleRow(page, roleName)).toBeFocused();
    await expect(page.locator(`[data-role-description="${roleName}"]`).first()).toHaveText(
      "Keyboard path",
    );

    // Save writes, then returns focus to the row with a status notice.
    await roleRow(page, roleName).click();
    await page.locator('[data-testid="developer-roles-edit-description"]').fill("focus-note");
    const saved = page.waitForResponse(
      (response) => isRolePut(response.request()) && !response.url().includes("create=true"),
    );
    await page.locator('[data-testid="developer-roles-edit-save"]').click();
    const saveResponse = await saved;
    expect(saveResponse.status(), await saveResponse.text()).toBe(200);
    await expect(page.locator('[data-testid="developer-roles-edit-form"]')).toHaveCount(0);
    await expect(roleRow(page, roleName)).toBeFocused();
    await expect(page.locator(`[data-role-description="${roleName}"]`).first()).toHaveText(
      "focus-note",
      { timeout: 30_000 },
    );
    await expect(page.locator('[data-testid="developer-roles-edit-notice"]')).toHaveAttribute(
      "role",
      "status",
    );
    await page.screenshot({ path: `test-results/live-proof-6905-roles-edit-focus-return.png` });

    await deleteRole(page, roleName);
    assertClean(consoleErrors, pageErrors);
  });
});
