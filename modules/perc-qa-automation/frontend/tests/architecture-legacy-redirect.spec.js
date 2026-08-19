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
 * Architecture legacy entry retirement (#3099 / #3587 / parent #3092).
 * Console-clean bookmark / ?view=arch (#3612) — no HTTP 500 allowlist.
 *
 * Surface-filtered:
 *   npm run test:surface -- --path tests/architecture-legacy-redirect.spec.js
 *
 * Proves the retired siteArchitecture.jsp bookmark URL and ?view=arch land on
 * SPA Architecture shell (no leftover CM1 site-map host chrome) and that the
 * remaining Architecture load (section tree) is HTTP 200 / console-clean.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  attachConsoleCleanGate,
  formatHttp5xxHits,
  isSectionTreeRequestUrl,
} = require("./helpers/architecture-legacy-redirect");

test.describe("Architecture legacy entry retirement (#3099 / #3587)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("siteArchitecture.jsp bookmark lands on SPA Architecture @smoke @ui", async ({
    page,
  }) => {
    const { pageErrors, consoleErrors, http5xx } =
      attachConsoleCleanGate(page);
    const treeRespPromise = page.waitForResponse(
      (res) =>
        isSectionTreeRequestUrl(res.url()) &&
        res.request().method() === "GET",
      { timeout: 30_000 },
    );
    // Host JSP is no longer shipped (#3587); filter 301 keeps this bookmark.
    const url = `${BASE_URL}/Rhythmyx/cm/app/siteArchitecture.jsp?_=${Date.now()}`;
    await page.goto(url, { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("perc-spa-topnav")).toBeVisible({
      timeout: 30_000,
    });
    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 30_000,
    });
    const treeResp = await treeRespPromise;
    expect(
      treeResp.status(),
      `section/tree GET ${treeResp.url()}`,
    ).toBe(200);
    // Must not still be the classic site-map host
    await expect(page.locator("#perc_site_map")).toHaveCount(0);
    await expect(page.locator(".perc-mcol")).toHaveCount(0);
    const finalUrl = page.url();
    expect(finalUrl).toMatch(/architecture|entry=architecture|view=arch/i);
    expect(pageErrors, "uncaught pageerror on Architecture bookmark").toEqual(
      [],
    );
    expect(consoleErrors, "console error on Architecture bookmark").toEqual(
      [],
    );
    expect(
      formatHttp5xxHits(http5xx),
      "HTTP 5xx on Architecture bookmark (do not allowlist 500)",
    ).toEqual([]);
  });

  test("?view=arch deep link lands on SPA Architecture @smoke @ui", async ({
    page,
  }) => {
    const { pageErrors, consoleErrors, http5xx } =
      attachConsoleCleanGate(page);
    const treeRespPromise = page.waitForResponse(
      (res) =>
        isSectionTreeRequestUrl(res.url()) &&
        res.request().method() === "GET",
      { timeout: 30_000 },
    );
    const url = `${BASE_URL}/Rhythmyx/cm/app/?view=arch&_=${Date.now()}`;
    await page.goto(url, { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 30_000,
    });
    const treeResp = await treeRespPromise;
    expect(
      treeResp.status(),
      `section/tree GET ${treeResp.url()}`,
    ).toBe(200);
    await expect(page.locator("#perc_site_map")).toHaveCount(0);
    expect(pageErrors, "uncaught pageerror on view=arch").toEqual([]);
    expect(consoleErrors, "console error on view=arch").toEqual([]);
    expect(
      formatHttp5xxHits(http5xx),
      "HTTP 5xx on view=arch (do not allowlist 500)",
    ).toEqual([]);
  });
});
