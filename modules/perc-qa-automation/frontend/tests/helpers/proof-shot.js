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
 * Proof-screenshot helper (issue #7064).
 *
 * <p>Playwright proof captures must land in the gitignored
 * {@code test-results/} tree so routine runs never dirty the worktree by
 * overwriting the tracked PNGs under {@code docs/ai-generated/proofs/}.
 * The curated proofs PNGs are refreshed only when
 * {@code PERC_REFRESH_PROOFS=1} is set, which copies the fresh capture
 * over the committed file — an intentional, reviewable refresh whose
 * diff shows up in {@code git status} on purpose.
 *
 * <p>Paths are anchored at this file's {@code __dirname}, never at the
 * process working directory, so specs behave identically whether they run
 * from {@code frontend/} or the repo root.
 *
 * <p>Test seams: {@code PERC_TEST_RESULTS_DIR} and
 * {@code PERC_PROOFS_DIR} override the capture and curated directories
 * (used by the hermetic unit test; also handy for custom CI artifact
 * dirs).
 *
 * <p>Usage:
 * {@code const { proofScreenshot } = require("./helpers/proof-shot");}
 * {@code await proofScreenshot(page, "issue-1234-surface.png");}
 * Refresh:
 * {@code PERC_REFRESH_PROOFS=1 npm run test:surface -- --path tests/foo.spec.js}
 */

"use strict";

const fs = require("node:fs");
const path = require("node:path");

const FRONTEND_DIR = path.join(__dirname, "..", "..");
const REPO_ROOT = path.join(FRONTEND_DIR, "..", "..", "..");

function testResultsDir() {
  return (
    process.env.PERC_TEST_RESULTS_DIR || path.join(FRONTEND_DIR, "test-results")
  );
}

function proofsDir() {
  return (
    process.env.PERC_PROOFS_DIR ||
    path.join(REPO_ROOT, "docs", "ai-generated", "proofs")
  );
}

function shouldRefreshProofs() {
  const raw = String(process.env.PERC_REFRESH_PROOFS || "")
    .trim()
    .toLowerCase();
  return raw === "1" || raw === "true" || raw === "yes";
}

function resolveProofPaths(fileName) {
  return {
    shotPath: path.join(testResultsDir(), fileName),
    proofPath: path.join(proofsDir(), fileName),
  };
}

/**
 * Captures a screenshot to the gitignored {@code test-results/} tree and,
 * only when {@code PERC_REFRESH_PROOFS=1}, copies it over the curated
 * {@code docs/ai-generated/proofs/} file.
 *
 * @param {object} target Playwright {@code Page} or {@code Locator}
 *   (both expose {@code screenshot({ path })}).
 * @param {string} fileName proof file name, e.g.
 *   {@code "issue-1234-surface.png"}.
 * @param {object} [options] extra screenshot options; {@code path} is
 *   always overridden so callers cannot bypass the proofs guard.
 * @returns {Promise<{shotPath: string, proofPath: string, refreshed: boolean}>}
 */
async function proofScreenshot(target, fileName, options = {}) {
  const { shotPath, proofPath } = resolveProofPaths(fileName);
  fs.mkdirSync(path.dirname(shotPath), { recursive: true });
  await target.screenshot({ ...options, path: shotPath });
  let refreshed = false;
  if (shouldRefreshProofs()) {
    fs.mkdirSync(path.dirname(proofPath), { recursive: true });
    fs.copyFileSync(shotPath, proofPath);
    refreshed = true;
  }
  return { shotPath, proofPath, refreshed };
}

module.exports = {
  proofScreenshot,
  resolveProofPaths,
  shouldRefreshProofs,
  testResultsDir,
  proofsDir,
};
