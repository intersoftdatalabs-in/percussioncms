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
 * Explorer Views Playwright + a11y surface (#3117 / parent #3110 / #3102).
 *
 * <p>Does <strong>not</strong> rebuild the Views catalog UI (that is V2
 * #3116 / cluster PR #3252). This spec is additive: it drives the V2
 * testids when present and <strong>soft-skips</strong> with a clear
 * reason when the H2 cell has no views or V2 chrome is not on the
 * build under test.</p>
 *
 * <h3>Unattended surface (QA mode)</h3>
 * <pre>
 *   python docker/scripts/perc-devctl.py qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=http://127.0.0.1:${QA_CMS_HOST_PORT} \
 *     ADMIN_USERNAME=Admin ADMIN_PASSWORD=&lt;from-qa-up&gt; \
 *     npm run test:surface -- --path tests/explorer-views.spec.js
 *   # tags:
 *   npm run test:surface -- --tag explorer-views
 *   npm run test:surface -- --tag views
 *   # list only (no live CMS):
 *   npm run test:surface:list -- --path tests/explorer-views.spec.js
 *   python docker/scripts/perc-devctl.py qa-down
 * </pre>
 *
 * <p><strong>Fixture / chrome soft-skip:</strong> V2 tree missing
 * ({@code explorer-views-tree}) or GET /services/views has no runnable
 * standard view. Explorer shell load remains a hard assertion.</p>
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
  PARENT_MY_CONTENT,
  explorerEntryUrl,
  viewsCatalogUrl,
  unwrapViewDefs,
  isCustomUrlView,
  isInboxView,
  viewParentCategory,
  viewDefKey,
  viewDefLabel,
  pickRunnableStandardView,
  noViewsChromeSkipMessage,
  noRunnableViewSkipMessage,
  postExecuteRegionSelector,
  attachConsoleErrorCollector,
} = require("./helpers/explorer-views");

/**
 * Wait briefly for V2 Views chrome; return whether the tree mounted.
 *
 * @param {import("@playwright/test").Page} page
 * @returns {Promise<boolean>}
 */
async function viewsChromeVisible(page) {
  const tree = page.getByTestId(TEST_IDS.viewsTree);
  try {
    await tree.waitFor({ state: "visible", timeout: 8_000 });
    return true;
  } catch {
    return false;
  }
}

/**
 * Expand a Views parent category if the group row is collapsed.
 *
 * @param {import("@playwright/test").Page} page
 * @param {number} category parentCategory (1 = My Content)
 * @returns {Promise<void>}
 */
async function expandViewsGroup(page, category) {
  const group = page.getByTestId(TEST_IDS.group(category));
  await expect(group).toBeVisible({ timeout: 10_000 });
  const row = page.getByTestId(TEST_IDS.groupRow(category));
  await expect(row).toBeVisible();
  const expanded = await row.getAttribute("aria-expanded");
  if (expanded !== "true") {
    await row.click();
  }
  await expect(row).toHaveAttribute("aria-expanded", "true");
}

/**
 * Expand My Content (parentCategory=1) if the group row is collapsed.
 *
 * @param {import("@playwright/test").Page} page
 * @returns {Promise<void>}
 */
async function expandMyContent(page) {
  await expandViewsGroup(page, PARENT_MY_CONTENT);
}

// Tags live on individual test() titles only — Playwright ignores @tags on describe names.
test.describe("Explorer Views Playwright + a11y (#3117 / #3110)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("Views category visible, expand My Content, a11y on Views surface @explorer-views @views @a11y @explorer", async ({
    page,
  }) => {
    const consoleErrors = attachConsoleErrorCollector(page);
    await page.goto(explorerEntryUrl(BASE_URL), { waitUntil: "networkidle" });
    await expect(page.getByTestId(TEST_IDS.shell)).toBeVisible({
      timeout: 30_000,
    });

    const hasChrome = await viewsChromeVisible(page);
    if (!hasChrome) {
      test.info().annotations.push({
        type: "soft-skip",
        description: noViewsChromeSkipMessage(),
      });
      test.skip(true, noViewsChromeSkipMessage());
      return;
    }

    await expect(page.getByTestId(TEST_IDS.viewsRoot)).toBeVisible();
    await expandMyContent(page);

    await expectNoSeriousA11yViolations(page, {
      scope: `[data-testid="${TEST_IDS.viewsTree}"]`,
    });

    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test("select a standard view when catalog non-empty; results or soft-skip @explorer-views @views @a11y @explorer", async ({
    page,
    request,
  }) => {
    const consoleErrors = attachConsoleErrorCollector(page);

    const headers = adminBasicAuthHeaders();
    const catalogUrl = viewsCatalogUrl(BASE_URL);
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
    } catch {
      restBody = null;
    }

    const runnable = pickRunnableStandardView(restBody);
    const defs = unwrapViewDefs(restBody);
    const onlyCustom =
      defs.length > 0 &&
      defs.every((d) => isCustomUrlView(d) || isInboxView(d));

    await page.goto(explorerEntryUrl(BASE_URL), { waitUntil: "networkidle" });
    await expect(page.getByTestId(TEST_IDS.shell)).toBeVisible({
      timeout: 30_000,
    });

    const hasChrome = await viewsChromeVisible(page);
    if (!hasChrome) {
      test.info().annotations.push({
        type: "soft-skip",
        description: noViewsChromeSkipMessage(),
      });
      test.skip(true, noViewsChromeSkipMessage());
      return;
    }

    await expandMyContent(page);

    if (!runnable) {
      test.info().annotations.push({
        type: "soft-skip",
        description: noRunnableViewSkipMessage({
          empty: defs.length === 0,
          onlyCustom,
          restStatus,
        }),
      });
      test.skip(
        true,
        noRunnableViewSkipMessage({
          empty: defs.length === 0,
          onlyCustom,
          restStatus,
        }),
      );
      return;
    }

    const leaf = page.getByTestId(TEST_IDS.leaf(runnable.key));
    const inboxLeaf = page.getByTestId(TEST_IDS.inboxLeaf);
    if (!(await leaf.isVisible().catch(() => false))) {
      // Leaf may live under another category; expand that group and wait
      // for children (aria-expanded + leaf visible) before skip checks.
      if (
        runnable.parentCategory &&
        runnable.parentCategory !== PARENT_MY_CONTENT
      ) {
        const otherRow = page.getByTestId(
          TEST_IDS.groupRow(runnable.parentCategory),
        );
        if (await otherRow.isVisible().catch(() => false)) {
          await expandViewsGroup(page, runnable.parentCategory);
          await leaf
            .waitFor({ state: "visible", timeout: 10_000 })
            .catch(() => undefined);
        }
      }
    }

    if (!(await leaf.isVisible().catch(() => false))) {
      // Inbox is not a standard-view execute; do not click it as a fallback.
      if (await inboxLeaf.isVisible().catch(() => false)) {
        test.skip(
          true,
          noRunnableViewSkipMessage({
            onlyCustom: true,
            restStatus,
          }),
        );
        return;
      }
      test.skip(
        true,
        noRunnableViewSkipMessage({ empty: true, restStatus }),
      );
      return;
    }

    await leaf.click();

    await expect(page.locator(postExecuteRegionSelector()).first()).toBeVisible(
      {
        timeout: 30_000,
      },
    );

    const loading = page.getByTestId(TEST_IDS.resultsLoading);
    if (await loading.isVisible().catch(() => false)) {
      await expect(loading).toBeHidden({ timeout: 30_000 });
    }

    const results = page.getByTestId(TEST_IDS.results);
    await expect(results).toBeVisible({ timeout: 10_000 });

    const list = page.getByTestId(TEST_IDS.resultsList);
    const empty = page.getByTestId(TEST_IDS.resultsEmpty);
    const err = page.getByTestId(TEST_IDS.resultsError);
    if (await list.isVisible().catch(() => false)) {
      await expect(page.getByTestId(TEST_IDS.resultRow).first()).toBeVisible();
    } else if (await empty.isVisible().catch(() => false)) {
      await expect(empty).toBeVisible();
    } else {
      await expect(err).toBeVisible();
    }

    await expectNoSeriousA11yViolations(page, {
      scope: `[data-testid="${TEST_IDS.viewsTree}"], [data-testid="${TEST_IDS.results}"]`,
    });

    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test("All Content lists unique view leaves (no seven All dups) @explorer-views @views @3325", async ({
    page,
    request,
  }) => {
    const consoleErrors = attachConsoleErrorCollector(page);
    const headers = adminBasicAuthHeaders();
    let restAllLabels = [];
    try {
      const res = await request.get(viewsCatalogUrl(BASE_URL), {
        headers: { ...headers, Accept: "application/json" },
      });
      if (res.ok()) {
        const defs = unwrapViewDefs(await res.json().catch(() => null));
        restAllLabels = defs
          .filter((d) => viewParentCategory(d) === 3)
          .map((d) => viewDefLabel(d) || viewDefKey(d));
      }
    } catch {
      restAllLabels = [];
    }

    await page.goto(explorerEntryUrl(BASE_URL), { waitUntil: "networkidle" });
    await expect(page.getByTestId(TEST_IDS.shell)).toBeVisible({
      timeout: 30_000,
    });

    const hasChrome = await viewsChromeVisible(page);
    if (!hasChrome) {
      test.skip(true, noViewsChromeSkipMessage());
      return;
    }

    await expandViewsGroup(page, 3);
    const group = page.getByTestId(TEST_IDS.group(3));
    const leafLabels = await group.locator('[role="treeitem"][data-testid^="explorer-views-leaf-"]').allTextContents();
    const trimmed = leafLabels.map((s) => s.replace(/\s+/g, " ").trim()).filter(Boolean);
    const allExact = trimmed.filter((s) => /^all$/i.test(s));
    expect(
      allExact.length,
      `All Content should not list seven identical All leaves (ui=${JSON.stringify(trimmed)} rest=${JSON.stringify(restAllLabels)})`,
    ).toBeLessThanOrEqual(1);

    const keys = await group.locator('[data-testid^="explorer-views-leaf-"]').evaluateAll((els) =>
      els.map((el) => el.getAttribute("data-testid")),
    );
    expect(new Set(keys).size, `duplicate leaf testids: ${keys.join(",")}`).toBe(
      keys.length,
    );

    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test("REST: GET /services/views answers for Admin @explorer-views @views", async ({
    request,
  }) => {
    test.setTimeout(45_000);
    const headers = adminBasicAuthHeaders();
    const res = await request.get(viewsCatalogUrl(BASE_URL), {
      headers: { ...headers, Accept: "application/json" },
    });
    expect(
      res.status(),
      `GET views catalog should not 5xx (status=${res.status()})`,
    ).toBeLessThan(500);
    expect([200, 401, 403]).toContain(res.status());
    if (res.status() === 200) {
      const body = await res.json().catch(() => null);
      expect(
        body == null || typeof body === "object" || Array.isArray(body),
      ).toBe(true);
      const defs = unwrapViewDefs(body);
      expect(Array.isArray(defs)).toBe(true);
    }
  });
});
