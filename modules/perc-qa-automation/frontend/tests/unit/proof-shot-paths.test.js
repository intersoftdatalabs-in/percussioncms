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

const { describe, it, afterEach } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const {
  proofScreenshot,
  resolveProofPaths,
  shouldRefreshProofs,
} = require("../helpers/proof-shot");

const SAVED_ENV = {};
for (const key of [
  "PERC_REFRESH_PROOFS",
  "PERC_TEST_RESULTS_DIR",
  "PERC_PROOFS_DIR",
]) {
  SAVED_ENV[key] = process.env[key];
}

afterEach(() => {
  for (const key of Object.keys(SAVED_ENV)) {
    if (SAVED_ENV[key] === undefined) {
      delete process.env[key];
    } else {
      process.env[key] = SAVED_ENV[key];
    }
  }
});

describe("proof-shot helper (issue #7064)", () => {
  it("resolveProofPaths anchors test-results at the frontend tree, not cwd", () => {
    const before = process.cwd();
    const { shotPath } = resolveProofPaths("issue-7064-anchor.png");
    assert.ok(
      shotPath.endsWith(
        path.join("perc-qa-automation", "frontend", "test-results", "issue-7064-anchor.png"),
      ),
      `shotPath must live under frontend/test-results, got: ${shotPath}`,
    );
    fs.mkdtempSync(path.join(os.tmpdir(), "proof-shot-cwd-"));
    process.chdir(os.tmpdir());
    try {
      const again = resolveProofPaths("issue-7064-anchor.png");
      assert.equal(again.shotPath, shotPath);
    } finally {
      process.chdir(before);
    }
  });

  it("shouldRefreshProofs only honors explicit opt-in values", () => {
    delete process.env.PERC_REFRESH_PROOFS;
    assert.equal(shouldRefreshProofs(), false);
    process.env.PERC_REFRESH_PROOFS = "0";
    assert.equal(shouldRefreshProofs(), false);
    process.env.PERC_REFRESH_PROOFS = "1";
    assert.equal(shouldRefreshProofs(), true);
    process.env.PERC_REFRESH_PROOFS = "true";
    assert.equal(shouldRefreshProofs(), true);
  });

  it("proofScreenshot writes to test-results and leaves proofs alone by default", async () => {
    const scratch = fs.mkdtempSync(path.join(os.tmpdir(), "proof-shot-"));
    const shots = path.join(scratch, "shots");
    const proofs = path.join(scratch, "proofs");
    process.env.PERC_TEST_RESULTS_DIR = shots;
    process.env.PERC_PROOFS_DIR = proofs;
    delete process.env.PERC_REFRESH_PROOFS;
    const fake = {
      screenshot: async ({ path: target }) => {
        fs.writeFileSync(target, "fake-png-bytes");
      },
    };
    const result = await proofScreenshot(fake, "issue-7064-default.png");
    assert.equal(result.refreshed, false);
    assert.equal(
      fs.readFileSync(path.join(shots, "issue-7064-default.png"), "utf8"),
      "fake-png-bytes",
    );
    assert.equal(fs.existsSync(path.join(proofs, "issue-7064-default.png")), false);
  });

  it("proofScreenshot copies into proofs only when PERC_REFRESH_PROOFS=1", async () => {
    const scratch = fs.mkdtempSync(path.join(os.tmpdir(), "proof-shot-"));
    const shots = path.join(scratch, "shots");
    const proofs = path.join(scratch, "proofs");
    process.env.PERC_TEST_RESULTS_DIR = shots;
    process.env.PERC_PROOFS_DIR = proofs;
    process.env.PERC_REFRESH_PROOFS = "1";
    const fake = {
      screenshot: async ({ path: target }) => {
        fs.writeFileSync(target, "fake-png-bytes");
      },
    };
    const result = await proofScreenshot(fake, "issue-7064-refresh.png");
    assert.equal(result.refreshed, true);
    assert.equal(
      fs.readFileSync(path.join(proofs, "issue-7064-refresh.png"), "utf8"),
      "fake-png-bytes",
    );
  });
});

/**
 * Removes block comments and `//` line comments that sit outside string
 * literals, so the lint below only sees code (specs may mention the proofs
 * directory in comments explaining the intentional-copy convention).
 */
function stripComments(source) {
  const withoutBlocks = source.replace(/\/\*[\s\S]*?\*\//g, "");
  return withoutBlocks
    .split("\n")
    .map((line) => {
      let quote = null;
      for (let i = 0; i < line.length; i += 1) {
        const ch = line[i];
        if (quote) {
          if (ch === "\\") {
            i += 1;
          } else if (ch === quote) {
            quote = null;
          }
        } else if (ch === '"' || ch === "'" || ch === "`") {
          quote = ch;
        } else if (ch === "/" && line[i + 1] === "/") {
          return line.slice(0, i);
        }
      }
      return line;
    })
    .join("\n");
}

function collectSpecFiles(dir) {
  const found = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      found.push(...collectSpecFiles(full));
    } else if (entry.isFile() && entry.name.endsWith(".spec.js")) {
      found.push(full);
    }
  }
  return found;
}

describe("proof-shot repo lint (issue #7064)", () => {
  it("no spec writes screenshots into tracked docs/ai-generated/proofs", () => {
    const testsDir = path.join(__dirname, "..");
    const offenders = [];
    for (const file of collectSpecFiles(testsDir)) {
      const code = stripComments(fs.readFileSync(file, "utf8"));
      if (
        code.includes("ai-generated/proofs") ||
        /["']proofs["']/.test(code)
      ) {
        offenders.push(path.relative(testsDir, file));
      }
    }
    assert.deepEqual(
      offenders,
      [],
      `specs must capture into test-results/ via helpers/proof-shot ` +
        `(PERC_REFRESH_PROOFS=1 for intentional refresh), ` +
        `but these still reference the tracked proofs tree in code: ` +
        offenders.join(", "),
    );
  });
});
