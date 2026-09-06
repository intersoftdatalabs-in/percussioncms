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
 * Developer Sites → Virtual Site source panel
 * (#2956 / #3020 / #3300 / #3687 / #3697 / #3699 / #3707 / #3735 / #3759 / #3778 / #3796 / #3808 / #3820 / #3856 / #3868 / #3869 / #3870 / #3879 / #3927 / #3928 / #3931 / #3989 / #4115 / #4164 / #4165 / #4166 / epic #2678).
 *
 * Opens Sites catalog detail and asserts the Virtual Site source section mounts
 * with source-kind control (repository default, git-filesystem, csv-filesystem,
 * sql-database, http-json, object-storage, rss-atom, icalendar, sitemap-xml, robots-txt), save chrome, Build + Preview + Publish chrome for git-filesystem,
 * csv-filesystem, sql-database, http-json, object-storage, rss-atom, and icalendar (never repository). rss-atom save/GET-roundtrip
 * uses a local fixture rootPath only (no live feed credentials, no virtual.remoteUrl);
 * Build, Preview, and Publish chrome are shown after save for rss-atom. icalendar save/GET-roundtrip
 * uses a local RFC 5545 fixture rootPath only (no CalDAV, no virtual.remoteUrl);
 * Build, Preview, and Publish chrome are shown after save for icalendar. sitemap-xml save/GET-roundtrip
 * uses a portable-safe local rootPath only (no live crawl credentials, no virtual.remoteUrl);
 * Build, Preview, and Publish chrome are shown after save for sitemap-xml. robots-txt save/GET-roundtrip
 * uses a portable-safe local rootPath only (no live crawl credentials, no virtual.remoteUrl);
 * Build, Preview, and Publish chrome stay hidden (later slices). Also intercepts build REST
 * to prove link-problem detail lines render on HTTP 200 and publish REST to prove
 * dest path + files copied on HTTP 200 (including csv-filesystem, http-json, object-storage, rss-atom, and icalendar). Live H2 QA
 * deploys a CSV tree into the cell and asserts POST /virtual/build, GET
 * /virtual/preview home HTML, and POST /virtual/publish complete. SQL save
 * persists sourceKind=sql-database and live Build then Publish complete after save
 * (in-memory H2 SELECT fixture; published HTML exists under the Site filesystem root).
 * HTTP JSON live Build deploys a local pages.json fixture and asserts pagesWritten > 0
 * then Preview assembled site streams last-build home HTML; after Build, Publish
 * succeeds and 8.2/index.html exists under the Site filesystem root. object-storage live
 * Build deploys a local object-key directory and asserts pagesWritten > 0; live Preview
 * then streams last-build home HTML; after Build, Publish succeeds and 8.2/index.html
 * exists under the Site filesystem root (local object-key fixture only; remoteUrl/secrets stay 400).
 * rss-atom live Build deploys a local feed.xml fixture and asserts pagesWritten > 0; live Preview
 * then streams last-build home HTML; after Build, Publish succeeds and 8.2/index.html
 * exists under the Site filesystem root (local RSS/Atom fixture only; remoteUrl/credentials stay 400).
 * icalendar live Build deploys a local calendar.ics fixture and asserts pagesWritten > 0; live Preview
 * then streams last-build home HTML; after Build, Publish succeeds and 8.2/index.html
 * exists under the Site filesystem root (local RFC 5545 fixture only; remoteUrl/credentials stay 400).
 * sitemap-xml live Build deploys a local sitemap.xml fixture and asserts pagesWritten > 0.
 * sitemap-xml live Preview deploys a local sitemap.xml fixture, Builds, then streams last-build
 * home HTML (no live crawl). sitemap-xml live Publish deploys a local sitemap.xml fixture then
 * POST /virtual/publish copies assembled HTML; leftover remoteUrl/credentials stay 400 (no live crawl).
 * sitemap-xml live rebuild copies an edited sitemap.xml into the cell (docker cp), runs a
 * second Build without restarting Jetty, and asserts assembled HTML / pagesWritten change.
 *
 * Surface-filtered QA mode:
 * <pre>
 *   perc-devctl qa-up
 *   cd modules/perc-qa-automation/frontend
 *   TEST_CMS_URL=… ADMIN_USERNAME=Admin ADMIN_PASSWORD=… \
 *     npm run test:surface -- --path tests/developer-site-virtual-source.spec.js
 *   perc-devctl qa-down
 * </pre>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  catalogRowsSelector,
} = require("./helpers/developer-catalog-selectors");
const { deployCsvVirtualFixtureToQaCell } = require("./helpers/csv-virtual-qa-fixture");
const {
  deploySqlVirtualFixtureToQaCell,
  assertPublishedSqlFilesOnQaCell,
} = require("./helpers/sql-virtual-qa-fixture");
const {
  deployHttpJsonVirtualFixtureToQaCell,
  assertPublishedHttpJsonFilesOnQaCell,
} = require("./helpers/http-json-virtual-qa-fixture");
const {
  deployObjectStorageVirtualFixtureToQaCell,
  assertPublishedObjectStorageFilesOnQaCell,
} = require("./helpers/object-storage-virtual-qa-fixture");
const {
  deployRssAtomVirtualFixtureToQaCell,
  assertPublishedRssAtomFilesOnQaCell,
} = require("./helpers/rss-atom-virtual-qa-fixture");
const {
  deployIcalendarVirtualFixtureToQaCell,
  assertPublishedIcalendarFilesOnQaCell,
} = require("./helpers/icalendar-virtual-qa-fixture");
const {
  deploySitemapXmlVirtualFixtureToQaCell,
  assertPublishedSitemapXmlFilesOnQaCell,
  copySitemapXmlRebuildIntoQaCell,
  SITEMAP_XML_VIRTUAL_BUILD_MARKER,
  SITEMAP_XML_VIRTUAL_REBUILD_MARKER,
  SITEMAP_XML_VIRTUAL_REBUILD_LASTMOD,
} = require("./helpers/sitemap-xml-virtual-qa-fixture");
const { saveVirtualSiteAndExpectSaved } = require("./helpers/virtual-site-save");
const {
  missingVirtualSourceKindValues,
  formatMissingVirtualSourceKindMessage,
} = require("./helpers/virtual-source-kind-options");

function developerSectionUrl(section) {
  const q = new URLSearchParams({
    entry: "developer",
    section,
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

/**
 * Last-build home HTML via Admin preview REST (same-origin cookies).
 * Path segments are encoded; remaining {@code ..} is rejected.
 */
async function readVirtualPreviewHomeHtml(page, siteName) {
  const statusUrl = `${BASE_URL}/Rhythmyx/services/sites/${encodeURIComponent(siteName)}/virtual/preview`;
  const statusResp = await page.request.get(statusUrl);
  const statusBody = await statusResp.text();
  expect(
    statusResp.ok(),
    `GET /virtual/preview HTTP ${statusResp.status()}: ${statusBody}`,
  ).toBeTruthy();
  let statusJson = {};
  try {
    statusJson = JSON.parse(statusBody);
  } catch {
    throw new Error(`Preview status was not JSON: ${statusBody}`);
  }
  const statusRoot =
    statusJson.VirtualSitePreviewStatus ||
    statusJson.virtualSitePreviewStatus ||
    statusJson;
  expect(
    statusRoot.available === true || statusRoot.available === "true",
    `preview available: ${statusBody}`,
  ).toBeTruthy();
  const homePath = String(statusRoot.homePath || "")
    .replace(/\\/g, "/")
    .replace(/^\/+/, "");
  expect(homePath, `homePath in ${statusBody}`).toMatch(/index\.html$/);
  const fileUrl = `${BASE_URL}/Rhythmyx/services/sites/${encodeURIComponent(siteName)}/virtual/preview/${homePath
    .split("/")
    .filter((seg) => seg.length > 0 && seg !== "." && seg !== "..")
    .map((seg) => encodeURIComponent(seg))
    .join("/")}`;
  const fileResp = await page.request.get(fileUrl);
  const html = await fileResp.text();
  expect(
    fileResp.ok(),
    `GET preview home HTTP ${fileResp.status()} ${fileUrl}: ${html.slice(0, 400)}`,
  ).toBeTruthy();
  return html;
}

test.describe("Developer Site Virtual Site source panel (#2956 / #3020)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));
    page.__virtPageErrors = pageErrors;
    await loginAsAdmin(page);
  });

  test("Sites detail shows Virtual Site source panel with repository default", async ({
    page,
  }) => {
    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    // Wait for catalog panel, empty, or error (H2 QA may have zero sites)
    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const empty = page.locator('[data-testid="developer-site-empty"]');
    if (await empty.isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — Virtual Site panel requires a site row",
      });
      return;
    }
    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-detail"]')).toBeVisible({
      timeout: 15_000,
    });

    const virtualSection = page.locator('[data-testid="developer-site-virtual"]');
    await expect(virtualSection).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('[data-testid="developer-site-virtual-title"]')).toBeVisible();

    // Load success or error (REST may 404 if site name only on summary list)
    const formOrError = page.locator(
      [
        '[data-testid="developer-site-virtual-form"]',
        '[data-testid="developer-site-virtual-error"]',
      ].join(", "),
    );
    await expect(formOrError.first()).toBeVisible({ timeout: 20_000 });

    if (await page.locator('[data-testid="developer-site-virtual-form"]').isVisible()) {
      const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
      await expect(kind).toBeVisible();
      const liveKindValues = await kind.locator("option").evaluateAll((opts) =>
        opts.map((o) => (o.getAttribute("value") || o.value || "").trim()),
      );
      const missingKinds = missingVirtualSourceKindValues(liveKindValues);
      expect(
        missingKinds,
        formatMissingVirtualSourceKindMessage(missingKinds, liveKindValues),
      ).toEqual([]);
      await expect(kind.locator('option[value="object-storage"]')).toHaveCount(1);
      await expect(kind.locator('option[value="rss-atom"]')).toHaveCount(1);
      await expect(kind.locator('option[value="icalendar"]')).toHaveCount(1);
      await expect(kind.locator('option[value="sitemap-xml"]')).toHaveCount(1);
      await expect(kind.locator('option[value="robots-txt"]')).toHaveCount(1);
      await expect(kind.locator('option[value="sql-api"]')).toHaveCount(0);
      // Default traditional sites use repository option
      await expect(kind).toHaveValue(
        /repository|git-filesystem|csv-filesystem|sql-database|http-json|object-storage|rss-atom|icalendar|sitemap-xml|robots-txt/,
      );
      await expect(page.locator('[data-testid="developer-site-virtual-save"]')).toBeVisible();

      // Repository mode: Build chrome must not appear (no misleading virtual-build UI)
      const current = await kind.inputValue();
      if (current === "repository" || current === "") {
        await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
        await expect(
          page.locator('[data-testid="developer-site-virtual-build-section"]'),
        ).toHaveCount(0);
        await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
        await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
      }

      // Switch to csv-filesystem reveals root path + Build / Publish (no Git remotes)
      await kind.selectOption("csv-filesystem");
      await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-csv-hint"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-csv-hint"]')).toContainText(
        "Preview assembled site",
      );
      await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-branch"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

      // Switch to sql-database reveals root path + Build / Publish (no Git remotes)
      await kind.selectOption("sql-database");
      await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-sql-hint"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-sql-hint"]')).toContainText(
        "Build Virtual Site",
      );
      await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-branch"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

      // Switch to http-json reveals root path + Build / Preview / Publish (no Git remotes)
      await kind.selectOption("http-json");
      await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-http-json-hint"]')).toBeVisible();
      await expect(
        page.locator('[data-testid="developer-site-virtual-http-json-hint"]'),
      ).toContainText("Build Virtual Site");
      await expect(
        page.locator('[data-testid="developer-site-virtual-http-json-hint"]'),
      ).toContainText("Preview assembled site");
      await expect(
        page.locator('[data-testid="developer-site-virtual-http-json-hint"]'),
      ).toContainText("Publish Virtual Site");
      await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-branch"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

      // Switch to object-storage reveals root path + Build / Preview / Publish (no Git remotes)
      await kind.selectOption("object-storage");
      await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
      await expect(
        page.locator('[data-testid="developer-site-virtual-object-storage-hint"]'),
      ).toBeVisible();
      await expect(
        page.locator('[data-testid="developer-site-virtual-object-storage-hint"]'),
      ).toContainText("Build Virtual Site");
      await expect(
        page.locator('[data-testid="developer-site-virtual-object-storage-hint"]'),
      ).toContainText("Preview assembled site");
      await expect(
        page.locator('[data-testid="developer-site-virtual-object-storage-hint"]'),
      ).toContainText("Publish Virtual Site");
      await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-branch"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

      // Switch to rss-atom reveals root path + Build / Preview / Publish (no Git remotes)
      await kind.selectOption("rss-atom");
      await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-rss-atom-hint"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-rss-atom-hint"]')).toContainText(
        "Build Virtual Site",
      );
      await expect(page.locator('[data-testid="developer-site-virtual-rss-atom-hint"]')).toContainText(
        "Preview assembled site",
      );
      await expect(page.locator('[data-testid="developer-site-virtual-rss-atom-hint"]')).toContainText(
        "Publish Virtual Site",
      );
      await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-branch"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

      // Switch to icalendar reveals root path + Build / Preview / Publish (no Git remotes)
      await kind.selectOption("icalendar");
      await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-icalendar-hint"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-icalendar-hint"]')).toContainText(
        "Build Virtual Site",
      );
      await expect(page.locator('[data-testid="developer-site-virtual-icalendar-hint"]')).toContainText(
        "Preview assembled site",
      );
      await expect(page.locator('[data-testid="developer-site-virtual-icalendar-hint"]')).toContainText(
        "Publish Virtual Site",
      );
      await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-branch"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

      // Switch to sitemap-xml reveals root path + Build / Preview / Publish (no Git remotes)
      await kind.selectOption("sitemap-xml");
      await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-sitemap-xml-hint"]')).toBeVisible();
      await expect(
        page.locator('[data-testid="developer-site-virtual-sitemap-xml-hint"]'),
      ).toContainText("Build Virtual Site");
      await expect(
        page.locator('[data-testid="developer-site-virtual-sitemap-xml-hint"]'),
      ).toContainText("Preview assembled site");
      await expect(
        page.locator('[data-testid="developer-site-virtual-sitemap-xml-hint"]'),
      ).toContainText("Publish Virtual Site");
      await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-branch"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

      // Switch to robots-txt reveals root path (no Git remotes, no Build/Preview/Publish yet)
      await kind.selectOption("robots-txt");
      await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-robots-txt-hint"]')).toBeVisible();
      await expect(
        page.locator('[data-testid="developer-site-virtual-robots-txt-hint"]'),
      ).toContainText("later slices");
      await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-branch"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);

      // Switch to git-filesystem reveals root path, optional remote, + Build / Publish
      await kind.selectOption("git-filesystem");
      await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-branch"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-remote-hint"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-preview-hint"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-build-hint"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-publish-hint"]')).toBeVisible();

      // Preview: empty/error when no last build, or a same-origin popup when a prior
      // live CSV build left output on this site. Either is fail-closed (no 500 / crash).
      const previewPopupPromise = page.waitForEvent("popup", { timeout: 20_000 }).catch(() => null);
      await page.locator('[data-testid="developer-site-virtual-preview"]').click();
      const previewPopup = await previewPopupPromise;
      if (previewPopup) {
        await previewPopup.close();
      } else {
        const previewChrome = page.locator(
          [
            '[data-testid="developer-site-virtual-preview-error"]',
            '[data-testid="developer-site-virtual-build-error"]',
          ].join(", "),
        );
        await expect(previewChrome.first()).toBeVisible({ timeout: 20_000 });
      }

      // Optional: click Build without save — expect client validation or API error chrome
      // (H2 QA rarely has a saved virtual root; do not require full build success)
      await page.locator('[data-testid="developer-site-virtual-build"]').click();
      const buildChrome = page.locator(
        [
          '[data-testid="developer-site-virtual-build-error"]',
          '[data-testid="developer-site-virtual-build-result"]',
          '[data-testid="developer-site-virtual-build-busy"]',
        ].join(", "),
      );
      await expect(buildChrome.first()).toBeVisible({ timeout: 20_000 });

      // Publish without a saved virtual source / Site root: error or success chrome
      // (H2 QA rarely has a saved virtual source; do not require live publish success)
      await page.locator('[data-testid="developer-site-virtual-publish"]').click();
      const publishChrome = page.locator(
        [
          '[data-testid="developer-site-virtual-publish-error"]',
          '[data-testid="developer-site-virtual-publish-result"]',
          '[data-testid="developer-site-virtual-publish-busy"]',
        ].join(", "),
      );
      await expect(publishChrome.first()).toBeVisible({ timeout: 20_000 });

      // Restore repository to avoid leaving QA site dirty when save is not exercised
      await kind.selectOption("repository");
      await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    }

    const jsErrors = page.__virtPageErrors || [];
    expect(jsErrors, `uncaught page errors: ${jsErrors.join(" | ")}`).toEqual([]);
  });

  test("csv-filesystem live save+reload persists then restore repository (#3687)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — live CSV persist requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    async function openFirstSite() {
      const rows = page.locator(catalogRowsSelector("developer-site-row"));
      await expect(rows.first()).toBeVisible({ timeout: 15_000 });
      await rows.first().locator('[data-testid="developer-site-open"]').click();
      await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
        timeout: 20_000,
      });
    }

    await openFirstSite();
    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("csv-filesystem");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/csv-docs");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(kind).toHaveValue("csv-filesystem");

    await page.locator('[data-testid="developer-site-back"]').click();
    await expect(page.locator(catalogRowsSelector("developer-site-row")).first()).toBeVisible({
      timeout: 15_000,
    });
    await openFirstSite();
    await expect(kind).toHaveValue("csv-filesystem");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/csv-docs",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("sql-database live save+reload persists then restore repository (#3735)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — live SQL persist requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    async function openFirstSite() {
      const rows = page.locator(catalogRowsSelector("developer-site-row"));
      await expect(rows.first()).toBeVisible({ timeout: 15_000 });
      await rows.first().locator('[data-testid="developer-site-open"]').click();
      await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
        timeout: 20_000,
      });
    }

    await openFirstSite();
    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("sql-database");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-sql-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/sql-docs");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(kind).toHaveValue("sql-database");
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();

    await page.locator('[data-testid="developer-site-back"]').click();
    await expect(page.locator(catalogRowsSelector("developer-site-row")).first()).toBeVisible({
      timeout: 15_000,
    });
    await openFirstSite();
    await expect(kind).toHaveValue("sql-database");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/sql-docs",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("http-json live save+reload persists then restore repository (#3796)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — live HTTP JSON persist requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    async function openFirstSite() {
      const rows = page.locator(catalogRowsSelector("developer-site-row"));
      await expect(rows.first()).toBeVisible({ timeout: 15_000 });
      await rows.first().locator('[data-testid="developer-site-open"]').click();
      await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
        timeout: 20_000,
      });
    }

    await openFirstSite();
    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("http-json");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-http-json-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/http-json-docs");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(kind).toHaveValue("http-json");
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await page.locator('[data-testid="developer-site-back"]').click();
    await expect(page.locator(catalogRowsSelector("developer-site-row")).first()).toBeVisible({
      timeout: 15_000,
    });
    await openFirstSite();
    await expect(kind).toHaveValue("http-json");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/http-json-docs",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-http-json-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("object-storage live save+reload persists then restore repository (#3856)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — live object-storage persist requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    async function openFirstSite() {
      const rows = page.locator(catalogRowsSelector("developer-site-row"));
      await expect(rows.first()).toBeVisible({ timeout: 15_000 });
      await rows.first().locator('[data-testid="developer-site-open"]').click();
      await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
        timeout: 20_000,
      });
    }

    await openFirstSite();
    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("object-storage");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-site-virtual-object-storage-hint"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/object-docs");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(kind).toHaveValue("object-storage");
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await page.locator('[data-testid="developer-site-back"]').click();
    await expect(page.locator(catalogRowsSelector("developer-site-row")).first()).toBeVisible({
      timeout: 15_000,
    });
    await openFirstSite();
    await expect(kind).toHaveValue("object-storage");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/object-docs",
    );
    await expect(
      page.locator('[data-testid="developer-site-virtual-object-storage-hint"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("rss-atom save PUTs envelope and GET-roundtrip persists with Build/Preview/Publish chrome (#3927/#3928/#3931)", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    let virtualState = {
      sourceKind: "repository",
      rootPath: null,
      remoteUrl: null,
      branch: null,
      configFile: null,
      siteKey: null,
      virtual: false,
    };
    /** @type {unknown} */
    let lastPutBody = null;

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (/\/virtual\//.test(url)) {
        await route.fallback();
        return;
      }
      const method = route.request().method();
      if (method === "PUT") {
        lastPutBody = route.request().postDataJSON();
        const envelope =
          lastPutBody &&
          typeof lastPutBody === "object" &&
          lastPutBody.VirtualSiteProperties
            ? lastPutBody.VirtualSiteProperties
            : lastPutBody;
        virtualState = {
          sourceKind: envelope.sourceKind || "repository",
          rootPath: envelope.rootPath || null,
          remoteUrl:
            envelope.remoteUrl === undefined || envelope.remoteUrl === null
              ? virtualState.remoteUrl
              : envelope.remoteUrl || null,
          branch:
            envelope.branch === undefined || envelope.branch === null
              ? virtualState.branch
              : envelope.branch || null,
          configFile: envelope.configFile || null,
          siteKey: envelope.siteKey || null,
          virtual:
            envelope.sourceKind === "git-filesystem" ||
            envelope.sourceKind === "csv-filesystem" ||
            envelope.sourceKind === "sql-database" ||
            envelope.sourceKind === "http-json" ||
            envelope.sourceKind === "object-storage" ||
            envelope.sourceKind === "rss-atom",
        };
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ VirtualSiteProperties: virtualState }),
        });
        return;
      }
      if (method !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ VirtualSiteProperties: virtualState }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — rss-atom Virtual Site save test requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("rss-atom");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-rss-atom-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/rss-atom-docs");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody).toBeTruthy();
    expect(lastPutBody).toHaveProperty("VirtualSiteProperties");
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("rss-atom");
    expect(lastPutBody.VirtualSiteProperties.rootPath).toBe("C:/rss-atom-docs");
    expect(lastPutBody.VirtualSiteProperties.remoteUrl ?? "").toBe("");
    expect(lastPutBody).not.toHaveProperty("sourceKind");
    expect(lastPutBody.VirtualSiteProperties).not.toHaveProperty("password");
    expect(JSON.stringify(lastPutBody)).not.toMatch(
      /password|authorization|api[_-]?key|feed[_-]?url|credential|token/i,
    );

    await expect(kind).toHaveValue("rss-atom");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/rss-atom-docs",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    lastPutBody = null;
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("repository");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("rss-atom live save+reload persists then restore repository (#3889)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — live rss-atom persist requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    async function openFirstSite() {
      const rows = page.locator(catalogRowsSelector("developer-site-row"));
      await expect(rows.first()).toBeVisible({ timeout: 15_000 });
      await rows.first().locator('[data-testid="developer-site-open"]').click();
      await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
        timeout: 20_000,
      });
    }

    await openFirstSite();
    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("rss-atom");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-rss-atom-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/rss-atom-docs");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(kind).toHaveValue("rss-atom");
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await page.locator('[data-testid="developer-site-back"]').click();
    await expect(page.locator(catalogRowsSelector("developer-site-row")).first()).toBeVisible({
      timeout: 15_000,
    });
    await openFirstSite();
    await expect(kind).toHaveValue("rss-atom");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/rss-atom-docs",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-rss-atom-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("icalendar save PUTs envelope and GET-roundtrip persists with Build/Preview/Publish chrome (#3989)", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    let virtualState = {
      sourceKind: "repository",
      rootPath: null,
      remoteUrl: null,
      branch: null,
      configFile: null,
      siteKey: null,
      virtual: false,
    };
    /** @type {unknown} */
    let lastPutBody = null;

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (/\/virtual\//.test(url)) {
        await route.fallback();
        return;
      }
      const method = route.request().method();
      if (method === "PUT") {
        lastPutBody = route.request().postDataJSON();
        const envelope =
          lastPutBody &&
          typeof lastPutBody === "object" &&
          lastPutBody.VirtualSiteProperties
            ? lastPutBody.VirtualSiteProperties
            : lastPutBody;
        virtualState = {
          sourceKind: envelope.sourceKind || "repository",
          rootPath: envelope.rootPath || null,
          remoteUrl:
            envelope.remoteUrl === undefined || envelope.remoteUrl === null
              ? virtualState.remoteUrl
              : envelope.remoteUrl || null,
          branch:
            envelope.branch === undefined || envelope.branch === null
              ? virtualState.branch
              : envelope.branch || null,
          configFile: envelope.configFile || null,
          siteKey: envelope.siteKey || null,
          virtual:
            envelope.sourceKind === "git-filesystem" ||
            envelope.sourceKind === "csv-filesystem" ||
            envelope.sourceKind === "sql-database" ||
            envelope.sourceKind === "http-json" ||
            envelope.sourceKind === "object-storage" ||
            envelope.sourceKind === "rss-atom" ||
            envelope.sourceKind === "icalendar",
        };
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ VirtualSiteProperties: virtualState }),
        });
        return;
      }
      if (method !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ VirtualSiteProperties: virtualState }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — icalendar Virtual Site save test requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("icalendar");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-icalendar-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/icalendar-docs");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody).toBeTruthy();
    expect(lastPutBody).toHaveProperty("VirtualSiteProperties");
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("icalendar");
    expect(lastPutBody.VirtualSiteProperties.rootPath).toBe("C:/icalendar-docs");
    expect(lastPutBody.VirtualSiteProperties.remoteUrl ?? "").toBe("");
    expect(lastPutBody).not.toHaveProperty("sourceKind");
    expect(lastPutBody.VirtualSiteProperties).not.toHaveProperty("password");
    expect(JSON.stringify(lastPutBody)).not.toMatch(
      /password|authorization|api[_-]?key|caldav|credential|token/i,
    );

    await expect(kind).toHaveValue("icalendar");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/icalendar-docs",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    lastPutBody = null;
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("repository");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("icalendar live save+reload persists then restore repository (#3983)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — live icalendar persist requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    async function openFirstSite() {
      const rows = page.locator(catalogRowsSelector("developer-site-row"));
      await expect(rows.first()).toBeVisible({ timeout: 15_000 });
      await rows.first().locator('[data-testid="developer-site-open"]').click();
      await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
        timeout: 20_000,
      });
    }

    await openFirstSite();
    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("icalendar");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-icalendar-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/icalendar-docs");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(kind).toHaveValue("icalendar");
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await page.locator('[data-testid="developer-site-back"]').click();
    await expect(page.locator(catalogRowsSelector("developer-site-row")).first()).toBeVisible({
      timeout: 15_000,
    });
    await openFirstSite();
    await expect(kind).toHaveValue("icalendar");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/icalendar-docs",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-icalendar-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("sitemap-xml save PUTs envelope and GET-roundtrip persists with Build/Preview chrome (#4115 / #4164 / #4165)", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    let virtualState = {
      sourceKind: "repository",
      rootPath: null,
      remoteUrl: null,
      branch: null,
      configFile: null,
      siteKey: null,
      virtual: false,
    };
    /** @type {unknown} */
    let lastPutBody = null;

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (/\/virtual\//.test(url)) {
        await route.fallback();
        return;
      }
      const method = route.request().method();
      if (method === "PUT") {
        lastPutBody = route.request().postDataJSON();
        const envelope =
          lastPutBody &&
          typeof lastPutBody === "object" &&
          lastPutBody.VirtualSiteProperties
            ? lastPutBody.VirtualSiteProperties
            : lastPutBody;
        virtualState = {
          sourceKind: envelope.sourceKind || "repository",
          rootPath: envelope.rootPath || null,
          remoteUrl:
            envelope.remoteUrl === undefined || envelope.remoteUrl === null
              ? virtualState.remoteUrl
              : envelope.remoteUrl || null,
          branch:
            envelope.branch === undefined || envelope.branch === null
              ? virtualState.branch
              : envelope.branch || null,
          configFile: envelope.configFile || null,
          siteKey: envelope.siteKey || null,
          virtual:
            envelope.sourceKind === "git-filesystem" ||
            envelope.sourceKind === "csv-filesystem" ||
            envelope.sourceKind === "sql-database" ||
            envelope.sourceKind === "http-json" ||
            envelope.sourceKind === "object-storage" ||
            envelope.sourceKind === "rss-atom" ||
            envelope.sourceKind === "icalendar" ||
            envelope.sourceKind === "sitemap-xml",
        };
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ VirtualSiteProperties: virtualState }),
        });
        return;
      }
      if (method !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ VirtualSiteProperties: virtualState }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — sitemap-xml Virtual Site save test requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("sitemap-xml");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-sitemap-xml-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/sitemap-xml-docs");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody).toBeTruthy();
    expect(lastPutBody).toHaveProperty("VirtualSiteProperties");
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("sitemap-xml");
    expect(lastPutBody.VirtualSiteProperties.rootPath).toBe("C:/sitemap-xml-docs");
    expect(lastPutBody.VirtualSiteProperties.remoteUrl ?? "").toBe("");
    expect(lastPutBody).not.toHaveProperty("sourceKind");
    expect(lastPutBody.VirtualSiteProperties).not.toHaveProperty("password");
    expect(JSON.stringify(lastPutBody)).not.toMatch(
      /password|authorization|api[_-]?key|crawl|credential|token/i,
    );

    await expect(kind).toHaveValue("sitemap-xml");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/sitemap-xml-docs",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    lastPutBody = null;
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("repository");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("sitemap-xml live save+reload persists then restore repository (#4115)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — live sitemap-xml persist requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    async function openFirstSite() {
      const rows = page.locator(catalogRowsSelector("developer-site-row"));
      await expect(rows.first()).toBeVisible({ timeout: 15_000 });
      await rows.first().locator('[data-testid="developer-site-open"]').click();
      await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
        timeout: 20_000,
      });
    }

    await openFirstSite();
    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("sitemap-xml");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-sitemap-xml-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/sitemap-xml-docs");
    await saveVirtualSiteAndExpectSaved(page, { timeout: 15_000 });
    await expect(kind).toHaveValue("sitemap-xml");
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await page.locator('[data-testid="developer-site-back"]').click();
    await expect(page.locator(catalogRowsSelector("developer-site-row")).first()).toBeVisible({
      timeout: 15_000,
    });
    await openFirstSite();
    await expect(kind).toHaveValue("sitemap-xml");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/sitemap-xml-docs",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-sitemap-xml-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    await saveVirtualSiteAndExpectSaved(page, { timeout: 15_000 });
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("robots-txt save PUTs envelope and GET-roundtrip persists without Build chrome (#4360)", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    let virtualState = {
      sourceKind: "repository",
      rootPath: null,
      remoteUrl: null,
      branch: null,
      configFile: null,
      siteKey: null,
      virtual: false,
    };
    /** @type {unknown} */
    let lastPutBody = null;

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (/\/virtual\//.test(url)) {
        await route.fallback();
        return;
      }
      const method = route.request().method();
      if (method === "PUT") {
        lastPutBody = route.request().postDataJSON();
        const envelope =
          lastPutBody &&
          typeof lastPutBody === "object" &&
          lastPutBody.VirtualSiteProperties
            ? lastPutBody.VirtualSiteProperties
            : lastPutBody;
        virtualState = {
          sourceKind: envelope.sourceKind || "repository",
          rootPath: envelope.rootPath || null,
          remoteUrl:
            envelope.remoteUrl === undefined || envelope.remoteUrl === null
              ? virtualState.remoteUrl
              : envelope.remoteUrl || null,
          branch:
            envelope.branch === undefined || envelope.branch === null
              ? virtualState.branch
              : envelope.branch || null,
          configFile: envelope.configFile || null,
          siteKey: envelope.siteKey || null,
          virtual:
            envelope.sourceKind === "git-filesystem" ||
            envelope.sourceKind === "csv-filesystem" ||
            envelope.sourceKind === "sql-database" ||
            envelope.sourceKind === "http-json" ||
            envelope.sourceKind === "object-storage" ||
            envelope.sourceKind === "rss-atom" ||
            envelope.sourceKind === "icalendar" ||
            envelope.sourceKind === "sitemap-xml" ||
            envelope.sourceKind === "robots-txt",
        };
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ VirtualSiteProperties: virtualState }),
        });
        return;
      }
      if (method !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ VirtualSiteProperties: virtualState }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — robots-txt Virtual Site save test requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("robots-txt");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-robots-txt-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);

    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/robots-txt-docs");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody).toBeTruthy();
    expect(lastPutBody).toHaveProperty("VirtualSiteProperties");
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("robots-txt");
    expect(lastPutBody.VirtualSiteProperties.rootPath).toBe("C:/robots-txt-docs");
    expect(lastPutBody.VirtualSiteProperties.remoteUrl ?? "").toBe("");
    expect(lastPutBody).not.toHaveProperty("sourceKind");
    expect(lastPutBody.VirtualSiteProperties).not.toHaveProperty("password");
    expect(JSON.stringify(lastPutBody)).not.toMatch(
      /password|authorization|api[_-]?key|crawl|credential|token/i,
    );

    await expect(kind).toHaveValue("robots-txt");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/robots-txt-docs",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);

    await kind.selectOption("repository");
    lastPutBody = null;
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("repository");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("robots-txt live save+reload persists then restore repository (#4360)", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — live robots-txt persist requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    async function openFirstSite() {
      const rows = page.locator(catalogRowsSelector("developer-site-row"));
      await expect(rows.first()).toBeVisible({ timeout: 15_000 });
      await rows.first().locator('[data-testid="developer-site-open"]').click();
      await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
        timeout: 20_000,
      });
    }

    await openFirstSite();
    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("robots-txt");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-robots-txt-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toHaveCount(0);
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/robots-txt-docs");
    await saveVirtualSiteAndExpectSaved(page, { timeout: 15_000 });
    await expect(kind).toHaveValue("robots-txt");
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);

    await page.locator('[data-testid="developer-site-back"]').click();
    await expect(page.locator(catalogRowsSelector("developer-site-row")).first()).toBeVisible({
      timeout: 15_000,
    });
    await openFirstSite();
    await expect(kind).toHaveValue("robots-txt");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/robots-txt-docs",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-robots-txt-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);

    await kind.selectOption("repository");
    await saveVirtualSiteAndExpectSaved(page, { timeout: 15_000 });
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("build result lists linkProblems on HTTP 200", async ({ page }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      // Mocked catalog site has no GUID; ACL GET 404 is expected host noise, not a JS exception.
      if (/Failed to load resource:.*404/.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual/build", async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          siteName: "Help",
          siteKey: "product-docs",
          outputPath: "C:/tmp/virtual-sites/product-docs",
          pagesWritten: 3,
          linkProblemCount: 2,
          hasLinkProblems: true,
          linkProblems: [
            "broken id:missing-page from 8.2/index.md",
            "unresolved relative ./gone.md",
          ],
        }),
      });
    });
    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (url.includes("/virtual/build")) {
        await route.fallback();
        return;
      }
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          sourceKind: "git-filesystem",
          rootPath: "C:/docs",
          configFile: "_config.yaml",
          siteKey: "product-docs",
          virtual: true,
        }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const empty = page.locator('[data-testid="developer-site-empty"]');
    if (await empty.isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — Virtual Site link-problem list requires a site row",
      });
      return;
    }
    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible({
      timeout: 20_000,
    });
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-site-virtual-build-link-problems"]'),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-site-virtual-build-link-line"]'),
    ).toHaveCount(2);
    await page.locator('[data-testid="developer-site-virtual-build-link-toggle"]').click();
    await expect(
      page.locator('[data-testid="developer-site-virtual-build-link-line"]').nth(0),
    ).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-site-virtual-build-link-line"]').nth(0),
    ).toHaveText("broken id:missing-page from 8.2/index.md");
    expect(consoleErrors).toEqual([]);
  });

  test("publish result shows files copied and dest path on HTTP 200", async ({ page }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (/Failed to load resource:.*404/.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual/publish", async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          siteName: "Help",
          siteKey: "product-docs",
          publishPath: "C:/inetpub/wwwroot/help",
          buildOutputPath: "C:/tmp/virtual-sites/product-docs",
          pagesWritten: 5,
          filesCopied: 11,
          linkProblemCount: 0,
          hasLinkProblems: false,
        }),
      });
    });
    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (url.includes("/virtual/publish") || url.includes("/virtual/build")) {
        await route.fallback();
        return;
      }
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          sourceKind: "git-filesystem",
          rootPath: "C:/docs",
          configFile: "_config.yaml",
          siteKey: "product-docs",
          virtual: true,
        }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const empty = page.locator('[data-testid="developer-site-empty"]');
    if (await empty.isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — Virtual Site publish result requires a site row",
      });
      return;
    }
    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible({
      timeout: 20_000,
    });
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-files"]')).toHaveText(
      "11",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-publish-dest"]')).toContainText(
      "inetpub",
    );
    expect(consoleErrors).toEqual([]);
  });

  test("save PUTs VirtualSiteProperties envelope and GET-roundtrip shows Build chrome (#3365)", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    let virtualState = {
      sourceKind: "repository",
      rootPath: null,
      remoteUrl: null,
      branch: null,
      configFile: null,
      siteKey: null,
      virtual: false,
    };
    /** @type {unknown} */
    let lastPutBody = null;

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (/\/virtual\//.test(url)) {
        await route.fallback();
        return;
      }
      const method = route.request().method();
      if (method === "PUT") {
        lastPutBody = route.request().postDataJSON();
        const envelope =
          lastPutBody &&
          typeof lastPutBody === "object" &&
          lastPutBody.VirtualSiteProperties
            ? lastPutBody.VirtualSiteProperties
            : lastPutBody;
        virtualState = {
          sourceKind: envelope.sourceKind || "repository",
          rootPath: envelope.rootPath || null,
          remoteUrl:
            envelope.remoteUrl === undefined || envelope.remoteUrl === null
              ? virtualState.remoteUrl
              : envelope.remoteUrl || null,
          branch:
            envelope.branch === undefined || envelope.branch === null
              ? virtualState.branch
              : envelope.branch || null,
          configFile: envelope.configFile || null,
          siteKey: envelope.siteKey || null,
          virtual:
            envelope.sourceKind === "git-filesystem" ||
            envelope.sourceKind === "csv-filesystem",
        };
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ VirtualSiteProperties: virtualState }),
        });
        return;
      }
      if (method !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ VirtualSiteProperties: virtualState }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — Virtual Site save envelope test requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toHaveCount(
      0,
    );

    await page.locator('[data-testid="developer-site-virtual-source-kind"]').selectOption(
      "git-filesystem",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/docs/product-docs");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody).toBeTruthy();
    expect(lastPutBody).toHaveProperty("VirtualSiteProperties");
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("git-filesystem");
    expect(lastPutBody.VirtualSiteProperties.rootPath).toBe("C:/docs/product-docs");
    expect(lastPutBody.VirtualSiteProperties.remoteUrl ?? "").toBe("");
    expect(lastPutBody).not.toHaveProperty("sourceKind");

    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();

    await page
      .locator('[data-testid="developer-site-virtual-remote-url"]')
      .fill("https://git.example.com/org/docs.git");
    await page.locator('[data-testid="developer-site-virtual-branch"]').fill("main");
    lastPutBody = null;
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect
      .poll(() => lastPutBody && lastPutBody.VirtualSiteProperties && lastPutBody.VirtualSiteProperties.remoteUrl)
      .toBe("https://git.example.com/org/docs.git");
    expect(lastPutBody.VirtualSiteProperties.branch).toBe("main");
    expect(lastPutBody.VirtualSiteProperties.rootPath).toBe("C:/docs/product-docs");
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveValue(
      "https://git.example.com/org/docs.git",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-branch"]')).toHaveValue("main");

    lastPutBody = null;
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect
      .poll(() => lastPutBody && lastPutBody.VirtualSiteProperties && lastPutBody.VirtualSiteProperties.remoteUrl)
      .toBe("https://git.example.com/org/docs.git");
    expect(lastPutBody.VirtualSiteProperties.branch).toBe("main");
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("csv-filesystem save PUTs envelope and GET-roundtrip persists with Build chrome (#3697)", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    let virtualState = {
      sourceKind: "repository",
      rootPath: null,
      remoteUrl: null,
      branch: null,
      configFile: null,
      siteKey: null,
      virtual: false,
    };
    /** @type {unknown} */
    let lastPutBody = null;

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (/\/virtual\//.test(url)) {
        await route.fallback();
        return;
      }
      const method = route.request().method();
      if (method === "PUT") {
        lastPutBody = route.request().postDataJSON();
        const envelope =
          lastPutBody &&
          typeof lastPutBody === "object" &&
          lastPutBody.VirtualSiteProperties
            ? lastPutBody.VirtualSiteProperties
            : lastPutBody;
        virtualState = {
          sourceKind: envelope.sourceKind || "repository",
          rootPath: envelope.rootPath || null,
          remoteUrl:
            envelope.remoteUrl === undefined || envelope.remoteUrl === null
              ? virtualState.remoteUrl
              : envelope.remoteUrl || null,
          branch:
            envelope.branch === undefined || envelope.branch === null
              ? virtualState.branch
              : envelope.branch || null,
          configFile: envelope.configFile || null,
          siteKey: envelope.siteKey || null,
          virtual:
            envelope.sourceKind === "git-filesystem" ||
            envelope.sourceKind === "csv-filesystem",
        };
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ VirtualSiteProperties: virtualState }),
        });
        return;
      }
      if (method !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ VirtualSiteProperties: virtualState }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — CSV Virtual Site save test requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("csv-filesystem");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-csv-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/csv-docs");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody).toBeTruthy();
    expect(lastPutBody).toHaveProperty("VirtualSiteProperties");
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("csv-filesystem");
    expect(lastPutBody.VirtualSiteProperties.rootPath).toBe("C:/csv-docs");
    expect(lastPutBody.VirtualSiteProperties.remoteUrl ?? "").toBe("");
    expect(lastPutBody).not.toHaveProperty("sourceKind");

    await expect(kind).toHaveValue("csv-filesystem");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/csv-docs",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    lastPutBody = null;
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("repository");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("sql-database save PUTs envelope and GET-roundtrip persists with Build chrome (#3735)", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    let virtualState = {
      sourceKind: "repository",
      rootPath: null,
      remoteUrl: null,
      branch: null,
      configFile: null,
      siteKey: null,
      virtual: false,
    };
    /** @type {unknown} */
    let lastPutBody = null;

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (/\/virtual\//.test(url)) {
        await route.fallback();
        return;
      }
      const method = route.request().method();
      if (method === "PUT") {
        lastPutBody = route.request().postDataJSON();
        const envelope =
          lastPutBody &&
          typeof lastPutBody === "object" &&
          lastPutBody.VirtualSiteProperties
            ? lastPutBody.VirtualSiteProperties
            : lastPutBody;
        virtualState = {
          sourceKind: envelope.sourceKind || "repository",
          rootPath: envelope.rootPath || null,
          remoteUrl:
            envelope.remoteUrl === undefined || envelope.remoteUrl === null
              ? virtualState.remoteUrl
              : envelope.remoteUrl || null,
          branch:
            envelope.branch === undefined || envelope.branch === null
              ? virtualState.branch
              : envelope.branch || null,
          configFile: envelope.configFile || null,
          siteKey: envelope.siteKey || null,
          virtual:
            envelope.sourceKind === "git-filesystem" ||
            envelope.sourceKind === "csv-filesystem" ||
            envelope.sourceKind === "sql-database",
        };
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ VirtualSiteProperties: virtualState }),
        });
        return;
      }
      if (method !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ VirtualSiteProperties: virtualState }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — SQL Virtual Site save test requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("sql-database");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-sql-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/sql-docs");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody).toBeTruthy();
    expect(lastPutBody).toHaveProperty("VirtualSiteProperties");
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("sql-database");
    expect(lastPutBody.VirtualSiteProperties.rootPath).toBe("C:/sql-docs");
    expect(lastPutBody.VirtualSiteProperties.remoteUrl ?? "").toBe("");
    expect(lastPutBody).not.toHaveProperty("sourceKind");
    expect(lastPutBody.VirtualSiteProperties).not.toHaveProperty("password");
    expect(JSON.stringify(lastPutBody)).not.toMatch(/password/i);

    await expect(kind).toHaveValue("sql-database");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/sql-docs",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    lastPutBody = null;
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("repository");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("http-json save PUTs envelope and GET-roundtrip persists with Build chrome (#3808)", async ({
    page,
  }) => {
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    let virtualState = {
      sourceKind: "repository",
      rootPath: null,
      remoteUrl: null,
      branch: null,
      configFile: null,
      siteKey: null,
      virtual: false,
    };
    /** @type {unknown} */
    let lastPutBody = null;

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (/\/virtual\//.test(url)) {
        await route.fallback();
        return;
      }
      const method = route.request().method();
      if (method === "PUT") {
        lastPutBody = route.request().postDataJSON();
        const envelope =
          lastPutBody &&
          typeof lastPutBody === "object" &&
          lastPutBody.VirtualSiteProperties
            ? lastPutBody.VirtualSiteProperties
            : lastPutBody;
        virtualState = {
          sourceKind: envelope.sourceKind || "repository",
          rootPath: envelope.rootPath || null,
          remoteUrl:
            envelope.remoteUrl === undefined || envelope.remoteUrl === null
              ? virtualState.remoteUrl
              : envelope.remoteUrl || null,
          branch:
            envelope.branch === undefined || envelope.branch === null
              ? virtualState.branch
              : envelope.branch || null,
          configFile: envelope.configFile || null,
          siteKey: envelope.siteKey || null,
          virtual:
            envelope.sourceKind === "git-filesystem" ||
            envelope.sourceKind === "csv-filesystem" ||
            envelope.sourceKind === "sql-database" ||
            envelope.sourceKind === "http-json",
        };
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({ VirtualSiteProperties: virtualState }),
        });
        return;
      }
      if (method !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ VirtualSiteProperties: virtualState }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      test.info().annotations.push({
        type: "note",
        description: "No sites in catalog — HTTP JSON Virtual Site save test requires a site row",
      });
      return;
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("http-json");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-http-json-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill("C:/http-json-docs");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody).toBeTruthy();
    expect(lastPutBody).toHaveProperty("VirtualSiteProperties");
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("http-json");
    expect(lastPutBody.VirtualSiteProperties.rootPath).toBe("C:/http-json-docs");
    expect(lastPutBody.VirtualSiteProperties.remoteUrl ?? "").toBe("");
    expect(lastPutBody).not.toHaveProperty("sourceKind");
    expect(lastPutBody.VirtualSiteProperties).not.toHaveProperty("password");
    expect(JSON.stringify(lastPutBody)).not.toMatch(/password|authorization|api[_-]?key/i);

    await expect(kind).toHaveValue("http-json");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveValue(
      "C:/http-json-docs",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    lastPutBody = null;
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("repository");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("http-json build result shows pages written on HTTP 200 (#3808)", async ({ page }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (/Failed to load resource:.*404/.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual/build", async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          siteName: "Help",
          siteKey: "http-json-docs",
          outputPath: "C:/tmp/virtual-sites/http-json-docs",
          pagesWritten: 1,
          linkProblemCount: 0,
          hasLinkProblems: false,
          linkProblems: [],
        }),
      });
    });
    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (url.includes("/virtual/build")) {
        await route.fallback();
        return;
      }
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          sourceKind: "http-json",
          rootPath: "C:/http-json-docs",
          virtual: true,
        }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const empty = page.locator('[data-testid="developer-site-empty"]');
    if (await empty.isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — HTTP JSON Virtual Site build intercept requires a site row");
    }
    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build-pages"]')).toHaveText("1");
    expect(consoleErrors).toEqual([]);
  });

  test("csv-filesystem build result shows pages written on HTTP 200 (#3697)", async ({ page }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (/Failed to load resource:.*404/.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual/build", async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          siteName: "Help",
          siteKey: "csv-docs",
          outputPath: "C:/tmp/virtual-sites/csv-docs",
          pagesWritten: 2,
          linkProblemCount: 0,
          hasLinkProblems: false,
          linkProblems: [],
        }),
      });
    });
    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (url.includes("/virtual/build")) {
        await route.fallback();
        return;
      }
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          sourceKind: "csv-filesystem",
          rootPath: "C:/csv-docs",
          virtual: true,
        }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const empty = page.locator('[data-testid="developer-site-empty"]');
    if (await empty.isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — CSV Virtual Site build intercept requires a site row");
    }
    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build-pages"]')).toHaveText("2");
    expect(consoleErrors).toEqual([]);
  });

  test("csv-filesystem live Build Virtual Site completes on H2 QA (#3697)", async ({ page }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const csvRoot = deployCsvVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live CSV Build requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("csv-filesystem");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(csvRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();
    const pagesText = (
      await page.locator('[data-testid="developer-site-virtual-build-pages"]').textContent()
    ).trim();
    expect(Number.parseInt(pagesText, 10), `pages written: ${pagesText}`).toBeGreaterThan(0);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("sql-database build result shows pages written on HTTP 200 (#3759)", async ({ page }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (/Failed to load resource:.*404/.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual/build", async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          siteName: "Help",
          siteKey: "sql-docs",
          outputPath: "C:/tmp/virtual-sites/sql-docs",
          pagesWritten: 1,
          linkProblemCount: 0,
          hasLinkProblems: false,
          linkProblems: [],
        }),
      });
    });
    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (url.includes("/virtual/build")) {
        await route.fallback();
        return;
      }
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          sourceKind: "sql-database",
          rootPath: "C:/sql-docs",
          virtual: true,
        }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const empty = page.locator('[data-testid="developer-site-empty"]');
    if (await empty.isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — SQL Virtual Site build intercept requires a site row");
    }
    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-build-pages"]')).toHaveText("1");
    expect(consoleErrors).toEqual([]);
  });

  test("sql-database live Build Virtual Site completes on H2 QA (#3759)", async ({ page }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const sqlRoot = deploySqlVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live SQL Build requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("sql-database");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-sql-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(sqlRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();
    const pagesText = (
      await page.locator('[data-testid="developer-site-virtual-build-pages"]').textContent()
    ).trim();
    expect(Number.parseInt(pagesText, 10), `pages written: ${pagesText}`).toBeGreaterThan(0);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("http-json live Build Virtual Site completes on H2 QA (#3808)", async ({ page }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const httpJsonRoot = deployHttpJsonVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live HTTP JSON Build requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("http-json");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-http-json-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(httpJsonRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();
    const pagesText = (
      await page.locator('[data-testid="developer-site-virtual-build-pages"]').textContent()
    ).trim();
    expect(Number.parseInt(pagesText, 10), `pages written: ${pagesText}`).toBeGreaterThan(0);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("object-storage live Build Virtual Site completes on H2 QA (#3869)", async ({ page }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const objectRoot = deployObjectStorageVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live object-storage Build requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("object-storage");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-site-virtual-object-storage-hint"]'),
    ).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(objectRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();
    const pagesText = (
      await page.locator('[data-testid="developer-site-virtual-build-pages"]').textContent()
    ).trim();
    expect(Number.parseInt(pagesText, 10), `pages written: ${pagesText}`).toBeGreaterThan(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("rss-atom live Build Virtual Site completes on H2 QA (#3927)", async ({ page }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const rssRoot = deployRssAtomVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live rss-atom Build requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("rss-atom");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-rss-atom-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(rssRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();
    const pagesText = (
      await page.locator('[data-testid="developer-site-virtual-build-pages"]').textContent()
    ).trim();
    expect(Number.parseInt(pagesText, 10), `pages written: ${pagesText}`).toBeGreaterThan(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });


  test("rss-atom publish result shows files copied on HTTP 200 (#3931)", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (/Failed to load resource:.*404/.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual/publish", async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          siteName: "Help",
          publishPath: "C:/inetpub/wwwroot/rss-atom-help",
          filesCopied: 3,
          pagesWritten: 1,
          hasLinkProblems: false,
        }),
      });
    });
    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (url.includes("/virtual/publish") || url.includes("/virtual/build")) {
        await route.fallback();
        return;
      }
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          sourceKind: "rss-atom",
          rootPath: "C:/rss-atom-docs",
          virtual: true,
        }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const empty = page.locator('[data-testid="developer-site-empty"]');
    if (await empty.isVisible().catch(() => false)) {
      throw new Error(
        "No sites in catalog - rss-atom Virtual Site publish intercept requires a site row",
      );
    }
    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-rss-atom-hint"]')).toContainText(
      "Publish Virtual Site",
    );
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-files"]')).toHaveText(
      "3",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-publish-dest"]')).toContainText(
      "rss-atom-help",
    );
    expect(consoleErrors).toEqual([]);
  });

  test("object-storage publish result shows files copied on HTTP 200 (#3879)", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (/Failed to load resource:.*404/.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual/publish", async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          siteName: "Help",
          publishPath: "C:/inetpub/wwwroot/object-help",
          filesCopied: 4,
          pagesWritten: 1,
          hasLinkProblems: false,
        }),
      });
    });
    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (url.includes("/virtual/publish") || url.includes("/virtual/build")) {
        await route.fallback();
        return;
      }
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          sourceKind: "object-storage",
          rootPath: "C:/object-docs",
          virtual: true,
        }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const empty = page.locator('[data-testid="developer-site-empty"]');
    if (await empty.isVisible().catch(() => false)) {
      throw new Error(
        "No sites in catalog — object-storage Virtual Site publish intercept requires a site row",
      );
    }
    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-files"]')).toHaveText(
      "4",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-publish-dest"]')).toContainText(
      "object-help",
    );
    expect(consoleErrors).toEqual([]);
  });

  test("object-storage live Publish Virtual Site completes on H2 QA (#3879)", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const objectStorageRoot = deployObjectStorageVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live object-storage Publish requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("object-storage");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(
      page.locator('[data-testid="developer-site-virtual-object-storage-hint"]'),
    ).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(objectStorageRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();

    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const publishRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/publish(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    const publishResp = await publishRespPromise;
    const publishBody = await publishResp.text();
    expect(
      publishResp.ok(),
      `POST /virtual/publish HTTP ${publishResp.status()}: ${publishBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    const filesText = (
      await page.locator('[data-testid="developer-site-virtual-publish-files"]').textContent()
    ).trim();
    expect(Number.parseInt(filesText, 10), `files copied: ${filesText}`).toBeGreaterThan(0);
    const destText = (
      await page.locator('[data-testid="developer-site-virtual-publish-dest"]').textContent()
    ).trim();
    assertPublishedObjectStorageFilesOnQaCell(destText);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("http-json publish result shows files copied on HTTP 200 (#3820)", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (/Failed to load resource:.*404/.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual/publish", async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          siteName: "Help",
          publishPath: "C:/inetpub/wwwroot/http-json-help",
          filesCopied: 3,
          pagesWritten: 1,
          hasLinkProblems: false,
        }),
      });
    });
    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (url.includes("/virtual/publish") || url.includes("/virtual/build")) {
        await route.fallback();
        return;
      }
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          sourceKind: "http-json",
          rootPath: "C:/http-json-docs",
          virtual: true,
        }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const empty = page.locator('[data-testid="developer-site-empty"]');
    if (await empty.isVisible().catch(() => false)) {
      throw new Error(
        "No sites in catalog — HTTP JSON Virtual Site publish intercept requires a site row",
      );
    }
    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-files"]')).toHaveText(
      "3",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-publish-dest"]')).toContainText(
      "http-json-help",
    );
    expect(consoleErrors).toEqual([]);
  });

  test("http-json live Publish Virtual Site completes on H2 QA (#3820)", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const httpJsonRoot = deployHttpJsonVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live HTTP JSON Publish requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("http-json");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-http-json-hint"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(httpJsonRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();

    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const publishRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/publish(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    const publishResp = await publishRespPromise;
    const publishBody = await publishResp.text();
    expect(
      publishResp.ok(),
      `POST /virtual/publish HTTP ${publishResp.status()}: ${publishBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    const filesText = (
      await page.locator('[data-testid="developer-site-virtual-publish-files"]').textContent()
    ).trim();
    expect(Number.parseInt(filesText, 10), `files copied: ${filesText}`).toBeGreaterThan(0);
    const destText = (
      await page.locator('[data-testid="developer-site-virtual-publish-dest"]').textContent()
    ).trim();
    assertPublishedHttpJsonFilesOnQaCell(destText);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("sql-database publish result shows files copied on HTTP 200 (#3759)", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (/Failed to load resource:.*404/.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual/publish", async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          siteName: "Help",
          siteKey: "sql-docs",
          publishPath: "C:/inetpub/wwwroot/sql-help",
          buildOutputPath: "C:/tmp/virtual-sites/sql-docs",
          pagesWritten: 1,
          filesCopied: 3,
          linkProblemCount: 0,
          hasLinkProblems: false,
        }),
      });
    });
    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (url.includes("/virtual/publish") || url.includes("/virtual/build")) {
        await route.fallback();
        return;
      }
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          sourceKind: "sql-database",
          rootPath: "C:/sql-docs",
          virtual: true,
        }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const empty = page.locator('[data-testid="developer-site-empty"]');
    if (await empty.isVisible().catch(() => false)) {
      throw new Error(
        "No sites in catalog — SQL Virtual Site publish intercept requires a site row",
      );
    }
    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible({
      timeout: 20_000,
    });
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-files"]')).toHaveText(
      "3",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-publish-dest"]')).toContainText(
      "sql-help",
    );
    expect(consoleErrors).toEqual([]);
  });

  test("sql-database live Publish Virtual Site completes on H2 QA (#3759)", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const sqlRoot = deploySqlVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live SQL Publish requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("sql-database");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-sql-hint"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(sqlRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();

    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const publishRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/publish(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    const publishResp = await publishRespPromise;
    const publishBody = await publishResp.text();
    expect(
      publishResp.ok(),
      `POST /virtual/publish HTTP ${publishResp.status()}: ${publishBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    const filesText = (
      await page.locator('[data-testid="developer-site-virtual-publish-files"]').textContent()
    ).trim();
    expect(Number.parseInt(filesText, 10), `files copied: ${filesText}`).toBeGreaterThan(0);
    const destText = (
      await page.locator('[data-testid="developer-site-virtual-publish-dest"]').textContent()
    ).trim();
    assertPublishedSqlFilesOnQaCell(destText);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("csv-filesystem publish result shows files copied on HTTP 200 (#3699)", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (/Failed to load resource:.*404/.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual/publish", async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          siteName: "Help",
          siteKey: "csv-docs",
          publishPath: "C:/inetpub/wwwroot/csv-help",
          buildOutputPath: "C:/tmp/virtual-sites/csv-docs",
          pagesWritten: 2,
          filesCopied: 4,
          linkProblemCount: 0,
          hasLinkProblems: false,
        }),
      });
    });
    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (url.includes("/virtual/publish") || url.includes("/virtual/build")) {
        await route.fallback();
        return;
      }
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          sourceKind: "csv-filesystem",
          rootPath: "C:/csv-docs",
          virtual: true,
        }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const empty = page.locator('[data-testid="developer-site-empty"]');
    if (await empty.isVisible().catch(() => false)) {
      throw new Error(
        "No sites in catalog — CSV Virtual Site publish intercept requires a site row",
      );
    }
    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible({
      timeout: 20_000,
    });
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-files"]')).toHaveText(
      "4",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-publish-dest"]')).toContainText(
      "csv-help",
    );
    expect(consoleErrors).toEqual([]);
  });

  test("csv-filesystem live Publish Virtual Site completes on H2 QA (#3699)", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const csvRoot = deployCsvVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live CSV Publish requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("csv-filesystem");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(csvRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const publishRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/publish(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    const publishResp = await publishRespPromise;
    const publishBody = await publishResp.text();
    expect(
      publishResp.ok(),
      `POST /virtual/publish HTTP ${publishResp.status()}: ${publishBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    const filesText = (
      await page.locator('[data-testid="developer-site-virtual-publish-files"]').textContent()
    ).trim();
    expect(Number.parseInt(filesText, 10), `files copied: ${filesText}`).toBeGreaterThan(0);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("csv-filesystem live Preview assembled site after Build (#3707)", async ({ page }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const csvRoot = deployCsvVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live CSV Preview requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const siteName = (
      await page.locator('[data-testid="developer-site-detail-title"]').textContent()
    ).trim();
    expect(siteName, "Site detail title required for preview URL").toBeTruthy();

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("csv-filesystem");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview-hint"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(csvRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();

    const previewStatusPromise = page.waitForResponse((resp) => {
      if (resp.request().method() !== "GET") {
        return false;
      }
      const url = resp.url();
      return /\/virtual\/preview(\?|$)/.test(url) && !/\/virtual\/preview\/.+/.test(url);
    });
    const popupPromise = page.waitForEvent("popup", { timeout: 20_000 }).catch(() => null);
    await page.locator('[data-testid="developer-site-virtual-preview"]').click();
    const statusResp = await previewStatusPromise;
    const statusBody = await statusResp.text();
    expect(
      statusResp.ok(),
      `GET /virtual/preview HTTP ${statusResp.status()}: ${statusBody}`,
    ).toBeTruthy();
    let statusJson = {};
    try {
      statusJson = JSON.parse(statusBody);
    } catch {
      throw new Error(`Preview status was not JSON: ${statusBody}`);
    }
    const statusRoot =
      statusJson.VirtualSitePreviewStatus ||
      statusJson.virtualSitePreviewStatus ||
      statusJson;
    expect(
      statusRoot.available === true || statusRoot.available === "true",
      `preview available: ${statusBody}`,
    ).toBeTruthy();
    const homePath = String(statusRoot.homePath || "").replace(/\\/g, "/").replace(/^\/+/, "");
    expect(homePath, `homePath in ${statusBody}`).toMatch(/index\.html$/);

    const popup = await popupPromise;
    let html = "";
    if (popup) {
      await popup.waitForLoadState("domcontentloaded").catch(() => {});
      html = await popup.content().catch(() => "");
      if (!html || /about:blank/i.test(popup.url())) {
        const probeUrl = popup.url();
        if (probeUrl && !/about:blank/i.test(probeUrl)) {
          const probe = await page.request.get(probeUrl);
          html = await probe.text();
        }
      }
      if (!popup.isClosed()) {
        await popup.close().catch(() => {});
      }
    }
    if (!/CSV QA Home|Hello from CSV Virtual Site/.test(html)) {
      const fileUrl = `${BASE_URL}/Rhythmyx/services/sites/${encodeURIComponent(siteName)}/virtual/preview/${homePath
        .split("/")
        .filter((seg) => seg.length > 0 && seg !== "." && seg !== "..")
        .map((seg) => encodeURIComponent(seg))
        .join("/")}`;
      const fileResp = await page.request.get(fileUrl);
      expect(
        fileResp.ok(),
        `GET preview home HTTP ${fileResp.status()} ${fileUrl}`,
      ).toBeTruthy();
      html = await fileResp.text();
    }
    expect(
      html,
      "assembled CSV home HTML should contain fixture title or body",
    ).toMatch(/CSV QA Home|Hello from CSV Virtual Site/);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("sql-database live Preview assembled site after Build (#3768)", async ({ page }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const sqlRoot = deploySqlVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live SQL Preview requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const siteName = (
      await page.locator('[data-testid="developer-site-detail-title"]').textContent()
    ).trim();
    expect(siteName, "Site detail title required for preview URL").toBeTruthy();

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("sql-database");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-sql-hint"]')).toContainText(
      "Build Virtual Site",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview-hint"]')).toContainText(
      "SQL database",
    );
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(sqlRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();

    const previewStatusPromise = page.waitForResponse((resp) => {
      if (resp.request().method() !== "GET") {
        return false;
      }
      const url = resp.url();
      return /\/virtual\/preview(\?|$)/.test(url) && !/\/virtual\/preview\/.+/.test(url);
    });
    const popupPromise = page.waitForEvent("popup", { timeout: 20_000 }).catch(() => null);
    await page.locator('[data-testid="developer-site-virtual-preview"]').click();
    const statusResp = await previewStatusPromise;
    const statusBody = await statusResp.text();
    expect(
      statusResp.ok(),
      `GET /virtual/preview HTTP ${statusResp.status()}: ${statusBody}`,
    ).toBeTruthy();
    let statusJson = {};
    try {
      statusJson = JSON.parse(statusBody);
    } catch {
      throw new Error(`Preview status was not JSON: ${statusBody}`);
    }
    const statusRoot =
      statusJson.VirtualSitePreviewStatus ||
      statusJson.virtualSitePreviewStatus ||
      statusJson;
    expect(
      statusRoot.available === true || statusRoot.available === "true",
      `preview available: ${statusBody}`,
    ).toBeTruthy();
    const homePath = String(statusRoot.homePath || "").replace(/\\/g, "/").replace(/^\/+/, "");
    expect(homePath, `homePath in ${statusBody}`).toMatch(/index\.html$/);

    const popup = await popupPromise;
    let html = "";
    if (popup) {
      await popup.waitForLoadState("domcontentloaded").catch(() => {});
      html = await popup.content().catch(() => "");
      if (!html || /about:blank/i.test(popup.url())) {
        const probeUrl = popup.url();
        if (probeUrl && !/about:blank/i.test(probeUrl)) {
          const probe = await page.request.get(probeUrl);
          html = await probe.text();
        }
      }
      if (!popup.isClosed()) {
        await popup.close().catch(() => {});
      }
    }
    if (!/SQL Home|Hello from SQL/.test(html)) {
      const fileUrl = `${BASE_URL}/Rhythmyx/services/sites/${encodeURIComponent(siteName)}/virtual/preview/${homePath
        .split("/")
        .filter((seg) => seg.length > 0 && seg !== "." && seg !== "..")
        .map((seg) => encodeURIComponent(seg))
        .join("/")}`;
      const fileResp = await page.request.get(fileUrl);
      expect(
        fileResp.ok(),
        `GET preview home HTTP ${fileResp.status()} ${fileUrl}`,
      ).toBeTruthy();
      html = await fileResp.text();
    }
    expect(
      html,
      "assembled SQL home HTML should contain fixture title or body",
    ).toMatch(/SQL Home|Hello from SQL/);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("http-json live Preview assembled site after Build (#3819)", async ({ page }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const httpJsonRoot = deployHttpJsonVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live HTTP JSON Preview requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const siteName = (
      await page.locator('[data-testid="developer-site-detail-title"]').textContent()
    ).trim();
    expect(siteName, "Site detail title required for preview URL").toBeTruthy();

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("http-json");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-http-json-hint"]')).toContainText(
      "Preview assembled site",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview-hint"]')).toContainText(
      "HTTP JSON",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(httpJsonRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();

    const previewStatusPromise = page.waitForResponse((resp) => {
      if (resp.request().method() !== "GET") {
        return false;
      }
      const url = resp.url();
      return /\/virtual\/preview(\?|$)/.test(url) && !/\/virtual\/preview\/.+/.test(url);
    });
    const popupPromise = page.waitForEvent("popup", { timeout: 20_000 }).catch(() => null);
    await page.locator('[data-testid="developer-site-virtual-preview"]').click();
    const statusResp = await previewStatusPromise;
    const statusBody = await statusResp.text();
    expect(
      statusResp.ok(),
      `GET /virtual/preview HTTP ${statusResp.status()}: ${statusBody}`,
    ).toBeTruthy();
    let statusJson = {};
    try {
      statusJson = JSON.parse(statusBody);
    } catch {
      throw new Error(`Preview status was not JSON: ${statusBody}`);
    }
    const statusRoot =
      statusJson.VirtualSitePreviewStatus ||
      statusJson.virtualSitePreviewStatus ||
      statusJson;
    expect(
      statusRoot.available === true || statusRoot.available === "true",
      `preview available: ${statusBody}`,
    ).toBeTruthy();
    const homePath = String(statusRoot.homePath || "").replace(/\\/g, "/").replace(/^\/+/, "");
    expect(homePath, `homePath in ${statusBody}`).toMatch(/index\.html$/);

    const popup = await popupPromise;
    let html = "";
    if (popup) {
      await popup.waitForLoadState("domcontentloaded").catch(() => {});
      html = await popup.content().catch(() => "");
      if (!html || /about:blank/i.test(popup.url())) {
        const probeUrl = popup.url();
        if (probeUrl && !/about:blank/i.test(probeUrl)) {
          const probe = await page.request.get(probeUrl);
          html = await probe.text();
        }
      }
      if (!popup.isClosed()) {
        await popup.close().catch(() => {});
      }
    }
    if (!/HTTP JSON Home|Hello from JSON/.test(html)) {
      const fileUrl = `${BASE_URL}/Rhythmyx/services/sites/${encodeURIComponent(siteName)}/virtual/preview/${homePath
        .split("/")
        .filter((seg) => seg.length > 0 && seg !== "." && seg !== "..")
        .map((seg) => encodeURIComponent(seg))
        .join("/")}`;
      const fileResp = await page.request.get(fileUrl);
      expect(
        fileResp.ok(),
        `GET preview home HTTP ${fileResp.status()} ${fileUrl}`,
      ).toBeTruthy();
      html = await fileResp.text();
    }
    expect(
      html,
      "assembled HTTP JSON home HTML should contain fixture title or body",
    ).toMatch(/HTTP JSON Home|Hello from JSON/);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("object-storage live Preview assembled site after Build (#3870)", async ({ page }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const objectStorageRoot = deployObjectStorageVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live object-storage Preview requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const siteName = (
      await page.locator('[data-testid="developer-site-detail-title"]').textContent()
    ).trim();
    expect(siteName, "Site detail title required for preview URL").toBeTruthy();

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("object-storage");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-object-storage-hint"]')).toContainText(
      "Preview assembled site",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview-hint"]')).toContainText(
      "Object storage",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(objectStorageRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();

    const previewStatusPromise = page.waitForResponse((resp) => {
      if (resp.request().method() !== "GET") {
        return false;
      }
      const url = resp.url();
      return /\/virtual\/preview(\?|$)/.test(url) && !/\/virtual\/preview\/.+/.test(url);
    });
    const popupPromise = page.waitForEvent("popup", { timeout: 20_000 }).catch(() => null);
    await page.locator('[data-testid="developer-site-virtual-preview"]').click();
    const statusResp = await previewStatusPromise;
    const statusBody = await statusResp.text();
    expect(
      statusResp.ok(),
      `GET /virtual/preview HTTP ${statusResp.status()}: ${statusBody}`,
    ).toBeTruthy();
    let statusJson = {};
    try {
      statusJson = JSON.parse(statusBody);
    } catch {
      throw new Error(`Preview status was not JSON: ${statusBody}`);
    }
    const statusRoot =
      statusJson.VirtualSitePreviewStatus ||
      statusJson.virtualSitePreviewStatus ||
      statusJson;
    expect(
      statusRoot.available === true || statusRoot.available === "true",
      `preview available: ${statusBody}`,
    ).toBeTruthy();
    const homePath = String(statusRoot.homePath || "").replace(/\\/g, "/").replace(/^\/+/, "");
    expect(homePath, `homePath in ${statusBody}`).toMatch(/index\.html$/);

    const popup = await popupPromise;
    let html = "";
    if (popup) {
      await popup.waitForLoadState("domcontentloaded").catch(() => {});
      html = await popup.content().catch(() => "");
      if (!html || /about:blank/i.test(popup.url())) {
        const probeUrl = popup.url();
        if (probeUrl && !/about:blank/i.test(probeUrl)) {
          const probe = await page.request.get(probeUrl);
          html = await probe.text();
        }
      }
      if (!popup.isClosed()) {
        await popup.close().catch(() => {});
      }
    }
    if (!/Object Home|Hello from objects/.test(html)) {
      const fileUrl = `${BASE_URL}/Rhythmyx/services/sites/${encodeURIComponent(siteName)}/virtual/preview/${homePath
        .split("/")
        .filter((seg) => seg.length > 0 && seg !== "." && seg !== "..")
        .map((seg) => encodeURIComponent(seg))
        .join("/")}`;
      const fileResp = await page.request.get(fileUrl);
      expect(
        fileResp.ok(),
        `GET preview home HTTP ${fileResp.status()} ${fileUrl}`,
      ).toBeTruthy();
      html = await fileResp.text();
    }
    expect(
      html,
      "assembled object-storage home HTML should contain fixture title or body",
    ).toMatch(/Object Home|Hello from objects/);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });


  test("rss-atom live Publish Virtual Site completes on H2 QA (#3931)", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const rssRoot = deployRssAtomVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog - live rss-atom Publish requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("rss-atom");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-rss-atom-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(rssRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();

    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const publishRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/publish(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    const publishResp = await publishRespPromise;
    const publishBody = await publishResp.text();
    expect(
      publishResp.ok(),
      `POST /virtual/publish HTTP ${publishResp.status()}: ${publishBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    const filesText = (
      await page.locator('[data-testid="developer-site-virtual-publish-files"]').textContent()
    ).trim();
    expect(Number.parseInt(filesText, 10), `files copied: ${filesText}`).toBeGreaterThan(0);
    const destText = (
      await page.locator('[data-testid="developer-site-virtual-publish-dest"]').textContent()
    ).trim();
    assertPublishedRssAtomFilesOnQaCell(destText);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("rss-atom live Preview assembled site after Build (#3928)", async ({ page }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const rssRoot = deployRssAtomVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live rss-atom Preview requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const siteName = (
      await page.locator('[data-testid="developer-site-detail-title"]').textContent()
    ).trim();
    expect(siteName, "Site detail title required for preview URL").toBeTruthy();

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("rss-atom");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-rss-atom-hint"]')).toContainText(
      "Preview assembled site",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview-hint"]')).toContainText(
      "RSS / Atom",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(rssRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();

    const previewStatusPromise = page.waitForResponse((resp) => {
      if (resp.request().method() !== "GET") {
        return false;
      }
      const url = resp.url();
      return /\/virtual\/preview(\?|$)/.test(url) && !/\/virtual\/preview\/.+/.test(url);
    });
    const popupPromise = page.waitForEvent("popup", { timeout: 20_000 }).catch(() => null);
    await page.locator('[data-testid="developer-site-virtual-preview"]').click();
    const statusResp = await previewStatusPromise;
    const statusBody = await statusResp.text();
    expect(
      statusResp.ok(),
      `GET /virtual/preview HTTP ${statusResp.status()}: ${statusBody}`,
    ).toBeTruthy();
    let statusJson = {};
    try {
      statusJson = JSON.parse(statusBody);
    } catch {
      throw new Error(`Preview status was not JSON: ${statusBody}`);
    }
    const statusRoot =
      statusJson.VirtualSitePreviewStatus ||
      statusJson.virtualSitePreviewStatus ||
      statusJson;
    expect(
      statusRoot.available === true || statusRoot.available === "true",
      `preview available: ${statusBody}`,
    ).toBeTruthy();
    const homePath = String(statusRoot.homePath || "").replace(/\\/g, "/").replace(/^\/+/, "");
    expect(homePath, `homePath in ${statusBody}`).toMatch(/index\.html$/);

    const popup = await popupPromise;
    let html = "";
    if (popup) {
      await popup.waitForLoadState("domcontentloaded").catch(() => {});
      html = await popup.content().catch(() => "");
      if (!html || /about:blank/i.test(popup.url())) {
        const probeUrl = popup.url();
        if (probeUrl && !/about:blank/i.test(probeUrl)) {
          const probe = await page.request.get(probeUrl);
          html = await probe.text();
        }
      }
      if (!popup.isClosed()) {
        await popup.close().catch(() => {});
      }
    }
    if (!/RSS Home|Hello from RSS/.test(html)) {
      const fileUrl = `${BASE_URL}/Rhythmyx/services/sites/${encodeURIComponent(siteName)}/virtual/preview/${homePath
        .split("/")
        .filter((seg) => seg.length > 0 && seg !== "." && seg !== "..")
        .map((seg) => encodeURIComponent(seg))
        .join("/")}`;
      const fileResp = await page.request.get(fileUrl);
      expect(
        fileResp.ok(),
        `GET preview home HTTP ${fileResp.status()} ${fileUrl}`,
      ).toBeTruthy();
      html = await fileResp.text();
    }
    expect(
      html,
      "assembled rss-atom home HTML should contain fixture title or body",
    ).toMatch(/RSS Home|Hello from RSS/);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("icalendar live Build Virtual Site completes on H2 QA (#3989)", async ({ page }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const icalRoot = deployIcalendarVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live icalendar Build requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("icalendar");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-icalendar-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(icalRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();
    const pagesText = (
      await page.locator('[data-testid="developer-site-virtual-build-pages"]').textContent()
    ).trim();
    expect(Number.parseInt(pagesText, 10), `pages written: ${pagesText}`).toBeGreaterThan(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("sitemap-xml live Build Virtual Site completes on H2 QA (#4164)", async ({ page }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const sitemapRoot = deploySitemapXmlVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live sitemap-xml Build requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("sitemap-xml");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-sitemap-xml-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(sitemapRoot);
    await saveVirtualSiteAndExpectSaved(page, { timeout: 20_000 });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();
    const pagesText = (
      await page.locator('[data-testid="developer-site-virtual-build-pages"]').textContent()
    ).trim();
    expect(Number.parseInt(pagesText, 10), `pages written: ${pagesText}`).toBeGreaterThan(0);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    await saveVirtualSiteAndExpectSaved(page, { timeout: 15_000 });
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("icalendar publish result shows files copied on HTTP 200 (#3989)", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (/Failed to load resource:.*404/.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual/publish", async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          siteName: "Help",
          publishPath: "C:/inetpub/wwwroot/icalendar-help",
          filesCopied: 3,
          pagesWritten: 1,
          hasLinkProblems: false,
        }),
      });
    });
    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (url.includes("/virtual/publish") || url.includes("/virtual/build")) {
        await route.fallback();
        return;
      }
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          sourceKind: "icalendar",
          rootPath: "C:/icalendar-docs",
          virtual: true,
        }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const empty = page.locator('[data-testid="developer-site-empty"]');
    if (await empty.isVisible().catch(() => false)) {
      throw new Error(
        "No sites in catalog - icalendar Virtual Site publish intercept requires a site row",
      );
    }
    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-icalendar-hint"]')).toContainText(
      "Publish Virtual Site",
    );
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-files"]')).toHaveText(
      "3",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-publish-dest"]')).toContainText(
      "icalendar-help",
    );
    expect(consoleErrors).toEqual([]);
  });

  test("icalendar live Publish Virtual Site completes on H2 QA (#3989)", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const icalRoot = deployIcalendarVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog - live icalendar Publish requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("icalendar");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-icalendar-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(icalRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();

    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const publishRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/publish(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    const publishResp = await publishRespPromise;
    const publishBody = await publishResp.text();
    expect(
      publishResp.ok(),
      `POST /virtual/publish HTTP ${publishResp.status()}: ${publishBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    const filesText = (
      await page.locator('[data-testid="developer-site-virtual-publish-files"]').textContent()
    ).trim();
    expect(Number.parseInt(filesText, 10), `files copied: ${filesText}`).toBeGreaterThan(0);
    const destText = (
      await page.locator('[data-testid="developer-site-virtual-publish-dest"]').textContent()
    ).trim();
    assertPublishedIcalendarFilesOnQaCell(destText);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("sitemap-xml publish result shows files copied on HTTP 200 (#4166)", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (/Failed to load resource:.*404/.test(text)) {
        return;
      }
      consoleErrors.push(text);
    });

    await page.route(
      (url) => /\/services\/sites\/?(\?|$)/.test(url.toString()),
      async (route) => {
        if (route.request().method() !== "GET") {
          await route.continue();
          return;
        }
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            SiteList: [{ name: "Help", label: "Help" }],
          }),
        });
      },
    );

    await page.route("**/services/sites/**/virtual/publish", async (route) => {
      if (route.request().method() !== "POST") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          siteName: "Help",
          publishPath: "C:/inetpub/wwwroot/sitemap-xml-help",
          filesCopied: 3,
          pagesWritten: 1,
          hasLinkProblems: false,
        }),
      });
    });
    await page.route("**/services/sites/**/virtual", async (route) => {
      const url = route.request().url();
      if (url.includes("/virtual/publish") || url.includes("/virtual/build")) {
        await route.fallback();
        return;
      }
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          sourceKind: "sitemap-xml",
          rootPath: "C:/sitemap-xml-docs",
          virtual: true,
        }),
      });
    });

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });

    const empty = page.locator('[data-testid="developer-site-empty"]');
    if (await empty.isVisible().catch(() => false)) {
      throw new Error(
        "No sites in catalog - sitemap-xml Virtual Site publish intercept requires a site row",
      );
    }
    const err = page.locator('[data-testid="developer-site-error"]');
    if (await err.isVisible().catch(() => false)) {
      throw new Error(`Sites catalog error: ${await err.textContent()}`);
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();

    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-sitemap-xml-hint"]')).toContainText(
      "Publish Virtual Site",
    );
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-files"]')).toHaveText(
      "3",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-publish-dest"]')).toContainText(
      "sitemap-xml-help",
    );
    expect(consoleErrors).toEqual([]);
  });

  test("sitemap-xml live Publish Virtual Site completes on H2 QA (#4166)", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const sitemapRoot = deploySitemapXmlVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog - live sitemap-xml Publish requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("sitemap-xml");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-sitemap-xml-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(sitemapRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const publishRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/publish(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-publish"]').click();
    const publishResp = await publishRespPromise;
    const publishBody = await publishResp.text();
    expect(
      publishResp.ok(),
      `POST /virtual/publish HTTP ${publishResp.status()}: ${publishBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-publish-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish-success"]')).toBeVisible();
    const filesText = (
      await page.locator('[data-testid="developer-site-virtual-publish-files"]').textContent()
    ).trim();
    expect(Number.parseInt(filesText, 10), `files copied: ${filesText}`).toBeGreaterThan(0);
    const destText = (
      await page.locator('[data-testid="developer-site-virtual-publish-dest"]').textContent()
    ).trim();
    assertPublishedSitemapXmlFilesOnQaCell(destText);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("icalendar live Preview assembled site after Build (#3989)", async ({ page }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const icalRoot = deployIcalendarVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live icalendar Preview requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const siteName = (
      await page.locator('[data-testid="developer-site-detail-title"]').textContent()
    ).trim();
    expect(siteName, "Site detail title required for preview URL").toBeTruthy();

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("icalendar");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-icalendar-hint"]')).toContainText(
      "Preview assembled site",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview-hint"]')).toContainText(
      "iCalendar",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(icalRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();

    const previewStatusPromise = page.waitForResponse((resp) => {
      if (resp.request().method() !== "GET") {
        return false;
      }
      const url = resp.url();
      return /\/virtual\/preview(\?|$)/.test(url) && !/\/virtual\/preview\/.+/.test(url);
    });
    const popupPromise = page.waitForEvent("popup", { timeout: 20_000 }).catch(() => null);
    await page.locator('[data-testid="developer-site-virtual-preview"]').click();
    const statusResp = await previewStatusPromise;
    const statusBody = await statusResp.text();
    expect(
      statusResp.ok(),
      `GET /virtual/preview HTTP ${statusResp.status()}: ${statusBody}`,
    ).toBeTruthy();
    let statusJson = {};
    try {
      statusJson = JSON.parse(statusBody);
    } catch {
      throw new Error(`Preview status was not JSON: ${statusBody}`);
    }
    const statusRoot =
      statusJson.VirtualSitePreviewStatus ||
      statusJson.virtualSitePreviewStatus ||
      statusJson;
    expect(
      statusRoot.available === true || statusRoot.available === "true",
      `preview available: ${statusBody}`,
    ).toBeTruthy();
    const homePath = String(statusRoot.homePath || "").replace(/\\/g, "/").replace(/^\/+/, "");
    expect(homePath, `homePath in ${statusBody}`).toMatch(/index\.html$/);

    const popup = await popupPromise;
    let html = "";
    if (popup) {
      await popup.waitForLoadState("domcontentloaded").catch(() => {});
      html = await popup.content().catch(() => "");
      if (!html || /about:blank/i.test(popup.url())) {
        const probeUrl = popup.url();
        if (probeUrl && !/about:blank/i.test(probeUrl)) {
          const probe = await page.request.get(probeUrl);
          html = await probe.text();
        }
      }
      if (!popup.isClosed()) {
        await popup.close().catch(() => {});
      }
    }
    if (!/iCalendar Home|Hello from iCalendar/.test(html)) {
      const fileUrl = `${BASE_URL}/Rhythmyx/services/sites/${encodeURIComponent(siteName)}/virtual/preview/${homePath
        .split("/")
        .filter((seg) => seg.length > 0 && seg !== "." && seg !== "..")
        .map((seg) => encodeURIComponent(seg))
        .join("/")}`;
      const fileResp = await page.request.get(fileUrl);
      expect(
        fileResp.ok(),
        `GET preview home HTTP ${fileResp.status()} ${fileUrl}`,
      ).toBeTruthy();
      html = await fileResp.text();
    }
    expect(
      html,
      "assembled icalendar home HTML should contain fixture title or body",
    ).toMatch(/iCalendar Home|Hello from iCalendar/);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("sitemap-xml live Preview assembled site after Build (#4165)", async ({ page }) => {
    test.setTimeout(120_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));

    const sitemapRoot = deploySitemapXmlVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live sitemap-xml Preview requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const siteName = (
      await page.locator('[data-testid="developer-site-detail-title"]').textContent()
    ).trim();
    expect(siteName, "Site detail title required for preview URL").toBeTruthy();

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("sitemap-xml");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-sitemap-xml-hint"]')).toContainText(
      "Preview assembled site",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview-hint"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview-hint"]')).toContainText(
      "Sitemap XML",
    );
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(sitemapRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const buildRespPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const buildResp = await buildRespPromise;
    const buildBody = await buildResp.text();
    expect(
      buildResp.ok(),
      `POST /virtual/build HTTP ${buildResp.status()}: ${buildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-result"]')).toBeVisible({
      timeout: 60_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible();

    const previewStatusPromise = page.waitForResponse((resp) => {
      if (resp.request().method() !== "GET") {
        return false;
      }
      const url = resp.url();
      return /\/virtual\/preview(\?|$)/.test(url) && !/\/virtual\/preview\/.+/.test(url);
    });
    const popupPromise = page.waitForEvent("popup", { timeout: 20_000 }).catch(() => null);
    await page.locator('[data-testid="developer-site-virtual-preview"]').click();
    const statusResp = await previewStatusPromise;
    const statusBody = await statusResp.text();
    expect(
      statusResp.ok(),
      `GET /virtual/preview HTTP ${statusResp.status()}: ${statusBody}`,
    ).toBeTruthy();
    let statusJson = {};
    try {
      statusJson = JSON.parse(statusBody);
    } catch {
      throw new Error(`Preview status was not JSON: ${statusBody}`);
    }
    const statusRoot =
      statusJson.VirtualSitePreviewStatus ||
      statusJson.virtualSitePreviewStatus ||
      statusJson;
    expect(
      statusRoot.available === true || statusRoot.available === "true",
      `preview available: ${statusBody}`,
    ).toBeTruthy();
    const homePath = String(statusRoot.homePath || "").replace(/\\/g, "/").replace(/^\/+/, "");
    expect(homePath, `homePath in ${statusBody}`).toMatch(/index\.html$/);

    const popup = await popupPromise;
    let html = "";
    if (popup) {
      await popup.waitForLoadState("domcontentloaded").catch(() => {});
      html = await popup.content().catch(() => "");
      if (!html || /about:blank/i.test(popup.url())) {
        const probeUrl = popup.url();
        if (probeUrl && !/about:blank/i.test(probeUrl)) {
          const probe = await page.request.get(probeUrl);
          html = await probe.text();
        }
      }
      if (!popup.isClosed()) {
        await popup.close().catch(() => {});
      }
    }
    if (!/Sitemap Home|Hello from sitemap/.test(html)) {
      const fileUrl = `${BASE_URL}/Rhythmyx/services/sites/${encodeURIComponent(siteName)}/virtual/preview/${homePath
        .split("/")
        .filter((seg) => seg.length > 0 && seg !== "." && seg !== "..")
        .map((seg) => encodeURIComponent(seg))
        .join("/")}`;
      const fileResp = await page.request.get(fileUrl);
      expect(
        fileResp.ok(),
        `GET preview home HTTP ${fileResp.status()} ${fileUrl}`,
      ).toBeTruthy();
      html = await fileResp.text();
    }
    expect(
      html,
      "assembled sitemap-xml home HTML should contain fixture title or body",
    ).toMatch(/Sitemap Home|Hello from sitemap/);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });

  test("sitemap-xml live rebuild after in-cell sitemap.xml edit without Jetty restart (#4188)", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const pageErrors = [];
    page.on("pageerror", (err) => pageErrors.push(String(err)));
    page.on("console", (msg) => {
      if (msg.type() !== "error") {
        return;
      }
      const text = msg.text();
      if (/Failed to load resource:.*404/.test(text)) {
        return;
      }
      pageErrors.push(text);
    });

    const sitemapRoot = deploySitemapXmlVirtualFixtureToQaCell();

    await page.goto(developerSectionUrl("sites"), {
      waitUntil: "networkidle",
    });
    await expect(page.locator('[data-testid="tab-developer-sites"]')).toBeVisible({
      timeout: 20_000,
    });

    const settled = page.locator(
      [
        '[data-testid="developer-site-panel"]',
        '[data-testid="developer-site-empty"]',
        '[data-testid="developer-site-error"]',
      ].join(", "),
    );
    await expect(settled.first()).toBeVisible({ timeout: 30_000 });
    if (await page.locator('[data-testid="developer-site-empty"]').isVisible().catch(() => false)) {
      throw new Error("No sites in catalog — live sitemap-xml rebuild requires a site row");
    }
    if (await page.locator('[data-testid="developer-site-error"]').isVisible().catch(() => false)) {
      throw new Error(
        `Sites catalog error: ${await page.locator('[data-testid="developer-site-error"]').textContent()}`,
      );
    }

    const rows = page.locator(catalogRowsSelector("developer-site-row"));
    await expect(rows.first()).toBeVisible({ timeout: 15_000 });
    await rows.first().locator('[data-testid="developer-site-open"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-form"]')).toBeVisible({
      timeout: 20_000,
    });

    const siteName = (
      await page.locator('[data-testid="developer-site-detail-title"]').textContent()
    ).trim();
    expect(siteName, "Site detail title required for preview URL").toBeTruthy();

    const kind = page.locator('[data-testid="developer-site-virtual-source-kind"]');
    await kind.selectOption("sitemap-xml");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-sitemap-xml-hint"]')).toBeVisible();
    // #4166: sitemap-xml shows Build + Preview + Publish chrome (shouldShowVirtualPublishChrome).
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();
    await page.locator('[data-testid="developer-site-virtual-root-path"]').fill(sitemapRoot);
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    const firstBuildPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const firstBuildResp = await firstBuildPromise;
    const firstBuildBody = await firstBuildResp.text();
    expect(
      firstBuildResp.ok(),
      `first POST /virtual/build HTTP ${firstBuildResp.status()}: ${firstBuildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible({
      timeout: 60_000,
    });
    const firstPagesText = (
      await page.locator('[data-testid="developer-site-virtual-build-pages"]').textContent()
    ).trim();
    const firstPages = Number.parseInt(firstPagesText, 10);
    expect(firstPages, `first pages written: ${firstPagesText}`).toBeGreaterThan(0);
    const firstHtml = await readVirtualPreviewHomeHtml(page, siteName);
    expect(firstHtml, "first assemble should include the original fixture body").toContain(
      SITEMAP_XML_VIRTUAL_BUILD_MARKER,
    );
    expect(firstHtml).not.toContain(SITEMAP_XML_VIRTUAL_REBUILD_MARKER);

    copySitemapXmlRebuildIntoQaCell();

    const secondBuildPromise = page.waitForResponse(
      (resp) =>
        resp.request().method() === "POST" && /\/virtual\/build(\?|$)/.test(resp.url()),
    );
    await page.locator('[data-testid="developer-site-virtual-build"]').click();
    const secondBuildResp = await secondBuildPromise;
    const secondBuildBody = await secondBuildResp.text();
    expect(
      secondBuildResp.ok(),
      `second POST /virtual/build HTTP ${secondBuildResp.status()}: ${secondBuildBody}`,
    ).toBeTruthy();
    await expect(page.locator('[data-testid="developer-site-virtual-build-success"]')).toBeVisible({
      timeout: 60_000,
    });
    const secondPagesText = (
      await page.locator('[data-testid="developer-site-virtual-build-pages"]').textContent()
    ).trim();
    const secondPages = Number.parseInt(secondPagesText, 10);
    expect(secondPages, `second pages written: ${secondPagesText}`).toBeGreaterThan(firstPages);
    const secondHtml = await readVirtualPreviewHomeHtml(page, siteName);
    expect(secondHtml, "second assemble should include the edited sitemap page body").toContain(
      SITEMAP_XML_VIRTUAL_REBUILD_MARKER,
    );
    expect(secondHtml).toContain(`Last modified: ${SITEMAP_XML_VIRTUAL_REBUILD_LASTMOD}`);
    expect(secondHtml).not.toContain(SITEMAP_XML_VIRTUAL_BUILD_MARKER);
    expect(secondHtml).not.toBe(firstHtml);
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toBeVisible();

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toHaveCount(0);
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
  });
});
