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
 * Playwright surface: #4970 — Explorer Create Asset in the selected folder.
 *
 * <p>Run (QA mode after perc-devctl qa-up):
 * {@code npm run test:surface -- --path tests/explorer-create-asset.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  CREATE_TEST_IDS,
  explorerProductCreateFolderUrl,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-create-folder");

const TAGS = ["@explorer-create-asset", "@explorer", "@smoke"];

function isItemCreateUrl(url) {
  return String(url).includes("/itemmanagement/item/create");
}

test.describe("Explorer Create Asset in the selected folder (#4970)", () => {
  test(
    "UI: confirm creates an asset; cancel and a blank name do not",
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
      page.on("request", (req) => {
        if (isItemCreateUrl(req.url()) && req.method() === "POST") {
          creates += 1;
        }
      });

      await loginAsAdmin(page);
      await page.goto(explorerProductCreateFolderUrl(BASE_URL), {
        waitUntil: "domcontentloaded",
      });
      const shell = page.locator(`[data-testid="${CREATE_TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 30_000 });
      const assets = page
        .locator(
          '[data-testid="tree-node-/Assets/"], [data-testid="tree-node-/Assets"]',
        )
        .first();
      await expect(assets).toBeVisible({ timeout: 30_000 });
      await assets.click({ force: true });

      const createBtn = page.locator('[data-testid="action-create-asset"]');
      await expect(createBtn).toBeEnabled({ timeout: 20_000 });
      await createBtn.click();
      const dialog = page.locator('[data-testid="explorer-create-asset"]');
      await expect(dialog).toBeVisible();
      await page.locator('[data-testid="explorer-create-asset-confirm"]').click();
      await expect(page.locator('[data-testid="explorer-create-asset-error"]')).toBeVisible();
      expect(creates).toBe(0);

      await page.locator('[data-testid="explorer-create-asset-cancel"]').click();
      await expect(dialog).toBeHidden();
      expect(creates).toBe(0);

      const assetName = `a4970${Date.now()}`;
      await createBtn.click();
      await expect(dialog).toBeVisible();
      const typeSelect = page.locator('[data-testid="explorer-create-asset-type"]');
      await expect
        .poll(
          async () =>
            typeSelect.locator("option").evaluateAll((els) =>
              els.map((el) => String(el.value || "").trim()).filter(Boolean),
            ),
          { timeout: 20_000 },
        )
        .not.toEqual([]);
      await page.locator('[data-testid="explorer-create-asset-name"]').fill(assetName);

      const createResp = page.waitForResponse(
        (res) =>
          isItemCreateUrl(res.url()) && res.request().method() === "POST",
        { timeout: 40_000 },
      );
      await page.locator('[data-testid="explorer-create-asset-confirm"]').click();
      const res = await createResp;
      const bodyText = await res.text().catch(() => "");
      expect(res.status(), bodyText).toBe(200);
      expect(bodyText, bodyText).toContain(assetName);
      await expect(dialog).toBeHidden({ timeout: 15_000 });
      const refresh = page.getByRole("button", { name: "Refresh the current folder list" });
      await refresh.click();
      const list = page.locator(`[data-testid="${CREATE_TEST_IDS.detailList}"]`);
      const nameCell = list.locator(`[data-testid^="detail-cell-name-"]`, {
        hasText: assetName,
      });
      await expect(nameCell).toBeVisible({ timeout: 20_000 });

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
