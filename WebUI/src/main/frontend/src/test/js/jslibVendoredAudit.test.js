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
 * availability, and follow-on migration issue. The same files are mirrored
 * under `cm/vendor/js/legacy/` and `WebUI/war/jslib/` (presence pinned;
 * content drift between the mirrors is recorded in #7181).
 *
 * Inventory (canonical path under cm/jslib):
 *
 * | path | version | license | consumer | npm | migration |
 * |------|---------|---------|----------|-----|-----------|
 * | jquery-layout/jquery.layout_and_plugins.js | 1.6.3 (2018, Dalman) | MIT/GPL | common_js.jsp + shared-common.js | none (name taken) | #7175 |
 * | jquery-layout/jquery.layout_customresizer.js | unstated (no banner; Percussion glue) | UNSTATED | same as layout | none | #7175 |
 * | jquery-layout/layout-default.css | 1.6.3-era | MIT/GPL | none found (possibly dynamic) | none | #7175 |
 * | jquery-dropdown/jquery.dropdown.js + .css | 2013 claviska, no stamp | MIT | common_js.jsp + shared-common | none (name taken) | #7176 |
 * | retiredjs/jquery.blockUI.js | 2.70.0 malsup | MIT/GPL | common_js.jsp + shared-common.js | block-ui@2.70.1 | #7177 |
 * | retiredjs/jquery.cookie.js | UNKNOWN (bannerless rewrite) | UNKNOWN | common_js.jsp + shared-common.js | jquery.cookie/js-cookie | #7177 |
 * | retiredjs/jquery.tooltip.js + .css | 2006-2008 Zaefferer | MIT/GPL | common_js.jsp + shared-common | none equivalent | #7178 |
 * | retiredjs/jquery.autocomplete.js + .css | 2007 bassistance | MIT/GPL | common_js.jsp + shared-common | none equivalent | #7178 |
 * | retiredjs/jquery.metadata.js | 2006-era Resig et al | MIT/GPL | shared-common.js | none | #7178 |
 * | retiredjs/jquery.text-overflow.js | v0.7.4 Bram Stein | BSD | common_js.jsp + shared-common.js | none | #7178 |
 * | retiredjs/jquery.caret.js | 2010 C.F. Wong | MIT | common_js.jsp + shared-common.js | none equivalent | #7178 |
 * | retiredjs/jquery.print-this.js | UNKNOWN (no banner anywhere) | UNSTATED | perc_admin pack + PercSiteSummaryDialog | none | #7178 |
 * | retiredjs/timepicker.js | 0.2.1 Milesich 2009 (copyright, no grant) | UNSTATED | common_js.jsp; Schedule/Finder/Page views | partial (different lineage) | #7179 |
 * | retiredjs/datepicker.js | UNKNOWN (@VERSION never built) | MIT (jQuery Fdn header) | NONE (dead) | n/a (UI npm #5660) | #7179 |
 * | libraries/perc-retiredjs/date.js | v1.02 Matt Kruse | MIT/GPL | NONE (dead; moment npm #5665) | n/a | #7179 |
 * | retiredjs/jquery.jsonp.js | 2012 Aubourg | MIT | common-ui-bundle | none | #7180 |
 * | retiredjs/jquery.jfeed.js | 2007 Hovinne | MIT/GPL | common-ui-bundle | none | #7180 |
 * | retiredjs/jquery.xmldom-1.0.js + libraries duplicate | v1.0 2009 Out West | MIT/GPL | webmgt/editTemplate packs + JSP tags | none | #7180 |
 * | retiredjs/jquery.jmodal.js | v2.0 2009 Jericho | GPL-ONLY | webmgt/editTemplate packs + JSP tags | none | #7181 |
 * | retiredjs/tools.scrollable-1.1.2.js + mousewheel | 2009 Piirainen | MIT/GPL-2+ | common_js.jsp + shared-common.js | none (holder) | #7181 |
 * | libraries/perc-retiredjs/rAF.js | 2011 Moller polyfill | snippet/PD | perc_editTemplate pack (droppable) | n/a (native) | #7181 |
 * | jquery-testrunner/testrunner.js | 2009 QUnit-era | MIT/GPL | manual testing pages only | n/a (QUnit npm #6068) | #7181 |
 * | retiredjs/json2.js | 2017-06-12 Crockford | Public Domain | manual testing pages only | n/a (native JSON) | #7181 |
 *
 * First-party (no migration): jquery-percutils/jquery.percutils.js and
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
  [`${JSLIB}/jquery/plugins/jquery-layout/jquery.layout_and_plugins.js`, /jquery\.layout 1\.6\.3/, /Dual licensed/, 7175],
  // No banner: Percussion resize glue over $.layout (provenance confirmed in #7175).
  [`${JSLIB}/jquery/plugins/jquery-layout/jquery.layout_customresizer.js`, /resizeJQuery/, null, 7175],
  [`${JSLIB}/jquery/plugins/jquery-dropdown/jquery.dropdown.js`, /jQuery Dropdown/, /MIT license/, 7176],
  [`${JSLIB}/jquery/plugins/jquery-dropdown/jquery.dropdown.css`, /Cory LaViska/, /MIT license/, 7176],
  [`${JSLIB}/jquery/plugins/jquery-layout/layout-default.css`, /Fabrizio Balliano/, /Dual licensed/, 7175],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.blockUI.js`, /Version 2\.70/, /MIT/, 7177],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.cookie.js`, null, null, 7177],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.tooltip.js`, /Jorn Zaef|Jörn Zaef/, /MIT and GPL/, 7178],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.autocomplete.js`, /Dylan Verheul/, /MIT and GPL/, 7178],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.metadata.js`, /John Resig/, /MIT and GPL/, 7178],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.text-overflow.js`, /Bram Stein/, /BSD/, 7178],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.caret.js`, /C\. F\., Wong/, /MIT License/, 7178],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.print-this.js`, null, null, 7178],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/timepicker.js`, /jQuery UI Timepicker 0\.2\.1/, null, 7179],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/datepicker.js`, /@VERSION/, /MIT license/, 7179],
  [`${JSLIB}/libraries/perc-retiredjs/date.js`, /\$VERSION = 1\.02/, /Dual licensed/, 7179],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.jsonp.js`, /jaubourg/i, /MIT License/, 7180],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.jfeed.js`, /Hovinne/, /MIT/, 7180],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.xmldom-1.0.js`, /xmlDOM Plugin v1\.0/, /MIT and GPL/, 7180],
  [`${JSLIB}/libraries/perc-retiredjs/jquery.xmldom-1.0.js`, /xmlDOM Plugin v1\.0/, /MIT and GPL/, 7180],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.jmodal.js`, /version: 2\.0/, /Licensed under the GPL/, 7181],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/tools.scrollable-1.1.2.js`, /Tero Piirainen/, /MIT and GPL/, 7181],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/tools.scrollable.mousewheel-1.0.1.js`, /Tero Piirainen/, /MIT and GPL/, 7181],
  [`${JSLIB}/libraries/perc-retiredjs/rAF.js`, /requestAnimationFrame/, null, 7181],
  [`${JSLIB}/jquery/plugins/jquery-testrunner/testrunner.js`, /John Resig/, /MIT/, 7181],
  [`${JSLIB}/jquery/plugins/jquery-perc-retiredjs/json2.js`, /json2\.js/, /Public Domain/, 7181],
];

const FIRST_PARTY = [
  `${JSLIB}/jquery/plugins/jquery-percutils/jquery.percutils.js`,
  `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.deparam.js`,
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
  "jquery-layout/jquery.layout_and_plugins.js",
  "jquery-layout/jquery.layout_customresizer.js",
  "jquery-perc-retiredjs/jquery.metadata.js",
  "jquery-perc-retiredjs/jquery.deparam.js",
  "jquery-perc-retiredjs/jquery.cookie.js",
  "jquery-perc-retiredjs/jquery.tooltip.js",
  "jquery-perc-retiredjs/jquery.autocomplete.js",
  "jquery-perc-retiredjs/timepicker.js",
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

  it("keeps the GPL-only jmodal grant visible (legal flag, #7181)", () => {
    const head = readHead(
      path.join(
        WEBAPP_CM,
        `${JSLIB}/jquery/plugins/jquery-perc-retiredjs/jquery.jmodal.js`,
      ),
    );
    expect(head).toMatch(/Licensed under the GPL/);
    expect(head).not.toMatch(/MIT|Apache/i);
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

  it("mirrors carry the audited set (triple-tree reality, drift in #7181)", () => {
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
