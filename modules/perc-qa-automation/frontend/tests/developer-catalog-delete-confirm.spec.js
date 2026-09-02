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
 * Developer catalog in-app delete confirm (#4122 / parent #4112 / #1690).
 *
 * Uses Developer Locales create/delete as the live catalog family sample
 * (same CatalogConfirmDialog as Searches, Views, Slots, …).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-catalog-delete-confirm.spec.js
 *   perc-devctl qa-down
 * </pre>
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

function uniqueLang() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `xx-${suffix || "qa4122"}`;
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

test.describe("Developer catalog in-app delete confirm (#4122)", () => {
  test("Locales delete uses in-app dialog, not window.confirm", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    const nativeDialogs = [];
    page.on("dialog", (dialog) => {
      nativeDialogs.push(dialog.type());
      void dialog.dismiss();
    });

    await loginAsAdmin(page);
    await openLocalesCatalog(page);

    const lang = uniqueLang();
    const label = `QA 4122 ${lang}`;

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

    await page.locator('[data-testid="developer-loc-delete"]').click();
    const dialog = page.locator('[data-testid="developer-catalog-confirm-dialog"]');
    await expect(dialog).toBeVisible();
    await expect(dialog).toHaveAttribute("role", "dialog");
    await expect(dialog).toHaveAttribute("aria-modal", "true");
    expect(nativeDialogs, "native window.confirm must not open").toEqual([]);

    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-loc-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(`[data-loc-lang="${lang}"]`)).toHaveCount(0);
    expect(nativeDialogs).toEqual([]);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
