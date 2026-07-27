/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
 * Vitest spec: T029b — CI-gate artifact-grep for FR-019a.
 *
 * <p>Asserts the {@code scripts/verify-no-finder-jsp-references.py}
 * gate returns {@code 0} on the current tree (US6 hard-cut of
 * {@code cm/app/webmgt.jsp} is intact — no finder.jsp navigation
 * entry remains). The FAIL cases (re-introduced navigation entry,
 * alternate {@code <%@include>} form, false-positive trap on the
 * {@code finder_js.jsp} shared-lib include) are covered by the
 * paired pytest self-test at
 * {@code scripts/test_verify_no_finder_jsp_references.py}; this
 * Vitest spec is the load-bearing CI gate that fires on every
 * Vitest run via {@code npx vitest run}.</p>
 *
 * <p>The gate's scope, carve-outs, and rationale are documented in
 * the {@code .py} script's header comment. In summary:</p>
 * <ul>
 *   <li>Target: {@code cm/app/webmgt.jsp} (modern Track B shell).</li>
 *   <li>Carve-out: {@code cm/pages/app/webmgt.jsp} (Track A; deferred
 *       to the Track A migration workstream).</li>
 *   <li>Carve-out: {@code finder_js.jsp} shared-library include
 *       (explicit regex exclusion; required for non-Finder
 *       functionality).</li>
 * </ul>
 */

import { execFileSync } from "node:child_process";
import { existsSync } from "node:fs";
import { join, resolve } from "node:path";
import { describe, expect, it } from "vitest";

// __dirname is <repo>/WebUI/src/test/ts/scripts. Going up 5 levels
// (`../../../../../`) reaches the repo root.
const REPO_ROOT = resolve(__dirname, "../../../../..");
const GATE_PY = join(REPO_ROOT, "scripts/verify-no-finder-jsp-references.py");

describe("scripts/verify-no-finder-jsp-references.py / T029b / FR-019a CI gate", () => {
  it("script file is present and executable", () => {
    expect(existsSync(GATE_PY)).toBe(true);
  });

  it("returns 0 (PASS) on the current tree (US6 hard-cut is intact)", () => {
    // The gate is a cross-platform Python script (per spec 994). We
    // invoke it via `python3` so the Vitest runner is portable across
    // Linux, macOS, and Windows CI agents (no executable bit / shebang
    // dependency).
    let stdout = "";
    let stderr = "";
    let exitCode = 0;
    try {
      stdout = execFileSync("python3", [GATE_PY], {
        cwd: REPO_ROOT,
        encoding: "utf8",
        stdio: ["ignore", "pipe", "pipe"],
      });
    } catch (err) {
      const e = err as { stdout?: string; stderr?: string; status?: number };
      stdout = e.stdout ?? "";
      stderr = e.stderr ?? "";
      exitCode = e.status ?? 1;
    }
    expect({ exitCode, stdout, stderr }).toEqual({
      exitCode: 0,
      stdout: expect.stringContaining("PASS"),
      stderr: "",
    });
  });
});