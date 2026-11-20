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
 * Coverage-matrix index (#6238 aggregate, SC-004): the single accounting
 * point over the three QA coverage matrices — US1 creation
 * ({@code content-create-matrix}, #6239), US2 static render
 * ({@code published-widget-matrix}, #6240), and US3 DTS interaction
 * ({@code dts-interaction-matrix}, #6241).
 *
 * <p>Each matrix keeps its own closed inventory in its own helper; this
 * module only aggregates and cross-checks so the family cannot drift apart
 * silently:</p>
 *
 * <ul>
 *   <li>every dynamic widget ({@code WIDGETS} entry with a {@code dts} link)
 *   names a DTS service that has an interaction case, and</li>
 *   <li>every DTS service is covered by at least one proving artifact — a
 *   static widget render, a published surface case, or a per-service deep
 *   matrix. Coverage reasons are recorded per service so a full run has
 *   zero <em>unexplained</em> passes or gaps.</li>
 * </ul>
 *
 * <p>Named exception, not drift: the forms deep matrix
 * ({@code tests/forms-confirmation-matrix.spec.js}) is owned by #6482,
 * which is deliberately held out of this run — the index cites the forms
 * interaction row plus the {@code percForm} static case as its forms
 * evidence and records the held-out deep matrix as a named note instead of
 * importing it.</p>
 *
 * <p>Pure inventory logic — no live CMS calls — so the node unit suite
 * covers it without a cell.</p>
 */

const path = require("node:path");
const createMatrix = require("./content-create-matrix");
const widgetMatrix = require("./published-widget-matrix");
const dtsMatrix = require("./dts-interaction-matrix");
const widgetSurfaces = require("./published-widget-surfaces");

const MATRIX_SUITE = "coverage-matrix-index";
const MATRIX_SPEC = "tests/coverage-matrix-index.spec.js";
const DOC_PAGE = "product-docs/8.2/admin/qa-coverage-matrices.md";
const DOC_ID = "admin-qa-coverage-matrices";

/**
 * Per-service deep-matrix registry: the proving spec per DTS service.
 * Values are module-relative spec paths (read-only references — this
 * module never imports them, so held-out #6482 stays untouched).
 */
const SERVICE_MATRICES = Object.freeze({
  polls: "tests/polls-vote-matrix.spec.js",
  feeds: "tests/feeds-entries-matrix.spec.js",
  forms: "tests/forms-confirmation-matrix.spec.js",
  comments: "tests/comments-moderation-matrix.spec.js",
  metadata: "tests/metadata-index-matrix.spec.js",
  membership: "tests/membership-registration-matrix.spec.js",
});

/**
 * The three matrices under index, in epic order.
 *
 * @returns {{suite: string, spec: string, count: number}[]}
 */
function matrixInventories() {
  return [
    {
      suite: createMatrix.MATRIX_SUITE,
      spec: createMatrix.MATRIX_SPEC,
      count: createMatrix.BUILT_IN_TYPES.length,
    },
    {
      suite: widgetMatrix.MATRIX_SUITE,
      spec: widgetMatrix.MATRIX_SPEC,
      count: widgetMatrix.WIDGETS.length,
    },
    {
      suite: dtsMatrix.MATRIX_SUITE,
      spec: dtsMatrix.MATRIX_SPEC,
      count: dtsMatrix.DTS_SERVICES.length,
    },
  ];
}

/**
 * @param {string[]} names
 * @returns {string[]} names that appear more than once (empty when closed)
 */
function findDuplicates(names) {
  const seen = new Set();
  const dupes = new Set();
  for (const name of names) {
    if (seen.has(name)) {
      dupes.add(name);
    }
    seen.add(name);
  }
  return [...dupes].sort();
}

/**
 * Dynamic widgets whose {@code dts} link names no DTS interaction service.
 *
 * @returns {{widget: string, dts: string}[]}
 */
function uncoveredDynamicWidgets() {
  const services = new Set(dtsMatrix.DTS_SERVICES.map((entry) => entry.name));
  return widgetMatrix.WIDGETS.filter(
    (entry) => entry.dts !== null && entry.dts !== undefined && !services.has(entry.dts),
  ).map((entry) => ({ widget: entry.name, dts: entry.dts }));
}

/**
 * Per-service coverage accounting: every DTS service with the artifacts
 * that prove it (static widget renders, published surface cases, deep
 * matrix spec). Membership has no published-surface row — its widgets and
 * deep matrix are the evidence, recorded here by name.
 *
 * @returns {{service: string, widgets: string[], surface: boolean, deepMatrix: string|null, note: string, covered: boolean}[]}
 */
function serviceCoverage() {
  const surfaceByService = new Map(
    widgetSurfaces.SURFACES.map((entry) => [entry.name, entry.widget]),
  );
  return dtsMatrix.DTS_SERVICES.map((entry) => {
    const widgets = widgetMatrix.WIDGETS.filter(
      (widget) => widget.dts === entry.name,
    ).map((widget) => widget.name);
    const surfaceWidget = surfaceByService.get(entry.name) || null;
    const deepMatrix = SERVICE_MATRICES[entry.name] || null;
    let note = "";
    if (entry.name === "forms") {
      note =
        "forms deep matrix owned by #6482 (held out of this run); " +
        "index evidence is the interaction row plus the percForm static case";
    }
    if (entry.name === "membership") {
      note =
        "no published-surface row; evidence is the widget renders plus " +
        "the membership deep matrix";
    }
    return {
      service: entry.name,
      widgets,
      surface: surfaceWidget !== null,
      deepMatrix,
      note,
      covered:
        widgets.length > 0 || surfaceWidget !== null || deepMatrix !== null,
    };
  });
}

/**
 * @returns {string[]} DTS service names with no proving artifact
 */
function unaccountedServices() {
  return serviceCoverage()
    .filter((row) => !row.covered)
    .map((row) => row.service);
}

/**
 * @param {string} helperDir {@code __dirname} of the caller (tests/helpers)
 * @returns {string} absolute repo root (portable: resolved, never concatenated)
 */
function repoRootFromHelper(helperDir) {
  return path.resolve(
    String(helperDir),
    "..",
    "..",
    "..",
    "..",
    "..",
  );
}

/**
 * @param {string} repoRoot absolute repo root
 * @returns {string} absolute path of the operator docs page
 */
function docPagePath(repoRoot) {
  return path.join(String(repoRoot), DOC_PAGE);
}

/**
 * Aggregate summary for the Playwright index spec and full-run evidence.
 *
 * @returns {{suite: string, spec: string, matrices: {suite: string, spec: string, count: number}[], services: {service: string, widgets: string[], surface: boolean, deepMatrix: string|null, note: string, covered: boolean}[], updated: string}}
 */
function indexSummary() {
  return {
    suite: MATRIX_SUITE,
    spec: MATRIX_SPEC,
    matrices: matrixInventories(),
    services: serviceCoverage(),
    updated: new Date().toISOString().slice(0, 10),
  };
}

module.exports = {
  MATRIX_SUITE,
  MATRIX_SPEC,
  DOC_PAGE,
  DOC_ID,
  SERVICE_MATRICES,
  matrixInventories,
  findDuplicates,
  uncoveredDynamicWidgets,
  serviceCoverage,
  unaccountedServices,
  repoRootFromHelper,
  docPagePath,
  indexSummary,
};
