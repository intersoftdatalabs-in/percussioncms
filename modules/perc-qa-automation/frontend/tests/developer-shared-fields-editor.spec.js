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
 * Developer Shared Fields create / save / delete chrome (#4029 CD-15 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-shared-fields-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerSharedFieldsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "shared-fields",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** REST-safe unique group name (no spaces or path characters). */
function uniqueGroupName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `qa4029${suffix || "x"}`;
}

async function openSharedFieldsCatalog(page) {
  await page.goto(developerSharedFieldsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-sf-panel"]');
  const empty = page.locator('[data-testid="developer-sf-empty"]');
  const listError = page.locator('[data-testid="developer-sf-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer shared fields catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-sf-new"]')).toBeVisible();
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

test.describe("Developer shared field group editor (#4029 / CD-15)", () => {
  test("Admin can create, save, and delete a shared field group", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    page.on("dialog", (dialog) => dialog.accept());

    await loginAsAdmin(page);
    await openSharedFieldsCatalog(page);

    const groupName = uniqueGroupName();
    const savedFilename = `${groupName}saved.xml`;

    await page.locator('[data-testid="developer-sf-new"]').click();
    await expect(page.locator('[data-testid="developer-sf-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-sf-save"]');
    await expect(saveBtn).toBeDisabled();

    await page.locator('[data-testid="developer-sf-name"]').fill(groupName);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();

    const notice = page.locator('[data-testid="developer-sf-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-sf-detail-error"]');
    await expect(notice).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    await expect(page.locator('[data-testid="developer-sf-name"]')).toHaveValue(groupName);

    await page.locator('[data-testid="developer-sf-filename"]').fill(savedFilename);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Save failed: ${(await saveError.innerText()).trim()}`);
    }

    await page.locator('[data-testid="developer-sf-delete"]').click();
    await expect(page.locator('[data-testid="developer-sf-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator(`[data-sf-name="${groupName}"]`)).toHaveCount(0);

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("duplicate group name 409 is surfaced in the UI", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);

    await loginAsAdmin(page);
    await openSharedFieldsCatalog(page);

    await page.locator('[data-testid="developer-sf-new"]').click();
    await expect(page.locator('[data-testid="developer-sf-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-sf-name"]').fill("shared");
    await page.locator('[data-testid="developer-sf-save"]').click();

    const err = page.locator('[data-testid="developer-sf-detail-error"]');
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/already exists|409|duplicate/i);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
