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
 * Developer Sites → Virtual Site source panel (#2956 / #3020 / #3300 / #3687 / #3697 / epic #2678).
 *
 * Opens Sites catalog detail and asserts the Virtual Site source section mounts
 * with source-kind control (repository default, git-filesystem, csv-filesystem),
 * save chrome, Build chrome for git-filesystem and csv-filesystem (never repository),
 * and Publish chrome for git-filesystem only (CSV publish is a later slice).
 * Also intercepts build REST to prove link-problem detail lines render on HTTP 200
 * and publish REST to prove dest path + files copied on HTTP 200. Live H2 QA
 * deploys a CSV tree into the cell and asserts POST /virtual/build completes.
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

function developerSectionUrl(section) {
  const q = new URLSearchParams({
    entry: "developer",
    section,
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
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
      await expect(kind.locator('option[value="repository"]')).toHaveCount(1);
      await expect(kind.locator('option[value="git-filesystem"]')).toHaveCount(1);
      await expect(kind.locator('option[value="csv-filesystem"]')).toHaveCount(1);
      // Default traditional sites use repository option
      await expect(kind).toHaveValue(/repository|git-filesystem|csv-filesystem/);
      await expect(page.locator('[data-testid="developer-site-virtual-save"]')).toBeVisible();

      // Repository mode: Build chrome must not appear (no misleading virtual-build UI)
      const current = await kind.inputValue();
      if (current === "repository" || current === "") {
        await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toHaveCount(0);
        await expect(
          page.locator('[data-testid="developer-site-virtual-build-section"]'),
        ).toHaveCount(0);
        await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
      }

      // Switch to csv-filesystem reveals root path + Build (no Git remotes / Publish)
      await kind.selectOption("csv-filesystem");
      await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-csv-hint"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-remote-url"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-branch"]')).toHaveCount(0);
      await expect(page.locator('[data-testid="developer-site-virtual-build-section"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-build"]')).toBeVisible();
      await expect(page.locator('[data-testid="developer-site-virtual-preview"]')).toBeVisible();
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

      // Preview without a last build: in-panel empty/error (no 500 / no crash)
      await page.locator('[data-testid="developer-site-virtual-preview"]').click();
      const previewChrome = page.locator(
        [
          '[data-testid="developer-site-virtual-preview-error"]',
          '[data-testid="developer-site-virtual-build-error"]',
        ].join(", "),
      );
      await expect(previewChrome.first()).toBeVisible({ timeout: 20_000 });

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
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);

    await kind.selectOption("repository");
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
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
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);

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
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);

    await kind.selectOption("repository");
    lastPutBody = null;
    await page.locator('[data-testid="developer-site-virtual-save"]').click();
    await expect(page.locator('[data-testid="developer-site-virtual-saved"]')).toBeVisible({
      timeout: 15_000,
    });
    expect(lastPutBody.VirtualSiteProperties.sourceKind).toBe("repository");
    await expect(page.locator('[data-testid="developer-site-virtual-root-path"]')).toHaveCount(0);
    expect(pageErrors, `uncaught page errors: ${pageErrors.join(" | ")}`).toEqual([]);
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
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);
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
    await expect(page.locator('[data-testid="developer-site-virtual-publish"]')).toHaveCount(0);

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
});
