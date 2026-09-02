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
 * Developer Item Filters create / save / delete chrome (#4060 AS-07 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-item-filter-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

function developerItemFiltersUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "item-filters",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** REST-safe unique filter name (no spaces, wildcards, or path characters). */
function uniqueFilterName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `qa4060${suffix || "x"}`;
}

async function openItemFiltersCatalog(page) {
  await page.goto(developerItemFiltersUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-if-panel"]');
  const empty = page.locator('[data-testid="developer-if-empty"]');
  const listError = page.locator('[data-testid="developer-if-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer item filters catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-if-new"]')).toBeVisible();
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

test.describe("Developer item filter editor (#4060 / AS-07)", () => {
  test("Admin can create, save, and delete an item filter", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openItemFiltersCatalog(page);

    const filterName = uniqueFilterName();
    const savedDescription = `${filterName} saved`;

    await page.locator('[data-testid="developer-if-new"]').click();
    await expect(page.locator('[data-testid="developer-if-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-if-save"]');
    await expect(saveBtn).toBeDisabled();

    await page.locator('[data-testid="developer-if-name"]').fill(filterName);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();

    const notice = page.locator('[data-testid="developer-if-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-if-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    await expect(page.locator('[data-testid="developer-if-name"]')).toHaveValue(filterName);
    await expect(page.locator('[data-testid="developer-if-name"]')).toBeDisabled();

    await page.locator('[data-testid="developer-if-description"]').fill(savedDescription);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Save failed: ${(await saveError.innerText()).trim()}`);
    }

    await page.locator('[data-testid="developer-if-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-if-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(`[data-if-name="${filterName}"]`)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("duplicate filter name 409 is surfaced in the UI", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openItemFiltersCatalog(page);

    await page.locator('[data-testid="developer-if-new"]').click();
    await expect(page.locator('[data-testid="developer-if-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-if-name"]').fill("preview");
    await page.locator('[data-testid="developer-if-save"]').click();

    const err = page.locator('[data-testid="developer-if-detail-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/already exists|409|duplicate/i);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
