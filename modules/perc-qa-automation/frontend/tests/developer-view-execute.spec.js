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
 * Developer Views custom URL execute (#4472 UI-07 / parent #1690).
 *
 * Admin creates a user custom URL view (non-Inbox catalog key) and Execute
 * shows result rows or a documented error. Does not cover #4542–#4544.
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/developer-view-execute.spec.js
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
  return `qa4472${suffix || "x"}`;
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

test.describe("Developer view custom URL execute (#4472 / UI-07)", () => {
  test("Admin can execute a user custom URL view and see results or a documented error", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await openViewsCatalog(page);

    const viewName = uniqueViewName();
    const viewUrl = `../qa4472app/${viewName}.xml`;

    await page.locator('[data-testid="developer-vw-new"]').click();
    await expect(page.locator('[data-testid="developer-vw-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-vw-save"]');
    await page.locator('[data-testid="developer-vw-name"]').fill(viewName);
    await page.locator('[data-testid="developer-vw-type"]').selectOption("CustomView");
    await expect(page.locator('[data-testid="developer-vw-url"]')).toBeVisible();
    await page.locator('[data-testid="developer-vw-url"]').fill(viewUrl);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();

    const notice = page.locator('[data-testid="developer-vw-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-vw-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    const executeBtn = page.locator('[data-testid="developer-vw-execute"]');
    await expect(executeBtn).toBeVisible();
    await executeBtn.click();

    const results = page.locator('[data-testid="developer-vw-execute-results"]');
    const empty = page.locator('[data-testid="developer-vw-execute-empty"]');
    const execError = page.locator('[data-testid="developer-vw-detail-error"]');
    await expect(results.or(empty).or(execError).first()).toBeVisible({
      timeout: 20_000,
    });
    if (await execError.isVisible()) {
      const text = (await execError.innerText()).trim();
      expect(
        /unsupported custom url/i.test(text),
        `execute still reports the old Inbox-only gap: ${text}`,
      ).toBe(false);
      expect(text.length).toBeGreaterThan(0);
    }

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
});
