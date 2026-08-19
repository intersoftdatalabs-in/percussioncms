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
 * Explorer saved-search picker surface (#2507 / parent #2409 / grandparent #2400).
 *
 * <p>Live-CMS companion to WebUI slice C (#2506 / PR #2606): open modern
 * Content Explorer → SearchPanel → pick a design search from the catalog →
 * assert results region, empty handling, or error/loading post-execute
 * wiring.</p>
 *
 * <p>Vitest covers mocked catalog/execute in
 * {@code WebUI/src/test/ts/contentExplorer/SearchPanel.test.tsx}; this
 * spec is the surface-filtered Playwright HARD GATE for overnight agents.</p>
 *
 * <h3>Unattended surface (QA mode)</h3>
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/explorer-saved-search.spec.js
 *   # tags:
 *   npm run test:surface -- --tag saved-search
 *   npm run test:surface -- --tag explorer-saved-search
 *   # list only (no live CMS):
 *   npm run test:surface:list -- --path tests/explorer-saved-search.spec.js
 *   npm run test:surface:list -- --tag saved-search
 *   python docker/scripts/perc-devctl.py qa-down
 * </pre>
 *
 * <p><strong>Fixture soft-skip:</strong> execute soft-skips <em>only</em>
 * when the catalog is empty or every row is a custom-URL search. A visible
 * picker with a standard/user search (or REST View_All) must Run — do not
 * skip (#3576 / parent #3102).</p>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const {
  TEST_IDS,
  explorerEntryUrl,
  searchesCatalogUrl,
  unwrapSearchDefs,
  pickRunnableSavedSearch,
  pickRunnableSelectOption,
  shouldSkipMissingRunnableSearch,
  noRunnableSearchSkipMessage,
  postExecuteRegionSelector,
  catalogSettledSelector,
  isCustomUrlSearch,
  isCustomSelectOption,
  isDefaultAllView,
  searchDefKey,
  searchesExecuteUrl,
} = require("./helpers/explorer-saved-search");

/**
 * Collect uncaught page errors for the C5 zero-errors gate.
 *
 * @param {import('@playwright/test').Page} page
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
    if (/Failed to load resource: the server responded with a status of (404|400)/i.test(text)) {
      return;
    }
    pageErrors.push(text);
  });
  return pageErrors;
}

// Tags live on individual test() titles only — Playwright ignores @tags on describe names.
test.describe("Explorer saved-search picker (#2507 / #2409)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("Explorer shell opens SearchPanel with saved-search catalog chrome @saved-search @explorer-saved-search @explorer", async ({
    page,
  }) => {
    const url = explorerEntryUrl(BASE_URL);
    await page.goto(url, { waitUntil: "networkidle" });

    const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
    await expect(shell).toBeVisible({ timeout: 20_000 });

    // #2731: search toggle lives under View menu dropdown.
    await page.locator(`[data-testid="${TEST_IDS.menuView}"]`).click();
    await page.locator(`[data-testid="${TEST_IDS.toggleSearch}"]`).click();
    await expect(
      page.locator(`[data-testid="${TEST_IDS.searchPanelHost}"]`),
    ).toBeVisible({ timeout: 10_000 });
    await expect(
      page.locator(`[data-testid="${TEST_IDS.searchPanel}"]`),
    ).toBeVisible();

    // Catalog settles to empty, error, or picker (loading may flash first).
    await expect(page.locator(catalogSettledSelector()).first()).toBeVisible({
      timeout: 20_000,
    });

    const picker = page.locator(`[data-testid="${TEST_IDS.savedPicker}"]`);
    const empty = page.locator(`[data-testid="${TEST_IDS.savedEmpty}"]`);
    const err = page.locator(`[data-testid="${TEST_IDS.savedError}"]`);

    if (await picker.isVisible()) {
      await expect(
        page.locator(`[data-testid="${TEST_IDS.savedSelect}"]`),
      ).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.savedRun}"]`),
      ).toBeVisible();
      // Run stays disabled until a non-placeholder option is chosen.
      await expect(
        page.locator(`[data-testid="${TEST_IDS.savedRun}"]`),
      ).toBeDisabled();
    } else if (await empty.isVisible()) {
      await expect(empty).toBeVisible();
    } else {
      await expect(err).toBeVisible();
      await expect(
        page.locator(`[data-testid="${TEST_IDS.savedRetry}"]`),
      ).toBeVisible();
    }
  });

  test("pick known saved search and assert results / empty / error region @saved-search @explorer-saved-search", async ({
    page,
    request,
  }) => {
    const pageErrors = attachPageErrors(page);
    // Probe REST catalog so we prefer View_All; UI picker is the no-skip gate.
    const headers = adminBasicAuthHeaders();
    const catalogUrl = searchesCatalogUrl(BASE_URL, { includeViews: true });
    let restStatus = 0;
    let restBody = null;
    try {
      const res = await request.get(catalogUrl, {
        headers: { ...headers, Accept: "application/json" },
      });
      restStatus = res.status();
      if (res.ok()) {
        restBody = await res.json().catch(() => null);
      }
    } catch (e) {
      // Network failure against a dead stack should fail hard below on shell.
      restBody = null;
    }

    const runnable = pickRunnableSavedSearch(restBody);
    const defs = unwrapSearchDefs(restBody);
    const onlyCustom =
      defs.length > 0 && defs.every((d) => isCustomUrlSearch(d));

    const url = explorerEntryUrl(BASE_URL);
    await page.goto(url, { waitUntil: "networkidle" });
    await expect(
      page.locator(`[data-testid="${TEST_IDS.shell}"]`),
    ).toBeVisible({ timeout: 20_000 });
    await page.locator(`[data-testid="${TEST_IDS.menuView}"]`).click();
    await page.locator(`[data-testid="${TEST_IDS.toggleSearch}"]`).click();
    await expect(
      page.locator(`[data-testid="${TEST_IDS.searchPanel}"]`),
    ).toBeVisible({ timeout: 10_000 });
    await expect(page.locator(catalogSettledSelector()).first()).toBeVisible({
      timeout: 20_000,
    });

    const empty = page.locator(`[data-testid="${TEST_IDS.savedEmpty}"]`);
    const picker = page.locator(`[data-testid="${TEST_IDS.savedPicker}"]`);
    const pickerVisible = await picker.isVisible().catch(() => false);
    const select = page.locator(`[data-testid="${TEST_IDS.savedSelect}"]`);
    const selectOptions = pickerVisible
      ? await select.locator("option").evaluateAll((opts) =>
          opts.map((o) => ({
            value: /** @type {HTMLOptionElement} */ (o).value,
            text: String(/** @type {HTMLOptionElement} */ (o).textContent || ""),
          })),
        )
      : [];
    const uiRunnable = pickRunnableSelectOption(selectOptions);
    const optionCount = selectOptions.filter((o) => String(o.value || "").trim())
      .length;

    if (
      shouldSkipMissingRunnableSearch({
        runnable,
        pickerVisible,
        optionCount,
        hasRunnableOption: Boolean(uiRunnable),
        onlyCustom,
        catalogEmpty: defs.length === 0 && !pickerVisible,
      })
    ) {
      if (await empty.isVisible()) {
        await expect(empty).toBeVisible();
      } else if (pickerVisible) {
        await expect(select).toBeVisible();
        const runBtn = page.locator(`[data-testid="${TEST_IDS.savedRun}"]`);
        const customOpt = selectOptions.find((o) => isCustomSelectOption(o));
        if (customOpt && customOpt.value) {
          await select.selectOption(customOpt.value);
          await expect(runBtn).toBeDisabled();
        }
      }
      test.skip(
        true,
        noRunnableSearchSkipMessage({
          empty: defs.length === 0,
          onlyCustom,
          restStatus,
          pickerVisible,
        }),
      );
      return;
    }

    expect(
      pickerVisible,
      "catalog has a runnable search so SearchPanel picker must be visible (#3576)",
    ).toBe(true);
    await expect(select).toBeVisible({ timeout: 10_000 });

    const runBtn = page.locator(`[data-testid="${TEST_IDS.savedRun}"]`);
    const customOpt = selectOptions.find((o) => isCustomSelectOption(o));
    if (customOpt && customOpt.value) {
      await select.selectOption(customOpt.value);
      await expect(runBtn).toBeDisabled();
    }

    const optionValues = selectOptions
      .map((o) => String(o.value || "").trim())
      .filter(Boolean);
    const keyToPick =
      runnable && optionValues.includes(runnable.key)
        ? runnable.key
        : uiRunnable
          ? uiRunnable.value
          : optionValues[0];
    expect(
      keyToPick,
      "saved-search select should expose at least one design search option",
    ).toBeTruthy();

    await select.selectOption(keyToPick);
    await expect(runBtn).toBeEnabled({ timeout: 5_000 });
    await runBtn.click();

    // Post-execute: loading may flash; settle on results, empty, or error.
    await expect(page.locator(postExecuteRegionSelector()).first()).toBeVisible({
      timeout: 30_000,
    });

    const results = page.locator(`[data-testid="${TEST_IDS.resultsList}"]`);
    const emptyResults = page.locator(
      `[data-testid="${TEST_IDS.resultsEmpty}"]`,
    );
    const errResults = page.locator(`[data-testid="${TEST_IDS.resultsError}"]`);
    const loading = page.locator(`[data-testid="${TEST_IDS.resultsLoading}"]`);

    // Wait out loading if it is still visible.
    if (await loading.isVisible().catch(() => false)) {
      await expect(loading).toBeHidden({ timeout: 30_000 });
    }

    await expect(page.locator(postExecuteRegionSelector()).first()).toBeVisible({
      timeout: 5_000,
    });

    if (await results.isVisible()) {
      await expect(
        page.locator(`[data-testid="${TEST_IDS.resultRow}"]`).first(),
      ).toBeVisible();
    } else if (await emptyResults.isVisible()) {
      await expect(emptyResults).toBeVisible();
    } else {
      const errText = ((await errResults.textContent()) || "").trim();
      // #3517: All / View_All (and other standard searches) must not surface
      // the generic search-web-service IOException. Free-text FTS may still
      // error without a Lucene index; design-search execute must not.
      expect(
        /java\.io\.IOException|search web service/i.test(errText),
        `saved-search execute must not report I/O / search web service error: ${errText}`,
      ).toBe(false);
      if (runnable && /view_all|^all$/i.test(String(runnable.key))) {
        throw new Error(
          `All / View_All execute must show results or empty, not error: ${errText}`,
        );
      }
      await expect(errResults).toBeVisible();
      await expect(
        page.locator(`[data-testid="search-panel-retry"]`),
      ).toBeVisible();
    }
    expect(pageErrors, "uncaught pageerror on saved-search picker/run").toEqual(
      [],
    );
  });

  test("REST: POST /services/searches/View_All/execute is 200 page not IOException @saved-search @explorer-saved-search", async ({
    page,
  }) => {
    test.setTimeout(45_000);
    const headers = {
      ...adminBasicAuthHeaders(),
      Accept: "application/json",
      "Content-Type": "application/json",
    };
    const catalogRes = await page.request.get(
      searchesCatalogUrl(BASE_URL, { includeViews: true }),
      { headers },
    );
    if (catalogRes.status() === 401 || catalogRes.status() === 403) {
      test.skip(true, `catalog auth ${catalogRes.status()} — no execute probe`);
      return;
    }
    expect(catalogRes.status(), `catalog status=${catalogRes.status()}`).toBeLessThan(
      500,
    );
    const defs = unwrapSearchDefs(
      catalogRes.ok() ? await catalogRes.json().catch(() => null) : null,
    );
    const allView = defs.find((d) => isDefaultAllView(d));
    const runnable = pickRunnableSavedSearch(defs);
    if (defs.length === 0) {
      test.skip(
        true,
        "H2 catalog empty — soft-skip execute REST (no catalog search exists).",
      );
      return;
    }
    expect(
      allView || runnable,
      `catalog has searches so execute must run (names=${defs
        .map((d) => searchDefKey(d))
        .join(",")})`,
    ).toBeTruthy();
    const key = allView ? searchDefKey(allView) : runnable.key;
    const execUrl = searchesExecuteUrl(BASE_URL, key);
    const body = JSON.stringify({
      SearchExecuteRequest: { startIndex: 1, maxResults: 25 },
    });
    const res = await page.request.post(execUrl, { headers, data: body });
    const text = await res.text();
    expect(
      res.status(),
      `POST ${execUrl} must not 5xx (status=${res.status()} body=${text.slice(0, 400)})`,
    ).toBeLessThan(500);
    expect([200, 401, 403]).toContain(res.status());
    expect(
      /java\.io\.IOException|search web service/i.test(text),
      `execute body must not be generic I/O error: ${text.slice(0, 400)}`,
    ).toBe(false);
    if (res.status() === 200) {
      const parsed = JSON.parse(text);
      const envelope = parsed.SearchExecuteResult || parsed.searchExecuteResult || parsed;
      expect(Array.isArray(envelope.children || [])).toBe(true);
    }
  });

  test("REST: GET /services/searches answers for Admin @saved-search @explorer-saved-search", async ({
    request,
  }) => {
    test.setTimeout(45_000);
    const headers = adminBasicAuthHeaders();
    const res = await request.get(searchesCatalogUrl(BASE_URL, { includeViews: true }), {
      headers: { ...headers, Accept: "application/json" },
    });
    // 200 = catalog; 401/403 still prove webapp auth surface; 5xx is hard fail.
    expect(
      res.status(),
      `GET searches catalog should not 5xx (status=${res.status()})`,
    ).toBeLessThan(500);
    expect([200, 401, 403]).toContain(res.status());
    if (res.status() === 200) {
      const body = await res.json().catch(() => null);
      expect(body == null || typeof body === "object" || Array.isArray(body)).toBe(
        true,
      );
      // Structural unwrap never throws on common shapes.
      const defs = unwrapSearchDefs(body);
      expect(Array.isArray(defs)).toBe(true);
      // Default CX All view must appear when views are included (#3205 / #3199).
      const names = defs
        .map((d) => (d && d.name != null ? String(d.name) : ""))
        .filter(Boolean);
      if (names.length > 0) {
        const hasViewAll = names.some(
          (n) => n.toLowerCase() === "view_all" || n.toLowerCase() === "all",
        );
        expect(
          hasViewAll,
          `includeViews catalog should expose View_All (names=${names.join(",")})`,
        ).toBe(true);
      }
    }
  });
});
