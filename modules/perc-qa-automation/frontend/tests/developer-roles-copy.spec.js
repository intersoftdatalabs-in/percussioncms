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
 * Developer Roles — copy one role (#5334 / #1690).
 *
 * Confirm creates one role under the new name with the same description,
 * home page, and users. The source role stays. Blank and duplicate names
 * do not create. Cancel does not write. HTTP 400, 403, and 409 do not
 * claim the copy finished. A later-step failure stays visible.
 *
 *   npm run test:surface -- --path tests/developer-roles-copy.spec.js
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

async function createRole(page, roleName, description) {
  await page.locator('[data-testid="developer-roles-create"]').click();
  await page.locator('[data-testid="developer-roles-create-name"]').fill(roleName);
  await page.locator('[data-testid="developer-roles-create-description"]').fill(description);
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

async function setHomePage(page, roleName, homePage) {
  const readDone = page.waitForResponse(
    (response) => isRoleMemberGet(response.request(), roleName) && response.status() === 200,
  );
  await page.locator(`[data-role-name="${roleName}"]`).first().click();
  await readDone;
  await page.locator('[data-testid="developer-roles-edit-homepage"]').fill(homePage);
  const putDone = page.waitForResponse(
    (response) => isRolePut(response.request()) && response.url().includes("homePage=true"),
  );
  await page.locator('[data-testid="developer-roles-homepage-save"]').click();
  const response = await putDone;
  expect(response.status(), await response.text()).toBe(200);
  await expect(page.locator(`[data-role-homepage="${roleName}"]`).first()).toHaveText(homePage, {
    timeout: 30_000,
  });
}

async function addUser(page, roleName, userName) {
  const readDone = page.waitForResponse(
    (response) => isRoleMemberGet(response.request(), roleName) && response.status() === 200,
  );
  await page.locator(`[data-role-name="${roleName}"]`).first().click();
  await readDone;
  await page.locator('[data-testid="developer-roles-add-user-name"]').fill(userName);
  const putDone = page.waitForResponse(
    (response) => isRolePut(response.request()) && response.url().includes("addUser=true"),
  );
  await page.locator('[data-testid="developer-roles-add-user-save"]').click();
  const response = await putDone;
  expect(response.status(), await response.text()).toBe(200);
  await expect(
    page.locator(`[data-testid="developer-roles-member"][data-user-name="${userName}"]`),
  ).toBeVisible();
  await page.locator('[data-testid="developer-roles-edit-cancel"]').click();
  await expect(page.locator('[data-testid="developer-roles-edit-form"]')).toHaveCount(0);
}

async function openCopy(page, roleName) {
  const readDone = page.waitForResponse(
    (response) => isRoleMemberGet(response.request(), roleName) && response.status() === 200,
  );
  await page
    .locator(`[data-testid="developer-roles-copy"][data-role-name="${roleName}"]`)
    .first()
    .click();
  await readDone;
  await expect(page.locator('[data-testid="developer-roles-copy-source"]')).toHaveValue(roleName);
}

test.describe("Developer copy one role (#5334)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("copies description, home page, and users only after success", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const puts = [];
    page.on("request", (request) => {
      if (isRolePut(request)) {
        puts.push(request);
      }
    });

    await openRoles(page);
    const stamp = Date.now();
    const sourceName = `NdCopySrc${stamp}`;
    const description = "Copy source description";
    await createRole(page, sourceName, description);
    await setHomePage(page, sourceName, "Developer");
    await addUser(page, sourceName, "Admin");
    const putsBeforeCopy = puts.length;

    await openCopy(page, sourceName);
    await expect(page.locator('[data-testid="developer-roles-copy-description"]')).toHaveText(
      description,
    );
    await expect(page.locator('[data-testid="developer-roles-copy-homepage"]')).toHaveText(
      "Developer",
    );
    await expect(
      page.locator('[data-testid="developer-roles-copy-user"][data-user-name="Admin"]'),
    ).toBeVisible();

    const save = page.locator('[data-testid="developer-roles-copy-save"]');
    await page.locator('[data-testid="developer-roles-copy-name"]').fill("   ");
    await expect(save).toBeDisabled();
    expect(puts.length, "blank name must not copy").toBe(putsBeforeCopy);

    await page.locator('[data-testid="developer-roles-copy-name"]').fill(sourceName);
    await expect(save).toBeEnabled();
    await save.click();
    await expect(page.locator('[data-testid="developer-roles-copy-error"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-copy-notice"]')).toHaveCount(0);
    expect(puts.length, "duplicate name must not copy").toBe(putsBeforeCopy);
    await expect(page.locator(`[data-role-name="${sourceName}"]`).first()).toBeVisible();

    await page.locator('[data-testid="developer-roles-copy-cancel"]').click();
    await expect(page.locator('[data-testid="developer-roles-copy-form"]')).toHaveCount(0);
    expect(puts.length, "cancel must not copy").toBe(putsBeforeCopy);

    const newName = `NdCopyNew${stamp}`;
    await openCopy(page, sourceName);
    await page.locator('[data-testid="developer-roles-copy-name"]').fill(newName);
    const copyStarted = puts.length;
    await save.click();
    await expect(page.locator('[data-testid="developer-roles-copy-notice"]')).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.locator(`[data-role-name="${newName}"]`).first()).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.locator(`[data-role-description="${newName}"]`).first()).toHaveText(
      description,
    );
    await expect(page.locator(`[data-role-homepage="${newName}"]`).first()).toHaveText(
      "Developer",
    );
    await expect(page.locator(`[data-role-description="${sourceName}"]`).first()).toHaveText(
      description,
    );
    await expect(page.locator(`[data-role-homepage="${sourceName}"]`).first()).toHaveText(
      "Developer",
    );

    const copyPuts = puts.slice(copyStarted).map((request) => ({
      url: request.url(),
      body: roleBody(request.postDataJSON()),
    }));
    expect(copyPuts.length).toBeGreaterThanOrEqual(4);
    expect(copyPuts[0].url).toContain("create=true");
    expect(copyPuts[0].body).toEqual({ name: newName });
    expect(copyPuts[1].url).toContain("update=true");
    expect(copyPuts[1].body).toMatchObject({ name: newName, description });
    expect(copyPuts[1].body.users).toBeUndefined();
    expect(copyPuts[2].url).toContain("homePage=true");
    expect(copyPuts[2].body).toMatchObject({ name: newName, homePage: "Developer" });
    expect(copyPuts[3].url).toContain("addUser=true");
    expect(copyPuts[3].body).toEqual({ name: newName, users: ["Admin"] });
    expect(copyPuts.every((call) => call.body.name !== sourceName)).toBe(true);

    const newRead = page.waitForResponse(
      (response) => isRoleMemberGet(response.request(), newName) && response.status() === 200,
    );
    await page.locator(`[data-role-name="${newName}"]`).first().click();
    const newRole = await newRead;
    expect(await newRole.text()).toContain("Admin");
    await expect(
      page.locator('[data-testid="developer-roles-member"][data-user-name="Admin"]'),
    ).toBeVisible();
    await page.locator('[data-testid="developer-roles-edit-cancel"]').click();

    const sourceRead = page.waitForResponse(
      (response) => isRoleMemberGet(response.request(), sourceName) && response.status() === 200,
    );
    await page.locator(`[data-role-name="${sourceName}"]`).first().click();
    const sourceRole = await sourceRead;
    const sourceText = await sourceRole.text();
    expect(sourceText).toContain(description);
    expect(sourceText).toContain("Developer");
    expect(sourceText).toContain("Admin");
    await expect(
      page.locator('[data-testid="developer-roles-member"][data-user-name="Admin"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-edit-description"]')).toHaveValue(
      description,
    );
    await expect(page.locator('[data-testid="developer-roles-edit-homepage"]')).toHaveValue(
      "Developer",
    );
    assertClean(consoleErrors, pageErrors);
  });

  test("does not claim success on HTTP 403 create or a later 400 or 409", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openRoles(page);
    const stamp = Date.now();
    const sourceName = `NdCopyErr${stamp}`;
    const description = "Copy error source";
    await createRole(page, sourceName, description);
    await setHomePage(page, sourceName, "Developer");
    await addUser(page, sourceName, "Admin");

    const forbidName = `NdCopyForbid${stamp}`;
    const partialName = `NdCopyPartial${stamp}`;
    const conflictName = `NdCopyConflict${stamp}`;
    await page.route("**/services/roles/**", async (route) => {
      const request = route.request();
      if (request.method() !== "PUT") {
        await route.continue();
        return;
      }
      const url = request.url();
      const body = roleBody(request.postDataJSON());
      if (body.name === forbidName && url.includes("create=true")) {
        await route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({ message: "forbidden" }),
        });
        return;
      }
      if (body.name === partialName && url.includes("update=true")) {
        await route.fulfill({
          status: 400,
          contentType: "application/json",
          body: JSON.stringify({ message: "rejected" }),
        });
        return;
      }
      if (body.name === conflictName && url.includes("addUser=true")) {
        await route.fulfill({
          status: 409,
          contentType: "application/json",
          body: JSON.stringify({ message: "conflict" }),
        });
        return;
      }
      await route.continue();
    });

    await openCopy(page, sourceName);
    await page.locator('[data-testid="developer-roles-copy-name"]').fill(forbidName);
    const forbidDone = page.waitForResponse(
      (response) =>
        isRolePut(response.request()) &&
        response.url().includes("create=true") &&
        response.status() === 403,
    );
    await page.locator('[data-testid="developer-roles-copy-save"]').click();
    await forbidDone;
    await expect(page.locator('[data-testid="developer-roles-copy-error"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-roles-copy-notice"]')).toHaveCount(0);
    await expect(page.locator(`[data-role-name="${forbidName}"]`)).toHaveCount(0);

    await page.locator('[data-testid="developer-roles-copy-name"]').fill(partialName);
    const partialDone = page.waitForResponse(
      (response) =>
        isRolePut(response.request()) &&
        response.url().includes("update=true") &&
        response.status() === 400,
    );
    await page.locator('[data-testid="developer-roles-copy-save"]').click();
    await partialDone;
    await expect(page.locator('[data-testid="developer-roles-copy-error"]')).toContainText(
      "did not finish",
    );
    await expect(page.locator('[data-testid="developer-roles-copy-notice"]')).toHaveCount(0);
    await expect(page.locator(`[data-role-name="${partialName}"]`)).toHaveCount(0);

    await page.locator('[data-testid="developer-roles-copy-name"]').fill(conflictName);
    const conflictDone = page.waitForResponse(
      (response) =>
        isRolePut(response.request()) &&
        response.url().includes("addUser=true") &&
        response.status() === 409,
    );
    await page.locator('[data-testid="developer-roles-copy-save"]').click();
    await conflictDone;
    await expect(page.locator('[data-testid="developer-roles-copy-error"]')).toContainText(
      "did not finish",
    );
    await expect(page.locator('[data-testid="developer-roles-copy-notice"]')).toHaveCount(0);
    await expect(page.locator(`[data-role-name="${conflictName}"]`)).toHaveCount(0);
    await expect(page.locator(`[data-role-description="${sourceName}"]`).first()).toHaveText(
      description,
    );
    await expect(page.locator(`[data-role-homepage="${sourceName}"]`).first()).toHaveText(
      "Developer",
    );
    assertClean(consoleErrors, pageErrors);
  });
});
