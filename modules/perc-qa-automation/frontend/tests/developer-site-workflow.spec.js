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
 * Developer site workflow association (#4892 / parent #1690).
 *
 * Surface-filtered QA:
 *   npm run test:surface -- --path tests/developer-site-workflow.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function developerSitesUrl() {
  const q = new URLSearchParams({
    entry: "developer",
    section: "sites",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
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
  return {
    assertClean() {
      expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      const unexpectedConsole = consoleErrors.filter(
        (t) => !/Failed to load resource/i.test(t) && !/favicon/i.test(t),
      );
      expect(
        unexpectedConsole,
        `console error: ${unexpectedConsole.join(" | ")}`,
      ).toEqual([]);
    },
  };
}

test.describe("Developer site workflow (#4892)", () => {
  test("Admin can set a site workflow and see it after reload", async ({ page }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    await loginAsAdmin(page);
    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const row = page.locator('[data-testid^="developer-site-row-"]').first();
    await expect(row).toBeVisible({ timeout: 20_000 });
    const open = row.locator('[data-testid="developer-site-open"]');
    const siteName = ((await open.getAttribute("aria-label")) || "")
      .replace(/^Open\s+/, "")
      .trim();
    expect(siteName.length).toBeGreaterThan(0);
    await open.click();
    await expect(page.locator('[data-testid="developer-site-detail"]')).toBeVisible({
      timeout: 20_000,
    });

    const select = page.locator('[data-testid="developer-site-workflow"]');
    await expect(select.locator("option")).not.toHaveCount(1, { timeout: 20_000 });
    const options = await select.locator("option").allTextContents();
    const choice = options.map((t) => t.trim()).find((t) => t && t !== "Select a workflow");
    expect(choice, `workflow options: ${options.join(" | ")}`).toBeTruthy();
    await select.selectOption({ label: choice });
    await page.locator('[data-testid="developer-site-save"]').click();
    await expect(page.locator('[data-testid="developer-site-save-notice"]')).toBeVisible({
      timeout: 20_000,
    });

    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    const again = page
      .locator('[data-testid="developer-site-open"]')
      .filter({ hasText: siteName })
      .first();
    await again.click();
    await expect(page.locator('[data-testid="developer-site-workflow"]')).toHaveValue(choice, {
      timeout: 20_000,
    });
    guards.assertClean();
  });
});
