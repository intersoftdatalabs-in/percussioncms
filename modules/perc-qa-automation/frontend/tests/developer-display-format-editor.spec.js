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
 * Developer Display Formats create / delete chrome (#4086 UI-05 / parent #1690).
 *
 * Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   python docker/scripts/perc-devctl.py qa-health
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-display-format-editor.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerDisplayFormatsUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "display-formats",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/** REST-safe unique display-format name (no spaces, wildcards, or path characters). */
function uniqueDisplayFormatName() {
  const a = Date.now().toString(36).replace(/[^a-z0-9]/g, "").slice(-4);
  const b = Math.random().toString(36).replace(/[^a-z0-9]/g, "").slice(2, 6);
  const suffix = `${a}${b}`.slice(0, 8);
  return `qa4086${suffix || "x"}`;
}

async function openDisplayFormatsCatalog(page) {
  await page.goto(developerDisplayFormatsUrl(), { waitUntil: "networkidle" });
  await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
    timeout: 20_000,
  });
  const panel = page.locator('[data-testid="developer-df-panel"]');
  const empty = page.locator('[data-testid="developer-df-empty"]');
  const listError = page.locator('[data-testid="developer-df-error"]');
  await expect(panel.or(empty).or(listError).first()).toBeVisible({
    timeout: 30_000,
  });
  if (await listError.isVisible()) {
    throw new Error(
      `Developer display formats catalog error: ${(await listError.innerText()).trim()}`,
    );
  }
  await expect(page.locator('[data-testid="developer-df-new"]')).toBeVisible();
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

test.describe("Developer display format editor (#4086 / UI-05)", () => {
  test("Admin can create a display format and see it in the catalog", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    page.on("dialog", (dialog) => dialog.accept());

    await loginAsAdmin(page);
    await openDisplayFormatsCatalog(page);

    const formatName = uniqueDisplayFormatName();
    const formatLabel = `${formatName} label`;

    await page.locator('[data-testid="developer-df-new"]').click();
    await expect(page.locator('[data-testid="developer-df-detail"]')).toBeVisible();
    const saveBtn = page.locator('[data-testid="developer-df-save"]');
    await expect(saveBtn).toBeDisabled();

    await page.locator('[data-testid="developer-df-name"]').fill(formatName);
    await page.locator('[data-testid="developer-df-label"]').fill(formatLabel);
    await page.locator('[data-testid="developer-df-description"]').fill("SPA UI-05 create");
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();

    const notice = page.locator('[data-testid="developer-df-editor-notice"]');
    const saveError = page.locator('[data-testid="developer-df-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Create failed: ${(await saveError.innerText()).trim()}`);
    }

    await expect(page.locator('[data-testid="developer-df-name"]')).toHaveValue(formatName);
    await expect(page.locator('[data-testid="developer-df-name"]')).toBeDisabled();
    await expect(page.locator('[data-testid="developer-df-delete"]')).toBeVisible();

    await page.locator('[data-testid="developer-df-back"]').click();
    await expect(page.locator('[data-testid="developer-df-panel"]')).toBeVisible({
      timeout: 20_000,
    });
    const createdOpen = page.locator(
      `[data-testid="developer-df-open"][data-df-name="${formatName}"]`,
    );
    await expect(createdOpen).toHaveCount(1, { timeout: 20_000 });

    await createdOpen.click();
    await expect(page.locator('[data-testid="developer-df-detail"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-df-name"]')).toHaveValue(formatName);
    await expect(page.locator('[data-testid="developer-df-delete"]')).toBeVisible();

    assertConsoleClean(pageErrors, consoleErrors);
  });

  test("duplicate display format name 409 is surfaced in the UI", async ({ page }) => {
    test.setTimeout(120_000);
    const { pageErrors, consoleErrors } = attachConsoleGuards(page);
    page.on("dialog", (dialog) => dialog.accept());

    await loginAsAdmin(page);
    await openDisplayFormatsCatalog(page);

    const formatName = uniqueDisplayFormatName();

    await page.locator('[data-testid="developer-df-new"]').click();
    await expect(page.locator('[data-testid="developer-df-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-df-name"]').fill(formatName);
    await page.locator('[data-testid="developer-df-save"]').click();

    const notice = page.locator('[data-testid="developer-df-editor-notice"]');
    const err = page.locator('[data-testid="developer-df-detail-error"]');
    await expect(notice.or(err).first()).toBeVisible({ timeout: 20_000 });
    if (await err.isVisible()) {
      throw new Error(`Setup create failed: ${(await err.innerText()).trim()}`);
    }

    await page.locator('[data-testid="developer-df-back"]').click();
    await expect(page.locator('[data-testid="developer-df-panel"]')).toBeVisible({
      timeout: 20_000,
    });

    await page.locator('[data-testid="developer-df-new"]').click();
    await expect(page.locator('[data-testid="developer-df-detail"]')).toBeVisible();
    await page.locator('[data-testid="developer-df-name"]').fill(formatName);
    await page.locator('[data-testid="developer-df-save"]').click();
    await expect(err).toBeVisible({ timeout: 20_000 });
    await expect(err).toContainText(/already exists|409|duplicate/i);

    await page.locator('[data-testid="developer-df-cancel"]').click();
    await expect(page.locator('[data-testid="developer-df-panel"]')).toBeVisible();
    await expect(
      page.locator(`[data-testid="developer-df-open"][data-df-name="${formatName}"]`),
    ).toHaveCount(1);

    assertConsoleClean(pageErrors, consoleErrors);
  });
});
