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
 * Classic-UI Handlebars served from npm (issue #5666).
 *
 * <p>Tags: {@code @classic-ui} {@code @ui}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/classic-handlebars-npm.spec.js}</p>
 *
 * <p>Covers the vendored-copy removal: the vulnerable
 * {@code handlebars-v4.0.12.js} (template-escape CVEs fixed in >= 4.7.7) and
 * the duplicate {@code handlebars-v4.7.8.js} are deleted, and classic pages
 * that include {@code cm/app/includes/common_js.jsp} (dashboard, …) must load
 * Handlebars >= 4.7.7 from the npm-backed versioned URL (Dependabot-tracked
 * in {@code WebUI/src/main/frontend/package.json}). The deleted vendored
 * files must no longer resolve, and rendering a template on the page must
 * HTML-escape double-stash output (the CVE class).</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const NPM_BACKED = "/Rhythmyx/cm/jslib/profiles/3x/libraries/handlebars/handlebars-v4.7.8.js";

const REMOVED_VENDORED = [
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/handlebars/handlebars-v4.0.12.js",
];

test.describe("Classic-UI Handlebars from npm (issue #5666)", () => {
  test(
    "dashboard loads npm Handlebars >= 4.7.7 with zero script 404s @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const pageErrors = [];
      const failedRequests = [];
      const badResponses = [];
      // Pre-existing H2-cell noise, unrelated to the Handlebars migration
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
        // absence as above) — not the Handlebars migration.
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

      const result = await page.evaluate(() => {
        const version =
          typeof window.Handlebars !== "undefined"
            ? window.Handlebars.VERSION || null
            : null;
        let rendered = null;
        let escaped = null;
        if (typeof window.Handlebars !== "undefined") {
          rendered = window.Handlebars.compile(
            '<div class="alert">{{source}}:{{status}}</div>',
          )({ source: "Publish", status: "ok" });
          escaped = window.Handlebars.compile("{{source}}")({
            source: '<script>alert("x")</script>',
          });
        }
        return { version, rendered, escaped };
      });

      expect(result.version, "Handlebars missing on dashboard").not.toBeNull();
      const parts = String(result.version).split(".").map(Number);
      const atLeast477 =
        parts[0] > 4 ||
        (parts[0] === 4 && (parts[1] > 7 || (parts[1] === 7 && parts[2] >= 7)));
      expect(
        atLeast477,
        `shipped Handlebars ${result.version} is below the CVE floor 4.7.7`,
      ).toBe(true);
      expect(result.rendered).toBe(
        '<div class="alert">Publish:ok</div>',
      );
      expect(result.escaped).toBe(
        "&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;",
      );

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
    "npm-backed URL resolves, vulnerable vendored copy is gone @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      await loginAsAdmin(page);

      const resp = await page.request.get(`${BASE_URL}${NPM_BACKED}`);
      expect(resp.status(), `expected 200 for ${NPM_BACKED}`).toBe(200);
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
