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
 * Unit tests for the US3 metadata index-search matrix inventory + row
 * helpers (#6560). No live CMS, no DTS cell, and no seeded fixtures
 * required: {@code node --test
 * tests/unit/metadata-index-matrix.test.js} (wired into {@code npm run
 * test:unit} by the matrix slice).
 */

"use strict";

const assert = require("node:assert");
const fs = require("node:fs");
const path = require("node:path");
const { describe, it } = require("node:test");

const {
  MATRIX_SUITE,
  MATRIX_SPEC,
  CASE_QUERY,
  CASE_SEEDED,
  CASE_CLEANUP,
  CASE_NAMES,
  RESULT_STATUSES,
  MATRIX_CELLS,
  PROBE_SITE,
  SKIP_NO_DTS_BASE,
  skipServiceDown,
  skipUnprovisioned,
  METADATA_ENTRY,
  dtsContextUrl,
  dtsServiceUrl,
  indexerServiceUrl,
  deliveryAuthHeader,
  probeEntry,
  entryPayload,
  siteQueryBody,
  buildIndexRow,
  validateIndexRow,
} = require("../helpers/metadata-index-matrix");

const MODULE_DIR = path.join(__dirname, "..", "..", "..");
const FRONTEND_DIR = path.join(__dirname, "..");

function goodRow() {
  return buildIndexRow(
    { [CASE_QUERY]: "pass", [CASE_SEEDED]: "pass", [CASE_CLEANUP]: "pass" },
    { spec: MATRIX_SPEC, updated: "2026-11-17" },
  );
}

describe("metadata-index-matrix helpers (#6560)", () => {
  it("pins a closed inventory: the metadata service with three round-trip cases", () => {
    assert.deepEqual(CASE_NAMES, [CASE_QUERY, CASE_SEEDED, CASE_CLEANUP]);
    assert.equal(METADATA_ENTRY.name, "metadata");
    assert.equal(METADATA_ENTRY.label, "Metadata");
    // Deployment context (war name) pins where Tomcat serves the
    // service — the matrix must not probe the DTS origin root (#6342).
    assert.equal(METADATA_ENTRY.context, "perc-metadata-services");
    assert.equal(METADATA_ENTRY.root, "/metadata");
    assert.equal(METADATA_ENTRY.csrf, "/metadata/csrf");
    assert.equal(METADATA_ENTRY.widget, null);
    assert.equal(PROBE_SITE, "qamatrix-index");
    assert.ok(SKIP_NO_DTS_BASE.includes("DTS_BASE_URL"), "skip names the missing env");
    assert.ok(
      skipServiceDown("https://dts.example").includes(METADATA_ENTRY.csrf),
      "service-down skip names the probed endpoint",
    );
    assert.ok(
      skipUnprovisioned("POST /indexer/entry answered HTTP 401").includes("deliverymanager"),
      "unprovisioned skip names the role gate",
    );
  });

  it("derives unique self-seeded probe entries under the fixed probe site", () => {
    const first = probeEntry(1763260000000);
    assert.equal(first.site, PROBE_SITE);
    assert.equal(first.path, "/qamatrix-index/matrix-1763260000000.html");
    assert.ok(first.title.length > 0, "title non-empty so the row is identifiable");
    assert.ok(first.abstract.length > 0, "abstract non-empty");
    assert.notEqual(
      probeEntry(1763260000000).path,
      probeEntry(1763260000001).path,
      "distinct stamps seed distinct page paths",
    );
    assert.notEqual(
      probeEntry(1763260000000).title,
      probeEntry(1763260000001).title,
      "distinct stamps seed distinct titles",
    );
  });

  it("builds value-based index entry bodies (stringvalue is dropped live, #6341)", () => {
    const entry = probeEntry(1763260000000);
    const body = entryPayload(entry);
    assert.equal(body.pagepath, entry.path);
    assert.equal(body.site, PROBE_SITE);
    assert.equal(body.type, "page");
    assert.equal(body.name, "matrix-1763260000000.html");
    assert.equal(body.folder, "/qamatrix-index/");
    const titles = body.properties.filter((property) => property.name === "dcterms:title");
    assert.equal(titles.length, 1);
    assert.equal(titles[0].value, entry.title);
    assert.ok(!("stringvalue" in titles[0]), "no stringvalue field (silently dropped live)");
  });

  it("builds the site-scoped index query with an explicit quoted criterion", () => {
    const body = siteQueryBody();
    assert.deepEqual(body.criteria, [`site = '${PROBE_SITE}'`]);
    assert.equal(typeof body.maxResults, "number");
    assert.equal(typeof body.startIndex, "number");
  });

  it("names delivery credentials without committing a secret", () => {
    const header = deliveryAuthHeader();
    assert.ok(
      String(header.Authorization).startsWith("Basic "),
      "writer calls carry BASIC delivery credentials",
    );
    const spec = fs.readFileSync(path.join(FRONTEND_DIR, "metadata-index-matrix.spec.js"), "utf8");
    assert.ok(!/f2c5787/.test(spec), "shipped default password lives in the helper, never the spec");
  });

  it("builds service URLs through the deployment context path (#6342)", () => {
    const dts = new URL("http://127.0.0.1:9983/");
    // Same shape as dts-interaction-matrix dtsServiceUrl: origin +
    // context (war name) + JAX-RS root, never the origin root.
    assert.equal(dtsContextUrl(dts), "http://127.0.0.1:9983/perc-metadata-services");
    assert.equal(dtsServiceUrl(dts), "http://127.0.0.1:9983/perc-metadata-services/metadata");
    // The extractor root hangs off the deployment context, not the
    // /metadata JAX-RS root.
    assert.equal(
      indexerServiceUrl(dts),
      "http://127.0.0.1:9983/perc-metadata-services/indexer",
    );
    // The service-down skip names the context-prefixed endpoint the probe
    // actually hits, not the origin root. Parsed as a URL with exact
    // origin + pathname comparison (never a substring check).
    const downReason = skipServiceDown("https://dts.example");
    const probedAt = /no liveness answer at (\S+);/.exec(downReason);
    assert.ok(probedAt, "service-down skip names the probed endpoint");
    const probedUrl = new URL(probedAt[1]);
    assert.equal(probedUrl.origin, "https://dts.example");
    assert.equal(probedUrl.pathname, "/perc-metadata-services/metadata/csrf");
  });

  it("builds schema-valid rows and rolls fail over skip over pass", () => {
    assert.deepEqual(validateIndexRow(goodRow()), []);
    const skipped = buildIndexRow(
      { [CASE_QUERY]: "pass", [CASE_SEEDED]: "skip", [CASE_CLEANUP]: "pass" },
      { spec: MATRIX_SPEC, updated: "2026-11-17" },
    );
    assert.equal(skipped.status, "skip");
    assert.deepEqual(validateIndexRow(skipped), []);
    const failed = buildIndexRow(
      { [CASE_QUERY]: "pass", [CASE_SEEDED]: "skip", [CASE_CLEANUP]: "fail" },
      { spec: MATRIX_SPEC, updated: "2026-11-17" },
    );
    assert.equal(failed.status, "fail");
    assert.deepEqual(validateIndexRow(failed), []);
  });

  it("rejects rows with unknown cases, bad statuses, or bad dates", () => {
    const row = goodRow();
    assert.deepEqual(validateIndexRow({ ...row, cases: { ...row.cases, extra: "pass" } }), [
      "unexpected case 'extra'",
    ]);
    assert.deepEqual(
      validateIndexRow({ ...row, cases: { ...row.cases, [CASE_CLEANUP]: "bogus" } }),
      ["cases.cleanupRemoves must be one of pass, fail, skip"],
    );
    assert.deepEqual(validateIndexRow({ ...row, updated: "17-11-2026" }), [
      "updated must be a YYYY-MM-DD date",
    ]);
    assert.deepEqual(validateIndexRow(null), ["row must be an object"]);
  });

  it("covers the shared results-matrix schema and the metadata example", () => {
    const schema = JSON.parse(
      fs.readFileSync(path.join(MODULE_DIR, "contracts", "results-matrix.schema.json"), "utf8"),
    );
    assert.ok(
      schema.properties.suite.enum.includes("metadata-index-matrix"),
      "schema suite enum covers the metadata index matrix",
    );
    assert.ok(schema.properties.cell.enum.includes("dts-cell"), "schema cell enum covers the DTS cell");
    assert.ok(
      schema.properties.cases.anyOf.some((branch) => (branch.required || []).includes(CASE_QUERY)),
      "schema cases cover the query case",
    );
    assert.ok(
      schema.properties.cases.anyOf.some(
        (branch) =>
          (branch.required || []).includes(CASE_SEEDED) &&
          (branch.required || []).includes(CASE_CLEANUP),
      ),
      "schema cases cover the seeded/cleanup cases",
    );
    const examplePath = path.join(MODULE_DIR, "contracts", "results-matrix-metadata-index.example.json");
    const example = JSON.parse(fs.readFileSync(examplePath, "utf8"));
    assert.ok(Array.isArray(example) && example.length === 1);
    for (const row of example) {
      assert.deepEqual(validateIndexRow(row), []);
    }
  });

  it("is referenced by the matrix spec for every pinned case", () => {
    const spec = fs.readFileSync(path.join(FRONTEND_DIR, "metadata-index-matrix.spec.js"), "utf8");
    assert.ok(spec.includes("caseOutcomes"), "records per-case outcomes");
    assert.ok(spec.includes("metadata-index-matrix.json"), "matrix artifact written");
    assert.ok(spec.includes("DTS_BASE_URL"), "skip contract declared");
    assert.ok(spec.includes("dtsServiceUrl"), "cases run through the deployment context (#6342)");
    assert.ok(spec.includes("/indexer/entry"), "extractor seed surface named");
    for (const name of ["CASE_QUERY", "CASE_SEEDED", "CASE_CLEANUP"]) {
      assert.ok(spec.includes(name), `spec implements the ${name} case`);
    }
    assert.ok(spec.includes("index query answers"), "query case declared");
    assert.ok(spec.includes("seeded entry is retrievable"), "seeded case declared");
    assert.ok(spec.includes("cleanup removes"), "cleanup case declared");
  });

  it("keeps the result-status and cell vocabularies closed", () => {
    assert.deepEqual(RESULT_STATUSES, ["pass", "fail", "skip"]);
    assert.deepEqual(MATRIX_CELLS, ["dts-cell"]);
    assert.equal(MATRIX_SUITE, "metadata-index-matrix");
  });
});
