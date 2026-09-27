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
 * Developer site: reorder a navigation section (#4956 / parent #1690).
 *
 * Surface-filtered QA:
 *   npm run test:surface -- --path tests/developer-site-nav-reorder.spec.js
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

async function itemIndex(page, name) {
  const texts = await page.locator('[data-testid="developer-site-nav-item"]').allTextContents();
  return texts.map((t) => t.trim()).indexOf(name);
}

test.describe("Developer reorder navigation section (#4956)", () => {
  test("cancel does not move; move up swaps siblings and survives reload", async ({ page }) => {
    test.setTimeout(240_000);
    const guards = attachConsoleGuards(page);
    let moves = 0;
    page.on("request", (req) => {
      if (req.method() === "POST" && req.url().includes("/section/move")) {
        moves += 1;
      }
    });
    await loginAsAdmin(page);
    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });
    const siteName = await openFirstEditableSite(page);

    const target = page.locator('[data-testid="developer-site-nav-reorder-target"]');
    await expect(target.locator("option").nth(1)).toBeAttached({ timeout: 20_000 });
    const first = ((await target.locator("option").nth(0).textContent()) || "").trim();
    const second = ((await target.locator("option").nth(1).textContent()) || "").trim();
    expect(first.length).toBeGreaterThan(0);
    expect(second.length).toBeGreaterThan(0);
    expect(second).not.toBe(first);
    await target.selectOption({ label: second });
    await page.locator('[data-testid="developer-site-nav-reorder-cancel"]').click();
    expect(moves).toBe(0);
    const beforeFirst = await itemIndex(page, first);
    const beforeSecond = await itemIndex(page, second);
    expect(beforeSecond).toBeGreaterThan(beforeFirst);

    await page.locator('[data-testid="developer-site-nav-move-up"]').click();
    const reorderError = page.locator('[data-testid="developer-site-nav-reorder-error"]');
    const reorderNotice = page.locator('[data-testid="developer-site-nav-reorder-notice"]');
    await expect(reorderNotice.or(reorderError)).toBeVisible({ timeout: 60_000 });
    if (await reorderError.isVisible()) {
      throw new Error(`reorder failed: ${await reorderError.innerText()}`);
    }
    expect(moves).toBe(1);
    expect(await itemIndex(page, second)).toBeLessThan(await itemIndex(page, first));

    await page.goto(developerSitesUrl(), { waitUntil: "networkidle" });
    await page
      .locator('[data-testid="developer-site-open"]')
      .filter({ hasText: siteName })
      .first()
      .click();
    await expect(
      page.locator('[data-testid="developer-site-nav-item"]', { hasText: second }),
    ).toBeVisible({ timeout: 20_000 });
    expect(await itemIndex(page, second)).toBeLessThan(await itemIndex(page, first));
    guards.assertClean();
  });
});
