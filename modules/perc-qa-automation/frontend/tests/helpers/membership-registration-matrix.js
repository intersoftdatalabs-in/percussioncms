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
 * Closed inventory + results-matrix row helpers for the US3 membership
 * registration matrix (#6484 / parent #6238): prove the register/login
 * round-trip against the in-cell DTS membership service — a fresh visitor
 * account registers, logs in with the same credentials, resolves through
 * the session lookup, and closes on logout.
 *
 * <p>Three pinned cases, one row:</p>
 * <ul>
 *   <li>{@code registerCreates} — the open registration surface:
 *   {@code POST <context>/membership/user} with
 *   {@code confirmationRequired: false} answers
 *   {@code PSMembershipResult STATUS SUCCESS} and issues a session id.</li>
 *   <li>{@code loginRoundTrip} — the returning-visitor surface:
 *   {@code POST <context>/membership/login} with the freshly registered
 *   credentials answers {@code SUCCESS} with a session id, and
 *   {@code POST <context>/membership/session} resolves the same
 *   account.</li>
 *   <li>{@code logoutCloses} — the session-close surface:
 *   {@code POST <context>/membership/logout} answers {@code SUCCESS} and
 *   the session lookup no longer resolves the account.</li>
 * </ul>
 *
 * <p>Registrations are self-seeded under a unique
 * {@code qamember-<stamp>@example.com} address (see
 * {@link probeAccount}), so no CMS fixture and no pre-seeded
 * {@code DTS_FIXTURE_USER} account are needed — unlike the US3
 * interaction-matrix membership case (#6241), which logs in with a
 * provisioned account. Each Playwright case registers its own probe
 * account so cases stay independent under parallel workers.</p>
 *
 * <p>Skip contract (mirrors the US3 interaction matrix): without
 * {@code DTS_BASE_URL} every case skips naming the missing cell (#6237);
 * with a base URL each case probes liveness ({@code HEAD
 * <context>/membership/csrf} — the service lives under its Tomcat
 * deployment context, issue #6342) before touching the service. A cell
 * that disables open registration, or that forces account confirmation
 * even for {@code confirmationRequired: false}, skips the affected cases
 * with a named policy reason instead of failing, so the default
 * CMS-only cell and policy-gated DTS cells stay green.</p>
 *
 * <p>URL paths below use {@code /} (web origins, not OS file paths). File
 * paths are joined with {@code node:path} so the suite stays portable.</p>
 */

"use strict";

const path = require("node:path");

const MATRIX_SUITE = "membership-registration-matrix";
const MATRIX_SPEC = "tests/membership-registration-matrix.spec.js";
const SCHEMA_FILE = "results-matrix.schema.json";
const EXAMPLE_FILE = "results-matrix-membership.example.json";

const CASE_REGISTER = "registerCreates";
const CASE_LOGIN = "loginRoundTrip";
const CASE_LOGOUT = "logoutCloses";
const CASE_NAMES = Object.freeze([CASE_REGISTER, CASE_LOGIN, CASE_LOGOUT]);

const RESULT_STATUSES = Object.freeze(["pass", "fail", "skip"]);
const MATRIX_CELLS = Object.freeze(["dts-cell"]);

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/** Skip reason when no DTS-providing cell is configured. */
const SKIP_NO_DTS_BASE =
  "needs DTS_BASE_URL=(DTS-providing cell origin); " +
  "default CMS-only H2 QA cell serves no DTS services (#6237)";

/**
 * Skip reason when the DTS origin answers but the membership service
 * does not. A 401/403 still counts as "served" (the service is up, the
 * caller lacks the role); only unreachable / 404 probes land here. The
 * probed endpoint is the deployment context path (war name) plus the
 * JAX-RS root — the service is served under a Tomcat context (see
 * {@link MEMBERSHIP_ENTRY}), not at the DTS origin root.
 *
 * @param {string} baseUrl DTS origin that was probed
 * @returns {string}
 */
function skipServiceDown(baseUrl) {
  return (
    `membership registration (Membership): no liveness answer at ` +
    `${baseUrl}/${MEMBERSHIP_ENTRY.context}${MEMBERSHIP_ENTRY.csrf}; the DTS cell does not serve ` +
    `${MEMBERSHIP_ENTRY.context}${MEMBERSHIP_ENTRY.root} yet (#6237)`
  );
}

/**
 * Skip reason when the service is up but the cell policy does not allow
 * the self-seeded register/login round-trip. Each skip names the exact
 * policy gap so the cell slice can close it without touching the spec.
 *
 * @param {string} detail what was probed and what the cell answered
 * @returns {string}
 */
function skipNoRegistration(detail) {
  return (
    `membership registration (Membership): ${detail}; ` +
    `the cell must serve open registration with confirmationRequired=false ` +
    `(no forced account confirmation) on the DTS membership service ` +
    `to turn these cases green (parent #6238)`
  );
}

/**
 * The pinned membership service entry. {@code context} is the DTS Tomcat
 * context path (war name {@code perc-membership-services});
 * {@code root} is the JAX-RS root; {@code csrf} the {@code HEAD}
 * liveness probe. Registrations are self-seeded, so there is no fixture
 * env — the probe account is derived per run ({@link probeAccount}).
 */
const MEMBERSHIP_ENTRY = Object.freeze({
  name: "membership",
  label: "Membership",
  context: "perc-membership-services",
  root: "/membership",
  csrf: "/membership/csrf",
  widget: "percLogin",
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
 * Deployment context URL for the membership service: the DTS origin
 * plus the deployment context path (war name), without the JAX-RS root.
 * Same shape as
 * {@code tests/helpers/dts-interaction-matrix.js#dtsContextUrl}.
 *
 * @param {URL} dtsOrigin DTS-providing cell origin from {@link dtsBaseUrl}
 * @returns {string} context URL (no trailing slash)
 */
function dtsContextUrl(dtsOrigin) {
  return `${dtsOrigin.origin}/${MEMBERSHIP_ENTRY.context}`;
}

/**
 * Service base URL for the membership registration cases: the DTS
 * origin plus the deployment context path (war name) plus the JAX-RS
 * root, mirroring how the published login widget addresses the service.
 * Same shape as
 * {@code tests/helpers/dts-interaction-matrix.js#dtsServiceUrl}.
 *
 * @param {URL} dtsOrigin DTS-providing cell origin from {@link dtsBaseUrl}
 * @returns {string} service base URL (no trailing slash)
 */
function dtsServiceUrl(dtsOrigin) {
  return `${dtsContextUrl(dtsOrigin)}${MEMBERSHIP_ENTRY.root}`;
}

/**
 * Unique self-seeded probe account for one registration round-trip.
 * The local part is timestamp-derived so parallel workers and reruns
 * never collide; the shape satisfies the service's non-empty
 * email/password validation
 * ({@code PSMembershipRestService#createUser}).
 *
 * @param {number} [stamp] timestamp millis; defaults to now
 * @returns {{ email: string, password: string }} probe credentials
 */
function probeAccount(stamp) {
  const at = typeof stamp === "number" ? stamp : Date.now();
  return {
    email: `qamember-${at}@example.com`,
    password: `Qa-${at}!x9`,
  };
}

/**
 * Normalizes a {@code PSMembershipResult STATUS} wire value. The JAX-RS
 * layer serializes the Java enum name ({@code SUCCESS}); the comparison
 * is case-insensitive so provider formatting drift cannot flip a green
 * run red.
 *
 * @param {unknown} status raw status value from the service JSON body
 * @returns {boolean} true when the status reads as SUCCESS
 */
function isSuccessStatus(status) {
  return String(status || "").toUpperCase() === "SUCCESS";
}

/**
 * Builds the one results-matrix row for the pinned membership entry.
 * The row shape mirrors {@code contracts/results-matrix.schema.json};
 * see {@link validateRegistrationRow}.
 *
 * @param {{ registerCreates: string, loginRoundTrip: string, logoutCloses: string }} caseResults
 * @param {{ spec?: string, updated?: string }} [options]
 * @returns {object}
 */
function buildRegistrationRow(caseResults, options = {}) {
  const status = caseResults[CASE_REGISTER] === "fail"
    || caseResults[CASE_LOGIN] === "fail"
    || caseResults[CASE_LOGOUT] === "fail"
    ? "fail"
    : caseResults[CASE_REGISTER] === "skip"
      || caseResults[CASE_LOGIN] === "skip"
      || caseResults[CASE_LOGOUT] === "skip"
      ? "skip"
      : "pass";
  return {
    suite: MATRIX_SUITE,
    spec: options.spec || MATRIX_SPEC,
    contentType: MEMBERSHIP_ENTRY.name,
    label: MEMBERSHIP_ENTRY.label,
    cell: MATRIX_CELLS[0],
    cases: {
      [CASE_REGISTER]: caseResults[CASE_REGISTER],
      [CASE_LOGIN]: caseResults[CASE_LOGIN],
      [CASE_LOGOUT]: caseResults[CASE_LOGOUT],
    },
    status,
    updated: options.updated || new Date().toISOString().slice(0, 10),
  };
}

/**
 * Validates the scalar header of one membership registration
 * results-matrix row.
 *
 * @param {object} row
 * @returns {string[]} validation errors
 */
function validateRegistrationRowHeader(row) {
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
  if (row.contentType !== MEMBERSHIP_ENTRY.name) {
    errors.push(`contentType must be '${MEMBERSHIP_ENTRY.name}'`);
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
 * Validates the per-case block of one membership registration
 * results-matrix row.
 *
 * @param {unknown} cases
 * @returns {string[]} validation errors
 */
function validateRegistrationRowCases(cases) {
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
 * Validates one membership registration results-matrix row against the
 * contract. Dependency-free so both Playwright specs and plain
 * {@code node --test} unit tests can use it.
 *
 * @param {unknown} row
 * @returns {string[]} validation errors, empty when the row is valid
 */
function validateRegistrationRow(row) {
  if (row == null || typeof row !== "object" || Array.isArray(row)) {
    return ["row must be an object"];
  }
  const errors = [...validateRegistrationRowHeader(row), ...validateRegistrationRowCases(row.cases)];
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
  dtsBaseUrl,
  dtsContextUrl,
  dtsServiceUrl,
  probeAccount,
  isSuccessStatus,
  buildRegistrationRow,
  validateRegistrationRow,
  schemaPathFor,
};
