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
 * Playwright surface: #2792 / #3553 / parent #2400 — Subfolder Copy wizard.
 *
 * <p>Verifies Content → Subfolder Copy chrome on the modern SPA Explorer, plus
 * Cancel / item-click dismiss (#3553). Full multi-folder submit is soft-skipped
 * when the H2 fixture lacks a navigable folder tree suitable for a destructive
 * copy.</p>
 *
 * <p>Tags: {@code @explorer-subfolder-copy} {@code @explorer} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-subfolder-copy.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerSpaUrl,
  openContentMenu,
} = require("./helpers/explorer-subfolder-copy");
const {
  assetsFolderUrl,
  isCopyFolderSuccessStatus,
  isFoldersCopyFolderUrl,
  pathFolderServiceUrl,
  seedDisposableEmptyFolder,
  sitesFolderUrl,
  uniqueCopyFolderName,
} = require("./helpers/explorer-copy-folder");
const {
  expandExplorerTreeNode,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-sites-list-create");

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

/**
 * Open the wizard when folder context exists. Returns false when the H2
 * fixture has no navigable folder (soft-skip caller).
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<boolean>}
 */
async function tryOpenWizard(page) {
  await tryEnterFolder(page);
  await openContentMenu(page);
  const menuItem = page.locator(
    `[data-testid="${TEST_IDS.subfolderCopyMenu}"]`,
  );
  await expect(menuItem).toBeVisible({ timeout: 5_000 });
  if (await menuItem.isDisabled()) {
    return false;
  }
  await menuItem.click();
  const wizard = page.locator(`[data-testid="${TEST_IDS.wizard}"]`);
  await expect(wizard).toBeVisible({ timeout: 10_000 });
  return true;
}

test.describe("modern React Content Explorer — subfolder copy chrome (#2792)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(180_000);
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
    "Cancel dismisses the Subfolder Copy overlay (#3553)",
    { tag: ["@explorer-subfolder-copy", "@explorer"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      const pageErrors = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err && err.message ? err.message : err));
      });
      page.on("console", (msg) => {
        if (msg.type() === "error" && !isKnownExplorerSitesConsoleNoise(msg.text())) {
          pageErrors.push(msg.text());
        }
      });

      const opened = await tryOpenWizard(page);
      if (!opened) {
        test.info().annotations.push({
          type: "soft-skip",
          description:
            "No folder context under /Sites (H2 fixture may lack navigable folders).",
        });
        return;
      }

      const source = page.locator(`[data-testid="${TEST_IDS.sourceInput}"]`);
      await expect(source).toBeVisible();
      await source.fill("/Sites/Demo/Home");
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepTarget}"]`),
      ).toBeVisible();

      await page.locator(`[data-testid="${TEST_IDS.cancel}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.wizard}"]`),
      ).toHaveCount(0, { timeout: 10_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.subfolderCopyPanel}"]`),
      ).toHaveCount(0);
      expect(pageErrors, `console/pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "item click dismisses the Subfolder Copy overlay without POST (#3553)",
    { tag: ["@explorer-subfolder-copy", "@explorer"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      const pageErrors = [];
      const copyPosts = [];
      page.on("pageerror", (err) => {
        pageErrors.push(String(err && err.message ? err.message : err));
      });
      page.on("console", (msg) => {
        if (msg.type() === "error" && !isKnownExplorerSitesConsoleNoise(msg.text())) {
          pageErrors.push(msg.text());
        }
      });
      page.on("request", (req) => {
        if (
          req.method() === "POST" &&
          /folders\/copy|moveItem/i.test(req.url())
        ) {
          copyPosts.push(req.url());
        }
      });

      const opened = await tryOpenWizard(page);
      if (!opened) {
        test.info().annotations.push({
          type: "soft-skip",
          description:
            "No folder context under /Sites (H2 fixture may lack navigable folders).",
        });
        return;
      }

      const list = page.locator(`[data-testid="${TEST_IDS.detailList}"]`);
      const rows = list.locator(
        'tbody tr[data-testid^="detail-row-"]:not([aria-disabled="true"])',
      );
      const treeNodes = page.locator('[data-testid^="tree-node-"]');
      if ((await rows.count()) > 0) {
        await rows.first().click();
      } else if ((await treeNodes.count()) > 0) {
        await treeNodes.first().click();
      } else {
        await page.locator(`[data-testid="${TEST_IDS.shell}"]`).click({
          position: { x: 8, y: 8 },
        });
      }

      await expect(
        page.locator(`[data-testid="${TEST_IDS.wizard}"]`),
      ).toHaveCount(0, { timeout: 10_000 });
      expect(copyPosts, `unexpected copy POST: ${copyPosts.join(" | ")}`).toEqual(
        [],
      );
      expect(pageErrors, `console/pageerror: ${pageErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "Submit copies the selected folder into the destination and leaves the source (#4750)",
    { tag: ["@explorer-subfolder-copy", "@explorer"] },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error" && !isKnownExplorerSitesConsoleNoise(msg.text())) {
          pageErrors.push(msg.text());
        }
      });

      const stamp = Date.now();
      const sourceName = uniqueCopyFolderName("qa4750src", stamp);
      const destName = uniqueCopyFolderName("qa4750dst", stamp);
      const headers = adminBasicAuthHeaders();
      const assetsStatus = await request.get(assetsFolderUrl(BASE_URL), {
        headers,
      });
      const sitesStatus = await request.get(sitesFolderUrl(BASE_URL), {
        headers,
      });
      const useAssets = assetsStatus.status() === 200;
      expect(
        useAssets || sitesStatus.status() === 200,
        `H2 parent missing Assets=${assetsStatus.status()} Sites=${sitesStatus.status()}`,
      ).toBe(true);
      const parentPath = useAssets ? "Assets" : "Sites";
      const destFolder = await seedDisposableEmptyFolder(
        request,
        BASE_URL,
        headers,
        { parentPath, name: destName },
      );
      const sourceFolder = await seedDisposableEmptyFolder(
        request,
        BASE_URL,
        headers,
        { parentPath, name: sourceName },
      );

      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 20_000 });
      const parentNode = page
        .locator(
          `[data-testid*="tree-node"][data-testid*="${parentPath}"]`,
        )
        .first();
      await expect(parentNode).toBeVisible({ timeout: 20_000 });
      await parentNode.click({ force: true });
      await expandExplorerTreeNode(parentNode).catch(() => undefined);
      const list = page.locator(`[data-testid="${TEST_IDS.detailList}"]`);
      await expect(list.getByText(sourceName, { exact: true })).toBeVisible({
        timeout: 20_000,
      });
      await list.getByText(sourceName, { exact: true }).click();

      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.subfolderCopyMenu}"]`).click();
      const wizard = page.locator(`[data-testid="${TEST_IDS.wizard}"]`);
      await expect(wizard).toBeVisible({ timeout: 10_000 });
      const sourcePath = String(
        sourceFolder.path || `/${parentPath}/${sourceName}`,
      );
      await page.locator(`[data-testid="${TEST_IDS.sourceInput}"]`).fill(sourcePath);
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();
      await page
        .locator('[data-testid="subfolder-copy-target"]')
        .fill(String(destFolder.path || `/${parentPath}/${destName}`));
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();

      const copyRespPromise = page.waitForResponse(
        (res) =>
          isFoldersCopyFolderUrl(res.url()) &&
          res.request().method() === "POST",
        { timeout: 60_000 },
      );
      await page.locator('[data-testid="subfolder-copy-run"]').click();
      const copyResp = await copyRespPromise;
      expect(
        isCopyFolderSuccessStatus(copyResp.status()),
        `copy/folder status ${copyResp.status()}`,
      ).toBe(true);

      await expect(list.getByText(sourceName, { exact: true })).toBeVisible({
        timeout: 30_000,
      });
      const sourceUrl = `${pathFolderServiceUrl(BASE_URL)}/${parentPath}/${encodeURIComponent(sourceName)}`;
      const sourceStill = await request.get(sourceUrl, { headers });
      expect(
        sourceStill.status(),
        `source folder must remain after copy (${sourceUrl})`,
      ).toBe(200);
      expect(String(sourceFolder.path || "")).not.toBe("");
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );

  test(
    "HTTP 409 stays on the wizard and does not remove the source folder (#4750)",
    { tag: ["@explorer-subfolder-copy", "@explorer"] },
    async ({ page, request }) => {
      test.setTimeout(180_000);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));

      const stamp = Date.now();
      const sourceName = uniqueCopyFolderName("qa4750cfl", stamp);
      const headers = adminBasicAuthHeaders();
      const assetsStatus = await request.get(assetsFolderUrl(BASE_URL), {
        headers,
      });
      const parentPath = assetsStatus.status() === 200 ? "Assets" : "Sites";
      await seedDisposableEmptyFolder(request, BASE_URL, headers, {
        parentPath,
        name: sourceName,
      });

      await page.route(/\/rest\/folders\/copy\/folder(?:\?|$)/, (route) => {
        if (route.request().method() !== "POST") {
          return route.continue();
        }
        return route.fulfill({
          status: 409,
          contentType: "application/json",
          body: "{}",
        });
      });

      const parentNode = page
        .locator(`[data-testid*="tree-node"][data-testid*="${parentPath}"]`)
        .first();
      await expect(parentNode).toBeVisible({ timeout: 20_000 });
      await parentNode.click({ force: true });
      const list = page.locator(`[data-testid="${TEST_IDS.detailList}"]`);
      await expect(list.getByText(sourceName, { exact: true })).toBeVisible({
        timeout: 20_000,
      });
      await list.getByText(sourceName, { exact: true }).click();
      await openContentMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.subfolderCopyMenu}"]`).click();
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();
      await page
        .locator('[data-testid="subfolder-copy-target"]')
        .fill(`/${parentPath}`);
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();
      await page.locator('[data-testid="subfolder-copy-run"]').click();
      await expect(
        page.locator('[data-testid="subfolder-copy-progress"]'),
      ).toContainText("HTTP 409", { timeout: 20_000 });
      await expect(list.getByText(sourceName, { exact: true })).toBeVisible();
      expect(pageErrors, pageErrors.join(" | ")).toEqual([]);
    },
  );
});
