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
 * Explorer detail-list loading/empty/error states (#7036 / parent #4530).
 *
 * <p>Every user-visible list-state string resolves through EXPLORER_MSG
 * (en-us TMX TUs land with this slice): loading announces via
 * {@code role=status}, the empty state keeps stable
 * {@code detail-list-empty}/{@code detail-list-empty-hint} testids, and
 * the error names the failed folder with a Retry that re-runs the fetch.</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/explorer-detail-list-states.spec.js}
 * from {@code modules/perc-qa-automation/frontend}.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const { explorerSpaUrl } = require("./helpers/explorer-menu-bar");
const { expectNoSeriousA11yViolations } = require("./helpers/a11y");
const {
  isKnownExplorerSitesConsoleNoise,
} = require("./helpers/explorer-copy-item");

const TAGS = ["@explorer", "@explorer-detail-list-states"];

const ROW_BODY = JSON.stringify({
  PagedItemList: {
    childrenInPage: [
      {
        id: "ls-1",
        name: "ListStateRow",
        path: "/Sites/Demo/ListStateRow",
        type: "percPage",
        category: "page",
        accessLevel: "READ",
        leaf: true,
      },
    ],
    childrenCount: 1,
    startIndex: 0,
  },
});

const EMPTY_BODY = JSON.stringify({
  PagedItemList: {
    childrenInPage: [],
    childrenCount: 0,
    startIndex: 0,
  },
});

const PAGE0_BODY = JSON.stringify({
  PagedItemList: {
    childrenInPage: [
      {
        id: "ls-p0",
        name: "ListStatePageZero",
        path: "/Sites/Demo/ListStatePageZero",
        type: "percPage",
        category: "page",
        accessLevel: "READ",
        leaf: true,
      },
    ],
    childrenCount: 120,
    startIndex: 0,
  },
});

const PAGE1_BODY = JSON.stringify({
  PagedItemList: {
    childrenInPage: [
      {
        id: "ls-p1",
        name: "ListStatePageOne",
        path: "/Sites/Demo/ListStatePageOne",
        type: "percPage",
        category: "page",
        accessLevel: "READ",
        leaf: true,
      },
    ],
    childrenCount: 120,
    startIndex: 50,
  },
});

/**
 * Folder path segment from a paginatedFolder request URL, e.g.
 * `.../paginatedFolder/Sites/Demo?startIndex=0` → `/Sites/Demo`.
 * @param {string} url
 * @returns {string}
 */
function folderPathFromListingUrl(url) {
  const marker = "/paginatedFolder";
  const at = String(url).indexOf(marker);
  if (at < 0) return "";
  const after = String(url).slice(at + marker.length).split("?")[0];
  return after.startsWith("/") ? after : `/${after}`;
}

async function openShell(page) {
  await page.goto(explorerSpaUrl(BASE_URL));
  await page.waitForLoadState("networkidle");
  await expect(page.locator('[data-testid="content-explorer-shell"]')).toBeVisible({
    timeout: 20_000,
  });
}

test.describe("Explorer detail-list loading/empty/error states (#7036)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(120_000);
    await loginAsAdmin(page);
  });

  test(
    "loading announces via role=status, then rows render",
    { tag: TAGS },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      // Hold the first listing response until the loading state is
      // asserted, so the assertion cannot race the fetch.
      let releaseListing;
      const gate = new Promise((resolve) => {
        releaseListing = resolve;
      });
      let listingHits = 0;
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        listingHits += 1;
        if (listingHits === 1) {
          await gate;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: ROW_BODY,
        });
      });
      try {
        // NOTE: no networkidle wait here — the gated listing response
        // below intentionally holds one request open, so networkidle
        // would never fire. domcontentloaded + the shell/loading
        // assertions are the readiness signal for this test.
        await page.goto(explorerSpaUrl(BASE_URL), {
          waitUntil: "domcontentloaded",
        });
        await expect(
          page.locator('[data-testid="content-explorer-shell"]'),
        ).toBeVisible({ timeout: 20_000 });
        const loading = page.locator('[data-testid="detail-list-loading"]');
        await expect(loading).toBeVisible({ timeout: 20_000 });
        await expect(loading).toHaveAttribute("role", "status");
        await expect(loading).toContainText(/Loading items/);
        await page.screenshot({
          path: "test-results/live-proof-7036-detail-list-loading.png",
        });
        releaseListing();
        await expect(page.locator('[data-testid="detail-row-ls-1"]')).toBeVisible({
          timeout: 20_000,
        });
        await expect(loading).toHaveCount(0);
        await expectNoSeriousA11yViolations(page, {
          scope: '[data-testid="content-explorer-shell"]',
        });
        const related = pageErrors.filter(
          (text) => !isKnownExplorerSitesConsoleNoise(text),
        );
        expect(related, related.join("\n")).toEqual([]);
      } finally {
        await page.unroute("**/pathmanagement/path/paginatedFolder**").catch(() => undefined);
      }
    },
  );

  test(
    "empty folder keeps stable empty testids with accessible names",
    { tag: TAGS },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: EMPTY_BODY,
        });
      });
      try {
        await openShell(page);
        const empty = page.locator('[data-testid="detail-list-empty"]');
        await expect(empty).toBeVisible({ timeout: 20_000 });
        await expect(empty).toHaveAttribute("role", "status");
        await expect(empty).toContainText(/No items in this folder/);
        const hint = page.locator('[data-testid="detail-list-empty-hint"]');
        await expect(hint).toBeVisible();
        await expect(hint).toContainText(/New Item/);
        await page.screenshot({
          path: "test-results/live-proof-7036-detail-list-empty.png",
        });
        await expectNoSeriousA11yViolations(page, {
          scope: '[data-testid="content-explorer-shell"]',
        });
        const related = pageErrors.filter(
          (text) => !isKnownExplorerSitesConsoleNoise(text),
        );
        expect(related, related.join("\n")).toEqual([]);
      } finally {
        await page.unroute("**/pathmanagement/path/paginatedFolder**").catch(() => undefined);
      }
    },
  );

  test(
    "failed fetch names the folder; Retry re-runs the fetch and rows render",
    { tag: TAGS },
    async ({ page }) => {
      const jsErrors = [];
      page.on("pageerror", (err) => jsErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() !== "error") return;
        const text = msg.text();
        // The stubbed 500 is the failure under test; the browser logs
        // the failed resource (same filter as the #6874 catalog spec).
        if (
          /Failed to load resource: the server responded with a status of (404|400|500)/i.test(
            text,
          )
        ) {
          return;
        }
        jsErrors.push(text);
      });
      let listingHits = 0;
      let requestedFolder = "";
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        listingHits += 1;
        if (!requestedFolder) {
          requestedFolder = folderPathFromListingUrl(route.request().url());
        }
        if (listingHits === 1) {
          await route.fulfill({
            status: 500,
            contentType: "application/json",
            body: "{}",
          });
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: ROW_BODY,
        });
      });
      try {
        await openShell(page);
        const error = page.locator('[data-testid="detail-list-error"]');
        await expect(error).toBeVisible({ timeout: 20_000 });
        await expect(error).toHaveAttribute("role", "alert");
        await expect(
          page.locator('[data-testid="detail-list-error-summary"]'),
        ).toContainText(/Failed to load items/);
        // The error names the folder whose list failed, not a bare failure.
        const folder = page.locator('[data-testid="detail-list-error-folder"]');
        await expect(folder).toBeVisible();
        await expect(folder).toContainText(/Folder:/);
        if (requestedFolder) {
          await expect(folder).toContainText(requestedFolder);
        }
        const retry = page.locator('[data-testid="detail-list-retry"]');
        await expect(retry).toBeVisible();
        await expect(retry).toHaveAccessibleName(/Retry/);
        await page.screenshot({
          path: "test-results/live-proof-7036-detail-list-error.png",
        });
        await retry.click();
        await expect(page.locator('[data-testid="detail-row-ls-1"]')).toBeVisible({
          timeout: 20_000,
        });
        await expect(error).toHaveCount(0);
        expect(listingHits).toBeGreaterThanOrEqual(2);
        await page.screenshot({
          path: "test-results/live-proof-7036-detail-list-error-retry.png",
        });
        await expectNoSeriousA11yViolations(page, {
          scope: '[data-testid="content-explorer-shell"]',
        });
        const related = jsErrors.filter(
          (text) => !isKnownExplorerSitesConsoleNoise(text),
        );
        expect(related, related.join("\n")).toEqual([]);
      } finally {
        await page.unroute("**/pathmanagement/path/paginatedFolder**").catch(() => undefined);
      }
    },
  );

  test(
    "page turn announces via role=status with aria-busy and busy pager (#7089)",
    { tag: TAGS },
    async ({ page }) => {
      const pageErrors = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      // Hold the second-page listing response until the page-turn
      // loading state is asserted, so the assertion cannot race the fetch.
      let releasePage2;
      const gate = new Promise((resolve) => {
        releasePage2 = resolve;
      });
      let listingHits = 0;
      await page.route("**/pathmanagement/path/paginatedFolder**", async (route) => {
        listingHits += 1;
        if (listingHits === 1) {
          await route.fulfill({
            status: 200,
            contentType: "application/json",
            body: PAGE0_BODY,
          });
          return;
        }
        await gate;
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: PAGE1_BODY,
        });
      });
      try {
        await openShell(page);
        await expect(page.locator('[data-testid="detail-row-ls-p0"]')).toBeVisible({
          timeout: 20_000,
        });
        // The pager offers a next page (120 items over a 50-row page).
        const next = page.locator('[data-testid="detail-pager-next"]');
        await expect(next).toBeEnabled({ timeout: 20_000 });
        await next.click();
        // Stale rows stay mounted while the next page fetches, with a
        // polite live-region announcement on the existing loading key.
        const more = page.locator('[data-testid="detail-list-loading-more"]');
        await expect(more).toBeVisible({ timeout: 20_000 });
        await expect(more).toHaveAttribute("role", "status");
        await expect(more).toContainText(/Loading items/);
        await expect(
          page.locator('[data-testid="detail-row-ls-p0"]'),
        ).toBeVisible();
        // The first-load region stays unmounted — exactly one live region.
        await expect(page.locator('[data-testid="detail-list-loading"]')).toHaveCount(0);
        // The table and pager expose the busy state (the list root
        // stays busy-free so the live region is never inside a busy
        // container, #7089 Codex review); pager controls are busy
        // (disabled + aria-disabled) while in flight.
        await expect(page.locator('[data-testid="detail-list"]')).not.toHaveAttribute(
          "aria-busy",
          "true",
        );
        await expect(page.locator('[data-testid="detail-table"]')).toHaveAttribute(
          "aria-busy",
          "true",
        );
        await expect(page.locator('[data-testid="detail-pagination"]')).toHaveAttribute(
          "aria-busy",
          "true",
        );
        await expect(page.locator('[data-testid="detail-pager-prev"]')).toBeDisabled();
        await expect(next).toBeDisabled();
        await expect(page.locator('[data-testid="detail-pager-prev"]')).toHaveAttribute(
          "aria-disabled",
          "true",
        );
        await expect(next).toHaveAttribute("aria-disabled", "true");
        await page.screenshot({
          path: "test-results/live-proof-7089-detail-list-page-turn.png",
        });
        releasePage2();
        await expect(page.locator('[data-testid="detail-row-ls-p1"]')).toBeVisible({
          timeout: 20_000,
        });
        // The region persists but clears — no stale announcement text.
        await expect(more).toBeEmpty();
        await expect(next).toBeEnabled();
        await expectNoSeriousA11yViolations(page, {
          scope: '[data-testid="content-explorer-shell"]',
        });
        const related = pageErrors.filter(
          (text) => !isKnownExplorerSitesConsoleNoise(text),
        );
        expect(related, related.join("\n")).toEqual([]);
      } finally {
        await page.unroute("**/pathmanagement/path/paginatedFolder**").catch(() => undefined);
      }
    },
  );
});
