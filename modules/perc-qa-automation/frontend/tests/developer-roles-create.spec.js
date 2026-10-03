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
 * Developer Roles create (#5101 / #1690).
 *
 * Admin creates one role (name + description). The catalog row appears only
 * after PUT ?create=true succeeds. Cancel and a blank name do not call the
 * server. HTTP 400 and 403 do not claim the role exists.
 *
 *   npm run test:surface -- --path tests/developer-roles-create.spec.js
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
      /Failed to load resource: the server responded with a status of (400|403)/.test(
        text,
      )
    ) {
      return false;
    }
    return true;
  });
  expect(unexpected, `console error: ${unexpected.join(" | ")}`).toEqual([]);
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

function isRolePut(request) {
  return (
    request.method() === "PUT" &&
    /\/services\/roles\/?(?:\?|$)/.test(request.url()) &&
    !/\/services\/roles\/catalog/.test(request.url())
  );
}

test.describe("Developer create a role (#5101)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("blank name and cancel do not call the server", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const puts = [];
    page.on("request", (request) => {
      if (isRolePut(request)) {
        puts.push(request.url());
      }
    });

    await openRoles(page);
    await page.locator('[data-testid="developer-roles-create"]').click();
    const save = page.locator('[data-testid="developer-roles-create-save"]');
    await expect(save).toBeDisabled();
    await page.locator('[data-testid="developer-roles-create-name"]').fill("   ");
    await expect(save).toBeDisabled();

    await page.locator('[data-testid="developer-roles-create-name"]').fill("NightRoleCancel");
    await page.locator('[data-testid="developer-roles-create-cancel"]').click();
    await expect(page.locator('[data-testid="developer-roles-create-form"]')).toHaveCount(0);
    await expect(page.locator('[data-role-name="NightRoleCancel"]')).toHaveCount(0);
    expect(puts, `unexpected role PUT: ${puts.join(" | ")}`).toEqual([]);
    assertClean(consoleErrors, pageErrors);
  });

  test("catalog lists the role only after create succeeds", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const roleName = `Nr${Date.now()}`;
    await openRoles(page);
    await expect(page.locator(`[data-role-name="${roleName}"]`)).toHaveCount(0);

    await page.locator('[data-testid="developer-roles-create"]').click();
    await page.locator('[data-testid="developer-roles-create-name"]').fill(roleName);
    await page
      .locator('[data-testid="developer-roles-create-description"]')
      .fill("Created from Developer Roles");

    const putDone = page.waitForResponse(
      (response) =>
        isRolePut(response.request()) && response.url().includes("create=true"),
    );
    await page.locator('[data-testid="developer-roles-create-save"]').click();
    const response = await putDone;
    expect(response.status(), await response.text()).toBe(200);
    await expect(page.locator(`[data-role-name="${roleName}"]`)).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.locator('[data-testid="developer-roles-create-notice"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-create-error"]')).toHaveCount(0);
    assertClean(consoleErrors, pageErrors);
  });

  test("HTTP 400 does not claim the role exists", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await page.route("**/services/roles/**", async (route) => {
      const request = route.request();
      if (isRolePut(request)) {
        await route.fulfill({
          status: 400,
          contentType: "application/json",
          body: JSON.stringify({ message: "Role name is not valid" }),
        });
        return;
      }
      await route.continue();
    });
    await openRoles(page);
    await page.locator('[data-testid="developer-roles-create"]').click();
    await page.locator('[data-testid="developer-roles-create-name"]').fill("BadRole400");
    await page.locator('[data-testid="developer-roles-create-save"]').click();
    await expect(page.locator('[data-testid="developer-roles-create-error"]')).toBeVisible();
    await expect(page.locator('[data-role-name="BadRole400"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-roles-create-notice"]')).toHaveCount(0);
    assertClean(consoleErrors, pageErrors, { allowHttpStatus: true });
  });

  test("HTTP 403 does not claim the role exists", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await page.route("**/services/roles/**", async (route) => {
      const request = route.request();
      if (isRolePut(request)) {
        await route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({ message: "Admin role required" }),
        });
        return;
      }
      await route.continue();
    });
    await openRoles(page);
    await page.locator('[data-testid="developer-roles-create"]').click();
    await page.locator('[data-testid="developer-roles-create-name"]').fill("BadRole403");
    await page.locator('[data-testid="developer-roles-create-save"]').click();
    await expect(page.locator('[data-testid="developer-roles-create-error"]')).toBeVisible();
    await expect(page.locator('[data-role-name="BadRole403"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-roles-create-notice"]')).toHaveCount(0);
    assertClean(consoleErrors, pageErrors, { allowHttpStatus: true });
  });
});
