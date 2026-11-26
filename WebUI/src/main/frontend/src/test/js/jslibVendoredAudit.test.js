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
 * | retiredjs/jquery.tooltip.js + .css | 2006-2008 Zaefferer | MIT/GPL | common_js.jsp + shared-common | none equivalent | #7178 |
 * | retiredjs/jquery.autocomplete.js + .css | 2007 bassistance | MIT/GPL | common_js.jsp + shared-common | none equivalent | #7178 |
 * | retiredjs/jquery.metadata.js | 2006-era Resig et al | MIT/GPL | shared-common.js | none | #7178 |
 * | retiredjs/jquery.text-overflow.js | v0.7.4 Bram Stein | BSD | common_js.jsp + shared-common.js | none | #7178 |
 * | retiredjs/jquery.caret.js | 2010 C.F. Wong | MIT | common_js.jsp + shared-common.js | none equivalent | #7178 |
 * | retiredjs/jquery.print-this.js | UNKNOWN (no banner anywhere) | UNSTATED | perc_admin pack + PercSiteSummaryDialog | none | #7178 |
 * | retiredjs/timepicker.js | 0.2.1 Milesich 2009 (copyright, no grant) | UNSTATED | common_js.jsp; Schedule/Finder/Page views | partial (different lineage) | #7179 |
 * | retiredjs/datepicker.js | UNKNOWN (@VERSION never built) | MIT (jQuery Fdn header) | NONE (dead) | n/a (UI npm #5660) | #7179 |
 * | libraries/perc-retiredjs/date.js | v1.02 Matt Kruse | MIT/GPL | NONE (dead; moment npm #5665) | n/a | #7179 |
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
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.tooltip.js`, /Jorn Zaef|Jörn Zaef/, /MIT and GPL/, 7178],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.autocomplete.js`, /Dylan Verheul/, /MIT and GPL/, 7178],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.metadata.js`, /John Resig/, /MIT and GPL/, 7178],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.text-overflow.js`, /Bram Stein/, /BSD/, 7178],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.caret.js`, /C\. F\., Wong/, /MIT License/, 7178],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.print-this.js`, null, null, 7178],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/timepicker.js`, /jQuery UI Timepicker 0\.2\.1/, null, 7179],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/datepicker.js`, /@VERSION/, /MIT license/, 7179],
  [`${JSLIB}/libraries/perc-retiredjs/date.js`, /\$VERSION = 1\.02/, /Dual licensed/, 7179],
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

/** Provably dead: zero references outside their own vendored copies. */
const DEAD = [
  {
    rel: `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/datepicker.js`,
    needle: "retiredjs/datepicker.js",
  },
  {
    rel: `${JSLIB}/libraries/perc-retiredjs/date.js`,
    needle: "perc-retiredjs/date.js",
  },
];

/** Direct script loads in common_js.jsp that must survive until migration. */
const DIRECT_LOADS = [
  "jquery-perc-retiredjs/jquery.blockUI.js",
  "jquery-perc-retiredjs/jquery.caret.js",
  "jquery-dropdown/jquery.dropdown.js",
  "jquery-perc-retiredjs/jquery.text-overflow.js",
  "jquery-perc-retiredjs/jquery.metadata.js",
  "jquery-perc-retiredjs/jquery.deparam.js",
  "jquery-perc-retiredjs/jquery.cookie.js",
  "jquery-perc-retiredjs/jquery.tooltip.js",
  "jquery-perc-retiredjs/jquery.autocomplete.js",
  "jquery-perc-retiredjs/timepicker.js",
  // #7181 carousel shim (first-party replacement for tools.scrollable).
  "jquery-percutils/jquery.percscrollable.js",
];

describe("jslib vendored-JS slice-1 audit (issue #7165)", () => {
  it("pins every third-party row with version + license banners", () => {
    expect(THIRD_PARTY.length).toBeGreaterThan(0);
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

  it("dead files stay unreferenced outside vendored copies", () => {
    const roots = [WEBAPP_CM, MINIFY_DIR];
    const skipDir = (p) =>
      p.split(path.sep).includes("jslib") ||
      p.split(path.sep).includes("vendor");
    for (const { rel, needle } of DEAD) {
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
              if (content.includes(needle)) hits.push(full);
            }
          }
        };
        walk(root);
      }
      expect(hits, `dead file ${base} gained consumers`).toEqual([]);
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
