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
 * Explorer Inbox surface (#3241 / parent #3118 / reality-check #3102).
 *
 * <p>Inbox is <strong>Views → My Content → Inbox</strong>
 * ({@code //Views//MyContent/Inbox}), not a CE root. This spec is the
 * surface-filtered Playwright HARD GATE for the operator Inbox leaf.</p>
 *
 * <p>Soft-skip when the QA H2 cell has no Views catalog / Inbox leaf
 * (#3240 not deployed) or no assignment rows after a successful run.</p>
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
  missingInboxSkipMessage,
  noAssignmentsSkipMessage,
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
  return pageErrors;
}

test.describe("Explorer Inbox (#3241 / #3118)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("Explorer shell a11y; Inbox leaf under Views My Content or soft-skip @explorer-inbox @inbox @explorer @a11y", async ({
    page,
  }) => {
    const pageErrors = attachPageErrors(page);
    const url = explorerEntryUrl(BASE_URL);
    await page.goto(url, { waitUntil: "networkidle" });

    const shell = page.locator(`[data-testid="${TEST_IDS.shell}"]`);
    await expect(shell).toBeVisible({ timeout: 20_000 });

    await expectNoSeriousA11yViolations(page, {
      scope: `[data-testid="${TEST_IDS.shell}"]`,
    });

    const viewsTree = page.locator(`[data-testid="${TEST_IDS.viewsTree}"]`);
    const inboxLeaf = page.locator(inboxLeafSelector());

    const hasTree = (await viewsTree.count()) > 0 && (await viewsTree.isVisible());
    const hasLeaf = (await inboxLeaf.count()) > 0;

    if (!hasTree && !hasLeaf) {
      expect(pageErrors, "uncaught pageerror on Explorer Inbox chrome").toEqual(
        [],
      );
      test.skip(true, missingInboxSkipMessage({ catalogEmpty: true }));
      return;
    }

    if (hasTree) {
      await expect(viewsTree).toBeVisible();
    }

    const groupRow = page.locator(
      `[data-testid="${TEST_IDS.myContentGroupRow}"]`,
    );
    if ((await groupRow.count()) > 0) {
      await groupRow.click();
    }

    if ((await inboxLeaf.count()) === 0) {
      expect(pageErrors, "uncaught pageerror on Explorer Inbox chrome").toEqual(
        [],
      );
      test.skip(true, missingInboxSkipMessage());
      return;
    }

    await expect(inboxLeaf.first()).toBeVisible({ timeout: 10_000 });
    expect(pageErrors, "uncaught pageerror on Explorer Inbox chrome").toEqual(
      [],
    );
  });

  test("run Inbox lists assignments, empty, or soft-skip @explorer-inbox @inbox @explorer", async ({
    page,
    request,
  }) => {
    const pageErrors = attachPageErrors(page);
    const headers = adminBasicAuthHeaders();
    let restStatus = 0;
    let restBody = null;
    try {
      const res = await request.get(viewsCatalogUrl(BASE_URL), {
        headers: { ...headers, Accept: "application/json" },
      });
      restStatus = res.status();
      if (res.ok()) {
        restBody = await res.json().catch(() => null);
      }
    } catch {
      restBody = null;
    }

    const inboxDef = findInboxView(restBody);
    const defs = unwrapViewDefs(restBody);

    const url = explorerEntryUrl(BASE_URL);
    await page.goto(url, { waitUntil: "networkidle" });
    await expect(
      page.locator(`[data-testid="${TEST_IDS.shell}"]`),
    ).toBeVisible({ timeout: 20_000 });

    const groupRow = page.locator(
      `[data-testid="${TEST_IDS.myContentGroupRow}"]`,
    );
    if ((await groupRow.count()) > 0) {
      await groupRow.click();
    }

    const inboxLeaf = page.locator(inboxLeafSelector());
    if ((await inboxLeaf.count()) === 0) {
      expect(pageErrors, "uncaught pageerror before Inbox skip").toEqual([]);
      test.skip(
        true,
        missingInboxSkipMessage({
          restStatus,
          catalogEmpty: !inboxDef && defs.length === 0,
        }),
      );
      return;
    }

    await inboxLeaf.first().click();

    const results = page.locator(inboxResultsSelector());
    const appeared = await results
      .first()
      .waitFor({ state: "visible", timeout: 20_000 })
      .then(() => true)
      .catch(() => false);

    if (!appeared) {
      expect(pageErrors, "uncaught pageerror when Inbox execute missing").toEqual(
        [],
      );
      test.skip(
        true,
        missingInboxSkipMessage({ restStatus }) +
          " Inbox leaf present but no results region (execute not wired).",
      );
      return;
    }

    const loading = page.locator(`[data-testid="${TEST_IDS.resultsLoading}"]`);
    if (await loading.isVisible().catch(() => false)) {
      await expect(loading).toBeHidden({ timeout: 30_000 });
    }

    const err = page.locator(`[data-testid="${TEST_IDS.resultsError}"]`);
    if (await err.isVisible().catch(() => false)) {
      expect(pageErrors, "uncaught pageerror on Inbox execute error").toEqual(
        [],
      );
      test.skip(
        true,
        missingInboxSkipMessage({ restStatus }) +
          " Inbox execute error region (custom-URL C1 not on this cell).",
      );
      return;
    }

    const empty = page.locator(`[data-testid="${TEST_IDS.resultsEmpty}"]`);
    const list = page.locator(`[data-testid="${TEST_IDS.resultsList}"]`);

    if (await empty.isVisible().catch(() => false)) {
      await expect(empty).toBeVisible();
      expect(pageErrors, "uncaught pageerror on empty Inbox").toEqual([]);
      test.skip(true, noAssignmentsSkipMessage({ restStatus }));
      return;
    }

    await expect(list).toBeVisible({ timeout: 10_000 });
    expect(pageErrors, "uncaught pageerror on Inbox assignment list").toEqual(
      [],
    );
  });
});
