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
 * Closed inventory + results-matrix row helpers for the US3 DTS dynamic
 * interaction matrix (#6241 / parent #6238).
 *
 * <p>The pinned {@link DTS_SERVICES} list is the source of truth for "every
 * DTS-backed service": polls, feeds, forms, comments, metadata, membership.
 * Each entry pins the service's JAX-RS root (verified against
 * {@code deliverytiersuite/delivery-tier-suite/<service>/src/main/java}),
 * the {@code HEAD <root>/csrf} liveness probe every service implements, and
 * the fixture-gated read/write endpoints the interaction case uses. US2
 * (#6240) asserts the static shell of the DTS-backed widgets; every US2
 * dynamic widget's {@code dts} service has an interaction entry here, plus
 * {@code metadata}, which backs auto-list/indexing with no dedicated
 * widget.</p>
 *
 * <p>Two-level skip contract (mirrors US2): without {@code DTS_BASE_URL}
 * every case skips naming its service (T020, verifiable on the CMS-only
 * cell); with a base URL the case probes liveness, then the seeded fixture
 * (poll, feed, form, site/page, query, account) — an unseeded fixture skips
 * with its own named reason instead of failing, so the #6237 cell can turn
 * cases green one fixture at a time.</p>
 *
 * <p>URL paths below use {@code /} (web origins, not OS file paths). File
 * paths are joined with {@code node:path} so the suite stays portable.</p>
 */

"use strict";

const path = require("node:path");

const MATRIX_SUITE = "dts-interaction-matrix";
const MATRIX_SPEC = "tests/dts-interaction-matrix.spec.js";
const SCHEMA_FILE = "results-matrix.schema.json";
const EXAMPLE_FILE = "results-matrix-dts.example.json";

const CASE_INTERACT = "interact";
const CASE_NAMES = Object.freeze([CASE_INTERACT]);

const RESULT_STATUSES = Object.freeze(["pass", "fail", "skip"]);
const MATRIX_CELLS = Object.freeze(["dts-cell"]);

const SERVICE_NAME_PATTERN = /^(polls|feeds|forms|comments|metadata|membership)$/;
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/** Skip reason when no DTS-providing cell is configured. */
const SKIP_NO_DTS_BASE =
  "needs DTS_BASE_URL=(DTS-providing cell origin); " +
  "default CMS-only H2 QA cell serves no DTS services (#6237)";

/**
 * Skip reason when the cell serves no DTS-providing origin at all.
 *
 * @param {{ name: string, label: string }} entry DTS service entry
 * @returns {string}
 */
function skipNoDts(entry) {
  return (
    `service ${entry.name} (${entry.label}): ${SKIP_NO_DTS_BASE}`
  );
}

/**
 * Skip reason when the DTS origin answers but the service root does not.
 * A 401/403 still counts as "served" (the service is up, the caller lacks
 * the role); only unreachable / 404 probes land here.
 *
 * @param {{ name: string, label: string, root: string }} entry service entry
 * @param {string} baseUrl DTS origin that was probed
 * @returns {string}
 */
function skipServiceDown(entry, baseUrl) {
  return (
    `service ${entry.name} (${entry.label}): no liveness answer at ` +
    `${baseUrl}${entry.csrf}; the DTS cell does not serve ${entry.root} yet (#6237)`
  );
}

/**
 * Skip reason when the service is up but its interaction fixture is not
 * seeded yet. Each skip names the exact env/fixture gap so the fixture
 * slice can close them one by one without touching the spec.
 *
 * @param {{ name: string, label: string }} entry service entry
 * @param {string} detail what was probed and what is missing
 * @returns {string}
 */
function skipNoFixture(entry, detail) {
  return (
    `service ${entry.name} (${entry.label}): ${detail}; ` +
    "seed the US3 interaction fixture on the DTS cell (parent #6238) " +
    "to turn this case green"
  );
}

/**
 * One entry per DTS-backed service.
 *
 * <p>{@code root} is the JAX-RS root; {@code csrf} the {@code HEAD}
 * liveness probe. {@code fixtureEnv} names the env var selecting the
 * seeded fixture resource (with {@code fixtureDefault}); {@code widget}
 * names the US2 dynamic widget that surfaces this service ({@code null}
 * for metadata, which has no dedicated widget).</p>
 */
const DTS_SERVICES = Object.freeze([
  Object.freeze({
    name: "polls",
    label: "Polls",
    root: "/polls",
    csrf: "/polls/csrf",
    fixtureEnv: "DTS_FIXTURE_POLL",
    fixtureDefault: "qamatrix-poll",
    widget: "percPoll",
    interaction: "vote persists: POST /polls/save increments submits, re-GET shows it",
  }),
  Object.freeze({
    name: "feeds",
    label: "Feeds",
    root: "/rss",
    csrf: "/rss/csrf",
    fixtureEnv: "DTS_FIXTURE_FEED",
    fixtureDefault: "EnterpriseInvestments/qamatrix-feed/localhost",
    widget: "percRss",
    interaction: "feed renders entries: fixture feed URL returns text/xml with items",
  }),
  Object.freeze({
    name: "forms",
    label: "Forms",
    root: "/forms",
    csrf: "/forms/csrf",
    fixtureEnv: "DTS_FIXTURE_FORM",
    fixtureDefault: "qamatrix-form",
    widget: "percForm",
    interaction: "submit confirms and is retrievable: POST /forms/form/collect 2xx, GET /forms/form/cms/{form} 200",
  }),
  Object.freeze({
    name: "comments",
    label: "Comments",
    root: "/comment",
    csrf: "/comment/csrf",
    fixtureEnv: "DTS_FIXTURE_SITE",
    fixtureDefault: "EnterpriseInvestments",
    widget: "percComments",
    interaction: "comment appears per moderation rules: POST /comment/addcomment, then visible in POST /comment/list",
  }),
  Object.freeze({
    name: "metadata",
    label: "Metadata",
    root: "/metadata",
    csrf: "/metadata/csrf",
    fixtureEnv: "DTS_FIXTURE_SITE",
    fixtureDefault: "EnterpriseInvestments",
    widget: null,
    interaction: "index reflects published content: POST /metadata/get returns a result set",
  }),
  Object.freeze({
    name: "membership",
    label: "Membership",
    root: "/membership",
    csrf: "/membership/csrf",
    fixtureEnv: "DTS_FIXTURE_USER",
    fixtureDefault: "",
    widget: "percLogin",
    interaction: "login round-trip: POST /membership/login, session lookup, logout",
  }),
]);

/**
 * @param {string} name service name
 * @returns {string} Playwright tag for the service's case
 */
function tagForService(name) {
  return `@dts-${name}`;
}

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
 * @param {{ fixtureEnv: string, fixtureDefault: string }} entry service entry
 * @returns {string} configured fixture resource name (empty when the
 * fixture needs explicit configuration, e.g. the membership account)
 */
function fixtureValue(entry) {
  return process.env[entry.fixtureEnv] || entry.fixtureDefault;
}

/**
 * Builds one results-matrix row for a pinned DTS service. The row shape
 * mirrors {@code contracts/results-matrix.schema.json}; see
 * {@link validateInteractionRow}.
 *
 * @param {{ name: string, label: string }} entry service entry
 * @param {{ interact: string }} caseResults
 * @param {{ spec?: string, updated?: string }} [options]
 * @returns {object}
 */
function buildInteractionRow(entry, caseResults, options = {}) {
  return {
    suite: MATRIX_SUITE,
    spec: options.spec || MATRIX_SPEC,
    contentType: entry.name,
    label: entry.label,
    cell: MATRIX_CELLS[0],
    cases: {
      [CASE_INTERACT]: caseResults[CASE_INTERACT],
    },
    status: caseResults[CASE_INTERACT],
    updated: options.updated || new Date().toISOString().slice(0, 10),
  };
}

/**
 * Validates the scalar header of one DTS interaction results-matrix row.
 *
 * @param {object} row
 * @returns {string[]} validation errors
 */
function validateInteractionRowHeader(row) {
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
  if (typeof row.contentType !== "string" || !SERVICE_NAME_PATTERN.test(row.contentType)) {
    errors.push("contentType must be a pinned DTS service name");
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
 * Validates the per-case block of one DTS interaction results-matrix row.
 *
 * @param {unknown} cases
 * @returns {string[]} validation errors
 */
function validateInteractionRowCases(cases) {
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
 * Validates one DTS interaction results-matrix row against the contract.
 * Dependency-free so both Playwright specs and plain {@code node --test}
 * unit tests can use it.
 *
 * @param {unknown} row
 * @returns {string[]} validation errors, empty when the row is valid
 */
function validateInteractionRow(row) {
  if (row == null || typeof row !== "object" || Array.isArray(row)) {
    return ["row must be an object"];
  }
  const errors = [...validateInteractionRowHeader(row), ...validateInteractionRowCases(row.cases)];
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
  CASE_INTERACT,
  CASE_NAMES,
  RESULT_STATUSES,
  MATRIX_CELLS,
  SERVICE_NAME_PATTERN,
  SKIP_NO_DTS_BASE,
  skipNoDts,
  skipServiceDown,
  skipNoFixture,
  DTS_SERVICES,
  tagForService,
  dtsBaseUrl,
  fixtureValue,
  buildInteractionRow,
  validateInteractionRow,
  schemaPathFor,
};
