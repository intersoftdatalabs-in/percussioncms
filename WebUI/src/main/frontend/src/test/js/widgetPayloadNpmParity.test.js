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
 * with the npm dist builds (lodash 4.17.21, moment 2.31.0), both
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
 * The remaining #6319 rows (FullCalendar 1.x/2.x, Slick, Foundation,
 * nav superfish/hoverIntent/supersubs, List.js, MiniColors, qTip,
 * gcal/lang-all) stay vendored and are tracked in the #6319 leftover
 * residual — this file must grow a parity block each time another row
 * migrates.
 */

import { describe, expect, it } from "vitest";
import fs from "node:fs";
import path from "node:path";
import { createRequire } from "node:module";
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
    versionRe: /4\.17\.21/,
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
    expect(lodash.VERSION).toBe("4.17.21");
  });

  it("frontend package.json tracks both ranges (npm audit visible)", () => {
    const manifest = JSON.parse(
      fs.readFileSync(path.join(FRONTEND_ROOT, "package.json"), "utf8"),
    );
    expect(manifest.dependencies["lodash"]).toBe("^4.17.21");
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
