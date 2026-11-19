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
 * {@code DTS_BASE_URL} (or its {@code DTS_URL} banner alias) every case
 * skips naming the missing cell (#6237);
 * with a base URL each case probes liveness ({@code HEAD
 * <context>/forms/csrf} — the service lives under its Tomcat deployment
 * context, issue #6342) before touching the service. The visitor submit
 * runs over plain HTTP (open surface). The two retrieval cases run over
 * the published DTS HTTPS origin ({@link dtsHttpsBaseUrl} — the
 * {@code CONFIDENTIAL} retrieval surface 302-redirects plain-HTTP callers
 * to the in-cell HTTPS port, published by {@code qa-up --with-dts} since
 * #6375/#6599) with the provisioned {@code deliverymanager} credential, so
 * a fresh cell goes green instead of named-skipping. Submissions are
 * self-seeded under a unique {@code qaconfirm-<stamp>} form name (valid per
 * {@code PSFormService.isValidFormName}), so no CMS fixture is needed.</p>
 *
 * <p>URL paths below use {@code /} (web origins, not OS file paths). File
 * paths are joined with {@code node:path} so the suite stays portable.</p>
 */

"use strict";

const path = require("node:path");

const dtsCell = require("./dts-cell");

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

/** Skip reason when no DTS-providing cell is configured (shared cell contract). */
const SKIP_NO_DTS_BASE = dtsCell.SKIP_NO_DTS_BASE;

/** Skip reason when the cell publishes no DTS HTTPS origin (shared cell contract). */
const SKIP_NO_DTS_HTTPS = dtsCell.SKIP_NO_DTS_HTTPS;

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
 * Skip reason when the admin retrieval surface cannot be read over the
 * published DTS HTTPS origin in this cell. The summaries/export endpoints
 * are {@code deliverymanager}-gated (BASIC), so without the provisioned
 * credential — seed it with {@code qa-seed-widget-fixtures
 * --delivery-password <secret>} then export {@code DTS_DELIVERY_USER} /
 * {@code DTS_DELIVERY_PASSWORD} — or without a reachable DTS HTTPS port,
 * the two retrieval cases skip here instead of failing. The HTTPS publish
 * itself landed in #6375/#6599; a 302/401/403 over HTTPS therefore names a
 * cell-credential gap, not a missing platform feature.
 *
 * @param {string} detail what was probed and what the cell answered
 * @returns {string}
 */
function skipNoRetrieval(detail) {
  return (
    `forms confirmation (Forms): ${detail}; ` +
    `the admin retrieval surface is deliverymanager-gated — provision ` +
    `DTS_DELIVERY_USER / DTS_DELIVERY_PASSWORD and a reachable DTS HTTPS ` +
    `port on the DTS cell (#6375) to turn these cases green`
  );
}

/**
 * Builds the probe detail embedded in a retrieval skip so the skip names
 * what the cell answered, including the {@code CONFIDENTIAL} redirect
 * target when the cell answered 302 — the target proves whether the
 * redirect points at a container-local HTTPS port the host runner cannot
 * reach. Pure string formatting (no Playwright objects) so plain
 * {@code node --test} unit tests cover it.
 *
 * @param {string} probedPath request path that was probed
 * @param {number} status HTTP status the cell answered
 * @param {string|null} [location] redirect target the cell advertised, if any
 * @returns {string}
 */
function retrievalDetail(probedPath, status, location = null) {
  const at = location ? ` (location ${location})` : "";
  return `GET ${probedPath} answered HTTP ${status}${at}`;
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
 * CMS-only (default H2 QA cell: every case skips named). Shared cell
 * contract ({@code tests/helpers/dts-cell.js#dtsBaseUrl}): accepts the
 * {@code DTS_URL} banner alias.
 */
function dtsBaseUrl() {
  return dtsCell.dtsBaseUrl();
}

/**
 * In-cell DTS HTTPS origin for CONFIDENTIAL writer paths, or null when
 * the cell publishes no HTTPS origin. Shared cell contract (see
 * {@code tests/helpers/dts-cell.js#dtsHttpsBaseUrl}) — re-exported here
 * so future HTTPS-gated retrieval work reuses the single choke point
 * instead of adding another copy.
 *
 * @returns {URL|null} in-cell DTS HTTPS origin for host-side writer calls
 */
function dtsHttpsBaseUrl() {
  return dtsCell.dtsHttpsBaseUrl();
}

/**
 * Shared HTTPS writer context (see
 * {@code tests/helpers/dts-cell.js#openDtsHttpsContext}): probes the
 * origin, skips by name, and disposes on the skip path. Re-exported so
 * the spec stays data-driven from this helper.
 */
const openDtsHttpsContext = dtsCell.openDtsHttpsContext;

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
 * Service base URL for the forms retrieval cases: the in-cell DTS HTTPS
 * origin plus the deployment context path (war name) plus the JAX-RS
 * root, mirroring how the admin retrieval surface is addressed under the
 * {@code CONFIDENTIAL} guarantee.
 *
 * @param {URL} dtsHttpsOrigin in-cell DTS HTTPS origin from {@link dtsHttpsBaseUrl}
 * @returns {string} service base URL (no trailing slash)
 */
function dtsHttpsServiceUrl(dtsHttpsOrigin) {
  return `${dtsHttpsOrigin.origin}/${FORMS_ENTRY.context}${FORMS_ENTRY.root}`;
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
 * Reads the submitted-entry total for one form out of an admin summaries
 * body. The container serializes the summary list either through the bean
 * getter ({@code summaries}) or the JAXB field ({@code formsInfo})
 * depending on the JSON provider — accept both shapes. Returns
 * {@code null} when the form has no entry or the total is not a number so
 * callers can fail with before/after totals instead of a TypeError. Pure
 * data shaping (no Playwright objects) so plain {@code node --test} unit
 * tests cover it.
 *
 * @param {unknown} body parsed summaries JSON
 * @param {string} formName probe form name to look up
 * @returns {number|null} total submissions for the form, or null
 */
function summariesTotal(body, formName) {
  const summaries =
    body && Array.isArray(body.summaries)
      ? body.summaries
      : body && Array.isArray(body.formsInfo)
        ? body.formsInfo
        : [];
  const entry = summaries.find((row) => row && row.name === formName);
  return entry && typeof entry.totalForms === "number" ? entry.totalForms : null;
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
  SKIP_NO_DTS_HTTPS,
  skipServiceDown,
  skipNoRetrieval,
  FORMS_ENTRY,
  dtsBaseUrl,
  dtsContextUrl,
  dtsServiceUrl,
  dtsHttpsBaseUrl,
  dtsHttpsBaseUrl,
  openDtsHttpsContext,
  dtsHttpsServiceUrl,
  deliveryAuthHeader,
  probeFormName,
  retrievalDetail,
  summariesTotal,
  buildConfirmationRow,
  validateConfirmationRow,
  schemaPathFor,
};
