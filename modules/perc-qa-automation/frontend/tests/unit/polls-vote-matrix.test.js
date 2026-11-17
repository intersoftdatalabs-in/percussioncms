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
 * Unit tests for the US3 polls vote matrix inventory + row helpers
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
  CASE_VOTE,
  CASE_PERSIST,
  CASE_RESULTS,
  CASE_NAMES,
  RESULT_STATUSES,
  MATRIX_CELLS,
  SKIP_NO_DTS_BASE,
  skipServiceDown,
  skipUnprovisioned,
  POLLS_ENTRY,
  dtsContextUrl,
  dtsServiceUrl,
  probePoll,
  isSuccessStatus,
  buildVoteRow,
  validateVoteRow,
} = require("../helpers/polls-vote-matrix");

const MODULE_DIR = path.join(__dirname, "..", "..", "..");
const FRONTEND_DIR = path.join(__dirname, "..");

function goodRow() {
  return buildVoteRow(
    { [CASE_VOTE]: "pass", [CASE_PERSIST]: "pass", [CASE_RESULTS]: "pass" },
    { spec: MATRIX_SPEC, updated: "2026-11-16" },
  );
}

describe("polls-vote-matrix helpers (#6241)", () => {
  it("pins a closed inventory: the polls service with three round-trip cases", () => {
    assert.deepEqual(CASE_NAMES, [CASE_VOTE, CASE_PERSIST, CASE_RESULTS]);
    assert.equal(POLLS_ENTRY.name, "polls");
    assert.equal(POLLS_ENTRY.label, "Polls");
    // Deployment context (war name) pins where Tomcat serves the
    // service — the matrix must not probe the DTS origin root (#6342).
    assert.equal(POLLS_ENTRY.context, "perc-polls-services");
    assert.equal(POLLS_ENTRY.root, "/polls");
    assert.equal(POLLS_ENTRY.csrf, "/polls/csrf");
    assert.equal(POLLS_ENTRY.widget, "percPoll");
    assert.ok(SKIP_NO_DTS_BASE.includes("DTS_BASE_URL"), "skip names the missing env");
    assert.ok(
      skipServiceDown("https://dts.example").includes(POLLS_ENTRY.csrf),
      "service-down skip names the probed endpoint",
    );
    assert.ok(
      skipUnprovisioned("POST /polls/save answered status 'ERROR'").includes("writable polls store"),
      "unprovisioned skip names the store gap",
    );
  });

  it("derives unique self-seeded probe polls with a votable choice", () => {
    const first = probePoll(1763260000000);
    assert.equal(first.pollName, "qamatrix-poll-1763260000000");
    assert.ok(first.pollQuestion.length > 0, "question non-empty per savePoll validation");
    assert.ok(first.choice.length > 0, "choice non-empty so pollSubmits records a vote");
    assert.notEqual(
      probePoll(1763260000000).pollName,
      probePoll(1763260000001).pollName,
      "distinct stamps never collide",
    );
    assert.notEqual(
      probePoll(1763260000000).pollQuestion,
      probePoll(1763260000001).pollQuestion,
      "distinct stamps seed distinct questions",
    );
  });

  it("normalizes polls SUCCESS statuses case-insensitively", () => {
    assert.equal(isSuccessStatus("SUCCESS"), true);
    assert.equal(isSuccessStatus("success"), true);
    assert.equal(isSuccessStatus("ERROR"), false);
    assert.equal(isSuccessStatus(null), false);
    assert.equal(isSuccessStatus(undefined), false);
  });

  it("builds service URLs through the deployment context path (#6342)", () => {
    const dts = new URL("http://127.0.0.1:9983/");
    // Same shape as dts-interaction-matrix dtsServiceUrl: origin +
    // context (war name) + JAX-RS root, never the origin root.
    assert.equal(dtsContextUrl(dts), "http://127.0.0.1:9983/perc-polls-services");
    assert.equal(dtsServiceUrl(dts), "http://127.0.0.1:9983/perc-polls-services/polls");
    // The service-down skip names the context-prefixed endpoint the probe
    // actually hits, not the origin root. Parsed as a URL with exact
    // origin + pathname comparison (never a substring check).
    const downReason = skipServiceDown("https://dts.example");
    const probedAt = /no liveness answer at (\S+);/.exec(downReason);
    assert.ok(probedAt, "service-down skip names the probed endpoint");
    const probedUrl = new URL(probedAt[1]);
    assert.equal(probedUrl.origin, "https://dts.example");
    assert.equal(probedUrl.pathname, "/perc-polls-services/polls/csrf");
  });

  it("builds schema-valid rows and rolls fail over skip over pass", () => {
    assert.deepEqual(validateVoteRow(goodRow()), []);
    const skipped = buildVoteRow(
      { [CASE_VOTE]: "pass", [CASE_PERSIST]: "skip", [CASE_RESULTS]: "pass" },
      { spec: MATRIX_SPEC, updated: "2026-11-16" },
    );
    assert.equal(skipped.status, "skip");
    assert.deepEqual(validateVoteRow(skipped), []);
    const failed = buildVoteRow(
      { [CASE_VOTE]: "pass", [CASE_PERSIST]: "skip", [CASE_RESULTS]: "fail" },
      { spec: MATRIX_SPEC, updated: "2026-11-16" },
    );
    assert.equal(failed.status, "fail");
    assert.deepEqual(validateVoteRow(failed), []);
  });

  it("rejects rows with unknown cases, bad statuses, or bad dates", () => {
    const row = goodRow();
    assert.deepEqual(validateVoteRow({ ...row, cases: { ...row.cases, extra: "pass" } }), [
      "unexpected case 'extra'",
    ]);
    assert.deepEqual(validateVoteRow({ ...row, cases: { ...row.cases, [CASE_RESULTS]: "bogus" } }), [
      "cases.resultsByQuestion must be one of pass, fail, skip",
    ]);
    assert.deepEqual(validateVoteRow({ ...row, updated: "16-11-2026" }), [
      "updated must be a YYYY-MM-DD date",
    ]);
    assert.deepEqual(validateVoteRow(null), ["row must be an object"]);
  });

  it("covers the shared results-matrix schema and the polls example", () => {
    const schema = JSON.parse(
      fs.readFileSync(path.join(MODULE_DIR, "contracts", "results-matrix.schema.json"), "utf8"),
    );
    assert.ok(
      schema.properties.suite.enum.includes("polls-vote-matrix"),
      "schema suite enum covers the polls vote matrix",
    );
    assert.ok(schema.properties.cell.enum.includes("dts-cell"), "schema cell enum covers the DTS cell");
    assert.ok(
      schema.properties.cases.anyOf.some((branch) => (branch.required || []).includes(CASE_VOTE)),
      "schema cases cover the vote case",
    );
    assert.ok(
      schema.properties.cases.anyOf.some(
        (branch) =>
          (branch.required || []).includes(CASE_PERSIST) &&
          (branch.required || []).includes(CASE_RESULTS),
      ),
      "schema cases cover the persist/results cases",
    );
    const examplePath = path.join(MODULE_DIR, "contracts", "results-matrix-polls.example.json");
    const example = JSON.parse(fs.readFileSync(examplePath, "utf8"));
    assert.ok(Array.isArray(example) && example.length === 1);
    for (const row of example) {
      assert.deepEqual(validateVoteRow(row), []);
    }
  });

  it("is referenced by the matrix spec for every pinned case", () => {
    const spec = fs.readFileSync(path.join(FRONTEND_DIR, "polls-vote-matrix.spec.js"), "utf8");
    assert.ok(spec.includes("caseOutcomes"), "records per-case outcomes");
    assert.ok(spec.includes("polls-vote-matrix.json"), "matrix artifact written");
    assert.ok(spec.includes("DTS_BASE_URL"), "skip contract declared");
    assert.ok(spec.includes("dtsServiceUrl"), "cases run through the deployment context (#6342)");
    assert.ok(spec.includes("pollSubmits"), "vote payload shape named");
    for (const name of ["CASE_VOTE", "CASE_PERSIST", "CASE_RESULTS"]) {
      assert.ok(spec.includes(name), `spec implements the ${name} case`);
    }
    assert.ok(spec.includes("vote creates the poll"), "vote case declared");
    assert.ok(spec.includes("vote persists"), "persistence case declared");
    assert.ok(spec.includes("results surface reflects the votes"), "results case declared");
  });

  it("keeps the result-status and cell vocabularies closed", () => {
    assert.deepEqual(RESULT_STATUSES, ["pass", "fail", "skip"]);
    assert.deepEqual(MATRIX_CELLS, ["dts-cell"]);
    assert.equal(MATRIX_SUITE, "polls-vote-matrix");
  });
});
