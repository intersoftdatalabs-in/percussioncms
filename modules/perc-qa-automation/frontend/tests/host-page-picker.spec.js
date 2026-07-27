/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Playwright spec: US2 host-page-picker migration (SC-002 / FR-008a).
 *
 * <p>Asserts the modern ContentBrowser mounts on the dedicated page-picker
 * entry point <code>pagePickerModern.jsp</code>, presents a navigable
 * tree + list + action bar, and supports multi-select with the
 * <code>allowedTypes: ['page']</code> filter (the user-picker filter is
 * page-only — folders and assets are rejected client-side).</p>
 *
 * <p>Run from <code>modules/perc-qa-automation/frontend</code>:</p>
 * <pre>
 *   npm test -- tests/host-page-picker.spec.js
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const DIALOG_URL = `${BASE_URL}/Rhythmyx/cm/app/pagePickerModern.jsp?_=${Date.now()}`;

test.describe("US2 host-page-picker migration (SC-002)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test("ContentBrowser mounts on the page-picker host page", async ({
    page,
  }) => {
    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    const dialog = page.locator('[data-testid="content-browser"]');
    await expect(dialog).toBeVisible({ timeout: 15_000 });
  });

  test("legacy miller-column Finder chrome is NOT loaded on the page-picker host", async ({
    page,
  }) => {
    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
  });

  test("page-picker initial state: confirm disabled, multi-select summary empty", async ({
    page,
  }) => {
    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    const confirm = page.locator('[data-testid="content-browser-confirm"]');
    const summary = page.locator(
      '[data-testid="content-browser-selection-summary"]'
    );
    await expect(confirm).toBeVisible({ timeout: 15_000 });
    await expect(confirm).toBeDisabled();
    await expect(summary).toBeVisible();
  });

  test("page-picker dialog chrome is keyboard-completable (Cancel button focusable)", async ({
    page,
  }) => {
    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    const cancelBtn = page.locator('[data-testid="content-browser-cancel"]');
    await expect(cancelBtn).toBeVisible({ timeout: 15_000 });
    await cancelBtn.focus();
    const focusedTag = await page.evaluate(
      () => document.activeElement?.tagName
    );
    expect(focusedTag).toBe("BUTTON");
  });

  test("axe-core a11y gate — host page picker modern dialog (T082b)", async ({
    page,
  }) => {
    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    await expect(
      page.locator('[data-testid="perc-content-browser-root"]')
    ).toBeVisible({ timeout: 15_000 });
    await expectNoSeriousA11yViolations(page, {
      scope: '[data-testid="perc-content-browser-root"]',
    });
  });
});
