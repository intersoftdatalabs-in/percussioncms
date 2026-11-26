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
 * superfish 1.7.10, timeago 1.6.7, multiselect 3.0.x, hoverintent 2.2.x,
 * claviska dropdown 2.0.3 (issue #7176), malsup blockUI 2.70.1 +
 * Klaus Hartl cookie 1.4.1 (issue #7177), jasonday printThis 2.0.0
 * (issue #7178) —
 * Dependabot-tracked in {@code WebUI/src/main/frontend/package.json}), and
 * the deleted dead copies (FixedHeader, standalone print-this, collapser,
 * war/api/lib ba-bbq/slideto/wiggle, retiredjs tooltip/autocomplete/
 * metadata/text-overflow/caret (issue #7178)) must no longer resolve.
 * Autocomplete widgets run from the already-loaded jQuery UI bundle, so no
 * per-URL autocomplete file remains.</p>
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
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-dropdown/jquery.dropdown.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-dropdown/jquery.dropdown.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-dropdown/jquery.dropdown.css",
  // printThis is npm-served since #7178 (print-this 2.0.0, jasonday
  // lineage, same stable retiredjs URL). Autocomplete is NOT served: its
  // stable URL 404s (see REMOVED_DEAD) — the widget ships inside the
  // already-loaded jQuery UI bundle, so no per-URL file remains.
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.cookie.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.print-this.js",
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
  // Retired outright in #7178 (dead upstreams, no npm equivalent): the
  // files are deleted and the tags/bundle entries dropped, so these stable
  // URLs must 404. Autocomplete joins them: its widget ships inside the
  // already-loaded jQuery UI bundle, so no per-URL file remains (the
  // tag-list editor loads jquery-ui.min.js instead).
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.tooltip.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.tooltip.css",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.metadata.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.text-overflow.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.caret.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.autocomplete.js",
  "/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.autocomplete.css",
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
          /plugins\/jquery-(form|validation|jeditable|superfish|hoverintent|timeago|multiselect|dropdown|perc-retiredjs)/.test(
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
        // Dropdown is npm-served since #7176 (@claviska/jquery-dropdown
        // 2.0.3 at the same stable URL); blockUI + cookie are npm-served
        // since #7177 (block-ui 2.70.1 + jquery.cookie 1.4.1 at the same
        // stable retiredjs URLs); Dalman layout stays keep-vendored.
        jqDropdown:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.jqDropdown === "function",
        blockUI:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.blockUI === "function" &&
          typeof window.jQuery.unblockUI === "function",
        cookie:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.cookie === "function",
        dataTable:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.fn.DataTable === "function",
        // Issue #7178: jQuery UI 1.14 owns $.fn.tooltip/$.fn.autocomplete
        // now (bassistance files deleted); the plugin namespaces are gone.
        uiTooltip:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.ui === "object" &&
          typeof window.jQuery.ui.tooltip === "function",
        uiAutocomplete:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.ui === "object" &&
          typeof window.jQuery.ui.autocomplete === "function",
        bassistanceGone:
          typeof window.jQuery === "function" &&
          typeof window.jQuery.tooltip === "undefined" &&
          typeof window.jQuery.Autocompleter === "undefined" &&
          typeof window.jQuery.fn.caret === "undefined" &&
          typeof window.jQuery.fn.textOverflow === "undefined" &&
          typeof window.jQuery.metadata === "undefined",
        // Issue #7178 constraint: Bootstrap 5.3 re-registers its jQuery
        // plugins on DOMContentLoaded, after every synchronous script
        // (including jQuery UI), so $.fn.tooltip is Bootstrap's on classic
        // pages. Tooltips must use the Bootstrap API (as the ported call
        // sites do); a $.fn.tooltip widget call would silently no-op.
        fnTooltipIsBootstrap:
          typeof window.jQuery === "function" &&
          /getOrCreateInstance/.test(
            String(window.jQuery.fn.tooltip || ""),
          ),
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
      expect(plugins.jqDropdown, "dropdown regressed (npm-served #7176)").toBe(
        true,
      );
      expect(plugins.blockUI, "blockUI regressed (npm-served #7177)").toBe(
        true,
      );
      expect(plugins.cookie, "cookie regressed (npm-served #7177)").toBe(true);
      expect(plugins.dataTable, "kept DataTables regressed").toBe(true);
      expect(
        plugins.uiTooltip,
        "jQuery UI tooltip missing (bassistance retired #7178)",
      ).toBe(true);
      expect(
        plugins.uiAutocomplete,
        "jQuery UI autocomplete missing (bassistance retired #7178)",
      ).toBe(true);
      expect(
        plugins.bassistanceGone,
        "bassistance/caret/textOverflow/metadata namespace resurrected (#7178)",
      ).toBe(true);
      expect(
        plugins.fnTooltipIsBootstrap,
        "expected Bootstrap-owned $.fn.tooltip (jQUI widget cannot survive DOMContentLoaded re-registration)",
      ).toBe(true);

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
    "bassistance retirement: jQUI tooltip/autocomplete live, printThis served (#7178)",
    { tag: ["@classic-ui", "@ui"] },
    async ({ page }) => {
      test.setTimeout(120_000);
      await loginAsAdmin(page);
      await page.goto(`${BASE_URL}/Rhythmyx/cm/app/dashboard.jsp`, {
        waitUntil: "networkidle",
      });

      // Finder-style tooltip through the ported Bootstrap 5 option shape
      // (new bootstrap.Tooltip with html title/delay/placement/offset/
      // customClass — see PercFinderView).
      const tooltip = await page.evaluate(() => {
        const $ = window.jQuery;
        const anchor = $(
          "<a style='float: right;' href='javascript:void(0);' class='perc-7178-probe'><span>x</span></a>",
        );
        $("body").append(anchor);
        new window.bootstrap.Tooltip(anchor[0], {
          html: true,
          title: "<p class='perc-comment-tip'>probe tip</p>",
          delay: { show: 50, hide: 100 },
          placement: "left",
          offset: [25, 150],
          customClass: "perc-7178-probe-tip",
        });
        anchor
          .find("span")
          .each((_, el) =>
            el.dispatchEvent(new MouseEvent("mouseover", { bubbles: true })),
          );
        return new Promise((resolve) => {
          setTimeout(() => {
            const tip = $(".tooltip.show.perc-7178-probe-tip");
            resolve({
              bubbles: tip.length,
              text: tip.text(),
              margin: tip.css("margin-right"),
              background: tip.css("background-color"),
            });
          }, 500);
        });
      });
      expect(tooltip.bubbles, "Bootstrap tooltip bubble never opened").toBe(1);
      expect(tooltip.text).toContain("probe tip");

      // List-editor-style autocomplete (source/minLength/search/select).
      const autocomplete = await page.evaluate(() => {
        const $ = window.jQuery;
        $("body").append('<input id="perc-7178-ac" type="text" />');
        const input = $("#perc-7178-ac");
        let selected = null;
        input
          .autocomplete({ source: ["Alpha", "Beta"], minLength: 0 })
          .on("focus", () => input.autocomplete("search", ""))
          .on("autocompleteselect", (event, ui) => {
            input.val(ui.item.value);
            selected = ui.item.value;
          });
        input.trigger("focus");
        const items = $(".ui-autocomplete .ui-menu-item")
          .map((_, li) => $(li).text())
          .get();
        const menuBackground = $(".ui-autocomplete").css("background-color");
        $(".ui-autocomplete .ui-menu-item").first().trigger("click");
        return { items, selected, value: input.val(), menuBackground };
      });
      expect(autocomplete.items).toEqual(["Alpha", "Beta"]);
      expect(autocomplete.selected, "autocompleteselect did not fire").toBe(
        "Alpha",
      );
      expect(autocomplete.value, "selection not inserted before select").toBe(
        "Alpha",
      );
      // styles.css carries the retired autocomplete.css dropdown look.
      expect(autocomplete.menuBackground).toBe("rgb(255, 255, 255)");

      // printThis 2.0.0 served from the stable retiredjs URL still exposes
      // the zero-options call shape PercSiteSummaryDialog uses.
      await page.addScriptTag({
        url: `${BASE_URL}/Rhythmyx/cm/jslib/profiles/3x/jquery/plugins/jquery-perc-retiredjs/jquery.print-this.js`,
      });
      const printThis = await page.evaluate(() => ({
        fn: typeof window.jQuery.fn.printThis,
        printContainer: window.jQuery.fn.printThis.defaults.printContainer,
        importCSS: window.jQuery.fn.printThis.defaults.importCSS,
      }));
      expect(printThis.fn).toBe("function");
      expect(printThis.printContainer).toBe(true);
      expect(printThis.importCSS).toBe(true);
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
