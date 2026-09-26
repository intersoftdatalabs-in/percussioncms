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
 * Playwright: #4911 / parent #4530 — Content → Copy folder path.
 *
 * Tags: @explorer-copy-folder-path @explorer @folder @smoke
 *
 * npm run test:surface -- --path tests/explorer-copy-folder-path.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerCopyFolderPathUrl,
  isCopiedFolderPathStatus,
  isKnownExplorerCopyPathConsoleNoise,
} = require("./helpers/explorer-copy-folder-path");
const {
  openContentMenu,
  sitesTreeRootLocator,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-copy-folder-path", "@explorer", "@folder", "@smoke"];

async function openExplorer(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error" && !isKnownExplorerCopyPathConsoleNoise(msg.text())) {
      jsErrors.push(msg.text());
    }
  });
  await loginAsAdmin(page);
  await page.goto(explorerCopyFolderPathUrl(BASE_URL), { waitUntil: "domcontentloaded" });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 30_000,
  });
  return jsErrors;
}

test.describe("Explorer copy selected folder path (#4911 / #4530)", () => {
  test(
    "UI: confirm copies the Sites folder path and leaves the selection",
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
      page.once("dialog", async (dialog) => {
        expect(dialog.message()).toContain(before);
        await dialog.accept();
      });
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.copyFolderPath}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "success");
      const copied = await status.getAttribute("data-copied-path");
      expect(isCopiedFolderPathStatus("success", copied)).toBe(true);
      expect(copied).toBe(before);
      await expect(shell).toHaveAttribute("data-selected-folder-path", before);
      const clip = await page.evaluate(() => navigator.clipboard.readText());
      expect(clip).toBe(copied);
      await expectNoSeriousA11yViolations(page, `[data-testid="${TEST_IDS.shell}"]`);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: cancel does not copy and does not change the selection",
    { tag: TAGS },
    async ({ page, context }) => {
      test.setTimeout(120_000);
      await context.grantPermissions(["clipboard-read", "clipboard-write"]);
      const jsErrors = await openExplorer(page);
      const sites = sitesTreeRootLocator(page);
      await expect(sites.first()).toBeVisible({ timeout: 30_000 });
      await sites.first().click();
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      const before = await shell.getAttribute("data-selected-folder-path");
      await page.evaluate(() => navigator.clipboard.writeText("unchanged-sentinel"));
      page.once("dialog", (dialog) => dialog.dismiss());
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.copyFolderPath}"]`).click();
      await expect(page.locator(`[data-testid="${TEST_IDS.status}"]`)).toHaveCount(0);
      await expect(shell).toHaveAttribute("data-selected-folder-path", before);
      const clip = await page.evaluate(() => navigator.clipboard.readText());
      expect(clip).toBe("unchanged-sentinel");
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: clipboard failure is shown and the selection stays",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
      await page.evaluate(() => {
        Object.defineProperty(navigator, "clipboard", {
          configurable: true,
          value: {
            writeText: () => Promise.reject(new Error("denied")),
          },
        });
      });
      const sites = sitesTreeRootLocator(page);
      await expect(sites.first()).toBeVisible({ timeout: 30_000 });
      await sites.first().click();
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      const before = await shell.getAttribute("data-selected-folder-path");
      page.once("dialog", (dialog) => dialog.accept());
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.copyFolderPath}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-copied-path", before);
      await expect(shell).toHaveAttribute("data-selected-folder-path", before);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );
});
