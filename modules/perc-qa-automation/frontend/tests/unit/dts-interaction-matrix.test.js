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
 * Unit tests for the US3 DTS interaction matrix inventory + row helpers
 * (#6241). No live CMS, no DTS cell, and no seeded fixtures required:
 * {@code npm run test:unit}.
 */

"use strict";

const assert = require("node:assert");
const fs = require("node:fs");
const path = require("node:path");
const { describe, it } = require("node:test");

const {
  MATRIX_SUITE,
  MATRIX_SPEC,
  CASE_INTERACT,
  CASE_NAMES,
  RESULT_STATUSES,
  MATRIX_CELLS,
  SKIP_NO_DTS_BASE,
  skipNoDts,
  skipServiceDown,
  skipNoFixture,
  DTS_SERVICES,
  tagForService,
  fixtureValue,
  buildInteractionRow,
  validateInteractionRow,
} = require("../helpers/dts-interaction-matrix");

const MODULE_DIR = path.join(__dirname, "..", "..", "..");
const FRONTEND_DIR = path.join(__dirname, "..");

function goodRow() {
  return buildInteractionRow(
    DTS_SERVICES.find((entry) => entry.name === "polls"),
    { [CASE_INTERACT]: "pass" },
    { spec: MATRIX_SPEC, updated: "2026-11-13" },
  );
}

describe("dts-interaction-matrix helpers (#6241)", () => {
  it("pins a closed, duplicate-free inventory of DTS services", () => {
    assert.equal(DTS_SERVICES.length, 6);
    const names = DTS_SERVICES.map((entry) => entry.name);
    assert.equal(new Set(names).size, names.length);
    for (const entry of DTS_SERVICES) {
      assert.match(entry.name, /^(polls|feeds|forms|comments|metadata|membership)$/, `${entry.name} name`);
      assert.ok(entry.label.length > 0, `${entry.name} label`);
      assert.ok(entry.root.startsWith("/"), `${entry.name} root`);
      assert.equal(entry.csrf, `${entry.root}/csrf`, `${entry.name} csrf probe`);
      assert.ok(entry.fixtureEnv.startsWith("DTS_FIXTURE_"), `${entry.name} fixture env`);
      assert.ok(entry.interaction.length > 0, `${entry.name} interaction`);
      assert.ok(tagForService(entry.name).startsWith("@dts-"), "per-service tag");
    }
    // Every US2 dynamic widget's backing service has an interaction case,
    // plus metadata (auto-list/indexing, no dedicated widget).
    const widgetMatrix = require("../helpers/published-widget-matrix");
    const expected = [...widgetMatrix.DYNAMIC_SERVICES, "metadata"].sort();
    assert.deepEqual([...names].sort(), expected);
    for (const entry of DTS_SERVICES) {
      if (entry.name === "metadata") {
        assert.equal(entry.widget, null, "metadata has no dedicated widget");
      } else {
        assert.ok(
          widgetMatrix.WIDGETS.some((widget) => widget.dts === entry.name),
          `service ${entry.name} surfaces a US2 dynamic widget`,
        );
      }
    }
    assert.ok(SKIP_NO_DTS_BASE.includes("DTS_BASE_URL"), "skip names the missing env");
    assert.ok(
      skipNoDts(DTS_SERVICES[0]).includes("DTS_BASE_URL"),
      "no-cell skip names the missing env",
    );
    assert.ok(
      skipServiceDown(DTS_SERVICES[0], "https://dts.example").includes("https://dts.example"),
      "service-down skip names the probed origin",
    );
    assert.ok(
      skipNoFixture(DTS_SERVICES[0], "fixture poll 'x' is not seeded").includes("not seeded"),
      "fixture skip names the gap",
    );
    assert.equal(fixtureValue(DTS_SERVICES[0]), "qamatrix-poll", "fixture default");
  });

  it("resolves the DTS base URL only from DTS_BASE_URL", () => {
    const saved = process.env.DTS_BASE_URL;
    try {
      delete process.env.DTS_BASE_URL;
      const { dtsBaseUrl } = require("../helpers/dts-interaction-matrix");
      assert.equal(dtsBaseUrl(), null);
      process.env.DTS_BASE_URL = "https://dts.example";
      assert.equal(dtsBaseUrl().origin, "https://dts.example");
    } finally {
      if (saved === undefined) {
        delete process.env.DTS_BASE_URL;
      } else {
        process.env.DTS_BASE_URL = saved;
      }
    }
  });

  it("builds rows the validator accepts", () => {
    const row = goodRow();
    assert.equal(row.suite, MATRIX_SUITE);
    assert.deepEqual(row.cases, { [CASE_INTERACT]: "pass" });
    assert.equal(row.status, "pass");
    assert.equal(row.cell, MATRIX_CELLS[0]);
    assert.deepEqual(validateInteractionRow(row), []);
    assert.ok(CASE_NAMES.includes(CASE_INTERACT));
    assert.ok(RESULT_STATUSES.includes("skip"));
    const skipped = buildInteractionRow(
      DTS_SERVICES[0],
      { [CASE_INTERACT]: "skip" },
      { spec: MATRIX_SPEC, updated: "2026-11-13" },
    );
    assert.equal(skipped.status, "skip");
    assert.deepEqual(validateInteractionRow(skipped), []);
  });

  it("rejects rows that break the contract", () => {
    assert.ok(validateInteractionRow(null).length > 0);
    assert.ok(validateInteractionRow([]).length > 0);
    const extra = { ...goodRow(), bogus: 1 };
    assert.ok(validateInteractionRow(extra).some((e) => e.includes("unexpected property")));
    const badSuite = { ...goodRow(), suite: "published-widget-matrix" };
    assert.ok(validateInteractionRow(badSuite).length > 0);
    const badName = { ...goodRow(), contentType: "poll" };
    assert.ok(validateInteractionRow(badName).some((e) => e.includes("pinned DTS service")));
    const badCell = { ...goodRow(), cell: "cms-only" };
    assert.ok(validateInteractionRow(badCell).length > 0);
    const badCase = { ...goodRow(), cases: { [CASE_INTERACT]: "maybe" } };
    assert.ok(validateInteractionRow(badCase).length > 0);
    const missingCase = { ...goodRow(), cases: {} };
    assert.ok(validateInteractionRow(missingCase).length > 0);
    const wrongCase = { ...goodRow(), cases: { staticRender: "pass" } };
    assert.ok(validateInteractionRow(wrongCase).length > 0);
    const badStatus = { ...goodRow(), status: "unknown" };
    assert.ok(validateInteractionRow(badStatus).length > 0);
    const badDate = { ...goodRow(), updated: "11/13/2026" };
    assert.ok(validateInteractionRow(badDate).length > 0);
  });

  it("ships a schema covering the interaction suite and a valid example matrix", () => {
    const schemaPath = path.join(MODULE_DIR, "contracts", "results-matrix.schema.json");
    const schema = JSON.parse(fs.readFileSync(schemaPath, "utf8"));
    assert.equal(schema.title, "Content/widget results-matrix row");
    assert.ok(Array.isArray(schema.required) && schema.required.includes("contentType"));
    assert.ok(
      schema.properties.suite.enum.includes("dts-interaction-matrix"),
      "schema suite enum covers the interaction matrix",
    );
    assert.ok(
      schema.properties.cell.enum.includes("dts-cell"),
      "schema cell enum covers the DTS cell",
    );
    assert.ok(
      schema.properties.cases.anyOf.some((branch) => (branch.required || []).includes("interact")),
      "schema cases cover the interact case",
    );
    const examplePath = path.join(MODULE_DIR, "contracts", "results-matrix-dts.example.json");
    const example = JSON.parse(fs.readFileSync(examplePath, "utf8"));
    assert.ok(Array.isArray(example) && example.length === 6);
    for (const row of example) {
      assert.deepEqual(validateInteractionRow(row), []);
    }
  });

  it("is referenced by the matrix spec for every pinned service", () => {
    const spec = fs.readFileSync(path.join(FRONTEND_DIR, "dts-interaction-matrix.spec.js"), "utf8");
    assert.ok(spec.includes("DTS_SERVICES.forEach"), "data-driven over the pinned inventory");
    assert.ok(spec.includes("vote persists") || spec.includes("interaction"), "interaction case declared");
    assert.ok(spec.includes("dts-interaction-matrix.json"), "matrix artifact written");
    assert.ok(spec.includes("DTS_BASE_URL"), "skip contract declared");
    for (const entry of DTS_SERVICES) {
      assert.ok(
        spec.includes(`interact${entry.name[0].toUpperCase()}${entry.name.slice(1)}`),
        `spec implements the ${entry.name} interaction`,
      );
    }
  });
});
