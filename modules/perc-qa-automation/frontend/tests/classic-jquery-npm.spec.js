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
 * Classic-UI jQuery trio served from npm (issue #5660).
 *
 * <p>Tags: {@code @classic-ui} {@code @ui}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/classic-jquery-npm.spec.js}</p>
 *
 * <p>Covers the vendored-copy removal: classic pages that include
 * {@code cm/app/includes/common_js.jsp} (dashboard, adminWorkflow, webmgt,
 * …) must load jQuery core + Migrate + UI from the npm-backed versionless
 * mins (jQuery 4.x + Migrate 4.x + UI 1.14.x, Dependabot-tracked in
 * {@code WebUI/src/main/frontend/package.json}), and the deleted vendored
 * versioned files must no longer resolve.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const TRIO = [
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/jquery.min.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/jquery-migrate.min.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.min.js",
];

const REMOVED_VENDORED = [
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/jquery-3.6.0.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/jquery-migrate-3.3.2.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/libraries/jquery-ui/jquery-ui.js",
  "/Rhythmyx/cm/jslib/jquery-ui.js",
];

test.describe("Classic-UI jQuery trio from npm (issue #5660)", () => {
  test(
    "dashboard loads npm jQuery/Migrate/UI with zero script 404s @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const pageErrors = [];
      const failedRequests = [];
      page.on("pageerror", (err) => pageErrors.push(String(err)));
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          pageErrors.push(msg.text());
        }
      });
      page.on("requestfailed", (req) =>
        failedRequests.push(`${req.url()} :: ${req.failure()?.errorText}`),
      );
      page.on("response", (resp) => {
        if (resp.status() >= 400 && resp.url().includes("/cm/jslib/")) {
          failedRequests.push(`${resp.url()} :: HTTP ${resp.status()}`);
        }
      });

      await loginAsAdmin(page);
      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/dashboard.jsp`, {
        waitUntil: "networkidle",
      });

      const versions = await page.evaluate(() => ({
        jquery:
          typeof window.jQuery === "function" ? window.jQuery.fn.jquery : null,
        migrate:
          typeof window.jQuery !== "undefined"
            ? window.jQuery.migrateVersion || null
            : null,
        ui:
          typeof window.jQuery !== "undefined" &&
          window.jQuery.ui &&
          window.jQuery.ui.version
            ? window.jQuery.ui.version
            : null,
      }));

      expect(versions.jquery, "jQuery core missing on dashboard").not.toBeNull();
      expect(versions.jquery).toMatch(/^4\./);
      expect(versions.migrate, "jQuery Migrate missing on dashboard").not.toBeNull();
      expect(versions.migrate).toMatch(/^4\./);
      expect(versions.ui, "jQuery UI missing on dashboard").not.toBeNull();
      expect(versions.ui).toMatch(/^1\.14\./);

      expect(
        failedRequests,
        `failed jslib requests:\n${failedRequests.join("\n")}`,
      ).toEqual([]);
      expect(
        pageErrors,
        `page errors:\n${pageErrors.join("\n")}`,
      ).toEqual([]);
    },
  );

  test(
    "npm trio resolves, vendored versioned copies are gone @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      await loginAsAdmin(page);

      for (const asset of TRIO) {
        const resp = await page.request.get(`${BASE_URL}${asset}`);
        expect(resp.status(), `expected 200 for ${asset}`).toBe(200);
      }
      for (const asset of REMOVED_VENDORED) {
        const resp = await page.request.get(`${BASE_URL}${asset}`);
        expect(
          resp.status(),
          `expected 404 for removed vendored ${asset}`,
        ).toBe(404);
      }
    },
  );
});
