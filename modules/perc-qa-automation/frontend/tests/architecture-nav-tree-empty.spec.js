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
 * Architecture nav tree empty / missing NavTree UX (#3218 / parent #3197).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-nav-tree-empty.spec.js
 *
 * Intercepts site list + tree so the assertion does not depend on seeded
 * sites. Empty 200 tree must be an operator empty state, not HTTP 500 chrome.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

function architectureUrl(extra = {}) {
  const q = new URLSearchParams({
    entry: "architecture",
    site: "BareSite",
    _: String(Date.now()),
    ...extra,
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Architecture empty nav tree (#3218)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("empty 200 tree is operator empty state not 500 banner @smoke @ui", async ({
    page,
  }) => {
    const consoleErrors = [];
    page.on("pageerror", (err) => {
      consoleErrors.push(String(err && err.message ? err.message : err));
    });
    page.on("console", (msg) => {
      if (msg.type() === "error") {
        consoleErrors.push(msg.text());
      }
    });

    await page.route("**/sitemanage/site/**", async (route) => {
      if (route.request().method() !== "GET") {
        await route.continue();
        return;
      }
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          SiteSummary: [{ name: "BareSite" }],
        }),
      });
    });
    await page.route("**/sitemanage/section/tree/**", async (route) => {
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          SectionNode: {
            title: "BareSite",
            folderPath: "//Sites/BareSite",
            childNodes: [],
          },
        }),
      });
    });

    await page.goto(architectureUrl(), { waitUntil: "domcontentloaded" });

    await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
      timeout: 20_000,
    });
    await expect(page.getByTestId("architecture-nav-tree-empty")).toBeVisible({
      timeout: 20_000,
    });
    await expect(
      page.getByTestId("architecture-nav-tree-empty-title"),
    ).toContainText(/no navigation tree/i);
    await expect(page.getByTestId("architecture-nav-tree-error")).toHaveCount(0);
    await expect(page.getByText(/HTTP 500/i)).toHaveCount(0);

    expect(
      consoleErrors.filter(
        (e) =>
          !/favicon|Download the React DevTools|ResizeObserver|third-party|Failed to load resource|net::ERR_/i.test(
            e,
          ),
      ),
    ).toEqual([]);
  });
});
