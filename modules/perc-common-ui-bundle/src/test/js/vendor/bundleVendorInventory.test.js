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
 * Slice-5A vendored inventory for the delivery-tier widget bundle
 * (issue #7375, parent audit #5627).
 *
 * `build-scripts/bundle.mjs` concatenates two committed vendor sources
 * into `perc_common_ui.js` (shipped to published pages), bypassing the npm
 * graph — the exact class of asset the #5627 audit exists to find:
 *
 * | path | version | license | consumer | npm | disposition |
 * |------|---------|---------|----------|-----|-------------|
 * | src/main/js/vendor/jquery.timeago.js | 1.6.3 (Ryan McGeary) | MIT | published delivery-tier pages via perc_common_ui.js | timeago@1.6.7 ALREADY npm-tracked in WebUI frontend | #7379 (add npm dep, drop vendored 1.6.3, parity test) |
 * | src/main/js/vendor/simpledateformat.js | 2007 (Tim Down) | Apache-2.0 | same | none (canonical file is timdown.co.uk-hosted) | RETAIN (#5665 precedent, same as the jslib twin) |
 *
 * The bundle.mjs "not on npm" comment above the vendor entries is STALE
 * for timeago (only simpledateformat earns it). These pins fail loudly on
 * banner, license, or concat-order drift; the #7379 migration updates this
 * file (vendored timeago gone, npm dep asserted) when it lands.
 */

import { readFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const MODULE_ROOT = resolve(__dirname, "../../../..");

function readHead(rel, bytes = 2048) {
  return readFileSync(join(MODULE_ROOT, rel), "utf8").slice(0, bytes);
}

describe("delivery-tier bundle vendored inventory (issue #7375)", () => {
  it("pins the vendored timeago 1.6.3 until the #7379 npm migration lands", () => {
    const rel = "src/main/js/vendor/jquery.timeago.js";
    const head = readHead(rel);
    expect(head, `${rel} lost its version banner`).toMatch(/@version 1\.6\.3/);
    expect(head, `${rel} lost its MIT marker`).toMatch(/MIT License/);
    expect(head, `${rel} lost its author line`).toMatch(/Ryan McGeary/);
    // The npm gap that #7379 closes: WebUI frontend already tracks
    // timeago@1.6.7, but this bundle's package.json does not declare it —
    // the vendored 1.6.3 is the only source. When #7379 lands (npm dep +
    // vendored file deleted), this assertion flips to toHaveProperty.
    const pkg = JSON.parse(
      readFileSync(join(MODULE_ROOT, "package.json"), "utf8"),
    );
    expect(
      pkg.dependencies ?? {},
      "timeago unexpectedly gained an npm dep without the #7379 test update",
    ).not.toHaveProperty("timeago");
  });

  it("retains the Tim Down simpledateformat under the #5665 precedent", () => {
    const rel = "src/main/js/vendor/simpledateformat.js";
    const head = readHead(rel);
    expect(head, `${rel} lost its Tim Down header`).toMatch(
      /Copyright 2007 Tim Down/,
    );
    expect(head, `${rel} lost its Apache grant`).toMatch(/Apache License/);
  });

  it("concatenates the vendor pair first in bundle.mjs production order", () => {
    const script = readFileSync(
      join(MODULE_ROOT, "build-scripts/bundle.mjs"),
      "utf8",
    );
    const simpleIdx = script.indexOf(
      '"src/main/js/vendor/simpledateformat.js"',
    );
    const timeagoIdx = script.indexOf(
      '"src/main/js/vendor/jquery.timeago.js"',
    );
    expect(simpleIdx, "bundle.mjs lost the simpledateformat entry").toBeGreaterThanOrEqual(
      0,
    );
    expect(timeagoIdx, "bundle.mjs lost the timeago entry").toBeGreaterThanOrEqual(
      0,
    );
    expect(
      simpleIdx < timeagoIdx,
      "bundle.mjs vendor order flipped (simpledateformat must precede timeago)",
    ).toBe(true);
  });
});
