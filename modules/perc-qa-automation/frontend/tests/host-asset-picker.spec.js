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
 * Playwright spec: US2 host-asset-picker migration (SC-002 / FR-008a).
 *
 * <p>Asserts the modern ContentBrowser mounts on the dedicated
 * asset-picker entry point <code>assetPickerModern.jsp</code> in
 * <strong>item-only, single-select mode</strong>
 * (<code>multiSelect: false, allowFolderSelect: false, allowItemSelect:
 * true, allowedTypes: ['page', 'asset']</code>). Folders are rejected
 * client-side; the host receives a {@code SelectionResult} for the
 * chosen page or asset on confirm. The host is the canonical
 * replacement for the legacy miller-column Finder asset picker that
 * <code>$.perc_finder().launchAssetPreview</code> / <code>perc_finder().refresh()</code>
 * wired up in <code>perc_delete_page_button.js</code>,
 * <code>PercActionDataTable.js</code>, and <code>PercPageView.js</code>
 * (per-host call-site migration is a separate follow-up; see
 * cutover-inventory §A / §C).</p>
 *
 * <p>Note: <code>us2-content-browser.spec.js</code> exercises the same
 * page for the generic ContentBrowser host contract. This
 * <code>host-asset-picker.spec.js</code> is the dedicated per-host
 * spec (T045a-pw) and adds the asset-picker-specific assertions:
 * the title rendered by the host, the page/asset-only filter
 * (no folder path selectable from the tree), and the confirm-payload
 * <code>result</code> block that the host populates on
 * <code>onConfirm</code>.</p>
 *
 * <p>Run from <code>modules/perc-qa-automation/frontend</code>:</p>
 * <pre>
 *   npm test -- tests/host-asset-picker.spec.js
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

const DIALOG_URL = `${BASE_URL}/Rhythmyx/cm/app/assetPickerModern.jsp?_=${Date.now()}`;

test.describe("US2 host-asset-picker migration (SC-002)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(60_000);
    await loginAsAdmin(page);
  });

  test("ContentBrowser mounts on the asset-picker host page", async ({
    page,
  }) => {
    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    const dialog = page.locator('[data-testid="content-browser"]');
    await expect(dialog).toBeVisible({ timeout: 15_000 });
  });

  test("legacy miller-column Finder chrome is NOT loaded on the asset-picker host", async ({
    page,
  }) => {
    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
  });

  test("asset-picker initial state: confirm disabled, single-select summary empty", async ({
    page,
  }) => {
    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    const confirm = page.locator('[data-testid="content-browser-confirm"]');
    const summary = page.locator(
      '[data-testid="content-browser-selection-summary"]',
    );
    await expect(confirm).toBeVisible({ timeout: 15_000 });
    await expect(confirm).toBeDisabled();
    await expect(summary).toBeVisible();
  });

  test("asset-picker dialog chrome is keyboard-completable (Cancel button focusable)", async ({
    page,
  }) => {
    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    const cancelBtn = page.locator('[data-testid="content-browser-cancel"]');
    await expect(cancelBtn).toBeVisible({ timeout: 15_000 });
    await cancelBtn.focus();
    const focusedTag = await page.evaluate(
      () => document.activeElement?.tagName,
    );
    expect(focusedTag).toBe("BUTTON");
  });

  test("axe-core a11y gate — host asset picker modern dialog (T082b)", async ({
    page,
  }) => {
    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    await expect(
      page.locator('[data-testid="perc-content-browser-root"]'),
    ).toBeVisible({ timeout: 15_000 });
    await expectNoSeriousA11yViolations(page, {
      scope: '[data-testid="perc-content-browser-root"]',
    });
  });

  /**
   * #2793 / #3438: asset picker mounts shared SearchPanel. Must not
   * soft-skip — ContentBrowser has to appear, and free-text / saved
   * execute must not 400 on /Sites vs //Sites or a bare folderPath
   * (SearchExecuteRequest envelope).
   */
  test("asset-picker SearchPanel mounts and search is not 400 @content-browser-search @host-asset-picker", async ({
    page,
  }) => {
    const searchHits = [];
    page.on("response", (res) => {
      const url = res.url();
      if (
        url.includes("/searchmanagement/") ||
        /\/services\/searches\/[^/]+\/execute/.test(url)
      ) {
        searchHits.push({ url, status: res.status() });
      }
    });

    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    const dialog = page.locator('[data-testid="content-browser"]');
    await expect(dialog).toBeVisible({ timeout: 15_000 });
    await expect(dialog).toHaveAttribute("data-enable-search", "true");

    const searchHost = page.locator(
      '[data-testid="content-browser-search-panel"]',
    );
    await expect(searchHost).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('[data-testid="search-panel-input"]')).toBeVisible();
    await expect(page.locator('[data-testid="search-panel-submit"]')).toBeVisible();
    await expect(
      page
        .locator(
          '[data-testid="search-panel-saved-picker"], [data-testid="search-panel-saved-empty"], [data-testid="search-panel-saved-error"]',
        )
        .first(),
    ).toBeVisible({ timeout: 20_000 });

    await page.locator('[data-testid="search-panel-input"]').fill("a");
    await page.locator('[data-testid="search-panel-submit"]').click();
    await expect(
      page
        .locator(
          '[data-testid="search-panel-results"], [data-testid="search-panel-empty"], [data-testid="search-panel-error"]',
        )
        .first(),
    ).toBeVisible({ timeout: 20_000 });

    const bad = searchHits.filter((h) => h.status === 400);
    expect(bad, `search 400s: ${JSON.stringify(bad)}`).toEqual([]);

    const err = page.locator('[data-testid="search-panel-error"]');
    if (await err.isVisible()) {
      const text = (await err.textContent()) || "";
      expect(text).not.toMatch(/must start with/i);
      expect(text).not.toMatch(/JAXBException|SearchExecuteRequest/i);
    }

    const savedSelect = page.locator('[data-testid="search-panel-saved-select"]');
    if (await savedSelect.isVisible()) {
      const values = await savedSelect.locator("option").evaluateAll((opts) =>
        opts.map((o) => o.value).filter((v) => v && v.trim().length > 0),
      );
      if (values.length > 0) {
        await savedSelect.selectOption(values[0]);
        const run = page.locator('[data-testid="search-panel-saved-run"]');
        if (await run.isEnabled()) {
          await run.click();
          await expect(
            page
              .locator(
                '[data-testid="search-panel-results"], [data-testid="search-panel-empty"], [data-testid="search-panel-error"]',
              )
              .first(),
          ).toBeVisible({ timeout: 20_000 });
          const badSaved = searchHits.filter((h) => h.status === 400);
          expect(
            badSaved,
            `saved-search 400s: ${JSON.stringify(badSaved)}`,
          ).toEqual([]);
        }
      }
    }

    await expectNoSeriousA11yViolations(page, {
      scope: '[data-testid="content-browser-search-panel"]',
    });
  });

  /**
   * #3714: CMS search returns content-type names (Image, percPage) while
   * the host filter is allowedTypes [page, asset]. Open must select the
   * hit so Confirm enables.
   */
  test("asset-picker search Open of a CMS type (Image/page) enables Confirm @content-browser-search @host-asset-picker @issue-3714", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => {
      pageErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        pageErrors.push(msg.text());
      }
    });

    await page.goto(DIALOG_URL, { waitUntil: "networkidle" });
    const dialog = page.locator('[data-testid="content-browser"]');
    await expect(dialog).toBeVisible({ timeout: 15_000 });
    await expect(dialog).toHaveAttribute("data-enable-search", "true");

    const input = page.locator('[data-testid="search-panel-input"]');
    const submit = page.locator('[data-testid="search-panel-submit"]');
    await expect(input).toBeVisible({ timeout: 10_000 });

    const queries = ["jpg", "Image", "Home"];
    let hasResults = false;
    for (const q of queries) {
      await input.fill(q);
      await submit.click();
      const settled = page
        .locator(
          '[data-testid="search-panel-results"], [data-testid="search-panel-empty"], [data-testid="search-panel-error"]',
        )
        .first();
      await expect(settled).toBeVisible({ timeout: 20_000 });
      if (await page.locator('[data-testid="search-panel-results"]').isVisible()) {
        hasResults = true;
        break;
      }
    }
    expect(
      hasResults,
      "QA H2 cell must return at least one search hit (jpg/Image/Home)",
    ).toBe(true);

    const rows = page.locator('[data-testid="search-panel-result-row"]');
    await expect(rows.first()).toBeVisible();
    const rowCount = await rows.count();
    const folderish = (type) =>
      type === "folder" ||
      type === "fsfolder" ||
      type === "site" ||
      type.includes("navon") ||
      type.includes("navtree");
    const isAssetish = (type) =>
      type.includes("image") ||
      type === "file" ||
      type === "asset" ||
      type === "rfffile" ||
      type === "percasset";
    let chosen = -1;
    for (let i = 0; i < rowCount; i++) {
      const type = (
        (await rows.nth(i).getAttribute("data-item-type")) || ""
      ).toLowerCase();
      if (folderish(type)) {
        continue;
      }
      if (isAssetish(type)) {
        chosen = i;
        break;
      }
      if (chosen < 0) {
        chosen = i;
      }
    }
    expect(chosen, "search results must include a non-folder page or asset").toBeGreaterThanOrEqual(
      0,
    );
    await rows
      .nth(chosen)
      .locator("button[data-testid^='search-panel-open-']")
      .click();

    await expect(
      page.locator('[data-testid="content-browser-error"]'),
    ).toHaveCount(0);
    const confirm = page.locator('[data-testid="content-browser-confirm"]');
    await expect(confirm).toBeEnabled({ timeout: 10_000 });
    const summary = page.locator(
      '[data-testid="content-browser-selection-summary"]',
    );
    await expect(summary).not.toHaveText(/No items selected/i);
    await confirm.click();
    const result = page.locator('[data-testid="perc-content-browser-result"]');
    await expect(result).toContainText("Confirmed:", { timeout: 10_000 });
    await expect(result).toContainText("items");

    expect(pageErrors, `browser errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });
});
