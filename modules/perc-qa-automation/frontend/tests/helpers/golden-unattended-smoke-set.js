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
 * Golden / unattended Playwright smoke surface inventory (#2490 / parent #2423).
 *
 * <p><strong>Decision:</strong> include {@code @folder-recycle} in the
 * <em>extended</em> golden unattended multi-path set. The minimal default
 * ({@code npm run test:golden}) remains login + Content Explorer only so agents
 * never default to the full suite.</p>
 *
 * <p>Used by unit tests and docs; keep in lockstep with package.json scripts
 * {@code test:golden} and {@code test:golden-extended}.</p>
 *
 * @module helpers/golden-unattended-smoke-set
 */

"use strict";

/**
 * @typedef {"baseline" | "extended"} GoldenTier
 */

/**
 * @typedef {object} GoldenSmokeEntry
 * @property {string} id stable inventory id
 * @property {string} file Playwright spec under tests/
 * @property {string} path path relative to frontend/ for surface / playwright CLI
 * @property {string} tag primary Playwright title tag (without @)
 * @property {GoldenTier} tier baseline = test:golden; extended also in golden-extended
 * @property {string} [notes] short operator note
 */

/**
 * Spec path prefix used by npm scripts and surface filter (under frontend/).
 * @type {string}
 */
const TESTS_PREFIX = "tests/";

/**
 * Canonical golden unattended surface inventory.
 *
 * @type {GoldenSmokeEntry[]}
 */
const GOLDEN_UNATTENDED_SMOKE_SET = [
  {
    id: "golden-login-explorer",
    file: "golden-unattended-smoke.spec.js",
    path: `${TESTS_PREFIX}golden-unattended-smoke.spec.js`,
    tag: "golden",
    tier: "baseline",
    notes:
      "Minimal unattended reference: Admin login + Content Explorer (#2065 / #1928)",
  },
  {
    id: "folder-recycle",
    file: "folder-recycle-smoke.spec.js",
    path: `${TESTS_PREFIX}folder-recycle-smoke.spec.js`,
    tag: "folder-recycle",
    tier: "extended",
    notes:
      "Post-#2423 pathmanagement + recycle REST smoke; optional overnight extended set (#2490)",
  },
];

/**
 * Entries in the minimal golden set ({@code npm run test:golden}).
 *
 * @returns {GoldenSmokeEntry[]}
 */
function listBaselineEntries() {
  return GOLDEN_UNATTENDED_SMOKE_SET.filter((e) => e.tier === "baseline");
}

/**
 * Entries in the extended golden set ({@code npm run test:golden-extended}):
 * baseline plus optional surfaces such as folder-recycle.
 *
 * @returns {GoldenSmokeEntry[]}
 */
function listExtendedEntries() {
  return GOLDEN_UNATTENDED_SMOKE_SET.slice();
}

/**
 * Playwright CLI path args for a tier (relative to frontend/).
 *
 * @param {GoldenTier} tier
 * @returns {string[]}
 */
function pathsForTier(tier) {
  if (tier === "baseline") {
    return listBaselineEntries().map((e) => e.path);
  }
  if (tier === "extended") {
    return listExtendedEntries().map((e) => e.path);
  }
  throw new TypeError(`Unknown golden tier: ${tier}`);
}

/**
 * @param {string} id inventory id
 * @returns {GoldenSmokeEntry}
 */
function getGoldenEntry(id) {
  const found = GOLDEN_UNATTENDED_SMOKE_SET.find((e) => e.id === id);
  if (!found) {
    throw new Error(`Unknown golden unattended smoke entry id: ${id}`);
  }
  return found;
}

module.exports = {
  GOLDEN_UNATTENDED_SMOKE_SET,
  TESTS_PREFIX,
  listBaselineEntries,
  listExtendedEntries,
  pathsForTier,
  getGoldenEntry,
};
