/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
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
 * Playwright surface: #5575 / parent #4530 — Explorer Search refuses a
 * NUL (U+0000) in the free-text query on {@code spa.jsp?entry=explorer}.
 * The search POST is not sent. The panel stays open. An ordinary query
 * still searches. Close and HTTP 400/403/409 are not success.
 *
 * <p>Peers: {@code tests/explorer-search-submit.spec.js},
 * {@code tests/explorer-rename-item-nul.spec.js}.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-search-query-nul.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
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
  searchQueryContainsNul,
} = require("./helpers/explorer-search-submit");
const {
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-copy-item");

const TAGS = ["@explorer-search-query-nul", "@explorer", "@search"];
const NUL_QUERY = `bad${String.fromCharCode(0)}query`;
const EXTENDED_ROUTE = "**/searchmanagement/search/get/extendedresults";

/**
 * Controlled input update, including U+0000. Code points cross the Playwright
 * bridge as numbers so a NUL is not dropped by JSON transport.
 *
 * @param {import("@playwright/test").Locator} locator
 * @param {string} value
 */
async function setInputValue(locator, value) {
  const codes = Array.from(value, (ch) => ch.codePointAt(0));
  await locator.evaluate((el, payload) => {
    const next = String.fromCodePoint(...payload);
    const proto = Object.getOwnPropertyDescriptor(
      window.HTMLInputElement.prototype,
      "value",
    );
    if (!proto || typeof proto.set !== "function") {
      throw new Error("input value setter missing");
    }
    const previous = el.value;
    proto.set.call(el, next);
    const tracker = el._valueTracker;
    if (tracker) {
      tracker.setValue(previous);
    }
    el.dispatchEvent(new Event("input", { bubbles: true }));
  }, codes);
}

async function openSearchPanel(page) {
  await openViewMenu(page);
  const toggle = page.locator(`[data-testid="${TEST_IDS.toggleSearch}"]`);
  await expect(toggle).toBeVisible();
  await toggle.click();
  const panel = page.locator(`[data-testid="${TEST_IDS.searchPanel}"]`);
  await expect(panel).toBeVisible({ timeout: 10_000 });
  return panel;
}

test.describe("Explorer search query refuses a NUL (#5575)", () => {
  test(
    "a NUL query does not POST; an ordinary query still searches",
    { tag: TAGS },
    async ({ page }) => {
      test.setTimeout(180_000);
      expect(searchQueryContainsNul(NUL_QUERY)).toBe(true);

      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          jsErrors.push(msg.text());
        }
      });

      let searchPosts = 0;
      page.on("request", (req) => {
        if (req.method() === "OPTIONS") {
          return;
        }
        if (isExtendedResultsPost(req.url(), req.method())) {
          searchPosts += 1;
        }
      });

      await loginAsAdmin(page);
      const url = explorerEntryUrl(BASE_URL);
      expect(isPilotSearchJsp(url)).toBe(false);
      await page.goto(url, { waitUntil: "networkidle" });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 20_000 });

      const panel = await openSearchPanel(page);
      const input = page.locator(`[data-testid="${TEST_IDS.searchInput}"]`);
      const submit = page.locator(`[data-testid="${TEST_IDS.searchSubmit}"]`);
      await expect(input).toBeVisible();
      const postsBeforeUi = searchPosts;

      await setInputValue(input, NUL_QUERY);
      await expect(submit).toBeEnabled();
      await submit.click();
      await expect(page.getByTestId(TEST_IDS.queryError)).toContainText(
        /cannot be stored/i,
      );
      await expect(panel).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.results)).toHaveCount(0);
      await expect(page.getByTestId(TEST_IDS.empty)).toHaveCount(0);
      expect(searchPosts, "NUL query must not POST extendedresults").toBe(
        postsBeforeUi,
      );

      await openViewMenu(page);
      await page.locator(`[data-testid="${TEST_IDS.toggleSearch}"]`).click();
      await expect(panel).toHaveCount(0);
      expect(searchPosts, "close must not POST").toBe(postsBeforeUi);

      await page.goto(explorerEntryUrl(BASE_URL), { waitUntil: "networkidle" });
      await expect(
        page.locator(`[data-testid="${TEST_IDS.shell}"]`),
      ).toBeVisible({ timeout: 20_000 });
      await expect(page.getByTestId(TEST_IDS.results)).toHaveCount(0);
      await expect(page.getByTestId(TEST_IDS.empty)).toHaveCount(0);
      expect(searchPosts, "reload must not POST a NUL query").toBe(
        postsBeforeUi,
      );

      const panelAgain = await openSearchPanel(page);
      const inputAgain = page.locator(
        `[data-testid="${TEST_IDS.searchInput}"]`,
      );
      const submitAgain = page.locator(
        `[data-testid="${TEST_IDS.searchSubmit}"]`,
      );

      let forcedStatus = 0;
      await page.route(EXTENDED_ROUTE, async (route) => {
        if (route.request().method() === "OPTIONS") {
          await route.continue();
          return;
        }
        if (forcedStatus) {
          await route.fulfill({
            status: forcedStatus,
            contentType: "text/plain",
            body: "no",
          });
          return;
        }
        await route.continue();
      });

      for (const status of [400, 403, 409]) {
        forcedStatus = status;
        const before = searchPosts;
        await inputAgain.fill("a");
        await submitAgain.click();
        await expect(page.getByTestId(TEST_IDS.error)).toBeVisible({
          timeout: 15_000,
        });
        expect(
          searchPosts,
          `HTTP ${status} must not be treated as success`,
        ).toBeGreaterThan(before);
        await expect(page.getByTestId(TEST_IDS.results)).toHaveCount(0);
        await expect(page.getByTestId(TEST_IDS.empty)).toHaveCount(0);
        const kind = classifySubmitOutcome({
          status,
          hasResults: false,
          hasEmpty: false,
          hasError: true,
        });
        expect(isSuccessfulSubmitOutcome(kind)).toBe(false);
      }

      forcedStatus = 0;
      await page.unroute(EXTENDED_ROUTE);
      await inputAgain.fill("a");
      const responsePromise = page.waitForResponse(
        (res) => isExtendedResultsPost(res.url(), res.request().method()),
        { timeout: 30_000 },
      );
      await submitAgain.click();
      const response = await responsePromise;
      expect(
        isSearchSubmitSuccessStatus(response.status()),
        `ordinary query expected 200, got ${response.status()}`,
      ).toBe(true);
      await expect(page.locator(terminalSuccessSelector()).first()).toBeVisible({
        timeout: 15_000,
      });
      await expect(panelAgain).toBeVisible();
      await expect(page.getByTestId(TEST_IDS.queryError)).toHaveCount(0);

      await expectNoSeriousA11yViolations(page, {
        scope: `[data-testid="${TEST_IDS.shell}"]`,
      });
      const relatedConsole = jsErrors.filter(
        (t) => !isKnownExplorerSitesConsoleNoise(t),
      );
      expect(relatedConsole, relatedConsole.join("\n")).toEqual([]);
    },
  );
});
