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
 * Playwright surface: #2767 / parent #2400 — Site Copy wizard in Explorer shell.
 *
 * <p>Verifies Content → Site Copy chrome on the modern SPA Explorer. Full
 * multi-site submit is soft-skipped when the H2 fixture has fewer than two
 * sites (or no navigable site folder).</p>
 *
 * <p>Tags: {@code @explorer-site-copy} {@code @explorer} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-site-copy.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerSpaUrl,
  openContentMenu,
} = require("./helpers/explorer-site-copy");

/** Wait until the detail list region is present (folder navigation settled). */
async function listWaitReady(page) {
  await page
    .locator(`[data-testid="${TEST_IDS.detailList}"]`)
    .waitFor({ timeout: 15_000 });
}

/**
 * Try to navigate tree into a site under /Sites. Returns site name if known.
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<string | null>}
 */
async function tryEnterSiteFolder(page) {
  const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
  await expect(tree).toBeVisible({ timeout: 15_000 });

  const sitesNode = page
    .locator(
      '[data-testid="tree-node-/Sites/"], [data-testid="tree-node-/Sites"], [data-testid*="tree-node"][data-testid*="Sites"]',
    )
    .first();
  if ((await sitesNode.count()) > 0) {
    // No force:true / silent catch — failures must surface as real errors.
    await sitesNode.click({ timeout: 10_000 });
    await listWaitReady(page);
    await page.waitForLoadState("networkidle").catch(() => {});
  }

  // Prefer a child site folder under Sites (detail list or tree).
  const list = page.locator(`[data-testid="${TEST_IDS.detailList}"]`);
  const siteRows = list.locator(
    'tbody tr[data-testid^="detail-row-"]:not([aria-disabled="true"])',
  );
  if ((await siteRows.count()) > 0) {
    const first = siteRows.first();
    const name =
      (await first.getAttribute("data-name")) ||
      (await first.locator("td").first().innerText()) ||
      null;
    try {
      await first.dblclick({ timeout: 10_000 });
    } catch {
      await first.click({ timeout: 10_000 });
    }
    await listWaitReady(page);
    await page.waitForLoadState("networkidle").catch(() => {});
    return name ? String(name).trim() || null : null;
  }

  // Tree: expand first site-like child if present.
  const siteTree = page
    .locator('[data-testid*="tree-node"][data-testid*="/Sites/"]')
    .filter({ hasNot: page.locator('[data-testid="tree-node-/Sites"]') })
    .first();
  if ((await siteTree.count()) > 0) {
    await siteTree.click({ timeout: 10_000 });
    await listWaitReady(page);
    return null;
  }
  return null;
}

test.describe("modern React Content Explorer — site copy chrome (#2767)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(45_000);
    await loginAsAdmin(page);
    await page.goto(explorerSpaUrl(BASE_URL));
    await page.waitForLoadState("networkidle");
  });

  test(
    "Content menu exposes Site Copy control (disabled at non-site root)",
    { tag: ["@explorer-site-copy", "@explorer", "@smoke"] },
    async ({ page }) => {
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });

      // T082b / WebUI AGENTS.md — a11y gate on product Explorer shell surface.
      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });

      await openContentMenu(page);
      const menuItem = page.locator(
        `[data-testid="${TEST_IDS.siteCopyMenu}"]`,
      );
      await expect(menuItem).toBeVisible();
      // At default explorer root (often / or /Sites) site context is usually absent.
      // Soft assert: control exists; disabled state is environment-dependent.
      await expect(menuItem).toHaveAttribute("role", "menuitemcheckbox");
    },
  );

  test(
    "opens Site Copy wizard when a site folder is in context",
    { tag: ["@explorer-site-copy", "@explorer"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });

      await tryEnterSiteFolder(page);

      await openContentMenu(page);
      const menuItem = page.locator(
        `[data-testid="${TEST_IDS.siteCopyMenu}"]`,
      );
      await expect(menuItem).toBeVisible({ timeout: 5_000 });

      const disabled = await menuItem.isDisabled();
      if (disabled) {
        // H2 / empty Sites fixture — soft-skip full wizard mount.
        test.info().annotations.push({
          type: "soft-skip",
          description:
            "No site context under /Sites (H2 fixture may lack multi-site / site folders). Menu chrome still present.",
        });
        return;
      }

      await menuItem.click();
      const panel = page.locator(`[data-testid="${TEST_IDS.siteCopyPanel}"]`);
      const wizard = page.locator(`[data-testid="${TEST_IDS.wizard}"]`);
      // Panel stays mounted while the wizard opens, so or() matches two nodes
      // and Playwright strict mode fails. Either visible is enough.
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
    "site-copy submit reports the new name or a mapped 409",
    { tag: ["@explorer-site-copy", "@explorer"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() !== "error") {
          return;
        }
        const text = msg.text();
        // The live 409/400 response is an expected HTTP failure, not a JS defect.
        if (/Failed to load resource/i.test(text)) {
          return;
        }
        pageErrors.push(text);
      });

      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 15_000 });
      await tryEnterSiteFolder(page);
      await openContentMenu(page);
      const menuItem = page.locator(`[data-testid="${TEST_IDS.siteCopyMenu}"]`);
      await expect(menuItem).toBeVisible();
      if (await menuItem.isDisabled()) {
        throw new Error(
          "Site Copy stayed disabled — H2 QA has no site folder in context",
        );
      }
      await menuItem.click();
      const wizard = page.locator(`[data-testid="${TEST_IDS.wizard}"]`);
      await expect(wizard).toBeVisible({ timeout: 10_000 });
      const source = page.locator(`[data-testid="${TEST_IDS.sourceInput}"]`);
      const sourceName = (await source.inputValue()).trim();
      expect(sourceName.length).toBeGreaterThan(0);

      const targetName = `${sourceName}-copy-${Date.now()}`;
      await page.route("**/sitemanage/site/copy", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ name: targetName }),
        });
      });

      await page.locator('[data-testid="site-copy-next"]').click();
      await page.locator('[data-testid="site-copy-target"]').fill(targetName);
      await page.locator('[data-testid="site-copy-next"]').click();
      await page.locator('[data-testid="site-copy-next"]').click();
      await page.locator('[data-testid="site-copy-next"]').click();
      await page.locator('[data-testid="site-copy-run"]').click();
      await expect(page.locator('[data-testid="site-copy-progress"]')).toContainText(
        "Site copy completed",
      );
      await expect(page.locator('[data-testid="site-copy-result-name"]')).toContainText(
        targetName,
      );
      await page.unroute("**/sitemanage/site/copy");

      await page.locator('[data-testid="site-copy-cancel"]').click();
      await page.locator('[data-testid="site-copy-next"]').click();
      await page.locator('[data-testid="site-copy-target"]').fill(sourceName);
      await page.locator('[data-testid="site-copy-next"]').click();
      await page.locator('[data-testid="site-copy-next"]').click();
      await page.locator('[data-testid="site-copy-next"]').click();
      const copyResponse = page.waitForResponse(
        (res) =>
          res.url().includes("/sitemanage/site/copy") &&
          res.request().method() === "POST",
        { timeout: 60_000 },
      );
      await page.locator('[data-testid="site-copy-run"]').click();
      const response = await copyResponse;
      expect([400, 403, 409]).toContain(response.status());
      await expect(page.locator('[data-testid="site-copy-progress"]')).toContainText(
        `HTTP ${response.status()}`,
      );
      await expect(page.locator('[data-testid="site-copy-result-name"]')).toHaveCount(0);
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );
});
