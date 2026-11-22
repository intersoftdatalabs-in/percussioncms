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
 * Explorer dialogs share one focus trap (#7003, parent standalone).
 *
 * <p>All 23 hand-rolled contentExplorer Tab traps were consolidated onto
 * {@code useDialogFocusTrap} keeping each dialog's initial-focus choice.
 * This spec proves the shared behaviour live: initial focus, Tab cycling
 * inside the dialog, and Escape closing with focus back on the invoking
 * toolbar control — for a text-first dialog (check-in comment) and a
 * button-first dialog (Publishing History).</p>
 *
 * <p>Tags: {@code @explorer} {@code @explorer-dialog} {@code @a11y}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-dialog-focus-trap.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");

const PAGE_LIST = {
  PagedItemList: {
    childrenInPage: [
      {
        id: "42",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
        category: "page",
        accessLevel: "WRITE",
        leaf: true,
      },
    ],
    childrenCount: 1,
    startIndex: 0,
  },
};

function ownerBody(checkOutUser) {
  return JSON.stringify({
    EditorItemLockInfo: {
      itemName: "Home",
      checkOutUser,
      currentUser: "Admin",
      assignmentType: "Reader",
    },
  });
}

async function stubListing(page) {
  await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify(PAGE_LIST),
    });
  });
  await page.route("**/rest/editor/items/**/checkout-owner", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: ownerBody("editor"),
    });
  });
}

async function selectHomeRow(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
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
}

/**
 * @param {import('@playwright/test').Page} page
 * @param {string} dialogTestId
 */
async function expectFocusInsideDialog(page, dialogTestId) {
  const inside = await page.evaluate((testId) => {
    const root = document.querySelector(`[data-testid="${testId}"]`);
    return !!root && !!root.contains(document.activeElement);
  }, dialogTestId);
  expect(inside).toBe(true);
}

test.describe("Explorer dialog focus trap (#7003)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "check-in comment traps Tab and returns focus on Escape",
    { tag: ["@explorer", "@explorer-dialog", "@a11y"] },
    async ({ page }) => {
      const pageErrors = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() !== "error") {
          return;
        }
        if (/Failed to load resource/i.test(msg.text())) {
          return;
        }
        consoleErrors.push(msg.text());
      });
      await stubListing(page);
      await page.route("**/rest/editor/items/**/checkin**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: ownerBody(""),
        });
      });

      await selectHomeRow(page);
      const checkIn = page.locator(
        '[data-testid="action-toolbar-item-Check_In"]',
      );
      await expect(checkIn).toBeVisible({ timeout: 15_000 });
      await checkIn.click();

      const dialog = page.locator('[data-testid="explorer-checkin-comment"]');
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      // Text-first dialog: initial focus lands in the comment box.
      await expect(
        page.locator('[data-testid="explorer-checkin-comment-input"]'),
      ).toBeFocused({ timeout: 10_000 });

      // Tab cycles inside the dialog (wrap, never leaks to the shell).
      for (let i = 0; i < 6; i += 1) {
        await page.keyboard.press("Tab");
        await expectFocusInsideDialog(page, "explorer-checkin-comment");
      }
      // Shift+Tab from the first control wraps to the last.
      await page
        .locator('[data-testid="explorer-checkin-comment-input"]')
        .focus();
      await page.keyboard.press("Shift+Tab");
      await expectFocusInsideDialog(page, "explorer-checkin-comment");

      await page.screenshot({
        path: "test-results/live-proof-7003-checkin-trap.png",
      });

      // Escape cancels and returns focus to the invoking toolbar control.
      await page.keyboard.press("Escape");
      await expect(dialog).toBeHidden({ timeout: 10_000 });
      await expect(checkIn).toBeFocused({ timeout: 10_000 });

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
      expect(
        consoleErrors,
        `console errors: ${consoleErrors.join(" | ")}`,
      ).toEqual([]);
    },
  );

  test(
    "publishing history traps Tab and returns focus on Escape",
    { tag: ["@explorer", "@explorer-dialog", "@a11y"] },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await stubListing(page);
      await page.route("**/itemmanagement/item/pubhistory/**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ ItemPublishingHistory: [] }),
        });
      });

      await selectHomeRow(page);
      const history = page.locator(
        '[data-testid="action-toolbar-item-Publishing_History"]',
      );
      await expect(history).toBeVisible({ timeout: 15_000 });
      await history.click();

      const dialog = page.locator(
        '[data-testid="explorer-publishing-history-dialog"]',
      );
      await expect(dialog).toBeVisible({ timeout: 10_000 });
      // Button-first dialog: initial focus lands on the close button.
      await expect(
        page.locator('[data-testid="explorer-publishing-history-close"]'),
      ).toBeFocused({ timeout: 10_000 });

      for (let i = 0; i < 6; i += 1) {
        await page.keyboard.press("Tab");
        await expectFocusInsideDialog(
          page,
          "explorer-publishing-history-dialog",
        );
      }

      await page.screenshot({
        path: "test-results/live-proof-7003-history-trap.png",
      });

      await page.keyboard.press("Escape");
      await expect(dialog).toBeHidden({ timeout: 10_000 });
      await expect(history).toBeFocused({ timeout: 10_000 });

      expect(pageErrors, `uncaught pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
