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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Unit tests for the coverage-matrix index (#6238 aggregate, SC-004). No
 * live CMS and no QA cell required:
 * {@code node --test tests/unit/coverage-matrix-index.test.js} (wired into
 * {@code npm run test:unit}).
 */

"use strict";

const assert = require("node:assert");
const fs = require("node:fs");
const path = require("node:path");
const { describe, it } = require("node:test");

const {
  MATRIX_SUITE,
  MATRIX_SPEC,
  DOC_PAGE,
  DOC_ID,
  SERVICE_MATRICES,
  matrixInventories,
  findDuplicates,
  uncoveredDynamicWidgets,
  serviceCoverage,
  unaccountedServices,
  repoRootFromHelper,
  docPagePath,
  indexSummary,
} = require("../helpers/coverage-matrix-index");

const FRONTEND_DIR = path.join(__dirname, "..");
const HELPERS_DIR = path.join(FRONTEND_DIR, "helpers");

describe("coverage-matrix-index helper", () => {
  it("pins the three epic matrices in order with non-empty counts", () => {
    const inventories = matrixInventories();
    assert.deepStrictEqual(
      inventories.map((entry) => entry.suite),
      [
        "content-create-matrix",
        "published-widget-matrix",
        "dts-interaction-matrix",
      ],
    );
    for (const entry of inventories) {
      assert.ok(entry.count > 0, `${entry.suite}: expected a non-empty inventory`);
      assert.ok(
        entry.spec.startsWith("tests/") && entry.spec.endsWith(".spec.js"),
        `${entry.suite}: expected a module-relative spec path`,
      );
    }
  });

  it("reports duplicates across a combined inventory", () => {
    assert.deepStrictEqual(findDuplicates(["a", "b", "a", "c", "b"]), ["a", "b"]);
    assert.deepStrictEqual(findDuplicates([]), []);
  });

  it("has no dynamic widget without an interaction service", () => {
    assert.deepStrictEqual(uncoveredDynamicWidgets(), []);
  });

  it("covers every DTS service with a proving artifact", () => {
    assert.deepStrictEqual(unaccountedServices(), []);
    const rows = serviceCoverage();
    assert.strictEqual(rows.length, 6);
    for (const row of rows) {
      assert.strictEqual(row.covered, true, `${row.service}: expected coverage`);
    }
    const byService = new Map(rows.map((row) => [row.service, row]));
    assert.strictEqual(byService.get("membership").surface, false);
    assert.ok(
      byService.get("membership").widgets.length > 0,
      "membership: expected widget evidence",
    );
    assert.ok(
      byService.get("forms").note.includes("#6482"),
      "forms: expected the held-out note",
    );
    for (const [service, spec] of Object.entries(SERVICE_MATRICES)) {
      assert.ok(byService.has(service), `registry names unknown service ${service}`);
      assert.ok(
        spec.startsWith("tests/") && spec.endsWith(".spec.js"),
        `expected a module-relative spec path for ${service}`,
      );
    }
  });

  it("resolves the operator docs page under the repo root", () => {
    const repoRoot = repoRootFromHelper(HELPERS_DIR);
    assert.ok(
      fs.existsSync(path.join(repoRoot, "product-docs", "_config.yaml")),
      `expected the product-docs tree under ${repoRoot}`,
    );
    assert.strictEqual(
      docPagePath(repoRoot),
      path.join(repoRoot, DOC_PAGE),
    );
  });

  it("summarizes the family for full-run evidence", () => {
    const summary = indexSummary();
    assert.strictEqual(summary.suite, MATRIX_SUITE);
    assert.strictEqual(summary.spec, MATRIX_SPEC);
    assert.strictEqual(summary.matrices.length, 3);
    assert.strictEqual(summary.services.length, 6);
    assert.match(summary.updated, /^\d{4}-\d{2}-\d{2}$/);
  });

  it("pins the operator docs page and id contract", () => {
    assert.strictEqual(DOC_PAGE, "product-docs/8.2/admin/qa-coverage-matrices.md");
    assert.strictEqual(DOC_ID, "admin-qa-coverage-matrices");
  });
});
