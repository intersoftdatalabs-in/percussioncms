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
 * Playwright: #4672 / parent #4530 — Explorer folder security ACL save.
 *
 * Tags: @explorer-folder-acl-save @explorer @folder @smoke
 *
 * npm run test:surface -- --path tests/explorer-folder-acl-save.spec.js
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  ACL_TEST_IDS,
  explorerProductAclUrl,
  saveFolderPropertiesUrl,
  isSaveFolderPropertiesUrl,
  uniqueAclPrincipalName,
  missingFolderSaveBody,
  assetsFolderUrl,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-folder-acl-save");

const TAGS = ["@explorer-folder-acl-save", "@explorer", "@folder", "@smoke"];

async function getStatus(request, url) {
  const headers = adminBasicAuthHeaders();
  const res = await request.get(url, { headers });
  return res.status();
}

test.describe("Explorer folder security ACL save (#4672 / #4530)", () => {
  test(
    "REST: Assets parent exists (no skip)",
    { tag: TAGS },
    async ({ request }) => {
      test.setTimeout(30_000);
      const assetsStatus = await getStatus(request, assetsFolderUrl(BASE_URL));
      expect(
        assetsStatus,
        `H2 demo-sites should expose /Assets; got Assets=${assetsStatus}`,
      ).toBe(200);
    },
  );

  test(
    "REST: missing folder saveFolderProperties is HTTP 404 not 200 (#4672)",
    { tag: TAGS },
    async ({ request }) => {
      test.setTimeout(30_000);
      const headers = {
        ...adminBasicAuthHeaders(),
        "Content-Type": "application/json",
      };
      const res = await request.post(saveFolderPropertiesUrl(BASE_URL), {
        headers,
        data: missingFolderSaveBody(),
      });
      expect(
        res.status(),
        `missing folder ACL save must not succeed; body=${(await res.text()).slice(0, 400)}`,
      ).toBe(404);
    },
  );

  test(
    "UI: save ACL principal on spa.jsp?entry=explorer",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          const text = msg.text();
          if (isKnownExplorerSitesConsoleNoise(text)) {
            return;
          }
          if (/Failed to load resource/i.test(text)) {
            return;
          }
          jsErrors.push(text);
        }
      });

      /** @type {number | undefined} */
      let saveStatus;
      page.on("response", (res) => {
        if (isSaveFolderPropertiesUrl(res.url()) && res.request().method() !== "OPTIONS") {
          saveStatus = res.status();
        }
      });

      await loginAsAdmin(page);
      await page.goto(explorerProductAclUrl(BASE_URL), {
        waitUntil: "networkidle",
      });

      const shell = page.locator(`[data-testid="${ACL_TEST_IDS.shell}"]`);
      await expect(shell).toBeVisible({ timeout: 20_000 });
      await expect(
        page.locator(`[data-testid="${ACL_TEST_IDS.tree}"]`),
      ).toBeVisible({ timeout: 20_000 });

      const assetsRoot = treeRootLocator(page, "Assets");
      await expect(assetsRoot.first()).toBeVisible({ timeout: 20_000 });
      await assetsRoot.first().locator('[role="treeitem"]').click();

      const toggle = page.locator(`[data-testid="${ACL_TEST_IDS.toggleSecurity}"]`);
      await expect(toggle).toBeVisible({ timeout: 10_000 });
      await toggle.click();

      const panel = page.locator(`[data-testid="${ACL_TEST_IDS.securityPanel}"]`);
      await expect(panel).toBeVisible({ timeout: 25_000 });

      const principal = uniqueAclPrincipalName();
      await page.locator(`[data-testid="${ACL_TEST_IDS.adminAdd}"]`).click();
      await page
        .locator(`[data-testid="${ACL_TEST_IDS.adminInput}"]`)
        .fill(principal);
      await page
        .locator(`[data-testid="${ACL_TEST_IDS.adminAddConfirm}"]`)
        .click();

      const save = page.locator(`[data-testid="${ACL_TEST_IDS.save}"]`);
      await expect(save).toBeEnabled();
      await save.click();

      await expect(
        page.locator(`[data-testid="${ACL_TEST_IDS.securityError}"]`),
      ).toHaveCount(0);
      await expect(
        page.locator(`[data-testid="${ACL_TEST_IDS.dirty}"]`),
      ).toHaveText("○", { timeout: 20_000 });
      expect(
        saveStatus,
        "saveFolderProperties must be HTTP 200 on successful ACL save",
      ).toBe(200);

      await expectNoSeriousA11yViolations(page, {
        scope: '[data-testid="content-explorer-shell"]',
      });
      expect(jsErrors, `console/page errors: ${jsErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
