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
 * DataTables dead styling removal + keep-vendored disposition (issue #6002).
 *
 * <p>Tags: {@code @classic-ui} {@code @ui}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/classic-datatables-prune.spec.js}</p>
 *
 * <p>Covers the #6002 slice: the unreferenced DataTables 1.12.1 styling
 * integrations (bootstrap/bootstrap4/bootstrap5/bulma/foundation/semanticui/
 * jqueryui + dataTables.dataTables CSS+JS) and sort_*.png icons were deleted
 * from the jslib tree, so classic pages must still load the live core
 * ({@code jquery.dataTables.js} + {@code jquery.dataTables.css} via
 * {@code common_js.jsp} / {@code common_css.jsp}) with {@code $.fn.DataTable}
 * present and zero jslib failures, while every pruned styling URL resolves
 * to 404. The DataTables 1.12.1 core itself stays vendored (the npm
 * datatables.net 3.x line removed the legacy fn* instance API, so the core
 * swap needs a call-site rewrite with live grid parity — deferred).</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const KEPT_SERVED = [
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/jquery.dataTables.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/css/jquery.dataTables.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-layout/jquery.layout_and_plugins.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-layout/jquery.layout_customresizer.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-dropdown/jquery.dropdown.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-dropdown/jquery.dropdown.css",
];

const PRUNED_404 = [
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/css/dataTables.bootstrap.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/css/dataTables.bootstrap4.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/css/dataTables.bootstrap5.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/css/dataTables.bulma.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/css/dataTables.dataTables.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/css/dataTables.foundation.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/css/dataTables.jqueryui.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/css/dataTables.semanticui.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/dataTables.bootstrap.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/dataTables.bootstrap4.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/dataTables.bootstrap5.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/dataTables.bulma.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/dataTables.dataTables.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/dataTables.foundation.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/dataTables.jqueryui.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/js/dataTables.semanticui.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/images/sort_asc.png",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/images/sort_asc_disabled.png",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/images/sort_both.png",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/images/sort_desc.png",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables/images/sort_desc_disabled.png",
];

test.describe("DataTables prune + keep-vendored (issue #6002)", () => {
  test(
    "dashboard keeps DataTables core with zero jslib failures @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const pluginErrorStacks = [];
      const failedRequests = [];
      page.on("pageerror", (err) => {
        const stack = String((err && err.stack) || err);
        if (/plugins\/jquery-(datatables|layout|dropdown)/.test(stack)) {
          pluginErrorStacks.push(stack);
        }
      });
      page.on("console", (msg) => {
        if (
          msg.type() === "error" &&
          msg.text().includes("/cm/jslib/profiles/3x/jquery/plugins/jquery-")
        ) {
          pluginErrorStacks.push(msg.text());
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

      const grid = await page.evaluate(() => ({
        dataTable:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.DataTable === "function",
        jqDropdown:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.jqDropdown === "function",
      }));

      expect(grid.dataTable, "kept DataTables core regressed").toBe(true);
      expect(grid.jqDropdown, "kept dropdown regressed").toBe(true);
      expect(
        failedRequests,
        `failed jslib requests:\n${failedRequests.join("\n")}`,
      ).toEqual([]);
      expect(
        pluginErrorStacks,
        `plugin JS errors:\n${pluginErrorStacks.join("\n")}`,
      ).toEqual([]);
    },
  );

  test(
    "pruned DT styling is gone, kept files resolve @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      await loginAsAdmin(page);

      for (const asset of KEPT_SERVED) {
        const resp = await page.request.get(`${BASE_URL}${asset}`);
        expect(resp.status(), `expected 200 for ${asset}`).toBe(200);
      }
      for (const asset of PRUNED_404) {
        const resp = await page.request.get(`${BASE_URL}${asset}`);
        expect(
          resp.status(),
          `expected 404 for pruned styling copy ${asset}`,
        ).toBe(404);
      }
    },
  );
});
