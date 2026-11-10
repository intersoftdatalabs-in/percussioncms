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
 * Classic-UI Bootstrap 5 served from npm (issue #5661).
 *
 * <p>Tags: {@code @classic-ui} {@code @ui}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/classic-bootstrap-npm.spec.js}</p>
 *
 * <p>Covers the vendored-copy removal: the Bootstrap 4.6.2 tree (EOL January
 * 2023) under cm/jslib, cm/vendor/js/legacy, and war/jslib was deleted, and
 * classic pages that include {@code cm/app/includes/common_js.jsp}
 * (dashboard, adminWorkflow, webmgt, …) now load Bootstrap 5 from the same
 * stable versionless jslib URLs, served out of npm
 * (bootstrap ^5.3.8, Dependabot-tracked in
 * {@code WebUI/src/main/frontend/package.json}) via the generated-webui
 * overlay. The Bootstrap 4 jQuery call shapes classic code still uses
 * ({@code $.fn.modal}, {@code $.fn.tooltip}, …) are bridged by the
 * {@code /cm/assets/js/bootstrap5-compat.js} shim loaded right after the
 * bundle in those pages.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const NPM_SERVED = [
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/bootstrap/js/bootstrap.bundle.js",
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/bootstrap/js/bootstrap.bundle.min.js",
  "/Rhythmyx/cm/jslib/profiles/3x/libraries/bootstrap/css/bootstrap.min.css",
];

const SHIM_SERVED = ["/Rhythmyx/cm/assets/js/bootstrap5-compat.js"];

test.describe("Classic-UI Bootstrap 5 from npm (issue #5661)", () => {
  test(
    "dashboard loads npm Bootstrap 5 with zero jslib 404s @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const bootstrapErrors = [];
      const failedRequests = [];
      page.on("pageerror", (err) => {
        const stack = String((err && err.stack) || err);
        // Fail only on errors arising from the migrated bootstrap files.
        // The dashboard emits pre-existing cell errors (never-shipped
        // cm/gadgets/container/* probes, a PercDashboard I18N.meesage typo,
        // dashboard.jsp set_label) that sibling classic specs fail on
        // identically in this cell — unrelated to #5661.
        if (/bootstrap/i.test(stack)) {
          bootstrapErrors.push(stack);
        }
      });
      page.on("console", (msg) => {
        if (msg.type() === "error" && /bootstrap/i.test(msg.text())) {
          bootstrapErrors.push(msg.text());
        }
      });
      page.on("requestfailed", (req) => {
        if (req.url().includes("/cm/jslib/")) {
          failedRequests.push(`${req.url()} :: ${req.failure()?.errorText}`);
        }
      });
      page.on("response", (resp) => {
        if (resp.status() >= 400 && resp.url().includes("/cm/jslib/")) {
          failedRequests.push(`${resp.url()} :: HTTP ${resp.status()}`);
        }
      });

      await loginAsAdmin(page);
      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/dashboard.jsp`, {
        waitUntil: "networkidle",
      });

      const flags = await page.evaluate(() => ({
        bootstrap:
          typeof window.bootstrap === "object" &&
          window.bootstrap !== null &&
          typeof window.bootstrap.Modal === "function" &&
          typeof window.bootstrap.Tooltip === "function",
        jqueryBridge:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.modal === "function" &&
          typeof window.jQuery.fn.tooltip === "function",
      }));

      expect(flags.bootstrap, "npm Bootstrap 5 missing on dashboard").toBe(
        true,
      );
      expect(
        flags.jqueryBridge,
        "bootstrap5-compat jQuery bridge missing on dashboard",
      ).toBe(true);

      expect(
        failedRequests,
        `failed jslib requests:\n${failedRequests.join("\n")}`,
      ).toEqual([]);
      expect(
        bootstrapErrors,
        `bootstrap errors:\n${bootstrapErrors.join("\n")}`,
      ).toEqual([]);
    },
  );

  test(
    "npm Bootstrap URLs serve v5 bytes (no 4.6.2 remnants) @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      await loginAsAdmin(page);

      for (const asset of [...NPM_SERVED, ...SHIM_SERVED]) {
        const resp = await page.request.get(`${BASE_URL}${asset}`);
        expect(resp.status(), `expected 200 for ${asset}`).toBe(200);
      }
      for (const asset of NPM_SERVED) {
        const body = await (
          await page.request.get(`${BASE_URL}${asset}`)
        ).text();
        expect(
          body,
          `${asset} should carry the Bootstrap 5 banner`,
          // Note: bootstrap.min.css ships "Bootstrap  v5…" (two spaces).
        ).toMatch(/Bootstrap\s+v5\./);
        expect(
          body,
          `${asset} still serves vendored 4.6.2 bytes`,
        ).not.toMatch(/Bootstrap \(v4\.6\.2\)|Bootstrap v4\.6/);
      }
    },
  );

  test(
    "adminWorkflow loads npm Bootstrap with zero jslib 404s @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const failedRequests = [];
      page.on("requestfailed", (req) => {
        if (req.url().includes("/cm/jslib/")) {
          failedRequests.push(`${req.url()} :: ${req.failure()?.errorText}`);
        }
      });
      page.on("response", (resp) => {
        if (resp.status() >= 400 && resp.url().includes("/cm/jslib/")) {
          failedRequests.push(`${resp.url()} :: HTTP ${resp.status()}`);
        }
      });

      await loginAsAdmin(page);
      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/adminWorkflow.jsp`, {
        waitUntil: "networkidle",
      });

      const bootstrap = await page.evaluate(
        () =>
          typeof window.bootstrap === "object" &&
          window.bootstrap !== null &&
          typeof window.bootstrap.Modal === "function",
      );
      expect(bootstrap, "npm Bootstrap 5 missing on adminWorkflow").toBe(true);

      expect(
        failedRequests,
        `failed jslib requests:\n${failedRequests.join("\n")}`,
      ).toEqual([]);
    },
  );
});
