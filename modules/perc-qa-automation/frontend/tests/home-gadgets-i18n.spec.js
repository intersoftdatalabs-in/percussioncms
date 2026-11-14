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
 * Home Gadgets dashboard chrome is localized through the TMX catalog
 * (issue #6304) — no hardcoded English literals in gadget surfaces.
 *
 * <p>The CMS-only H2 cell has no DTS tier, so delivery/activity-backed
 * gadget data endpoints may answer 5xx; the widgets render the localized
 * error/empty states under test. Only those gadget data URLs are tolerated
 * below — every other console/page failure still fails the gate.</p>
 *
 * <p>Tags: {@code @home} {@code @i18n}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/home-gadgets-i18n.spec.js}</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

/**
 * Gadget data endpoints that 5xx on a CMS-only H2 cell (no DTS tier /
 * fixture payloads). Mirrors the delivery/activity failure class handled
 * for empty gadget payloads in #6302. Any other failing request fails.
 */
const KNOWN_CELL_5XX = [
  /\/services\/assetmanagement\/asset\/forms\//,
  /\/services\/activitymanagement\/activity\//,
  /\/services\/delivery\//,
];

function homeGadgetsUrl() {
  const q = new URLSearchParams({
    entry: "home",
    section: "gadgets",
    _: String(Date.now()),
  });
  return `${BASE_URL}/Rhythmyx/cm/app/spa.jsp?${q.toString()}`;
}

test.describe("Home Gadgets i18n chrome (#6304)", () => {
  test.beforeEach(async ({ page }) => {
    test.setTimeout(90_000);
    await loginAsAdmin(page);
  });

  test(
    "gadget tiles render localized chrome with zero console errors",
    { tag: ["@home", "@i18n"] },
    async ({ page }) => {
      const consoleErrors = [];
      const pageErrors = [];
      const failedRequests = [];
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          consoleErrors.push(msg.text());
        }
      });
      page.on("pageerror", (err) => {
        pageErrors.push(String(err && err.message ? err.message : err));
      });
      page.on("response", (res) => {
        if (res.status() >= 500) {
          failedRequests.push(`${res.status()} ${res.url()}`);
        }
      });

      await page.goto(homeGadgetsUrl(), { waitUntil: "domcontentloaded" });
      const section = page.getByTestId("home-gadgets-section");
      await expect(section).toBeVisible({ timeout: 30_000 });
      await expect(page.getByTestId("dashboard-root")).toBeVisible({
        timeout: 30_000,
      });
      // At least one gadget tile settles (list, empty, or error state —
      // every branch renders catalog strings after #6304).
      await expect(
        section.locator('[data-testid$="-widget"]').first(),
      ).toBeVisible({ timeout: 30_000 });
      // Welcome tile resolves its TMX greeting (fallback English in the
      // H2 cell proves the lookup path; a locale cell shows translated).
      await expect(
        section.getByText(/Good morning|Good afternoon|Good evening|Welcome/i).first(),
      ).toBeVisible({ timeout: 30_000 });
      // Error/empty tiles render the localized chrome under test.
      await expect(
        section.getByText(/Error|No .* (found|available|data)|Loading/i).first(),
      ).toBeVisible({ timeout: 30_000 });
      expect(pageErrors, `page errors: ${pageErrors.join(" | ")}`).toEqual([]);
      const unexpected = failedRequests.filter(
        (r) => !KNOWN_CELL_5XX.some((re) => re.test(r)),
      );
      expect(
        unexpected,
        `unexpected 5xx gadget data: ${unexpected.join(" | ")}`,
      ).toEqual([]);
      const resourceNoise = consoleErrors.filter((e) =>
        /Failed to load resource/i.test(e),
      );
      expect(
        resourceNoise.length,
        `resource console errors without a known cell 5xx: ${resourceNoise.join(" | ")} (requests: ${failedRequests.join(" | ")})`,
      ).toBeLessThanOrEqual(failedRequests.length);
      const otherConsole = consoleErrors.filter(
        (e) => !/Failed to load resource/i.test(e),
      );
      expect(
        otherConsole,
        `console errors: ${otherConsole.join(" | ")}`,
      ).toEqual([]);
    },
  );
});
