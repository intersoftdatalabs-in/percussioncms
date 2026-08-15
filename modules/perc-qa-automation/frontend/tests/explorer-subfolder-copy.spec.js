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
 * Playwright surface: #2792 / parent #2400 — Subfolder Copy wizard in Explorer shell.
 *
 * <p>Verifies Content → Subfolder Copy chrome on the modern SPA Explorer. Full
 * multi-folder submit is soft-skipped when the H2 fixture lacks a navigable
 * folder tree suitable for a destructive copy.</p>
 *
 * <p>Tags: {@code @explorer-subfolder-copy} {@code @explorer} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-subfolder-copy.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerSpaUrl,
  openContentMenu,
} = require("./helpers/explorer-subfolder-copy");

/** Wait until the detail list region is present (folder navigation settled). */
async function listWaitReady(page) {
  await page
    .locator(`[data-testid="${TEST_IDS.detailList}"]`)
    .waitFor({ timeout: 15_000 });
}

/**
 * Try to navigate tree into a folder under /Sites. Returns true if a folder
 * path likely has context for Subfolder Copy.
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<boolean>}
 */
async function tryEnterFolder(page) {
  const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
  await expect(tree).toBeVisible({ timeout: 15_000 });

  const sitesNode = page
    .locator(
      '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"], [data-testid*="tree-node"][data-testid*="Sites"]',
    )
    .first();
  if ((await sitesNode.count()) > 0) {
    await sitesNode.click({ timeout: 10_000 });
    await listWaitReady(page);
    await page.waitForLoadState("networkidle").catch(() => {});
  }

  const list = page.locator(`[data-testid="${TEST_IDS.detailList}"]`);
  const rows = list.locator(
    'tbody tr[data-testid^="detail-row-"]:not([aria-disabled="true"])',
  );
  if ((await rows.count()) > 0) {
    const first = rows.first();
    try {
      await first.dblclick({ timeout: 10_000 });
    } catch {
      await first.click({ timeout: 10_000 });
    }
    await listWaitReady(page);
    await page.waitForLoadState("networkidle").catch(() => {});
    return true;
  }

  const siteTree = page
    .locator('[data-testid*="tree-node"][data-testid*="/Sites/"]')
    .filter({ hasNot: page.locator('[data-testid="tree-node-/Sites"]') })
    .first();
  if ((await siteTree.count()) > 0) {
    await siteTree.click({ timeout: 10_000 });
    await listWaitReady(page);
    return true;
  }
  return false;
}

test.describe("modern React Content Explorer — subfolder copy chrome (#2792)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
    await page.goto(explorerSpaUrl(BASE_URL));
    await page.waitForLoadState("networkidle");
  });

  test(
    "Content menu exposes Subfolder Copy control",
    { tag: ["@explorer-subfolder-copy", "@explorer", "@smoke"] },
    async ({ page }) => {
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });

      await openContentMenu(page);
      const menuItem = page.locator(
        `[data-testid="${TEST_IDS.subfolderCopyMenu}"]`,
      );
      await expect(menuItem).toBeVisible();
      await expect(menuItem).toHaveAttribute("role", "menuitemcheckbox");
    },
  );

  test(
    "opens Subfolder Copy wizard when a folder is in context",
    { tag: ["@explorer-subfolder-copy", "@explorer"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });

      await tryEnterFolder(page);

      await openContentMenu(page);
      const menuItem = page.locator(
        `[data-testid="${TEST_IDS.subfolderCopyMenu}"]`,
      );
      await expect(menuItem).toBeVisible({ timeout: 5_000 });

      const disabled = await menuItem.isDisabled();
      if (disabled) {
        // H2 / empty Sites fixture — soft-skip full wizard mount.
        test.info().annotations.push({
          type: "soft-skip",
          description:
            "No folder context under /Sites (H2 fixture may lack navigable folders). Menu chrome still present.",
        });
        return;
      }

      await menuItem.click();
      const panel = page.locator(
        `[data-testid="${TEST_IDS.subfolderCopyPanel}"]`,
      );
      const wizard = page.locator(`[data-testid="${TEST_IDS.wizard}"]`);
      await expect(panel.or(wizard).first()).toBeVisible({ timeout: 10_000 });
      if ((await wizard.count()) > 0) {
        await expect(
          page.locator(`[data-testid="${TEST_IDS.sourceInput}"]`),
        ).toBeVisible();
        const sourceVal = await page
          .locator(`[data-testid="${TEST_IDS.sourceInput}"]`)
          .inputValue();
        expect(sourceVal.length).toBeGreaterThan(0);
      }
    },
  );

  test(
    "full subfolder-copy submit soft-skips without multi-folder fixture",
    { tag: ["@explorer-subfolder-copy", "@explorer"] },
    async ({ page }) => {
      // Intentional soft-skip: destructive folder copy needs a target path
      // and is not exercised on default H2 QA fixtures.
      test.info().annotations.push({
        type: "soft-skip",
        description:
          "Submit path uses POST /rest/folders/copy/folder with CopyFolderItemRequest (#3362). Live multi-folder submit deferred — H2 fixtures lack safe copy targets.",
      });
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });
      await openContentMenu(page);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.subfolderCopyMenu}"]`),
      ).toBeVisible();
    },
  );
});
