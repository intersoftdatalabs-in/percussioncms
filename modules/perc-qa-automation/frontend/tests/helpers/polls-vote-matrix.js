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
 * Closed inventory + results-matrix row helpers for the US3 polls vote
 * matrix (#6241 / parent #6241): prove the vote+results round-trip
 * against the in-cell DTS polls service — a self-seeded probe poll is
 * created by voting ({@code POST <context>/polls/save}), a second vote
 * persists ({@code totalVotes} increments on re-read), and the results
 * surfaces ({@code GET <context>/polls/{pollName}} and
 * {@code GET <context>/polls/question/{pollQuestion}}) reflect the votes.
 *
 * <p>Three pinned cases, one row:</p>
 * <ul>
 *   <li>{@code voteCreates} — the vote surface:
 *   {@code POST <context>/polls/save} with a fresh poll name, a fresh
 *   poll question, and {@code pollSubmits} selecting one choice answers
 *   {@code PSPollsResponse} status {@code SUCCESS} with
 *   {@code totalVotes >= 1} ({@code PSPollsService#savePoll} creates the
 *   poll when the question is new, so no CMS or DTS fixture is needed).</li>
 *   <li>{@code votePersists} — the persistence surface: a second
 *   {@code POST <context>/polls/save} for the same question followed by
 *   {@code GET <context>/polls/{pollName}} shows {@code totalVotes}
 *   incremented by exactly one and the voted choice present in
 *   {@code pollResults}.</li>
 *   <li>{@code resultsByQuestion} — the results surface:
 *   {@code GET <context>/polls/question/{pollQuestion}} answers
 *   {@code SUCCESS} with the same totals and the voted choice.</li>
 * </ul>
 *
 * <p>Each Playwright case votes on its own unique probe poll
 * (timestamp-derived name + question, see {@link probePoll}), so cases
 * stay independent under parallel workers and reruns never collide.
 * {@code restrictBySession} is always {@code false} so no session vote
 * gate interferes with the counts.</p>
 *
 * <p>Skip contract (mirrors the US3 membership matrix): without
 * {@code DTS_BASE_URL} (or its {@code DTS_URL} banner alias) every case
 * skips naming the missing cell (#6237); with a base URL each case
 * probes liveness ({@code HEAD
 * <context>/polls/csrf} — the service lives under its Tomcat deployment
 * context, issue #6342) before touching the service. A cell whose polls
 * store answers {@code ERROR} to a save (unprovisioned polls database)
 * skips the affected cases with a named reason instead of failing, so
 * the default CMS-only cell and unprovisioned DTS cells stay green.</p>
 *
 * <p>URL paths below use {@code /} (web origins, not OS file paths). File
 * paths are joined with {@code node:path} so the suite stays portable.</p>
 */

"use strict";

const path = require("node:path");

const dtsCell = require("./dts-cell");

const MATRIX_SUITE = "polls-vote-matrix";
const MATRIX_SPEC = "tests/polls-vote-matrix.spec.js";
const SCHEMA_FILE = "results-matrix.schema.json";
const EXAMPLE_FILE = "results-matrix-polls.example.json";

const CASE_VOTE = "voteCreates";
const CASE_PERSIST = "votePersists";
const CASE_RESULTS = "resultsByQuestion";
const CASE_NAMES = Object.freeze([CASE_VOTE, CASE_PERSIST, CASE_RESULTS]);

const RESULT_STATUSES = Object.freeze(["pass", "fail", "skip"]);
const MATRIX_CELLS = Object.freeze(["dts-cell"]);

const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/;

/** Skip reason when no DTS-providing cell is configured (shared cell contract). */
const SKIP_NO_DTS_BASE = dtsCell.SKIP_NO_DTS_BASE;

/**
 * Skip reason when the DTS origin answers but the polls service does
 * not. A 401/403 still counts as "served" (the service is up, the
 * caller lacks the role); only unreachable / 404 probes land here. The
 * probed endpoint is the deployment context path (war name) plus the
 * JAX-RS root — the service is served under a Tomcat context (see
 * {@link POLLS_ENTRY}), not at the DTS origin root.
 *
 * @param {string} baseUrl DTS origin that was probed
 * @returns {string}
 */
function skipServiceDown(baseUrl) {
  return (
    `polls vote (Polls): no liveness answer at ` +
    `${baseUrl}/${POLLS_ENTRY.context}${POLLS_ENTRY.csrf}; the DTS cell does not serve ` +
    `${POLLS_ENTRY.context}${POLLS_ENTRY.root} yet (#6237)`
  );
}

/**
 * Skip reason when the service is up but the cell cannot persist the
 * probe vote. Each skip names the exact gap so the cell slice can close
 * it without touching the spec.
 *
 * @param {string} detail what was probed and what the cell answered
 * @returns {string}
 */
function skipUnprovisioned(detail) {
  return (
    `polls vote (Polls): ${detail}; ` +
    `the cell must serve a writable polls store on the DTS polls service ` +
    `to turn these cases green (parent #6241)`
  );
}

/**
 * The pinned polls service entry. {@code context} is the DTS Tomcat
 * context path (war name {@code perc-polls-services});
 * {@code root} is the JAX-RS root; {@code csrf} the {@code HEAD}
 * liveness probe. Votes are self-seeded, so there is no fixture
 * env — the probe poll is derived per run ({@link probePoll}).
 */
const POLLS_ENTRY = Object.freeze({
  name: "polls",
  label: "Polls",
  context: "perc-polls-services",
  root: "/polls",
  csrf: "/polls/csrf",
  widget: "percPoll",
});

/**
 * DTS-providing cell origin, or null when the cell is CMS-only (default
 * H2 QA cell: every case skips named). Delegates to the shared cell
 * contract ({@code tests/helpers/dts-cell.js#dtsBaseUrl}): accepts the
 * {@code DTS_URL} banner alias.
 *
 * @returns {URL|null}
 */
function dtsBaseUrl() {
  return dtsCell.dtsBaseUrl();
}

/**
 * Deployment context URL for the polls service: the DTS origin
 * plus the deployment context path (war name), without the JAX-RS root.
 * Same shape as
 * {@code tests/helpers/dts-interaction-matrix.js#dtsContextUrl}.
 *
 * @param {URL} dtsOrigin DTS-providing cell origin from {@link dtsBaseUrl}
 * @returns {string} context URL (no trailing slash)
 */
function dtsContextUrl(dtsOrigin) {
  return `${dtsOrigin.origin}/${POLLS_ENTRY.context}`;
}

/**
 * Service base URL for the polls vote cases: the DTS
 * origin plus the deployment context path (war name) plus the JAX-RS
 * root, mirroring how the published poll widget addresses the service.
 * Same shape as
 * {@code tests/helpers/dts-interaction-matrix.js#dtsServiceUrl}.
 *
 * @param {URL} dtsOrigin DTS-providing cell origin from {@link dtsBaseUrl}
 * @returns {string} service base URL (no trailing slash)
 */
function dtsServiceUrl(dtsOrigin) {
  return `${dtsContextUrl(dtsOrigin)}${POLLS_ENTRY.root}`;
}

/**
 * Unique self-seeded probe poll for one vote round-trip. The name and
 * question are timestamp-derived so parallel workers and reruns never
 * collide; {@code PSPollsService#savePoll} creates the poll when the
 * question is new, so no fixture is needed.
 *
 * @param {number} [stamp] timestamp millis; defaults to now
 * @returns {{ pollName: string, pollQuestion: string, choice: string }} probe poll
 */
function probePoll(stamp) {
  const at = typeof stamp === "number" ? stamp : Date.now();
  return {
    pollName: `qamatrix-poll-${at}`,
    pollQuestion: `QA matrix probe ${at}?`,
    choice: "Choice A",
  };
}

/**
 * Normalizes a {@code PSPollsResponse} status wire value. The JAX-RS
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
 * Builds the one results-matrix row for the pinned polls entry.
 * The row shape mirrors {@code contracts/results-matrix.schema.json};
 * see {@link validateVoteRow}.
 *
 * @param {{ voteCreates: string, votePersists: string, resultsByQuestion: string }} caseResults
 * @param {{ spec?: string, updated?: string }} [options]
 * @returns {object}
 */
function buildVoteRow(caseResults, options = {}) {
  const status = caseResults[CASE_VOTE] === "fail"
    || caseResults[CASE_PERSIST] === "fail"
    || caseResults[CASE_RESULTS] === "fail"
    ? "fail"
    : caseResults[CASE_VOTE] === "skip"
      || caseResults[CASE_PERSIST] === "skip"
      || caseResults[CASE_RESULTS] === "skip"
      ? "skip"
      : "pass";
  return {
    suite: MATRIX_SUITE,
    spec: options.spec || MATRIX_SPEC,
    contentType: POLLS_ENTRY.name,
    label: POLLS_ENTRY.label,
    cell: MATRIX_CELLS[0],
    cases: {
      [CASE_VOTE]: caseResults[CASE_VOTE],
      [CASE_PERSIST]: caseResults[CASE_PERSIST],
      [CASE_RESULTS]: caseResults[CASE_RESULTS],
    },
    status,
    updated: options.updated || new Date().toISOString().slice(0, 10),
  };
}

/**
 * Validates the scalar header of one polls vote
 * results-matrix row.
 *
 * @param {object} row
 * @returns {string[]} validation errors
 */
function validateVoteRowHeader(row) {
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
  if (row.contentType !== POLLS_ENTRY.name) {
    errors.push(`contentType must be '${POLLS_ENTRY.name}'`);
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
 * Validates the per-case block of one polls vote
 * results-matrix row.
 *
 * @param {unknown} cases
 * @returns {string[]} validation errors
 */
function validateVoteRowCases(cases) {
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
 * Validates one polls vote results-matrix row against the
 * contract. Dependency-free so both Playwright specs and plain
 * {@code node --test} unit tests can use it.
 *
 * @param {unknown} row
 * @returns {string[]} validation errors, empty when the row is valid
 */
function validateVoteRow(row) {
  if (row == null || typeof row !== "object" || Array.isArray(row)) {
    return ["row must be an object"];
  }
  const errors = [...validateVoteRowHeader(row), ...validateVoteRowCases(row.cases)];
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
  dtsBaseUrl,
  dtsContextUrl,
  dtsServiceUrl,
  probePoll,
  isSuccessStatus,
  buildVoteRow,
  validateVoteRow,
  schemaPathFor,
};
