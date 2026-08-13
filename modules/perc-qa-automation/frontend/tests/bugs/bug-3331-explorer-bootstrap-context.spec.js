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
 * GH-3331 / parent #3329 — ContentExplorerShell must not throw
 * {@code useContext} on BootstrapContext when opening a folder.
 *
 * Run after perc-devctl qa-up:
 *   npm run test:surface -- --path tests/bugs/bug-3331-explorer-bootstrap-context.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const { expectNoSeriousA11yViolations } = require("../helpers/a11y");

const EXPLORER_URL = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=${Date.now()}`;

function isUseContextCrash(text) {
  return /useContext|BootstrapContext|Cannot read properties of null/i.test(
    String(text || ""),
  );
}

test.describe("GH-3331 Explorer BootstrapContext useContext", () => {
  test(
    "opening a folder does not throw useContext on BootstrapContext",
    { tag: ["@explorer", "@bootstrap", "@smoke"] },
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
      await expect(
        page.locator('[data-testid="explorer-bootstrap-unavailable"]'),
      ).toHaveCount(0);

      const foldersNode = page.locator(
        '[data-testid="tree-node-/Folders/"], [data-testid="tree-node-/Folders"]',
      );
      if ((await foldersNode.count()) > 0) {
        await foldersNode.first().click();
      }
      await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible();
      await expect(
        page.locator('[data-testid="explorer-bootstrap-unavailable"]'),
      ).toHaveCount(0);

      const childFolder = page
        .locator('[data-testid^="detail-row-"]:not([aria-disabled="true"])')
        .first();
      if ((await childFolder.count()) > 0) {
        await childFolder.dblclick();
        await expect(
          page.locator('[data-testid="content-explorer-shell"]'),
        ).toBeVisible();
      }

      const useContextHits = consoleErrors.filter(isUseContextCrash);
      expect(
        useContextHits,
        `useContext/BootstrapContext console errors: ${useContextHits.join(" | ")}`,
      ).toEqual([]);
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
    },
  );
});
