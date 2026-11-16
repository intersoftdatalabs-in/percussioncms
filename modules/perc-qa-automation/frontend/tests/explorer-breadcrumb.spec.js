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
 * Playwright: #6468 / parent #4530 — Explorer breadcrumb wayfinding polish:
 * overflow + deep-link copy.
 *
 * Tags: @explorer-breadcrumb @explorer @smoke
 *
 * npm run test:surface -- --path tests/explorer-breadcrumb.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerBreadcrumbUrl,
  isKnownExplorerBreadcrumbConsoleNoise,
} = require("./helpers/explorer-breadcrumb");
const {
  sitesTreeRootLocator,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-breadcrumb", "@explorer", "@smoke"];

async function openExplorer(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error" && !isKnownExplorerBreadcrumbConsoleNoise(msg.text())) {
      jsErrors.push(msg.text());
    }
  });
  await loginAsAdmin(page);
  await page.goto(explorerBreadcrumbUrl(BASE_URL), { waitUntil: "domcontentloaded" });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 30_000,
  });
  return jsErrors;
}

test.describe("Explorer breadcrumb wayfinding (#6468 / #4530)", () => {
  test(
    "UI: breadcrumb shows the open folder and steps back to an ancestor",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
      const sites = sitesTreeRootLocator(page);
      await expect(sites.first()).toBeVisible({ timeout: 30_000 });
      await sites.first().click();
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toHaveAttribute("data-selected-folder-path", /Sites/);
      const crumb = page.locator(`[data-testid="${TEST_IDS.breadcrumb}"]`);
      await expect(crumb).toBeVisible();
      const fullPath = await crumb.getAttribute("data-full-path");
      expect(fullPath).toMatch(/Sites/);
      const before = await shell.getAttribute("data-selected-folder-path");
      expect(fullPath).toBe(before);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.current}"]`),
      ).toBeVisible();
      await expectNoSeriousA11yViolations(page, `[data-testid="${TEST_IDS.shell}"]`);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: copy folder link writes the Explorer deep link and keeps the selection",
    { tag: TAGS },
    async ({ page, context }) => {
      test.setTimeout(120_000);
      await context.grantPermissions(["clipboard-read", "clipboard-write"]);
      const jsErrors = await openExplorer(page);
      const sites = sitesTreeRootLocator(page);
      await expect(sites.first()).toBeVisible({ timeout: 30_000 });
      await sites.first().click();
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toHaveAttribute("data-selected-folder-path", /Sites/);
      const before = await shell.getAttribute("data-selected-folder-path");
      await page.locator(`[data-testid="${TEST_IDS.copyLink}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.copyStatus}"]`);
      await expect(status).toHaveAttribute("data-kind", "success");
      await expect(shell).toHaveAttribute("data-selected-folder-path", before);
      const clip = await page.evaluate(() => navigator.clipboard.readText());
      expect(clip).toContain("explorer");
      expect(clip).toContain(`path=${encodeURIComponent(before)}`);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: home crumb returns to the content root",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
      const sites = sitesTreeRootLocator(page);
      await expect(sites.first()).toBeVisible({ timeout: 30_000 });
      await sites.first().click();
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toHaveAttribute("data-selected-folder-path", /Sites/);
      await page.locator(`[data-testid="${TEST_IDS.home}"]`).click();
      await expect(shell).toHaveAttribute("data-selected-folder-path", "/");
      await expect(page.locator(`[data-testid="${TEST_IDS.breadcrumb}"]`)).toHaveAttribute(
        "data-full-path",
        "",
      );
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );
});
