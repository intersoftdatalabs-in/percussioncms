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
 * Developer site: delete a navigation section (#4920 / parent #1690).
 *
 * Surface-filtered QA:
 *   npm run test:surface -- --path tests/developer-site-nav-delete.spec.js
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

test.describe("Developer delete navigation section (#4920)", () => {
  test("cancel does not delete; confirm removes only that section", async ({ page }) => {
    test.setTimeout(180_000);
    const guards = attachConsoleGuards(page);
    let deletes = 0;
    page.on("request", (req) => {
      const url = req.url();
      if (
        req.method() === "DELETE" &&
        url.includes("/section/") &&
        !url.includes("convertToFolder") &&
        !url.includes("deleteSectionLink")
      ) {
        deletes += 1;
      }
    });
    await loginAsAdmin(page);
    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });
    const siteName = await openFirstEditableSite(page);

    const target = page.locator('[data-testid="developer-site-nav-delete-target"]');
    await expect(target.locator("option").first()).toBeAttached({ timeout: 20_000 });
    const optionCount = await target.locator("option").count();
    const leaf = target.locator("option").nth(optionCount - 1);
    const created = ((await leaf.textContent()) || "").trim();
    const optionValue = await leaf.getAttribute("value");
    expect(created.length).toBeGreaterThan(0);
    const keptOption = target.locator("option").nth(0);
    const kept = ((await keptOption.textContent()) || "").trim();
    expect(kept).not.toBe(created);
    await target.selectOption(optionValue);

    await page.locator('[data-testid="developer-site-nav-delete-cancel"]').click();
    expect(deletes).toBe(0);
    await expect(
      page.locator('[data-testid="developer-site-nav-item"]', { hasText: created }),
    ).toBeVisible();

    await page.locator('[data-testid="developer-site-nav-delete-confirm"]').click();
    const deleteError = page.locator('[data-testid="developer-site-nav-delete-error"]');
    const deleteNotice = page.locator('[data-testid="developer-site-nav-delete-notice"]');
    await expect(deleteNotice.or(deleteError)).toBeVisible({ timeout: 60_000 });
    if (await deleteError.isVisible()) {
      throw new Error(`delete failed: ${await deleteError.innerText()}`);
    }
    expect(deletes).toBe(1);
    await expect(
      page.locator('[data-testid="developer-site-nav-item"]', { hasText: created }),
    ).toHaveCount(0);
    await expect(
      page.locator('[data-testid="developer-site-nav-item"]', { hasText: kept }),
    ).toBeVisible();

    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    const again = page
      .locator('[data-testid="developer-site-open"]')
      .filter({ hasText: siteName })
      .first();
    await again.click();
    await expect(
      page.locator('[data-testid="developer-site-nav-item"]', { hasText: kept }),
    ).toBeVisible({ timeout: 20_000 });
    await expect(
      page.locator('[data-testid="developer-site-nav-item"]', { hasText: created }),
    ).toHaveCount(0);
    guards.assertClean();
  });
});
