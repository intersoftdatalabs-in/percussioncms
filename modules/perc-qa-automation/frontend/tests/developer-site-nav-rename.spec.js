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
 * Developer site: rename a navigation section (#4919 / parent #1690).
 *
 * Surface-filtered QA:
 *   npm run test:surface -- --path tests/developer-site-nav-rename.spec.js
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

async function openFirstEditableSite(page) {
  const rows = page.locator('[data-testid^="developer-site-row-"]');
  const count = await rows.count();
  expect(count).toBeGreaterThan(0);
  for (let i = 0; i < count; i += 1) {
    const open = rows.nth(i).locator('[data-testid="developer-site-open"]');
    const siteName = ((await open.getAttribute("aria-label")) || "")
      .replace(/^Open\s+/, "")
      .trim();
    await open.click();
    await expect(page.locator('[data-testid="developer-site-detail"]')).toBeVisible({
      timeout: 20_000,
    });
    const add = page.locator('[data-testid="developer-site-nav-add"]');
    if (await add.isVisible().catch(() => false)) {
      return siteName;
    }
    const back = page.locator('[data-testid="developer-site-back"]');
    if (await back.isVisible().catch(() => false)) {
      await back.click();
    } else {
      await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    }
  }
  throw new Error("No traditional site with a navigation section editor");
}

test.describe("Developer rename navigation section (#4919)", () => {
  test("invalid name and cancel do not post; rename survives reload", async ({ page }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    let updates = 0;
    page.on("request", (req) => {
      if (req.method() === "POST" && req.url().includes("/section/update")) {
        updates += 1;
      }
    });
    await loginAsAdmin(page);
    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });
    const siteName = await openFirstEditableSite(page);

    const created = `NightSec${Date.now().toString().slice(-6)}`;
    await page.locator('[data-testid="developer-site-nav-name"]').fill(created);
    await page.locator('[data-testid="developer-site-nav-add"]').click();
    await expect(page.locator('[data-testid="developer-site-nav-notice"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(
      page.locator('[data-testid="developer-site-nav-item"]', { hasText: created }),
    ).toBeVisible();

    const target = page.locator('[data-testid="developer-site-nav-rename-target"]');
    const leaf = target.locator("option", { hasText: created });
    await expect(leaf).toHaveCount(1, { timeout: 20_000 });
    const optionValue = await leaf.first().getAttribute("value");
    await target.selectOption(optionValue);
    const original = created;
    const renamed = `NightRen${Date.now().toString().slice(-6)}`;

    const renameName = page.locator('[data-testid="developer-site-nav-rename-name"]');
    await expect(renameName).toHaveValue(original);
    await renameName.fill("!!!");
    await page.locator('[data-testid="developer-site-nav-rename-save"]').click();
    await expect(page.locator('[data-testid="developer-site-nav-rename-error"]')).toBeVisible();
    expect(updates).toBe(0);

    await renameName.fill(renamed);
    await page.locator('[data-testid="developer-site-nav-rename-cancel"]').click();
    await expect(renameName).toHaveValue(original);
    expect(updates).toBe(0);

    await renameName.fill(renamed);
    await page.locator('[data-testid="developer-site-nav-rename-save"]').click();
    const renameError = page.locator('[data-testid="developer-site-nav-rename-error"]');
    const renameNotice = page.locator('[data-testid="developer-site-nav-rename-notice"]');
    await expect(renameNotice.or(renameError)).toBeVisible({ timeout: 30_000 });
    if (await renameError.isVisible()) {
      throw new Error(`rename failed: ${await renameError.innerText()}`);
    }
    expect(updates).toBe(1);
    await expect(
      page.locator('[data-testid="developer-site-nav-item"]', { hasText: renamed }),
    ).toBeVisible();

    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    const again = page
      .locator('[data-testid="developer-site-open"]')
      .filter({ hasText: siteName })
      .first();
    await again.click();
    await expect(
      page.locator('[data-testid="developer-site-nav-item"]', { hasText: renamed }),
    ).toBeVisible({ timeout: 20_000 });

    await target.selectOption(optionValue);
    await renameName.fill(original);
    await page.locator('[data-testid="developer-site-nav-rename-save"]').click();
    await expect(renameNotice).toBeVisible({ timeout: 30_000 });
    guards.assertClean();
  });
});
