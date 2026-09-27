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
 * Developer site: edit one external navigation link URL (#4985 / parent #1690).
 *
 * Surface-filtered QA:
 *   npm run test:surface -- --path tests/developer-site-nav-external-link-edit.spec.js
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
    const add = page.locator('[data-testid="developer-site-nav-ext-add"]');
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
  throw new Error("No traditional site with an external-link control");
}

test.describe("Developer edit external navigation link (#4985)", () => {
  test("cancel does not post; saved https URL shows after reload", async ({ page }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    let updates = 0;
    page.on("request", (req) => {
      if (req.method() === "POST" && req.url().includes("/section/updateExternalLink/")) {
        updates += 1;
      }
    });
    await loginAsAdmin(page);
    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });
    const siteName = await openFirstEditableSite(page);

    const unique = `Ext${Date.now().toString().slice(-6)}`;
    const createdUrl = `https://example.com/${unique}`;
    const editedUrl = `https://example.com/${unique}-edit`;
    await page.locator('[data-testid="developer-site-nav-ext-title"]').fill(unique);
    await page.locator('[data-testid="developer-site-nav-ext-url"]').fill(createdUrl);
    await page.locator('[data-testid="developer-site-nav-ext-add"]').click();
    await expect(page.locator('[data-testid="developer-site-nav-ext-notice"]')).toBeVisible({
      timeout: 30_000,
    });

    const target = page.locator('[data-testid="developer-site-nav-ext-edit-target"]');
    await expect(target.locator("option", { hasText: unique })).toHaveCount(1, {
      timeout: 20_000,
    });
    await target.selectOption({ label: unique });
    const editUrl = page.locator('[data-testid="developer-site-nav-ext-edit-url"]');
    await expect(editUrl).toHaveValue(createdUrl, { timeout: 20_000 });

    await editUrl.fill("not a url");
    await page.locator('[data-testid="developer-site-nav-ext-edit-save"]').click();
    await expect(page.locator('[data-testid="developer-site-nav-ext-edit-error"]')).toBeVisible();
    expect(updates).toBe(0);

    await editUrl.fill(editedUrl);
    await page.locator('[data-testid="developer-site-nav-ext-edit-cancel"]').click();
    await expect(editUrl).toHaveValue(createdUrl);
    expect(updates).toBe(0);

    await editUrl.fill(editedUrl);
    await page.locator('[data-testid="developer-site-nav-ext-edit-window"]').selectOption("_blank");
    await page.locator('[data-testid="developer-site-nav-ext-edit-save"]').click();
    await expect(page.locator('[data-testid="developer-site-nav-ext-edit-notice"]')).toBeVisible({
      timeout: 30_000,
    });
    expect(updates).toBe(1);
    await expect(page.locator('[data-testid="developer-site-nav-item-url"]', { hasText: editedUrl })).toBeVisible();

    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    const again = page
      .locator('[data-testid="developer-site-open"]')
      .filter({ hasText: siteName })
      .first();
    await again.click();
    await expect(
      page.locator('[data-testid="developer-site-nav-item-url"]', { hasText: editedUrl }),
    ).toBeVisible({ timeout: 20_000 });
    guards.assertClean();
  });
});
