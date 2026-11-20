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
 * Developer ContentType/Locale — set-label/set-description keyboard path
 * (#6747 / parent #6733; mirrors slot #6713/#6733 and display-format #6630).
 *
 * The label and description inputs autofocus when the editor opens, and
 * Escape cancels the edit without writing (same as Cancel): no PUT fires
 * and the stored value is kept. When an editor closes (Escape, Cancel, or
 * a successful save) the input unmounts, so focus returns to the invoking
 * edit button instead of falling back to <body>.
 *
 *   npm run test:surface -- --path tests/developer-ct-loc-set-editors-keyboard.spec.js
 *
 * QA mode: perc-devctl qa-up → qa-health → TEST_CMS_URL → test:surface → qa-down.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

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
  return `xx-${suffix || "qa6747"}`;
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

function isContentTypeDetailWrite(request) {
  return (
    request.method() === "PUT" &&
    /\/services\/contenttypes\/[^/?]+\/?(?:\?|$)/.test(request.url())
  );
}

function isLocaleWrite(request) {
  if (request.method() !== "PUT") return false;
  const path = request.url().split("?")[0];
  return /\/services\/locales\/[^/]+$/.test(path) && !/auto-translations$/.test(path);
}

function isLocaleDelete(request) {
  return request.method() === "DELETE" && /\/services\/locales\/[^/?]+$/.test(request.url());
}

async function openPercPage(page) {
  await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const openBtn = page.locator('[data-testid="developer-ct-open"][data-ct-name="percPage"]');
  await expect(openBtn, "H2 catalog must include percPage").toBeVisible({ timeout: 20_000 });
  await openBtn.click();
  const detail = page.locator('[data-testid="developer-ct-detail"]');
  const detailError = page.locator('[data-testid="developer-ct-detail-error"]');
  await expect(detail.or(detailError).first()).toBeVisible({ timeout: 30_000 });
  if (await detailError.isVisible()) {
    throw new Error(`Content type detail error: ${(await detailError.innerText()).trim()}`);
  }
  await expect(page.locator('[data-testid="developer-ct-set-label"]')).toBeVisible({
    timeout: 30_000,
  });
  await page.locator('[data-testid="developer-ct-lock"]').click();
  await expect(page.locator('[data-testid="developer-ct-lock-status"]')).toHaveText(
    /Locked by you/i,
    { timeout: 20_000 },
  );
  await expect(page.locator('[data-testid="developer-ct-set-label-edit"]')).toBeEnabled();
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
    throw new Error(`Developer locales catalog error: ${(await node.innerText()).trim()}`);
  }
  await expect(page.locator('[data-testid="developer-loc-new"]')).toBeVisible();
}

async function createLocale(page, lang, label) {
  await page.locator('[data-testid="developer-loc-new"]').click();
  await expect(page.locator('[data-testid="developer-loc-detail"]')).toBeVisible();
  await page.locator('[data-testid="developer-loc-language"]').fill(lang);
  await page.locator('[data-testid="developer-loc-label"]').fill(label);
  await page.locator('[data-testid="developer-loc-description"]').fill("QA locale");
  const created = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      /\/services\/locales\/?(?:\?|$)/.test(response.url()),
  );
  await page.locator('[data-testid="developer-loc-save"]').click();
  const response = await created;
  expect(response.status(), await response.text()).toBe(200);
  await expect(page.locator('[data-testid="developer-loc-set-label"]')).toBeVisible({
    timeout: 30_000,
  });
}

async function deleteLocale(page, lang) {
  const deleted = page.waitForResponse((response) => isLocaleDelete(response.request()));
  await page.locator('[data-testid="developer-loc-delete"]').click();
  await confirmDeveloperCatalogDelete(page);
  const deleteResponse = await deleted;
  expect(deleteResponse.status()).toBe(204);
}

test.describe("Developer CT/Locale set-label/set-description keyboard path (#6747)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
    await loginAsAdmin(page);
  });

  test("content-type label input autofocuses, Escape cancels, focus returns", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openPercPage(page);
    const writes = [];
    page.on("request", (request) => {
      if (isContentTypeDetailWrite(request)) {
        writes.push(request.url());
      }
    });
    const original =
      (await page
        .locator('[data-testid="developer-ct-set-label-text"]')
        .getAttribute("data-ct-label")) || "";

    // Escape closes the editor and returns focus without writing.
    await page.locator('[data-testid="developer-ct-set-label-edit"]').click();
    const labelInput = page.locator('[data-testid="developer-ct-set-label-input"]');
    await expect(labelInput).toBeFocused();
    await labelInput.fill("not-saved");
    await labelInput.press("Escape");
    await expect(labelInput).toHaveCount(0);
    expect(writes, "Escape must not update the content type").toEqual([]);
    await expect(page.locator('[data-testid="developer-ct-set-label-edit"]')).toBeFocused();
    await expect(page.locator('[data-testid="developer-ct-set-label-text"]')).toHaveAttribute(
      "data-ct-label",
      original,
    );

    // Cancel closes the editor and returns focus without writing.
    await page.locator('[data-testid="developer-ct-set-label-edit"]').click();
    await expect(page.locator('[data-testid="developer-ct-set-label-input"]')).toBeFocused();
    await page.locator('[data-testid="developer-ct-set-label-input"]').fill("not-saved");
    await page.locator('[data-testid="developer-ct-set-label-cancel"]').click();
    await expect(page.locator('[data-testid="developer-ct-set-label-input"]')).toHaveCount(0);
    expect(writes, "Cancel must not update the content type").toEqual([]);
    await expect(page.locator('[data-testid="developer-ct-set-label-edit"]')).toBeFocused();

    // Save writes, then returns focus to the edit button.
    await page.locator('[data-testid="developer-ct-set-label-edit"]').click();
    await page.locator('[data-testid="developer-ct-set-label-input"]').fill("qa6747 label");
    const saved = page.waitForResponse((response) =>
      isContentTypeDetailWrite(response.request()),
    );
    await page.locator('[data-testid="developer-ct-set-label-save"]').click();
    const saveResponse = await saved;
    expect(saveResponse.status(), await saveResponse.text()).toBe(200);
    await expect(page.locator('[data-testid="developer-ct-set-label-input"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-ct-set-label-edit"]')).toBeFocused();
    await expect(page.locator('[data-testid="developer-ct-set-label-notice"]')).toHaveAttribute(
      "role",
      "status",
    );

    // Restore the original label so the H2 catalog stays stable.
    await page.locator('[data-testid="developer-ct-set-label-edit"]').click();
    await page.locator('[data-testid="developer-ct-set-label-input"]').fill(original);
    const restored = page.waitForResponse((response) =>
      isContentTypeDetailWrite(response.request()),
    );
    await page.locator('[data-testid="developer-ct-set-label-save"]').click();
    expect((await restored).status()).toBe(200);
    await page.screenshot({ path: `test-results/live-proof-6747-ct-label-keyboard.png` });
    assertClean(consoleErrors, pageErrors);
  });

  test("content-type description input autofocuses, Escape cancels, focus returns", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openPercPage(page);
    const writes = [];
    page.on("request", (request) => {
      if (isContentTypeDetailWrite(request)) {
        writes.push(request.url());
      }
    });
    const original =
      (await page
        .locator('[data-testid="developer-ct-set-description-text"]')
        .getAttribute("data-ct-description")) || "";

    await page.locator('[data-testid="developer-ct-set-description-edit"]').click();
    const descriptionInput = page.locator(
      '[data-testid="developer-ct-set-description-input"]',
    );
    await expect(descriptionInput).toBeFocused();
    await descriptionInput.fill("not-saved");
    await descriptionInput.press("Escape");
    await expect(descriptionInput).toHaveCount(0);
    expect(writes, "Escape must not update the content type").toEqual([]);
    await expect(
      page.locator('[data-testid="developer-ct-set-description-edit"]'),
    ).toBeFocused();
    await expect(page.locator('[data-testid="developer-ct-set-description-cancel"]')).toHaveCount(
      0,
    );

    await page.locator('[data-testid="developer-ct-set-description-edit"]').click();
    await expect(
      page.locator('[data-testid="developer-ct-set-description-input"]'),
    ).toBeFocused();
    await page.locator('[data-testid="developer-ct-set-description-input"]').fill("not-saved");
    await page.locator('[data-testid="developer-ct-set-description-cancel"]').click();
    await expect(
      page.locator('[data-testid="developer-ct-set-description-input"]'),
    ).toHaveCount(0);
    await expect(
      page.locator('[data-testid="developer-ct-set-description-edit"]'),
    ).toBeFocused();
    await expect(page.locator('[data-testid="developer-ct-set-description-text"]')).toHaveAttribute(
      "data-ct-description",
      original,
    );
    await page.screenshot({ path: `test-results/live-proof-6747-ct-description-keyboard.png` });
    assertClean(consoleErrors, pageErrors);
  });

  test("locale label input autofocuses, Escape cancels, focus returns", async ({ page }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openLocalesCatalog(page);
    const lang = uniqueLang();
    await createLocale(page, lang, "QA locale");
    const writes = [];
    page.on("request", (request) => {
      if (isLocaleWrite(request)) {
        writes.push(request.url());
      }
    });

    await page.locator('[data-testid="developer-loc-set-label-edit"]').click();
    const labelInput = page.locator('[data-testid="developer-loc-set-label-input"]');
    await expect(labelInput).toBeFocused();
    await labelInput.fill("not-saved");
    await labelInput.press("Escape");
    await expect(labelInput).toHaveCount(0);
    expect(writes, "Escape must not update the locale").toEqual([]);
    await expect(page.locator('[data-testid="developer-loc-set-label-edit"]')).toBeFocused();
    await expect(page.locator('[data-testid="developer-loc-set-label-text"]')).toHaveAttribute(
      "data-loc-label",
      "QA locale",
    );

    await page.locator('[data-testid="developer-loc-set-label-edit"]').click();
    await expect(page.locator('[data-testid="developer-loc-set-label-input"]')).toBeFocused();
    await page.locator('[data-testid="developer-loc-set-label-input"]').fill("not-saved");
    await page.locator('[data-testid="developer-loc-set-label-cancel"]').click();
    await expect(page.locator('[data-testid="developer-loc-set-label-input"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-loc-set-label-edit"]')).toBeFocused();

    await page.locator('[data-testid="developer-loc-set-label-edit"]').click();
    await page.locator('[data-testid="developer-loc-set-label-input"]').fill("qa6747 locale");
    const saved = page.waitForResponse((response) => isLocaleWrite(response.request()));
    await page.locator('[data-testid="developer-loc-set-label-save"]').click();
    const saveResponse = await saved;
    expect(saveResponse.status(), await saveResponse.text()).toBe(200);
    await expect(page.locator('[data-testid="developer-loc-set-label-input"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-loc-set-label-edit"]')).toBeFocused();
    await expect(page.locator('[data-testid="developer-loc-set-label-notice"]')).toHaveAttribute(
      "role",
      "status",
    );
    await page.screenshot({ path: `test-results/live-proof-6747-loc-label-keyboard.png` });

    await deleteLocale(page, lang);
    assertClean(consoleErrors, pageErrors);
  });

  test("locale description input autofocuses, Escape cancels, focus returns", async ({
    page,
  }) => {
    const { consoleErrors, pageErrors } = attachConsole(page);
    await openLocalesCatalog(page);
    const lang = uniqueLang();
    await createLocale(page, lang, "QA locale");
    const writes = [];
    page.on("request", (request) => {
      if (isLocaleWrite(request)) {
        writes.push(request.url());
      }
    });

    await page.locator('[data-testid="developer-loc-set-description-edit"]').click();
    const descriptionInput = page.locator(
      '[data-testid="developer-loc-set-description-input"]',
    );
    await expect(descriptionInput).toBeFocused();
    await descriptionInput.fill("not-saved");
    await descriptionInput.press("Escape");
    await expect(descriptionInput).toHaveCount(0);
    expect(writes, "Escape must not update the locale").toEqual([]);
    await expect(
      page.locator('[data-testid="developer-loc-set-description-edit"]'),
    ).toBeFocused();
    await expect(page.locator('[data-testid="developer-loc-set-description-text"]')).toHaveAttribute(
      "data-loc-description",
      "QA locale",
    );

    await page.locator('[data-testid="developer-loc-set-description-edit"]').click();
    await expect(
      page.locator('[data-testid="developer-loc-set-description-input"]'),
    ).toBeFocused();
    await page.locator('[data-testid="developer-loc-set-description-input"]').fill("not-saved");
    await page.locator('[data-testid="developer-loc-set-description-cancel"]').click();
    await expect(
      page.locator('[data-testid="developer-loc-set-description-input"]'),
    ).toHaveCount(0);
    await expect(
      page.locator('[data-testid="developer-loc-set-description-edit"]'),
    ).toBeFocused();
    await page.screenshot({
      path: `test-results/live-proof-6747-loc-description-keyboard.png`,
    });

    await deleteLocale(page, lang);
    assertClean(consoleErrors, pageErrors);
  });
});
