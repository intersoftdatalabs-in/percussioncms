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
 * Unit tests for the US3 forms confirmation matrix inventory + row helpers
 * (#6482). No live CMS, no DTS cell, and no seeded fixtures required:
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
  CASE_SUBMIT,
  CASE_SUMMARIES,
  CASE_CSV,
  CASE_NAMES,
  RESULT_STATUSES,
  MATRIX_CELLS,
  SKIP_NO_DTS_BASE,
  skipServiceDown,
  skipNoRetrieval,
  FORMS_ENTRY,
  dtsContextUrl,
  dtsServiceUrl,
  deliveryAuthHeader,
  probeFormName,
  retrievalDetail,
  summariesTotal,
  buildConfirmationRow,
  validateConfirmationRow,
} = require("../helpers/forms-confirmation-matrix");

const MODULE_DIR = path.join(__dirname, "..", "..", "..");
const FRONTEND_DIR = path.join(__dirname, "..");

function goodRow() {
  return buildConfirmationRow(
    { [CASE_SUBMIT]: "pass", [CASE_SUMMARIES]: "pass", [CASE_CSV]: "pass" },
    { spec: MATRIX_SPEC, updated: "2026-11-16" },
  );
}

describe("forms-confirmation-matrix helpers (#6482)", () => {
  it("pins a closed inventory: the forms service with three confirmation cases", () => {
    assert.deepEqual(CASE_NAMES, [CASE_SUBMIT, CASE_SUMMARIES, CASE_CSV]);
    assert.equal(FORMS_ENTRY.name, "forms");
    assert.equal(FORMS_ENTRY.label, "Forms");
    // Deployment context (war name) pins where Tomcat serves the
    // service — the matrix must not probe the DTS origin root (#6342).
    assert.equal(FORMS_ENTRY.context, "perc-form-processor");
    assert.equal(FORMS_ENTRY.root, "/forms");
    assert.equal(FORMS_ENTRY.csrf, "/forms/csrf");
    assert.equal(FORMS_ENTRY.widget, "percForm");
    assert.ok(SKIP_NO_DTS_BASE.includes("DTS_BASE_URL"), "skip names the missing env");
    assert.ok(
      skipServiceDown("https://dts.example").includes(FORMS_ENTRY.csrf),
      "service-down skip names the probed endpoint",
    );
    assert.ok(
      skipNoRetrieval("GET /forms/form/cms/qaconfirm-1 answered HTTP 401").includes(
        "DTS_DELIVERY_USER",
      ),
      "retrieval skip names the credential gap",
    );
    assert.ok(
      skipNoRetrieval("GET /forms/form/cms/qaconfirm-1 answered HTTP 302").includes(
        "#6375",
      ),
      "retrieval skip points at the cell provisioning owner",
    );
  });

  it("formats retrieval probe details with the redirect target when present (#6375)", () => {
    // A 302 skip must carry the CONFIDENTIAL redirect target so the skip
    // itself proves the redirect points at a container-local HTTPS port.
    assert.equal(
      retrievalDetail(
        "/forms/form/cms/qaconfirm-1",
        302,
        "https://127.0.0.1:8443/perc-form-processor/forms/form/cms/qaconfirm-1",
      ),
      "GET /forms/form/cms/qaconfirm-1 answered HTTP 302 " +
        "(location https://127.0.0.1:8443/perc-form-processor/forms/form/cms/qaconfirm-1)",
    );
    // Non-redirect statuses carry no location clause.
    assert.equal(
      retrievalDetail("/forms/form/cms/qaconfirm-1", 401),
      "GET /forms/form/cms/qaconfirm-1 answered HTTP 401",
    );
  });

  it("derives self-seeded probe form names valid per PSFormService.isValidFormName", () => {
    const name = probeFormName(1763260000000);
    assert.equal(name, "qaconfirm-1763260000000");
    assert.match(name, /^[a-zA-Z0-9_-]*$/, "probe name matches the service pattern");
  });

  it("builds service URLs through the deployment context path (#6342)", () => {
    const dts = new URL("http://127.0.0.1:9983/");
    // Same shape as dts-interaction-matrix dtsServiceUrl: origin +
    // context (war name) + JAX-RS root, never the origin root.
    assert.equal(
      dtsContextUrl(dts),
      "http://127.0.0.1:9983/perc-form-processor",
    );
    assert.equal(
      dtsServiceUrl(dts),
      "http://127.0.0.1:9983/perc-form-processor/forms",
    );
    // The service-down skip names the context-prefixed endpoint the probe
    // actually hits, not the origin root. Parsed as a URL with exact
    // origin + pathname comparison (never a substring check).
    const downReason = skipServiceDown("https://dts.example");
    const probedAt = /no liveness answer at (\S+);/.exec(downReason);
    assert.ok(probedAt, "service-down skip names the probed endpoint");
    const probedUrl = new URL(probedAt[1]);
    assert.equal(probedUrl.origin, "https://dts.example");
    assert.equal(probedUrl.pathname, "/perc-form-processor/forms/csrf");
  });

  it("builds the default deliverymanager BASIC auth header", () => {
    const header = deliveryAuthHeader();
    assert.ok(header.Authorization.startsWith("Basic "), "BASIC scheme");
    const decoded = Buffer.from(header.Authorization.slice("Basic ".length), "base64").toString(
      "utf8",
    );
    assert.ok(decoded.includes(":"), "user:password shape");
    assert.ok(decoded.startsWith("ps_manager:"), "shipped DTS development default user");
  });

  it("reads entry totals from either summaries shape (bean getter or JAXB field)", () => {
    // The container serializes the summary list through the bean getter
    // (summaries) or the JAXB field (formsInfo) depending on the JSON
    // provider — the retrieval case must accept both.
    assert.equal(
      summariesTotal(
        { summaries: [{ name: "qaconfirm-1", totalForms: 3 }] },
        "qaconfirm-1",
      ),
      3,
    );
    assert.equal(
      summariesTotal(
        { formsInfo: [{ name: "qaconfirm-1", totalForms: 3 }] },
        "qaconfirm-1",
      ),
      3,
    );
    // Unknown forms, non-number totals, and unparseable bodies read as
    // null so the spec fails with before/after totals, not a TypeError.
    assert.equal(
      summariesTotal({ summaries: [{ name: "other", totalForms: 1 }] }, "qaconfirm-1"),
      null,
    );
    assert.equal(
      summariesTotal({ summaries: [{ name: "qaconfirm-1" }] }, "qaconfirm-1"),
      null,
    );
    assert.equal(
      summariesTotal(
        { summaries: [{ name: "qaconfirm-1", totalForms: "3" }] },
        "qaconfirm-1",
      ),
      null,
    );
    assert.equal(summariesTotal(null, "qaconfirm-1"), null);
    assert.equal(summariesTotal({}, "qaconfirm-1"), null);
  });

  it("builds schema-valid rows and rolls fail over skip over pass", () => {
    assert.deepEqual(validateConfirmationRow(goodRow()), []);
    const skipped = buildConfirmationRow(
      { [CASE_SUBMIT]: "pass", [CASE_SUMMARIES]: "skip", [CASE_CSV]: "pass" },
      { spec: MATRIX_SPEC, updated: "2026-11-16" },
    );
    assert.equal(skipped.status, "skip");
    assert.deepEqual(validateConfirmationRow(skipped), []);
    const failed = buildConfirmationRow(
      { [CASE_SUBMIT]: "pass", [CASE_SUMMARIES]: "skip", [CASE_CSV]: "fail" },
      { spec: MATRIX_SPEC, updated: "2026-11-16" },
    );
    assert.equal(failed.status, "fail");
    assert.deepEqual(validateConfirmationRow(failed), []);
  });

  it("rejects rows with unknown cases, bad statuses, or bad dates", () => {
    const row = goodRow();
    assert.deepEqual(
      validateConfirmationRow({ ...row, cases: { ...row.cases, extra: "pass" } }),
      ["unexpected case 'extra'"],
    );
    assert.deepEqual(
      validateConfirmationRow({ ...row, cases: { ...row.cases, [CASE_CSV]: "bogus" } }),
      ["cases.csvRetrievable must be one of pass, fail, skip"],
    );
    assert.deepEqual(validateConfirmationRow({ ...row, updated: "16-11-2026" }), [
      "updated must be a YYYY-MM-DD date",
    ]);
    assert.deepEqual(validateConfirmationRow(null), ["row must be an object"]);
  });

  it("covers the shared results-matrix schema and the forms example", () => {
    const schema = JSON.parse(
      fs.readFileSync(path.join(MODULE_DIR, "contracts", "results-matrix.schema.json"), "utf8"),
    );
    assert.ok(
      schema.properties.suite.enum.includes("forms-confirmation-matrix"),
      "schema suite enum covers the forms confirmation matrix",
    );
    assert.ok(
      schema.properties.cell.enum.includes("dts-cell"),
      "schema cell enum covers the DTS cell",
    );
    assert.ok(
      schema.properties.cases.anyOf.some((branch) =>
        (branch.required || []).includes(CASE_SUBMIT),
      ),
      "schema cases cover the submit confirmation case",
    );
    assert.ok(
      schema.properties.cases.anyOf.some(
        (branch) =>
          (branch.required || []).includes(CASE_SUMMARIES) &&
          (branch.required || []).includes(CASE_CSV),
      ),
      "schema cases cover the retrieval cases",
    );
    const examplePath = path.join(MODULE_DIR, "contracts", "results-matrix-forms.example.json");
    const example = JSON.parse(fs.readFileSync(examplePath, "utf8"));
    assert.ok(Array.isArray(example) && example.length === 1);
    for (const row of example) {
      assert.deepEqual(validateConfirmationRow(row), []);
    }
  });

  it("is referenced by the matrix spec for every pinned case", () => {
    const spec = fs.readFileSync(
      path.join(FRONTEND_DIR, "forms-confirmation-matrix.spec.js"),
      "utf8",
    );
    assert.ok(spec.includes("caseOutcomes"), "records per-case outcomes");
    assert.ok(spec.includes("forms-confirmation-matrix.json"), "matrix artifact written");
    assert.ok(spec.includes("DTS_BASE_URL"), "skip contract declared");
    assert.ok(spec.includes("dtsServiceUrl"), "cases run through the deployment context (#6342)");
    assert.ok(spec.includes("DTS_DELIVERY_USER"), "retrieval credential gap named");
    for (const name of ["CASE_SUBMIT", "CASE_SUMMARIES", "CASE_CSV"]) {
      assert.ok(spec.includes(name), `spec implements the ${name} case`);
    }
    assert.ok(spec.includes("submit confirms"), "submit-confirm case declared");
    assert.ok(
      spec.includes("must answer 200 (HTTP "),
      "summaries-200 failure names the received status",
    );
    assert.ok(
      spec.includes("before=${beforeTotal}, after=${afterTotal}"),
      "missing-entry failure carries before/after totals",
    );
    assert.ok(
      spec.includes("marker=${marker}"),
      "csv-entry failure names the missing probe marker",
    );
    assert.ok(
      spec.includes("summaries count increments"),
      "summaries-retrievable case declared",
    );
    assert.ok(spec.includes("CSV export contains"), "csv-retrievable case declared");
    assert.ok(
      !spec.includes("function summariesTotal"),
      "no local summariesTotal copy; the spec uses the helper",
    );
  });
});
