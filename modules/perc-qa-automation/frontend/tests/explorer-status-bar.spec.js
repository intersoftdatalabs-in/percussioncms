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
 * Playwright: #6517 / parent #4530 — Explorer DCE chrome-model parity
 * slice 1: status bar (selection/folder counts, operation feedback,
 * user/context readout) + Workbench visibility wiring regression.
 *
 * The spec uses only real H2 seed rows (no fake content ids) so the run
 * leaves no server.log ERROR/FATAL behind. The only stub is the publish
 * endpoint response in the failure-feed test (fulfilled in-page, the
 * server is never hit).
 *
 * Tags: @explorer-status-bar @explorer @smoke
 *
 * npm run test:surface -- --path tests/explorer-status-bar.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerStatusBarUrl,
  isKnownExplorerStatusBarConsoleNoise,
} = require("./helpers/explorer-status-bar");
const {
  sitesTreeRootLocator,
} = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-status-bar", "@explorer", "@smoke"];

async function openExplorer(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (
      msg.type() === "error" &&
      !isKnownExplorerStatusBarConsoleNoise(msg.text())
    ) {
      jsErrors.push(msg.text());
    }
  });
  await loginAsAdmin(page);
  await page.goto(explorerStatusBarUrl(BASE_URL), {
    waitUntil: "domcontentloaded",
  });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 30_000,
  });
  return jsErrors;
}

async function openSitesFolder(page) {
  const sites = sitesTreeRootLocator(page);
  await expect(sites.first()).toBeVisible({ timeout: 30_000 });
  await sites.first().click();
  const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
  await expect(shell).toHaveAttribute("data-selected-folder-path", /Sites/, {
    timeout: 15_000,
  });
  return shell;
}

test.describe("Explorer status bar DCE parity (#6517 / #4530)", () => {
  test(
    "UI: status bar names the open folder, row counts, selection, and user",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);
      const shell = await openSitesFolder(page);

      const bar = page.locator(`[data-testid="${TEST_IDS.statusBar}"]`);
      await expect(bar).toBeVisible({ timeout: 15_000 });
      await expect(bar).toHaveAttribute("role", "contentinfo");

      // Folder segment names the open folder with its live row count.
      const folder = page.locator(`[data-testid="${TEST_IDS.folder}"]`);
      await expect(folder).toContainText(/\/Sites/, { timeout: 15_000 });
      await expect(folder).toContainText(/items in|No items in/, {
        timeout: 15_000,
      });

      // No selection yet: idle selection + idle operation states.
      await expect(
        page.locator(`[data-testid="${TEST_IDS.selection}"]`),
      ).toContainText(/No selection/i);
      const operation = page.locator(
        `[data-testid="${TEST_IDS.operation}"]`,
      );
      await expect(operation).toHaveAttribute("role", "status");
      await expect(operation).toHaveAttribute("aria-live", "polite");
      await expect(operation).toHaveAttribute("data-kind", "idle");

      // User/context readout names the signed-in operator.
      await expect(
        page.locator(`[data-testid="${TEST_IDS.context}"]`),
      ).toContainText(/Signed in as/i);

      // Selecting a real row renames the selection segment.
      const firstRow = page.locator('[data-testid^="detail-row-"]').first();
      await expect(firstRow).toBeVisible({ timeout: 20_000 });
      const rowId = await firstRow.getAttribute("data-item-id");
      await firstRow.click();
      await expect(shell).toHaveAttribute("data-selected-item-id", rowId, {
        timeout: 10_000,
      });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.selection}"]`),
      ).toContainText(/Selected:|1 item selected/i);

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: operation segment mirrors a failed menu outcome as an error",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("dialog", (dialog) => {
        void dialog.accept();
      });
      await loginAsAdmin(page);
      // Fulfilled in-page: the CMS server is never hit, so no
      // server.log entries are produced by this failure path.
      await page.route("**/services/sitemanage/publish/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            status: "FORBIDDEN",
            warningMessage: "Publication stopped because of licensing issues",
          }),
        });
      });
      await page.goto(explorerStatusBarUrl(BASE_URL), {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 30_000 });
      const shell = await openSitesFolder(page);

      const firstRow = page.locator(
        '[data-testid^="detail-row-"][data-row-kind="item"]',
      ).first();
      await expect(firstRow).toBeVisible({ timeout: 20_000 });
      const rowId = await firstRow.getAttribute("data-item-id");
      await firstRow.click();
      await expect(shell).toHaveAttribute("data-selected-item-id", rowId, {
        timeout: 10_000,
      });
      const publishNow = page.locator(
        '[data-testid="action-toolbar-item-Publish_Now"], [data-testid="action-toolbar-item-publish_now"]',
      );
      await expect(publishNow.first()).toBeVisible({ timeout: 15_000 });
      await publishNow.first().click();

      const operation = page.locator(
        `[data-testid="${TEST_IDS.operation}"]`,
      );
      await expect(operation).toHaveAttribute("data-kind", "error", {
        timeout: 15_000,
      });
      await expect(operation).toContainText(
        /FORBIDDEN|licensing|Publication stopped/i,
      );

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );
});
