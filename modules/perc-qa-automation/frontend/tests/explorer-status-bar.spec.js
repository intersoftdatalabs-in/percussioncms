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

const TAGS = ["@explorer-status-bar", "@explorer", "@smoke"];

function stubFolderList(page) {
  return page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({
        PagedItemList: {
          childrenInPage: [
            {
              id: "42",
              name: "Home",
              title: "Home Page",
              path: "/Sites/Demo/Home",
              type: "percPage",
              category: "page",
              accessLevel: "WRITE",
              leaf: true,
            },
            {
              id: "43",
              name: "About",
              path: "/Sites/Demo/About",
              type: "percPage",
              category: "page",
              accessLevel: "WRITE",
              leaf: true,
            },
          ],
          childrenCount: 2,
          startIndex: 0,
        },
      }),
    });
  });
}

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
  await stubFolderList(page);
  await page.goto(explorerStatusBarUrl(BASE_URL), {
    waitUntil: "domcontentloaded",
  });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 30_000,
  });
  return jsErrors;
}

test.describe("Explorer status bar DCE parity (#6517 / #4530)", () => {
  test(
    "UI: status bar names the open folder, row counts, selection, and user",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = await openExplorer(page);

      const bar = page.locator(`[data-testid="${TEST_IDS.statusBar}"]`);
      await expect(bar).toBeVisible({ timeout: 15_000 });
      await expect(bar).toHaveAttribute("role", "contentinfo");

      // Folder segment: stubbed list reports two rows for the open folder.
      const folder = page.locator(`[data-testid="${TEST_IDS.folder}"]`);
      await expect(folder).toContainText(/2 items in/i, { timeout: 15_000 });

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

      // Selecting a row renames the selection segment without moving focus.
      const itemRow = page.locator(
        '[data-testid="detail-row-42"][data-row-kind="item"]',
      );
      await expect(itemRow).toBeVisible({ timeout: 20_000 });
      await itemRow.click();
      await expect(
        page.locator(
          '[data-testid="content-explorer-shell"][data-selected-item-id="42"]',
        ),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.selection}"]`),
      ).toContainText(/Home Page|1 item selected/i);

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
      await stubFolderList(page);
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

      const itemRow = page.locator(
        '[data-testid="detail-row-42"][data-row-kind="item"]',
      );
      await expect(itemRow).toBeVisible({ timeout: 20_000 });
      await itemRow.click();
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
