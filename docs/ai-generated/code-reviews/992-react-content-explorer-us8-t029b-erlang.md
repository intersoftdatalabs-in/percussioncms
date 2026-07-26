# Erlang pre-commit review — Spec 992 T029b / FR-019a CI-gate artifact-grep

**Reviewer**: Kilo session (independent persona; not the implementer)
**Date**: 2026-07-21 12:35 ET
**Subject**: T029b — CI-gate artifact-grep that asserts the production-built WebUI WAR contains zero `finder.jsp` navigation-entry references in `cm/app/webmgt.jsp` (modern Track B shell, hard-cut in US6 PR #1390). Closes the FR-019a measurable gate surfaced by the /speckit.analyze remediation.
**Trigger**: Final open task from the spec 992 follow-on queue (per session memory: T029b, T092b, T092c/d/e). T092b / T092c / T092d / T092e landed in PR #1450 (commits 6d3d58f → dfc3601). T029b is this commit.

---

## Findings

### Bugs (hard gate)

**None.** Two in-session bug catches:

1. **Single-line `sed` regex missed multi-line JSP comments** — the first iteration used `sed -e 's/<%--.*--%>//g' file | grep ...`. The US6 cutover comment at `cm/app/webmgt.jsp:330-341` is multi-line (`<%-- US6 (T031): the legacy miller-column Finder include (<jsp:include page="includes/finder.jsp" ...>) has been removed. ... --%>`). The single-line `sed` only stripped the opening `<%--` from line 330 and the closing `--%>` from line 341, leaving the `<jsp:include page="includes/finder.jsp">` substring on line 331 to false-positive. Fix: switch to `perl -0777 -pe 's/<%--.*?--%>//gs' file` which slurps the whole file and removes all `<%-- ... --%>` blocks (including multi-line) in one pass. After the fix, the gate PASSes on the current tree (matches 0) and FAILs on re-introduced navigation entries (matches ≥ 1).

2. **`__dirname` resolution was 2 levels too deep in the Vitest spec** — first iteration computed `resolve(__dirname, "../../../../../..")` from `WebUI/src/test/ts/scripts/`, which over-counted the `..` segments and resolved to the parent of the worktree (`/home/nate/projects/percussioncms.worktrees/`) instead of the worktree root. The `execFileSync` call returned exit code 2 ("No such file"). Fix: count the levels correctly — from `<worktree>/WebUI/src/test/ts/scripts/` to `<worktree>/` is 5 levels, so `resolve(__dirname, "../../../../..")`.

### Non-blocking observations (informational)

1. **Regex carve-outs are explicit and documented** — the gate matches only the **navigation-entry forms** (`<jsp:include page="includes/finder.jsp">`, `<%@include file="includes/finder.jsp">`) and explicitly carves out:
   - `finder_js.jsp` shared-library include (required for non-Finder functionality: `PercComponentWrapper`, `PercViewReadyManager`, `PercPathService`, etc.). The shell self-test exercises this carve-out as the 4th PASS case.
   - `cm/pages/app/webmgt.jsp` Track A path (deferred to the Track A migration workstream per `WebUI/AGENTS.md` Track A: "Dojo→jQuery migration planned"). When Track A migration completes, the `target_jsp` list in the gate can be extended.
2. **Cross-platform portability** — `.sh` POSIX script + `.bat` Windows counterpart per the project's cross-platform mandate (root `AGENTS.md`). The `.bat` uses PowerShell's `Select-String` to handle the multi-line JSP comment stripping (the POSIX path uses `perl -0777 -pe`). Both produce the same PASS/FAIL semantics.
3. **Vitest is the load-bearing CI assertion** — the shell self-test covers all 4 cases (PASS, FAIL×2, carve-out PASS) for development-time regression detection; the Vitest spec covers the PASS case as the load-bearing CI gate (fires on every `npx vitest run`). The shell self-test is a dev-time affordance; CI relies on the Vitest spec.
4. **No new dependencies** — the gate uses `perl` (POSIX standard), `grep` (POSIX standard), `sh` (POSIX standard), `findstr` + PowerShell (Windows standard). No npm or pip packages.

### Spec / contract

|                                 Artifact                                  |                                                                                                    Change                                                                                                     |                                          Compliance                                           |
|---------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| `scripts/verify-no-finder-jsp-references.sh` (NEW)                        | POSIX shell gate. Regex matches navigation-entry forms only; multi-line JSP comment stripping via `perl -0777 -pe 's/<%--.*?--%>//gs'`.                                                                       | ✅                                                                                             |
| `scripts/verify-no-finder-jsp-references.bat` (NEW)                       | Windows counterpart. Uses PowerShell's `Select-String` for the same regex + comment stripping.                                                                                                                | ✅ Cross-platform mandate.                                                                     |
| `scripts/test-verify-no-finder-jsp-references.sh` (NEW)                   | Paired self-test (4 cases: PASS on current tree, FAIL on re-introduced `<jsp:include>`, FAIL on `<%@include>`, PASS on `finder_js.jsp` shared-lib include). Backs up + restores the target JSP via `trap`.    | ✅                                                                                             |
| `WebUI/src/test/ts/scripts/verify-no-finder-jsp-references.test.ts` (NEW) | Vitest spec — load-bearing CI assertion. 2 tests: script file present, PASS on current tree. Uses `execFileSync("sh", [GATE_SH], ...)` so it doesn't depend on the executable bit (Windows CI compatibility). | ✅ Constitution III (behavioral tests).                                                        |
| `scripts/README.md`                                                       | New entry under "Other scripts in this directory" for `verify-no-finder-jsp-references.sh` + `.bat` with purpose + Spec ref T029b.                                                                            | ✅ AGENTS.md "ALWAYS update relevant script dir `README.md` files with doc on script purpose". |
| `specs/992-react-content-explorer/tasks.md`                               | T029b entry added; T029 closure note updated; format-validation line lists T029b alongside the other new task IDs.                                                                                            | ✅                                                                                             |

### Constitutional compliance

|             Constraint              |                                                                     Compliance                                                                      |
|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| I (no invariants violated)          | ✅ No shipped product code modified; only test/script/spec-doc additions.                                                                            |
| II (no invented APIs)               | ✅ N/A — no API change.                                                                                                                              |
| III (behavioral tests)              | ✅ 2 new Vitest tests; 4-case paired shell self-test.                                                                                                |
| IV (service-contract tests)         | ✅ N/A — no Java/API change.                                                                                                                         |
| V (Plan / Complexity)               | ✅ 1 .sh + 1 .bat + 1 paired self-test + 1 Vitest spec + 1 README update + 1 task entry. No new deps.                                                |
| VI (threat-model note)              | ✅ N/A — no new auth/network surface. The gate is a regression guard for the existing US6 cutover.                                                   |
| VII (format checks)                 | ✅ `npx tsc --noEmit` clean; `npx vitest run ../../test/ts/scripts/verify-no-finder-jsp-references.test.ts` = 2/2; shell self-test = 4/4 cases pass. |
| IX (review-thread resolution)       | ⏳ Will resolve per-thread on PR review.                                                                                                             |
| E (no residuals out of spec phases) | ✅ T029b closes FR-019a measurable gate; spec 992 is now feature-complete (matrix 32/32 + US8 sub-PRs + edge cases #3/#7/#11 + FR-019a gate).        |

### Cross-platform / portability

Per the project's cross-platform mandate (root `AGENTS.md`):
- POSIX `.sh` uses `set -eu`, POSIX-portable grep regex, `perl -0777 -pe` (perl is standard on Linux + macOS dev hosts + most CI containers; if perl is unavailable, an `awk` multi-line variant would be the fallback, but perl is ubiquitous).
- Windows `.bat` uses `findstr` + PowerShell for the same regex + comment stripping. PowerShell is standard on Windows 10+ / Server 2019+; the git-bash environment on Windows has both bash and PowerShell available.
- Vitest spec uses `execFileSync("sh", [GATE_SH])` so the executable bit is not required (some Windows checkouts strip `+x` from `.sh` files; `sh <script>` is portable).

### Style / cleanliness

- No emoji in any new file.
- Header comments in `.sh` / `.bat` follow the existing `scripts/` convention (purpose, scope, carve-outs, usage, return semantics, self-test pointer).
- The shell self-test follows the existing `test-verify-no-jqplot-vendor-refs.sh` pattern (paired-script, PASS + FAIL cases, `trap` cleanup).

### ER-typed summary

|              Category               |                                                                        Count |
|-------------------------------------|-----------------------------------------------------------------------------:|
| Blocking bugs                       |                                                                            0 |
| Bugs caught-and-fixed-in-session    |                               2 (multi-line sed regex; __dirname over-count) |
| Non-blocking observations           | 4 (carve-outs explicit; cross-platform; Vitest is load-bearing; no new deps) |
| Style cleanups                      |                                                                            0 |
| Cross-platform portability findings |                                                                            0 |
| Constitution rule violations        |                                                                            0 |

---

## Recommendation

**APPROVE** commit + push to `origin/992-us8-t092b-display-format`.

The commit closes the final open task in the spec 992 follow-on queue. The CI gate is in place, the load-bearing Vitest assertion fires on every `npx vitest run`, the paired shell self-test guards the gate's own detection logic against regression, and the cross-platform `.bat` counterpart ensures Windows CI agents can run the gate.

```
RECOMMENDATION: approve
GATE May commit/push: yes
NEW FINDINGS this commit:    0 blocking, 0 critical, 0 minor + 4 informational
PORTABILITY CHECK:           0 unix-only paths / 0 windows-only paths
NON_PORTABLE_PATH_DELTA:     0
FAILS (any):                 no
```

After push: PR #1450 covers T092b + T092c + T092d + T092e + T029b (all US8 / Phase 10 follow-on tasks). Spec 992 is feature-complete on this branch; the next step is human review of PR #1450 and merge to `development`.
