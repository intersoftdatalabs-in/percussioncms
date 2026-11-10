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
 * Classic-UI jQuery plugin cluster served from npm (issue #5663).
 *
 * <p>Tags: {@code @classic-ui} {@code @ui}</p>
 *
 * <p>Run (QA mode after {@code perc-devctl qa-up}):
 * {@code npm run test:surface -- --path tests/classic-jquery-plugins-npm.spec.js}</p>
 *
 * <p>Covers the vendored-copy removal: classic pages that include
 * {@code cm/app/includes/common_js.jsp} / {@code common_css.jsp} (dashboard,
 * adminWorkflow, webmgt, …) must load the plugin cluster from the
 * npm-backed files (form 4.3.0, validation 1.22.x, jeditable 2.0.x,
 * superfish 1.7.10, timeago 1.6.7, multiselect 3.0.x, hoverintent 2.2.x —
 * Dependabot-tracked in {@code WebUI/src/main/frontend/package.json}), and
 * the deleted dead copies (FixedHeader, standalone print-this, collapser,
 * war/api/lib ba-bbq/slideto/wiggle) must no longer resolve.</p>
 */

const { test, expect } = require("@playwright/test");
const { loginAsAdmin, BASE_URL } = require("./helpers/auth");

const NPM_SERVED = [
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-form/jquery.form.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-validation/jquery.validate.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-validation/additional-methods.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-superfish/js/superfish.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-superfish/js/hoverIntent.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-superfish/css/superfish.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-superfish/css/superfish-navbar.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-superfish/css/superfish-vertical.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-superfish/css/megafish.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-hoverintent/jquery.hoverIntent.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-timeago/jquery.timeago.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-ui-multiselect-widget/jquery.multiselect.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-ui-multiselect-widget/jquery.multiselect.filter.js",
];

const REMOVED_DEAD = [
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables-fixedheader/js/dataTables.fixedHeader.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-datatables-fixedheader/css/fixedHeader.dataTables.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-print-this/printThis.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-collapser/jquery.collapser.js",
  "/Rhythmyx/api/lib/jquery.ba-bbq.min.js",
  "/Rhythmyx/api/lib/jquery.slideto.min.js",
  "/Rhythmyx/api/lib/jquery.wiggle.min.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-validation/localization/messages_de.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-jeditable/jquery.jeditable.datepicker.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-superfish/js/supersubs.js",
];

test.describe("Classic-UI jQuery plugin cluster from npm (issue #5663)", () => {
  test(
    "dashboard loads npm plugin cluster with zero script 404s @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      const pluginErrorStacks = [];
      const failedRequests = [];
      // Scope network-failure tracking to the jslib surface this issue owns.
      // (The dashboard gadget tray also probes never-shipped
      // cm/gadgets/container/* paths — pre-existing, unrelated to #5663.)
      page.on("pageerror", (err) => {
        const stack = String((err && err.stack) || err);
        // Fail only on errors arising from the migrated plugin files. The
        // dashboard itself emits pre-existing cell errors (gadget-tray MIME
        // refusals, a PercDashboard I18N.meesage typo, dashboard.jsp
        // set_label) that the already-merged #5660 dashboard spec fails on
        // identically in this cell — unrelated to the plugin cluster.
        if (
          /plugins\/jquery-(form|validation|jeditable|superfish|hoverintent|timeago|multiselect)/.test(
            stack,
          )
        ) {
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

      const plugins = await page.evaluate(() => ({
        ajaxForm:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.ajaxForm === "function",
        validate:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.validate === "function",
        editable:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.editable === "function",
        superfish:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.superfish === "function",
        hoverIntent:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.hoverIntent === "function",
        multiselect:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.multiselect === "function",
        // Kept-vendored controls: claviska dropdown + Dalman layout stay.
        jqDropdown:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.jqDropdown === "function",
        dataTable:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.DataTable === "function",
      }));

      expect(plugins.ajaxForm, "jquery-form missing on dashboard").toBe(true);
      expect(plugins.validate, "jquery-validation missing on dashboard").toBe(
        true,
      );
      expect(plugins.editable, "jeditable missing on dashboard").toBe(true);
      expect(plugins.superfish, "superfish missing on dashboard").toBe(true);
      expect(plugins.hoverIntent, "hoverIntent missing on dashboard").toBe(
        true,
      );
      expect(plugins.multiselect, "multiselect missing on dashboard").toBe(
        true,
      );
      expect(plugins.jqDropdown, "kept dropdown regressed").toBe(true);
      expect(plugins.dataTable, "kept DataTables regressed").toBe(true);

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
    "npm plugin cluster resolves, dead copies are gone @ui",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(60_000);
      await loginAsAdmin(page);

      for (const asset of NPM_SERVED) {
        const resp = await page.request.get(`${BASE_URL}${asset}`);
        expect(resp.status(), `expected 200 for ${asset}`).toBe(200);
      }
      for (const asset of REMOVED_DEAD) {
        const resp = await page.request.get(`${BASE_URL}${asset}`);
        expect(
          resp.status(),
          `expected 404 for removed dead copy ${asset}`,
        ).toBe(404);
      }
    },
  );
});
