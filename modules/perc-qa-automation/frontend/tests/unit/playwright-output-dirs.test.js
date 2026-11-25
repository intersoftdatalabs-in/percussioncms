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
 * Regression test for issue #7263: Playwright clears {@code outputDir} at
 * the start of every invocation, so the matrix evidence JSONs
 * ({@code test-results/*.json}) were silently wiped whenever a later spec
 * ran (creation matrix evidence gone after the inventory/index run). The
 * config must keep {@code outputDir} (failure artifacts) outside the
 * {@code test-results/} evidence dir, and the module {@code .gitignore}
 * must cover the artifact dir.
 *
 * No live CMS required: {@code npm run test:unit}.
 */

"use strict";

const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { describe, it } = require("node:test");

const FRONTEND_DIR = path.join(__dirname, "..", "..");
const MODULE_DIR = path.join(FRONTEND_DIR, "..");
const EVIDENCE_DIR_NAME = "test-results";

function loadConfig() {
  // eslint-disable-next-line global-require -- test-only config load.
  return require("../../playwright.config.js");
}

function evidenceSpecs() {
  // Matrix + index specs persist evidence JSONs into test-results/ via
  // afterAll writeFileSync. (content-create-inventory.spec.js only
  // validates the live catalog against the matrix helpers and writes no
  // evidence file, so it is not in this list.)
  const testsDir = path.join(FRONTEND_DIR, "tests");
  return fs
    .readdirSync(testsDir)
    .filter(
      (name) =>
        name.endsWith("-matrix.spec.js") || name === "coverage-matrix-index.spec.js",
    )
    .map((name) => path.join(testsDir, name));
}

describe("playwright output separation (issue #7263)", () => {
  it("config outputDir stays outside the test-results evidence dir", () => {
    const config = loadConfig();
    assert.ok(
      config.outputDir,
      "playwright.config.js must set an explicit outputDir",
    );
    const resolved = path.resolve(FRONTEND_DIR, config.outputDir);
    assert.notEqual(
      path.basename(resolved),
      EVIDENCE_DIR_NAME,
      `outputDir (${config.outputDir}) must not be the ${EVIDENCE_DIR_NAME}/ evidence dir`,
    );
    assert.ok(
      !resolved.startsWith(path.join(FRONTEND_DIR, EVIDENCE_DIR_NAME) + path.sep),
      `outputDir (${config.outputDir}) must not live inside ${EVIDENCE_DIR_NAME}/`,
    );
  });

  it("evidence specs still write their JSONs into test-results/", () => {
    const files = evidenceSpecs();
    assert.ok(files.length > 0, "expected at least one evidence spec");
    const offenders = [];
    for (const file of files) {
      const source = fs.readFileSync(file, "utf8");
      const writesJson =
        /\.json/.test(source) && source.includes(EVIDENCE_DIR_NAME);
      if (!writesJson) {
        offenders.push(path.basename(file));
      }
    }
    assert.deepEqual(
      offenders,
      [],
      `evidence specs must keep writing matrix JSONs under ${EVIDENCE_DIR_NAME}/: ` +
        offenders.join(", "),
    );
  });

  it("module .gitignore covers the configured outputDir", () => {
    const config = loadConfig();
    const dirName = path.basename(path.resolve(FRONTEND_DIR, config.outputDir));
    const ignore = fs.readFileSync(path.join(MODULE_DIR, ".gitignore"), "utf8");
    const covers = ignore
      .split("\n")
      .map((line) => line.trim())
      .some((line) => line === dirName || line === `${dirName}/`);
    assert.ok(
      covers,
      `modules/perc-qa-automation/.gitignore must ignore the Playwright outputDir (${dirName})`,
    );
  });
});
