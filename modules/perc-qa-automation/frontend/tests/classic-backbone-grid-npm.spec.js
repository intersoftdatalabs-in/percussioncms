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
 * Classic-UI Backbone/Underscore from npm, Backgrid retired (issue #5664).
 *
 * <p>Tags: {@code @classic-ui} {@code @ui}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/classic-backbone-grid-npm.spec.js}</p>
 *
 * <p>Covers the vendored-copy removal: Backbone 1.4.0 and Underscore 1.13.1
 * (under cm/jslib, cm/vendor/js/legacy, and the war/ mirror) move to the npm
 * graph (Dependabot-tracked in {@code WebUI/src/main/frontend/package.json}:
 * backbone ^1.6.0, underscore ^1.13.7), and every vendored Backgrid (~0.3.x,
 * EOL ~2017) copy is retired — a repo-wide grep proves zero JSP/JS/bundle
 * consumers, so Backgrid is deleted with no replacement mapping and
 * DataTables (the grid classic pages actually load via
 * {@code cm/app/includes/common_js.jsp}) is the grid going forward. The
 * deleted vendored files must no longer resolve; the dashboard must load
 * with zero jslib failures; the DataTables plugin classic grids use must
 * still serve.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

// The grid classic-UI pages actually load (designated replacement surface).
const DATATABLES_PLUGIN =
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js";

const REMOVED_VENDORED = [
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/backbone/backbone.js",
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/underscore/underscore.js",
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/underscore/underscore-min.js",
  "/Rhythmyx/cm/jslib/backgridjs/backgrid.js",
  "/Rhythmyx/cm/jslib/backgridjs/backgrid.css",
  "/Rhythmyx/cm/jslib/backgridjs/extensions/filter/backgrid-filter.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/libraries/backgridjs/backgrid.js",
  "/Rhythmyx/cm/vendor/js/legacy/profiles/3x/libraries/backbone/backbone.js",
  "/Rhythmyx/cm/vendor/js/legacy/profiles/3x/libraries/underscore/underscore.js",
  "/Rhythmyx/cm/vendor/js/legacy/backgridjs/backgrid.js",
  "/Rhythmyx/cm/vendor/js/legacy/backgridjs/extensions/paginator/backgrid-paginator.js",
  "/Rhythmyx/cm/vendor/js/legacy/profiles/3x/jquery/libraries/backgridjs/backgrid.js",
];

test.describe("Classic-UI Backbone/Underscore from npm, Backgrid retired (issue #5664)", () => {
  test(
    "dashboard loads with zero jslib 404s after vendored removal @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const pageErrors = [];
      const failedRequests = [];
      const badResponses = [];
      // Pre-existing H2-cell noise, unrelated to the Backgrid retirement
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
        // absence as above) — not the Backgrid retirement.
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

      // No classic page may still reference the deleted vendored URLs:
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
    "removed vendored copies 404, grid plugin still serves @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      await loginAsAdmin(page);

      for (const asset of REMOVED_VENDORED) {
        const gone = await page.request.get(`${BASE_URL}${asset}`);
        expect(
          gone.status(),
          `expected 404 for removed vendored ${asset}`,
        ).toBe(404);
      }

      // Grid parity: Backgrid is retired, but the DataTables plugin the
      // classic grids actually load (common_js.jsp) must still resolve —
      // its migration belongs to the jQuery-plugin cluster (#5663), so this
      // pins the surface intact rather than migrating it.
      const grid = await page.request.get(`${BASE_URL}${DATATABLES_PLUGIN}`);
      expect(grid.status(), `expected 200 for ${DATATABLES_PLUGIN}`).toBe(
        200,
      );
      const gridBody = await grid.text();
      expect(gridBody).toContain("DataTable");
    },
  );
});
