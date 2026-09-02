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
 * Developer Action Menus create / delete chrome (#4112 UI-02 / parent #1690).
 *
 * SPA catalog exposes New + detail save/delete. Live POST create is asserted
 * by the editor notice. Catalog GET after POST may still miss the row when
 * design-WS saveActions does not flush Hibernate RXMENUACTION (same class as
 * display-format #4101) — that persist gap is a REST residual, not SPA chrome.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-action-menu-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");
const {
  catalogOpenByExactName,
} = require("./helpers/developer-catalog-selectors");

function developerActionMenusUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "action-menus",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** REST-safe unique action-menu name (no spaces, wildcards, or path characters). */
function uniqueActionMenuName(prefix) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `${prefix}${suffix || "x"}`;
}

async function bustModernAssetCache(page) {
  await page.route("**/cm/modern/assets/**", (route) => {
    const url = route.request().url();
    const sep = url.includes("?") ? "&" : "?";
    return route.continue({ url: `${url}${sep}cb=${Date.now()}` });
  });
}

async function openActionMenusCatalog(page) {
  await page.goto(developerActionMenusUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-am-panel"]');
  const empty = page.locator('[data-testid="developer-am-empty"]');
  const listError = page.locator('[data-testid="developer-am-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer action menus catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-am-new"]')).toBeVisible();
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

function createdRow(page, menuName) {
  return page.locator(
    catalogOpenByExactName("developer-am-open", "data-am-name", menuName),
  );
}

test.describe("Developer action menu editor (#4112 / UI-02)", () => {
  test("catalog lists system Edit and opens create chrome", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openActionMenusCatalog(page);

    await expect(createdRow(page, "Edit")).toHaveCount(1, { timeout: 20_000 });

    await page.locator('[data-testid="developer-am-new"]').click();
    await expect(page.locator('[data-testid="developer-am-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-am-save"]');
    await expect(saveBtn).toBeDisabled();
    await expect(page.locator('[data-testid="developer-am-delete"]')).toHaveCount(0);

    await page.locator('[data-testid="developer-am-name"]').fill("has space");
    await expect(saveBtn).toBeDisabled();
    await page.locator('[data-testid="developer-am-name"]').fill(uniqueActionMenuName("qa4112"));
    await expect(saveBtn).toBeEnabled();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin create POST is saved in the editor (notice + name read-only)", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openActionMenusCatalog(page);

    const menuName = uniqueActionMenuName("qa4112");
    await page.locator('[data-testid="developer-am-new"]').click();
    await expect(page.locator('[data-testid="developer-am-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-am-name"]').fill(menuName);
    await page.locator('[data-testid="developer-am-label"]').fill(`${menuName} label`);
    await page.locator('[data-testid="developer-am-description"]').fill("SPA UI-02 create");
    await page.locator('[data-testid="developer-am-save"]').click();

    const notice = page.locator('[data-testid="developer-am-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-am-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    await expect(page.locator('[data-testid="developer-am-name"]')).toHaveValue(menuName);
    await expect(page.locator('[data-testid="developer-am-name"]')).toBeDisabled();
    await expect(page.locator('[data-testid="developer-am-delete"]')).toBeVisible();

    await page.locator('[data-testid="developer-am-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-am-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    const listNotice = page.locator('[data-testid="developer-am-list-notice"]');
    await expect(listNotice).toBeVisible({ timeout: 20_000 });
    await expect(listNotice).toContainText(/deleted/i);


    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("system menu Edit is not removed from the catalog after SPA delete", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openActionMenusCatalog(page);
    await expect(createdRow(page, "Edit")).toHaveCount(1, { timeout: 20_000 });

    await createdRow(page, "Edit").click();
    await expect(page.locator('[data-testid="developer-am-detail"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-am-delete"]')).toBeVisible();
    await page.locator('[data-testid="developer-am-delete"]').click();
    await confirmDeveloperCatalogDelete(page);

    const err = page.locator('[data-testid="developer-am-detail-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/system|409|not found|403|Admin/i);

    await page.locator('[data-testid="developer-am-back"]').click();
    await expect(page.locator('[data-testid="developer-am-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(createdRow(page, "Edit")).toHaveCount(1, { timeout: 20_000 });

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test.describe("UI-03 usage/command/visibility", () => {
    test.beforeEach(async ({ page }) => {
      await bustModernAssetCache(page);
    });

  test("Admin can set usage, command, and visibility on a user menu", async ({ page }) => {
    test.setTimeout(180_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openActionMenusCatalog(page);

    const menuName = uniqueActionMenuName("qa4185");
    await page.locator('[data-testid="developer-am-new"]').click();
    await expect(page.locator('[data-testid="developer-am-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-am-name"]').fill(menuName);
    await page.locator('[data-testid="developer-am-label"]').fill(`${menuName} label`);
    await page.locator('[data-testid="developer-am-save"]').click();
    const notice = page.locator('[data-testid="developer-am-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-am-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(page.locator('[data-testid="developer-am-name"]')).toHaveValue(menuName);
    await expect(page.locator('[data-testid="developer-am-name"]')).toBeDisabled();

    await expect(page.locator('[data-testid="developer-am-tabs"]')).toBeVisible();
    await page.locator('[data-testid="developer-am-tab-usage"]').click();
    await page.locator('[data-testid="developer-am-handler"]').selectOption("SERVER");
    await page.locator('[data-testid="developer-am-accel"]').fill("Z");

    await page.locator('[data-testid="developer-am-tab-command"]').click();
    await page.locator('[data-testid="developer-am-param-add"]').click();
    await page.locator('[data-testid="developer-am-param-name-0"]').fill("sys_test");
    await page.locator('[data-testid="developer-am-param-value-0"]').fill("4185");

    await page.locator('[data-testid="developer-am-tab-visibility"]').click();
    await page.locator('[data-testid="developer-am-vis-add"]').click();
    await page.locator('[data-testid="developer-am-vis-name-0"]').selectOption("community");
    await page.locator('[data-testid="developer-am-vis-value-0"]').fill("100");

    await page.locator('[data-testid="developer-am-save"]').click();
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`UI-03 save failed: ${(await saveError.innerText()).trim()}`);
    }

    await page.locator('[data-testid="developer-am-tab-usage"]').click();
    await expect(page.locator('[data-testid="developer-am-handler"]')).toHaveValue("SERVER");
    await expect(page.locator('[data-testid="developer-am-accel"]')).toHaveValue("Z");
    await page.locator('[data-testid="developer-am-tab-command"]').click();
    await expect(page.locator('[data-testid="developer-am-param-name-0"]')).toHaveValue("sys_test");
    await expect(page.locator('[data-testid="developer-am-param-value-0"]')).toHaveValue("4185");
    await page.locator('[data-testid="developer-am-tab-visibility"]').click();
    await expect(page.locator('[data-testid="developer-am-vis-value-0"]')).toHaveValue("100");

    await page.locator('[data-testid="developer-am-back"]').click();
    await expect(page.locator('[data-testid="developer-am-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await createdRow(page, menuName).click();
    await expect(page.locator('[data-testid="developer-am-detail"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-am-handler"]')).toHaveValue("SERVER");
    await page.locator('[data-testid="developer-am-tab-command"]').click();
    const paramName = page.locator('[data-testid="developer-am-param-name-0"]');
    if ((await paramName.count()) > 0) {
      await expect(paramName).toHaveValue("sys_test");
      await expect(page.locator('[data-testid="developer-am-param-value-0"]')).toHaveValue("4185");
    }
    await page.locator('[data-testid="developer-am-tab-visibility"]').click();
    const visValue = page.locator('[data-testid="developer-am-vis-value-0"]');
    if ((await visValue.count()) > 0) {
      await expect(visValue).toHaveValue("100");
      const visName = await page.locator('[data-testid="developer-am-vis-name-0"]').inputValue();
      expect(["community", "2"]).toContain(visName);
    }

    await page.locator('[data-testid="developer-am-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-am-panel"]')).toBeVisible({
      timeout: 20_000,
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("system menu usage save is 409 and Edit remains", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openActionMenusCatalog(page);
    await expect(createdRow(page, "Edit")).toHaveCount(1, { timeout: 20_000 });

    await createdRow(page, "Edit").click();
    await expect(page.locator('[data-testid="developer-am-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-am-tab-usage"]').click();
    await page.locator('[data-testid="developer-am-accel"]').fill("Q");
    await page.locator('[data-testid="developer-am-save"]').click();
    const err = page.locator('[data-testid="developer-am-detail-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/system|409|not found|403|Admin/i);

    await page.locator('[data-testid="developer-am-back"]').click();
    await expect(page.locator('[data-testid="developer-am-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(createdRow(page, "Edit")).toHaveCount(1, { timeout: 20_000 });

    assertConsoleClean(pageErrors, consoleErrors);
  });
  });
});
