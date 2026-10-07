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
 * Developer Roles — show the users on one role (#5308 / #1690).
 *
 * Opening a role lists the users from GET /services/roles/{name}.
 * An empty role shows an empty state. Description save does not send users.
 * HTTP 403 and 404 do not invent members.
 *
 *   npm run test:surface -- --path tests/developer-roles-users.spec.js
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

function isRoleMemberGet(request, roleName) {
  if (request.method() !== "GET") return false;
  const url = request.url();
  if (/\/services\/roles\/catalog(?:\?|$)/.test(url)) return false;
  return url.includes(`/services/roles/${encodeURIComponent(roleName)}`);
}

function usersFromRolePayload(payload) {
  const body = payload && (payload.Role || payload.role || payload);
  if (!body || typeof body !== "object") return [];
  const raw = body.users != null ? body.users : body.Users;
  if (raw == null) return [];
  if (typeof raw === "string") {
    const trimmed = raw.trim();
    return trimmed ? [trimmed] : [];
  }
  if (Array.isArray(raw)) {
    return raw
      .map((item) => (typeof item === "string" ? item.trim() : ""))
      .filter((name) => name.length > 0);
  }
  return [];
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

async function openRole(page, roleName) {
  const readDone = page.waitForResponse(
    (response) => isRoleMemberGet(response.request(), roleName) && response.status() === 200,
  );
  await page.locator(`[data-role-name="${roleName}"]`).first().click();
  const response = await readDone;
  const payload = await response.json();
  await expect(page.locator('[data-testid="developer-roles-edit-name"]')).toHaveValue(roleName);
  return usersFromRolePayload(payload);
}

test.describe("Developer show the users on one role (#5308)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test("lists stored users and an empty role stays empty", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openRoles(page);
    const adminUsers = await openRole(page, "Admin");
    expect(adminUsers.length, "Admin role GET returned no users").toBeGreaterThan(0);
    await expect(page.locator('[data-testid="developer-roles-members-empty"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-roles-members-error"]')).toHaveCount(0);
    for (const userName of adminUsers) {
      await expect(
        page.locator(`[data-testid="developer-roles-member"][data-user-name="${userName}"]`),
      ).toBeVisible();
    }
    await expect(page.locator('[data-testid="developer-roles-members"] button')).toHaveCount(0);

    const roleName = `Ru${Date.now()}`;
    await page.locator('[data-testid="developer-roles-edit-cancel"]').click();
    await createRole(page, roleName, "No members yet");
    const createdUsers = await openRole(page, roleName);
    expect(createdUsers).toEqual([]);
    await expect(page.locator('[data-testid="developer-roles-members-empty"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-member"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-roles-members-error"]')).toHaveCount(0);

    await page.locator('[data-testid="developer-roles-edit-description"]').fill("Still no members");
    const putDone = page.waitForResponse((response) =>
      isDescriptionUpdate(response.request()),
    );
    await page.locator('[data-testid="developer-roles-edit-save"]').click();
    const saved = await putDone;
    expect(saved.status(), await saved.text()).toBe(200);
    const sent = saved.request().postDataJSON();
    const body = sent && (sent.Role || sent);
    expect(body.description).toBe("Still no members");
    expect(body.users).toBeUndefined();
    await expect(page.locator('[data-testid="developer-roles-edit-notice"]')).toBeVisible();

    const afterSave = await openRole(page, roleName);
    expect(afterSave).toEqual([]);
    await expect(page.locator('[data-testid="developer-roles-members-empty"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-member"]')).toHaveCount(0);
    assertClean(consoleErrors, pageErrors);
  });

  test("HTTP 403 and 404 do not invent members", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openRoles(page);
    const roleName = `Ru${Date.now()}`;
    await createRole(page, roleName, "Error read");
    const statuses = [403, 404];
    let forced = 0;
    await page.route("**/services/roles/**", async (route) => {
      const request = route.request();
      if (isRoleMemberGet(request, roleName) && forced < statuses.length) {
        const status = statuses[forced];
        forced += 1;
        await route.fulfill({
          status,
          contentType: "application/json",
          body: JSON.stringify({
            message: `forced ${status}`,
            Role: { name: roleName, users: ["InventedUser"] },
            users: ["InventedUser"],
          }),
        });
        return;
      }
      await route.continue();
    });
    await page.locator(`[data-role-name="${roleName}"]`).first().click();
    await expect(page.locator('[data-testid="developer-roles-members-error"]')).toContainText(
      "forced 403",
    );
    await expect(page.locator('[data-testid="developer-roles-member"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-roles-members-empty"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-roles-members"]')).not.toContainText(
      "InventedUser",
    );

    await page.locator(`[data-role-name="${roleName}"]`).first().click();
    await expect(page.locator('[data-testid="developer-roles-members-error"]')).toContainText(
      "forced 404",
    );
    await expect(page.locator('[data-testid="developer-roles-member"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-roles-members-empty"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-roles-members"]')).not.toContainText(
      "InventedUser",
    );
    assertClean(consoleErrors, pageErrors, { allowHttpStatus: true });
  });
});
