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
 * Playwright surface: #3522 / parent #3512 — Create Site type picker +
 * Traditional path (no page / base template prompt).
 *
 * <p>Covers Explorer Content → Create Site and Navigation New Site:</p>
 * <ul>
 *   <li>Type picker first (Traditional / Page / Virtual); Traditional default</li>
 *   <li>Page and Virtual stay on the type step with a clear message</li>
 *   <li>Traditional: details → confirm with no template-name / base-template</li>
 * </ul>
 *
 * <p>Tags: {@code @explorer-site-create-type-picker} {@code @explorer}
 * {@code @sites} {@code @architecture}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-site-create-type-picker.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerSpaUrl,
  openContentMenu,
  uniqueQaSiteName,
  createSiteMissingSkipReason,
  isKnownExplorerSitesConsoleNoise,
  advanceTraditionalTypeStep,
} = require("./helpers/explorer-sites-list-create");

function architectureUrl() {
  const q = new URLSearchParams({
    entry: "architecture",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * @param {import("@playwright/test").Page} page
 * @returns {Promise<boolean>}
 */
async function openExplorerCreateSiteOrSkip(page) {
  await loginAsAdmin(page);
  await page.goto(explorerSpaUrl(BASE_URL), { waitUntil: "networkidle" });
  await expect(
    page.locator(`[data-testid="${TEST_IDS.shell}"]`),
  ).toBeVisible({ timeout: 20_000 });
  await openContentMenu(page);
  const menuItem = page.locator(`[data-testid="${TEST_IDS.createSiteMenu}"]`);
  if ((await menuItem.count()) === 0) {
    test.skip(createSiteMissingSkipReason());
    return false;
  }
  await menuItem.click();
  await expect(
    page.locator(`[data-testid="${TEST_IDS.wizard}"]`),
  ).toBeVisible({ timeout: 10_000 });
  return true;
}

test.describe("Create Site type picker (#3522 / #3512)", () => {
  test(
    "Explorer: type picker defaults Traditional; Page/Virtual block Next",
    {
      tag: [
        "@explorer-site-create-type-picker",
        "@explorer",
        "@sites",
      ],
    },
    async ({ page }) => {
      test.setTimeout(90_000);
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

      const present = await openExplorerCreateSiteOrSkip(page);
      if (!present) {
        return;
      }

      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepType}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.typeTraditional}"]`),
      ).toBeChecked();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.typePage}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.typeVirtual}"]`),
      ).toBeVisible();

      const next = page.locator(`[data-testid="${TEST_IDS.next}"]`);
      await page.locator(`[data-testid="${TEST_IDS.typePage}"]`).check();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.typeUnavailable}"]`),
      ).toBeVisible();
      await expect(next).toBeDisabled();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepDetails}"]`),
      ).toHaveCount(0);

      await page.locator(`[data-testid="${TEST_IDS.typeVirtual}"]`).check();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.typeUnavailable}"]`),
      ).toBeVisible();
      await expect(next).toBeDisabled();

      await page.locator(`[data-testid="${TEST_IDS.typeTraditional}"]`).check();
      await expect(next).toBeEnabled();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.typeUnavailable}"]`),
      ).toHaveCount(0);

      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
      expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
    },
  );

  test(
    "Explorer: Traditional skips page/base template and reaches confirm",
    {
      tag: [
        "@explorer-site-create-type-picker",
        "@explorer",
        "@sites",
      ],
    },
    async ({ page }) => {
      test.setTimeout(90_000);
      const present = await openExplorerCreateSiteOrSkip(page);
      if (!present) {
        return;
      }

      await advanceTraditionalTypeStep(page);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.templateName}"]`),
      ).toHaveCount(0);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.baseTemplate}"]`),
      ).toHaveCount(0);

      const siteName = uniqueQaSiteName("QaType");
      await page.locator(`[data-testid="${TEST_IDS.siteName}"]`).fill(siteName);
      await page.locator(`[data-testid="${TEST_IDS.next}"]`).click();

      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepTemplate}"]`),
      ).toHaveCount(0);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.stepConfirm}"]`),
      ).toBeVisible({ timeout: 10_000 });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.confirmType}"]`),
      ).toContainText(/Traditional/i);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.confirmSummary}"]`),
      ).toContainText(siteName);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.confirmManagedNav}"]`),
      ).toContainText(/Yes/i);
    },
  );

  test(
    "Navigation New Site opens the same type picker",
    {
      tag: [
        "@explorer-site-create-type-picker",
        "@architecture",
        "@sites",
      ],
    },
    async ({ page }) => {
      test.setTimeout(90_000);
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));

      await loginAsAdmin(page);
      await page.goto(architectureUrl(), { waitUntil: "domcontentloaded" });
      await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
        timeout: 20_000,
      });
      const newSite = page.getByTestId("architecture-action-new-site");
      if ((await newSite.count()) === 0) {
        test.skip("BUG: Navigation New Site affordance not present in image.");
        return;
      }
      await expect(newSite).toBeEnabled();
      await newSite.click();
      await expect(page.getByTestId("architecture-new-site-panel")).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.stepType)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.typeTraditional)).toBeChecked();
      await expect(page.getByTestId(TEST_IDS.typePage)).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.typeVirtual)).toBeVisible();
      expect(pageErrors, pageErrors.join("\n")).toEqual([]);
    },
  );
});
