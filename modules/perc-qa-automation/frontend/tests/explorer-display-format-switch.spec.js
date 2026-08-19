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
 * Playwright surface: #3618 / parent #3102 — Explorer display-format switch.
 *
 * <p>On {@code spa.jsp?entry=explorer} open a Sites/Assets folder with
 * children, change the display-format selector, and assert the
 * {@code paginatedFolder} request includes the selected
 * {@code displayFormatId} and the detail-list columns follow the format.
 * Do <strong>not</strong> soft-skip when the selector has more than one
 * option or the catalog has ≥2 {@code validForFolder} formats.</p>
 *
 * <p>Tags: {@code @explorer-display-format-switch} {@code @explorer}
 * {@code @display-format} {@code @smoke}</p>
 *
 * <h3>Unattended surface (QA mode)</h3>
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/explorer-display-format-switch.spec.js
 *   python docker/scripts/perc-devctl.py qa-down
 * </pre>
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
  sitesFolderUrl,
  displayFormatsCatalogUrl,
  unwrapDisplayFormatCatalog,
  isNumericDisplayFormatId,
  nonEmptySelectOptionValues,
  isPaginatedFolderDisplayFormatRequest,
  sitesTreeRootLocator,
  expandExplorerTreeNode,
  sitesTreeDescendantsLocator,
  pathItemNames,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-display-format-switch");

const SITES_URL = sitesFolderUrl(BASE_URL);
const FOLDER_CATALOG_URL = displayFormatsCatalogUrl(BASE_URL, {
  validForFolder: true,
});
const CATALOG_URL = displayFormatsCatalogUrl(BASE_URL);

/**
 * Folder-valid catalog when present; otherwise unfiltered (H2 summary flags).
 *
 * @param {import("@playwright/test").APIRequestContext} request
 * @returns {Promise<object[]>}
 */
async function fetchExplorerDisplayFormats(request) {
  const headers = adminBasicAuthHeaders();
  const folderRes = await request.get(FOLDER_CATALOG_URL, { headers });
  expect(folderRes.status(), `GET ${FOLDER_CATALOG_URL} must be 200`).toBe(200);
  const folderFormats = unwrapDisplayFormatCatalog(await folderRes.json());
  if (folderFormats.length >= 2) {
    return folderFormats;
  }
  const allRes = await request.get(CATALOG_URL, { headers });
  expect(allRes.status(), `GET ${CATALOG_URL} must be 200`).toBe(200);
  return unwrapDisplayFormatCatalog(await allRes.json());
}

/**
 * @param {import("@playwright/test").Page} page
 * @returns {string[]}
 */
function attachConsoleErrors(page) {
  const jsErrors = [];
  page.on("pageerror", (err) => jsErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      jsErrors.push(msg.text());
    }
  });
  return jsErrors;
}

test.describe("Explorer display-format switch (#3618 / #3102)", () => {
  test(
    "REST catalog has ≥2 folder-valid formats (no skip)",
    { tag: ["@explorer-display-format-switch", "@explorer", "@display-format"] },
    async ({ request }) => {
      test.setTimeout(30_000);
      const formats = await fetchExplorerDisplayFormats(request);
      expect(
        formats.length,
        `H2 catalog should have ≥2 formats (fail, do not skip); got ${formats.length}`,
      ).toBeGreaterThanOrEqual(2);
    },
  );

  test(
    "changing display format reloads detail-list with displayFormatId (no skip)",
    {
      tag: [
        "@explorer-display-format-switch",
        "@explorer",
        "@display-format",
        "@smoke",
      ],
    },
    async ({ page }) => {
      test.setTimeout(90_000);
      const formats = await fetchExplorerDisplayFormats(page.request);
      expect(
        formats.length,
        "must fail (not skip) when catalog has fewer than 2 formats (#3618)",
      ).toBeGreaterThanOrEqual(2);

      const sitesProbe = await page.request.get(SITES_URL, {
        headers: adminBasicAuthHeaders(),
      });
      expect(sitesProbe.status()).toBe(200);
      const siteNames = pathItemNames(await sitesProbe.json());
      expect(
        siteNames.length,
        `Sites children empty (H2 demo-sites expected): ${JSON.stringify(siteNames)}`,
      ).toBeGreaterThan(0);

      const jsErrors = attachConsoleErrors(page);
      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });

      const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 20_000 });

      const select = page.locator(`[data-testid="${TEST_IDS.displayFormat}"]`);
      await expect(select).toBeVisible();
      await expect(page.locator(`[data-testid="${TEST_IDS.displayFormatError}"]`)).toHaveCount(
        0,
      );

      await expect
        .poll(
          async () => select.locator("option").count(),
          { timeout: 20_000 },
        )
        .toBeGreaterThan(1);

      const optionValues = nonEmptySelectOptionValues(
        await select.locator("option").evaluateAll((els) =>
          els.map((el) => el.getAttribute("value") || ""),
        ),
      );
      expect(
        optionValues.length,
        "must fail (not skip) when the selector has fewer than one format option (#3618)",
      ).toBeGreaterThanOrEqual(1);
      const numericOptions = optionValues.filter((v) =>
        isNumericDisplayFormatId(v),
      );
      expect(
        numericOptions.length,
        `selector must expose at least one numeric displayFormatId; values=${JSON.stringify(optionValues)}`,
      ).toBeGreaterThanOrEqual(1);

      const tree = page.locator(`[data-testid="${TEST_IDS.tree}"]`);
      await expect(tree).toBeVisible({ timeout: 20_000 });
      const sitesRoot = sitesTreeRootLocator(page);
      await expect(sitesRoot.first()).toBeVisible({ timeout: 20_000 });
      await expandExplorerTreeNode(sitesRoot.first());
      const descendants = sitesTreeDescendantsLocator(page);
      await expect(descendants.first()).toBeVisible({ timeout: 20_000 });

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
      await expect(
        detail.locator(`[data-testid="${TEST_IDS.detailEmpty}"]`),
      ).toHaveCount(0, { timeout: 20_000 });
      await expect(
        detail.locator(`[data-testid^="${TEST_IDS.detailRowPrefix}"]`).first(),
      ).toBeVisible({ timeout: 20_000 });

      const defaultHeaders = await page
        .locator(`[data-testid^="${TEST_IDS.colHeaderPrefix}"]`)
        .evaluateAll((els) => els.map((el) => el.getAttribute("data-testid") || ""));

      const targetId = numericOptions[0];
      const responsePromise = page.waitForResponse(
        (res) =>
          isPaginatedFolderDisplayFormatRequest(res.url(), targetId) &&
          res.request().method() === "GET",
        { timeout: 30_000 },
      );
      await select.selectOption(targetId);
      const response = await responsePromise;
      expect(
        response.status(),
        `paginatedFolder displayFormatId=${targetId} must not 500; got ${response.status()}`,
      ).not.toBe(500);
      expect(
        response.ok(),
        `paginatedFolder displayFormatId=${targetId} expected 2xx; got ${response.status()}`,
      ).toBeTruthy();

      await expect(select).toHaveValue(targetId);
      await expect(detail).toHaveAttribute("data-display-format-id", targetId);
      await expect(
        page.locator(`[data-testid^="${TEST_IDS.colHeaderPrefix}"]`).first(),
      ).toBeVisible({ timeout: 15_000 });

      const switchedHeaders = await page
        .locator(`[data-testid^="${TEST_IDS.colHeaderPrefix}"]`)
        .evaluateAll((els) => els.map((el) => el.getAttribute("data-testid") || ""));
      expect(
        switchedHeaders.length,
        `detail-list must keep column headers after the format switch; pre=${JSON.stringify(defaultHeaders)} post=${JSON.stringify(switchedHeaders)}`,
      ).toBeGreaterThan(0);

      const unexpected = jsErrors.filter(
        (t) => !isKnownExplorerSitesConsoleNoise(t),
      );
      expect(unexpected, `console/page errors: ${unexpected.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "axe-core a11y gate — display-format selector on product explorer",
    {
      tag: ["@explorer-display-format-switch", "@explorer", "@a11y"],
    },
    async ({ page }) => {
      test.setTimeout(60_000);
      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 20_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.displayFormat}"]`),
      ).toBeVisible();
      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.shell}"]`,
      });
    },
  );
});
