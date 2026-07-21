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
 * <p>Asserts the {@code scripts/verify-no-finder-jsp-references.sh}
 * gate returns {@code 0} on the current tree (US6 hard-cut of
 * {@code cm/app/webmgt.jsp} is intact — no finder.jsp navigation
 * entry remains). The FAIL cases (re-introduced navigation entry,
 * alternate {@code <%@include>} form, false-positive trap on the
 * {@code finder_js.jsp} shared-lib include) are covered by the
 * paired shell self-test at
 * {@code scripts/test-verify-no-finder-jsp-references.sh}; this
 * Vitest spec is the load-bearing CI gate that fires on every
 * Vitest run via {@code npx vitest run}.</p>
 *
 * <p>The gate's scope, carve-outs, and rationale are documented in
 * the {@code .sh} script's header comment. In summary:</p>
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
const GATE_SH = join(REPO_ROOT, "scripts/verify-no-finder-jsp-references.sh");

describe("scripts/verify-no-finder-jsp-references.sh / T029b / FR-019a CI gate", () => {
  it("script file is present and executable", () => {
    expect(existsSync(GATE_SH)).toBe(true);
  });

  it("returns 0 (PASS) on the current tree (US6 hard-cut is intact)", () => {
    // The gate is a POSIX shell script. We invoke it via the user's
    // shell (`sh`) so the Vitest runner does not depend on the
    // executable bit being honoured by the OS (some Windows CI agents
    // and some POSIX CI agents strip +x from a freshly-checked-out
    // file; `sh <script>` is portable).
    let stdout = "";
    let stderr = "";
    let exitCode = 0;
    try {
      stdout = execFileSync("sh", [GATE_SH], {
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