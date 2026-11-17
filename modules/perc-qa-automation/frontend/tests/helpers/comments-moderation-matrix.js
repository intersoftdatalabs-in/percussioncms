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
 * Closed inventory + results-matrix row helpers for the US3 comments
 * moderation matrix (#6485 / parent #6238): prove the approve/reject
 * round-trip against the in-cell DTS comments service — a visitor comment
 * is submitted through the open surface, approved through the
 * {@code deliverymanager}-gated moderation surface, confirmed publicly
 * visible, then rejected and confirmed hidden again.
 *
 * <p>Three pinned cases, one row:</p>
 * <ul>
 *   <li>{@code submitAccepted} — the open visitor surface:
 *   {@code POST <context>/comment/addcomment} (form: action, username,
 *   site, pagepath, email, text, title) is accepted (HTTP ok) and the
 *   probe comment is either already publicly listed (auto-approve cell)
 *   or present in the moderator queue.</li>
 *   <li>{@code approvePublishes} — the moderation approve surface:
 *   {@code PUT <context>/comment/moderation/approve} with the probe
 *   comment id ({@code deliverymanager} BASIC) makes the probe marker
 *   publicly visible through {@code POST <context>/comment/list}.</li>
 *   <li>{@code rejectHides} — the moderation reject surface:
 *   {@code PUT <context>/comment/moderation/reject} with a second probe
 *   comment id removes its marker from the public list.</li>
 * </ul>
 *
 * <p>Moderation reads and writes live behind the {@code deliverymanager}
 * role (BASIC, {@code CONFIDENTIAL} — see
 * {@code deliverytiersuite/delivery-tier-suite/comments/src/main/java/webapp/WEB-INF/web.xml}).
 * Credentials default to the shipped DTS development defaults (see
 * {@code delivery-tier-distribution/src/main/resources/etc/dts-realm.properties};
 * override with {@code DTS_DELIVERY_USER} / {@code DTS_DELIVERY_PASSWORD}
 * when the cell provisions its own). Without usable credentials — or
 * without a reachable DTS HTTPS port for the {@code CONFIDENTIAL}
 * redirect — the moderation legs skip with a named reason instead of
 * failing (cell provisioning tracked in #6375), so the default CMS-only
 * cell and credential-less DTS cells stay green.</p>
 *
 * <p>Skip contract (mirrors the US3 interaction matrix): without
 * {@code DTS_BASE_URL} every case skips naming the missing cell (#6237);
 * with a base URL each case probes liveness ({@code HEAD
 * <context>/comment/csrf} — the service lives under its Tomcat
 * deployment context, issue #6342) before touching the service. Probes
 * are self-seeded under a unique {@code qamod-<stamp>} site + page, so
 * no CMS fixture and no pre-seeded comment thread are needed. Probe ids
 * are resolved through the moderator queue
 * ({@code POST <context>/comment/moderation/asmoderator}), so each case
 * stays independent under parallel workers.</p>
 *
 * <p>URL paths below use {@code /} (web origins, not OS file paths). File
 * paths are joined with {@code node:path} so the suite stays portable.</p>
 */

"use strict";

const path = require("node:path");

const MATRIX_SUITE = "comments-moderation-matrix";
const MATRIX_SPEC = "tests/comments-moderation-matrix.spec.js";
const SCHEMA_FILE = "results-matrix.schema.json";
const EXAMPLE_FILE = "results-matrix-comments-moderation.example.json";

const CASE_SUBMIT = "submitAccepted";
const CASE_APPROVE = "approvePublishes";
const CASE_REJECT = "rejectHides";
const CASE_NAMES = Object.freeze([CASE_SUBMIT, CASE_APPROVE, CASE_REJECT]);

const RESULT_STATUSES = Object.freeze(["pass", "fail", "skip"]);
const MATRIX_CELLS = Object.freeze(["dts-cell"]);

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/** Skip reason when no DTS-providing cell is configured. */
const SKIP_NO_DTS_BASE =
  "needs DTS_BASE_URL=(DTS-providing cell origin); " +
  "default CMS-only H2 QA cell serves no DTS services (#6237)";

/**
 * Skip reason when the DTS origin answers but the comments service does
 * not. A 401/403 still counts as "served" (the service is up, the caller
 * lacks the role); only unreachable / 404 probes land here. The probed
 * endpoint is the deployment context path (war name) plus the JAX-RS
 * root — the service is served under a Tomcat context (issue #6342),
 * not at the DTS origin root.
 *
 * @param {string} baseUrl DTS origin that was probed
 * @returns {string}
 */
function skipServiceDown(baseUrl) {
  return (
    `comments moderation (Comments): no liveness answer at ` +
    `${baseUrl}/${COMMENTS_ENTRY.context}${COMMENTS_ENTRY.csrf}; the DTS cell does not serve ` +
    `${COMMENTS_ENTRY.context}${COMMENTS_ENTRY.root} yet (#6237)`
  );
}

/**
 * Skip reason when the moderation surface cannot be exercised in this
 * cell. The approve/reject endpoints and the moderator queue are
 * {@code deliverymanager}-gated (BASIC, {@code CONFIDENTIAL}), so plain-
 * HTTP cells without provisioned credentials — or without a reachable
 * DTS HTTPS port for the {@code CONFIDENTIAL} redirect — skip here
 * instead of failing.
 *
 * @param {string} detail what was probed and what the cell answered
 * @returns {string}
 */
function skipNoModeration(detail) {
  return (
    `comments moderation (Comments): ${detail}; ` +
    `the approve/reject surface is deliverymanager-gated — provision ` +
    `DTS_DELIVERY_USER / DTS_DELIVERY_PASSWORD and a reachable DTS HTTPS ` +
    `port on the DTS cell (parent #6238, provisioning #6375) to turn these cases green`
  );
}

/**
 * The pinned comments service entry. {@code context} is the DTS Tomcat
 * context path (war name {@code perc-comments-services}); {@code root}
 * is the JAX-RS root; {@code csrf} the {@code HEAD} liveness probe.
 * Probes are self-seeded, so there is no fixture env — the probe
 * site/page/marker derive per run ({@link probeThread}).
 */
const COMMENTS_ENTRY = Object.freeze({
  name: "comments",
  label: "Comments",
  context: "perc-comments-services",
  root: "/comment",
  csrf: "/comment/csrf",
  widget: "percComments",
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
 * Deployment context URL for the comments service: the DTS origin plus
 * the deployment context path (war name), without the JAX-RS root. Same
 * shape as {@code tests/helpers/dts-interaction-matrix.js#dtsContextUrl}.
 *
 * @param {URL} dtsOrigin DTS-providing cell origin from {@link dtsBaseUrl}
 * @returns {string} context URL (no trailing slash)
 */
function dtsContextUrl(dtsOrigin) {
  return `${dtsOrigin.origin}/${COMMENTS_ENTRY.context}`;
}

/**
 * Service base URL for the comments moderation cases: the DTS origin
 * plus the deployment context path (war name) plus the JAX-RS root,
 * mirroring how the published comments widget addresses the service.
 * Same shape as
 * {@code tests/helpers/dts-interaction-matrix.js#dtsServiceUrl}.
 *
 * @param {URL} dtsOrigin DTS-providing cell origin from {@link dtsBaseUrl}
 * @returns {string} service base URL (no trailing slash)
 */
function dtsServiceUrl(dtsOrigin) {
  return `${dtsContextUrl(dtsOrigin)}${COMMENTS_ENTRY.root}`;
}

/**
 * BASIC credentials for the role-protected moderation endpoints
 * ({@code deliverymanager} role). Defaults are the shipped DTS
 * development defaults (see
 * {@code delivery-tier-distribution/src/main/resources/etc/dts-realm.properties};
 * the same defaults the US3 forms matrix uses for its gated retrieval
 * legs); override with env when the cell provisions its own.
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
 * Unique self-seeded probe thread for one moderation round-trip. The
 * site, page, and marker share one stamp so parallel workers never
 * collide and no CMS fixture is needed.
 *
 * @param {number} [stamp] timestamp millis; defaults to now
 * @returns {{ site: string, page: string, marker: string, username: string, email: string, title: string }}
 */
function probeThread(stamp) {
  const at = typeof stamp === "number" ? stamp : Date.now();
  const tag = `qamod-${at}`;
  return {
    site: tag,
    page: `/${tag}`,
    marker: tag,
    username: "qa-moderation",
    email: `${tag}@example.com`,
    title: "QA moderation probe",
  };
}

/**
 * Builds the one results-matrix row for the pinned comments entry. The
 * row shape mirrors {@code contracts/results-matrix.schema.json}; see
 * {@link validateModerationRow}.
 *
 * @param {{ submitAccepted: string, approvePublishes: string, rejectHides: string }} caseResults
 * @param {{ spec?: string, updated?: string }} [options]
 * @returns {object}
 */
function buildModerationRow(caseResults, options = {}) {
  const status =
    caseResults[CASE_SUBMIT] === "fail" ||
    caseResults[CASE_APPROVE] === "fail" ||
    caseResults[CASE_REJECT] === "fail"
      ? "fail"
      : caseResults[CASE_SUBMIT] === "skip" ||
          caseResults[CASE_APPROVE] === "skip" ||
          caseResults[CASE_REJECT] === "skip"
        ? "skip"
        : "pass";
  return {
    suite: MATRIX_SUITE,
    spec: options.spec || MATRIX_SPEC,
    contentType: COMMENTS_ENTRY.name,
    label: COMMENTS_ENTRY.label,
    cell: MATRIX_CELLS[0],
    cases: {
      [CASE_SUBMIT]: caseResults[CASE_SUBMIT],
      [CASE_APPROVE]: caseResults[CASE_APPROVE],
      [CASE_REJECT]: caseResults[CASE_REJECT],
    },
    status,
    updated: options.updated || new Date().toISOString().slice(0, 10),
  };
}

/**
 * Validates the scalar header of one comments moderation results-matrix row.
 *
 * @param {object} row
 * @returns {string[]} validation errors
 */
function validateModerationRowHeader(row) {
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
  if (row.contentType !== COMMENTS_ENTRY.name) {
    errors.push(`contentType must be '${COMMENTS_ENTRY.name}'`);
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
 * Validates the per-case block of one comments moderation results-matrix row.
 *
 * @param {unknown} cases
 * @returns {string[]} validation errors
 */
function validateModerationRowCases(cases) {
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
 * Validates one comments moderation results-matrix row against the
 * contract. Dependency-free so both Playwright specs and plain
 * {@code node --test} unit tests can use it.
 *
 * @param {unknown} row
 * @returns {string[]} validation errors, empty when the row is valid
 */
function validateModerationRow(row) {
  if (row == null || typeof row !== "object" || Array.isArray(row)) {
    return ["row must be an object"];
  }
  const errors = [...validateModerationRowHeader(row), ...validateModerationRowCases(row.cases)];
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
  CASE_APPROVE,
  CASE_REJECT,
  CASE_NAMES,
  RESULT_STATUSES,
  MATRIX_CELLS,
  SKIP_NO_DTS_BASE,
  skipServiceDown,
  skipNoModeration,
  COMMENTS_ENTRY,
  dtsBaseUrl,
  dtsContextUrl,
  dtsServiceUrl,
  deliveryAuthHeader,
  probeThread,
  buildModerationRow,
  validateModerationRow,
  schemaPathFor,
};
