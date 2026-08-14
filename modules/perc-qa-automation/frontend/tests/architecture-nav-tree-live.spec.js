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
 * Live Navigation tree HTTP contract (#3218 / #3352 / parent #3197).
 *
 * Surface-filtered only:
 *   npm run test:surface -- --path tests/architecture-nav-tree-live.spec.js
 *
 * Hits real GET /Rhythmyx/services/sitemanage/section/tree/{siteName}
 * (does not mock). Seeded demo sites (Corporate_Investments /
 * Enterprise_Investments) must return a NavTree root (id set) so the SPA
 * renders role=tree with ≥1 treeitem. Sites that truly have no tree stay
 * HTTP 200 empty + operator empty state, never HTTP 500.
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");
const {
  isEmptyTreePayload,
  isSampleDemoSite,
  siteNamesFromPayload,
} = require("./helpers/nav-tree-live");

function architectureUrl(extra = {}) {
  const q = new URLSearchParams({
    entry: "architecture",
    _: String(Date.now()),
    ...extra,
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Architecture live nav tree (#3218 / #3352)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test("listed sites tree GET is 200 and empty tree is not HTTP 500 @smoke @ui", async ({
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

    const sitesResp = await page.request.get(
      `${BASE_URL}/Rhythmyx/services/sitemanage/site/`,
    );
    expect(sitesResp.status(), "site list must not be HTTP 500").not.toBe(500);
    expect(sitesResp.ok()).toBeTruthy();
    const names = siteNamesFromPayload(await sitesResp.json());
    expect(names.length, "QA cell must seed at least one site").toBeGreaterThan(
      0,
    );

    const emptySites = [];
    const demoSites = names.filter((n) => isSampleDemoSite(n));
    for (const name of names) {
      const treeResp = await page.request.get(
        `${BASE_URL}/Rhythmyx/services/sitemanage/section/tree/${encodeURIComponent(
          name,
        )}`,
      );
      expect(
        treeResp.status(),
        `tree GET for ${name} must not be HTTP 500`,
      ).not.toBe(500);
      expect(treeResp.status(), `tree GET for ${name}`).toBe(200);
      const text = await treeResp.text();
      const empty = isEmptyTreePayload(text);
      if (isSampleDemoSite(name)) {
        expect(
          empty,
          `demo site ${name} must have a NavTree root after first GET (#3352)`,
        ).toBe(false);
      }
      if (empty) {
        emptySites.push(name);
      }
    }

    const firstDemo = demoSites[0];
    if (firstDemo) {
      await page.goto(architectureUrl({ site: firstDemo }), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.getByTestId("architecture-nav-tree-error")).toHaveCount(
        0,
      );
      await expect(page.getByText(/HTTP 500/i)).toHaveCount(0);
      await expect(page.getByTestId("architecture-nav-tree-empty")).toHaveCount(
        0,
      );
      await expect(page.getByRole("tree")).toBeVisible({ timeout: 20_000 });
      await expect(page.getByRole("treeitem").first()).toBeVisible();
    } else {
      const firstEmpty = emptySites[0] || names[0];
      await page.goto(architectureUrl({ site: firstEmpty }), {
        waitUntil: "domcontentloaded",
      });
      await expect(page.getByTestId("perc-architecture-shell")).toBeVisible({
        timeout: 20_000,
      });
      await expect(page.getByTestId("architecture-nav-tree-error")).toHaveCount(
        0,
      );
      await expect(page.getByText(/HTTP 500/i)).toHaveCount(0);
      if (emptySites.length > 0) {
        await expect(
          page.getByTestId("architecture-nav-tree-empty"),
        ).toBeVisible({
          timeout: 20_000,
        });
        await expect(
          page.getByTestId("architecture-nav-tree-empty-title"),
        ).toContainText(/no navigation tree/i);
      }
    }

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
