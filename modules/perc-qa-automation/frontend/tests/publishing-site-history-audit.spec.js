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
 * Playwright surface: #6473 / parent #4531 — Site workspace publishing
 * history audit view with per-site CSV export. The workspace pins the
 * audit scope to the open site (POST …/sitemanage/pubstatus/logs with
 * that site id); status/search narrow the loaded rows client-side and
 * Export downloads only the visible rows as publish-history-<site>.csv.
 *
 * Polish (#6595): the truly-empty state carries a Reload history CTA,
 * filter-narrowed-to-zero shows distinct filtered-empty copy, and Export
 * with no visible rows names the export-empty error instead of
 * downloading an empty file.
 *
 * i18n keys (#6608, parent #4531): filter labels (Days / Show / Status /
 * Search), status options (All / Failed / Success), the search
 * placeholder, the failures-only label, and the Job table header resolve
 * through `PUBLISH_SITE_HISTORY_*` message keys backed by en-us-only
 * `CmsUi.tmx` TUs — no hardcoded English chrome remains on this panel.
 *
 * Parameterized count (#6622, parent #4531): the Showing-count line
 * resolves through `PUBLISH_SITE_HISTORY_COUNT` (`{0}`=visible,
 * `{1}`=total) with post-resolve substitution, so a translated template
 * with reordered placeholders still renders the counts.
 *
 * Tags: @publishing-site-history @publish
 *
 * Run (QA mode after perc-devctl qa-up):
 * npm run test:surface -- --path tests/publishing-site-history-audit.spec.js
 * from modules/perc-qa-automation/frontend
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  publishingProductUrl,
  isPubServersListUrl,
  mockPublishServer,
  isKnownPublishConsoleNoise,
} = require("./helpers/publishing-incremental-site");

const HISTORY_ROWS = [
  { jobId: 701, siteName: "AuditSite", serverName: "prod", status: "Completed" },
  { jobId: 702, siteName: "AuditSite", serverName: "stage", status: "Failed" },
];

async function stubSiteApis(page, historyBody) {
  await page.route("**/publishmanagement/servers/**", async (route) => {
    const req = route.request();
    if (req.method() === "GET" && isPubServersListUrl(req.url())) {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ PubServer: [mockPublishServer()] }),
      });
      return;
    }
    await route.continue();
  });
  await page.route("**/sitemanage/pubstatus/logs**", async (route) => {
    if (route.request().method() === "POST") {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify(historyBody),
      });
      return;
    }
    await route.continue();
  });
  await page.route("**/user/user/current**", async (route) => {
    await route.fulfill({
      status: 200,
      contentType: "application/json",
      body: JSON.stringify({ CurrentUser: { name: "Admin", email: "" } }),
    });
  });
}

async function openFirstSiteWorkspace(page) {
  await page.goto(publishingProductUrl(BASE_URL), {
    waitUntil: "domcontentloaded",
  });
  await expect(page.getByTestId("publishing-shell")).toBeVisible({
    timeout: 30000,
  });
  const siteCard = page.locator('[data-testid^="publish-site-card-"]').first();
  await expect(siteCard).toBeVisible({ timeout: 30000 });
  await siteCard.click();
  await expect(page.getByTestId("publish-site-workspace")).toBeVisible({
    timeout: 30000,
  });
  await expect(page.getByTestId("publish-site-history")).toBeVisible({
    timeout: 15000,
  });
}

function trackConsole(page) {
  const consoleErrors = [];
  page.on("pageerror", (err) => {
    consoleErrors.push(String(err));
  });
  page.on("console", (msg) => {
    if (msg.type() === "error") {
      const text = msg.text();
      if (!isKnownPublishConsoleNoise(text)) {
        consoleErrors.push(text);
      }
    }
  });
  return consoleErrors;
}

const TAGS = ["@publishing-site-history", "@publish"];

test.describe("Site workspace publishing history audit (#6473)", () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test(`audit rows filter by status and search, export downloads per-site CSV ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsole(page);
    await stubSiteApis(page, HISTORY_ROWS);
    await openFirstSiteWorkspace(page);

    const rows = page.getByTestId("publish-site-history-row");
    await expect(rows).toHaveCount(2);
    await expect(
      page.getByTestId("publish-site-history-count"),
    ).toContainText("Showing 2 of 2 history rows");

    await page.getByTestId("publish-site-history-status").selectOption("failed");
    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText("702");
    await expect(
      page.getByTestId("publish-site-history-count"),
    ).toContainText("Showing 1 of 2 history rows");

    await page.getByTestId("publish-site-history-status").selectOption("all");
    await page.getByTestId("publish-site-history-query").fill("stage");
    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText("702");

    await page.getByTestId("publish-site-history-query").fill("");
    await expect(rows).toHaveCount(2);
    const [download] = await Promise.all([
      page.waitForEvent("download"),
      page.getByTestId("publish-site-history-export").click(),
    ]);
    expect(download.suggestedFilename()).toMatch(
      /^publish-history-.*\.csv$/,
    );
    await download.path();

    await page.screenshot({
      path: "test-results/live-proof-6473-site-history.png",
    });
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`empty history shows the audit empty state, not a table ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsole(page);
    await stubSiteApis(page, []);
    await openFirstSiteWorkspace(page);

    await expect(
      page.getByTestId("publish-site-history-empty"),
    ).toBeVisible({ timeout: 15000 });
    await expect(
      page.getByTestId("publish-site-history-table"),
    ).toHaveCount(0);
    await expect(
      page.getByTestId("publish-site-history-count"),
    ).toContainText("Showing 0 of 0 history rows");
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`empty state offers a reload CTA that re-reads history ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsole(page);
    await stubSiteApis(page, []);
    await openFirstSiteWorkspace(page);

    const reload = page.getByTestId("publish-site-history-empty-reload");
    await expect(reload).toBeVisible({ timeout: 15000 });
    await expect(reload).toContainText("Reload history");
    await reload.click();
    await expect(
      page.getByTestId("publish-site-history-empty"),
    ).toBeVisible({ timeout: 15000 });
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`filters that hide every row show filtered-empty copy, not the run-a-publish hint ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsole(page);
    await stubSiteApis(page, HISTORY_ROWS);
    await openFirstSiteWorkspace(page);

    await expect(
      page.getByTestId("publish-site-history-row"),
    ).toHaveCount(2);
    await page.getByTestId("publish-site-history-query").fill("no-such-server-zzz");
    await expect(
      page.getByTestId("publish-site-history-empty-filtered"),
    ).toBeVisible({ timeout: 15000 });
    await expect(
      page.getByTestId("publish-site-history-empty-filtered"),
    ).toContainText("No history rows match the current filters");
    await expect(
      page.getByTestId("publish-site-history-empty"),
    ).toHaveCount(0);
    await expect(
      page.getByTestId("publish-site-history-table"),
    ).toHaveCount(0);
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`filter and table labels resolve through i18n keys (#6608) ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsole(page);
    await stubSiteApis(page, HISTORY_ROWS);
    await openFirstSiteWorkspace(page);

    const filters = page.getByTestId("publish-site-history-filters");
    for (const label of ["Days", "Show", "Status", "Search"]) {
      await expect(filters).toContainText(label);
    }
    await expect(
      page.getByTestId("publish-site-history-status"),
    ).toContainText("All");
    await expect(
      page.getByTestId("publish-site-history-status"),
    ).toContainText("Failed");
    await expect(
      page.getByTestId("publish-site-history-status"),
    ).toContainText("Success");
    await expect(page.getByTestId("publish-site-history-query")).toHaveAttribute(
      "placeholder",
      "Server, job, status",
    );
    await expect(filters).toContainText("Failures only (server)");
    const headers = page.getByTestId("publish-site-history-table").locator("th");
    await expect(headers.nth(0)).toContainText("Job");

    await page.screenshot({
      path: "test-results/live-proof-6608-site-history-i18n.png",
    });
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`count line resolves through the parameterized i18n key (#6622) ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsole(page);
    // Session-locale stand-in: the count key maps to a reordered template
    // while every other key echoes back (untranslated), exactly like the
    // tmx.jsp?mode=js bundle. The stub ignores the args array, so the UI
    // must substitute {0}/{1} after the resolve for the counts to render.
    const catalogJson = JSON.stringify({
      "perc.ui.publish.modern@Showing {0} of {1} history rows":
        "Verlauf: {1} gesamt, {0} sichtbar",
    }).replace(/</g, "\\u003c");
    await page.route("**/tmx/tmx.jsp*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "text/javascript; charset=UTF-8",
        body: `window.I18N = { message: function (k) { var d = ${catalogJson}; return Object.prototype.hasOwnProperty.call(d, k) ? d[k] : k; } };`,
      }),
    );
    await stubSiteApis(page, HISTORY_ROWS);
    await openFirstSiteWorkspace(page);

    await expect(page.getByTestId("publish-site-history-row")).toHaveCount(
      2,
    );
    await expect(
      page.getByTestId("publish-site-history-count"),
    ).toContainText("Verlauf: 2 gesamt, 2 sichtbar");

    await page.getByTestId("publish-site-history-status").selectOption("failed");
    await expect(
      page.getByTestId("publish-site-history-count"),
    ).toContainText("Verlauf: 2 gesamt, 1 sichtbar");

    await page.screenshot({
      path: "test-results/live-proof-6622-site-history-count-i18n.png",
    });
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });

  test(`export with no visible rows names the export-empty error, no download ${TAGS.join(" ")}`, async ({
    page,
  }) => {
    const consoleErrors = trackConsole(page);
    await stubSiteApis(page, HISTORY_ROWS);
    await openFirstSiteWorkspace(page);

    await expect(
      page.getByTestId("publish-site-history-row"),
    ).toHaveCount(2);
    await page.getByTestId("publish-site-history-query").fill("no-such-server-zzz");
    await expect(
      page.getByTestId("publish-site-history-empty-filtered"),
    ).toBeVisible({ timeout: 15000 });
    await page.getByTestId("publish-site-history-export").click();
    await expect(
      page.getByTestId("publish-site-history-export-error"),
    ).toContainText("No visible history rows to export");

    await page.screenshot({
      path: "test-results/live-proof-6595-site-history-polish.png",
    });
    expect(consoleErrors, consoleErrors.join("\n")).toEqual([]);
  });
});
