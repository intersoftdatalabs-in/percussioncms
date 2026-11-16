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
 * Parity tests for the #6319 slice-1 widget-payload npm refresh.
 *
 * The directory (`lodash.min.js`) and calendarTwo (`moment.min.js`) widget
 * payloads under `modules/perc-packages/.../Packages` install verbatim onto
 * every CMS and publish to live sites. Slice 1 replaces the vendored
 * lodash 4.14.0 (known prototype-pollution CVE class) and moment 2.13.0
 * with the npm dist builds (lodash 4.18.1 — refreshed from 4.17.21 as a
 * Dependabot #6411 follow-on — and moment 2.31.0), both
 * Dependabot-tracked via the frontend manifests. These tests pin:
 *
 *   1. The payload bytes are byte-identical to the node_modules dist files.
 *   2. The payload bytes carry the expected npm versions.
 *   3. The frontend package.json declares both ranges (npm-tracked).
 *   4. The lodash/moment APIs actually consumed by the widgets
 *      (`_.includes` in perc-directory.js; `moment().isBefore/format`
 *      in percCalendarTwoSnippet.vm) behave identically on the refreshed
 *      builds — drop-in proof for the published widgets.
 *
 * The remaining #6319 rows (FullCalendar 1.x/2.x base, Slick,
 * Foundation, nav hoverIntent, MiniColors, qTip) stay vendored and are
 * tracked in the #6319 leftover residual — this file must grow a parity
 * block each time another row migrates. Slice 6 (issue #6522) migrated
 * the calendarTwo gcal + lang-all companions to the npm
 * fullcalendar@2.9.1 dists below; slice 7 (issue #6403) dropped the dead
 * nav supersubs pair — zero callers — with its percNavBar.xml +
 * archive-manifest references. The rest of the #6522 rows (v1 calendar
 * widget, slick, nav hoverIntent, MiniColors twins, qTip twins,
 * Foundation 4.x) stay vendored pending replace-or-drop decisions or
 * legal sign-off.
 *
 * Slice 3 migrates the directory-widget `list.min.js` to the npm list.js
 * 1.2.0 dist (byte-identical to the payload; Dependabot-tracked via the
 * frontend manifests with an exact 1.2.0 pin because a caret range would
 * float to 1.5.0, whose dist bytes differ). Pinned below: byte-identity
 * of the payload file, the installed dist version, the manifest pin, and
 * the directory-widget List behavior (`new List` + `filter`/`search`
 * over `perc-person-*` value names, as driven by `perc-directory.js`).
 *
 * Slice 2 migrates the nav-widget superfish pair (`superfish.js` preview
 * copy + `superfish.min.js` published copy under
 * `perc.widgets.nav/.../navBar/js`) to the npm superfish 1.7.10 dist —
 * already Dependabot-tracked via the frontend manifests for the classic-UI
 * copies since #6016. The published min copy was already byte-identical to
 * the npm dist; the preview copy is refreshed to the npm dist bytes
 * (upstream Joel Birch banner restored). Pinned below: byte-identity of
 * both payload files, the v1.7.10 banners, the manifest range, and the
 * menu-enhancement behavior the published nav widget consumes
 * (`percNavBarSnippet.vm` renders `ul.sf-menu`; `navbar.js` drives it
 * through `$.fn.superfish`).
 */

import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
import { Script } from "node:vm";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// src/test/js -> frontend root (peer: buildLegacyBundles.test.js), then up
// to the repo root so the perc-packages payloads resolve portably.
const FRONTEND_ROOT = path.dirname(
  path.dirname(path.dirname(__dirname)),
);
const REPO_ROOT = path.join(FRONTEND_ROOT, "..", "..", "..", "..");
const NODE_MODULES = path.join(FRONTEND_ROOT, "node_modules");

const PAYLOADS = [
  {
    // Directory widget dependency (percDirectory.xml: lodash_min_js).
    payload: path.join(
      "modules",
      "perc-packages",
      "src",
      "main",
      "resources",
      "Packages",
      "perc.widget.directory",
      "sys__UserDependency--web_resources",
      "widgets",
      "directory",
      "js",
      "lodash.min.js",
    ),
    npmDist: path.join("lodash", "lodash.min.js"),
    versionRe: /4\.18\.1/,
  },
  {
    // CalendarTwo snippet script tag (percCalendarTwoSnippet.vm).
    payload: path.join(
      "modules",
      "perc-packages",
      "src",
      "main",
      "resources",
      "Packages",
      "perc.widget.calendar",
      "sys__UserDependency--web_resources",
      "widgets",
      "calendarTwo",
      "js",
      "moment.min.js",
    ),
    npmDist: path.join("moment", "min", "moment.min.js"),
    versionRe: /version : 2\.31\.0/,
  },
];

describe("widget-payload npm refresh parity (issue #6319 slice 1)", () => {
  it("payload files exist under the audited Packages tree", () => {
    for (const { payload } of PAYLOADS) {
      expect(
        fs.existsSync(path.join(REPO_ROOT, payload)),
        `missing payload: ${payload}`,
      ).toBe(true);
    }
  });

  it("payload bytes are byte-identical to the npm dist (Dependabot-visible)", () => {
    for (const { payload, npmDist } of PAYLOADS) {
      const shipped = fs.readFileSync(path.join(REPO_ROOT, payload));
      const dist = fs.readFileSync(path.join(NODE_MODULES, npmDist));
      expect(
        shipped.equals(dist),
        `${payload} drifted from node_modules/${npmDist} — refresh from npm`,
      ).toBe(true);
    }
  });

  it("payload bytes carry the expected npm versions", () => {
    for (const { payload, versionRe } of PAYLOADS) {
      const content = fs.readFileSync(path.join(REPO_ROOT, payload), "utf8");
      expect(content, `${payload} version banner mismatch`).toMatch(versionRe);
    }
    const lodash = require(path.join(NODE_MODULES, "lodash", "lodash.min.js"));
    expect(lodash.VERSION).toBe("4.18.1");
  });

  it("frontend package.json tracks both ranges (npm audit visible)", () => {
    const manifest = JSON.parse(
      fs.readFileSync(path.join(FRONTEND_ROOT, "package.json"), "utf8"),
    );
    expect(manifest.dependencies["lodash"]).toBe("^4.18.1");
    expect(manifest.dependencies["moment"]).toBe("^2.31.0");
  });

  it("lodash _.includes behaves for the directory-widget filter pattern", () => {
    const _ = require(path.join(NODE_MODULES, "lodash", "lodash.min.js"));
    // Mirrors perc-directory.js: _.includes(item.values()[...], name) || !name
    expect(_.includes(["Acme", "Globex"], "Acme")).toBe(true);
    expect(_.includes(["Acme", "Globex"], "Initech")).toBe(false);
    expect(_.includes([], "Acme")).toBe(false);
    expect(_.includes("perc-person-org value", "person-org")).toBe(true);
  });

  it("moment isBefore/format behaves for the calendarTwo snippet pattern", () => {
    const moment = require(
      path.join(NODE_MODULES, "moment", "min", "moment.min.js"),
    );
    // Mirrors percCalendarTwoSnippet.vm: moment(event.start).isBefore(date,'day')
    // and moment(event.start).format("MMM"/"D"/time).
    const start = moment("2026-11-14T10:00:00");
    expect(start.isBefore(moment("2026-11-15"), "day")).toBe(true);
    expect(start.isBefore(moment("2026-11-14"), "day")).toBe(false);
    expect(start.format("MMM").toUpperCase()).toBe("NOV");
    expect(start.format("D")).toBe("14");
    expect(start.format("YYYY-MM-DD")).toBe("2026-11-14");
  });
});

const NAV_PAYLOADS = [
  {
    // NavBar widget preview copy (percNavBar.xml: superfish_js_preview).
    payload: path.join(
      "modules",
      "perc-packages",
      "src",
      "main",
      "resources",
      "Packages",
      "perc.widgets.nav",
      "sys__UserDependency--web_resources",
      "widgets",
      "navBar",
      "js",
      "superfish.js",
    ),
    npmDist: path.join("superfish", "dist", "js", "superfish.js"),
    versionRe: /jQuery Superfish Menu Plugin - v1\.7\.10/,
  },
  {
    // NavBar widget published copy (percNavBar.xml: superfish_js).
    payload: path.join(
      "modules",
      "perc-packages",
      "src",
      "main",
      "resources",
      "Packages",
      "perc.widgets.nav",
      "sys__UserDependency--web_resources",
      "widgets",
      "navBar",
      "js",
      "superfish.min.js",
    ),
    npmDist: path.join("superfish", "dist", "js", "superfish.min.js"),
    versionRe: /Superfish Menu Plugin - v1\.7\.10/,
  },
];

function loadSuperfishPayloadAsJqueryPlugin(payloadRelPath) {
  // The npm superfish dist closes over bare `jQuery` and `window`
  // (`...})(jQuery, window)`), so expose the jsdom-bound jQuery globally
  // before evaluating the payload bytes verbatim.
  const $ = require(path.join(NODE_MODULES, "jquery", "dist", "jquery.js"));
  globalThis.jQuery = $;
  window.jQuery = $;
  const source = fs.readFileSync(path.join(REPO_ROOT, payloadRelPath), "utf8");
  window.eval(source);
  return $;
}

function renderNavMenu(document) {
  // Mirrors percNavBarSnippet.vm: the widget renders ul.sf-menu with nested
  // dropdown lists; navbar.js drives it through $.fn.superfish.
  document.body.innerHTML =
    '<ul class="sf-menu" id="test-nav">' +
    '<li><a href="/home">Home</a></li>' +
    '<li><a href="/sites">Sites</a>' +
    '<ul><li><a href="/sites/a">Site A</a></li></ul>' +
    "</li>" +
    "</ul>";
  return document.getElementById("test-nav");
}

describe("widget-payload npm refresh parity (issue #6319 slice 2: superfish)", () => {
  it("nav payload files exist under the audited Packages tree", () => {
    for (const { payload } of NAV_PAYLOADS) {
      expect(
        fs.existsSync(path.join(REPO_ROOT, payload)),
        `missing payload: ${payload}`,
      ).toBe(true);
    }
  });

  it("nav payload bytes are byte-identical to the npm superfish dist", () => {
    for (const { payload, npmDist } of NAV_PAYLOADS) {
      const shipped = fs.readFileSync(path.join(REPO_ROOT, payload));
      const dist = fs.readFileSync(path.join(NODE_MODULES, npmDist));
      expect(
        shipped.equals(dist),
        `${payload} drifted from node_modules/${npmDist} — refresh from npm`,
      ).toBe(true);
    }
  });

  it("nav payload bytes carry the v1.7.10 upstream banner", () => {
    for (const { payload, versionRe } of NAV_PAYLOADS) {
      const content = fs.readFileSync(path.join(REPO_ROOT, payload), "utf8");
      expect(content, `${payload} version banner mismatch`).toMatch(versionRe);
    }
  });

  it('frontend package.json tracks the superfish range (npm audit visible)', () => {
    const manifest = JSON.parse(
      fs.readFileSync(path.join(FRONTEND_ROOT, "package.json"), "utf8"),
    );
    expect(manifest.dependencies["superfish"]).toBe("^1.7.10");
  });

  it.each(NAV_PAYLOADS.map(({ payload }) => payload))(
    "published/preview copy %s enhances the nav menu (disableHI, no hoverIntent)",
    (payloadRelPath) => {
      const $ = loadSuperfishPayloadAsJqueryPlugin(payloadRelPath);
      expect(typeof $.fn.superfish).toBe("function");
      const menu = renderNavMenu(document);
      // disableHI: the payload ships without hoverIntent on the test page —
      // same fallback the widget relies on when hoverIntent is absent.
      $(menu).superfish({ delay: 0, disableHI: true });
      expect(menu.classList.contains("sf-js-enabled")).toBe(true);
      const parentAnchor = menu.querySelector('li > a[href="/sites"]');
      expect(parentAnchor.classList.contains("sf-with-ul")).toBe(true);
      $(menu).superfish("destroy");
      expect(menu.classList.contains("sf-js-enabled")).toBe(false);
      document.body.innerHTML = "";
    },
  );
});

const LIST_PAYLOAD = path.join(
  "modules",
  "perc-packages",
  "src",
  "main",
  "resources",
  "Packages",
  "perc.widget.directory",
  "sys__UserDependency--web_resources",
  "widgets",
  "directory",
  "js",
  "list.min.js",
);
// Directory widget published copy (percDirectory.xml: list_min_js). The
// payload is the npm list.js 1.2.0 dist build, verbatim.
const LIST_NPM_DIST = path.join("list.js", "dist", "list.min.js");

function loadListPayload() {
  // The list.js browserify bundle assigns the global `List` when evaluated
  // in a browser-like scope, so evaluate the payload bytes verbatim (same
  // pattern as the superfish loader above) rather than the npm main entry.
  const source = fs.readFileSync(path.join(REPO_ROOT, LIST_PAYLOAD), "utf8");
  window.eval(source);
  expect(typeof window.List).toBe("function");
  return window.List;
}

function renderDirectoryList(document) {
  // Mirrors perc-directory.js: `new List("percDirectoryList",
  // { valueNames: [...] })` over person rows, then `.filter(...)` /
  // `.search(...)` for the alpha/department filters.
  document.body.innerHTML =
    '<div id="percDirectoryList">' +
    '<input class="search" />' +
    '<ul class="list">' +
    '<li><span class="perc-person-first-name"></span> ' +
    '<span class="perc-person-last-name"></span> ' +
    '<span class="perc-person-org"></span></li>' +
    "</ul>" +
    "</div>";
  return document.getElementById("percDirectoryList");
}

const DIRECTORY_VALUE_NAMES = [
  "perc-person-first-name",
  "perc-person-last-name",
  "perc-person-org",
];

function seedDirectoryList(List, document) {
  renderDirectoryList(document);
  const directoryList = new List("percDirectoryList", {
    valueNames: DIRECTORY_VALUE_NAMES,
  });
  // The markup template row parses as an initial (blank) item; clear it so
  // the list holds exactly the seeded rows (supported clear/add pattern).
  directoryList.clear();
  directoryList.add({
    "perc-person-first-name": "Acme",
    "perc-person-last-name": "Anderson",
    "perc-person-org": "Globex",
  });
  directoryList.add({
    "perc-person-first-name": "Initech",
    "perc-person-last-name": "Bolton",
    "perc-person-org": "Initech",
  });
  directoryList.add({
    "perc-person-first-name": "Umbrella",
    "perc-person-last-name": "Anderson",
    "perc-person-org": "Umbrella",
  });
  return directoryList;
}

describe("widget-payload npm refresh parity (issue #6319 slice 3: list.js)", () => {
  it("directory payload file exists under the audited Packages tree", () => {
    expect(fs.existsSync(path.join(REPO_ROOT, LIST_PAYLOAD))).toBe(true);
  });

  it("directory payload bytes are byte-identical to the npm list.js dist", () => {
    const shipped = fs.readFileSync(path.join(REPO_ROOT, LIST_PAYLOAD));
    const dist = fs.readFileSync(path.join(NODE_MODULES, LIST_NPM_DIST));
    expect(
      shipped.equals(dist),
      `${LIST_PAYLOAD} drifted from node_modules/${LIST_NPM_DIST} — refresh from npm`,
    ).toBe(true);
  });

  it("installed list.js dist is 1.2.0 and payload carries the List marker", () => {
    const installed = JSON.parse(
      fs.readFileSync(
        path.join(NODE_MODULES, "list.js", "package.json"),
        "utf8",
      ),
    );
    expect(installed.version).toBe("1.2.0");
    const content = fs.readFileSync(path.join(REPO_ROOT, LIST_PAYLOAD), "utf8");
    expect(content).toMatch(/valueNames/);
  });

  it('frontend manifests pin list.js to 1.2.0 (npm audit visible)', () => {
    const manifest = JSON.parse(
      fs.readFileSync(path.join(FRONTEND_ROOT, "package.json"), "utf8"),
    );
    // Exact pin (not a caret): the payload is the 1.2.0 dist and a caret
    // range would float to 1.5.0+, whose dist bytes differ.
    expect(manifest.dependencies["list.js"]).toBe("1.2.0");
  });

  it("List constructor indexes person rows with perc-person-* values", () => {
    const List = loadListPayload();
    const directoryList = seedDirectoryList(List, document);
    try {
      expect(directoryList.items).toHaveLength(3);
      const values = directoryList.items[0].values();
      expect(values["perc-person-first-name"]).toBe("Acme");
      expect(values["perc-person-last-name"]).toBe("Anderson");
    } finally {
      document.body.innerHTML = "";
    }
  });

  it("List filter narrows rows like the directory alpha filter", () => {
    const List = loadListPayload();
    const directoryList = seedDirectoryList(List, document);
    try {
      // Mirrors perc-directory.js: keep rows whose last name starts letter.
      directoryList.filter(
        (item) => item.values()["perc-person-last-name"].charAt(0) === "A",
      );
      expect(directoryList.visibleItems).toHaveLength(2);
      directoryList.filter();
      expect(directoryList.visibleItems).toHaveLength(3);
    } finally {
      document.body.innerHTML = "";
    }
  });

  it("List search narrows rows like the directory search box", () => {
    const List = loadListPayload();
    const directoryList = seedDirectoryList(List, document);
    try {
      directoryList.search("Initech");
      expect(directoryList.visibleItems).toHaveLength(1);
      expect(
        directoryList.visibleItems[0].values()["perc-person-first-name"],
      ).toBe("Initech");
      directoryList.search();
      expect(directoryList.visibleItems).toHaveLength(3);
    } finally {
      document.body.innerHTML = "";
    }
  });
});

const FULLCALENDAR_BASE = path.join(
  "modules",
  "perc-packages",
  "src",
  "main",
  "resources",
  "Packages",
  "perc.widget.calendar",
  "sys__UserDependency--web_resources",
  "widgets",
  "calendarTwo",
  "js",
);

// CalendarTwo fullcalendar pair (percCalendarTwo.xml: fullcalendar-js,
// gcal-js, lang-all-js). Slice 5 of #6502 rebases the pair onto the npm
// fullcalendar@2.9.1 dist with the two Percussion mods re-applied (see the
// percCalendarTwoSnippet.vm contract comment): reportEvents resolves the
// per-widget Perc.events deferred, and month-view computeRange reads
// Perc.range instead of the stock hard-coded 6. Byte-identity with the npm
// dist is impossible for a fork, so this block reconstructs the expected
// payload from the installed dist plus the documented patches and pins the
// reconstruction byte-identical — any silent mod loss or drift fails loudly.
const FULLCALENDAR_NONMIN_PATCHES = [
  ["<%= meta.title %> v<%= meta.version %>", "FullCalendar v2.9.1"],
  ["<%= meta.homepage %>", "http://fullcalendar.io/"],
  ["<%= meta.copyright %>", "2016 Adam Shaw"],
  [
    '\tevents = _events;\n\t\trenderEvents();',
    '\tevents = _events;\n\t\tvar target = element[0]["id"];\n' +
      "\t\tPerc.events[target].resolve(_events);\n\t\trenderEvents();",
  ],
  [
    "range.end.add(6 - rowCnt, 'weeks');",
    "range.end.add(Perc.range - rowCnt, 'weeks');",
  ],
];

// The stock .min.js dist already ships the expanded banner; only the two
// mod hunks are re-applied (same mangled scope as the stock min: `y` is
// nested in the Qa calendar constructor, so the element parameter `c` is
// in scope at the reportEvents patch site).
const FULLCALENDAR_MIN_PATCHES = [
  [
    "function y(a){ia=a,v()}",
    "function y(a){ia=a;var target=c[0].id;Perc.events[target].resolve(a),v()}",
  ],
  ['6-b,"weeks")', 'Perc.range-b,"weeks")'],
];

const FULLCALENDAR_PAYLOADS = [
  {
    // Published/preview copy (percCalendarTwo.xml: fullcalendar-js). The
    // non-min dist also carries an unexpanded version:"<%= meta.version %>"
    // wart that ships in fullcalendar@2.9.1 upstream — preserved verbatim.
    payload: path.join(FULLCALENDAR_BASE, "fullcalendar.js"),
    npmDist: path.join("fullcalendar", "dist", "fullcalendar.js"),
    patches: FULLCALENDAR_NONMIN_PATCHES,
  },
  {
    payload: path.join(FULLCALENDAR_BASE, "fullcalendar.min.js"),
    npmDist: path.join("fullcalendar", "dist", "fullcalendar.min.js"),
    patches: FULLCALENDAR_MIN_PATCHES,
  },
];

function rebaseFullcalendarFromNpmDist(npmDistRelPath, patches) {
  // The repo normalizes *.js to LF on commit (.gitattributes `*.js text`),
  // so the reconstruction normalizes the stock dist the same way before
  // patching; the payloads stay LF-verbatim and platform-stable.
  let expected = fs
    .readFileSync(path.join(NODE_MODULES, npmDistRelPath), "utf8")
    .replace(/\r\n/g, "\n");
  for (const [from, to] of patches) {
    const occurrences = expected.split(from).length - 1;
    expect(
      occurrences,
      `rebase patch site not unique in node_modules/${npmDistRelPath}: ${from.slice(0, 60)}`,
    ).toBe(1);
    expected = expected.split(from).join(to);
  }
  return expected;
}

describe("widget-payload npm refresh parity (issue #6502 slice 5: fullcalendar)", () => {
  it("calendarTwo fullcalendar pair exists under the audited Packages tree", () => {
    for (const { payload } of FULLCALENDAR_PAYLOADS) {
      expect(
        fs.existsSync(path.join(REPO_ROOT, payload)),
        `missing payload: ${payload}`,
      ).toBe(true);
    }
  });

  it("calendarTwo fullcalendar pair equals stock 2.9.1 dist plus the two Percussion mods", () => {
    for (const { payload, npmDist, patches } of FULLCALENDAR_PAYLOADS) {
      const shipped = fs.readFileSync(path.join(REPO_ROOT, payload), "utf8");
      const expected = rebaseFullcalendarFromNpmDist(npmDist, patches);
      expect(
        shipped === expected,
        `${payload} is not stock node_modules/${npmDist} plus the documented ` +
          "Percussion mods — re-apply the slice-5 rebase, do not hand-edit",
      ).toBe(true);
    }
  });

  it("installed fullcalendar dist is 2.9.1 and payloads carry the expanded banner", () => {
    const installed = JSON.parse(
      fs.readFileSync(
        path.join(NODE_MODULES, "fullcalendar", "package.json"),
        "utf8",
      ),
    );
    expect(installed.version).toBe("2.9.1");
    for (const { payload } of FULLCALENDAR_PAYLOADS) {
      const content = fs.readFileSync(path.join(REPO_ROOT, payload), "utf8");
      expect(content).toMatch(/FullCalendar v2\.9\.1/);
      expect(content).toMatch(/Perc\.events/);
      expect(content).toMatch(/Perc\.range/);
    }
    // The rebase fixes the published min copy: it previously shipped a
    // different uglify mangling with an unexpanded version field.
    const minified = fs.readFileSync(
      path.join(REPO_ROOT, FULLCALENDAR_BASE, "fullcalendar.min.js"),
      "utf8",
    );
    expect(minified).toMatch(/version:"2\.9\.1"/);
    expect(minified).not.toMatch(/<%= meta\./);
  });

  it("frontend manifests pin fullcalendar to 2.9.1 (npm audit visible)", () => {
    const manifest = JSON.parse(
      fs.readFileSync(path.join(FRONTEND_ROOT, "package.json"), "utf8"),
    );
    // Exact pin (not a caret): the payload is the 2.9.1 dist plus the
    // Percussion mods, and later majors rewrite the API and the bundle.
    expect(manifest.dependencies["fullcalendar"]).toBe("2.9.1");
  });

  it("rebased calendarTwo fullcalendar pair parses as valid JavaScript", () => {
    for (const { payload } of FULLCALENDAR_PAYLOADS) {
      const source = fs.readFileSync(path.join(REPO_ROOT, payload), "utf8");
      expect(() => new Script(source), `${payload} failed to parse`).not.toThrow();
    }
  });
});

const CALENDARTWO_BASE = path.join(
  "modules",
  "perc-packages",
  "src",
  "main",
  "resources",
  "Packages",
  "perc.widget.calendar",
  "sys__UserDependency--web_resources",
  "widgets",
  "calendarTwo",
  "js",
);

// CalendarTwo gcal + lang-all companions (percCalendarTwo.xml: gcal-js,
// lang-all-js). Slice 6 of #6522 replaces the differently-minified payload
// copies with the npm fullcalendar@2.9.1 dist bytes verbatim (exact pin in
// both WebUI frontend manifests from slice 5, so Dependabot watches the
// base version). Version archaeology: the old gcal was the minified
// equivalent of the stock dist gcal logic, and the old lang-all covered
// the same 56-locale set with only minifier-artifact diffs
// (trailing-semicolon elision, comma-expression parens) — both payloads
// are behavior-preserving byte swaps. The stock gcal banner ships the
// unexpanded <%= meta.* %> wart verbatim (comment-only, inert — same
// precedent as the slice-5 non-min version wart).
const GCAL_LANGALL_PAYLOADS = [
  {
    // GCal source companion for googleCalendarId feeds
    // (percCalendarTwoSnippet.vm googleCalendarArray loop).
    payload: path.join(CALENDARTWO_BASE, "gcal.js"),
    npmDist: path.join("fullcalendar", "dist", "gcal.js"),
    marker: "googleCalendarId",
  },
  {
    // Locale companion for the widget `lang:` option
    // (percCalendarTwoSnippet.vm `lang: '${perc_calendar_display_language}'`).
    payload: path.join(CALENDARTWO_BASE, "lang-all.js"),
    npmDist: path.join("fullcalendar", "dist", "lang-all.js"),
    marker: "fullCalendar.datepickerLang",
  },
];

function loadCalendarTwoCompanionRuntime() {
  // Mirror the widget script order (percCalendarTwo.xml): jquery, moment,
  // fullcalendar.min.js, then the gcal/lang-all companions. Loading the
  // rebased fullcalendar.min also patches moment.fn with the hasZone /
  // hasTime / stripZone extensions the gcal fetcher relies on.
  const $ = require(path.join(NODE_MODULES, "jquery", "dist", "jquery.js"));
  const moment = require(
    path.join(NODE_MODULES, "moment", "min", "moment.min.js"),
  );
  globalThis.jQuery = $;
  window.jQuery = $;
  globalThis.moment = moment;
  window.moment = moment;
  window.eval(
    fs.readFileSync(
      path.join(NODE_MODULES, "fullcalendar", "dist", "fullcalendar.min.js"),
      "utf8",
    ),
  );
  expect($.fullCalendar.version).toBe("2.9.1");
  window.eval(
    fs.readFileSync(path.join(REPO_ROOT, GCAL_LANGALL_PAYLOADS[0].payload), "utf8"),
  );
  window.eval(
    fs.readFileSync(path.join(REPO_ROOT, GCAL_LANGALL_PAYLOADS[1].payload), "utf8"),
  );
  return { $, moment };
}

describe("widget-payload npm refresh parity (issue #6522 slice 6: calendarTwo gcal + lang-all)", () => {
  it("calendarTwo gcal/lang-all payloads exist under the audited Packages tree", () => {
    for (const { payload } of GCAL_LANGALL_PAYLOADS) {
      expect(
        fs.existsSync(path.join(REPO_ROOT, payload)),
        `missing payload: ${payload}`,
      ).toBe(true);
    }
  });

  it("calendarTwo gcal/lang-all payload bytes are byte-identical to the npm fullcalendar dists", () => {
    for (const { payload, npmDist } of GCAL_LANGALL_PAYLOADS) {
      const shipped = fs.readFileSync(path.join(REPO_ROOT, payload));
      const dist = fs.readFileSync(path.join(NODE_MODULES, npmDist));
      expect(
        shipped.equals(dist),
        `${payload} drifted from node_modules/${npmDist} — refresh from npm`,
      ).toBe(true);
    }
  });

  it("calendarTwo gcal/lang-all payloads carry the expected markers", () => {
    for (const { payload, marker } of GCAL_LANGALL_PAYLOADS) {
      const content = fs.readFileSync(path.join(REPO_ROOT, payload), "utf8");
      expect(content, `${payload} marker mismatch`).toContain(marker);
    }
    // The stock gcal dist banner ships unexpanded <%= meta.* %> fields
    // (comment-only, inert) — preserved verbatim from fullcalendar@2.9.1
    // upstream, same precedent as the slice-5 non-min version wart.
    const gcal = fs.readFileSync(
      path.join(REPO_ROOT, GCAL_LANGALL_PAYLOADS[0].payload),
      "utf8",
    );
    expect(gcal).toContain("Google Calendar Plugin");
  });

  it("no new frontend manifest pin is needed (fullcalendar 2.9.1 exact pin from slice 5 covers both dists)", () => {
    const manifest = JSON.parse(
      fs.readFileSync(path.join(FRONTEND_ROOT, "package.json"), "utf8"),
    );
    // Exact pin (not a caret): both payloads are the 2.9.1 dists and later
    // majors rewrite the API and the bundle.
    expect(manifest.dependencies["fullcalendar"]).toBe("2.9.1");
  });

  it("gcal plugin normalizes Google Calendar sources (gmail URL + v3 feed URL)", () => {
    const { $ } = loadCalendarTwoCompanionRuntime();
    const normalizers = $.fullCalendar.sourceNormalizers;
    expect(normalizers.length).toBeGreaterThan(0);
    // The widget loads gcal after fullcalendar, so its normalizer is last.
    const normalize = normalizers[normalizers.length - 1];
    // Plain Gmail address becomes the calendar id (widget googleSource loop
    // passes raw calendarId strings through the same path).
    const gmail = { url: "someone@gmail.com" };
    normalize(gmail);
    expect(gmail.googleCalendarId).toBe("someone@gmail.com");
    expect(gmail.editable).toBe(false);
    expect(gmail.url).toBe("someone@gmail.com");
    // V3 feed URL is scraped to the bare calendar id.
    const feed = {
      url: "https://www.googleapis.com/calendar/v3/calendars/abc123",
    };
    normalize(feed);
    expect(feed.googleCalendarId).toBe("abc123");
    expect(feed.editable).toBe(false);
  });

  it("gcal fetcher requires an API key and builds the v3 request otherwise", () => {
    const { $, moment } = loadCalendarTwoCompanionRuntime();
    const fetchers = $.fullCalendar.sourceFetchers;
    expect(fetchers.length).toBeGreaterThan(0);
    const fetch = fetchers[fetchers.length - 1];
    // Mirror the widget range (percCalendarTwoSnippet.vm month view).
    const start = moment("2026-11-14T10:00:00");
    const end = moment("2026-12-26T10:00:00");
    const calendar = { options: {} };
    // No key anywhere: empty source, no throw (widget renders no GCal feed).
    expect(
      fetch.call(
        calendar,
        { googleCalendarId: "test@gmail.com" },
        start.clone(),
        end.clone(),
        null,
      ),
    ).toEqual({});
    // Keyed feed: v3 JSONP request with the encoded calendar id.
    const out = fetch.call(
      calendar,
      { googleCalendarId: "test@gmail.com", googleCalendarApiKey: "KEY" },
      start.clone(),
      end.clone(),
      null,
    );
    expect(out.url).toContain(`calendars/${encodeURIComponent("test@gmail.com")}/events`);
    expect(out.data.key).toBe("KEY");
    expect(out.data.singleEvents).toBe(true);
    expect(out.data.maxResults).toBe(9999);
    // The fetcher nulls the GCal primitives so removeEventSource keeps
    // working through the url (upstream "obsolete since 2.2.3" hack).
    expect(out.googleCalendarId).toBeNull();
    expect(out.startParam).toBe(false);
    expect(out.endParam).toBe(false);
    expect(out.timezoneParam).toBe(false);
  });

  it("lang-all registers the full locale set on the rebased calendarTwo runtime", () => {
    const { $ } = loadCalendarTwoCompanionRuntime();
    const langs = $.fullCalendar.langs;
    // Stock 2.9.1 lang-all covers 56 codes (52 locales + en aliases);
    // the old differently-minified payload covered the same set.
    expect(Object.keys(langs).length).toBeGreaterThanOrEqual(50);
    for (const code of ["en", "fr", "de", "ja", "zh-cn", "ar", "pt-br"]) {
      expect(langs[code], `locale ${code} missing`).toBeTruthy();
    }
    // Spot-check the widget `lang:` option path: month names and the
    // "+ more" overflow label render from the registered definitions.
    expect(langs["de"].buttonText.month).toBe("Monat");
    expect(langs["zh-cn"].buttonText.month).toBe("月");
    expect(langs["de"].eventLimitText(3)).toBe("+ weitere 3");
    expect(langs["fr"].buttonText.today).toBe("Aujourd'hui");
  });
});
