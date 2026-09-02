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
 * Developer Views create / delete chrome (#4085 UI-07 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-view-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { confirmDeveloperCatalogDelete } = require("./helpers/developer-catalog-confirm");

function developerViewsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "views",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** REST-safe unique view name (no spaces, wildcards, or path characters). */
function uniqueViewName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `qa4085${suffix || "x"}`;
}

async function openViewsCatalog(page) {
  await page.goto(developerViewsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-vw-panel"]');
  const empty = page.locator('[data-testid="developer-vw-empty"]');
  const listError = page.locator('[data-testid="developer-vw-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer views catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-vw-new"]')).toBeVisible();
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

test.describe("Developer view editor (#4085 / UI-07)", () => {
  test("Admin can create and delete a standard view", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openViewsCatalog(page);

    const viewName = uniqueViewName();
    const viewLabel = `${viewName} label`;

    await page.locator('[data-testid="developer-vw-new"]').click();
    await expect(page.locator('[data-testid="developer-vw-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-vw-save"]');
    await expect(saveBtn).toBeDisabled();

    await page.locator('[data-testid="developer-vw-name"]').fill(viewName);
    await page.locator('[data-testid="developer-vw-label"]').fill(viewLabel);
    await page.locator('[data-testid="developer-vw-description"]').fill("SPA UI-07 create");
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();

    const notice = page.locator('[data-testid="developer-vw-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-vw-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    await expect(page.locator('[data-testid="developer-vw-name"]')).toHaveValue(viewName);
    await expect(page.locator('[data-testid="developer-vw-name"]')).toBeDisabled();
    await expect(page.locator('[data-testid="developer-vw-delete"]')).toBeVisible();

    await page.locator('[data-testid="developer-vw-back"]').click();
    await expect(page.locator('[data-testid="developer-vw-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    const createdRow = page.locator(`[data-vw-name="${viewName}"]`);
    await expect(createdRow).toHaveCount(1, { timeout: 20_000 });

    await createdRow.click();
    await expect(page.locator('[data-testid="developer-vw-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-vw-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-vw-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(`[data-vw-name="${viewName}"]`)).toHaveCount(0, {
      timeout: 20_000,
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("duplicate view name 409 is surfaced in the UI", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openViewsCatalog(page);

    const viewName = uniqueViewName();

    await page.locator('[data-testid="developer-vw-new"]').click();
    await expect(page.locator('[data-testid="developer-vw-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-vw-name"]').fill(viewName);
    await page.locator('[data-testid="developer-vw-save"]').click();

    const notice = page.locator('[data-testid="developer-vw-editor-notice"]');
    const err = page.locator('[data-testid="developer-vw-detail-error"]');
    await expect(notice.or(err).first()).toBeVisible({ timeout: 20_000 });
    if (await err.isVisible()) {
      throw new Error(`Setup create failed: ${(await err.innerText()).trim()}`);
    }

    await page.locator('[data-testid="developer-vw-back"]').click();
    await expect(page.locator('[data-testid="developer-vw-panel"]')).toBeVisible({
      timeout: 20_000,
    });

    await page.locator('[data-testid="developer-vw-new"]').click();
    await expect(page.locator('[data-testid="developer-vw-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-vw-name"]').fill(viewName);
    await page.locator('[data-testid="developer-vw-save"]').click();
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/already exists|409|duplicate/i);

    await page.locator('[data-testid="developer-vw-cancel"]').click();
    await expect(page.locator('[data-testid="developer-vw-panel"]')).toBeVisible();
    await page.locator(`[data-vw-name="${viewName}"]`).click();
    await expect(page.locator('[data-testid="developer-vw-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-vw-delete"]').click();
    await confirmDeveloperCatalogDelete(page);
    await expect(page.locator('[data-testid="developer-vw-panel"]')).toBeVisible({
      timeout: 20_000,
    });

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("Inbox-family views are not deleted from this catalog", async ({ page }) => {
    test.setTimeout(90_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openViewsCatalog(page);

    const inboxOpen = page.locator('[data-vw-name="Inbox"]');
    await expect(inboxOpen).toHaveCount(1, { timeout: 20_000 });
    await inboxOpen.click();
    await expect(page.locator('[data-testid="developer-vw-detail"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-vw-protected-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-vw-delete"]')).toHaveCount(0);

    await page.locator('[data-testid="developer-vw-back"]').click();
    await expect(page.locator('[data-testid="developer-vw-panel"]')).toBeVisible();
    await expect(page.locator('[data-vw-name="Inbox"]')).toHaveCount(1);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
