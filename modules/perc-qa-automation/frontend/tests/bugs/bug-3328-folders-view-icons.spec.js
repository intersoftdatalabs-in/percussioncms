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
 * GH-3328 — Folders detail list shows folder icons, not checkboxes as the
 * browse affordance.
 *
 * Run after perc-devctl qa-up:
 *   npm run test:surface -- --path tests/bugs/bug-3328-folders-view-icons.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const { expectNoSeriousA11yViolations } = require("../helpers/a11y");

const EXPLORER_URL = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;

test.describe("GH-3328 Folders view folder icons", () => {
  test(
    "Folders children show folder icons beside checkboxes",
    { tag: ["@explorer", "@folder-icons", "@smoke"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      const consoleErrors = [];
      page.on("pageerror", (err) => {
        consoleErrors.push(String(err && err.message ? err.message : err));
      });
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);
      await page.goto(EXPLORER_URL, { waitUntil: "networkidle" });
      const shell = page.locator('[data-testid="content-explorer-shell"]');
      await expect(shell).toBeVisible({ timeout: 15_000 });
      await expect(page.locator('[data-testid="detail-list"]')).toBeVisible();

      const foldersNode = page.locator(
        '[data-testid="tree-node-/Folders/"], [data-testid="tree-node-/Folders"]',
      );
      if ((await foldersNode.count()) === 0) {
        test.info().annotations.push({
          type: "note",
          description: "No /Folders tree node on this fixture; chrome-only icon column",
        });
        await expect(
          page.locator('[data-testid="detail-col-header-icon"]'),
        ).toBeVisible();
        return;
      }

      await foldersNode.first().click();
      await expect(page.locator('[data-testid="detail-list"]')).toBeVisible();
      await expect(
        page.locator('[data-testid="detail-col-header-icon"]'),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="detail-col-header-select"]'),
      ).toBeVisible();

      const folderRows = page.locator('[data-testid^="detail-row-"][data-row-kind="folder"]');
      const folderIcons = page.locator('[data-testid^="detail-folder-icon-"]');
      if ((await folderRows.count()) === 0) {
        test.info().annotations.push({
          type: "note",
          description: "Folders list empty on this fixture; icon header asserted",
        });
        return;
      }

      await expect(folderIcons.first()).toBeVisible();
      await expect(folderIcons.first()).toHaveAttribute("data-kind", "folder");
      await expect(folderIcons.first()).toHaveAttribute(
        "data-folder-state",
        /closed|open/,
      );

      const firstFolderRow = folderRows.first();
      const rowCheckbox = firstFolderRow.locator('input[type="checkbox"]');
      await expect(rowCheckbox).toBeVisible();
      await expect(firstFolderRow.locator('[data-testid^="detail-folder-icon-"]')).toBeVisible();

      const beforePath = await page
        .locator('[data-testid="detail-list"]')
        .getAttribute("data-folder-path")
        .catch(() => null);
      await firstFolderRow.locator('[data-testid^="detail-folder-icon-"]').click();
      await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible();
      await expect(page.locator('[data-testid="detail-list"]')).toBeVisible();
      await expect(page).not.toHaveURL(/view=editor/);
      if (beforePath) {
        await expect
          .poll(async () =>
            page.locator('[data-testid="detail-list"]').getAttribute("data-folder-path"),
          )
          .not.toBe(beforePath);
      }

      const unexpected = consoleErrors.filter(
        (t) =>
          !/favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
            t,
          ),
      );
      expect(unexpected, `console/page errors: ${unexpected.join(" | ")}`).toEqual(
        [],
      );
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
    },
  );
});
