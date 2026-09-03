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
 * SPA catalog New POSTs a user menu; GET catalog lists it; detail DELETE
 * omits it (following GET is 404). System menus stay in the catalog (409).
 * Stacks REST JAXB bind (#4171 / PR #4229) so POST is ActionMenu, not the
 * workflow-transitions finder DTO. Does not claim UI-03/UI-04.
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

/**
 * Same-origin fetch so OWASP CSRF + session cookies apply.
 *
 * @param {import("@playwright/test").Page} page
 * @param {string} path
 * @param {string} method
 * @returns {Promise<{status: number, text: string}>}
 */
async function inPageJson(page, path, method) {
  return page.evaluate(
    async ({ path: url, method: httpMethod }) => {
      const tokenObj = window.OWASP_CSRFTOKEN;
      const metaToken = document.querySelector('meta[name="_csrf"]');
      const metaHeader = document.querySelector('meta[name="_csrf_header"]');
      const token =
        (tokenObj && tokenObj.token) || (metaToken && metaToken.content) || "";
      const headerName =
        (metaHeader && metaHeader.content) || "OWASP-CSRFTOKEN";
      const headers = { Accept: "application/json" };
      if (token) {
        headers[headerName] = token;
      }
      const res = await fetch(url, {
        method: httpMethod,
        credentials: "same-origin",
        headers,
      });
      const text = await res.text();
      return { status: res.status, text };
    },
    { path, method },
  );
}

function nameInJson(text, name) {
  return new RegExp(`"name"\\s*:\\s*"${name}"`).test(text);
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

  test("Admin create POST is listed then DELETE omits it (GET 404)", async ({ page }) => {
    test.setTimeout(180_000);
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

    const catalog = await inPageJson(page, "/Rhythmyx/services/actions/catalog", "GET");
    expect(catalog.status, `GET catalog (got ${catalog.status}): ${catalog.text}`).toBe(200);
    expect(
      nameInJson(catalog.text, menuName),
      `GET catalog must list ${menuName}: ${catalog.text}`,
    ).toBeTruthy();

    await page.locator('[data-testid="developer-am-back"]').click();
    await expect(page.locator('[data-testid="developer-am-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(createdRow(page, menuName)).toHaveCount(1, { timeout: 20_000 });

    await createdRow(page, menuName).click();
    await expect(page.locator('[data-testid="developer-am-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-am-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-am-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    const listNotice = page.locator('[data-testid="developer-am-list-notice"]');
    await expect(listNotice).toBeVisible({ timeout: 20_000 });
    await expect(listNotice).toContainText(/deleted/i);
    await expect(createdRow(page, menuName)).toHaveCount(0);

    const afterDelete = await inPageJson(
      page,
      `/Rhythmyx/services/actions/catalog/${encodeURIComponent(menuName)}`,
      "GET",
    );
    expect(
      afterDelete.status,
      `GET after DELETE must be 404 (got ${afterDelete.status}): ${afterDelete.text}`,
    ).toBe(404);

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
});
