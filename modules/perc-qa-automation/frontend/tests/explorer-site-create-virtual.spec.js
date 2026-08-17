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
 * Playwright surface: #3521 / parent #3512 — Virtual site create from type picker.
 *
 * <p>Virtual flow: type → details (name/description only, no managed nav,
 * no page template) → confirm (optional Git root) → progress. Create is
 * traditional POST without nav, then PUT /services/sites/{name}/virtual
 * when a root path is provided.</p>
 *
 * Surface-filtered QA mode:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=…
 *     npm run test:surface -- --path tests/explorer-site-create-virtual.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const {
  TEST_IDS,
  explorerSpaUrl,
  uniqueQaSiteName,
  createSiteMissingSkipReason,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-sites-list-create");

/**
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<boolean>}
 */
async function ensureCreateSiteMenuOrSkip(page) {
  await page.locator(`[data-testid="${TEST_IDS.menuContent}"]`).click();
  const menu = page.locator(`[data-testid="${TEST_IDS.createSiteMenu}"]`);
  if ((await menu.count()) === 0) {
    test.skip(true, createSiteMissingSkipReason());
    return false;
  }
  return true;
}

function virtualPropertiesUrl(siteName) {
  const root = String(BASE_URL || "").replace(/\/$/, "");
  return `${root}/Rhythmyx/services/sites/${encodeURIComponent(siteName)}/virtual`;
}

test.describe("Explorer Virtual site create (#3521)", () => {
  test(
    "Virtual type hides managed nav and page template; PUT virtual after create",
    { tag: ["@explorer-site-create-virtual", "@explorer", "@sites"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          jsErrors.push(msg.text());
        }
      });

      await loginAsAdmin(page);
      await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 20_000 });

      const present = await ensureCreateSiteMenuOrSkip(page);
      if (!present) {
        return;
      }

      await page.locator(`[data-testid="${TEST_IDS.createSiteMenu}"]`).click();
      const wizard = page.locator(`[data-testid="${TEST_IDS.wizard}"]`);
      await expect(wizard).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepType}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.typeVirtual}"]`),
      ).toBeEnabled();
      await page.locator(`[data-testid="${TEST_IDS.typeVirtual}"]`).check();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.virtualNote}"]`),
      ).toBeVisible();

      const next = page.locator(`[data-testid="${TEST_IDS.next}"]`);
      await expect(next).toBeEnabled();
      await next.click();

      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepDetails}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.managedNav}"]`),
      ).toHaveCount(0);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.templateName}"]`),
      ).toHaveCount(0);

      const siteName = uniqueQaSiteName("QaVirt");
      await page.locator(`[data-testid="${TEST_IDS.siteName}"]`).fill(siteName);
      await next.click();

      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepConfirm}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.confirmManagedNav}"]`),
      ).toHaveCount(0);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.virtualSourceNote}"]`),
      ).toBeVisible();
      await page
        .locator(`[data-testid="${TEST_IDS.virtualRoot}"]`)
        .fill("/opt/Percussion");
      await next.click();

      const run = page.locator(`[data-testid="${TEST_IDS.run}"]`);
      await expect(run).toBeEnabled({ timeout: 5_000 });
      await run.click();

      await expect
        .poll(
          async () => {
            const errText = await page
              .locator(`[data-testid="${TEST_IDS.wizard}"] [role="status"]`)
              .allInnerTexts()
              .catch(() => []);
            const failed = errText.some((t) =>
              /error|failed|invalid|500|rolled back/i.test(String(t || "")),
            );
            if (failed) {
              return `error:${errText.join(" | ")}`;
            }
            const headers = adminBasicAuthHeaders();
            const res = await page.request.get(virtualPropertiesUrl(siteName), {
              headers,
            });
            if (res.status() !== 200) {
              return `pending-http-${res.status()}`;
            }
            const payload = await res.json();
            const root =
              (payload && payload.VirtualSiteProperties) || payload || {};
            const kind = String(root.sourceKind || "").toLowerCase();
            return kind === "git-filesystem" ? "ok" : `kind:${kind}`;
          },
          { timeout: 60_000 },
        )
        .toBe("ok");

      const unexpected = jsErrors.filter(
        (t) => !isKnownExplorerSitesConsoleNoise(t),
      );
      expect(
        unexpected,
        `console/page errors: ${unexpected.join(" | ")}`,
      ).toEqual([]);
    },
  );
});
