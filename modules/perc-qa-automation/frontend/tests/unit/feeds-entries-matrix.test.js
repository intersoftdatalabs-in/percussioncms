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
 * Unit tests for the US3 feeds entries matrix inventory + row helpers
 * (#6560). No live CMS, no DTS cell, and no seeded fixtures required:
 * {@code node --test tests/unit/feeds-entries-matrix.test.js} (wired
 * into {@code npm run test:unit} by the matrix slice).
 */

"use strict";

const assert = require("node:assert");
const fs = require("node:fs");
const path = require("node:path");
const { describe, it } = require("node:test");

const {
  MATRIX_SUITE,
  MATRIX_SPEC,
  CASE_DESCRIPTOR,
  CASE_ENTRIES,
  CASE_UNSEEDED,
  CASE_NAMES,
  RESULT_STATUSES,
  MATRIX_CELLS,
  SKIP_NO_DTS_BASE,
  skipServiceDown,
  skipUnprovisioned,
  FEEDS_ENTRY,
  METADATA_CONTEXT,
  dtsContextUrl,
  dtsServiceUrl,
  indexerServiceUrl,
  deliveryAuthHeader,
  probeFeed,
  descriptorPayload,
  indexEntryPayload,
  feedUrl,
  buildEntriesRow,
  validateEntriesRow,
} = require("../helpers/feeds-entries-matrix");

const MODULE_DIR = path.join(__dirname, "..", "..", "..");
const FRONTEND_DIR = path.join(__dirname, "..");

function goodRow() {
  return buildEntriesRow(
    { [CASE_DESCRIPTOR]: "pass", [CASE_ENTRIES]: "pass", [CASE_UNSEEDED]: "pass" },
    { spec: MATRIX_SPEC, updated: "2026-11-17" },
  );
}

describe("feeds-entries-matrix helpers (#6560)", () => {
  it("pins a closed inventory: the feeds service with three round-trip cases", () => {
    assert.deepEqual(CASE_NAMES, [CASE_DESCRIPTOR, CASE_ENTRIES, CASE_UNSEEDED]);
    assert.equal(FEEDS_ENTRY.name, "feeds");
    assert.equal(FEEDS_ENTRY.label, "Feeds");
    // Deployment context (war name) pins where Tomcat serves the
    // service — the matrix must not probe the DTS origin root (#6342).
    assert.equal(FEEDS_ENTRY.context, "feeds");
    assert.equal(FEEDS_ENTRY.root, "/rss");
    assert.equal(FEEDS_ENTRY.csrf, "/rss/csrf");
    assert.equal(FEEDS_ENTRY.widget, "percRss");
    assert.ok(SKIP_NO_DTS_BASE.includes("DTS_BASE_URL"), "skip names the missing env");
    assert.ok(
      skipServiceDown("https://dts.example").includes(FEEDS_ENTRY.csrf),
      "service-down skip names the probed endpoint",
    );
    assert.ok(
      skipUnprovisioned("PUT /rss/descriptors answered HTTP 401").includes("deliverymanager"),
      "unprovisioned skip names the role gate",
    );
  });

  it("derives unique self-seeded probe feeds with two entries", () => {
    const first = probeFeed(1763260000000);
    assert.equal(first.site, "qamatrix-feed-1763260000000");
    assert.equal(first.name, "qamatrix-entries-1763260000000");
    assert.ok(first.title.length > 0, "title non-empty so the feed carries it");
    assert.equal(first.entries.length, 2);
    for (const entry of first.entries) {
      assert.ok(entry.path.startsWith(`/${first.site}/`), "entry path scoped to the probe site");
      assert.ok(entry.title.length > 0, "entry title non-empty");
      assert.ok(entry.abstract.length > 0, "entry abstract non-empty");
    }
    assert.notEqual(
      probeFeed(1763260000000).site,
      probeFeed(1763260000001).site,
      "distinct stamps seed distinct probe sites (PUT scopes per site)",
    );
    assert.notEqual(
      probeFeed(1763260000000).entries[0].title,
      probeFeed(1763260000001).entries[0].title,
      "distinct stamps seed distinct entry titles",
    );
  });

  it("builds the PSFeedDescriptors PUT body with a site-scoped query", () => {
    const feed = probeFeed(1763260000000);
    const body = descriptorPayload(feed);
    assert.equal(body.site, feed.site);
    assert.equal(body.descriptors.length, 1);
    const descriptor = body.descriptors[0];
    assert.equal(descriptor.name, feed.name);
    assert.equal(descriptor.site, feed.site);
    assert.equal(descriptor.type, "RSS");
    const query = JSON.parse(descriptor.query);
    assert.deepEqual(query.criteria, [`site = '${feed.site}'`]);
    assert.equal(typeof query.maxResults, "number");
    assert.equal(typeof query.startIndex, "number");
  });

  it("builds value-based index entry bodies (stringvalue is dropped live, #6341)", () => {
    const feed = probeFeed(1763260000000);
    const body = indexEntryPayload(feed.entries[0], feed.site);
    assert.equal(body.pagepath, feed.entries[0].path);
    assert.equal(body.site, feed.site);
    assert.equal(body.type, "page");
    const titles = body.properties.filter((property) => property.name === "dcterms:title");
    assert.equal(titles.length, 1);
    assert.equal(titles[0].value, feed.entries[0].title);
    assert.ok(!("stringvalue" in titles[0]), "no stringvalue field (silently dropped live)");
  });

  it("names delivery credentials without committing a secret", () => {
    const header = deliveryAuthHeader();
    assert.ok(
      String(header.Authorization).startsWith("Basic "),
      "writer calls carry BASIC delivery credentials",
    );
    const spec = fs.readFileSync(path.join(FRONTEND_DIR, "feeds-entries-matrix.spec.js"), "utf8");
    assert.ok(!/f2c5787/.test(spec), "shipped default password lives in the helper, never the spec");
  });

  it("builds service URLs through the deployment context path (#6342)", () => {
    const dts = new URL("http://127.0.0.1:9983/");
    // Same shape as dts-interaction-matrix dtsServiceUrl: origin +
    // context (war name) + JAX-RS root, never the origin root.
    assert.equal(dtsContextUrl(dts), "http://127.0.0.1:9983/feeds");
    assert.equal(dtsServiceUrl(dts), "http://127.0.0.1:9983/feeds/rss");
    // The extractor root hangs off the metadata deployment context, not
    // the /metadata JAX-RS root.
    assert.equal(
      indexerServiceUrl(dts),
      `http://127.0.0.1:9983/${METADATA_CONTEXT}/indexer`,
    );
    const feed = probeFeed(1763260000000);
    assert.equal(
      feedUrl(dtsServiceUrl(dts), feed),
      `http://127.0.0.1:9983/feeds/rss/${feed.site}/${feed.name}/localhost`,
    );
    // The service-down skip names the context-prefixed endpoint the probe
    // actually hits, not the origin root. Parsed as a URL with exact
    // origin + pathname comparison (never a substring check).
    const downReason = skipServiceDown("https://dts.example");
    const probedAt = /no liveness answer at (\S+);/.exec(downReason);
    assert.ok(probedAt, "service-down skip names the probed endpoint");
    const probedUrl = new URL(probedAt[1]);
    assert.equal(probedUrl.origin, "https://dts.example");
    assert.equal(probedUrl.pathname, "/feeds/rss/csrf");
  });

  it("builds schema-valid rows and rolls fail over skip over pass", () => {
    assert.deepEqual(validateEntriesRow(goodRow()), []);
    const skipped = buildEntriesRow(
      { [CASE_DESCRIPTOR]: "pass", [CASE_ENTRIES]: "skip", [CASE_UNSEEDED]: "pass" },
      { spec: MATRIX_SPEC, updated: "2026-11-17" },
    );
    assert.equal(skipped.status, "skip");
    assert.deepEqual(validateEntriesRow(skipped), []);
    const failed = buildEntriesRow(
      { [CASE_DESCRIPTOR]: "pass", [CASE_ENTRIES]: "skip", [CASE_UNSEEDED]: "fail" },
      { spec: MATRIX_SPEC, updated: "2026-11-17" },
    );
    assert.equal(failed.status, "fail");
    assert.deepEqual(validateEntriesRow(failed), []);
  });

  it("rejects rows with unknown cases, bad statuses, or bad dates", () => {
    const row = goodRow();
    assert.deepEqual(validateEntriesRow({ ...row, cases: { ...row.cases, extra: "pass" } }), [
      "unexpected case 'extra'",
    ]);
    assert.deepEqual(
      validateEntriesRow({ ...row, cases: { ...row.cases, [CASE_ENTRIES]: "bogus" } }),
      ["cases.entriesServe must be one of pass, fail, skip"],
    );
    assert.deepEqual(validateEntriesRow({ ...row, updated: "17-11-2026" }), [
      "updated must be a YYYY-MM-DD date",
    ]);
    assert.deepEqual(validateEntriesRow(null), ["row must be an object"]);
  });

  it("covers the shared results-matrix schema and the feeds example", () => {
    const schema = JSON.parse(
      fs.readFileSync(path.join(MODULE_DIR, "contracts", "results-matrix.schema.json"), "utf8"),
    );
    assert.ok(
      schema.properties.suite.enum.includes("feeds-entries-matrix"),
      "schema suite enum covers the feeds entries matrix",
    );
    assert.ok(schema.properties.cell.enum.includes("dts-cell"), "schema cell enum covers the DTS cell");
    assert.ok(
      schema.properties.cases.anyOf.some((branch) => (branch.required || []).includes(CASE_DESCRIPTOR)),
      "schema cases cover the descriptor case",
    );
    assert.ok(
      schema.properties.cases.anyOf.some(
        (branch) =>
          (branch.required || []).includes(CASE_ENTRIES) &&
          (branch.required || []).includes(CASE_UNSEEDED),
      ),
      "schema cases cover the entries/unseeded cases",
    );
    const examplePath = path.join(MODULE_DIR, "contracts", "results-matrix-feeds.example.json");
    const example = JSON.parse(fs.readFileSync(examplePath, "utf8"));
    assert.ok(Array.isArray(example) && example.length === 1);
    for (const row of example) {
      assert.deepEqual(validateEntriesRow(row), []);
    }
  });

  it("is referenced by the matrix spec for every pinned case", () => {
    const spec = fs.readFileSync(path.join(FRONTEND_DIR, "feeds-entries-matrix.spec.js"), "utf8");
    assert.ok(spec.includes("caseOutcomes"), "records per-case outcomes");
    assert.ok(spec.includes("feeds-entries-matrix.json"), "matrix artifact written");
    assert.ok(spec.includes("DTS_BASE_URL"), "skip contract declared");
    assert.ok(spec.includes("dtsServiceUrl"), "cases run through the deployment context (#6342)");
    assert.ok(spec.includes("/descriptors"), "descriptor PUT surface named");
    for (const name of ["CASE_DESCRIPTOR", "CASE_ENTRIES", "CASE_UNSEEDED"]) {
      assert.ok(spec.includes(name), `spec implements the ${name} case`);
    }
    assert.ok(spec.includes("descriptor creates"), "descriptor case declared");
    assert.ok(spec.includes("entries serve"), "entries case declared");
    assert.ok(spec.includes("unseeded name redirects"), "unseeded case declared");
  });

  it("keeps the result-status and cell vocabularies closed", () => {
    assert.deepEqual(RESULT_STATUSES, ["pass", "fail", "skip"]);
    assert.deepEqual(MATRIX_CELLS, ["dts-cell"]);
    assert.equal(MATRIX_SUITE, "feeds-entries-matrix");
  });
});
