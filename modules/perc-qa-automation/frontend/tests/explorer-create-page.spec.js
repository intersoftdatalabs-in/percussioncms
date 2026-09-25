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
 * Playwright surface: #4874 — Explorer Create Page in the selected folder.
 *
 * <p>Run (QA mode after perc-devctl qa-up):
 * {@code npm run test:surface -- --path tests/explorer-create-page.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  CREATE_TEST_IDS,
  explorerProductCreateFolderUrl,
  isKnownExplorerSitesConsoleNoise,
  sitesTreeRootLocator,
} = require("./helpers/explorer-create-folder");
const { expandExplorerTreeNode } = require("./helpers/explorer-sites-list-create");

const TAGS = ["@explorer-create-page", "@explorer", "@smoke"];

function isItemCreateUrl(url) {
  return String(url).includes("/itemmanagement/item/create");
}

test.describe("Explorer Create Page in the selected folder (#4874)", () => {
  test(
    "UI: confirm creates a page; cancel and a blank name do not",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(180_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          jsErrors.push(msg.text());
        }
      });

      let creates = 0;
      const folderLoads = [];
      page.on("request", (req) => {
        if (isItemCreateUrl(req.url()) && req.method() === "POST") {
          creates += 1;
        }
      });
      page.on("response", async (res) => {
        if (!res.url().includes("paginatedFolder")) {
          return;
        }
        let text = "";
        try {
          text = (await res.text()).slice(0, 240);
        } catch {
          text = "";
        }
        folderLoads.push(`${res.status()} ${text}`);
      });

      await loginAsAdmin(page);
      await page.goto(explorerProductCreateFolderUrl(BASE_URL), {
        waitUntil: "domcontentloaded",
      });
      const shell = page.locator(`[data-testid="${CREATE_TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 30_000 });
      const sites = sitesTreeRootLocator(page).first();
      await expect(sites).toBeVisible({ timeout: 30_000 });
      await expandExplorerTreeNode(sites);
      const siteFolder = page
        .locator('[data-testid="tree-node-/Sites/Enterprise_Investments/"]')
        .first();
      await expect(siteFolder).toBeVisible({ timeout: 20_000 });
      await expandExplorerTreeNode(siteFolder);
      const pagesFolder = page
        .locator(
          '[data-testid="tree-node-/Sites/Enterprise_Investments/Pages/"], [data-testid="tree-node-/Sites/EnterpriseInvestments/Pages/"]',
        )
        .first();
      await expect(pagesFolder).toBeVisible({ timeout: 20_000 });
      await pagesFolder.click({ force: true });

      const createBtn = page.locator('[data-testid="action-create-page"]');
      await expect(createBtn).toBeEnabled({ timeout: 20_000 });
      await createBtn.click();
      const dialog = page.locator('[data-testid="explorer-create-page"]');
      await expect(dialog).toBeVisible();
      await page.locator('[data-testid="explorer-create-page-confirm"]').click();
      await expect(page.locator('[data-testid="explorer-create-page-error"]')).toBeVisible();
      expect(creates).toBe(0);

      await page.locator('[data-testid="explorer-create-page-cancel"]').click();
      await expect(dialog).toBeHidden();
      expect(creates).toBe(0);

      const pageName = `p4874${Date.now()}`;
      await createBtn.click();
      await expect(dialog).toBeVisible();
      const typeSelect = page.locator('[data-testid="explorer-create-page-type"]');
      await expect(typeSelect.locator("option").first()).toBeAttached({
        timeout: 20_000,
      });
      const optionCount = await typeSelect.locator("option").count();
      expect(optionCount, "a page content type must be listed").toBeGreaterThan(0);
      await page.locator('[data-testid="explorer-create-page-name"]').fill(pageName);

      const createResp = page.waitForResponse(
        (res) =>
          isItemCreateUrl(res.url()) && res.request().method() === "POST",
        { timeout: 40_000 },
      );
      await page.locator('[data-testid="explorer-create-page-confirm"]').click();
      const res = await createResp;
      const bodyText = await res.text().catch(() => "");
      expect(res.status(), bodyText).toBe(200);
      expect(bodyText, bodyText).toContain(pageName);
      await expect(dialog).toBeHidden({ timeout: 15_000 });
      const refresh = page.getByRole("button", { name: "Refresh the current folder list" });
      await refresh.click();
      const list = page.locator(`[data-testid="${CREATE_TEST_IDS.detailList}"]`);
      await expect(list.getByText(pageName, { exact: false }), folderLoads.slice(-2).join("\n")).toBeVisible({
        timeout: 20_000,
      });

      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${CREATE_TEST_IDS.shell}"]`,
      });
      const relatedConsole = jsErrors.filter(
        (t) => !isKnownExplorerSitesConsoleNoise(t),
      );
      expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
    },
  );
});
