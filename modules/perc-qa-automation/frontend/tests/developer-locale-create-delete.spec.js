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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Developer locale catalog create + delete (#6065 / parent #1690).
 *
 * Admin creates one locale from the React catalog and deletes it with
 * confirm. The catalog row drops only after DELETE succeeds. Cancel does
 * not call the server and the row stays. Admin 403, lock/dependency 409,
 * and duplicate-language 409 surface named messages and keep the row.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-locale-create-delete.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  confirmDeveloperCatalogDelete,
  cancelDeveloperCatalogDelete,
} = require("./helpers/developer-catalog-confirm");

function developerLocalesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "locales",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** BCP-47-style unique language matching REST LANGUAGE_PATTERN. */
function uniqueLang() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `xx-${suffix || "qa6065"}`;
}

async function openLocalesCatalog(page) {
  await page.goto(developerLocalesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-loc-panel"]');
  const empty = page.locator('[data-testid="developer-loc-empty"]');
  const listError = page.locator('[data-testid="developer-loc-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer locales catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-loc-new"]')).toBeVisible();
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

function isLocaleDelete(request) {
  return (
    request.method() === "DELETE" &&
    /\/services\/locales\/[^/]+(?:\?|$)/.test(request.url())
  );
}

function isLocaleCreate(request) {
  return (
    request.method() === "POST" &&
    /\/services\/locales(?:\?|$)/.test(request.url())
  );
}

function localeRow(page, lang) {
  return page.locator(`[data-loc-lang="${lang}"]`).first();
}

async function createLocale(page, lang, label) {
  await page.locator('[data-testid="developer-loc-new"]').click();
  await expect(page.locator('[data-testid="developer-loc-detail"]')).toBeVisible();
  await page.locator('[data-testid="developer-loc-language"]').fill(lang);
  await page.locator('[data-testid="developer-loc-label"]').fill(label);
  await page.locator('[data-testid="developer-loc-save"]').click();
  const notice = page.locator('[data-testid="developer-loc-editor-notice"]');
  const saveError = page.locator('[data-testid="developer-loc-detail-error"]');
  await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
  if (await saveError.isVisible()) {
    throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
  }
}

async function backToCatalog(page, lang) {
  await page.locator('[data-testid="developer-loc-back"]').click();
  await expect(page.locator('[data-testid="developer-loc-panel"]')).toBeVisible({
    timeout: 20_000,
  });
  await expect(localeRow(page, lang)).toBeVisible({ timeout: 20_000 });
}

test.describe("Developer locale catalog create-delete (#6065)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("cancel does not delete and the row stays", async ({ page }) => {
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    const deletes = [];
    page.on("request", (req) => {
      if (isLocaleDelete(req)) {
        deletes.push(req.url());
      }
    });

    await openLocalesCatalog(page);
    const lang = uniqueLang();
    await createLocale(page, lang, `QA 6065 ${lang}`);
    await backToCatalog(page, lang);

    await localeRow(page, lang).click();
    await expect(page.locator('[data-testid="developer-loc-delete"]')).toBeVisible();
    await page.locator('[data-testid="developer-loc-delete"]').click();
    await cancelDeveloperCatalogDelete(page);
    // Still on detail; nothing was deleted.
    await expect(page.locator('[data-testid="developer-loc-delete"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-loc-detail-error"]')).toHaveCount(0);
    expect(deletes, `unexpected locale DELETE: ${deletes.join(" | ")}`).toEqual([]);

    await backToCatalog(page, lang);
    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("catalog drops the row only after delete succeeds", async ({ page }) => {
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await openLocalesCatalog(page);
    const lang = uniqueLang();
    await createLocale(page, lang, `QA 6065 ${lang}`);
    await backToCatalog(page, lang);

    await localeRow(page, lang).click();
    await expect(page.locator('[data-testid="developer-loc-delete"]')).toBeVisible();
    const deleted = page.waitForResponse((response) =>
      isLocaleDelete(response.request()),
    );
    await page.locator('[data-testid="developer-loc-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    const response = await deleted;
    // Status only: success returns to the catalog, so the response body is
    // gone by the time navigation settles. Live DELETE answers 204.
    expect([200, 204]).toContain(response.status());
    await expect(page.locator('[data-testid="developer-loc-panel"]')).toBeVisible({
      timeout: 30_000,
    });
    await expect(localeRow(page, lang)).toHaveCount(0, { timeout: 30_000 });
    // Live Proof (#6065): pass-run capture of the catalog after the row
    // drops; committed copy under docs/ai-generated/proofs/.
    await page.screenshot({ path: "test-results/live-proof-6065-locale-create-delete.png" });
    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Admin 403, lock 409, missing 404, and duplicate 409 are named", async ({
    page,
  }) => {
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await openLocalesCatalog(page);
    const lang = uniqueLang();
    await createLocale(page, lang, `QA 6065 ${lang}`);
    await backToCatalog(page, lang);

    await localeRow(page, lang).click();
    await expect(page.locator('[data-testid="developer-loc-delete"]')).toBeVisible();

    const deleteCases = [
      { status: 403, body: "Admin role required", named: /Admin role required/ },
      { status: 409, body: "Locale has dependents", named: /locked or has dependents/i },
      { status: 404, body: "Locale not found", named: /was not found/ },
    ];
    let deleteRouted = false;
    for (const { status, body, named } of deleteCases) {
      if (deleteRouted) {
        await page.unroute("**/services/locales/*");
      }
      await page.route("**/services/locales/*", async (route) => {
        if (isLocaleDelete(route.request())) {
          await route.fulfill({
            status,
            contentType: "application/json",
            body: JSON.stringify({ message: body }),
          });
          return;
        }
        await route.continue();
      });
      await page.locator('[data-testid="developer-loc-delete"]').click();
      await expect(
        page.locator('[data-testid="developer-catalog-confirm-dialog"]'),
      ).toBeVisible({ timeout: 10_000 });
      await page.locator('[data-testid="developer-catalog-confirm-submit"]').click();
      const err = page.locator('[data-testid="developer-loc-detail-error"]');
      await expect(err).toBeVisible({ timeout: 20_000 });
      await expect(err).toContainText(named);
      // Still on detail; the locale was not deleted.
      await expect(page.locator('[data-testid="developer-loc-delete"]')).toBeVisible();
      deleteRouted = true;
    }
    await page.unroute("**/services/locales/*");

    // Routed create 403 surfaces the Admin name and does not create.
    await page.route("**/services/locales", async (route) => {
      if (isLocaleCreate(route.request())) {
        await route.fulfill({
          status: 403,
          contentType: "application/json",
          body: JSON.stringify({ message: "Admin role required" }),
        });
        return;
      }
      await route.continue();
    });
    await page.locator('[data-testid="developer-loc-back"]').click();
    await expect(page.locator('[data-testid="developer-loc-panel"]')).toBeVisible();
    const blockedLang = uniqueLang();
    await page.locator('[data-testid="developer-loc-new"]').click();
    await expect(page.locator('[data-testid="developer-loc-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-loc-language"]').fill(blockedLang);
    await page.locator('[data-testid="developer-loc-label"]').fill(`QA 6065 ${blockedLang}`);
    await page.locator('[data-testid="developer-loc-save"]').click();
    const createErr = page.locator('[data-testid="developer-loc-detail-error"]');
    await expect(createErr).toBeVisible({ timeout: 20_000 });
    await expect(createErr).toContainText(/Admin role required/);
    await page.unroute("**/services/locales");

    // Live duplicate language 409 surfaces the duplicate name.
    await page.locator('[data-testid="developer-loc-back"]').click();
    await expect(page.locator('[data-testid="developer-loc-panel"]')).toBeVisible();
    await page.locator('[data-testid="developer-loc-new"]').click();
    await expect(page.locator('[data-testid="developer-loc-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-loc-language"]').fill("en-us");
    await page.locator('[data-testid="developer-loc-label"]').fill("Duplicate English");
    await page.locator('[data-testid="developer-loc-save"]').click();
    const dupErr = page.locator('[data-testid="developer-loc-detail-error"]');
    await expect(dupErr).toBeVisible({ timeout: 20_000 });
    await expect(dupErr).toContainText(/already exists/i);

    // The original locale was never deleted: back on the catalog, its row stays.
    await page.locator('[data-testid="developer-loc-back"]').click();
    await expect(page.locator('[data-testid="developer-loc-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(localeRow(page, lang)).toBeVisible({ timeout: 20_000 });
    assertConsoleClean(pageErrors, consoleErrors);
  });
});
