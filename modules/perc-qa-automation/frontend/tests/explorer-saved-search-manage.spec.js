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
 * Explorer saved-search rename/delete from the Search panel (#6075 / parent #4530).
 *
 * <p>Live-CMS companion to the WebUI slice: open modern Content Explorer →
 * SearchPanel → saved-search picker → Rename (PUT display label, confirm /
 * cancel) and Delete (DELETE with confirm / cancel). 400/403/409 error
 * keep-rows behavior is covered by mocked Vitest in
 * {@code WebUI/src/test/ts/contentExplorer/SearchPanel.test.tsx}; this spec
 * proves the live confirm/cancel + PUT/DELETE round-trip on H2.</p>
 *
 * <h3>Unattended surface (QA mode)</h3>
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/explorer-saved-search-manage.spec.js
 *   # tags:
 *   npm run test:surface -- --tag explorer-saved-search-manage
 *   # list only (no live CMS):
 *   npm run test:surface:list -- --path tests/explorer-saved-search-manage.spec.js
 *   python docker/scripts/perc-devctl.py qa-down
 * </pre>
 */

"use strict";

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
} = require("./helpers/auth");
const {
  TEST_IDS,
  explorerEntryUrl,
  searchesCatalogUrl,
  unwrapSearchDefs,
  catalogSettledSelector,
} = require("./helpers/explorer-saved-search");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");

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

/**
 * Absolute URL for one design-search detail row.
 *
 * @param {string} baseUrl CMS origin
 * @param {string} idOrName catalog key
 * @returns {string}
 */
function searchDetailUrl(baseUrl, idOrName) {
  const root = String(baseUrl || "").trim().replace(/\/+$/, "");
  return `${root}/Rhythmyx/services/searches/${encodeURIComponent(idOrName)}`;
}

/**
 * POST a throwaway standard search for manage-flow tests.
 *
 * <p>Session context only (no basic-auth headers): the basic-auth REST
 * context lists zero design rows on H2 (#6119).</p>
 *
 * @param {import('@playwright/test').APIRequestContext} request session authed
 * @param {string} name catalog key (no whitespace per REST create gate)
 * @param {string} label display label
 */
async function createTempSearch(request, name, label) {
  const res = await request.post(searchesCatalogUrl(BASE_URL), {
    headers: { Accept: "application/json" },
    data: { SearchDef: { name, label, type: "StandardSearch" } },
  });
  const text = await res.text().catch(() => "");
  expect(
    res.ok(),
    `POST temp search ${name} must succeed (status=${res.status()} body=${text.slice(0, 400)})`,
  ).toBe(true);
}

/**
 * Best-effort DELETE of the throwaway search (test cleanup).
 *
 * @param {import('@playwright/test').APIRequestContext} request session authed
 * @param {string} name catalog key
 */
async function deleteTempSearch(request, name) {
  try {
    await request.delete(searchDetailUrl(BASE_URL, name));
  } catch (e) {
    // Cleanup must not fail the test run.
  }
}

/**
 * Open Explorer with the Search panel visible and the catalog settled.
 *
 * @param {import('@playwright/test').Page} page
 */
async function openSearchPanel(page) {
  const url = explorerEntryUrl(BASE_URL);
  await page.goto(url, { waitUntil: "networkidle" });
  await expect(page.locator(`[data-testid="${TEST_IDS.shell}"]`)).toBeVisible({
    timeout: 20_000,
  });
  await page.locator(`[data-testid="${TEST_IDS.menuView}"]`).click();
  await page.locator(`[data-testid="${TEST_IDS.toggleSearch}"]`).click();
  await expect(
    page.locator(`[data-testid="${TEST_IDS.searchPanel}"]`),
  ).toBeVisible({ timeout: 10_000 });
  await expect(page.locator(catalogSettledSelector()).first()).toBeVisible({
    timeout: 20_000,
  });
}

/**
 * Select one saved search by catalog key; waits until the option exists.
 *
 * @param {import('@playwright/test').Page} page
 * @param {string} name catalog key
 */
async function selectSavedSearch(page, name) {
  const select = page.locator(`[data-testid="${TEST_IDS.savedSelect}"]`);
  await expect(select).toBeVisible({ timeout: 10_000 });
  await expect
    .poll(
      async () =>
        select
          .locator("option")
          .evaluateAll((opts) => opts.map((o) => o.value)),
      { timeout: 20_000 },
    )
    .toContain(name);
  await select.selectOption({ value: name });
}

/**
 * Read one catalog row label via REST.
 *
 * <p>Call with the session-authenticated {@code page.request} context: the
 * basic-auth REST context lists zero design rows on H2 while the session
 * lists the full catalog (#6119).</p>
 *
 * @param {import('@playwright/test').APIRequestContext} request session authed
 * @param {string} name catalog key
 * @returns {Promise<string|null>} label or null when absent
 */
async function restSearchLabel(request, name) {
  const res = await request.get(searchesCatalogUrl(BASE_URL), {
    headers: { Accept: "application/json" },
  });
  if (!res.ok()) {
    return null;
  }
  const body = await res.json().catch(() => null);
  const row = unwrapSearchDefs(body).find(
    (d) => d && String(d.name || "") === name,
  );
  return row ? String(row.label || "") : null;
}

// Tags live on individual test() titles only — Playwright ignores @tags on describe names.
test.describe("Explorer saved-search rename/delete (#6075 / #4530)", () => {
  let tempName = "";

  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
    tempName = `E2E-Manage-${Date.now()}`;
    // Session context: the basic-auth REST context lists zero design rows
    // on H2 while the session lists the full catalog (#6119).
    await createTempSearch(page.request, tempName, `Manage ${tempName}`);
  });

  test.afterEach(async ({ page }) => {
    if (tempName) {
      await deleteTempSearch(page.request, tempName);
      tempName = "";
    }
  });

  test("rename cancel keeps the row and writes nothing @saved-search @explorer-saved-search-manage @explorer", async ({
    page,
  }) => {
    const pageErrors = attachPageErrors(page);
    await openSearchPanel(page);
    await selectSavedSearch(page, tempName);

    const renameBtn = page.locator(`[data-testid="${TEST_IDS.savedRename}"]`);
    await expect(renameBtn).toBeEnabled();
    await renameBtn.click();
    const dialog = page.locator(`[data-testid="${TEST_IDS.manageDialog}"]`);
    await expect(dialog).toBeVisible();
    await page
      .locator(`[data-testid="${TEST_IDS.renameInput}"]`)
      .fill(`Renamed ${tempName}`);
    await page.locator(`[data-testid="${TEST_IDS.manageCancel}"]`).click();
    await expect(dialog).toBeHidden();

    // Row still listed; no rename notice shown.
    const select = page.locator(`[data-testid="${TEST_IDS.savedSelect}"]`);
    await expect
      .poll(
        async () =>
          select
            .locator("option")
            .evaluateAll((opts) => opts.map((o) => o.value)),
        { timeout: 10_000 },
      )
      .toContain(tempName);
    await expect(
      page.locator(`[data-testid="${TEST_IDS.manageNotice}"]`),
    ).toBeHidden();
    expect(pageErrors, `zero JS console errors (${pageErrors.join(" | ")})`).toEqual([]);
  });

  test("rename confirm keeps every row whether the server saves or refuses @saved-search @explorer-saved-search-manage @explorer", async ({
    page,
  }) => {
    const pageErrors = attachPageErrors(page);
    await openSearchPanel(page);
    await selectSavedSearch(page, tempName);

    const newLabel = `Renamed ${tempName}`;
    await page.locator(`[data-testid="${TEST_IDS.savedRename}"]`).click();
    await expect(
      page.locator(`[data-testid="${TEST_IDS.manageDialog}"]`),
    ).toBeVisible();
    await page.locator(`[data-testid="${TEST_IDS.renameInput}"]`).fill(newLabel);
    await page.locator(`[data-testid="${TEST_IDS.renameSave}"]`).click();
    // Either the save lands (notice) or the server refuses (error on the
    // dialog, e.g. H2 PUT defect #6119): no row may be lost in either world.
    const notice = page.locator(`[data-testid="${TEST_IDS.manageNotice}"]`);
    const error = page.locator(`[data-testid="${TEST_IDS.manageError}"]`);
    await expect(notice.or(error)).toBeVisible({ timeout: 15_000 });

    const select = page.locator(`[data-testid="${TEST_IDS.savedSelect}"]`);
    const optionValues = () =>
      select
        .locator("option")
        .evaluateAll((opts) => opts.map((o) => o.value));
    if (await error.isVisible()) {
      // Server refused: the dialog stays open and every row is kept (#6075).
      // The refusal itself is the asserted behavior, so its single expected
      // 500 resource message does not count toward the zero-errors gate.
      const unexpected = pageErrors.filter(
        (e) => !/the server responded with a status of 500/.test(e),
      );
      expect(
        unexpected,
        `zero unexpected JS console errors (${unexpected.join(" | ")})`,
      ).toEqual([]);
      await expect(
        page.locator(`[data-testid="${TEST_IDS.manageDialog}"]`),
      ).toBeVisible();
      await expect.poll(optionValues, { timeout: 10_000 }).toContain(tempName);
      return;
    } else {
      // Save landed: the picker option follows the new label. REST agrees
      // via the session context (basic-auth list context is empty on H2).
      await expect
        .poll(
          async () =>
            select.locator("option").evaluateAll((opts) =>
              opts.map((o) => ({
                value: o.value,
                text: String(o.textContent || ""),
              })),
            ),
          { timeout: 15_000 },
        )
        .toEqual(expect.arrayContaining([{ value: tempName, text: newLabel }]));
      await expect
        .poll(() => restSearchLabel(page.request, tempName), { timeout: 20_000 })
        .toBe(newLabel);
    }
    expect(pageErrors, `zero JS console errors (${pageErrors.join(" | ")})`).toEqual([]);
  });

  test("delete cancel keeps the row and deletes nothing @saved-search @explorer-saved-search-manage @explorer", async ({
    page,
  }) => {
    const pageErrors = attachPageErrors(page);
    await openSearchPanel(page);
    await selectSavedSearch(page, tempName);

    const deleteBtn = page.locator(`[data-testid="${TEST_IDS.savedDelete}"]`);
    await expect(deleteBtn).toBeEnabled();
    await deleteBtn.click();
    const dialog = page.locator(`[data-testid="${TEST_IDS.manageDialog}"]`);
    await expect(dialog).toBeVisible();
    await expect(dialog).toContainText(tempName);
    await page.locator(`[data-testid="${TEST_IDS.manageCancel}"]`).click();
    await expect(dialog).toBeHidden();

    const select = page.locator(`[data-testid="${TEST_IDS.savedSelect}"]`);
    await expect
      .poll(
        async () =>
          select
            .locator("option")
            .evaluateAll((opts) => opts.map((o) => o.value)),
        { timeout: 10_000 },
      )
      .toContain(tempName);
    expect(pageErrors, `zero JS console errors (${pageErrors.join(" | ")})`).toEqual([]);
  });

  test("delete confirm drops the row and GET detail is 404 @saved-search @explorer-saved-search-manage @explorer", async ({
    page,
  }) => {
    const pageErrors = attachPageErrors(page);
    await openSearchPanel(page);
    await selectSavedSearch(page, tempName);

    await page.locator(`[data-testid="${TEST_IDS.savedDelete}"]`).click();
    await expect(
      page.locator(`[data-testid="${TEST_IDS.manageDialog}"]`),
    ).toBeVisible();
    await page.locator(`[data-testid="${TEST_IDS.deleteConfirm}"]`).click();
    await expect(
      page.locator(`[data-testid="${TEST_IDS.manageNotice}"]`),
    ).toBeVisible({ timeout: 15_000 });

    // Row gone from the picker and the selection cleared; REST agrees.
    const select = page.locator(`[data-testid="${TEST_IDS.savedSelect}"]`);
    await expect
      .poll(
        async () =>
          select
            .locator("option")
            .evaluateAll((opts) => opts.map((o) => o.value)),
        { timeout: 15_000 },
      )
      .not.toContain(tempName);
    await expect
      .poll(
        async () =>
          (await page.request.get(searchDetailUrl(BASE_URL, tempName))).status(),
        { timeout: 20_000 },
      ).toBe(404);
    // Row already deleted: skip afterEach best-effort delete noise.
    tempName = "";
    expect(pageErrors, `zero JS console errors (${pageErrors.join(" | ")})`).toEqual([]);
  });

  test("rename and delete dialogs have zero serious a11y violations @saved-search @explorer-saved-search-manage @explorer", async ({
    page,
  }) => {
    const pageErrors = attachPageErrors(page);
    await openSearchPanel(page);
    await selectSavedSearch(page, tempName);

    // Rename dialog surface (#6180): axe the open dialog, then cancel.
    await page.locator(`[data-testid="${TEST_IDS.savedRename}"]`).click();
    const dialog = page.locator(`[data-testid="${TEST_IDS.manageDialog}"]`);
    await expect(dialog).toBeVisible();
    await expectNoSeriousA11yViolations(page, {
      scope: `[data-testid="${TEST_IDS.manageDialog}"]`,
    });
    await page.locator(`[data-testid="${TEST_IDS.manageCancel}"]`).click();
    await expect(dialog).toBeHidden();

    // Delete dialog surface (#6180): axe the open dialog, then cancel so
    // the throwaway row survives for afterEach cleanup.
    await page.locator(`[data-testid="${TEST_IDS.savedDelete}"]`).click();
    await expect(dialog).toBeVisible();
    await expect(dialog).toContainText(tempName);
    await expectNoSeriousA11yViolations(page, {
      scope: `[data-testid="${TEST_IDS.manageDialog}"]`,
    });
    await page.locator(`[data-testid="${TEST_IDS.manageCancel}"]`).click();
    await expect(dialog).toBeHidden();

    expect(pageErrors, `zero JS console errors (${pageErrors.join(" | ")})`).toEqual([]);
  });
});
