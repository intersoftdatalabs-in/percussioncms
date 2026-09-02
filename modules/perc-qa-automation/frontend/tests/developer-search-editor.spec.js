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
 * Developer Searches create / delete chrome (#4076 UI-06 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-search-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

function developerSearchesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "searches",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** REST-safe unique search name (no spaces, wildcards, or path characters). */
function uniqueSearchName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `qa4076${suffix || "x"}`;
}

async function openSearchesCatalog(page) {
  await page.goto(developerSearchesUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-sr-panel"]');
  const empty = page.locator('[data-testid="developer-sr-empty"]');
  const listError = page.locator('[data-testid="developer-sr-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer searches catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-sr-new"]')).toBeVisible();
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

test.describe("Developer search editor (#4076 / UI-06)", () => {
  test("Admin can create and delete a standard search", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openSearchesCatalog(page);

    const searchName = uniqueSearchName();
    const searchLabel = `${searchName} label`;

    await page.locator('[data-testid="developer-sr-new"]').click();
    await expect(page.locator('[data-testid="developer-sr-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-sr-save"]');
    await expect(saveBtn).toBeDisabled();

    await page.locator('[data-testid="developer-sr-name"]').fill(searchName);
    await page.locator('[data-testid="developer-sr-label"]').fill(searchLabel);
    await page.locator('[data-testid="developer-sr-description"]').fill("SPA UI-06 create");
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();

    const notice = page.locator('[data-testid="developer-sr-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-sr-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    await expect(page.locator('[data-testid="developer-sr-name"]')).toHaveValue(searchName);
    await expect(page.locator('[data-testid="developer-sr-name"]')).toBeDisabled();
    await expect(page.locator('[data-testid="developer-sr-delete"]')).toBeVisible();

    await page.locator('[data-testid="developer-sr-back"]').click();
    await expect(page.locator('[data-testid="developer-sr-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    const createdRow = page.locator(`[data-sr-name="${searchName}"]`);
    await expect(createdRow).toHaveCount(1, { timeout: 20_000 });

    await createdRow.click();
    await expect(page.locator('[data-testid="developer-sr-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-sr-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-sr-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(`[data-sr-name="${searchName}"]`)).toHaveCount(0, {
      timeout: 20_000,
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("duplicate search name 409 is surfaced in the UI", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openSearchesCatalog(page);

    const searchName = uniqueSearchName();

    await page.locator('[data-testid="developer-sr-new"]').click();
    await expect(page.locator('[data-testid="developer-sr-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-sr-name"]').fill(searchName);
    await page.locator('[data-testid="developer-sr-save"]').click();

    const notice = page.locator('[data-testid="developer-sr-editor-notice"]');
    const err = page.locator('[data-testid="developer-sr-detail-error"]');
    await expect(notice.or(err).first()).toBeVisible({ timeout: 20_000 });
    if (await err.isVisible()) {
      throw new Error(`Setup create failed: ${(await err.innerText()).trim()}`);
    }

    await page.locator('[data-testid="developer-sr-back"]').click();
    await expect(page.locator('[data-testid="developer-sr-panel"]')).toBeVisible({
      timeout: 20_000,
    });

    await page.locator('[data-testid="developer-sr-new"]').click();
    await expect(page.locator('[data-testid="developer-sr-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-sr-name"]').fill(searchName);
    await page.locator('[data-testid="developer-sr-save"]').click();
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/already exists|409|duplicate/i);

    await page.locator('[data-testid="developer-sr-cancel"]').click();
    await expect(page.locator('[data-testid="developer-sr-panel"]')).toBeVisible();
    await page.locator(`[data-sr-name="${searchName}"]`).click();
    await expect(page.locator('[data-testid="developer-sr-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-sr-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-sr-panel"]')).toBeVisible({
      timeout: 20_000,
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
