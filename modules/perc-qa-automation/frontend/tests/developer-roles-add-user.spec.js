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
 * Developer Roles — add one existing user (#5309 / #1690).
 *
 * The user appears in the member list only after a successful save.
 * A blank name is not sent. An unknown user is HTTP 400 and does not
 * change the list. HTTP 403 and 409 leave the previous list. Cancel
 * does not write. Description save still does not send users.
 *
 *   npm run test:surface -- --path tests/developer-roles-add-user.spec.js
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

function assertClean(consoleErrors, pageErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpected = consoleErrors.filter((text) => {
    if (
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

function isAddUser(request) {
  return isRolePut(request) && /[?&]addUser=true(?:&|$)/.test(request.url());
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

function roleBody(payload) {
  if (!payload || typeof payload !== "object") return {};
  return payload.Role || payload.role || payload;
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
    .fill("Add one user from Developer Roles");
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

async function openRole(page, roleName) {
  const readDone = page.waitForResponse(
    (response) => isRoleMemberGet(response.request(), roleName) && response.status() === 200,
  );
  await page.locator(`[data-role-name="${roleName}"]`).first().click();
  await readDone;
  await expect(page.locator('[data-testid="developer-roles-edit-name"]')).toHaveValue(roleName);
}

test.describe("Developer add one existing user to a role (#5309)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("adds one user only after success and keeps the list on cancel, 400, 403, and 409", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const addUrls = [];
    page.on("request", (request) => {
      if (isAddUser(request)) {
        addUrls.push(request.url());
      }
    });

    await openRoles(page);
    const roleName = `NdAdd${Date.now()}`;
    await createRole(page, roleName);
    await openRole(page, roleName);
    await expect(page.locator('[data-testid="developer-roles-members-empty"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-member"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-roles-members"] button')).toHaveCount(0);

    const userInput = page.locator('[data-testid="developer-roles-add-user-name"]');
    const addButton = page.locator('[data-testid="developer-roles-add-user-save"]');
    await userInput.fill("   ");
    await expect(addButton).toBeDisabled();
    expect(addUrls, "blank name must not add a user").toEqual([]);

    await userInput.fill("NoSuchNightUser");
    await expect(addButton).toBeEnabled();
    const unknownDone = page.waitForResponse((response) => isAddUser(response.request()));
    await addButton.click();
    const unknown = await unknownDone;
    expect(unknown.status(), await unknown.text()).toBe(400);
    const unknownBody = roleBody(unknown.request().postDataJSON());
    expect(unknownBody.users).toEqual(["NoSuchNightUser"]);
    expect(unknownBody.description).toBeUndefined();
    await expect(page.locator('[data-testid="developer-roles-add-user-error"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-member"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-roles-members-empty"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-add-user-notice"]')).toHaveCount(0);

    await expect(page.locator('[data-testid="developer-roles-member"]')).toHaveCount(0);
    const addedDone = page.waitForResponse((response) => isAddUser(response.request()));
    await userInput.fill("Admin");
    await addButton.click();
    const added = await addedDone;
    expect(added.status(), await added.text()).toBe(200);
    const addedBody = roleBody(added.request().postDataJSON());
    expect(addedBody.name).toBe(roleName);
    expect(addedBody.users).toEqual(["Admin"]);
    expect(addedBody.description).toBeUndefined();
    expect(addedBody.homePage).toBeUndefined();
    await expect(
      page.locator('[data-testid="developer-roles-member"][data-user-name="Admin"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-member"]')).toHaveCount(1);
    await expect(page.locator('[data-testid="developer-roles-add-user-notice"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-members-empty"]')).toHaveCount(0);

    const beforeForced = addUrls.length;
    let forced = 0;
    const statuses = [403, 409];
    await page.route("**/services/roles/**", async (route) => {
      const request = route.request();
      if (isAddUser(request) && forced < statuses.length) {
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
    for (const status of statuses) {
      await userInput.fill("Editor");
      const forcedDone = page.waitForResponse((response) => isAddUser(response.request()));
      await addButton.click();
      const forcedResponse = await forcedDone;
      expect(forcedResponse.status()).toBe(status);
      await expect(page.locator('[data-testid="developer-roles-add-user-error"]')).toContainText(
        `forced ${status}`,
      );
      await expect(
        page.locator('[data-testid="developer-roles-member"][data-user-name="Admin"]'),
      ).toBeVisible();
      await expect(page.locator('[data-testid="developer-roles-member"]')).toHaveCount(1);
      await expect(page.locator('[data-testid="developer-roles-members"]')).not.toContainText(
        "InventedUser",
      );
      await expect(page.locator('[data-testid="developer-roles-members"]')).not.toContainText(
        "Editor",
      );
    }
    expect(addUrls.length).toBe(beforeForced + statuses.length);

    const addsBeforeCancel = addUrls.length;
    await userInput.fill("Admin");
    await page.locator('[data-testid="developer-roles-edit-cancel"]').click();
    await expect(page.locator('[data-testid="developer-roles-edit-form"]')).toHaveCount(0);
    expect(addUrls.length).toBe(addsBeforeCancel);

    await openRole(page, roleName);
    await expect(
      page.locator('[data-testid="developer-roles-member"][data-user-name="Admin"]'),
    ).toBeVisible();
    const description = page.locator('[data-testid="developer-roles-edit-description"]');
    const original = await description.inputValue();
    const next = `${original} kept`;
    await description.fill(next);
    const described = page.waitForResponse((response) => isDescriptionUpdate(response.request()));
    await page.locator('[data-testid="developer-roles-edit-save"]').click();
    const saved = await described;
    expect(saved.status(), await saved.text()).toBe(200);
    const sent = roleBody(saved.request().postDataJSON());
    expect(sent.description).toBe(next);
    expect(sent.users).toBeUndefined();
    await openRole(page, roleName);
    await expect(
      page.locator('[data-testid="developer-roles-member"][data-user-name="Admin"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-member"]')).toHaveCount(1);

    assertClean(consoleErrors, pageErrors);
  });
});
