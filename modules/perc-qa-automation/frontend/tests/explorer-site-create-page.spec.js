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
 * Playwright surface: #3520 / parent #3512 — Page site create from type picker.
 *
 * <p>Coverage:</p>
 * <ul>
 *   <li>Type picker: Page is selectable; Virtual stays enabled on the cluster
 *       union (no typeUnavailable banner; Next remains enabled)</li>
 *   <li>Page flow: details (managed nav locked) → page/base template → confirm</li>
 *   <li>Traditional still skips the template step</li>
 *   <li>Live Page create persists on POST /sitemanage/site/ and lists under Sites</li>
 * </ul>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-site-create-page.spec.js}
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
  explorerSpaUrl,
  sitesFolderUrl,
  uniqueQaSiteName,
  createSiteMissingSkipReason,
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-sites-list-create");
const { pathItemNames } = require("./helpers/demo-sites");

const SITES_URL = sitesFolderUrl(BASE_URL);

/**
 * @param {import("@playwright/test").APIRequestContext} request
 * @returns {Promise<string[]>}
 */
async function fetchSitesChildNames(request) {
  const headers = adminBasicAuthHeaders();
  const res = await request.get(SITES_URL, { headers });
  expect(res.status(), `GET ${SITES_URL} must be 200`).toBe(200);
  return pathItemNames(await res.json());
}

/**
 * @param {import("@playwright/test").APIRequestContext} request
 * @param {string} siteName
 * @returns {Promise<{ pageBased?: boolean } | null>}
 */
async function fetchSiteSummary(request, siteName) {
  const headers = adminBasicAuthHeaders();
  const url = `${BASE_URL.replace(/\/$/, "")}/Rhythmyx/services/sitemanage/site/${encodeURIComponent(siteName)}`;
  const res = await request.get(url, { headers });
  if (res.status() !== 200) {
    return null;
  }
  const body = await res.json();
  const site = body.Site || body.site || body;
  return site && typeof site === "object" ? site : null;
}

/**
 * @param {import("@playwright/test").Page} page
 * @returns {Promise<boolean>}
 */
async function ensureCreateSiteMenuOrSkip(page) {
  await page.locator(`[data-testid="${TEST_IDS.menuContent}"]`).click();
  const menuItem = page.locator(`[data-testid="${TEST_IDS.createSiteMenu}"]`);
  if ((await menuItem.count()) === 0) {
    test.skip(createSiteMissingSkipReason());
    return false;
  }
  await expect(menuItem).toBeVisible({ timeout: 5_000 });
  return true;
}

/**
 * @param {import("@playwright/test").Page} page
 */
function attachConsoleGate(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on("pageerror", (err) => pageErrors.push(String(err)));
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = msg.text();
    if (!isKnownExplorerSitesConsoleNoise(text)) {
      consoleErrors.push(text);
    }
  });
  return { pageErrors, consoleErrors };
}

test.describe("Explorer Page site create (#3520 / #3512)", () => {
  test(
    "type picker: Page selectable, Virtual enabled, Traditional skips template",
    { tag: ["@explorer-site-create-page", "@explorer", "@sites"] },
    async ({ page }) => {
      test.setTimeout(90_000);
      const { pageErrors, consoleErrors } = attachConsoleGate(page);
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
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepType}"]`),
      ).toBeVisible({ timeout: 10_000 });

      const unavailable = page.locator(
        `[data-testid="${TEST_IDS.typeUnavailable}"]`,
      );
      await page.locator(`[data-testid="${TEST_IDS.typeVirtual}"]`).check();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.virtualNote}"]`),
      ).toBeVisible();
      await expect(unavailable).toHaveCount(0);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.next}"]`),
      ).toBeEnabled();

      await page.locator(`[data-testid="${TEST_IDS.typeTraditional}"]`).check();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.traditionalNote}"]`),
      ).toBeVisible();
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepDetails}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.managedNav}"]`),
      ).toBeEnabled();
      await page.locator(`[data-testid="${TEST_IDS.siteName}"]`).fill("TradChk");
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepTemplate}"]`),
      ).toHaveCount(0);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepConfirm}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.confirmTemplateName}"]`),
      ).toHaveCount(0);

      await page.locator(`[data-testid="${TEST_IDS.back}"]`).click();
      await page.locator(`[data-testid="${TEST_IDS.back}"]`).click();
      await page.locator(`[data-testid="${TEST_IDS.typePage}"]`).check();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.pageNote}"]`),
      ).toBeVisible();
      await expect(unavailable).toHaveCount(0);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.next}"]`),
      ).toBeEnabled();
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.managedNav}"]`),
      ).toBeDisabled();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.managedNav}"]`),
      ).toBeChecked();
      await page.locator(`[data-testid="${TEST_IDS.siteName}"]`).fill("PageChk");
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepTemplate}"]`),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.templateName}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.baseTemplate}"]`),
      ).toBeVisible({ timeout: 20_000 });

      expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      expect(consoleErrors, `console error: ${consoleErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );

  test(
    "Create Site happy path: Page type with template and managed nav",
    { tag: ["@explorer-site-create-page", "@explorer", "@sites"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const { pageErrors, consoleErrors } = attachConsoleGate(page);
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
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepType}"]`),
      ).toBeVisible({ timeout: 10_000 });

      await page.locator(`[data-testid="${TEST_IDS.typePage}"]`).check();
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();

      const siteName = uniqueQaSiteName("QaPage");
      await page.locator(`[data-testid="${TEST_IDS.siteName}"]`).fill(siteName);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.managedNav}"]`),
      ).toBeDisabled();
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();

      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepTemplate}"]`),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.baseTemplate}"]`),
      ).toBeVisible({ timeout: 20_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.templateName}"]`),
      ).not.toHaveValue("");
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();

      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepConfirm}"]`),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.confirmSummary}"]`),
      ).toContainText(siteName);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.confirmType}"]`),
      ).toContainText(/Page/i);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.confirmManagedNav}"]`),
      ).toContainText(/Yes/i);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.confirmTemplateName}"]`),
      ).toBeVisible();

      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepProgress}"]`),
      ).toBeVisible({ timeout: 10_000 });
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
              return "error";
            }
            const names = await fetchSitesChildNames(page.request);
            const hit = names.some(
              (n) => String(n).toLowerCase() === siteName.toLowerCase(),
            );
            return hit ? "ok" : "pending";
          },
          { timeout: 60_000 },
        )
        .toBe("ok");

      const summary = await fetchSiteSummary(page.request, siteName);
      expect(summary, `GET sitemanage/site/${siteName}`).toBeTruthy();
      if (summary && typeof summary.pageBased !== "undefined") {
        expect(
          summary.pageBased === true || summary.pageBased === "true",
          `pageBased on created site: ${JSON.stringify(summary)}`,
        ).toBe(true);
      }

      expect(pageErrors, `pageerror: ${pageErrors.join(" | ")}`).toEqual([]);
      expect(consoleErrors, `console error: ${consoleErrors.join(" | ")}`).toEqual(
        [],
      );
    },
  );
});
