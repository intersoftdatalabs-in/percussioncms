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

async function roleNames(page) {
  return page.locator('[data-testid^="developer-roles-row-"]').evaluateAll((rows) => {
    const seen = new Set();
    const names = [];
    for (const row of rows) {
      const name = row.getAttribute("data-role-name");
      if (!name || seen.has(name)) continue;
      seen.add(name);
      names.push(name);
    }
    return names;
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

    await page.locator('[data-testid="developer-roles-edit-cancel"]').click();
    const names = await roleNames(page);
    let emptyRole = null;
    for (const name of names) {
      if (name === "Admin") continue;
      const users = await openRole(page, name);
      if (users.length === 0) {
        emptyRole = name;
        break;
      }
      await page.locator('[data-testid="developer-roles-edit-cancel"]').click();
    }
    expect(emptyRole, `no empty role in catalog: ${names.join(", ")}`).toBeTruthy();
    await expect(page.locator('[data-testid="developer-roles-members-empty"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-member"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-roles-members-error"]')).toHaveCount(0);

    const description = page.locator('[data-testid="developer-roles-edit-description"]');
    const original = await description.inputValue();
    const next = original === "Night role note" ? "Night role note 2" : "Night role note";
    await description.fill(next);
    const putDone = page.waitForResponse((response) =>
      isDescriptionUpdate(response.request()),
    );
    await page.locator('[data-testid="developer-roles-edit-save"]').click();
    const saved = await putDone;
    expect(saved.status(), await saved.text()).toBe(200);
    const sent = saved.request().postDataJSON();
    const body = sent && (sent.Role || sent);
    expect(body.description).toBe(next);
    expect(body.users).toBeUndefined();
    await expect(page.locator('[data-testid="developer-roles-edit-notice"]')).toBeVisible();

    const afterSave = await openRole(page, emptyRole);
    expect(afterSave).toEqual([]);
    await expect(page.locator('[data-testid="developer-roles-members-empty"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-member"]')).toHaveCount(0);

    await page.locator('[data-testid="developer-roles-edit-description"]').fill(original);
    const restoreDone = page.waitForResponse((response) =>
      isDescriptionUpdate(response.request()),
    );
    await page.locator('[data-testid="developer-roles-edit-save"]').click();
    const restored = await restoreDone;
    expect(restored.status(), await restored.text()).toBe(200);
    const restoredBody = restored.request().postDataJSON();
    const restoredRole = restoredBody && (restoredBody.Role || restoredBody);
    expect(restoredRole.users).toBeUndefined();
    assertClean(consoleErrors, pageErrors);
  });

  test("HTTP 403 and 404 do not invent members", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openRoles(page);
    const names = await roleNames(page);
    const roleName = names.find((name) => name !== "Admin") || names[0];
    expect(roleName).toBeTruthy();
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
