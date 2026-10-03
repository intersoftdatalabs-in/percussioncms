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
 * Developer Roles delete (#5103 / #1690).
 *
 * Admin confirms and deletes one non-system role. The catalog row is gone
 * only after DELETE succeeds. Cancel does not call the server. System and
 * Default stay. HTTP 400, 403, and 409 do not claim the role was deleted.
 *
 *   npm run test:surface -- --path tests/developer-roles-delete.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerRolesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "roles",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
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

function assertClean(consoleErrors, pageErrors, { allowHttpStatus = false } = {}) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpected = consoleErrors.filter((text) => {
    if (
      allowHttpStatus &&
      /Failed to load resource: the server responded with a status of (400|403|409)/.test(
        text,
      )
    ) {
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

function isRoleDelete(request) {
  return (
    request.method() === "DELETE" &&
    /\/services\/roles\/[^/?]+/.test(request.url()) &&
    !/\/services\/roles\/catalog/.test(request.url())
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
  await page
    .locator('[data-testid="developer-roles-create-description"]')
    .fill("Delete from Developer Roles");
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

function deleteButton(page, roleName) {
  return page.locator(
    `[data-testid="developer-roles-delete"][data-role-name="${roleName}"]`,
  );
}

test.describe("Developer delete a role (#5103)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("cancel does not delete and system roles stay disabled", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const deletes = [];
    page.on("request", (request) => {
      if (isRoleDelete(request)) {
        deletes.push(request.url());
      }
    });

    await openRoles(page);
    const system = deleteButton(page, "Default");
    await expect(system.first()).toBeVisible();
    const systemCount = await system.count();
    expect(systemCount).toBeGreaterThan(0);
    for (let i = 0; i < systemCount; i += 1) {
      await expect(system.nth(i)).toBeDisabled();
    }

    const roleName = `Nd${Date.now()}`;
    await createRole(page, roleName);
    await deleteButton(page, roleName).first().click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-body"]')).toContainText(
      roleName,
    );
    await page.locator('[data-testid="developer-catalog-confirm-cancel"]').click();
    await expect(page.locator('[data-testid="developer-catalog-confirm-dialog"]')).toHaveCount(0);
    await expect(page.locator(`[data-role-name="${roleName}"]`).first()).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-delete-notice"]')).toHaveCount(0);
    expect(deletes, `unexpected role DELETE: ${deletes.join(" | ")}`).toEqual([]);
    assertClean(consoleErrors, pageErrors);
  });

  test("catalog drops the row only after delete succeeds", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const roleName = `Nd${Date.now()}`;
    await openRoles(page);
    await createRole(page, roleName);

    await deleteButton(page, roleName).first().click();
    const deleted = page.waitForResponse(
      (response) =>
        isRoleDelete(response.request()) &&
        decodeURIComponent(response.url()).includes(roleName),
    );
    await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
    const response = await deleted;
    expect(response.status(), await response.text()).toBe(200);
    await expect(page.locator(`[data-role-name="${roleName}"]`)).toHaveCount(0, {
      timeout: 30_000,
    });
    await expect(page.locator('[data-testid="developer-roles-delete-notice"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-delete-error"]')).toHaveCount(0);
    assertClean(consoleErrors, pageErrors);
  });

  test("HTTP 400, 403, and 409 do not claim the role was deleted", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openRoles(page);
    const roleName = `Nd${Date.now()}`;
    await createRole(page, roleName);

    let deleteRouted = false;
    for (const status of [400, 403, 409]) {
      if (deleteRouted) {
        await page.unroute("**/services/roles/**");
      }
      await page.route("**/services/roles/**", async (route) => {
        const request = route.request();
        if (isRoleDelete(request)) {
          await route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message: `Rejected ${status}` }),
          });
          return;
        }
        await route.continue();
      });
      await deleteButton(page, roleName).first().click();
      await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
      await expect(page.locator('[data-testid="developer-roles-delete-error"]')).toContainText(
        String(status),
      );
      await expect(page.locator(`[data-role-name="${roleName}"]`).first()).toBeVisible();
      await expect(page.locator('[data-testid="developer-roles-delete-notice"]')).toHaveCount(0);
      deleteRouted = true;
    }
    assertClean(consoleErrors, pageErrors, { allowHttpStatus: true });
  });
});
