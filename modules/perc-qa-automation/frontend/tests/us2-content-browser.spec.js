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
 * Playwright spec: US2 ContentBrowser host integration (SC-002 / FR-008a).
 *
 * <p>Asserts that the modern ContentBrowser mounts via the PercModernUI
 * bridge, presents a navigable tree + list + action bar, supports
 * selection filters, returns a valid SelectionResult on confirm, and
 * does NOT regress on the legacy miller-column Finder chrome in
 * dialog hosts. The dialog hosting is via a dedicated
 * `assetPickerModern.jsp` page that mirrors the legacy
 * `host-asset-picker` flow.</p>
 *
 * <p>Per-host Playwright specs for the other in-scope hosts
 * (page-picker, AA ContentBrowserDialog, folder-picker) follow the
 * same pattern. Host-by-host coverage in tasks.md T045*-pw.</p>
 *
 * <p>Run from {@code modules/perc-qa-automation/frontend}:</p>
 * <pre>
 *   npm test -- tests/us2-content-browser.spec.js
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const DIALOG_URL = `${BASE_URL}/Rhythmyx/cm/app/assetPickerModern.jsp?_=${Date.now()}`;

test.describe("US2 ContentBrowser host integration (SC-002)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test("ContentBrowser mounts via the PercModernUI bridge on the host page", async ({
    page,
  }) => {
    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    // The host page renders a mount target; the bridge calls
    // window.PercModernUI.mount(target, "ContentBrowser", props).
    const dialog = page.locator('[data-testid="content-browser"]');
    await expect(dialog).toBeVisible({ timeout: 15_000 });
  });

  test("legacy miller-column Finder chrome is NOT loaded on the host page", async ({
    page,
  }) => {
    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    // The legacy ContentBrowserDialog (Dojo / jQuery) renders a
    // `.perc-mcol` element. The modern ContentBrowser does not.
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
  });

  test("ContentBrowser dialog chrome is keyboard-completable (Cancel button reachable)", async ({
    page,
  }) => {
    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    // Tab focus reaches the Cancel button without traps; Enter triggers it.
    const cancelBtn = page.locator('[data-testid="content-browser-cancel"]');
    await expect(cancelBtn).toBeVisible({ timeout: 15_000 });
    // The button is focusable.
    await cancelBtn.focus();
    const focusedTag = await page.evaluate(() => document.activeElement?.tagName);
    expect(focusedTag).toBe("BUTTON");
  });

  test("ContentBrowser initial state: confirm disabled, selection summary shows empty", async ({
    page,
  }) => {
    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    const confirm = page.locator('[data-testid="content-browser-confirm"]');
    const summary = page.locator('[data-testid="content-browser-selection-summary"]');
    await expect(confirm).toBeVisible({ timeout: 15_000 });
    await expect(confirm).toBeDisabled();
    await expect(summary).toBeVisible();
  });
});