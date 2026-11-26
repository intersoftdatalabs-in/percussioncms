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
 * Slice-1 audit of vendored JS in the WebUI jslib/cm trees (issue #7165,
 * parent audit #5627).
 *
 * Every third-party `.js`/`.css` asset under
 * `src/main/webapp/cm/jslib/**` that is NOT resolved from the npm graph
 * (no `NPM_LIBRARY_MAPPINGS` entry in `scripts/build-legacy-bundles.cjs`)
 * is pinned below with its version banner, license marker, consumer, npm
 * availability, and follow-on migration issue. Migrated assets leave this
 * list: jquery-dropdown (#7176, now `@claviska/jquery-dropdown` 2.0.3 via
 * the generated-webui overlay) is pinned in `buildLegacyBundles.test.js`
 * instead. The same files are mirrored
 * under `cm/vendor/js/legacy/` and `WebUI/war/jslib/` (presence pinned;
 * broader content drift between the mirrors is recorded in #7181 and
 * owned by a follow-on).
 *
 * Inventory (canonical path under cm/jslib):
 *
 * | path | version | license | consumer | npm | migration |
 * |------|---------|---------|----------|-----|-----------|
 * | jquery-layout/jquery.layout_and_plugins.js | 1.6.3 (2018, Dalman) | MIT/GPL | REMOVED #7175 (zero callers; dead upstream) | none (name taken) |
 * | jquery-layout/jquery.layout_customresizer.js | unstated (no banner; Percussion glue) | UNSTATED | REMOVED #7175 (same) | none |
 * | jquery-layout/layout-default.css | 1.6.3-era | MIT/GPL | REMOVED #7175 (zero refs) | none |
 * | jquery-dropdown/jquery.dropdown.js + .css | MIGRATED #7176: npm @claviska/jquery-dropdown 2.0.3 | MIT (dist banner + pkg) | same stable URLs, npm-backed | @claviska/jquery-dropdown | #7176 DONE |
 * | retiredjs/jquery.blockUI.js | MIGRATED #7177: npm block-ui 2.70.1 (malsup lineage) | MIT/GPL (dist banner + pkg) | same stable URLs, npm-backed | block-ui | #7177 DONE |
 * | retiredjs/jquery.cookie.js | MIGRATED #7177: npm jquery.cookie 1.4.1 (Klaus Hartl lineage) | MIT (dist banner + pkg) | same stable URLs, npm-backed | jquery.cookie | #7177 DONE |
 * | retiredjs/jquery.tooltip.js + .css | REMOVED #7178 (dead upstream; jQuery UI 1.14 tooltip owns $.fn.tooltip, call sites ported) | none | #7178 |
 * | retiredjs/jquery.autocomplete.js + .css | MIGRATED #7178: same-URL npm jQuery UI 1.14 (call sites ported to the widget API) | MIT (dist banner + pkg) | same stable URL, npm-backed | jquery-ui | #7178 DONE |
 * | retiredjs/jquery.metadata.js | REMOVED #7178 (dead upstream; last live call was a no-op temp-object write) | none | #7178 |
 * | retiredjs/jquery.text-overflow.js | REMOVED #7178 (dead upstream; native CSS ellipsis everywhere) | none | #7178 |
 * | retiredjs/jquery.caret.js | REMOVED #7178 (dead upstream; native selectionStart/End port) | none | #7178 |
 * | retiredjs/jquery.print-this.js | MIGRATED #7178: npm print-this 2.0.0 (jasonday lineage) | MIT (dist banner + pkg) | same stable URL, npm-backed | print-this | #7178 DONE |
 * | retiredjs/timepicker.js | RETAINED #7179 (fork decision recorded: Milesich
 * 0.2.1, dead upstream, no same-lineage npm artifact; the only npm
 * candidate jquery-ui-timepicker-addon 1.6.3 is an incompatible lineage —
 * datetimepicker()/timepicker() methods + div.ui-timepicker-div class vs
 * the Milesich datepicker({showTime}) options + #ui-timepicker-div id the
 * live sys_CalendarSimple/Schedule/Finder/Page call sites rely on) | UNSTATED (copyright, no grant) | common_js.jsp; Schedule/Finder/Page views | none (same lineage) | #7179 |
 * | retiredjs/datepicker.js | REMOVED #7179 (UNKNOWN @VERSION stub, never built; zero callers) | n/a (UI npm #5660) | #7179 |
 * | libraries/perc-retiredjs/date.js | REMOVED #7179 (Matt Kruse v1.02; zero API callers, vestigial control descriptors dropped) | n/a (moment npm #5665) | #7179 |
 * | retiredjs/jquery.jsonp.js | 2012 Aubourg | MIT | REMOVED #7180 (zero callers; CORS $.ajax) | none |
 * | retiredjs/jquery.jfeed.js | 2007 Hovinne | MIT/GPL | REMOVED #7180 (native jfeed-shim) | none |
 * | retiredjs/jquery.xmldom-1.0.js + libraries duplicate | v1.0 2009 Out West | MIT/GPL | REMOVED #7180 (first-party percxmldom shim) | none |
 * | retiredjs/jquery.jmodal.js | v2.0 2009 Jericho | GPL-ONLY | REMOVED #7181 (confirm_dialog) | n/a |
 * | retiredjs/tools.scrollable-1.1.2.js + mousewheel | 2009 Piirainen | MIT/GPL-2+ | REMOVED #7181 (percscrollable shim) | n/a |
 * | libraries/perc-retiredjs/rAF.js | 2011 Moller polyfill | snippet/PD | REMOVED #7181 (native rAF) | n/a |
 * | jquery-testrunner/testrunner.js | 2009 QUnit-era | MIT/GPL | REMOVED #7181 (dead 404 refs) | n/a |
 * | retiredjs/json2.js | 2017-06-12 Crockford | Public Domain | REMOVED #7181 (native JSON) | n/a |
 *
 * First-party (no migration): jquery-percutils/jquery.percutils.js,
 * jquery-percutils/jquery.percscrollable.js (the #7181 carousel shim with
 * the retired scrollable's $.fn.scrollable API), and
 * jquery-perc-retiredjs/jquery.deparam.js carry Intersoft headers (the
 * deparam shim is the #5663 BBQ replacement, upstream notice retained).
 *
 * Retained (not on npm, #5665 precedent): Tim Down simpledateformat.js,
 * byte-identical under both library dirs.
 */

import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const FRONTEND_ROOT = path.dirname(
  path.dirname(path.dirname(__dirname)),
);
const WEBAPP_CM = path.join(
  FRONTEND_ROOT,
  "..",
  "webapp",
  "cm",
);
const MINIFY_DIR = path.join(FRONTEND_ROOT, "..", "resources", "minify");
const WAR_ROOT = path.join(FRONTEND_ROOT, "..", "..", "..", "war");
// Packaged content-editor control descriptors (perc-packages): control XSLs
// declare cm/jslib FileDescriptors that render into live editor pages, so
// retired-vendored audits must cover them too (issue #7175 Codex review).
const PACKAGES_ROOT = path.join(
  FRONTEND_ROOT,
  "..",
  "..",
  "..",
  "..",
  "modules",
  "perc-packages",
  "src",
  "main",
  "resources",
  "Packages",
);

function readHead(abs, bytes = 4096) {
  const fd = fs.openSync(abs, "r");
  try {
    const buf = Buffer.alloc(bytes);
    const n = fs.readSync(fd, buf, 0, bytes, 0);
    return buf.subarray(0, n).toString("utf8");
  } finally {
    fs.closeSync(fd);
  }
}

const JSLIB = "jslib/profiles/3x";

/**
 * Third-party rows: [relative path, banner RegExp, license RegExp, migration issue].
 * `licenseRe: null` pins UNKNOWN/UNSTATED provenance: the head must NOT
 * match any license marker, so adding (or silently changing) provenance
 * forces a conscious test update alongside the migration.
 */
const THIRD_PARTY = [
  // #7175 rows retired: jquery-layout (Dalman UI Layout 1.6.3) deleted from
  // all three trees (see RETIRED_7175 below). Dead upstream (npm
  // `jquery-layout` is an unrelated scravy engine), zero in-repo
  // instantiations ($("body").layout only in a broken manual testing page
  // whose jQuery 1.3.2 deps are long gone), layout-default.css had zero
  // references, and the ui-layout-* classes left in markup are inert
  // without the plugin (the CSS only styled plugin-generated
  // resizers/togglers/masks that never existed).
  // #7178 rows retired: tooltip/autocomplete/metadata/text-overflow/caret
  // deleted from all three trees (see RETIRED_7178 below). Dead upstream
  // (Zaefferer 2008, bassistance 2007, Resig 2006, Bram Stein 2011, Wong
  // 2010); tooltips/autocompletes moved to the already-npm-tracked jQuery
  // UI 1.14 dist (#5660; autocomplete has no per-URL file left), metadata's
  // last live call was a no-op temp-object write, text-overflow folded into
  // native CSS ellipsis, caret ported to selectionStart/setSelectionRange.
  // #7179: datepicker.js (never-built @VERSION stub) and Matt Kruse
  // date.js (zero API callers; vestigial sys_CalendarSimple and
  // widget-control FileDescriptors dropped) are deleted from all three
  // trees (see RETIRED_7179 below). The Milesich timepicker stays vendored
  // under the recorded fork decision (see RETAINED_TIMEPICKER_FORK below):
  // dead upstream, no same-lineage npm artifact, and the only npm candidate
  // (Trent Richardson jquery-ui-timepicker-addon 1.6.3) is an incompatible
  // lineage, so it leaves this third-party list.
  // #7180 rows removed: jsonp / jfeed / both xmldom dupes were retired (see
  // RETIRED_7180 below). The full bundle now concatenates the native
  // ../../delivery/common/js/shims/jfeed-shim.js and the slim bundle is just
  // PercServiceUtils (jsonp had zero callers); webmgt/editTemplate packs and
  // JSP tags load the first-party jquery.percxmldom.js shim instead.
];

const FIRST_PARTY = [
  `${JSLIB}/jquery/plugins/jquery-percutils/jquery.percutils.js`,
  `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.deparam.js`,
  // #7181 carousel shim: same $.fn.scrollable API as the retired jTools
  // plugin, first-party maintained.
  `${JSLIB}/jquery/plugins/jquery-percutils/jquery.percscrollable.js`,
  // #7180 xmlDOM shim: same $.xmlDOM API as the retired Out West Media
  // plugin (native DOMParser, no IE ActiveX fallback), first-party
  // maintained.
  `${JSLIB}/jquery/plugins/jquery-percutils/jquery.percxmldom.js`,
];

/** Issue #7180 retired feed/XML cluster: deleted from all three trees. */
const RETIRED_7180 = [
  {
    rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.jsonp.js`,
    needles: ["jquery-perc-retiredjs/jquery.jsonp", "$.jsonp"],
  },
  {
    rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.jfeed.js`,
    needles: ["jquery-perc-retiredjs/jquery.jfeed", "jquery.jfeed"],
  },
  {
    rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.xmldom-1.0.js`,
    needles: ["jquery-perc-retiredjs/jquery.xmldom", "jquery.xmldom-1.0"],
  },
  {
    rel: `${JSLIB}/libraries/perc-retiredjs/jquery.xmldom-1.0.js`,
    needles: ["libraries/perc-retiredjs/jquery.xmldom", "jquery.xmldom-1.0"],
  },
];

/** Issue #7175 retired layout cluster: deleted from all three trees, refs dropped.
 *
 * Dalman UI Layout 1.6.3 is dead upstream (layout.jquery-dev.com unmaintained
 * since ~2018; the npm name `jquery-layout` is an unrelated scravy engine, so
 * no npm swap exists). The plugin was never instantiated in-product: no
 * `.layout(` / `$.layout` / `resizeAll` / `resizeJQuery` callers outside the
 * vendored files (the single `$("body").layout({})` lived in the broken
 * manual testing page `layout_view.html`, whose jQuery 1.3.2 deps are long
 * gone). `layout-default.css` had zero references and only styled
 * plugin-generated resizers/togglers/masks, so the bare `ui-layout-*`
 * classes left in classic markup are inert. Retired outright (#7180/#7181
 * precedent): no first-party shim, because there are zero callers to shim.
 */
const RETIRED_7175 = [
  {
    rel: `${JSLIB}/jquery/plugins/jquery-layout/jquery.layout_and_plugins.js`,
    needles: ["jquery-layout/jquery.layout_and_plugins", "$.layout", "resizeJQuery"],
  },
  {
    rel: `${JSLIB}/jquery/plugins/jquery-layout/jquery.layout_customresizer.js`,
    needles: ["jquery-layout/jquery.layout_customresizer", "resizeJQuery"],
  },
  {
    rel: `${JSLIB}/jquery/plugins/jquery-layout/layout-default.css`,
    needles: ["jquery-layout/layout-default", "layout-default.css"],
  },
];
/** Issue #7181 retired cluster: deleted from all three trees, refs dropped. */
/** Issue #7178 retired bassistance-era form plugins (parent audit #5627).
 *
 * Dead upstream with no npm equivalent (Zaefferer tooltip 2008, bassistance
 * autocomplete 2007, Resig metadata 2006, Bram Stein text-overflow 2011,
 * Wong caret 2010): deleted from all three trees (cm/jslib,
 * cm/vendor/js/legacy, WebUI/war) with the common_js.jsp tags,
 * common_js/minuet bundle entries, common_css.jsp links, dashboard and
 * testing-page tags, and perc-packages control FileDescriptors dropped.
 * Replaced in kind: tooltips/autocompletes moved to the already-npm-tracked
 * jQuery UI 1.14 dist (#5660, call sites ported to the widget API;
 * autocomplete has no per-URL file left, the widget ships inside the
 * already-loaded jQuery UI bundle); metadata's last live call
 * (perc_site_map) was a no-op write to a temp object, removed;
 * text-overflow folded into native CSS ellipsis (same $.PercTextOverflow
 * API); caret ported to selectionStart/setSelectionRange. The sibling
 * print-this is npm-served at the same stable URL instead (pinned in
 * buildLegacyBundles.test.js), so only these six rows assert unreferenced
 * here.
 */
const RETIRED_7178 = [
  {
    rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.tooltip.js`,
    needles: ["jquery-perc-retiredjs/jquery.tooltip"],
  },
  {
    rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.metadata.js`,
    needles: ["jquery-perc-retiredjs/jquery.metadata"],
  },
  {
    rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.text-overflow.js`,
    needles: ["jquery-perc-retiredjs/jquery.text-overflow"],
  },
  {
    rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.caret.js`,
    needles: ["jquery-perc-retiredjs/jquery.caret"],
  },
  {
    rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.autocomplete.js`,
    needles: ["jquery-perc-retiredjs/jquery.autocomplete"],
  },
];

/** Issue #7179 retired date cluster: deleted from all three trees, refs dropped.
 *
 * datepicker.js was a never-built jQuery UI stub (literal `@VERSION` stamp,
 * MIT header but no usable version): zero callers, superseded by the
 * npm-tracked jQuery UI 1.14 dist (#5660). Matt Kruse date.js v1.02 had
 * zero API callers repo-wide (no Date.parseString/Date.isValid/format
 * consumers outside its own copy); the FileDescriptors that declared it in
 * sys_CalendarSimple-adjacent widget controls (percQueryControl, image/page
 * auto-list, categoryList, fileAutoList, tagList, plus the PageAutoList
 * psx_archiveInfo user dependency) were vestigial and are dropped. Neither
 * file is replaced: there is nothing to shim with zero callers.
 */
const RETIRED_7179 = [
  {
    rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/datepicker.js`,
    needles: ["jquery-perc-retiredjs/datepicker"],
  },
  {
    rel: `${JSLIB}/libraries/perc-retiredjs/date.js`,
    needles: ["libraries/perc-retiredjs/date"],
  },
];

/** Issue #7179 fork decision: the live Milesich timepicker stays vendored.
 *
 * jQuery UI Timepicker 0.2.1 (Martin Milesich, 2009, copyright-without-grant
 * = UNSTATED provenance) extends the jQuery UI datepicker with the
 * showTime/stepMinutes/stepHours/altTimeField/time24h options and renders
 * into #ui-timepicker-div — the exact surface the live sys_CalendarSimple
 * control, PercScheduleDialog, PercFinderView, and PercPageView rely on.
 * Upstream is dead (milesich.com long gone) with no same-lineage npm
 * artifact; the only npm candidate, Trent Richardson's
 * jquery-ui-timepicker-addon 1.6.3 (MIT, 2016), is an incompatible lineage:
 * it exposes datetimepicker()/timepicker() methods (no datepicker()
 * showTime passthrough — the option would be silently ignored and the time
 * UI would disappear) and renders div.ui-timepicker-div (a class) instead
 * of the #ui-timepicker-div id the views' z-index handling targets, with
 * differently-named step options and value formats. Serving the addon at
 * the same stable URL would silently break the live schedule/calendar
 * controls, so the file stays vendored at its stable URLs (common_js.jsp
 * tags + common/minuet bundle entries unchanged) until those legacy
 * controls are rewritten or retired. This pin fails loudly on any banner,
 * license-marker, or consumer drift so the decision is revisited
 * consciously.
 */
const RETAINED_TIMEPICKER_FORK = {
  rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/timepicker.js`,
  bannerRe: /jQuery UI Timepicker 0\.2\.1/,
};

/** Issue #7181 retired cluster: deleted from all three trees, refs dropped. */
const RETIRED_7181 = [
  {
    rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.jmodal.js`,
    needles: ["jquery.jmodal", "fn.jmodal", "hideJmodal"],
  },
  {
    rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/tools.scrollable-1.1.2.js`,
    needles: ["tools.scrollable-1.1.2"],
  },
  {
    rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/tools.scrollable.mousewheel-1.0.1.js`,
    needles: ["scrollable.mousewheel"],
  },
  {
    rel: `${JSLIB}/libraries/perc-retiredjs/rAF.js`,
    needles: ["perc-retiredjs/rAF"],
  },
  {
    rel: `${JSLIB}/jquery/plugins/jquery-testrunner/testrunner.js`,
    needles: ["testrunner/testrunner"],
  },
  {
    rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/json2.js`,
    needles: ["retiredjs/json2"],
  },
];

const RETAINED_SIMPLEDATEFORMAT = [
  `${JSLIB}/libraries/momentjs/plugins/moment-jdateformatparser/simpledateformat.js`,
  `${JSLIB}/libraries/perc-retiredjs/simpledateformat.js`,
];


/** Direct script loads in common_js.jsp that must survive until migration. */
const DIRECT_LOADS = [
  "jquery-perc-retiredjs/jquery.blockUI.js",
  "jquery-dropdown/jquery.dropdown.js",
  "jquery-perc-retiredjs/jquery.deparam.js",
  "jquery-perc-retiredjs/jquery.cookie.js",
  "jquery-perc-retiredjs/timepicker.js",
  // jQuery UI owns $.fn.tooltip/$.fn.autocomplete now (issue #7178); the
  // bassistance caret/text-overflow/metadata/tooltip/autocomplete tags are
  // gone from all three common_js.jsp twins.
  // #7181 carousel shim (first-party replacement for tools.scrollable).
  "jquery-percutils/jquery.percscrollable.js",
];

describe("jslib vendored-JS slice-1 audit (issue #7165)", () => {
  it("pins every third-party row with version + license banners", () => {
    // Slice #7179 drains the list: every audited third-party row is now
    // migrated to npm, retained under a recorded decision (timepicker fork,
    // simpledateformat), or retired. The loop below stays as the gate for
    // any row a future slice adds back.
    for (const [rel, bannerRe, licenseRe] of THIRD_PARTY) {
      const abs = path.join(WEBAPP_CM, rel);
      expect(fs.existsSync(abs), `missing audited file: ${rel}`).toBe(true);
      const head = readHead(abs);
      if (bannerRe !== null) {
        expect(head, `${rel} lost its version banner`).toMatch(bannerRe);
      }
      if (licenseRe !== null) {
        expect(head, `${rel} lost its license marker`).toMatch(licenseRe);
      } else {
        // UNKNOWN/UNSTATED provenance pinned: no license grant marker may
        // appear without a conscious test + migration-issue update. (Bare
        // copyright lines without a grant, e.g. timepicker.js, still count
        // as UNSTATED.)
        expect(head, `${rel} gained an unrecorded license marker`).not.toMatch(
          /licen[sc]e|\bmit\b|gpl|apache|public domain/i,
        );
      }
    }
  });

  it("retired #7181 cluster stays deleted and unreferenced (GPL jmodal gone)", () => {
    // The GPL-only jmodal grant must not reappear anywhere in the source
    // tree: the file is deleted and no reference to it (or its $.fn API)
    // may remain. Same for the retired scrollable pair, rAF, testrunner,
    // and json2.
    for (const { rel, needles } of RETIRED_7181) {
      for (const tree of [WEBAPP_CM, WAR_ROOT]) {
        const relNoJslib = rel.replace(/^jslib\//, "");
        const candidates =
          tree === WEBAPP_CM
            ? [
                path.join(tree, rel),
                path.join(tree, "vendor/js/legacy", relNoJslib),
              ]
            : [path.join(tree, rel)];
        for (const abs of candidates) {
          expect(fs.existsSync(abs), `retired file resurrected: ${abs}`).toBe(
            false,
          );
        }
      }
    }
    const roots = [WEBAPP_CM, MINIFY_DIR];
    const skipDir = (p) =>
      p.split(path.sep).includes("jslib") ||
      p.split(path.sep).includes("vendor");
    for (const { rel, needles } of RETIRED_7181) {
      const base = path.basename(rel);
      const hits = [];
      for (const root of roots) {
        const walk = (dir) => {
          for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
            const full = path.join(dir, entry.name);
            if (entry.isDirectory()) {
              if (!skipDir(full)) walk(full);
            } else if (/\.(jsp|html|xls|xsl|js|json|css)$/.test(entry.name)) {
              // The first-party shim documents what it replaces in prose
              // ("retired jQuery Tools Scrollable"); only dotted file/API
              // references count as resurrections.
              if (full.endsWith("jquery.percscrollable.js")) continue;
              const content = fs.readFileSync(full, "utf8");
              for (const needle of needles) {
                if (content.includes(needle)) hits.push(`${full} :: ${needle}`);
              }
            }
          }
        };
        walk(root);
      }
      expect(hits, `retired ${base} still referenced`).toEqual([]);
    }
  });

  it("retired #7180 feed/XML cluster stays deleted and unreferenced (native shims in)", () => {
    // jsonp had zero callers (delivery calls use CORS $.ajax), jfeed is
    // replaced by the native delivery jfeed-shim, and both xmldom dupes are
    // replaced by the first-party jquery.percxmldom.js shim: the vendored
    // files are deleted and no reference to them (or their APIs) may remain
    // outside the shims that document what they replace.
    for (const { rel, needles } of RETIRED_7180) {
      for (const tree of [WEBAPP_CM, WAR_ROOT]) {
        const relNoJslib = rel.replace(/^jslib\//, "");
        const candidates =
          tree === WEBAPP_CM
            ? [
                path.join(tree, rel),
                path.join(tree, "vendor/js/legacy", relNoJslib),
              ]
            : [path.join(tree, rel)];
        for (const abs of candidates) {
          expect(fs.existsSync(abs), `retired file resurrected: ${abs}`).toBe(
            false,
          );
        }
      }
    }
    const roots = [WEBAPP_CM, MINIFY_DIR];
    const skipDir = (p) =>
      p.split(path.sep).includes("jslib") ||
      p.split(path.sep).includes("vendor");
    for (const { rel, needles } of RETIRED_7180) {
      const base = path.basename(rel);
      const hits = [];
      for (const root of roots) {
        const walk = (dir) => {
          for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
            const full = path.join(dir, entry.name);
            if (entry.isDirectory()) {
              if (!skipDir(full)) walk(full);
            } else if (/\.(jsp|html|xls|xsl|js|json|css)$/.test(entry.name)) {
              // The first-party shim documents what it replaces in prose
              // ("retired jQuery xmlDOM Plugin"); only dotted file/API
              // references count as resurrections.
              if (full.endsWith("jquery.percxmldom.js")) continue;
              const content = fs.readFileSync(full, "utf8");
              for (const needle of needles) {
                if (content.includes(needle)) hits.push(`${full} :: ${needle}`);
              }
            }
          }
        };
        walk(root);
      }
      expect(hits, `retired ${base} still referenced`).toEqual([]);
    }
    // The replacement shim ships in all three trees (byte-identical).
    const shim = `${JSLIB}/jquery/plugins/jquery-percutils/jquery.percxmldom.js`;
    const shimNoJslib = shim.replace(/^jslib\//, "");
    const copies = [
      path.join(WEBAPP_CM, shim),
      path.join(WEBAPP_CM, "vendor/js/legacy", shimNoJslib),
      path.join(WAR_ROOT, shim),
    ];
    for (const abs of copies) {
      expect(fs.existsSync(abs), `percxmldom shim missing: ${abs}`).toBe(true);
    }
    expect(fs.readFileSync(copies[0]).equals(fs.readFileSync(copies[1]))).toBe(
      true,
    );
    expect(fs.readFileSync(copies[0]).equals(fs.readFileSync(copies[2]))).toBe(
      true,
    );
  });

  it("retired #7175 layout cluster stays deleted and unreferenced (dead upstream, zero callers)", () => {
    // Dalman UI Layout 1.6.3: dead upstream, never instantiated in-product
    // (see RETIRED_7175). The vendored files are deleted and no reference
    // to them (or the $.layout plugin API) may remain outside the deleted
    // copies. Unlike #7180 there is no first-party shim: zero callers
    // means nothing to shim. (Bare ui-layout-* classes left in classic
    // markup and in the orphan droppable_layout.css are inert: no .layout()
    // instantiation exists to generate panes, resizers, or togglers.)
    for (const { rel, needles } of RETIRED_7175) {
      for (const tree of [WEBAPP_CM, WAR_ROOT]) {
        const relNoJslib = rel.replace(/^jslib\//, "");
        const candidates =
          tree === WEBAPP_CM
            ? [
                path.join(tree, rel),
                path.join(tree, "vendor/js/legacy", relNoJslib),
              ]
            : [path.join(tree, rel)];
        for (const abs of candidates) {
          expect(fs.existsSync(abs), `retired file resurrected: ${abs}`).toBe(
            false,
          );
        }
      }
    }
    // The reference walk covers the packaged control descriptors too:
    // control XSLs emit their FileDescriptors into live editor pages.
    const roots = [WEBAPP_CM, MINIFY_DIR, PACKAGES_ROOT];
    const skipDir = (p) =>
      p.split(path.sep).includes("jslib") ||
      p.split(path.sep).includes("vendor");
    for (const { rel, needles } of RETIRED_7175) {
      const base = path.basename(rel);
      const hits = [];
      for (const root of roots) {
        const walk = (dir) => {
          for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
            const full = path.join(dir, entry.name);
            if (entry.isDirectory()) {
              if (!skipDir(full)) walk(full);
            } else if (/\.(jsp|html|xls|xsl|js|json|css)$/.test(entry.name)) {
              const content = fs.readFileSync(full, "utf8");
              for (const needle of needles) {
                if (content.includes(needle)) hits.push(`${full} :: ${needle}`);
              }
            }
          }
        };
        walk(root);
      }
      expect(hits, `retired ${base} still referenced`).toEqual([]);
    }
  });

  it("retired #7178 bassistance-era form plugins stay deleted and unreferenced", () => {
    // Dead upstream (2006-2011, no npm equivalent): the files are deleted
    // and no reference to them (or their stable jslib URLs) may remain —
    // the common_js.jsp tags, bundle entries, CSS links, and perc-packages
    // control FileDescriptors are all dropped. Tooltips/autocompletes live
    // on as the jQuery UI widgets (call sites ported; no per-URL
    // autocomplete file remains); metadata's last live call was a no-op;
    // text-overflow/caret are native now. The sibling print-this is
    // npm-served at the same URL (pinned in buildLegacyBundles.test.js),
    // so it is not in this list.
    for (const { rel, needles } of RETIRED_7178) {
      for (const tree of [WEBAPP_CM, WAR_ROOT]) {
        const relNoJslib = rel.replace(/^jslib\//, "");
        const candidates =
          tree === WEBAPP_CM
            ? [
                path.join(tree, rel),
                path.join(tree, "vendor/js/legacy", relNoJslib),
              ]
            : [path.join(tree, rel)];
        for (const abs of candidates) {
          expect(fs.existsSync(abs), `retired file resurrected: ${abs}`).toBe(
            false,
          );
        }
      }
    }
    // The reference walk covers the packaged control descriptors too:
    // control XSLs emit their FileDescriptors into live editor pages.
    const roots = [WEBAPP_CM, MINIFY_DIR, PACKAGES_ROOT];
    const skipDir = (p) =>
      p.split(path.sep).includes("jslib") ||
      p.split(path.sep).includes("vendor");
    for (const { rel, needles } of RETIRED_7178) {
      const base = path.basename(rel);
      const hits = [];
      for (const root of roots) {
        const walk = (dir) => {
          for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
            const full = path.join(dir, entry.name);
            if (entry.isDirectory()) {
              if (!skipDir(full)) walk(full);
            } else if (/\.(jsp|html|xls|xsl|js|json|css)$/.test(entry.name)) {
              const content = fs.readFileSync(full, "utf8");
              for (const needle of needles) {
                if (content.includes(needle)) hits.push(`${full} :: ${needle}`);
              }
            }
          }
        };
        walk(root);
      }
      expect(hits, `retired ${base} still referenced`).toEqual([]);
    }
  });

  it("marks first-party shims with the Intersoft header (no migration)", () => {
    for (const rel of FIRST_PARTY) {
      const head = readHead(path.join(WEBAPP_CM, rel));
      expect(head, `${rel} is not marked first-party`).toMatch(
        /Intersoft Data Labs/,
      );
    }
  });

  it("retains the Tim Down simpledateformat twin byte-identical (not on npm)", () => {
    const [a, b] = RETAINED_SIMPLEDATEFORMAT.map((rel) =>
      path.join(WEBAPP_CM, rel),
    );
    expect(fs.existsSync(a)).toBe(true);
    expect(fs.existsSync(b)).toBe(true);
    expect(fs.readFileSync(a).equals(fs.readFileSync(b))).toBe(true);
    expect(readHead(a)).toMatch(/Tim Down/);
  });

  it("retired #7179 date cluster stays deleted and unreferenced", () => {
    // The never-built datepicker.js stub and the caller-less Matt Kruse
    // date.js are deleted from all three trees (cm/jslib,
    // cm/vendor/js/legacy, WebUI/war); the vestigial sys_CalendarSimple
    // datepicker.js descriptor and the widget-control date.js descriptors
    // (+ PageAutoList psx_archiveInfo user dependency) are dropped. Neither
    // file is replaced: zero callers means nothing to shim.
    for (const { rel, needles } of RETIRED_7179) {
      for (const tree of [WEBAPP_CM, WAR_ROOT]) {
        const relNoJslib = rel.replace(/^jslib\//, "");
        const candidates =
          tree === WEBAPP_CM
            ? [
                path.join(tree, rel),
                path.join(tree, "vendor/js/legacy", relNoJslib),
              ]
            : [path.join(tree, rel)];
        for (const abs of candidates) {
          expect(fs.existsSync(abs), `retired file resurrected: ${abs}`).toBe(
            false,
          );
        }
      }
    }
    // The reference walk covers the packaged control descriptors too:
    // control XSLs emit their FileDescriptors into live editor pages.
    const roots = [WEBAPP_CM, MINIFY_DIR, PACKAGES_ROOT];
    const skipDir = (p) =>
      p.split(path.sep).includes("jslib") ||
      p.split(path.sep).includes("vendor");
    for (const { rel, needles } of RETIRED_7179) {
      const base = path.basename(rel);
      const hits = [];
      for (const root of roots) {
        const walk = (dir) => {
          for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
            const full = path.join(dir, entry.name);
            if (entry.isDirectory()) {
              if (!skipDir(full)) walk(full);
            } else if (/\.(jsp|html|xls|xsl|js|json|css)$/.test(entry.name)) {
              const content = fs.readFileSync(full, "utf8");
              for (const needle of needles) {
                if (content.includes(needle)) hits.push(`${full} :: ${needle}`);
              }
            }
          }
        };
        walk(root);
      }
      expect(hits, `retired ${base} still referenced`).toEqual([]);
    }
    // sys_Templates.xsl lives outside the walked roots: the
    // sys_CalendarSimple datepicker.js descriptor must be gone while the
    // live timepicker.js descriptor (same AssociatedFileList) survives.
    const sysTemplates = path.join(
      FRONTEND_ROOT,
      "..",
      "..",
      "..",
      "..",
      "system",
      "cms",
      "content",
      "applications",
      "sys_resources",
      "ApplicationFiles",
      "stylesheets",
      "sys_Templates.xsl",
    );
    const sysContent = fs.readFileSync(sysTemplates, "utf8");
    expect(
      sysContent.includes("jquery-perc-retiredjs/datepicker"),
      "sys_CalendarSimple still declares the retired datepicker.js",
    ).toBe(false);
    expect(
      sysContent.includes("jquery-perc-retiredjs/timepicker.js"),
      "sys_CalendarSimple lost its live timepicker.js descriptor",
    ).toBe(true);
    // The retired stub used to supply $.datepicker on single-field editor
    // hosts (which load only jQuery + migrate); the npm-tracked jQuery UI
    // dist takes its place and must precede the fork that extends it.
    const uiIdx = sysContent.indexOf(
      "jquery/libraries/jquery-ui/jquery-ui.min.js",
    );
    const forkIdx = sysContent.indexOf("jquery-perc-retiredjs/timepicker.js");
    expect(uiIdx, "sys_CalendarSimple lost its jQuery UI descriptor").toBeGreaterThanOrEqual(
      0,
    );
    expect(
      uiIdx < forkIdx,
      "jQuery UI descriptor must precede the timepicker fork it feeds",
    ).toBe(true);
  });

  it("retains the Milesich timepicker under the recorded fork decision (issue #7179)", () => {
    // Dead upstream, no same-lineage npm artifact; the only npm candidate
    // (jquery-ui-timepicker-addon) is an incompatible lineage, so the file
    // stays vendored at its stable URLs. This pin fails loudly on banner,
    // provenance, or consumer drift so the decision is revisited
    // consciously instead of rotting.
    const { rel, bannerRe } = RETAINED_TIMEPICKER_FORK;
    const vendorMirror = path.join(
      WEBAPP_CM,
      "vendor/js/legacy",
      rel.replace(/^jslib\//, ""),
    );
    const copies = [
      path.join(WEBAPP_CM, rel),
      vendorMirror,
      path.join(WAR_ROOT, rel),
    ];
    for (const abs of copies) {
      expect(fs.existsSync(abs), `retained timepicker missing: ${abs}`).toBe(
        true,
      );
    }
    // cm/jslib and its vendor mirror are byte-identical; the war snapshot
    // carries a pre-existing older cut of the same file (mirror drift owned
    // by a follow-on per the #7181 record), so it is pinned by existence +
    // banner only.
    expect(fs.readFileSync(copies[0]).equals(fs.readFileSync(copies[1]))).toBe(
      true,
    );
    for (const abs of copies) {
      expect(readHead(abs), `retained timepicker lost its banner: ${abs}`).toMatch(
        bannerRe,
      );
    }
    // UNSTATED provenance pinned: no license grant may appear without a
    // conscious migration-issue update (bare copyright lines are not a
    // grant). The behavioral parity contract lives in
    // timepickerForkParity.test.js.
    expect(
      readHead(copies[0]),
      "retained timepicker gained an unrecorded license marker",
    ).not.toMatch(/licen[sc]e|\bmit\b|gpl|apache|public domain/i);
    // Live consumers must survive: direct common_js.jsp tags (both twins)
    // and the common/minuet bundle entries.
    for (const tree of ["app", "pages/app"]) {
      const jsp = path.join(WEBAPP_CM, tree, "includes", "common_js.jsp");
      expect(fs.readFileSync(jsp, "utf8")).toContain(
        "jquery-perc-retiredjs/timepicker.js",
      );
    }
    for (const bundle of [
      "common-bundles.json",
      "common-minuet-bundles.json",
    ]) {
      expect(fs.readFileSync(path.join(MINIFY_DIR, bundle), "utf8")).toContain(
        "jquery-perc-retiredjs/timepicker.js",
      );
    }
  });

  it("mirrors carry the audited set (triple-tree reality; #7181 cluster gone everywhere)", () => {
    for (const [rel] of THIRD_PARTY) {
      // The vendor mirror hangs directly under profiles/ (no jslib/ prefix);
      // the war mirror roots its own jslib/.
      const vendorMirror = path.join(
        WEBAPP_CM,
        "vendor/js/legacy",
        rel.replace(/^jslib\//, ""),
      );
      expect(
        fs.existsSync(vendorMirror),
        `vendor mirror missing: ${rel}`,
      ).toBe(true);
      const warMirror = path.join(WAR_ROOT, rel);
      expect(fs.existsSync(warMirror), `war mirror missing: ${rel}`).toBe(
        true,
      );
    }
  });

  it("common_js.jsp still loads the documented direct set", () => {
    for (const tree of ["app", "pages/app"]) {
      const jsp = path.join(
        WEBAPP_CM,
        tree,
        "includes",
        "common_js.jsp",
      );
      const content = fs.readFileSync(jsp, "utf8");
      for (const suffix of DIRECT_LOADS) {
        expect(content, `${tree} common_js.jsp lost ${suffix}`).toContain(
          suffix,
        );
      }
    }
  });
});
