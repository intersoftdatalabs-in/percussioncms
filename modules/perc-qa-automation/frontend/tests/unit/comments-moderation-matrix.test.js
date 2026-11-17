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
 * Unit tests for the US3 comments moderation matrix inventory + row
 * helpers (#6485). No live CMS, no DTS cell, and no seeded fixtures
 * required: {@code npm run test:unit}.
 */

"use strict";

const assert = require("node:assert");
const fs = require("node:fs");
const path = require("node:path");
const { describe, it } = require("node:test");

const {
  MATRIX_SUITE,
  MATRIX_SPEC,
  CASE_SUBMIT,
  CASE_APPROVE,
  CASE_REJECT,
  CASE_NAMES,
  RESULT_STATUSES,
  MATRIX_CELLS,
  SKIP_NO_DTS_BASE,
  skipServiceDown,
  skipNoModeration,
  COMMENTS_ENTRY,
  dtsContextUrl,
  dtsServiceUrl,
  deliveryAuthHeader,
  probeThread,
  buildModerationRow,
  validateModerationRow,
} = require("../helpers/comments-moderation-matrix");

const MODULE_DIR = path.join(__dirname, "..", "..", "..");
const FRONTEND_DIR = path.join(__dirname, "..");

function goodRow() {
  return buildModerationRow(
    { [CASE_SUBMIT]: "pass", [CASE_APPROVE]: "pass", [CASE_REJECT]: "pass" },
    { spec: MATRIX_SPEC, updated: "2026-11-17" },
  );
}

describe("comments-moderation-matrix helpers (#6485)", () => {
  it("pins a closed inventory: the comments service with three moderation cases", () => {
    assert.deepEqual(CASE_NAMES, [CASE_SUBMIT, CASE_APPROVE, CASE_REJECT]);
    assert.equal(COMMENTS_ENTRY.name, "comments");
    assert.equal(COMMENTS_ENTRY.label, "Comments");
    // Deployment context (war name) pins where Tomcat serves the
    // service — the matrix must not probe the DTS origin root (#6342).
    assert.equal(COMMENTS_ENTRY.context, "perc-comments-services");
    assert.equal(COMMENTS_ENTRY.root, "/comment");
    assert.equal(COMMENTS_ENTRY.csrf, "/comment/csrf");
    assert.equal(COMMENTS_ENTRY.widget, "percComments");
    assert.ok(SKIP_NO_DTS_BASE.includes("DTS_BASE_URL"), "skip names the missing env");
    assert.ok(
      skipServiceDown("https://dts.example").includes(COMMENTS_ENTRY.csrf),
      "service-down skip names the probed endpoint",
    );
    assert.ok(
      skipNoModeration("PUT /comment/moderation/approve answered HTTP 401").includes(
        "DTS_DELIVERY_USER",
      ),
      "moderation skip names the credential gap",
    );
    assert.ok(
      skipNoModeration("queue unreachable").includes("#6375"),
      "moderation skip names the provisioning tracker",
    );
  });

  it("derives unique self-seeded probe threads with marker-shaped site and page", () => {
    const first = probeThread(1763260000000);
    assert.equal(first.site, "qamod-1763260000000");
    assert.equal(first.page, "/qamod-1763260000000");
    assert.equal(first.marker, "qamod-1763260000000");
    assert.ok(first.email.includes("@"), "email shape");
    assert.ok(first.email.includes(first.marker), "email carries the marker");
    assert.notEqual(
      probeThread(1763260000000).site,
      probeThread(1763260000001).site,
      "distinct stamps never collide",
    );
  });

  it("builds BASIC deliverymanager credentials with env override", () => {
    const header = deliveryAuthHeader();
    assert.ok(
      header.Authorization.startsWith("Basic "),
      "moderation calls carry a BASIC Authorization header",
    );
    const decoded = Buffer.from(header.Authorization.slice("Basic ".length), "base64").toString(
      "utf8",
    );
    assert.ok(decoded.includes(":"), "BASIC value is user:password shaped");
    assert.ok(decoded.startsWith("ps_manager:"), "default is the shipped DTS development user");
    process.env.DTS_DELIVERY_USER = "qa-manager";
    process.env.DTS_DELIVERY_PASSWORD = "qa-secret";
    try {
      const overridden = deliveryAuthHeader();
      const overDecoded = Buffer.from(
        overridden.Authorization.slice("Basic ".length),
        "base64",
      ).toString("utf8");
      assert.equal(overDecoded, "qa-manager:qa-secret", "cell-provisioned credentials win");
    } finally {
      delete process.env.DTS_DELIVERY_USER;
      delete process.env.DTS_DELIVERY_PASSWORD;
    }
  });

  it("builds service URLs through the deployment context path (#6342)", () => {
    const dts = new URL("http://127.0.0.1:9983/");
    // Same shape as dts-interaction-matrix dtsServiceUrl: origin +
    // context (war name) + JAX-RS root, never the origin root.
    assert.equal(dtsContextUrl(dts), "http://127.0.0.1:9983/perc-comments-services");
    assert.equal(
      dtsServiceUrl(dts),
      "http://127.0.0.1:9983/perc-comments-services/comment",
    );
    // The service-down skip names the context-prefixed endpoint the probe
    // actually hits, not the origin root. Parsed as a URL with exact
    // origin + pathname comparison (never a substring check).
    const downReason = skipServiceDown("https://dts.example");
    const probedAt = /no liveness answer at (\S+);/.exec(downReason);
    assert.ok(probedAt, "service-down skip names the probed endpoint");
    const probedUrl = new URL(probedAt[1]);
    assert.equal(probedUrl.origin, "https://dts.example");
    assert.equal(probedUrl.pathname, "/perc-comments-services/comment/csrf");
  });

  it("builds schema-valid rows and rolls fail over skip over pass", () => {
    assert.deepEqual(validateModerationRow(goodRow()), []);
    const skipped = buildModerationRow(
      { [CASE_SUBMIT]: "pass", [CASE_APPROVE]: "skip", [CASE_REJECT]: "pass" },
      { spec: MATRIX_SPEC, updated: "2026-11-17" },
    );
    assert.equal(skipped.status, "skip");
    assert.deepEqual(validateModerationRow(skipped), []);
    const failed = buildModerationRow(
      { [CASE_SUBMIT]: "pass", [CASE_APPROVE]: "skip", [CASE_REJECT]: "fail" },
      { spec: MATRIX_SPEC, updated: "2026-11-17" },
    );
    assert.equal(failed.status, "fail");
    assert.deepEqual(validateModerationRow(failed), []);
  });

  it("rejects rows with unknown cases, bad statuses, or bad dates", () => {
    const row = goodRow();
    assert.deepEqual(
      validateModerationRow({ ...row, cases: { ...row.cases, extra: "pass" } }),
      ["unexpected case 'extra'"],
    );
    assert.deepEqual(
      validateModerationRow({ ...row, cases: { ...row.cases, [CASE_REJECT]: "bogus" } }),
      ["cases.rejectHides must be one of pass, fail, skip"],
    );
    assert.deepEqual(validateModerationRow({ ...row, updated: "17-11-2026" }), [
      "updated must be a YYYY-MM-DD date",
    ]);
    assert.deepEqual(validateModerationRow(null), ["row must be an object"]);
  });

  it("covers the shared results-matrix schema and the moderation example", () => {
    const schema = JSON.parse(
      fs.readFileSync(path.join(MODULE_DIR, "contracts", "results-matrix.schema.json"), "utf8"),
    );
    assert.ok(
      schema.properties.suite.enum.includes("comments-moderation-matrix"),
      "schema suite enum covers the comments moderation matrix",
    );
    assert.ok(
      schema.properties.cell.enum.includes("dts-cell"),
      "schema cell enum covers the DTS cell",
    );
    assert.ok(
      schema.properties.cases.anyOf.some((branch) =>
        (branch.required || []).includes(CASE_SUBMIT),
      ),
      "schema cases cover the submit case",
    );
    assert.ok(
      schema.properties.cases.anyOf.some(
        (branch) =>
          (branch.required || []).includes(CASE_APPROVE) &&
          (branch.required || []).includes(CASE_REJECT),
      ),
      "schema cases cover the approve/reject cases",
    );
    const examplePath = path.join(
      MODULE_DIR,
      "contracts",
      "results-matrix-comments-moderation.example.json",
    );
    const example = JSON.parse(fs.readFileSync(examplePath, "utf8"));
    assert.ok(Array.isArray(example) && example.length === 1);
    for (const row of example) {
      assert.deepEqual(validateModerationRow(row), []);
    }
  });

  it("is referenced by the matrix spec for every pinned case", () => {
    const spec = fs.readFileSync(
      path.join(FRONTEND_DIR, "comments-moderation-matrix.spec.js"),
      "utf8",
    );
    assert.ok(spec.includes("caseOutcomes"), "records per-case outcomes");
    assert.ok(spec.includes("comments-moderation-matrix.json"), "matrix artifact written");
    assert.ok(spec.includes("DTS_BASE_URL"), "skip contract declared");
    assert.ok(spec.includes("dtsServiceUrl"), "cases run through the deployment context (#6342)");
    assert.ok(spec.includes("moderation/${verb}"), "moderation surface exercised");
    assert.ok(spec.includes('"approve"'), "approve verb passed");
    assert.ok(spec.includes('"reject"'), "reject verb passed");
    assert.ok(spec.includes("moderation/asmoderator"), "moderator queue resolves probe ids");
    for (const name of ["CASE_SUBMIT", "CASE_APPROVE", "CASE_REJECT"]) {
      assert.ok(spec.includes(name), `spec implements the ${name} case`);
    }
    assert.ok(spec.includes("submit lands in the moderation queue"), "submit case declared");
    assert.ok(spec.includes("approve publishes"), "approve round-trip case declared");
    assert.ok(spec.includes("reject hides"), "reject case declared");
  });

  it("keeps the result-status and cell vocabularies closed", () => {
    assert.deepEqual(RESULT_STATUSES, ["pass", "fail", "skip"]);
    assert.deepEqual(MATRIX_CELLS, ["dts-cell"]);
    assert.equal(MATRIX_SUITE, "comments-moderation-matrix");
  });
});
