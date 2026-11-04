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
 * Developer Locales — set one locale first day of week (#5573 / #1690).
 *
 * The new first day of week shows only after the existing locale PUT succeeds.
 * The update sends format.firstDayOfWeek only, so the language string, label,
 * description, enabled status, date/time patterns, separators, currency
 * fields, and text direction stay. A blank first day of week clears it.
 * Out-of-range values do not write. Cancel does not write. HTTP 400, 403,
 * and 409 leave the previous first day of week.
 *
 *   npm run test:surface -- --path tests/developer-locale-first-day.spec.js
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
  return `xx-${suffix || "qa5573"}`;
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

function firstDayText(page) {
  return page.locator('[data-testid="developer-loc-set-first-day-text"]');
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
  await expect(page.locator('[data-testid="developer-loc-set-first-day"]')).toHaveCount(0);
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
  await page.locator('[data-testid="developer-loc-fmt-currency-pattern"]').fill("¤#,##0.00");
  await page.locator('[data-testid="developer-loc-fmt-dir"]').selectOption("ltr");
  await page.locator('[data-testid="developer-loc-fmt-first-day"]').fill("1");
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
  await expect(firstDayText(page)).toHaveAttribute("data-loc-first-day", "1");
  await expect(page.locator('[data-testid="developer-loc-fmt-date"]')).toHaveValue("yyyy-MM-dd");
  await expect(page.locator('[data-testid="developer-loc-fmt-currency-code"]')).toHaveValue("USD");
}

test.describe("Developer set a locale first day of week (#5573)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("sets the first day of week only after success, keeps language label description status date/time separators currency and text direction, and a blank value clears", async ({
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
    const label = `QA 5573 ${lang}`;
    await createLocaleWithFormat(page, lang, label);

    const writesBefore = updateUrls.length;
    await page.locator('[data-testid="developer-loc-set-first-day-edit"]').click();
    const dayInput = page.locator('[data-testid="developer-loc-set-first-day-input"]');
    await expect(dayInput).toHaveValue("1");
    await page.locator('[data-testid="developer-loc-set-first-day-save"]').click();
    await expect(dayInput).toHaveCount(0);
    expect(updateUrls.length, "the same first day of week must not update the locale").toBe(
      writesBefore,
    );
    await expect(firstDayText(page)).toHaveAttribute("data-loc-first-day", "1");

    await page.locator('[data-testid="developer-loc-set-first-day-edit"]').click();
    await dayInput.fill("8");
    await page.locator('[data-testid="developer-loc-set-first-day-save"]').click();
    expect(updateUrls.length, "out-of-range first day of week must not write").toBe(
      writesBefore,
    );
    await expect(page.locator('[data-testid="developer-loc-set-first-day-error"]')).toBeVisible();
    await expect(firstDayText(page)).toHaveAttribute("data-loc-first-day", "1");

    await dayInput.fill("2");
    await expect(firstDayText(page)).toHaveAttribute("data-loc-first-day", "1");
    await page.locator('[data-testid="developer-loc-set-first-day-cancel"]').click();
    await expect(dayInput).toHaveCount(0);
    expect(updateUrls.length, "cancel must not update the locale").toBe(writesBefore);
    expect(deleteUrls, "cancel must not delete the locale").toEqual([]);
    await expect(firstDayText(page)).toHaveAttribute("data-loc-first-day", "1");
    await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(lang);
    await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(label);
    await expect(page.locator('[data-testid="developer-loc-description"]')).toHaveValue(
      "Folder list",
    );
    await expect(page.locator('[data-testid="developer-loc-fmt-date"]')).toHaveValue("yyyy-MM-dd");
    await expect(page.locator('[data-testid="developer-loc-fmt-currency-code"]')).toHaveValue("USD");

    await page.locator('[data-testid="developer-loc-set-first-day-edit"]').click();
    await dayInput.fill(" 2 ");
    await expect(firstDayText(page)).toHaveAttribute("data-loc-first-day", "1");
    const savedDone = page.waitForResponse((response) => isLocaleWrite(response.request()));
    const listRefresh = page.waitForResponse((response) => isLocaleList(response));
    await page.locator('[data-testid="developer-loc-set-first-day-save"]').click();
    const saved = await savedDone;
    await listRefresh;
    expect(saved.status(), await saved.text()).toBe(200);
    const savedBody = detailBody(saved.request().postDataJSON());
    expect(savedBody).toEqual({ format: { firstDayOfWeek: 2 } });
    expect(deleteUrls, "setting a first day of week must not delete the locale").toEqual([]);
    await expect(firstDayText(page)).toHaveAttribute("data-loc-first-day", "2");
    await expect(page.locator('[data-testid="developer-loc-fmt-first-day"]')).toHaveValue("2");
    await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(lang);
    await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(label);
    await expect(page.locator('[data-testid="developer-loc-description"]')).toHaveValue(
      "Folder list",
    );
    await expect(page.locator('[data-testid="developer-loc-status"]')).toHaveValue("active");
    await expect(page.locator('[data-testid="developer-loc-fmt-date"]')).toHaveValue("yyyy-MM-dd");
    await expect(page.locator('[data-testid="developer-loc-fmt-time"]')).toHaveValue("HH:mm:ss");
    await expect(page.locator('[data-testid="developer-loc-fmt-datetime"]')).toHaveValue(
      "yyyy-MM-dd HH:mm:ss",
    );
    await expect(page.locator('[data-testid="developer-loc-fmt-decimal"]')).toHaveValue(".");
    await expect(page.locator('[data-testid="developer-loc-fmt-grouping"]')).toHaveValue(",");
    await expect(page.locator('[data-testid="developer-loc-fmt-currency-code"]')).toHaveValue("USD");
    await expect(page.locator('[data-testid="developer-loc-fmt-dir"]')).toHaveValue("ltr");
    await expect(page.locator('[data-testid="developer-loc-format-enable"]')).toBeChecked();
    await expect(page.locator('[data-testid="developer-loc-set-first-day-notice"]')).toBeVisible();

    await page.locator('[data-testid="developer-loc-back"]').click();
    await expect(page.locator('[data-testid="developer-loc-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await page.locator(`[data-testid="developer-loc-open"][data-loc-lang="${lang}"]`).click();
    await expect(firstDayText(page)).toHaveAttribute("data-loc-first-day", "2");
    await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(lang);
    await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(label);
    await expect(page.locator('[data-testid="developer-loc-fmt-date"]')).toHaveValue("yyyy-MM-dd");
    await expect(page.locator('[data-testid="developer-loc-fmt-currency-code"]')).toHaveValue("USD");
    await expect(page.locator('[data-testid="developer-loc-fmt-first-day"]')).toHaveValue("2");

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
              status: "inactive",
              format: { datePattern: "MM/yyyy", groupingSep: " ", firstDayOfWeek: 7 },
            },
          }),
        });
        return;
      }
      await route.continue();
    });
    for (const status of statuses) {
      const forcedDone = page.waitForResponse((response) => isLocaleWrite(response.request()));
      const draft = page.locator('[data-testid="developer-loc-set-first-day-input"]');
      if ((await draft.count()) === 0) {
        await page.locator('[data-testid="developer-loc-set-first-day-edit"]').click();
      }
      await draft.fill("7");
      await page.locator('[data-testid="developer-loc-set-first-day-save"]').click();
      const forcedResponse = await forcedDone;
      expect(forcedResponse.status()).toBe(status);
      await expect(
        page.locator('[data-testid="developer-loc-set-first-day-error"]'),
      ).toContainText(`forced ${status}`);
      await expect(firstDayText(page)).toHaveAttribute("data-loc-first-day", "2");
      await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(lang);
      await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(label);
      await expect(page.locator('[data-testid="developer-loc-fmt-currency-code"]')).toHaveValue(
        "USD",
      );
    }
    expect(updateUrls.length).toBe(beforeForced + statuses.length);
    expect(deleteUrls).toEqual([]);
    await page.unroute("**/services/locales/**");
    await page.locator('[data-testid="developer-loc-set-first-day-cancel"]').click();

    await page.locator('[data-testid="developer-loc-set-first-day-edit"]').click();
    await page.locator('[data-testid="developer-loc-set-first-day-input"]').fill("   ");
    const clearedDone = page.waitForResponse((response) => isLocaleWrite(response.request()));
    await page.locator('[data-testid="developer-loc-set-first-day-save"]').click();
    const cleared = await clearedDone;
    expect(cleared.status(), await cleared.text()).toBe(200);
    expect(detailBody(cleared.request().postDataJSON())).toEqual({
      format: { firstDayOfWeek: null },
    });
    await expect(firstDayText(page)).toHaveAttribute("data-loc-first-day", "");
    await expect(page.locator('[data-testid="developer-loc-fmt-first-day"]')).toHaveValue("");
    await expect(page.locator('[data-testid="developer-loc-fmt-currency-code"]')).toHaveValue("USD");
    await expect(page.locator('[data-testid="developer-loc-fmt-date"]')).toHaveValue("yyyy-MM-dd");
    await expect(page.locator('[data-testid="developer-loc-fmt-time"]')).toHaveValue("HH:mm:ss");
    await expect(page.locator('[data-testid="developer-loc-fmt-dir"]')).toHaveValue("ltr");
    await expect(page.locator('[data-testid="developer-loc-set-first-day-notice"]')).toBeVisible();

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
