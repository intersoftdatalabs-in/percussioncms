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
 * Unit tests for golden unattended smoke inventory (#2490).
 * Ensures @folder-recycle is wired into the extended set only, and package.json
 * scripts stay in lockstep with the inventory paths.
 *
 * Run: npm run test:unit  (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const {
  GOLDEN_UNATTENDED_SMOKE_SET,
  listBaselineEntries,
  listExtendedEntries,
  pathsForTier,
  getGoldenEntry,
} = require("../helpers/golden-unattended-smoke-set");

const PACKAGE_JSON = path.join(__dirname, "..", "..", "package.json");

/**
 * @param {string} scriptBody
 * @param {string} relPath
 * @returns {boolean}
 */
function scriptMentionsPath(scriptBody, relPath) {
  // package.json uses forward-slash paths (Playwright CLI) on all platforms
  return String(scriptBody).includes(relPath.replace(/\\/g, "/"));
}

describe("GOLDEN_UNATTENDED_SMOKE_SET", () => {
  it("includes baseline golden and extended folder-recycle (#2490)", () => {
    const ids = new Set(GOLDEN_UNATTENDED_SMOKE_SET.map((e) => e.id));
    assert.ok(ids.has("golden-login-explorer"));
    assert.ok(ids.has("folder-recycle"));
  });

  it("requires unique ids", () => {
    const ids = GOLDEN_UNATTENDED_SMOKE_SET.map((e) => e.id);
    assert.equal(ids.length, new Set(ids).size);
  });

  it("baseline is golden only (minimal unattended default)", () => {
    const baseline = listBaselineEntries();
    assert.equal(baseline.length, 1);
    assert.equal(baseline[0].id, "golden-login-explorer");
    assert.deepEqual(pathsForTier("baseline"), [
      "tests/golden-unattended-smoke.spec.js",
    ]);
  });

  it("extended includes folder-recycle without becoming full suite", () => {
    const extended = listExtendedEntries();
    assert.ok(extended.length >= 2);
    assert.ok(extended.some((e) => e.id === "folder-recycle"));
    const paths = pathsForTier("extended");
    assert.ok(paths.includes("tests/golden-unattended-smoke.spec.js"));
    assert.ok(paths.includes("tests/folder-recycle-smoke.spec.js"));
    // Guard: do not silently grow into a full-suite default
    assert.ok(
      paths.length <= 8,
      `extended set too large (${paths.length}); keep multi-path smoke small`,
    );
  });

  it("folder-recycle entry uses surface tag and path peers", () => {
    const entry = getGoldenEntry("folder-recycle");
    assert.equal(entry.tier, "extended");
    assert.equal(entry.tag, "folder-recycle");
    assert.equal(entry.path, "tests/folder-recycle-smoke.spec.js");
    assert.equal(entry.file, "folder-recycle-smoke.spec.js");
  });

  it("pathsForTier rejects unknown tier", () => {
    assert.throws(() => pathsForTier("full-suite"), /Unknown golden tier/);
  });

  it("getGoldenEntry rejects unknown id", () => {
    assert.throws(() => getGoldenEntry("not-a-real-id"), /Unknown golden/);
  });
});

describe("package.json golden scripts lockstep (#2490)", () => {
  it("test:golden stays baseline-only; test:golden-extended wires folder-recycle", () => {
    const pkg = JSON.parse(fs.readFileSync(PACKAGE_JSON, "utf8"));
    const scripts = pkg.scripts || {};

    assert.ok(scripts["test:golden"], "test:golden required");
    assert.ok(
      scripts["test:golden-extended"],
      "test:golden-extended required for optional @folder-recycle overnight set",
    );
    assert.ok(
      scripts["test:golden-extended:list"],
      "test:golden-extended:list required (no live CMS)",
    );

    const baselinePaths = pathsForTier("baseline");
    const extendedPaths = pathsForTier("extended");

    for (const p of baselinePaths) {
      assert.ok(
        scriptMentionsPath(scripts["test:golden"], p),
        `test:golden must include ${p}`,
      );
    }
    // Minimal golden must NOT pull folder-recycle by default
    assert.ok(
      !scriptMentionsPath(scripts["test:golden"], "folder-recycle-smoke.spec.js"),
      "test:golden must remain minimal (no folder-recycle path)",
    );

    for (const p of extendedPaths) {
      assert.ok(
        scriptMentionsPath(scripts["test:golden-extended"], p),
        `test:golden-extended must include ${p}`,
      );
      assert.ok(
        scriptMentionsPath(scripts["test:golden-extended:list"], p),
        `test:golden-extended:list must include ${p}`,
      );
    }
  });
});
