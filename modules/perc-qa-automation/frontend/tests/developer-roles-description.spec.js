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
 * Developer Roles description update (#5102 / #1690).
 *
 * Admin opens one existing non-system role and saves a new description.
 * Reload shows it. Cancel does not call the server. HTTP 400/403/404 do not
 * claim the description changed.
 *
 *   npm run test:surface -- --path tests/developer-roles-description.spec.js
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
      /Failed to load resource: the server responded with a status of (400|403|404)/.test(
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

function isDescriptionUpdate(request) {
  return isRolePut(request) && /[?&]update=true(?:&|$)/.test(request.url());
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

async function createRole(page, roleName, description) {
  await page.locator('[data-testid="developer-roles-create"]').click();
  await page.locator('[data-testid="developer-roles-create-name"]').fill(roleName);
  await page.locator('[data-testid="developer-roles-create-description"]').fill(description);
  const putDone = page.waitForResponse(
    (response) =>
      isRolePut(response.request()) && response.url().includes("create=true"),
  );
  await page.locator('[data-testid="developer-roles-create-save"]').click();
  const response = await putDone;
  expect(response.status(), await response.text()).toBe(200);
  await expect(page.locator(`[data-role-name="${roleName}"]`).first()).toBeVisible({
    timeout: 30_000,
  });
}

async function descriptionText(page, roleName) {
  return page.locator(`[data-role-description="${roleName}"]`).first().innerText();
}

test.describe("Developer update a role description (#5102)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("cancel does not call the server", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const updates = [];
    page.on("request", (request) => {
      if (isDescriptionUpdate(request)) {
        updates.push(request.url());
      }
    });
    await openRoles(page);
    const row = page.locator("[data-role-name]").first();
    await expect(row).toBeVisible();
    const roleName = await row.getAttribute("data-role-name");
    expect(roleName).toBeTruthy();
    const before = (await descriptionText(page, roleName)).trim();
    await row.click();
    await expect(page.locator('[data-testid="developer-roles-edit-name"]')).toHaveValue(roleName);
    await page.locator('[data-testid="developer-roles-edit-description"]').fill("Not saved description");
    await page.locator('[data-testid="developer-roles-edit-cancel"]').click();
    await expect(page.locator('[data-testid="developer-roles-edit-form"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-roles-edit-notice"]')).toHaveCount(0);
    expect((await descriptionText(page, roleName)).trim()).toBe(before);
    expect(updates, `unexpected description PUT: ${updates.join(" | ")}`).toEqual([]);
    assertClean(consoleErrors, pageErrors);
  });

  test("reload shows the saved description", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const roleName = `Nr${Date.now()}`;
    const next = `After ${Date.now()}`;
    await openRoles(page);
    await createRole(page, roleName, "Before edit");
    await expect(page.locator(`[data-role-description="${roleName}"]`).first()).toHaveText(
      "Before edit",
    );
    await page.locator(`[data-role-name="${roleName}"]`).first().click();
    await page.locator('[data-testid="developer-roles-edit-description"]').fill(next);
    const putDone = page.waitForResponse((response) =>
      isDescriptionUpdate(response.request()),
    );
    await page.locator('[data-testid="developer-roles-edit-save"]').click();
    const response = await putDone;
    expect(response.status(), await response.text()).toBe(200);
    expect(response.url()).not.toContain("create=true");
    await expect(page.locator(`[data-role-description="${roleName}"]`).first()).toHaveText(next, {
      timeout: 30_000,
    });
    await expect(page.locator('[data-testid="developer-roles-edit-notice"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-edit-error"]')).toHaveCount(0);
    await openRoles(page);
    await expect(page.locator(`[data-role-description="${roleName}"]`).first()).toHaveText(next, {
      timeout: 30_000,
    });
    assertClean(consoleErrors, pageErrors);
  });

  test("HTTP 400, 403, and 404 do not claim the description changed", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const statuses = [400, 403, 404];
    let forced = 0;
    await page.route("**/services/roles/**", async (route) => {
      const request = route.request();
      if (isDescriptionUpdate(request) && forced < statuses.length) {
        const status = statuses[forced];
        forced += 1;
        await route.fulfill({
          status,
          contentType: "application/json",
          body: JSON.stringify({ message: `forced ${status}` }),
        });
        return;
      }
      await route.continue();
    });
    await openRoles(page);
    const row = page.locator("[data-role-name]").first();
    await expect(row).toBeVisible();
    const roleName = await row.getAttribute("data-role-name");
    const before = (await descriptionText(page, roleName)).trim();
    await row.click();
    for (const status of statuses) {
      await page.locator('[data-testid="developer-roles-edit-description"]').fill(`Rejected ${status}`);
      await page.locator('[data-testid="developer-roles-edit-save"]').click();
      await expect(page.locator('[data-testid="developer-roles-edit-error"]')).toContainText(
        `forced ${status}`,
      );
      expect((await descriptionText(page, roleName)).trim()).toBe(before);
      await expect(page.locator('[data-testid="developer-roles-edit-notice"]')).toHaveCount(0);
    }
    assertClean(consoleErrors, pageErrors, { allowHttpStatus: true });
  });
});
