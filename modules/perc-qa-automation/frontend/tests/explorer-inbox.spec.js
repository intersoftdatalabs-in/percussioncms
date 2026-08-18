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
 * Explorer Inbox surface (#3446 / parent #3118 / reality-check #3102).
 *
 * <p>Inbox is <strong>Views → My Content → Inbox</strong>
 * ({@code //Views//MyContent/Inbox}), not a CE root. This spec is the
 * surface-filtered Playwright HARD GATE for the operator Inbox leaf.</p>
 *
 * <p>Soft-skip <strong>only</strong> when GET /services/views has no Inbox
 * design view <strong>and</strong> the product-route Inbox leaf is
 * absent. A visible Views tree synthesizes Inbox; a visible leaf, empty
 * assignment list (HTTP 200), or execute error must not skip
 * (#3561 / #3446).</p>
 *
 * <h3>Unattended surface (QA mode)</h3>
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/explorer-inbox.spec.js
 *   npm run test:surface -- --tag explorer-inbox
 *   npm run test:surface:list -- --path tests/explorer-inbox.spec.js
 *   python docker/scripts/perc-devctl.py qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const {
  loginAsAdmin,
  BASE_URL,
  adminBasicAuthHeaders,
} = require("./helpers/auth");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  TEST_IDS,
  explorerEntryUrl,
  viewsCatalogUrl,
  unwrapViewDefs,
  findInboxView,
  inboxLeafSelector,
  inboxResultsSelector,
  isViewExecuteJaxbError,
  isViewsExecuteUrl,
  shouldSkipMissingInboxCatalog,
  shouldExpandViewsGroup,
  missingInboxSkipMessage,
} = require("./helpers/explorer-inbox");

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
 * Expand Views → My Content only when the group is collapsed.
 * Clicking an already-open row hides the Inbox leaf (#3446).
 *
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<void>}
 */
async function expandMyContentIfCollapsed(page) {
  const groupRow = page.locator(`[data-testid="${TEST_IDS.myContentGroupRow}"]`);
  if ((await groupRow.count()) === 0) {
    return;
  }
  const expanded = await groupRow.first().getAttribute("aria-expanded");
  if (shouldExpandViewsGroup(expanded)) {
    await groupRow.first().click();
  }
}

/**
 * GET /services/views using the logged-in page session (plus Basic).
 *
 * @param {import('@playwright/test').Page} page
 * @returns {Promise<{ restStatus: number, restBody: unknown }>}
 */
async function fetchViewsCatalog(page) {
  const headers = {
    ...adminBasicAuthHeaders(),
    Accept: "application/json",
  };
  try {
    const res = await page.request.get(viewsCatalogUrl(BASE_URL), { headers });
    let restBody = null;
    if (res.ok()) {
      restBody = await res.json().catch(() => null);
    }
    return { restStatus: res.status(), restBody };
  } catch {
    return { restStatus: 0, restBody: null };
  }
}

test.describe("Explorer Inbox (#3446 / #3118)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("Explorer shell a11y; Inbox leaf under Views My Content @explorer-inbox @inbox @explorer @a11y", async ({
    page,
  }) => {
    const pageErrors = attachPageErrors(page);
    const { restStatus, restBody } = await fetchViewsCatalog(page);
    const inboxDef = findInboxView(restBody);
    const defs = unwrapViewDefs(restBody);

    const url = explorerEntryUrl(BASE_URL);
    await page.goto(url, { waitUntil: "networkidle" });

    const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
    await expect(shell).toBeVisible({ timeout: 20_000 });

    const viewsTree = page.locator(`[data-testid="${TEST_IDS.viewsTree}"]`);
    const treeVisible = await viewsTree
      .first()
      .isVisible()
      .catch(() => false);
    if (treeVisible) {
      await expandMyContentIfCollapsed(page);
    }
    const inboxLeaf = page.locator(inboxLeafSelector());
    const leafVisible = await inboxLeaf
      .first()
      .isVisible()
      .catch(() => false);

    if (
      shouldSkipMissingInboxCatalog({
        inboxDef,
        catalogEmpty: !inboxDef,
        leafVisible,
        treeVisible,
      })
    ) {
      test.skip(
        true,
        missingInboxSkipMessage({
          restStatus,
          catalogEmpty: defs.length === 0,
          leafVisible,
        }),
      );
      return;
    }

    await expectNoSeriousA11yViolations(page, {
      scope: `[data-testid="${TEST_IDS.shell}"]`,
    });

    await expect(viewsTree).toBeVisible({ timeout: 20_000 });
    await expandMyContentIfCollapsed(page);
    await expect(inboxLeaf.first()).toBeVisible({ timeout: 10_000 });
    expect(pageErrors, "uncaught pageerror on Explorer Inbox chrome").toEqual(
      [],
    );
  });

  test("run Inbox lists assignments or honest empty @explorer-inbox @inbox @explorer", async ({
    page,
  }) => {
    const pageErrors = attachPageErrors(page);
    const { restStatus, restBody } = await fetchViewsCatalog(page);
    const inboxDef = findInboxView(restBody);
    const defs = unwrapViewDefs(restBody);

    const url = explorerEntryUrl(BASE_URL);
    await page.goto(url, { waitUntil: "networkidle" });
    await expect(
      page.locator(`[data-testid="${TEST_IDS.shell}"]`),
    ).toBeVisible({ timeout: 20_000 });

    const viewsTree = page.locator(`[data-testid="${TEST_IDS.viewsTree}"]`);
    const treeVisible = await viewsTree
      .first()
      .isVisible()
      .catch(() => false);
    if (treeVisible) {
      await expandMyContentIfCollapsed(page);
    }
    const inboxLeaf = page.locator(inboxLeafSelector());
    const leafVisible = await inboxLeaf
      .first()
      .isVisible()
      .catch(() => false);

    if (
      shouldSkipMissingInboxCatalog({
        inboxDef,
        catalogEmpty: !inboxDef,
        leafVisible,
        treeVisible,
      })
    ) {
      test.skip(
        true,
        missingInboxSkipMessage({
          restStatus,
          catalogEmpty: defs.length === 0,
          leafVisible,
        }),
      );
      return;
    }

    await expandMyContentIfCollapsed(page);
    await expect(
      inboxLeaf.first(),
      "Inbox catalog row must have a visible Explorer leaf (#3561 / #3446)",
    ).toBeVisible({ timeout: 15_000 });

    const executeBodies = [];
    const executeStatuses = [];
    page.on("request", (req) => {
      if (req.method() !== "POST") return;
      if (!isViewsExecuteUrl(req.url())) return;
      executeBodies.push(req.postData() || "");
    });
    page.on("response", (res) => {
      if (res.request().method() !== "POST") return;
      if (!isViewsExecuteUrl(res.url())) return;
      executeStatuses.push(res.status());
    });

    await inboxLeaf.first().click();

    const results = page.locator(inboxResultsSelector());
    await expect(
      results.first(),
      "Inbox results region must mount after click (rows, empty, or error)",
    ).toBeVisible({ timeout: 20_000 });

    const loading = page.locator(`[data-testid="${TEST_IDS.resultsLoading}"]`);
    if (await loading.isVisible().catch(() => false)) {
      await expect(loading).toBeHidden({ timeout: 30_000 });
    }

    expect(
      executeBodies.length,
      "Inbox leaf must POST /services/views/{id}/execute",
    ).toBeGreaterThan(0);
    for (const raw of executeBodies) {
      const parsed = JSON.parse(raw);
      expect(
        parsed.ViewExecuteRequest,
        "JAXB root ViewExecuteRequest required (#3323)",
      ).toBeTruthy();
      expect(
        parsed.startIndex,
        "bare startIndex must not be the JSON root",
      ).toBeUndefined();
    }
    expect(
      executeStatuses.some((s) => s === 200),
      `Inbox execute must return 200 (statuses=${executeStatuses.join(",")})`,
    ).toBe(true);

    const err = page.locator(`[data-testid="${TEST_IDS.resultsError}"]`);
    if (await err.isVisible().catch(() => false)) {
      const errText = (await err.textContent().catch(() => "")) || "";
      expect(
        isViewExecuteJaxbError(errText),
        `Inbox must not fail JAXB startIndex / ViewExecuteRequest (#3323): ${errText}`,
      ).toBe(false);
      throw new Error(
        `Inbox execute showed an error region (must be 200 + rows or empty): ${errText}`,
      );
    }

    const empty = page.locator(`[data-testid="${TEST_IDS.resultsEmpty}"]`);
    const list = page.locator(`[data-testid="${TEST_IDS.resultsList}"]`);

    if (await empty.isVisible().catch(() => false)) {
      await expect(empty).toBeVisible();
      expect(pageErrors, "uncaught pageerror on empty Inbox").toEqual([]);
      return;
    }

    await expect(list).toBeVisible({ timeout: 10_000 });
    expect(pageErrors, "uncaught pageerror on Inbox assignment list").toEqual(
      [],
    );
  });
});
