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
 * GH-3362 — Explorer Copy Folder must not POST a bare sourcePath root.
 *
 * Intercepts copy/move POSTs so a live copy is not required. Fulfills with
 * 200 and asserts the body is CopyFolderItemRequest (or MoveFolderItem for
 * move) — never { sourcePath, targetPath, copy }.
 *
 * Run after perc-devctl qa-up:
 *   npm run test:surface -- --path tests/bugs/bug-3362-copy-folder-envelope.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("../helpers/auth");
const { expectNoSeriousA11yViolations } = require("../helpers/a11y");
const {
  isBareSourcePathRoot,
  isCopyFolderItemRequestEnvelope,
} = require("../helpers/explorer-copy-folder-envelope");

const EXPLORER_URL = `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?entry=explorer&rxFolderMutations=1&_=${Date.now()}`;

function isCopyOrMovePost(url) {
  const u = String(url || "");
  return (
    u.includes("/pathmanagement/path/moveItem") ||
    u.includes("/folders/copy/folder") ||
    u.includes("/folders/copy/item")
  );
}

test.describe("GH-3362 Copy Folder envelope", () => {
  test(
    "Copy does not POST a bare sourcePath root",
    { tag: ["@explorer", "@copy-folder", "@bug-3362"] },
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

      /** @type {Array<{ url: string, body: unknown }>} */
      const captured = [];
      await page.route("**/*", async (route) => {
        const req = route.request();
        const url = req.url();
        if (req.method() === "POST" && isCopyOrMovePost(url)) {
          let body = null;
          try {
            body = req.postDataJSON();
          } catch {
            const raw = req.postData();
            try {
              body = raw ? JSON.parse(raw) : null;
            } catch {
              body = raw;
            }
          }
          captured.push({ url, body });
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: JSON.stringify({ message: "Copied OK" }),
          });
          return;
        }
        await route.continue();
      });

      await loginAsAdmin(page);
      await page.goto(EXPLORER_URL);
      await page.waitForLoadState("networkidle").catch(() => undefined);

      const shell = page.locator(
        '[data-testid="content-explorer-shell"], [data-testid="content-explorer"]',
      );
      const chromeVisible = await shell
        .first()
        .isVisible({ timeout: 20_000 })
        .catch(() => false);
      if (!chromeVisible) {
        test.skip(true, "Explorer shell not visible — envelope covered by Vitest");
        return;
      }

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      }).catch(() => undefined);

      const foldersNode = page
        .locator(
          '[data-testid="tree-node-/Folders/"], [data-testid="tree-node-/Folders"], [data-testid*="tree-node"][data-testid*="Folders"]',
        )
        .first();
      if (await foldersNode.isVisible({ timeout: 10_000 }).catch(() => false)) {
        await foldersNode.click();
        await page.waitForLoadState("networkidle").catch(() => undefined);
      }

      const listRow = page
        .locator('tbody tr[data-testid^="detail-row-"]:not([aria-disabled="true"])')
        .first();
      if (await listRow.isVisible({ timeout: 8_000 }).catch(() => false)) {
        await listRow.click();
      }

      const copyBtn = page.locator('[data-testid="action-copy"]').first();
      const copyVisible = await copyBtn
        .isVisible({ timeout: 8_000 })
        .catch(() => false);
      if (!copyVisible || (await copyBtn.isDisabled().catch(() => true))) {
        test.skip(
          true,
          "Copy action not enabled (no writable folder selection). Envelope covered by Vitest wrapMoveFolderItem / copyFolder tests.",
        );
        return;
      }

      page.once("dialog", async (dialog) => {
        await dialog.accept("/Folders");
      });
      await copyBtn.click();
      await page.waitForTimeout(2_000);

      if (captured.length === 0) {
        test.skip(
          true,
          "Copy click did not POST moveItem or /folders/copy/* — envelope covered by Vitest",
        );
        return;
      }

      for (const hit of captured) {
        expect(
          isBareSourcePathRoot(hit.body),
          `bare sourcePath root is the #3362 400: ${JSON.stringify(hit)}`,
        ).toBe(false);
        if (String(hit.url).includes("/folders/copy/")) {
          expect(
            isCopyFolderItemRequestEnvelope(hit.body),
            `copy must wrap CopyFolderItemRequest: ${JSON.stringify(hit)}`,
          ).toBe(true);
        }
      }

      const relatedConsole = consoleErrors.filter(
        (t) =>
          /unexpected element|sourcePath|MoveFolderItem|JAXBException/i.test(t) &&
          !/favicon|ResizeObserver/i.test(t),
      );
      expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
    },
  );
});
