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
 * Classic-UI Moment.js served from npm (issue #5665).
 *
 * <p>Tags: {@code @classic-ui} {@code @ui}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/classic-moment-npm.spec.js}</p>
 *
 * <p>Covers the vendored-copy removal: the Moment 2.29.4
 * {@code moment-with-locales.js} and the vendored
 * {@code moment-jdateformatparser.js} copies (under cm/jslib,
 * cm/vendor/js/legacy, and the war/ mirror) are deleted, and the classic
 * minify bundles keep resolving the stable jslib paths from the npm graph
 * (Dependabot-tracked in {@code WebUI/src/main/frontend/package.json}:
 * moment ^2.31.0, moment-jdateformatparser ^1.2.1). The deleted vendored
 * files must no longer resolve; the generated bundles served by the cell
 * must carry the npm moment bytes (version + locale data + plugin).</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

// Generated bundles (target/generated-webui overlay) that embed npm moment.
const MINUET_BUNDLE = "/Rhythmyx/cm/shared-common-minuet.js";
const COMMON_UI_BUNDLE = "/Rhythmyx/cm/perc_common_ui.js";

const REMOVED_VENDORED = [
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/momentjs/moment-with-locales.js",
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/momentjs/plugins/moment-jdateformatparser/moment-jdateformatparser.js",
  "/Rhythmyx/cm/vendor/js/legacy/profiles/3x/libraries/momentjs/moment-with-locales.js",
  "/Rhythmyx/cm/vendor/js/legacy/profiles/3x/libraries/momentjs/plugins/moment-jdateformatparser/moment-jdateformatparser.js",
];

test.describe("Classic-UI Moment.js from npm (issue #5665)", () => {
  test(
    "dashboard loads with zero jslib 404s after vendored removal @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const pageErrors = [];
      const failedRequests = [];
      const badResponses = [];
      // Pre-existing H2-cell noise, unrelated to the Moment migration
      // (verified: gadget container files are absent from the cell image, so
      // the dashboard gadget list cannot load; the failure error-path then
      // hits the pre-existing I18N.meesage typo in PercDashboard.js:349, and
      // the gadget tray crashes dereferencing the missing gadget JSON in
      // PercDashboard.js addGadgetToTray:1193):
      //   - /cm/gadgets/... and PercBlogsGadget 404/MIME console errors
      //   - "I18N.meesage is not a function" from that error path
      //   - exactly "TypeError: Cannot read properties of undefined
      //     (reading 'toUpperCase')" from the tray crash (exact-message match
      //     only — any other TypeError still fails)
      // Anything else still fails the test.
      const KNOWN_CELL_NOISE = [
        "/cm/gadgets/",
        "PercBlogsGadget",
        "I18N.meesage is not a function",
        "TypeError: Cannot read properties of undefined (reading 'toUpperCase')",
      ];
      const isKnownNoise = (text) =>
        KNOWN_CELL_NOISE.some((marker) => String(text).includes(marker));
      page.on("pageerror", (err) => {
        if (!isKnownNoise(err)) {
          pageErrors.push(String(err));
        }
      });
      page.on("console", (msg) => {
        if (msg.type() === "error" && !isKnownNoise(msg.text())) {
          pageErrors.push(msg.text());
        }
      });
      page.on("requestfailed", (req) => {
        // Scope to the migrated library surface: dashboard gadgets abort
        // unrelated container requests in the H2 cell (same environmental
        // absence as above) — not the Moment migration.
        if (req.url().includes("/cm/jslib/")) {
          failedRequests.push(`${req.url()} :: ${req.failure()?.errorText}`);
        }
      });
      page.on("response", (resp) => {
        if (resp.status() >= 400) {
          badResponses.push({ url: resp.url(), status: resp.status() });
          if (resp.url().includes("/cm/jslib/")) {
            failedRequests.push(`${resp.url()} :: HTTP ${resp.status()}`);
          }
        }
      });

      await loginAsAdmin(page);
      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/dashboard.jsp`, {
        waitUntil: "networkidle",
      });

      // No classic page may still reference the deleted vendored moment URLs:
      // any such reference fails here via failedRequests.
      expect(
        failedRequests,
        `failed jslib requests:\n${failedRequests.join("\n")}`,
      ).toEqual([]);
      // Generic "Failed to load resource" console lines are the browser echo
      // of observed HTTP failures: each one must be accounted for by an
      // observed non-jslib failure (the gadget absence above). Any /cm/jslib/
      // failure already fails via failedRequests, so it cannot hide here.
      const genericLoadLines = pageErrors.filter((t) =>
        t.startsWith("Failed to load resource"),
      );
      const otherErrors = pageErrors.filter(
        (t) => !t.startsWith("Failed to load resource"),
      );
      const nonJslibFailures = badResponses.filter(
        (r) => !r.url.includes("/cm/jslib/"),
      );
      expect(
        otherErrors,
        `page errors:\n${otherErrors.join("\n")}`,
      ).toEqual([]);
      expect(
        genericLoadLines.length,
        `unaccounted resource-load errors:\n${genericLoadLines.join("\n")}\nobserved non-jslib failures:\n${nonJslibFailures.map((r) => `${r.status} ${r.url}`).join("\n")}`,
      ).toBeLessThanOrEqual(nonJslibFailures.length);
    },
  );

  test(
    "npm-backed bundles carry moment, removed vendored copies 404 @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      await loginAsAdmin(page);

      // The minuet intermediate embeds the npm with-locales dist (locale
      // data is load-bearing: delivery views call moment.locale()).
      const minuet = await page.request.get(`${BASE_URL}${MINUET_BUNDLE}`);
      expect(minuet.status(), `expected 200 for ${MINUET_BUNDLE}`).toBe(200);
      const minuetBody = await minuet.text();
      expect(minuetBody).toContain("2.31.0");
      expect(minuetBody).toContain("defineLocale");

      // The legacy delivery bundle embeds npm moment + the npm plugin.
      const commonUi = await page.request.get(
        `${BASE_URL}${COMMON_UI_BUNDLE}`,
      );
      expect(commonUi.status(), `expected 200 for ${COMMON_UI_BUNDLE}`).toBe(
        200,
      );
      const commonUiBody = await commonUi.text();
      expect(commonUiBody).toContain("2.31.0");
      expect(commonUiBody).toContain("formatWithJDF");

      for (const asset of REMOVED_VENDORED) {
        const gone = await page.request.get(`${BASE_URL}${asset}`);
        expect(
          gone.status(),
          `expected 404 for removed vendored ${asset}`,
        ).toBe(404);
      }
    },
  );
});
