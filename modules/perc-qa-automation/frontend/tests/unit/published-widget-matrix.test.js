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
 * Unit tests for the US2 published-widget matrix inventory + row helpers
 * (#6240). No live CMS and no published site required:
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
  CASE_STATIC,
  CASE_NAMES,
  RESULT_STATUSES,
  MATRIX_CELLS,
  SKIP_NO_SITE,
  skipNoFixture,
  WIDGET_EXCLUSIONS,
  WIDGETS,
  DYNAMIC_SERVICES,
  tagForWidget,
  fixtureUrlFor,
  buildWidgetRow,
  validateWidgetRow,
} = require("../helpers/published-widget-matrix");

const MODULE_DIR = path.join(__dirname, "..", "..", "..");
const FRONTEND_DIR = path.join(__dirname, "..");
const PACKAGES_DIR = path.join(
  MODULE_DIR,
  "..",
  "perc-packages",
  "src",
  "main",
  "resources",
  "Packages",
);

function goodRow() {
  return buildWidgetRow(
    WIDGETS.find((entry) => entry.name === "percCategoryList"),
    { [CASE_STATIC]: "pass" },
    { spec: MATRIX_SPEC, updated: "2026-11-13" },
  );
}

function templateDirsOnDisk() {
  const names = [];
  for (const pkg of fs.readdirSync(PACKAGES_DIR, { withFileTypes: true })) {
    if (!pkg.isDirectory()) {
      continue;
    }
    const widgetsDir = path.join(PACKAGES_DIR, pkg.name, "widgets");
    if (!fs.existsSync(widgetsDir)) {
      continue;
    }
    for (const widget of fs.readdirSync(widgetsDir, { withFileTypes: true })) {
      if (!widget.isDirectory()) {
        continue;
      }
      const templatesDir = path.join(widgetsDir, widget.name, "templates");
      if (fs.existsSync(templatesDir)) {
        names.push(widget.name);
      }
    }
  }
  return names.sort();
}

describe("published-widget-matrix helpers (#6240)", () => {
  it("pins a closed, duplicate-free inventory of published widgets", () => {
    assert.equal(WIDGETS.length, 45);
    const names = WIDGETS.map((entry) => entry.name);
    assert.equal(new Set(names).size, names.length);
    for (const entry of WIDGETS) {
      assert.match(entry.name, /^(perc|simple)[A-Za-z0-9]+$/, `${entry.name} name`);
      assert.ok(entry.label.length > 0, `${entry.name} label`);
      assert.ok(
        entry.kind === "static" || entry.kind === "dynamic" || entry.kind === "head",
        `${entry.name} kind`,
      );
      if (entry.marker == null) {
        assert.ok(
          ["percRawHtml", "percRichText", "percSimpleText", "percTitle"].includes(entry.name),
          `${entry.name}: marker-less entries are passthrough content only`,
        );
      } else {
        assert.ok(entry.marker.length > 0, `${entry.name} marker`);
      }
      if (entry.kind === "dynamic") {
        assert.ok(
          DYNAMIC_SERVICES.includes(entry.dts),
          `${entry.name}: dynamic widget names its DTS service`,
        );
      } else {
        assert.equal(entry.dts, null, `${entry.name}: static/head widgets name no service`);
      }
      assert.ok(tagForWidget(entry.name).startsWith("@widget-"), "per-widget tag");
      const fixture = fixtureUrlFor(new URL("https://published.example"), entry);
      assert.equal(fixture, `https://published.example/widgets/${entry.name}.html`);
    }
    for (const service of DYNAMIC_SERVICES) {
      assert.ok(
        WIDGETS.some((entry) => entry.dts === service),
        `DTS service ${service} has at least one dynamic widget`,
      );
    }
    const excluded = WIDGET_EXCLUSIONS.map((entry) => entry.name);
    assert.deepEqual([...excluded].sort(), [
      "PSWidget_TestProperties",
      "percJQueryUIWidget",
      "percJQueryWidget",
    ]);
    for (const name of excluded) {
      assert.ok(!names.includes(name), `${name} must not be pinned and excluded`);
    }
    for (const entry of WIDGET_EXCLUSIONS) {
      assert.ok(entry.reason.length > 0, `${entry.name} reason`);
    }
    assert.ok(SKIP_NO_SITE.includes("PUBLISHED_SITE_URL"), "skip names the missing env");
    assert.ok(
      skipNoFixture(WIDGETS[0], "https://published.example/widgets/x.html").includes("PUBLISHED_SITE_URL") ||
        skipNoFixture(WIDGETS[0], "https://published.example/widgets/x.html").includes("not published yet"),
      "fixture skip names the gap",
    );
  });

  it("closes the inventory against the shipped widget templates on disk", () => {
    assert.ok(fs.existsSync(PACKAGES_DIR), `widget packages dir exists: ${PACKAGES_DIR}`);
    const onDisk = templateDirsOnDisk();
    assert.ok(onDisk.length > 0, "template scan finds shipped widgets");
    const pinned = new Set(WIDGETS.map((entry) => entry.name));
    const excluded = new Set(WIDGET_EXCLUSIONS.map((entry) => entry.name));
    const unaccounted = onDisk.filter((name) => !pinned.has(name) && !excluded.has(name));
    assert.deepEqual(unaccounted, [], `template dirs with no matrix case and no exclusion reason: ${unaccounted.join(", ")}`);
    const missing = WIDGETS.map((entry) => entry.name).filter((name) => !onDisk.includes(name));
    assert.deepEqual(missing, [], `pinned widgets with no shipped template: ${missing.join(", ")}`);
  });

  it("builds rows the validator accepts", () => {
    const row = goodRow();
    assert.equal(row.suite, MATRIX_SUITE);
    assert.deepEqual(row.cases, { [CASE_STATIC]: "pass" });
    assert.equal(row.status, "pass");
    assert.equal(row.cell, MATRIX_CELLS[0]);
    assert.deepEqual(validateWidgetRow(row), []);
    assert.ok(CASE_NAMES.includes(CASE_STATIC));
    assert.ok(RESULT_STATUSES.includes("skip"));
    const skipped = buildWidgetRow(
      WIDGETS[0],
      { [CASE_STATIC]: "skip" },
      { spec: MATRIX_SPEC, updated: "2026-11-13" },
    );
    assert.equal(skipped.status, "skip");
    assert.deepEqual(validateWidgetRow(skipped), []);
  });

  it("rejects rows that break the contract", () => {
    assert.ok(validateWidgetRow(null).length > 0);
    assert.ok(validateWidgetRow([]).length > 0);
    const extra = { ...goodRow(), bogus: 1 };
    assert.ok(validateWidgetRow(extra).some((e) => e.includes("unexpected property")));
    const badSuite = { ...goodRow(), suite: "content-create-matrix" };
    assert.ok(validateWidgetRow(badSuite).length > 0);
    const badName = { ...goodRow(), contentType: "CustomWidget" };
    assert.ok(validateWidgetRow(badName).some((e) => e.includes("contentType")));
    const badCell = { ...goodRow(), cell: "cms-only" };
    assert.ok(validateWidgetRow(badCell).length > 0);
    const badCase = { ...goodRow(), cases: { [CASE_STATIC]: "maybe" } };
    assert.ok(validateWidgetRow(badCase).length > 0);
    const missingCase = { ...goodRow(), cases: {} };
    assert.ok(validateWidgetRow(missingCase).length > 0);
    const wrongCase = { ...goodRow(), cases: { createRoundTrip: "pass" } };
    assert.ok(validateWidgetRow(wrongCase).length > 0);
    const badStatus = { ...goodRow(), status: "unknown" };
    assert.ok(validateWidgetRow(badStatus).length > 0);
    const badDate = { ...goodRow(), updated: "11/13/2026" };
    assert.ok(validateWidgetRow(badDate).length > 0);
  });

  it("ships a schema covering the widget suite and a valid example matrix", () => {
    const schemaPath = path.join(MODULE_DIR, "contracts", "results-matrix.schema.json");
    const schema = JSON.parse(fs.readFileSync(schemaPath, "utf8"));
    assert.equal(schema.title, "Content/widget results-matrix row");
    assert.ok(Array.isArray(schema.required) && schema.required.includes("contentType"));
    assert.ok(
      schema.properties.suite.enum.includes("published-widget-matrix"),
      "schema suite enum covers the widget matrix",
    );
    assert.ok(
      schema.properties.cell.enum.includes("published-site"),
      "schema cell enum covers the published site",
    );
    const examplePath = path.join(MODULE_DIR, "contracts", "results-matrix-widget.example.json");
    const example = JSON.parse(fs.readFileSync(examplePath, "utf8"));
    assert.ok(Array.isArray(example) && example.length > 0);
    for (const row of example) {
      assert.deepEqual(validateWidgetRow(row), []);
    }
  });

  it("is referenced by the matrix spec for every pinned widget", () => {
    const spec = fs.readFileSync(path.join(FRONTEND_DIR, "published-widget-matrix.spec.js"), "utf8");
    assert.ok(spec.includes("WIDGETS.forEach"), "data-driven over the pinned inventory");
    assert.ok(spec.includes("zero missing resources"), "static case declared");
    assert.ok(spec.includes("published-widget-matrix.json"), "matrix artifact written");
    assert.ok(spec.includes("PUBLISHED_SITE_URL"), "skip contract declared");
  });
});
