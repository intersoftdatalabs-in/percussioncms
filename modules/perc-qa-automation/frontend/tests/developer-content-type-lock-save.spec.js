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
 * Developer Content Type detail lock / save / unlock chrome (#3744 / #3772 / parent #1690).
 *
 * Admin locks a type, saves a description, then unlocks. Surface-filtered QA:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-content-type-lock-save.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { catalogRowSelector } = require("./helpers/developer-catalog-selectors");

const MARKER = " [#3744-lock-save]";

function developerContentTypesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "content-types",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Developer content type lock/save chrome (#3744 / #3772)", () => {
  test("Admin can lock, save a description, and unlock", async ({ page }) => {
    test.setTimeout(120_000);
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

    await loginAsAdmin(page);
    await page.goto(developerContentTypesUrl(), { waitUntil: "networkidle" });

    await expect(page.locator('[data-testid="nav-developer"]')).toBeVisible({
      timeout: 20_000,
    });

    const panel = page.locator('[data-testid="developer-ct-panel"]');
    const empty = page.locator('[data-testid="developer-ct-empty"]');
    const listError = page.locator('[data-testid="developer-ct-error"]');
    await expect(panel.or(empty).or(listError).first()).toBeVisible({
      timeout: 30_000,
    });

    if (await listError.isVisible()) {
      throw new Error(
        `Developer content types catalog error: ${(await listError.innerText()).trim()}`,
      );
    }
    if (await empty.isVisible()) {
      test.skip(true, "No content types in catalog — cannot exercise lock/save");
    }

    const table = page.locator('[data-testid="developer-ct-table"]');
    await expect(table).toBeVisible({ timeout: 15_000 });
    const named = table.locator('[data-testid^="developer-ct-row-"]').filter({
      hasText: /percPage/,
    });
    const targetRow =
      (await named.count()) > 0
        ? named.first()
        : page.locator(catalogRowSelector("developer-ct-row", 0));
    await expect(targetRow).toBeVisible();
    const openBtn = targetRow.locator('button[aria-label^="Open "]');
    if (await openBtn.count()) {
      await openBtn.click();
    } else {
      await targetRow.click();
    }

    const detail = page.locator('[data-testid="developer-ct-detail"]');
    const detailError = page.locator('[data-testid="developer-ct-detail-error"]');
    await expect(detail.or(detailError).first()).toBeVisible({ timeout: 30_000 });
    if (await detailError.isVisible()) {
      throw new Error(
        `Content type detail error: ${(await detailError.innerText()).trim()}`,
      );
    }

    const desc = page.locator('[data-testid="developer-ct-description"]');
    const lockBtn = page.locator('[data-testid="developer-ct-lock"]');
    const saveBtn = page.locator('[data-testid="developer-ct-save"]');
    const unlockBtn = page.locator('[data-testid="developer-ct-unlock"]');
    const status = page.locator('[data-testid="developer-ct-lock-status"]');

    await expect(page.locator('[data-testid="developer-ct-lock-toolbar"]')).toBeVisible();
    await expect(lockBtn).toBeEnabled();
    await expect(saveBtn).toBeDisabled();
    await expect(unlockBtn).toBeDisabled();
    await expect(desc).toBeDisabled();
    await expect(status).toHaveText(/Not locked/i);

    await lockBtn.click();
    await expect(status).toHaveText(/Locked by you/i, { timeout: 20_000 });
    await expect(desc).toBeEnabled();
    await expect(unlockBtn).toBeEnabled();

    const original = (await desc.inputValue()).replace(MARKER, "");
    await desc.fill(`${original}${MARKER}`);
    await expect(saveBtn).toBeEnabled();
    await saveBtn.click();
    const notice = page.locator('[data-testid="developer-ct-detail-notice"]');
    const saveError = page.locator('[data-testid="developer-ct-detail-error"]');
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Save failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(notice).toContainText(/saved/i);

    await desc.fill(original);
    await saveBtn.click();
    await expect(notice.or(saveError).first()).toBeVisible({ timeout: 20_000 });
    if (await saveError.isVisible()) {
      throw new Error(`Restore save failed: ${(await saveError.innerText()).trim()}`);
    }
    await expect(notice).toContainText(/saved/i);

    await unlockBtn.click();
    await expect(status).toHaveText(/Not locked/i, { timeout: 20_000 });
    await expect(desc).toBeDisabled();
    await expect(saveBtn).toBeDisabled();
    await expect(lockBtn).toBeEnabled();

    expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
    const unexpectedConsole = consoleErrors.filter(
      (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
    );
    expect(
      unexpectedConsole,
      `console error: ${unexpectedConsole.join(" | ")}`,
    ).toEqual([]);
  });
});
