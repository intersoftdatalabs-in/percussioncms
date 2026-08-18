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
 * Playwright surface: #3575 / parent #3102 — H2 Sites/Assets tree + detail-list.
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>REST: path/folder/ lists Sites and Assets</li>
 *   <li>REST: path/folder/Sites is non-empty on H2 qa-up (demo-sites default)</li>
 *   <li>UI: {@code spa.jsp?entry=explorer} tree shows Sites and Assets roots</li>
 *   <li>UI: a sample site/folder lists children in {@code detail-list}</li>
 * </ul>
 *
 * <p><strong>No soft-skip</strong> when H2 has sample sites or
 * {@code TEST_DB_TYPE=h2} (CMS+H2 matrix {@code --demo-sites} default).
 * Do not claim gap-matrix Present from this surface.</p>
 *
 * <p>Tags: {@code @explorer-sites-assets-tree-list} {@code @explorer}
 * {@code @sites} {@code @smoke}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-sites-assets-tree-list.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const {
  TEST_IDS,
  REQUIRED_TREE_ROOTS,
  EXPECTED_SAMPLE_SITE_NAMES,
  explorerSpaUrl,
  sitesFolderUrl,
  assetsFolderUrl,
  rootFolderUrl,
  pathItemNames,
  hasAnyExpectedSampleSite,
  hasAllExpectedSampleSites,
  shouldSoftSkipSitesList,
  shouldSkipSitesAssetsTreeList,
  treeRootLocator,
  sitesTreeRootLocator,
  expandExplorerTreeNode,
  sitesTreeDescendantsLocator,
  siteChildNamesFromTreeTestIds,
  treeHasRoot,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-sites-assets-tree-list");

const ROOT_URL = rootFolderUrl(BASE_URL);
const SITES_URL = sitesFolderUrl(BASE_URL);
const ASSETS_URL = assetsFolderUrl(BASE_URL);

/**
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} url
 * @returns {Promise<string[]>}
 */
async function fetchChildNames(request, url) {
  const headers = adminBasicAuthHeaders();
  const res = await request.get(url, { headers });
  expect(res.status(), `GET ${url} must be 200`).toBe(200);
  return pathItemNames(await res.json());
}

test.describe("Explorer Sites/Assets tree + detail-list (#3575 / #3102)", () => {
  test(
    "REST: path/folder/ lists Sites and Assets; Sites children on H2",
    { tag: ["@explorer-sites-assets-tree-list", "@explorer", "@sites", "@smoke"] },
    async ({ request }) => {
      test.setTimeout(30_000);
      const rootNames = await fetchChildNames(request, ROOT_URL);
      for (const expected of REQUIRED_TREE_ROOTS) {
        expect(
          rootNames,
          `root folder list should include ${expected}; got ${JSON.stringify(rootNames)}`,
        ).toContain(expected);
      }
      const siteNames = await fetchChildNames(request, SITES_URL);
      expect(
        shouldSoftSkipSitesList(siteNames),
        "must not soft-skip Sites list when H2 has sample sites (#3575)",
      ).toBe(false);
      expect(
        siteNames.length,
        `H2 demo-sites should seed /Sites; got ${JSON.stringify(siteNames)}`,
      ).toBeGreaterThan(0);
      if (hasAnyExpectedSampleSite(siteNames)) {
        expect(
          hasAllExpectedSampleSites(siteNames),
          `partial sample set under Sites: ${JSON.stringify(siteNames)}; expected ${EXPECTED_SAMPLE_SITE_NAMES.join(", ")}`,
        ).toBe(true);
      }
      const assetNames = await fetchChildNames(request, ASSETS_URL);
      expect(assetNames, `GET ${ASSETS_URL} parsed`).toBeTruthy();
    },
  );

  test(
    "UI: explorer-tree shows Sites and Assets roots without skip",
    { tag: ["@explorer-sites-assets-tree-list", "@explorer", "@sites", "@smoke"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          jsErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });

      await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
        timeout: 20_000,
      });
      const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
      await expect(tree).toBeVisible({ timeout: 20_000 });

      const treeErr = page.locator(
        `[data-testid="${TEST_IDS.treeError}"], [data-testid="${TEST_IDS.tree}"] [role="alert"]`,
      );
      if ((await treeErr.count()) > 0 && (await treeErr.first().isVisible())) {
        const text = await treeErr.first().innerText();
        throw new Error(`Explorer tree failed to load: ${text}`);
      }

      const sitesRoot = sitesTreeRootLocator(page);
      const assetsRoot = treeRootLocator(page, "Assets");
      await expect(sitesRoot.first()).toBeVisible({ timeout: 20_000 });
      await expect(assetsRoot.first()).toBeVisible({ timeout: 20_000 });

      expect(
        shouldSkipSitesAssetsTreeList({
          sitesRootVisible: true,
          assetsRootVisible: true,
        }),
        "Sites/Assets tree must not soft-skip when roots are on the product route (#3575)",
      ).toBe(false);

      const nodeTestIds = await tree
        .locator('[data-testid^="tree-node-"]')
        .evaluateAll((els) =>
          els.map((el) => el.getAttribute("data-testid") || ""),
        );
      for (const root of REQUIRED_TREE_ROOTS) {
        expect(
          treeHasRoot(nodeTestIds, root),
          `expected explorer-tree root ${root}; testids=${JSON.stringify(nodeTestIds)}`,
        ).toBe(true);
      }

      const unexpected = jsErrors.filter((t) => !isKnownExplorerSitesConsoleNoise(t));
      expect(unexpected, `console/page errors: ${unexpected.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "UI: sample site folder lists children in detail-list (no skip)",
    { tag: ["@explorer-sites-assets-tree-list", "@explorer", "@sites"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const probe = await page.request.get(SITES_URL, {
        headers: adminBasicAuthHeaders(),
      });
      expect(probe.status()).toBe(200);
      const restNames = pathItemNames(await probe.json());
      expect(
        shouldSoftSkipSitesList(restNames),
        "must not soft-skip tree+list when H2 has sample sites (#3575)",
      ).toBe(false);
      expect(
        restNames.length,
        `Sites children empty (H2 demo-sites expected): ${JSON.stringify(restNames)}`,
      ).toBeGreaterThan(0);

      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          jsErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });

      const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
      await expect(tree).toBeVisible({ timeout: 20_000 });
      const sitesRoot = sitesTreeRootLocator(page);
      await expect(sitesRoot.first()).toBeVisible({ timeout: 20_000 });
      await expandExplorerTreeNode(sitesRoot.first());

      const descendants = sitesTreeDescendantsLocator(page);
      await expect(descendants.first()).toBeVisible({ timeout: 20_000 });

      const nodeTestIds = await tree
        .locator('[data-testid^="tree-node-"]')
        .evaluateAll((els) =>
          els.map((el) => el.getAttribute("data-testid") || ""),
        );
      const childNames = siteChildNamesFromTreeTestIds(nodeTestIds);
      expect(
        childNames.length,
        `tree under /Sites should list children; testids=${JSON.stringify(nodeTestIds)}`,
      ).toBeGreaterThan(0);

      const corporate = tree.locator(
        '[data-testid*="tree-node-/Sites/Corporate"][role="treeitem"], ' +
          '[data-testid*="tree-node-/Sites/Corporate"] [role="treeitem"]',
      );
      if ((await corporate.count()) > 0) {
        await corporate.first().click();
      } else {
        await descendants.first().locator('[role="treeitem"]').first().click();
      }

      const detail = page.locator(`[data-testid="${TEST_IDS.detailList}"]`);
      await expect(detail).toBeVisible({ timeout: 15_000 });
      await expect(detail.locator(`[data-testid="${TEST_IDS.detailEmpty}"]`)).toHaveCount(
        0,
        { timeout: 20_000 },
      );
      await expect(
        detail.locator(`[data-testid^="${TEST_IDS.detailRowPrefix}"]`).first(),
      ).toBeVisible({ timeout: 20_000 });

      const unexpected = jsErrors.filter((t) => !isKnownExplorerSitesConsoleNoise(t));
      expect(unexpected, `console/page errors: ${unexpected.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "UI: Assets root lists children in detail-list when REST has children",
    { tag: ["@explorer-sites-assets-tree-list", "@explorer"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const probe = await page.request.get(ASSETS_URL, {
        headers: adminBasicAuthHeaders(),
      });
      expect(probe.status()).toBe(200);
      const restNames = pathItemNames(await probe.json());

      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });

      const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
      await expect(tree).toBeVisible({ timeout: 20_000 });
      const assetsRoot = treeRootLocator(page, "Assets");
      await expect(assetsRoot.first()).toBeVisible({ timeout: 20_000 });
      await assetsRoot.first().locator('[role="treeitem"]').first().click();

      const detail = page.locator(`[data-testid="${TEST_IDS.detailList}"]`);
      await expect(detail).toBeVisible({ timeout: 15_000 });
      if (restNames.length > 0) {
        await expect(
          detail.locator(`[data-testid="${TEST_IDS.detailEmpty}"]`),
        ).toHaveCount(0, { timeout: 20_000 });
        await expect(
          detail.locator(`[data-testid^="${TEST_IDS.detailRowPrefix}"]`).first(),
        ).toBeVisible({ timeout: 20_000 });
      }
    },
  );
});
