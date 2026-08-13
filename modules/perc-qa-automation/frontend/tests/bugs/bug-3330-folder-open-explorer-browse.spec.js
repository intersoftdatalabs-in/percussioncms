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
 * GH-3330 / parent #3329 — folder open stays in Explorer browse.
 *
 * Opening a folder (tree or detail row) must list children in Content
 * Explorer. It must not navigate to {@code view=editor} or call
 * itemmanagement {@code getTransitions} / workflow load for {@code -1}.
 *
 * Run after perc-devctl qa-up:
 *   npm run test:surface -- --path tests/bugs/bug-3330-folder-open-explorer-browse.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const { expectNoSeriousA11yViolations } = require("../helpers/a11y");

const EXPLORER_URL = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;

function isWorkflowLoadUrl(url) {
  const u = String(url || "");
  return (
    u.includes("itemmanagement/workflow") ||
    u.includes("getTransitions") ||
    /workflow[/=]-1/.test(u)
  );
}

test.describe("GH-3330 folder open stays in Explorer browse", () => {
  test(
    "opening Folders stays on Explorer and does not hit workflow -1",
    { tag: ["@explorer", "@folder-browse", "@smoke"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      const workflowHits = [];
      const consoleErrors = [];
      page.on("pageerror", (err) => {
        consoleErrors.push(String(err && err.message ? err.message : err));
      });
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });
      page.on("request", (req) => {
        const url = req.url();
        if (isWorkflowLoadUrl(url)) {
          workflowHits.push(url);
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
          description: "No /Folders tree node on this fixture; chrome-only assert",
        });
        await expect(page).not.toHaveURL(/view=editor/);
        return;
      }
      await foldersNode.first().click();
      await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible();
      await expect(page.locator('[data-testid="detail-list"]')).toBeVisible();
      await expect(page).not.toHaveURL(/view=editor/);

      const childFolder = page
        .locator('[data-testid^="detail-row-"]:not([aria-disabled="true"])')
        .first();
      if ((await childFolder.count()) > 0) {
        await childFolder.dblclick();
        await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible();
        await expect(page).not.toHaveURL(/view=editor/);
      }

      expect(
        workflowHits,
        "folder browse must not call itemmanagement workflow / getTransitions",
      ).toEqual([]);
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
