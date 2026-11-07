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
 * Developer Locales — per-locale auto-translation rows (#5704 / #1690).
 *
 * The locale editor shows only the open locale's auto-translation rows.
 * Save replaces this locale's rows merged with every other locale's
 * untouched rows; removing every row and saving clears only this locale.
 * The locale PUT never fires, so language, label, description, enabled
 * status, and format fields stay. Unknown content type is 400 and the
 * editor shows the error without a success notice.
 *
 *   npm run test:surface -- --path tests/developer-locale-auto-translations.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

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
  return `xx-${suffix || "qa5704"}`;
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
    if (/Failed to load resource: the server responded with a status of (400|404)/.test(text)) {
      return false;
    }
    if (/rx_resources\/css\/(?:bootstrap|googleFonts)/.test(text)) {
      return false;
    }
    if (/favicon/i.test(text)) {
      return false;
    }
    return true;
  });
  expect(unexpected, `console error: ${unexpected.join(" | ")}`).toEqual([]);
}

function isLocaleWrite(request) {
  if (request.method() !== "PUT") return false;
  const path = request.url().split("?")[0];
  return /\/services\/locales\/[^/]+$/.test(path) && !/auto-translations$/.test(path);
}

function isLocaleDelete(request) {
  return request.method() === "DELETE" && /\/services\/locales\/[^/?]+$/.test(request.url());
}

function isAutoTranslationsPut(request) {
  return (
    request.method() === "PUT" && /\/services\/locales\/auto-translations/.test(request.url())
  );
}

function openLocalesCatalog(page) {
  await page.goto(developerLocalesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-loc-panel"]');
  const empty = page.locator('[data-testid="developer-loc-empty"]');
  const listError = page.locator('[data-testid="developer-loc-error"]');
  const sectionError = page.locator('[data-testid="developer-section-error"]');
  await expect(panel.or(empty).or(listError).or(sectionError).first()).toBeVisible({
    timeout: 30_000,
  });
  if ((await listError.isVisible()) || (await sectionError.isVisible())) {
    const node = (await listError.isVisible()) ? listError : sectionError;
    throw new Error(
      `Developer locales catalog error: ${(await node.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-loc-new"]')).toBeVisible();
}

async function createLocale(page, lang, label) {
  await page.locator('[data-testid="developer-loc-new"]').click();
  await expect(page.locator('[data-testid="developer-loc-detail"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-loc-set-at"]')).toHaveCount(0);
  await page.locator('[data-testid="developer-loc-language"]').fill(lang);
  await page.locator('[data-testid="developer-loc-label"]').fill(label);
  const created = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      /\/services\/locales\/?(?:\?|$)/.test(response.url()) &&
      !/auto-translations/.test(response.url()),
  );
  await page.locator('[data-testid="developer-loc-save"]').click();
  const response = await created;
  expect(response.status(), await response.text()).toBe(200);
  await expect(page.locator('[data-testid="developer-loc-editor-notice"]')).toBeVisible({
    timeout: 20_000,
  });
}

test.describe("Developer locale auto-translations (#5704)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("loads, saves, and clears one locale while other locales rows stay and locale fields stay", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const localePuts = [];
    const localeDeletes = [];
    const atPuts = [];
    page.on("request", (request) => {
      if (isLocaleWrite(request)) {
        localePuts.push(request.url());
      }
      if (isLocaleDelete(request)) {
        localeDeletes.push(request.url());
      }
      if (isAutoTranslationsPut(request)) {
        atPuts.push(request.postData() || "");
      }
    });

    await openLocalesCatalog(page);
    const lang = uniqueLang();
    const label = `QA 5704 ${lang}`;

    // Seed the singleton set with another locale's row; the editor must hide
    // it and preserve it across saves for this locale. Installed before the
    // locale detail (and its section) first loads.
    const otherRow = {
      locale: "en-us",
      contentTypeName: "percPage",
      workflowName: "Default Workflow",
      communityName: "Default",
    };
    let seed = { AutoTranslationRow: [otherRow] };
    await page.route("**/services/locales/auto-translations", async (route) => {
      const request = route.request();
      if (request.method() === "GET") {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(seed),
        });
        return;
      }
      if (request.method() === "PUT") {
        const body = request.postData() || '{"AutoTranslationRow":[]}';
        if (/xx-unknown-5704/i.test(body)) {
          await route.fulfill({
            status: 400,
            contentType: "application/json",
            body: JSON.stringify({ message: "unknown content type: xx-unknown-5704" }),
          });
          return;
        }
        seed = JSON.parse(body);
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(seed),
        });
        return;
      }
      await route.continue();
    });

    await createLocale(page, lang, label);

    await expect(page.locator('[data-testid="developer-loc-set-at"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-loc-set-at-text"]')).toHaveAttribute(
      "data-loc-at-count",
      "0",
    );
    await expect(page.locator('[data-testid="developer-loc-set-at-empty"]')).toBeVisible();

    const type =
      (await page
        .locator("#developer-loc-set-at-type-options option")
        .first()
        .getAttribute("value")) || "percPage";
    const workflow =
      (await page
        .locator("#developer-loc-set-at-workflow-options option")
        .first()
        .getAttribute("value")) || "Default Workflow";
    const community =
      (await page
        .locator("#developer-loc-set-at-community-options option")
        .first()
        .getAttribute("value")) || "Default";

    await page.locator('[data-testid="developer-loc-set-at-add"]').click();
    await expect(page.locator('[data-testid="developer-loc-set-at-row-0"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-loc-set-at-locale-0"]')).toHaveText(lang);
    await page.locator('[data-testid="developer-loc-set-at-type-0"]').fill(type);
    await page.locator('[data-testid="developer-loc-set-at-workflow-0"]').fill(workflow);
    await page.locator('[data-testid="developer-loc-set-at-community-0"]').fill(community);

    const localePutsBefore = localePuts.length;
    const savedDone = page.waitForResponse((response) =>
      isAutoTranslationsPut(response.request()),
    );
    await page.locator('[data-testid="developer-loc-set-at-save"]').click();
    const savedResponse = await savedDone;
    expect(savedResponse.status()).toBe(200);
    expect(atPuts.length).toBe(1);
    const savedBody = JSON.parse(atPuts[0]).AutoTranslationRow;
    expect(savedBody).toHaveLength(2);
    expect(savedBody[0]).toMatchObject(otherRow);
    expect(savedBody[1]).toMatchObject({ locale: lang, contentTypeName: type });
    expect(localePuts.length, "per-locale save must not PUT the locale").toBe(localePutsBefore);
    await expect(page.locator('[data-testid="developer-loc-set-at-notice"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-loc-set-at-text"]')).toHaveAttribute(
      "data-loc-at-count",
      "1",
    );
    await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(lang);
    await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(label);

    // Unknown content type is 400: error shows, no success is claimed.
    await page.locator('[data-testid="developer-loc-set-at-type-0"]').fill("xx-unknown-5704");
    await page.locator('[data-testid="developer-loc-set-at-save"]').click();
    await expect(page.locator('[data-testid="developer-loc-set-at-error"]')).toContainText(
      /unknown/i,
    );
    await expect(page.locator('[data-testid="developer-loc-set-at-notice"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-loc-set-at-text"]')).toHaveAttribute(
      "data-loc-at-count",
      "1",
    );
    expect(localePuts.length, "failed save must not PUT the locale").toBe(localePutsBefore);

    // Removing every row and saving clears only this locale.
    await page.locator('[data-testid="developer-loc-set-at-remove-0"]').click();
    await expect(page.locator('[data-testid="developer-loc-set-at-empty"]')).toBeVisible();
    await page.locator('[data-testid="developer-loc-set-at-save"]').click();
    await expect(page.locator('[data-testid="developer-loc-set-at-notice"]')).toContainText(
      /cleared/i,
    );
    expect(atPuts.length).toBe(3);
    const clearedBody = JSON.parse(atPuts[2]).AutoTranslationRow;
    expect(clearedBody).toHaveLength(1);
    expect(clearedBody[0]).toMatchObject(otherRow);
    await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(lang);
    await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(label);
    expect(localePuts.length, "clearing rows must not PUT the locale").toBe(localePutsBefore);
    expect(localeDeletes, "no locale delete during row edits").toEqual([]);

    await page.unroute("**/services/locales/auto-translations");
    const deleted = page.waitForResponse((response) => isLocaleDelete(response.request()));
    await page.locator('[data-testid="developer-loc-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    const deleteResponse = await deleted;
    expect(deleteResponse.status()).toBe(204);
    await expect(
      page.locator(`[data-testid="developer-loc-open"][data-loc-lang="${lang}"]`),
    ).toHaveCount(0);

    assertClean(consoleErrors, pageErrors);
  });
});
