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
 * Developer Locales — set one locale enabled status (#5550 / #1690).
 *
 * The new enabled status shows only after the existing locale PUT succeeds.
 * The update sends status only, so the language string, label, description,
 * and format stay. Cancel does not write. HTTP 400, 403, and 409 leave the
 * previous status.
 *
 *   npm run test:surface -- --path tests/developer-locale-enabled-status.spec.js
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
  return `xx-${suffix || "qa5550"}`;
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
    if (/Failed to load resource: the server responded with a status of 404/.test(text)) {
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

function isLocaleList(response) {
  return (
    response.request().method() === "GET" &&
    /\/services\/locales\/?(?:\?|$)/.test(response.url()) &&
    !/auto-translations/.test(response.url()) &&
    response.status() === 200
  );
}

function detailBody(payload) {
  if (!payload || typeof payload !== "object") return {};
  return payload.LocaleDetail || payload.localeDetail || payload;
}

function enabledStatusText(page) {
  return page.locator('[data-testid="developer-loc-set-enabled-status-text"]');
}

async function openLocalesCatalog(page) {
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

async function createLocaleWithFormat(page, lang, label) {
  await page.locator('[data-testid="developer-loc-new"]').click();
  await expect(page.locator('[data-testid="developer-loc-detail"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-loc-set-enabled-status"]')).toHaveCount(0);
  await page.locator('[data-testid="developer-loc-language"]').fill(lang);
  await page.locator('[data-testid="developer-loc-label"]').fill(label);
  await page.locator('[data-testid="developer-loc-description"]').fill("Folder list");
  await page.locator('[data-testid="developer-loc-format-enable"]').check();
  await page.locator('[data-testid="developer-loc-fmt-date"]').fill("yyyy-MM-dd");
  await page.locator('[data-testid="developer-loc-fmt-time"]').fill("HH:mm:ss");
  await page.locator('[data-testid="developer-loc-fmt-datetime"]').fill("yyyy-MM-dd HH:mm:ss");
  await page.locator('[data-testid="developer-loc-fmt-decimal"]').fill(".");
  await page.locator('[data-testid="developer-loc-fmt-grouping"]').fill(",");
  await page.locator('[data-testid="developer-loc-fmt-currency-code"]').fill("USD");
  await page.locator('[data-testid="developer-loc-fmt-dir"]').selectOption("ltr");
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
  await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(lang);
  await expect(enabledStatusText(page)).toHaveAttribute("data-loc-enabled-status", "active");
  await expect(page.locator('[data-testid="developer-loc-status"]')).toHaveValue("active");
  await expect(page.locator('[data-testid="developer-loc-fmt-date"]')).toHaveValue("yyyy-MM-dd");
  await expect(page.locator('[data-testid="developer-loc-fmt-grouping"]')).toHaveValue(",");
}

test.describe("Developer set a locale enabled status (#5550)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("sets enabled status only after success, keeps language label description and format, and 400/403/409 keep the previous status", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    const updateUrls = [];
    const deleteUrls = [];
    page.on("request", (request) => {
      if (isLocaleWrite(request)) {
        updateUrls.push(request.url());
      }
      if (isLocaleDelete(request)) {
        deleteUrls.push(request.url());
      }
    });

    await openLocalesCatalog(page);
    const lang = uniqueLang();
    const label = `QA 5550 ${lang}`;
    await createLocaleWithFormat(page, lang, label);

    const writesBeforeStatus = updateUrls.length;
    await page.locator('[data-testid="developer-loc-set-enabled-status-edit"]').click();
    const statusInput = page.locator('[data-testid="developer-loc-set-enabled-status-input"]');
    await expect(statusInput).toHaveValue("active");
    await page.locator('[data-testid="developer-loc-set-enabled-status-save"]').click();
    await expect(statusInput).toHaveCount(0);
    expect(updateUrls.length, "the same status must not update the locale").toBe(
      writesBeforeStatus,
    );
    await expect(enabledStatusText(page)).toHaveAttribute("data-loc-enabled-status", "active");

    await page.locator('[data-testid="developer-loc-set-enabled-status-edit"]').click();
    await statusInput.selectOption("inactive");
    await expect(enabledStatusText(page)).toHaveAttribute("data-loc-enabled-status", "active");
    await page.locator('[data-testid="developer-loc-set-enabled-status-cancel"]').click();
    await expect(statusInput).toHaveCount(0);
    expect(updateUrls.length, "cancel must not update the locale").toBe(writesBeforeStatus);
    expect(deleteUrls, "cancel must not delete the locale").toEqual([]);
    await expect(enabledStatusText(page)).toHaveAttribute("data-loc-enabled-status", "active");
    await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(lang);
    await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(label);
    await expect(page.locator('[data-testid="developer-loc-description"]')).toHaveValue(
      "Folder list",
    );
    await expect(page.locator('[data-testid="developer-loc-fmt-date"]')).toHaveValue("yyyy-MM-dd");
    await expect(page.locator('[data-testid="developer-loc-fmt-grouping"]')).toHaveValue(",");

    await page.locator('[data-testid="developer-loc-set-enabled-status-edit"]').click();
    await statusInput.selectOption("inactive");
    await expect(enabledStatusText(page)).toHaveAttribute("data-loc-enabled-status", "active");
    const savedDone = page.waitForResponse((response) => isLocaleWrite(response.request()));
    const listRefresh = page.waitForResponse((response) => isLocaleList(response));
    await page.locator('[data-testid="developer-loc-set-enabled-status-save"]').click();
    const saved = await savedDone;
    await listRefresh;
    expect(saved.status(), await saved.text()).toBe(200);
    const savedBody = detailBody(saved.request().postDataJSON());
    expect(savedBody).toEqual({ status: "inactive" });
    expect(deleteUrls, "setting enabled status must not delete the locale").toEqual([]);
    await expect(enabledStatusText(page)).toHaveAttribute("data-loc-enabled-status", "inactive");
    await expect(page.locator('[data-testid="developer-loc-status"]')).toHaveValue("inactive");
    await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(lang);
    await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(label);
    await expect(page.locator('[data-testid="developer-loc-description"]')).toHaveValue(
      "Folder list",
    );
    await expect(page.locator('[data-testid="developer-loc-fmt-date"]')).toHaveValue("yyyy-MM-dd");
    await expect(page.locator('[data-testid="developer-loc-fmt-time"]')).toHaveValue("HH:mm:ss");
    await expect(page.locator('[data-testid="developer-loc-fmt-datetime"]')).toHaveValue(
      "yyyy-MM-dd HH:mm:ss",
    );
    await expect(page.locator('[data-testid="developer-loc-fmt-decimal"]')).toHaveValue(".");
    await expect(page.locator('[data-testid="developer-loc-fmt-grouping"]')).toHaveValue(",");
    await expect(page.locator('[data-testid="developer-loc-fmt-dir"]')).toHaveValue("ltr");
    await expect(page.locator('[data-testid="developer-loc-fmt-currency-code"]')).toHaveValue("USD");
    await expect(page.locator('[data-testid="developer-loc-format-enable"]')).toBeChecked();
    await expect(page.locator('[data-testid="developer-loc-set-enabled-status-notice"]')).toBeVisible();

    await page.locator('[data-testid="developer-loc-back"]').click();
    await expect(page.locator('[data-testid="developer-loc-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await page.locator(`[data-testid="developer-loc-open"][data-loc-lang="${lang}"]`).click();
    await expect(enabledStatusText(page)).toHaveAttribute("data-loc-enabled-status", "inactive");
    await expect(page.locator('[data-testid="developer-loc-status"]')).toHaveValue("inactive");
    await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(lang);
    await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(label);
    await expect(page.locator('[data-testid="developer-loc-fmt-date"]')).toHaveValue("yyyy-MM-dd");
    await expect(page.locator('[data-testid="developer-loc-fmt-grouping"]')).toHaveValue(",");

    const beforeForced = updateUrls.length;
    let forced = 0;
    const statuses = [400, 403, 409];
    await page.route("**/services/locales/**", async (route) => {
      const request = route.request();
      if (isLocaleWrite(request) && forced < statuses.length) {
        const status = statuses[forced];
        forced += 1;
        await route.fulfill({
          status,
          contentType: "application/json",
          body: JSON.stringify({
            message: `forced ${status}`,
            LocaleDetail: {
              languageString: "fr-ca",
              label: "Renamed",
              description: "changed",
              status: "active",
              format: { datePattern: "MM/yyyy", groupingSep: " ", currencyCode: "GBP" },
            },
          }),
        });
        return;
      }
      await route.continue();
    });
    for (const status of statuses) {
      const forcedDone = page.waitForResponse((response) => isLocaleWrite(response.request()));
      const draft = page.locator('[data-testid="developer-loc-set-enabled-status-input"]');
      if ((await draft.count()) === 0) {
        await page.locator('[data-testid="developer-loc-set-enabled-status-edit"]').click();
      }
      await draft.selectOption("active");
      await page.locator('[data-testid="developer-loc-set-enabled-status-save"]').click();
      const forcedResponse = await forcedDone;
      expect(forcedResponse.status()).toBe(status);
      await expect(
        page.locator('[data-testid="developer-loc-set-enabled-status-error"]'),
      ).toContainText(`forced ${status}`);
      await expect(enabledStatusText(page)).toHaveAttribute("data-loc-enabled-status", "inactive");
      await expect(page.locator('[data-testid="developer-loc-status"]')).toHaveValue("inactive");
      await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(lang);
      await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(label);
      await expect(page.locator('[data-testid="developer-loc-fmt-grouping"]')).toHaveValue(",");
    }
    expect(updateUrls.length).toBe(beforeForced + statuses.length);
    expect(deleteUrls).toEqual([]);
    await page.unroute("**/services/locales/**");
    await page.locator('[data-testid="developer-loc-set-enabled-status-cancel"]').click();

    const deleted = page.waitForResponse((response) => isLocaleDelete(response.request()));
    await page.locator('[data-testid="developer-loc-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    const deleteResponse = await deleted;
    expect(deleteResponse.status()).toBe(204);
    await expect(page.locator(`[data-testid="developer-loc-open"][data-loc-lang="${lang}"]`)).toHaveCount(
      0,
    );

    assertClean(consoleErrors, pageErrors);
  });
});
