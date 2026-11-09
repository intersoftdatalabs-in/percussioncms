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
 * Developer Locales — edit the fallback chain for one locale (#5788 / #1690).
 *
 * The chain edits and saves for one locale with a fallback-chain-only locale
 * PUT, so the language, label, description, enabled status, and every format
 * field stay. An empty chain clears the stored chain. An unknown, self, or
 * malformed entry shows an error with no success. HTTP 400, 403, 404, and 409
 * leave the previous chain.
 *
 *   npm run test:surface -- --path tests/developer-locale-fallback-chain.spec.js
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
function uniqueLang(prefix) {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `${prefix}-${suffix || "qa5788"}`;
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
      /Failed to load resource: the server responded with a status of (400|403|404|409)/.test(
        text,
      )
    ) {
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

function detailBody(payload) {
  if (!payload || typeof payload !== "object") return {};
  return payload.LocaleDetail || payload.localeDetail || payload;
}

function fallbackText(page) {
  return page.locator('[data-testid="developer-loc-set-fallback-text"]');
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

async function createLocale(page, lang, label) {
  await page.locator('[data-testid="developer-loc-new"]').click();
  await expect(page.locator('[data-testid="developer-loc-detail"]')).toBeVisible();
  await expect(page.locator('[data-testid="developer-loc-set-fallback"]')).toHaveCount(0);
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
  await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(lang);
}

async function openLocaleDetail(page, lang) {
  await page.locator('[data-testid="developer-loc-back"]').click();
  await expect(page.locator('[data-testid="developer-loc-panel"]')).toBeVisible({
    timeout: 20_000,
  });
  await page.locator(`[data-testid="developer-loc-open"][data-loc-lang="${lang}"]`).click();
  await expect(fallbackText(page)).toBeVisible({ timeout: 20_000 });
}

async function deleteLocale(page, lang) {
  const deleted = page.waitForResponse((response) => isLocaleDelete(response.request()));
  await page.locator('[data-testid="developer-loc-delete"]').click();
  await confirmDeveloperCatalogDelete(page);
  const deleteResponse = await deleted;
  expect(deleteResponse.status()).toBe(204);
  await expect(page.locator(`[data-testid="developer-loc-open"][data-loc-lang="${lang}"]`)).toHaveCount(
    0,
  );
}

test.describe("Developer edit the fallback chain for one locale (#5788)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("saves an ordered chain for one locale, keeps other fields, and clears on empty; unknown/self/malformed entries never claim success", async ({
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
    const langA = uniqueLang("xx");
    const langB = uniqueLang("xy");
    const labelA = `QA 5788 ${langA}`;
    await createLocale(page, langA, labelA);
    // createLocale leaves the new detail open; ensure the fallback section is ready.
    await expect(fallbackText(page)).toBeVisible({ timeout: 20_000 });
    await expect(fallbackText(page)).toHaveAttribute("data-loc-fallback", "");

    const writesBefore = updateUrls.length;
    await page.locator('[data-testid="developer-loc-set-fallback-edit"]').click();
    const addInput = page.locator('[data-testid="developer-loc-set-fallback-input"]');
    await addInput.fill("zz!!");
    await page.locator('[data-testid="developer-loc-set-fallback-add"]').click();
    await expect(page.locator('[data-testid="developer-loc-set-fallback-error"]')).toBeVisible();
    expect(updateUrls.length, "a malformed entry must not write").toBe(writesBefore);

    await addInput.fill(langA);
    await page.locator('[data-testid="developer-loc-set-fallback-add"]').click();
    await expect(page.locator('[data-testid="developer-loc-set-fallback-error"]')).toBeVisible();
    expect(updateUrls.length, "the locale itself must not write").toBe(writesBefore);

    await addInput.fill("en-us");
    await page.locator('[data-testid="developer-loc-set-fallback-add"]').click();
    const savedDone = page.waitForResponse((response) => isLocaleWrite(response.request()));
    await page.locator('[data-testid="developer-loc-set-fallback-save"]').click();
    const saved = await savedDone;
    expect(saved.status(), await saved.text()).toBe(200);
    expect(detailBody(saved.request().postDataJSON())).toEqual({
      format: { fallbackChain: ["en-us"] },
    });
    expect(deleteUrls, "saving a chain must not delete the locale").toEqual([]);
    await expect(fallbackText(page)).toHaveAttribute("data-loc-fallback", "en-us");
    await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(langA);
    await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(labelA);
    await expect(
      page.locator('[data-testid="developer-loc-set-fallback-notice"]'),
    ).toBeVisible();

    // Second known locale for ordering: create B, then move it ahead of en-us.
    await page.locator('[data-testid="developer-loc-back"]').click();
    await expect(page.locator('[data-testid="developer-loc-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await createLocale(page, langB, `QA 5788 ${langB}`);
    await openLocaleDetail(page, langA);
    await expect(fallbackText(page)).toHaveAttribute("data-loc-fallback", "en-us");
    await page.locator('[data-testid="developer-loc-set-fallback-edit"]').click();
    await page.locator('[data-testid="developer-loc-set-fallback-input"]').fill(langB);
    await page.locator('[data-testid="developer-loc-set-fallback-add"]').click();
    await page.locator('[data-testid="developer-loc-set-fallback-up-1"]').click();
    const reorderedDone = page.waitForResponse((response) => isLocaleWrite(response.request()));
    await page.locator('[data-testid="developer-loc-set-fallback-save"]').click();
    const reordered = await reorderedDone;
    expect(reordered.status(), await reordered.text()).toBe(200);
    expect(detailBody(reordered.request().postDataJSON())).toEqual({
      format: { fallbackChain: [langB, "en-us"] },
    });
    await expect(fallbackText(page)).toHaveAttribute(
      "data-loc-fallback",
      `${langB},en-us`,
    );
    await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(langA);
    await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(labelA);

    // Unknown but well-formed locale: the server rejects with 400, chain stays.
    await page.locator('[data-testid="developer-loc-set-fallback-edit"]').click();
    await page.locator('[data-testid="developer-loc-set-fallback-input"]').fill("zz-qa99");
    await page.locator('[data-testid="developer-loc-set-fallback-add"]').click();
    const unknownDone = page.waitForResponse((response) => isLocaleWrite(response.request()));
    await page.locator('[data-testid="developer-loc-set-fallback-save"]').click();
    const unknown = await unknownDone;
    expect(unknown.status()).toBe(400);
    await expect(page.locator('[data-testid="developer-loc-set-fallback-error"]')).toBeVisible();
    await expect(fallbackText(page)).toHaveAttribute(
      "data-loc-fallback",
      `${langB},en-us`,
    );
    await expect(page.locator('[data-testid="developer-loc-set-fallback-notice"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(langA);
    await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(labelA);

    // Forced failure statuses never claim success and keep the previous chain.
    const beforeForced = updateUrls.length;
    const statuses = [404, 403, 409];
    let forced = 0;
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
              format: {
                datePattern: "MM/yyyy",
                fallbackChain: ["it"],
              },
            },
          }),
        });
        return;
      }
      await route.continue();
    });
    for (const status of statuses) {
      const forcedDone = page.waitForResponse((response) => isLocaleWrite(response.request()));
      const editor = page.locator('[data-testid="developer-loc-set-fallback-editor"]');
      if ((await editor.count()) === 0) {
        await page.locator('[data-testid="developer-loc-set-fallback-edit"]').click();
      }
      await page.locator('[data-testid="developer-loc-set-fallback-save"]').click();
      const forcedResponse = await forcedDone;
      expect(forcedResponse.status()).toBe(status);
      await expect(
        page.locator('[data-testid="developer-loc-set-fallback-error"]'),
      ).toContainText(`forced ${status}`);
      await expect(fallbackText(page)).toHaveAttribute(
        "data-loc-fallback",
        `${langB},en-us`,
      );
      await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(langA);
      await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(labelA);
    }
    expect(updateUrls.length).toBe(beforeForced + statuses.length);
    expect(deleteUrls).toEqual([]);
    await page.unroute("**/services/locales/**");
    await page.locator('[data-testid="developer-loc-set-fallback-cancel"]').click();

    // Removing every entry and saving clears only this locale's chain.
    await page.locator('[data-testid="developer-loc-set-fallback-edit"]').click();
    await page.locator('[data-testid="developer-loc-set-fallback-remove-0"]').click();
    await page.locator('[data-testid="developer-loc-set-fallback-remove-0"]').click();
    const clearedDone = page.waitForResponse((response) => isLocaleWrite(response.request()));
    await page.locator('[data-testid="developer-loc-set-fallback-save"]').click();
    const cleared = await clearedDone;
    expect(cleared.status(), await cleared.text()).toBe(200);
    // Empty clears are sent blank-only: the CXF JSON provider drops nested
    // empty arrays, so a bare [] would arrive as omitted and preserve the chain.
    expect(detailBody(cleared.request().postDataJSON())).toEqual({
      format: { fallbackChain: [""] },
    });
    await expect(fallbackText(page)).toHaveAttribute("data-loc-fallback", "");
    await expect(page.locator('[data-testid="developer-loc-language"]')).toHaveValue(langA);
    await expect(page.locator('[data-testid="developer-loc-label"]')).toHaveValue(labelA);
    await expect(
      page.locator('[data-testid="developer-loc-set-fallback-notice"]'),
    ).toBeVisible();

    await deleteLocale(page, langA);
    // Delete returns to the catalog; open B directly for cleanup.
    await expect(page.locator('[data-testid="developer-loc-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await page.locator(`[data-testid="developer-loc-open"][data-loc-lang="${langB}"]`).click();
    await expect(fallbackText(page)).toBeVisible({ timeout: 20_000 });
    await deleteLocale(page, langB);

    assertClean(consoleErrors, pageErrors);
  });
});
