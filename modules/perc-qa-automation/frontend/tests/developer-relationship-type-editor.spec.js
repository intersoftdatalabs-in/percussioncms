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
 * Developer Relationship Types write chrome — H2 Playwright SY-03 (#4253 / parent #1690).
 * Stacks on REST #4251 / PR #4254 and SPA #4252 / PR #4258.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-relationship-type-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

function developerRelationshipTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "relationship-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** Unique name: no whitespace / wildcards (REST requireValidName). */
function uniqueRtName(prefix = "QaRt") {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/gi, "").slice(-5);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/gi, "").slice(2, 6);
  return `${prefix}${a}${b}`.slice(0, 24);
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {string} name
 */
function rtOpen(page, name) {
  const escaped = name.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return page
    .locator('[data-testid="developer-rt-open"]')
    .filter({ hasText: new RegExp(`^${escaped}$`) });
}

async function openRtCatalog(page) {
  await page.goto(developerRelationshipTypesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-rt-panel"]');
  const empty = page.locator('[data-testid="developer-rt-empty"]');
  const listError = page.locator('[data-testid="developer-rt-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer relationship types catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-rt-new"]')).toBeVisible();
}

function attachConsoleGuards(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      consoleErrors.push(msg.text());
    }
  });
  return { pageErrors, consoleErrors };
}

function assertConsoleClean(pageErrors, consoleErrors) {
  expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
  const unexpectedConsole = consoleErrors.filter(
    (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
  );
  expect(
    unexpectedConsole,
    `console error: ${unexpectedConsole.join(" | ")}`,
  ).toEqual([]);
}

/**
 * @param {import("@playwright/test").Page} page
 * @param {{ name: string, label: string, category?: string, allowCloning?: boolean, copyFrom?: string }} opts
 */
async function createUserRelationshipType(page, opts) {
  await page.locator('[data-testid="developer-rt-new"]').click();
  await expect(page.locator('[data-testid="developer-rt-detail"]')).toBeVisible();
  const saveBtn = page.locator('[data-testid="developer-rt-save"]');
  await expect(saveBtn).toBeDisabled();

  await page.locator('[data-testid="developer-rt-name"]').fill(opts.name);
  await page.locator('[data-testid="developer-rt-label"]').fill(opts.label);

  if (opts.copyFrom) {
    await page.locator('[data-testid="developer-rt-copy-from"]').selectOption(opts.copyFrom);
  } else {
    await page
      .locator('[data-testid="developer-rt-category"]')
      .selectOption(opts.category || "rs_generic");
    if (opts.allowCloning === true) {
      await page.locator('[data-testid="developer-rt-allow-cloning"]').check();
    } else if (opts.allowCloning === false) {
      await page.locator('[data-testid="developer-rt-allow-cloning"]').uncheck();
    }
  }

  await expect(saveBtn).toBeEnabled();
  await saveBtn.click();

  const notice = page.locator('[data-testid="developer-rt-editor-notice"]');
  const saveError = page.locator('[data-testid="developer-rt-detail-error"]');
  await expect(notice.or(saveError).first()).toBeVisible({ timeout: 30_000 });
  if (await saveError.isVisible()) {
    throw new Error(
      `Create relationship type failed: ${(await saveError.innerText()).trim()}`,
    );
  }
  await expect(notice).toContainText(/saved/i);
}

async function deleteCurrentUserType(page) {
  await expect(page.locator('[data-testid="developer-rt-delete"]')).toBeVisible();
  await page.locator('[data-testid="developer-rt-delete"]').click();
  await confirmDeveloperCatalogDelete(page);
  await expect(page.locator('[data-testid="developer-rt-panel"]')).toBeVisible({
    timeout: 20_000,
  });
}

test.describe("Developer relationship type editor (#4253 / SY-03 H2)", () => {
  test("system packaged types stay immutable (no Save/Delete)", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openRtCatalog(page);

    const systemOpen = rtOpen(page, "ActiveAssembly");
    await expect(systemOpen).toBeVisible({ timeout: 20_000 });
    await systemOpen.click();
    await expect(page.locator('[data-testid="developer-rt-detail"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-rt-system-readonly"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-rt-save"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-rt-delete"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-rt-name"]')).toBeDisabled();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin create, edit cloning flags, and delete a user type", async ({ page }) => {
    test.setTimeout(150_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openRtCatalog(page);

    const name = uniqueRtName("QaRt");
    const label = `QA 4253 ${name}`;
    const savedLabel = `${label} saved`;

    await createUserRelationshipType(page, {
      name,
      label,
      category: "rs_generic",
      allowCloning: true,
    });

    await expect(page.locator('[data-testid="developer-rt-allow-cloning"]')).toBeChecked();
    await page.locator('[data-testid="developer-rt-label"]').fill(savedLabel);
    await page.locator('[data-testid="developer-rt-allow-cloning"]').uncheck();
    await page.locator('[data-testid="developer-rt-owner-revision"]').check();
    await page.locator('[data-testid="developer-rt-save"]').click();

    const notice = page.locator('[data-testid="developer-rt-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-rt-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Save failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(page.locator('[data-testid="developer-rt-allow-cloning"]')).not.toBeChecked();
    await expect(page.locator('[data-testid="developer-rt-owner-revision"]')).toBeChecked();

    await page.locator('[data-testid="developer-rt-back"]').click();
    await expect(page.locator('[data-testid="developer-rt-table"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(rtOpen(page, name)).toBeVisible();

    await rtOpen(page, name).click();
    await expect(page.locator('[data-testid="developer-rt-label"]')).toHaveValue(savedLabel);
    await deleteCurrentUserType(page);
    await expect(rtOpen(page, name)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin can create a user type by copying a system type", async ({ page }) => {
    test.setTimeout(150_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openRtCatalog(page);

    const name = uniqueRtName("QaCp");
    const label = `QA copy ${name}`;

    await createUserRelationshipType(page, {
      name,
      label,
      copyFrom: "ActiveAssembly",
    });

    await page.locator('[data-testid="developer-rt-back"]').click();
    await expect(rtOpen(page, name)).toBeVisible({ timeout: 20_000 });
    await rtOpen(page, name).click();
    await expect(page.locator('[data-testid="developer-rt-delete"]')).toBeVisible();
    await deleteCurrentUserType(page);
    await expect(rtOpen(page, name)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("duplicate user type name surfaces a 409 conflict in the UI", async ({ page }) => {
    test.setTimeout(150_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openRtCatalog(page);

    const name = uniqueRtName("QaDup");
    const label = `QA dup ${name}`;

    await createUserRelationshipType(page, {
      name,
      label,
      category: "rs_generic",
    });
    await page.locator('[data-testid="developer-rt-back"]').click();
    await expect(rtOpen(page, name)).toBeVisible({ timeout: 20_000 });

    await page.locator('[data-testid="developer-rt-new"]').click();
    await expect(page.locator('[data-testid="developer-rt-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-rt-name"]').fill(name);
    await page.locator('[data-testid="developer-rt-category"]').selectOption("rs_generic");
    await page.locator('[data-testid="developer-rt-label"]').fill(`${label} again`);
    await page.locator('[data-testid="developer-rt-save"]').click();

    const err = page.locator('[data-testid="developer-rt-detail-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/already exists|409|duplicate|conflict/i);

    await page.locator('[data-testid="developer-rt-cancel"]').click();
    await expect(page.locator('[data-testid="developer-rt-panel"]')).toBeVisible();
    await rtOpen(page, name).click();
    await deleteCurrentUserType(page);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("invalid name keeps Save disabled and shows validation chrome", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openRtCatalog(page);

    await page.locator('[data-testid="developer-rt-new"]').click();
    await expect(page.locator('[data-testid="developer-rt-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-rt-save"]');

    await page.locator('[data-testid="developer-rt-name"]').fill("has space");
    await page.locator('[data-testid="developer-rt-category"]').selectOption("rs_generic");
    await page.locator('[data-testid="developer-rt-label"]').fill("Invalid name");
    await expect(saveBtn).toBeDisabled();
    await expect(page.locator('[data-testid="developer-rt-name-invalid"]')).toBeVisible();

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
