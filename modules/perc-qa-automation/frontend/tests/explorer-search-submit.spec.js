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
 * Playwright surface: #3617 / parent #3102 / epic #2400 —
 * Explorer simple/extended search SUBMIT on the product route.
 *
 * <p>Opens {@code spa.jsp?entry=explorer} (not {@code searchModern.jsp}),
 * toggles SearchPanel, submits free-text (and extended chrome when present),
 * and requires HTTP 200 plus results or documented empty-success. Do
 * <strong>not</strong> soft-skip when SearchPanel is mounted. Do not claim
 * gap-matrix Present.</p>
 *
 * <p>Tags: {@code @explorer-search-submit} {@code @explorer} {@code @search}
 * {@code @smoke}</p>
 *
 * <h3>Unattended surface (QA mode)</h3>
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/explorer-search-submit.spec.js
 *   npm run test:surface -- --tag explorer-search-submit
 *   npm run test:surface:list -- --path tests/explorer-search-submit.spec.js
 *   python docker/scripts/perc-devctl.py qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  TEST_IDS,
  explorerEntryUrl,
  openViewMenu,
  isPilotSearchJsp,
  isExtendedResultsPost,
  isSearchSubmitSuccessStatus,
  terminalSuccessSelector,
  classifySubmitOutcome,
  isSuccessfulSubmitOutcome,
} = require("./helpers/explorer-search-submit");

/**
 * Collect uncaught page errors for the C5 zero-errors gate.
 *
 * @param {import("@playwright/test").Page} page
 * @returns {string[]}
 */
function attachPageErrors(page) {
  const pageErrors = [];
  page.on("pageerror", (err) => {
    pageErrors.push(String(err && err.message ? err.message : err));
  });
  page.on("console", (msg) => {
    if (msg.type() !== "error") {
      return;
    }
    const text = String(msg.text() || "");
    if (
      /Failed to load resource: the server responded with a status of (404|400)/i.test(
        text,
      )
    ) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

test.describe("Explorer search submit on product route (#3617 / #3102)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("product Explorer SearchPanel submit is 200 results or empty-success @explorer-search-submit @explorer @search @smoke", async ({
    page,
  }) => {
    const pageErrors = attachPageErrors(page);
    const url = explorerEntryUrl(BASE_URL);
    expect(isPilotSearchJsp(url), "operator path must not be searchModern.jsp").toBe(
      false,
    );

    await page.goto(url, { waitUntil: "networkidle" });
    await expect(
      page.locator(`[data-testid="${TEST_IDS.shell}"]`),
    ).toBeVisible({ timeout: 20_000 });
    expect(
      isPilotSearchJsp(page.url()),
      "Explorer must stay on spa.jsp?entry=explorer",
    ).toBe(false);

    await openViewMenu(page);
    const toggle = page.locator(`[data-testid="${TEST_IDS.toggleSearch}"]`);
    await expect(toggle).toBeVisible();
    await toggle.click();

    const panel = page.locator(`[data-testid="${TEST_IDS.searchPanel}"]`);
    await expect(
      panel,
      "SearchPanel must mount on the product Explorer shell — no skip",
    ).toBeVisible({ timeout: 10_000 });
    await expect(
      page.locator(`[data-testid="${TEST_IDS.searchInput}"]`),
    ).toBeVisible();
    await expect(
      page.locator(`[data-testid="${TEST_IDS.searchSubmit}"]`),
    ).toBeVisible();

    const responsePromise = page.waitForResponse(
      (res) => isExtendedResultsPost(res.url(), res.request().method()),
      { timeout: 30_000 },
    );

    const input = page.locator(`[data-testid="${TEST_IDS.searchInput}"]`);
    await input.fill("a");
    await page.locator(`[data-testid="${TEST_IDS.searchSubmit}"]`).click();

    const response = await responsePromise;
    expect(
      isSearchSubmitSuccessStatus(response.status()),
      `extendedresults must be HTTP 200 (empty-success allowed); got ${response.status()}`,
    ).toBe(true);

    await expect(page.locator(terminalSuccessSelector()).first()).toBeVisible({
      timeout: 15_000,
    });

    const hasResults = await page
      .locator(`[data-testid="${TEST_IDS.results}"]`)
      .isVisible()
      .catch(() => false);
    const hasEmpty = await page
      .locator(`[data-testid="${TEST_IDS.empty}"]`)
      .isVisible()
      .catch(() => false);
    const hasError = await page
      .locator(`[data-testid="${TEST_IDS.error}"]`)
      .isVisible()
      .catch(() => false);

    const kind = classifySubmitOutcome({
      status: response.status(),
      hasResults,
      hasEmpty,
      hasError,
    });
    expect(
      isSuccessfulSubmitOutcome(kind),
      `submit must be results or empty-success, not ${kind}`,
    ).toBe(true);
    expect(hasError, "error chrome is not empty-success when panel is mounted").toBe(
      false,
    );

    // Extended field chrome is optional; when present, submit it too.
    const extended = page.locator(`[data-testid="${TEST_IDS.extended}"]`);
    if ((await extended.count()) > 0 && (await extended.isVisible())) {
      const extResponsePromise = page.waitForResponse(
        (res) => isExtendedResultsPost(res.url(), res.request().method()),
        { timeout: 30_000 },
      );
      await page.locator(`[data-testid="${TEST_IDS.searchSubmit}"]`).click();
      const extRes = await extResponsePromise;
      expect(
        isSearchSubmitSuccessStatus(extRes.status()),
        `extended chrome submit must be HTTP 200; got ${extRes.status()}`,
      ).toBe(true);
      await expect(page.locator(terminalSuccessSelector()).first()).toBeVisible({
        timeout: 15_000,
      });
    }

    expect(isPilotSearchJsp(page.url())).toBe(false);
    expect(pageErrors, "uncaught pageerror on Explorer search submit").toEqual(
      [],
    );
  });
});
