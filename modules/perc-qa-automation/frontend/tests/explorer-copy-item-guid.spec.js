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
 * Playwright: #4989 / parent #4530 — Content → Copy item id.
 *
 * Tags: @explorer-copy-item-guid @explorer @smoke
 *
 * npm run test:surface -- --path tests/explorer-copy-item-guid.spec.js
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerCopyItemGuidUrl,
  isCopiedItemGuidStatus,
  isKnownExplorerCopyGuidConsoleNoise,
} = require("./helpers/explorer-copy-item-guid");
const { openContentMenu } = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-copy-item-guid", "@explorer", "@smoke"];

async function openExplorer(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error" && !isKnownExplorerCopyGuidConsoleNoise(msg.text())) {
      jsErrors.push(msg.text());
    }
  });
  await loginAsAdmin(page);
  await page.goto(explorerCopyItemGuidUrl(BASE_URL), { waitUntil: "domcontentloaded" });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 30_000,
  });
  return jsErrors;
}

async function listReady(page) {
  await page.locator(`[data-testid="${TEST_IDS.detailList}"]`).waitFor({ timeout: 20_000 });
}

async function openFirstFolderRow(page) {
  const folderRow = page.locator(
    '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])',
  );
  if ((await folderRow.count()) === 0) {
    return false;
  }
  const icon = folderRow.first().locator('[data-testid^="detail-folder-icon-"]');
  if ((await icon.count()) > 0) {
    await icon.first().click();
  } else {
    await folderRow.first().dblclick({ force: true });
  }
  await listReady(page);
  return true;
}

async function selectFirstContentItem(page) {
  await expect(
    page
      .locator(
        `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites"]`,
      )
      .first(),
  ).toBeVisible({ timeout: 30_000 });
  const roots = ["Sites", "Assets"];
  for (const rootName of roots) {
    const root = page.locator(
      `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/${rootName}/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/${rootName}"]`,
    );
    if ((await root.count()) === 0) {
      continue;
    }
    await root.first().click();
    await listReady(page);
    for (let depth = 0; depth < 6; depth += 1) {
      const itemRow = page.locator(
        '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="item"]:not([aria-disabled="true"])',
      );
      if ((await itemRow.count()) > 0) {
        await itemRow.first().click({ force: true, timeout: 10_000 });
        return true;
      }
      const opened = await openFirstFolderRow(page);
      if (!opened) {
        break;
      }
    }
  }
  return false;
}

test.describe("Explorer copy selected item id (#4989 / #4530)", () => {
  test(
    "UI: copies the selected page or asset content id",
    { tag: TAGS },
    async ({ page, context }) => {
      test.setTimeout(120_000);
      await context.grantPermissions(["clipboard-read", "clipboard-write"]);
      const jsErrors = await openExplorer(page);
      const found = await selectFirstContentItem(page);
      expect(found, "H2 Explorer has no selectable page or asset").toBe(true);
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      const before = await shell.getAttribute("data-selected-item-id");
      expect(isCopiedItemGuidStatus("success", before)).toBe(true);
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.copyItemGuid}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "success");
      const copied = await status.getAttribute("data-copied-guid");
      expect(copied).toBe(before);
      const clip = await page.evaluate(() => navigator.clipboard.readText());
      expect(clip).toBe(copied);
      await expect(shell).toHaveAttribute("data-selected-item-id", before);
      await expectNoSeriousA11yViolations(page, `[data-testid="${TEST_IDS.shell}"]`);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: no selection does not write the clipboard",
    { tag: TAGS },
    async ({ page, context }) => {
      test.setTimeout(120_000);
      await context.grantPermissions(["clipboard-read", "clipboard-write"]);
      const jsErrors = await openExplorer(page);
      await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toHaveAttribute(
        "data-selected-item-id",
        "",
      );
      await page.evaluate(() => navigator.clipboard.writeText("unchanged-sentinel"));
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.copyItemGuid}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "none");
      const clip = await page.evaluate(() => navigator.clipboard.readText());
      expect(clip).toBe("unchanged-sentinel");
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "UI: a folder is named and is not success",
    { tag: TAGS },
    async ({ page, context }) => {
      test.setTimeout(120_000);
      await context.grantPermissions(["clipboard-read", "clipboard-write"]);
      const jsErrors = await openExplorer(page);
      const root = page.locator(
        `[data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites/"], [data-testid="${TEST_IDS.tree}"] [data-testid="tree-node-/Sites"]`,
      );
      await expect(root.first()).toBeVisible({ timeout: 30_000 });
      await root.first().click();
      await listReady(page);
      const folderRow = page.locator(
        '[data-testid="detail-list"] tbody tr[data-testid^="detail-row-"][data-row-kind="folder"]:not([aria-disabled="true"])',
      );
      await expect(folderRow.first()).toBeVisible({ timeout: 20_000 });
      const folderName = await folderRow.first().getAttribute("data-item-name");
      await folderRow.first().click({ force: true });
      await page.evaluate(() => navigator.clipboard.writeText("unchanged-sentinel"));
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.copyItemGuid}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "folder");
      await expect(status).toContainText(folderName || "Folders are not copied");
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
      const found = await selectFirstContentItem(page);
      expect(found, "H2 Explorer has no selectable page or asset").toBe(true);
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      const before = await shell.getAttribute("data-selected-item-id");
      await page.evaluate(() => {
        Object.defineProperty(navigator, "clipboard", {
          configurable: true,
          value: {
            writeText: () => Promise.reject(new Error("denied")),
          },
        });
      });
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.copyItemGuid}"]`).click();
      const status = page.locator(`[data-testid="${TEST_IDS.status}"]`);
      await expect(status).toHaveAttribute("data-kind", "error");
      await expect(status).toHaveAttribute("data-reason", "clipboard");
      await expect(shell).toHaveAttribute("data-selected-item-id", before);
      expect(jsErrors, jsErrors.join("\n")).toEqual([]);
    },
  );
});
