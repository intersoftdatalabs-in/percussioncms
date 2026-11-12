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
 * Unit tests for the US1 creation-matrix inventory + row helpers (#6239).
 * No live CMS required: {@code npm run test:unit}.
 */

"use strict";

const assert = require("node:assert");
const fs = require("node:fs");
const path = require("node:path");
const { describe, it } = require("node:test");

const {
  MATRIX_SUITE,
  MATRIX_SPEC,
  CASE_CREATE,
  CASE_NEGATIVE,
  CASE_NAMES,
  BUILT_IN_TYPES,
  SYSTEM_EXCLUSIONS,
  buildResultRow,
  sampleValueFor,
  statusForCases,
  tagForType,
  validateResultRow,
} = require("../helpers/content-create-matrix");

const MODULE_DIR = path.join(__dirname, "..", "..", "..");
const FRONTEND_DIR = path.join(__dirname, "..");

function goodRow() {
  return buildResultRow(
    BUILT_IN_TYPES[0],
    { [CASE_CREATE]: "pass", [CASE_NEGATIVE]: "pass" },
    { spec: MATRIX_SPEC, updated: "2026-11-12" },
  );
}

describe("content-create-matrix helpers (#6239)", () => {
  it("pins a closed, duplicate-free inventory of built-in types", () => {
    assert.equal(BUILT_IN_TYPES.length, 54);
    const names = BUILT_IN_TYPES.map((entry) => entry.name);
    assert.equal(new Set(names).size, names.length);
    for (const entry of BUILT_IN_TYPES) {
      assert.match(entry.name, /^(perc|rff)[A-Za-z0-9]+$/);
      assert.ok(entry.label.length > 0, `${entry.name} label`);
      assert.ok(entry.folder.length > 0, `${entry.name} folder`);
      assert.ok(entry.roundTripField.length > 0, `${entry.name} roundTripField`);
      assert.ok(entry.negativeField.length > 0, `${entry.name} negativeField`);
      assert.ok(
        entry.folder === "/Assets" || entry.folder === "//Sites/EnterpriseInvestments",
        `${entry.name} folder`,
      );
      assert.ok(sampleValueFor(entry).includes(entry.name), "sample value names the type");
      assert.ok(tagForType(entry.name).startsWith("@content-create-"), "per-type tag");
    }
    const excluded = SYSTEM_EXCLUSIONS.map((entry) => entry.name);
    assert.deepEqual([...excluded].sort(), ["Folder", "PSWidgetProperties", "percPageTemplate"]);
    for (const name of excluded) {
      assert.ok(!names.includes(name), `${name} must not be pinned and excluded`);
    }
    for (const entry of SYSTEM_EXCLUSIONS) {
      assert.ok(entry.reason.length > 0, `${entry.name} reason`);
    }
  });

  it("derives row status with fail winning over skip winning over pass", () => {
    assert.equal(statusForCases({ [CASE_CREATE]: "pass", [CASE_NEGATIVE]: "pass" }), "pass");
    assert.equal(statusForCases({ [CASE_CREATE]: "pass", [CASE_NEGATIVE]: "skip" }), "skip");
    assert.equal(statusForCases({ [CASE_CREATE]: "skip", [CASE_NEGATIVE]: "pass" }), "skip");
    assert.equal(statusForCases({ [CASE_CREATE]: "pass", [CASE_NEGATIVE]: "fail" }), "fail");
    assert.equal(statusForCases({ [CASE_CREATE]: "fail", [CASE_NEGATIVE]: "skip" }), "fail");
  });

  it("builds rows the validator accepts", () => {
    const row = goodRow();
    assert.equal(row.suite, MATRIX_SUITE);
    assert.deepEqual(row.cases, { [CASE_CREATE]: "pass", [CASE_NEGATIVE]: "pass" });
    assert.equal(row.status, "pass");
    assert.deepEqual(validateResultRow(row), []);
    assert.ok(CASE_NAMES.includes(CASE_CREATE));
    assert.ok(CASE_NAMES.includes(CASE_NEGATIVE));
  });

  it("rejects rows that break the contract", () => {
    assert.ok(validateResultRow(null).length > 0);
    assert.ok(validateResultRow([]).length > 0);
    const extra = { ...goodRow(), bogus: 1 };
    assert.ok(validateResultRow(extra).some((e) => e.includes("unexpected property")));
    const badSuite = { ...goodRow(), suite: "other" };
    assert.ok(validateResultRow(badSuite).length > 0);
    const badType = { ...goodRow(), contentType: "CustomType" };
    assert.ok(validateResultRow(badType).some((e) => e.includes("contentType")));
    const badCell = { ...goodRow(), cell: "dts" };
    assert.ok(validateResultRow(badCell).length > 0);
    const badCase = { ...goodRow(), cases: { [CASE_CREATE]: "pass", [CASE_NEGATIVE]: "maybe" } };
    assert.ok(validateResultRow(badCase).length > 0);
    const missingCase = { ...goodRow(), cases: { [CASE_CREATE]: "pass" } };
    assert.ok(validateResultRow(missingCase).length > 0);
    const badStatus = { ...goodRow(), status: "unknown" };
    assert.ok(validateResultRow(badStatus).length > 0);
    const badDate = { ...goodRow(), updated: "11/12/2026" };
    assert.ok(validateResultRow(badDate).length > 0);
  });

  it("ships a schema file and a valid example matrix", () => {
    const schemaPath = path.join(MODULE_DIR, "contracts", "results-matrix.schema.json");
    const schema = JSON.parse(fs.readFileSync(schemaPath, "utf8"));
    assert.equal(schema.title, "Content/widget results-matrix row");
    assert.ok(Array.isArray(schema.required) && schema.required.includes("contentType"));
    const examplePath = path.join(MODULE_DIR, "contracts", "results-matrix.example.json");
    const example = JSON.parse(fs.readFileSync(examplePath, "utf8"));
    assert.ok(Array.isArray(example) && example.length > 0);
    for (const row of example) {
      assert.deepEqual(validateResultRow(row), []);
    }
  });

  it("is referenced by the matrix spec for every pinned type", () => {
    const spec = fs.readFileSync(path.join(FRONTEND_DIR, "content-create-matrix.spec.js"), "utf8");
    assert.ok(spec.includes("BUILT_IN_TYPES.forEach"), "data-driven over the pinned inventory");
    assert.ok(spec.includes("blank required field shows a message"), "negative case declared");
    assert.ok(spec.includes("creates, saves and reopens"), "round-trip case declared");
    assert.ok(spec.includes("content-create-matrix.json"), "matrix artifact written");
  });
});
