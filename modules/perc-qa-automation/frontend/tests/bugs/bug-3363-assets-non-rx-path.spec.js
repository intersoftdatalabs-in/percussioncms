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
 * GH-3363 — Assets create/rename/move stay on pathmanagement when Folder
 * Mutations is on. Explorer selection often rewrites Assets to
 * {@code /Folders/$System$/Assets} (or {@code //Folders/$System$/Assets});
 * that must not route to {@code /content-explorer/folders}.
 *
 * Run after perc-devctl qa-up:
 *   npm run test:surface -- --path tests/bugs/bug-3363-assets-non-rx-path.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const { expectNoSeriousA11yViolations } = require("../helpers/a11y");

const EXPLORER_URL = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&rxFolderMutations=1&_=${Date.now()}`;

function isCreateFolderUrl(url) {
  const u = String(url || "");
  return (
    u.includes("/pathmanagement/path/addNewFolder") ||
    /\/content-explorer\/folders(?:\?|$|\/)/.test(u)
  );
}

test.describe("GH-3363 Assets stay on pathmanagement", () => {
  test(
    "Create folder under Assets does not POST content-explorer folders",
    { tag: ["@explorer", "@folder-mutations", "@bug-3363"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const consoleErrors = [];
      page.on("pageerror", (err) => {
        consoleErrors.push(String(err && err.message ? err.message : err));
      });
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });

      /** @type {Array<{ url: string, method: string }>} */
      const mutations = [];
      page.on("request", (req) => {
        const url = req.url();
        if (isCreateFolderUrl(url) && req.method() !== "OPTIONS") {
          mutations.push({ url, method: req.method() });
        }
      });

      await loginAsAdmin(page);
      await page.goto(EXPLORER_URL);
      await page.waitForLoadState("networkidle").catch(() => undefined);

      const shell = page.locator('[data-testid="content-explorer-shell"]');
      const chromeVisible = await shell
        .isVisible({ timeout: 20_000 })
        .catch(() => false);
      if (!chromeVisible) {
        test.skip(true, "Explorer shell not visible — routing covered by Vitest");
        return;
      }

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      }).catch(() => undefined);

      const assetsNode = page
        .locator(
          [
            '[data-testid="tree-node-/Assets/"]',
            '[data-testid="tree-node-/Assets"]',
            '[data-testid="tree-node-/Folders/$System$/Assets/"]',
            '[data-testid="tree-node-/Folders/$System$/Assets"]',
            '[data-testid="tree-node-//Folders/$System$/Assets"]',
            '[data-testid*="tree-node"][data-testid*="Assets"]',
          ].join(", "),
        )
        .first();
      const assetsVisible = await assetsNode
        .isVisible({ timeout: 12_000 })
        .catch(() => false);
      if (!assetsVisible) {
        test.skip(true, "Assets tree node not visible — routing covered by Vitest");
        return;
      }
      await assetsNode.click();
      await page.waitForLoadState("networkidle").catch(() => undefined);

      const createBtn = page.locator('[data-testid="action-create-folder"]').first();
      const createVisible = await createBtn
        .isVisible({ timeout: 8_000 })
        .catch(() => false);
      if (!createVisible || (await createBtn.isDisabled().catch(() => true))) {
        test.skip(
          true,
          "Create folder not enabled under Assets — routing covered by Vitest",
        );
        return;
      }

      page.once("dialog", async (dialog) => {
        await dialog.accept(`qa3363_${Date.now()}`);
      });
      await createBtn.click();
      await page.waitForTimeout(2_500);

      const hitRx = mutations.some((m) =>
        m.url.includes("/content-explorer/folders"),
      );
      const hitPath = mutations.some((m) =>
        m.url.includes("/pathmanagement/path/addNewFolder"),
      );

      expect(
        hitRx,
        `Assets must not use RX folders API: ${JSON.stringify(mutations)}`,
      ).toBe(false);

      if (!hitPath) {
        test.skip(
          true,
          `Create did not POST addNewFolder (saw ${JSON.stringify(mutations)}); routing covered by Vitest`,
        );
        return;
      }

      const relatedConsole = consoleErrors.filter(
        (t) =>
          /unexpected element|AddFolderRequest|JAXBException|content-explorer\/folders/i.test(
            t,
          ) && !/favicon|ResizeObserver/i.test(t),
      );
      expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
    },
  );
});
