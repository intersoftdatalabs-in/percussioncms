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
 * Closed inventory + results-matrix row helpers for the US3 forms
 * confirmation matrix (#6482 / parent #6238): submit a form against the
 * in-cell DTS forms service and prove the confirmation is retrievable.
 *
 * <p>Three pinned cases, one row:</p>
 * <ul>
 *   <li>{@code submitConfirm} — the open visitor surface: {@code POST
 *   <context>/forms/form/collect/} takes the success redirect (HTTP 302 to
 *   {@code /success.html}). No credentials needed.</li>
 *   <li>{@code summariesRetrievable} — the admin retrieval surface:
 *   {@code GET <context>/forms/form/cms/{form}} summaries report exactly
 *   one more submission after the probe submit than before.</li>
 *   <li>{@code csvRetrievable} — the admin export surface: {@code GET
 *   <context>/forms/form/cms/{form}/{csvFile}} returns {@code text/csv}
 *   containing the submitted probe marker.</li>
 * </ul>
 *
 * <p>The retrieval endpoints live behind the {@code deliverymanager} role
 * (BASIC, {@code CONFIDENTIAL} — see
 * {@code deliverytiersuite/delivery-tier-suite/forms/src/main/java/webapp/WEB-INF/web.xml}).
 * Credentials default to the shipped DTS development defaults (see
 * {@code delivery-tier-distribution/src/main/resources/etc/dts-realm.properties};
 * override with {@code DTS_DELIVERY_USER} / {@code DTS_DELIVERY_PASSWORD}
 * when the cell provisions its own). Without usable credentials — or without
 * a reachable DTS HTTPS port for the {@code CONFIDENTIAL} redirect — the two
 * retrieval cases skip with a named reason instead of failing, so the
 * default CMS-only cell and credential-less DTS cells stay green.</p>
 *
 * <p>Skip contract (mirrors the US3 interaction matrix): without
 * {@code DTS_BASE_URL} every case skips naming the missing cell (#6237);
 * with a base URL each case probes liveness ({@code HEAD
 * <context>/forms/csrf} — the service lives under its Tomcat deployment
 * context, issue #6342) before touching the service. Submissions are
 * self-seeded under a unique {@code qaconfirm-<stamp>} form name (valid per
 * {@code PSFormService.isValidFormName}), so no CMS fixture is needed.</p>
 *
 * <p>URL paths below use {@code /} (web origins, not OS file paths). File
 * paths are joined with {@code node:path} so the suite stays portable.</p>
 */

"use strict";

const path = require("node:path");

const MATRIX_SUITE = "forms-confirmation-matrix";
const MATRIX_SPEC = "tests/forms-confirmation-matrix.spec.js";
const SCHEMA_FILE = "results-matrix.schema.json";
const EXAMPLE_FILE = "results-matrix-forms.example.json";

const CASE_SUBMIT = "submitConfirm";
const CASE_SUMMARIES = "summariesRetrievable";
const CASE_CSV = "csvRetrievable";
const CASE_NAMES = Object.freeze([CASE_SUBMIT, CASE_SUMMARIES, CASE_CSV]);

const RESULT_STATUSES = Object.freeze(["pass", "fail", "skip"]);
const MATRIX_CELLS = Object.freeze(["dts-cell"]);

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/** Skip reason when no DTS-providing cell is configured. */
const SKIP_NO_DTS_BASE =
  "needs DTS_BASE_URL=(DTS-providing cell origin); " +
  "default CMS-only H2 QA cell serves no DTS services (#6237)";

/**
 * Skip reason when the DTS origin answers but the forms service does not.
 * A 401/403 still counts as "served" (the service is up, the caller lacks
 * the role); only unreachable / 404 probes land here. The probed endpoint
 * is the deployment context path (war name) plus the JAX-RS root — the
 * service is served under Tomcat contexts (issue #6342), not at the DTS
 * origin root.
 *
 * @param {string} baseUrl DTS origin that was probed
 * @returns {string}
 */
function skipServiceDown(baseUrl) {
  return (
    `forms confirmation (Forms): no liveness answer at ` +
    `${baseUrl}/${FORMS_ENTRY.context}${FORMS_ENTRY.csrf}; the DTS cell does not serve ` +
    `${FORMS_ENTRY.context}${FORMS_ENTRY.root} yet (#6237)`
  );
}

/**
 * Skip reason when the admin retrieval surface cannot be read in this
 * cell. The summaries/export endpoints are {@code deliverymanager}-gated
 * (BASIC, {@code CONFIDENTIAL}), so plain-HTTP cells without provisioned
 * credentials — or without a reachable DTS HTTPS port for the
 * {@code CONFIDENTIAL} redirect — skip here instead of failing.
 *
 * @param {string} detail what was probed and what the cell answered
 * @returns {string}
 */
function skipNoRetrieval(detail) {
  return (
    `forms confirmation (Forms): ${detail}; ` +
    `the admin retrieval surface is deliverymanager-gated — provision ` +
    `DTS_DELIVERY_USER / DTS_DELIVERY_PASSWORD and a reachable DTS HTTPS ` +
    `port on the DTS cell (parent #6238) to turn these cases green`
  );
}

/**
 * The pinned forms service entry. {@code context} is the DTS Tomcat
 * context path (war name {@code perc-form-processor}); {@code root} is the
 * JAX-RS root; {@code csrf} the {@code HEAD} liveness probe. Submissions
 * are self-seeded, so there is no fixture env — the probe form name is
 * derived per run ({@link probeFormName}).
 */
const FORMS_ENTRY = Object.freeze({
  name: "forms",
  label: "Forms",
  context: "perc-form-processor",
  root: "/forms",
  csrf: "/forms/csrf",
  widget: "percForm",
});

/**
 * @returns {URL|null} DTS-providing cell origin, or null when the cell is
 * CMS-only (default H2 QA cell: every case skips named)
 */
function dtsBaseUrl() {
  const baseUrl = process.env.DTS_BASE_URL || "";
  if (!baseUrl) {
    return null;
  }
  return new URL(baseUrl);
}

/**
 * Deployment context URL for the forms service: the DTS origin plus the
 * deployment context path (war name), without the JAX-RS root. Same shape
 * as {@code tests/helpers/dts-interaction-matrix.js#dtsContextUrl}.
 *
 * @param {URL} dtsOrigin DTS-providing cell origin from {@link dtsBaseUrl}
 * @returns {string} context URL (no trailing slash)
 */
function dtsContextUrl(dtsOrigin) {
  return `${dtsOrigin.origin}/${FORMS_ENTRY.context}`;
}

/**
 * Service base URL for the forms confirmation cases: the DTS origin plus
 * the deployment context path (war name) plus the JAX-RS root, mirroring
 * how the published form widget addresses the service. Same shape as
 * {@code tests/helpers/dts-interaction-matrix.js#dtsServiceUrl}.
 *
 * @param {URL} dtsOrigin DTS-providing cell origin from {@link dtsBaseUrl}
 * @returns {string} service base URL (no trailing slash)
 */
function dtsServiceUrl(dtsOrigin) {
  return `${dtsContextUrl(dtsOrigin)}${FORMS_ENTRY.root}`;
}

/**
 * BASIC credentials for the role-protected forms retrieval endpoints
 * ({@code deliverymanager} role). Defaults are the shipped DTS development
 * defaults (see
 * {@code delivery-tier-distribution/src/main/resources/etc/dts-realm.properties}
 * and the cargo {@code ps_manager} user in the DTS distribution pom);
 * override with env when the cell provisions its own.
 *
 * @returns {{ Authorization: string }} BASIC auth header value
 */
function deliveryAuthHeader() {
  const user = process.env.DTS_DELIVERY_USER || "ps_manager";
  const password =
    process.env.DTS_DELIVERY_PASSWORD || "f2c57870308dc87f432e5912d4de6f8e322721ba";
  return {
    Authorization: `Basic ${Buffer.from(`${user}:${password}`).toString("base64")}`,
  };
}

/**
 * Unique self-seeded probe form name for one confirmation run. Matches
 * {@code PSFormService.isValidFormName} ({@code ^[a-zA-Z0-9_\-]*$}).
 *
 * @param {number} [stamp] timestamp millis; defaults to now
 * @returns {string} probe form name
 */
function probeFormName(stamp) {
  const at = typeof stamp === "number" ? stamp : Date.now();
  return `qaconfirm-${at}`;
}

/**
 * Builds the one results-matrix row for the pinned forms entry. The row
 * shape mirrors {@code contracts/results-matrix.schema.json}; see
 * {@link validateConfirmationRow}.
 *
 * @param {{ submitConfirm: string, summariesRetrievable: string, csvRetrievable: string }} caseResults
 * @param {{ spec?: string, updated?: string }} [options]
 * @returns {object}
 */
function buildConfirmationRow(caseResults, options = {}) {
  const status = caseResults[CASE_SUBMIT] === "fail"
    || caseResults[CASE_SUMMARIES] === "fail"
    || caseResults[CASE_CSV] === "fail"
    ? "fail"
    : caseResults[CASE_SUBMIT] === "skip"
      || caseResults[CASE_SUMMARIES] === "skip"
      || caseResults[CASE_CSV] === "skip"
      ? "skip"
      : "pass";
  return {
    suite: MATRIX_SUITE,
    spec: options.spec || MATRIX_SPEC,
    contentType: FORMS_ENTRY.name,
    label: FORMS_ENTRY.label,
    cell: MATRIX_CELLS[0],
    cases: {
      [CASE_SUBMIT]: caseResults[CASE_SUBMIT],
      [CASE_SUMMARIES]: caseResults[CASE_SUMMARIES],
      [CASE_CSV]: caseResults[CASE_CSV],
    },
    status,
    updated: options.updated || new Date().toISOString().slice(0, 10),
  };
}

/**
 * Validates the scalar header of one forms confirmation results-matrix row.
 *
 * @param {object} row
 * @returns {string[]} validation errors
 */
function validateConfirmationRowHeader(row) {
  const errors = [];
  const allowed = ["suite", "spec", "contentType", "label", "cell", "cases", "status", "updated"];
  for (const key of Object.keys(row)) {
    if (!allowed.includes(key)) {
      errors.push(`unexpected property '${key}'`);
    }
  }
  if (row.suite !== MATRIX_SUITE) {
    errors.push(`suite must be '${MATRIX_SUITE}'`);
  }
  if (typeof row.spec !== "string" || row.spec.length === 0) {
    errors.push("spec must be a non-empty string");
  }
  if (row.contentType !== FORMS_ENTRY.name) {
    errors.push(`contentType must be '${FORMS_ENTRY.name}'`);
  }
  if (typeof row.label !== "string" || row.label.length === 0) {
    errors.push("label must be a non-empty string");
  }
  if (!MATRIX_CELLS.includes(row.cell)) {
    errors.push(`cell must be one of ${MATRIX_CELLS.join(", ")}`);
  }
  return errors;
}

/**
 * Validates the per-case block of one forms confirmation results-matrix row.
 *
 * @param {unknown} cases
 * @returns {string[]} validation errors
 */
function validateConfirmationRowCases(cases) {
  if (cases == null || typeof cases !== "object" || Array.isArray(cases)) {
    return ["cases must be an object"];
  }
  const errors = [];
  for (const key of Object.keys(cases)) {
    if (!CASE_NAMES.includes(key)) {
      errors.push(`unexpected case '${key}'`);
    }
  }
  for (const key of CASE_NAMES) {
    if (!RESULT_STATUSES.includes(cases[key])) {
      errors.push(`cases.${key} must be one of ${RESULT_STATUSES.join(", ")}`);
    }
  }
  return errors;
}

/**
 * Validates one forms confirmation results-matrix row against the
 * contract. Dependency-free so both Playwright specs and plain
 * {@code node --test} unit tests can use it.
 *
 * @param {unknown} row
 * @returns {string[]} validation errors, empty when the row is valid
 */
function validateConfirmationRow(row) {
  if (row == null || typeof row !== "object" || Array.isArray(row)) {
    return ["row must be an object"];
  }
  const errors = [...validateConfirmationRowHeader(row), ...validateConfirmationRowCases(row.cases)];
  if (!RESULT_STATUSES.includes(row.status)) {
    errors.push(`status must be one of ${RESULT_STATUSES.join(", ")}`);
  }
  if (typeof row.updated !== "string" || !DATE_PATTERN.test(row.updated)) {
    errors.push("updated must be a YYYY-MM-DD date");
  }
  return errors;
}

/**
 * @param {string} moduleDir module root (has {@code contracts/} and {@code frontend/})
 * @returns {string} absolute path of the results-matrix JSON schema
 */
function schemaPathFor(moduleDir) {
  return path.join(String(moduleDir), "contracts", SCHEMA_FILE);
}

module.exports = {
  MATRIX_SUITE,
  MATRIX_SPEC,
  SCHEMA_FILE,
  EXAMPLE_FILE,
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
  dtsBaseUrl,
  dtsContextUrl,
  dtsServiceUrl,
  deliveryAuthHeader,
  probeFormName,
  buildConfirmationRow,
  validateConfirmationRow,
  schemaPathFor,
};
