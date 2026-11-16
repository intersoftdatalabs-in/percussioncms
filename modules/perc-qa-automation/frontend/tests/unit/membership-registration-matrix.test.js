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
 * Unit tests for the US3 membership registration matrix inventory + row
 * helpers (#6484). No live CMS, no DTS cell, and no seeded fixtures
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
  CASE_REGISTER,
  CASE_LOGIN,
  CASE_LOGOUT,
  CASE_NAMES,
  RESULT_STATUSES,
  MATRIX_CELLS,
  SKIP_NO_DTS_BASE,
  skipServiceDown,
  skipNoRegistration,
  MEMBERSHIP_ENTRY,
  dtsContextUrl,
  dtsServiceUrl,
  probeAccount,
  isSuccessStatus,
  buildRegistrationRow,
  validateRegistrationRow,
} = require("../helpers/membership-registration-matrix");

const MODULE_DIR = path.join(__dirname, "..", "..", "..");
const FRONTEND_DIR = path.join(__dirname, "..");

function goodRow() {
  return buildRegistrationRow(
    { [CASE_REGISTER]: "pass", [CASE_LOGIN]: "pass", [CASE_LOGOUT]: "pass" },
    { spec: MATRIX_SPEC, updated: "2026-11-16" },
  );
}

describe("membership-registration-matrix helpers (#6484)", () => {
  it("pins a closed inventory: the membership service with three round-trip cases", () => {
    assert.deepEqual(CASE_NAMES, [CASE_REGISTER, CASE_LOGIN, CASE_LOGOUT]);
    assert.equal(MEMBERSHIP_ENTRY.name, "membership");
    assert.equal(MEMBERSHIP_ENTRY.label, "Membership");
    // Deployment context (war name) pins where Tomcat serves the
    // service — the matrix must not probe the DTS origin root (#6342).
    assert.equal(MEMBERSHIP_ENTRY.context, "perc-membership-services");
    assert.equal(MEMBERSHIP_ENTRY.root, "/membership");
    assert.equal(MEMBERSHIP_ENTRY.csrf, "/membership/csrf");
    assert.equal(MEMBERSHIP_ENTRY.widget, "percLogin");
    assert.ok(SKIP_NO_DTS_BASE.includes("DTS_BASE_URL"), "skip names the missing env");
    assert.ok(
      skipServiceDown("https://dts.example").includes(MEMBERSHIP_ENTRY.csrf),
      "service-down skip names the probed endpoint",
    );
    assert.ok(
      skipNoRegistration("POST /membership/user answered status 'AUTH_FAILED'").includes(
        "confirmationRequired=false",
      ),
      "registration skip names the policy gap",
    );
  });

  it("derives unique self-seeded probe accounts with non-empty credentials", () => {
    const first = probeAccount(1763260000000);
    assert.equal(first.email, "qamember-1763260000000@example.com");
    assert.ok(first.email.includes("@"), "email shape");
    assert.ok(first.password.length > 0, "password non-empty per createUser validation");
    assert.notEqual(
      probeAccount(1763260000000).email,
      probeAccount(1763260000001).email,
      "distinct stamps never collide",
    );
  });

  it("normalizes membership SUCCESS statuses case-insensitively", () => {
    assert.equal(isSuccessStatus("SUCCESS"), true);
    assert.equal(isSuccessStatus("success"), true);
    assert.equal(isSuccessStatus("MEMBER_EXISTS"), false);
    assert.equal(isSuccessStatus("AUTH_FAILED"), false);
    assert.equal(isSuccessStatus(null), false);
    assert.equal(isSuccessStatus(undefined), false);
  });

  it("builds service URLs through the deployment context path (#6342)", () => {
    const dts = new URL("http://127.0.0.1:9983/");
    // Same shape as dts-interaction-matrix dtsServiceUrl: origin +
    // context (war name) + JAX-RS root, never the origin root.
    assert.equal(
      dtsContextUrl(dts),
      "http://127.0.0.1:9983/perc-membership-services",
    );
    assert.equal(
      dtsServiceUrl(dts),
      "http://127.0.0.1:9983/perc-membership-services/membership",
    );
    // The service-down skip names the context-prefixed endpoint the probe
    // actually hits, not the origin root. Parsed as a URL with exact
    // origin + pathname comparison (never a substring check).
    const downReason = skipServiceDown("https://dts.example");
    const probedAt = /no liveness answer at (\S+);/.exec(downReason);
    assert.ok(probedAt, "service-down skip names the probed endpoint");
    const probedUrl = new URL(probedAt[1]);
    assert.equal(probedUrl.origin, "https://dts.example");
    assert.equal(probedUrl.pathname, "/perc-membership-services/membership/csrf");
  });

  it("builds schema-valid rows and rolls fail over skip over pass", () => {
    assert.deepEqual(validateRegistrationRow(goodRow()), []);
    const skipped = buildRegistrationRow(
      { [CASE_REGISTER]: "pass", [CASE_LOGIN]: "skip", [CASE_LOGOUT]: "pass" },
      { spec: MATRIX_SPEC, updated: "2026-11-16" },
    );
    assert.equal(skipped.status, "skip");
    assert.deepEqual(validateRegistrationRow(skipped), []);
    const failed = buildRegistrationRow(
      { [CASE_REGISTER]: "pass", [CASE_LOGIN]: "skip", [CASE_LOGOUT]: "fail" },
      { spec: MATRIX_SPEC, updated: "2026-11-16" },
    );
    assert.equal(failed.status, "fail");
    assert.deepEqual(validateRegistrationRow(failed), []);
  });

  it("rejects rows with unknown cases, bad statuses, or bad dates", () => {
    const row = goodRow();
    assert.deepEqual(
      validateRegistrationRow({ ...row, cases: { ...row.cases, extra: "pass" } }),
      ["unexpected case 'extra'"],
    );
    assert.deepEqual(
      validateRegistrationRow({ ...row, cases: { ...row.cases, [CASE_LOGOUT]: "bogus" } }),
      ["cases.logoutCloses must be one of pass, fail, skip"],
    );
    assert.deepEqual(validateRegistrationRow({ ...row, updated: "16-11-2026" }), [
      "updated must be a YYYY-MM-DD date",
    ]);
    assert.deepEqual(validateRegistrationRow(null), ["row must be an object"]);
  });

  it("covers the shared results-matrix schema and the membership example", () => {
    const schema = JSON.parse(
      fs.readFileSync(path.join(MODULE_DIR, "contracts", "results-matrix.schema.json"), "utf8"),
    );
    assert.ok(
      schema.properties.suite.enum.includes("membership-registration-matrix"),
      "schema suite enum covers the membership registration matrix",
    );
    assert.ok(
      schema.properties.cell.enum.includes("dts-cell"),
      "schema cell enum covers the DTS cell",
    );
    assert.ok(
      schema.properties.cases.anyOf.some((branch) =>
        (branch.required || []).includes(CASE_REGISTER),
      ),
      "schema cases cover the register case",
    );
    assert.ok(
      schema.properties.cases.anyOf.some(
        (branch) =>
          (branch.required || []).includes(CASE_LOGIN) &&
          (branch.required || []).includes(CASE_LOGOUT),
      ),
      "schema cases cover the login/logout cases",
    );
    const examplePath = path.join(MODULE_DIR, "contracts", "results-matrix-membership.example.json");
    const example = JSON.parse(fs.readFileSync(examplePath, "utf8"));
    assert.ok(Array.isArray(example) && example.length === 1);
    for (const row of example) {
      assert.deepEqual(validateRegistrationRow(row), []);
    }
  });

  it("is referenced by the matrix spec for every pinned case", () => {
    const spec = fs.readFileSync(
      path.join(FRONTEND_DIR, "membership-registration-matrix.spec.js"),
      "utf8",
    );
    assert.ok(spec.includes("caseOutcomes"), "records per-case outcomes");
    assert.ok(spec.includes("membership-registration-matrix.json"), "matrix artifact written");
    assert.ok(spec.includes("DTS_BASE_URL"), "skip contract declared");
    assert.ok(spec.includes("dtsServiceUrl"), "cases run through the deployment context (#6342)");
    assert.ok(spec.includes("confirmationRequired"), "registration policy gap named");
    for (const name of ["CASE_REGISTER", "CASE_LOGIN", "CASE_LOGOUT"]) {
      assert.ok(spec.includes(name), `spec implements the ${name} case`);
    }
    assert.ok(spec.includes("registration creates the account"), "register case declared");
    assert.ok(
      spec.includes("login round-trips"),
      "login round-trip case declared",
    );
    assert.ok(spec.includes("logout closes the session"), "logout case declared");
  });

  it("keeps the result-status and cell vocabularies closed", () => {
    assert.deepEqual(RESULT_STATUSES, ["pass", "fail", "skip"]);
    assert.deepEqual(MATRIX_CELLS, ["dts-cell"]);
    assert.equal(MATRIX_SUITE, "membership-registration-matrix");
  });
});
