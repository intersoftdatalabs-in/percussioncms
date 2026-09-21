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
 * Playwright surface: #4674 / parent #4530 — Create Site name validation
 * and HTTP 400/403 mapping (no live site write).
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/explorer-site-create-errors.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerSpaUrl,
  openContentMenu,
  uniqueQaSiteName,
  createSiteMissingSkipReason,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-sites-list-create");

/**
 * @param {import("@playwright/test").Page} page
 * @returns {Promise<boolean>}
 */
async function openExplorerCreateSiteOrSkip(page) {
  const errors = [];
  page.on("pageerror", (err) => errors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      const t = msg.text();
      if (!isKnownExplorerSitesConsoleNoise(t)) {
        errors.push(t);
      }
    }
  });
  page._createSiteErrors = errors;
  await loginAsAdmin(page);
  await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });
  await expect(
    page.locator(`[data-testid="${TEST_IDS.shell}"]`),
  ).toBeVisible({ timeout: 20_000 });
  await openContentMenu(page);
  const menuItem = page.locator(`[data-testid="${TEST_IDS.createSiteMenu}"]`);
  if ((await menuItem.count()) === 0) {
    test.skip(createSiteMissingSkipReason());
    return false;
  }
  await menuItem.click();
  await expect(
    page.locator(`[data-testid="${TEST_IDS.wizard}"]`),
  ).toBeVisible({ timeout: 10_000 });
  return true;
}

test.describe("Create Site validation and 400/403 (#4674)", () => {
  test(
    "name required: Next stays disabled until a valid name",
    { tag: ["@explorer-site-create-errors", "@explorer", "@sites"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const ok = await openExplorerCreateSiteOrSkip(page);
      if (!ok) {
        return;
      }
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepDetails}"]`),
      ).toBeVisible();
      const next = page.locator(`[data-testid="${TEST_IDS.next}"]`);
      await expect(next).toBeDisabled();
      await page.locator(`[data-testid="${TEST_IDS.siteName}"]`).fill(" ");
      await expect(next).toBeDisabled();
      await page
        .locator(`[data-testid="${TEST_IDS.siteName}"]`)
        .fill(uniqueQaSiteName("QaCreate"));
      await expect(next).toBeEnabled();
      expect(page._createSiteErrors || []).toEqual([]);
    },
  );

  test(
    "POST 403 stays on wizard with permission chrome",
    { tag: ["@explorer-site-create-errors", "@explorer", "@sites"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const ok = await openExplorerCreateSiteOrSkip(page);
      if (!ok) {
        return;
      }
      await page.route("**/sitemanage/site/**", async (route) => {
        if (route.request().method() === "POST") {
          await route.fulfill({
            status: 403,
            contentType: "application/json",
            body: JSON.stringify({ message: "Forbidden" }),
          });
          return;
        }
        await route.continue();
      });
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();
      await page
        .locator(`[data-testid="${TEST_IDS.siteName}"]`)
        .fill(uniqueQaSiteName("QaDenied"));
      const next = page.locator(`[data-testid="${TEST_IDS.next}"]`);
      await expect(next).toBeEnabled();
      await next.click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepConfirm}"]`),
      ).toBeVisible();
      await next.click();
      await page.locator(`[data-testid="${TEST_IDS.run}"]`).click();
      const progress = page.locator(`[data-testid="${TEST_IDS.progress}"]`);
      await expect(progress).toBeVisible({ timeout: 15_000 });
      await expect(progress).toContainText(/permission/i);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.createSitePanel}"]`),
      ).toBeVisible();
      expect(page._createSiteErrors || []).toEqual([]);
    },
  );

  test(
    "POST 400 stays on wizard with mapped chrome",
    { tag: ["@explorer-site-create-errors", "@explorer", "@sites"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const ok = await openExplorerCreateSiteOrSkip(page);
      if (!ok) {
        return;
      }
      await page.route("**/sitemanage/site/**", async (route) => {
        if (route.request().method() === "POST") {
          await route.fulfill({
            status: 400,
            contentType: "application/json",
            body: JSON.stringify({ message: "NavTree already exists" }),
          });
          return;
        }
        await route.continue();
      });
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();
      await page
        .locator(`[data-testid="${TEST_IDS.siteName}"]`)
        .fill(uniqueQaSiteName("QaBad"));
      const next = page.locator(`[data-testid="${TEST_IDS.next}"]`);
      await next.click();
      await next.click();
      await page.locator(`[data-testid="${TEST_IDS.run}"]`).click();
      const progress = page.locator(`[data-testid="${TEST_IDS.progress}"]`);
      await expect(progress).toBeVisible({ timeout: 15_000 });
      await expect(progress).toContainText(/NavTree already exists/i);
      expect(page._createSiteErrors || []).toEqual([]);
    },
  );
});
