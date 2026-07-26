# Erlang Review — Spec 994 Phase 7 Polish (branch `994-python-build-scripts-phase7`)

## Summary

Phase 7 closes out spec 994-python-build-scripts. The diff is **docs-only** plus the deletion of 6 unreferenced historical `.sh` helper scripts under `docs/ai-generated/tasks/#000-webui-src-layout/` (T078 R3 carve-out). The dominant risk is **broken doc references** — I traced each one and applied `.sh → .py` only to **live** user-facing docs, intentionally preserving **historical** task-internal references and intentional "Replaces:" migration docstrings. The single non-doc change is the fix to the WebUI FR-019a Vitest CI gate (`verify-no-finder-jsp-references.test.ts`), which now invokes the Python entry point via `python3` instead of the deleted `.sh`; the test passes locally (2/2).

## Scope

- **Base:** `origin/development` (commit `04edfd2fc6` — PR #1469 US5 merge)
- **Head:** branch `994-python-build-scripts-phase7` (working tree, pre-commit)
- **Files:** 20 changed, +34 / -1048
- **Prior report:** `docs/ai-generated/code-reviews/994-python-build-scripts-us5-erlang.md` (last spec 994 Erlang report)
- **Memory patterns hit:** `paths.windows-backslash-as_posix` (Phase 5 follow-up), `tests.structural-only` (Vitest CI-gate pattern, applied to fix here)

## Recommendation

**approve**

## Gate

- **Blocking bugs:** 0
- **May commit/push: yes**

## Issues

No blocking issues.

## Re-review (commit `57a3905180`, post-initial-review)

Kilo-code-bot flagged CRITICAL dbId 3634830202: `docker-compose.yml` contained **TWO unresolved merge-conflict zones** (lines 8-25 and lines 53-64) inherited from `origin/development` since PRs #1386/#1389 (commits `2d35a93c49` vs `052c18b956`). My Phase 7 initial commit (`4da0e8a30e`) edited the file without resolving the conflicts — Erlang review was blind to this because the conflict markers pre-existed on the base branch.

**Fix applied in commit `57a3905180`:**
- Zone 1 (cms-dts.user): kept HEAD `${CMS_UID:-1000}:${CMS_GID:-1001}` env-var override; rejected hardcoded `user: "1000:1001"` from commit 2d35a93c49 (env-var-driven is portable across hosts per AGENTS.md cross-platform rules).
- Zone 2 (healthcheck + top-level volumes): kept HEAD content (retries + start_period + `volumes: mysql-data:` named volume); rejected bare retries/start_period from both incoming branches.

**Verified:**
- No `<<<<<<<`, `=======`, or `>>>>>>>` markers remain.
- YAML parses via PyYAML: `services=[cms-dts]`, `volumes=[mysql-data]`, `cms-dts.user="${CMS_UID:-1000}:${CMS_GID:-1001}"`, `healthcheck.retries=60`, `healthcheck.start_period=30s`.
- Kilo-code-bot review thread resolved (dbId 3634830202 → PRRT_kwDOKZBp3M6TG-Ev `isResolved: true`).

**Process improvement noted:** Erlang review on a branch must explicitly compare the working tree against `origin/development`'s blob, not just the diff line count, when the base branch may contain stale unresolved conflict markers. A pre-review `git show origin/development:<file>` check for any file in the diff that has no recorded modification history since the last clean base is warranted.

## Side benefit

Fixes a long-broken pre-existing dev container: `docker compose up` against the prior `docker-compose.yml` would have failed to parse the YAML due to the active conflict markers. Any developer running the docker dev runtime post-PR #1386 was silently affected.

## Memory

- `spec_994_phase7_merge_conflict_inheritance`: Erlang review must verify base-branch file content (not just the diff) when files appear "unchanged" in the diff stat but exist with broken markers on the base.

### Notes (non-blocking)

1. **`WebUI/src/test/ts/scripts/verify-no-finder-jsp-references.test.ts`** — Fixed a real bug introduced by Phase 2 (PR #1463): the Vitest CI gate still invoked the deleted `scripts/verify-no-finder-jsp-references.sh` via `sh`. The test asserted `existsSync(GATE_SH)` (always false post-Phase 2) and tried to exec the missing file. I rewrote the test to invoke `scripts/verify-no-finder-jsp-references.py` via `python3`, which is portable across Linux/macOS/Windows and matches spec 994 FR-001a. **Local run:** 2/2 tests pass in 3.84 s. (`WebUI/src/test/ts/scripts/verify-no-finder-jsp-references.test.ts:43-85`)
2. **T078 R3 carve-out (delete)** — Applied the preferred outcome from `research.md R3` and `tasks.md T078`: removed 6 unreferenced `.sh` helpers under `docs/ai-generated/tasks/#000-webui-src-layout/` (planning artifacts for an unstarted future spec). The directory's `.md` files (plan, phase docs, README) are preserved — they're design docs, not scripts. No references to the deleted files exist anywhere else in the repo (verified via `git grep`).
3. **SC-006 doc-drift sweep** — Found 9 live `*.sh` references in live user-facing docs that were not covered by the per-directory PRs. All 9 fixed (see Files table). Intentionally **not** modified:
   - `docs/ai-generated/tasks/gh-codeql-alerts/*.md` (historical release-readiness snapshots; analog to erlang review reports — preserve as historical artifacts).
   - `specs/001, 002, 004, 006/` (historical completed specs).
   - `specs/992-react-content-explorer/tasks.md` (T012f historical record; describes what was done 2026-07-19).
   - `scripts/*/...py` header docstrings like `r"""Cross-platform Python port of scripts/install-cms-dev.sh."""` (intentional migration documentation).
   - `specs/994-python-build-scripts/{contracts/cli-schemas.md,research.md,spec.md,quickstart.md}` (intentional migration documentation; the `Replaces:` lines, the `sh.LEGACY` SC-005 reference, and historical design discussion are all by design).
   - `docker/README.md` "Cross-platform replacement for the legacy `perc-devctl.sh`" lines (intentional migration documentation).
   - `modules/perc-distribution-tree/src/main/java/.../VerifyJdbcDrivers.java:39` Java comment (historical code reference).
4. **T079 / T080 spot-checks** — Verified `scripts/README.md` FR-014 in-scope/out-of-scope section (already complete from T031) and confirmed no in-scope `.sh/.bat` references slipped through into root `AGENTS.md` (only `mvn-env.{sh,bat}` remains, which is exempt per Clarification Q2).

### Cross-platform path review

**No issues.** This Phase is docs-only — no production code or test path-handling logic changed. The one behavioral change (`verify-no-finder-jsp-references.test.ts`) replaces a `sh <script>` invocation with `python3 <script>`, which is more portable (no shebang/executable-bit dependency), matching spec 994 FR-001a.

### Phase 7 verification (Scenario F excerpt)

|   SC   |         Result         |                                                                                                                                                                             Notes                                                                                                                                                                              |
|--------|------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| SC-001 | ✅                      | Survivors in `git ls-files` are vendor/runtime (`catalina.sh`, `digest.sh`, `perc-ant/bin/*`, `perc-packages/src/main/scripts/pkgBuild.{sh,bat}`, `system/release/tomcat/...`, `deliverytiersuite/.../tomcat*/bin/*`, `scripts/run-python-tests.sh` is the foundation runner); all explicitly out-of-scope per FR-013 / T004.                                  |
| SC-002 | ✅                      | `bash scripts/run-python-tests.sh --skip-install` → **344 passed, 1 skipped** in 13.38 s. (Local Debian 12 PEP 668 forces `--skip-install`; CI ubuntu-latest installs cleanly.)                                                                                                                                                                                |
| SC-003 | ✅                      | GH Actions last 4 runs on `development` (post-PR #1469 merge): all `success/completed` for both ubuntu-latest and windows-latest.                                                                                                                                                                                                                              |
| SC-004 | ✅                      | `mvn-env.sh` (head: `JAVA_HOME_21` + `set -e`) and `mvn-env.bat` (`@echo off` + `JAVA_HOME_21`) unchanged vs. pre-spec baseline.                                                                                                                                                                                                                               |
| SC-005 | n/a                    | Requires `scripts/verify-triage-inventory.sh.LEGACY` snapshot (not retained per tasks.md). Phase 2 PR #1463 already validated parity; no regression possible since the `.sh` is gone.                                                                                                                                                                          |
| SC-006 | ✅                      | 9 live references fixed; remaining matches are intentional/historical (see Notes 3).                                                                                                                                                                                                                                                                           |
| SC-007 | (manual, prior phases) | Per AGENTS.md pre-PR Maven gate: rest + projects/sitemanage (Phase 4 PR #1468) + modules/perc-distribution-tree (Phase 6 PR #1467) + modules/ai-shared-develop (Phase 4 PR #1468 + Phase 5 PR #1469) all `BUILD SUCCESS` with `Tests run: N, Failures: 0`. Phase 7 touches none of those modules' sources — only `rest/AGENTS.md` and `rest/README.md` (docs). |
| SC-008 | ✅                      | Runner `scripts/run-python-tests.{sh,cmd}` exists; idempotent re-run produces identical `344 passed, 1 skipped`.                                                                                                                                                                                                                                               |

## Files changed (20)

### Live doc fixes (12)

- `.env.compose.example:17,33` — `install-cms-dev.sh` → `.py`
- `WebUI/AGENTS.md:493,563` — `hot-deploy-local.sh` → `.py`
- `docker-compose.yml:28` — `install-cms-dev.sh` → `.py`
- `docs/docker/compose-dev.md:273` — `hot-deploy-jar.sh` → `.py`
- `modules/extensions-main/README.md:330` — `hot-deploy-local.sh` → `.py`
- `modules/extensions-main/src/site/markdown/adding-extensions.md:159` — `hot-deploy-local.sh` → `.py`
- `modules/extensions-main/src/site/markdown/runtime-lifecycle.md:90` — `hot-deploy-local.sh` → `.py`
- `rest/AGENTS.md:248` — `hot-deploy-local.sh` → `.py`
- `rest/README.md:161` — `hot-deploy-local.sh` → `.py`
- `specs/992-react-content-explorer/plan.md:23,127` — `install-cms-dev.sh` → `.py` (2 occurrences)
- `specs/992-react-content-explorer/quickstart.md:24,30` — `install-cms-dev.sh` → `.py` + `perc-devctl.sh` → `.py`
- `specs/992-react-content-explorer/spec.md:13,260` — `install-cms-dev.sh` → `.py` (2 occurrences)

### Behavioral fix (1)

- `WebUI/src/test/ts/scripts/verify-no-finder-jsp-references.test.ts` — Vitest CI gate now invokes `scripts/verify-no-finder-jsp-references.py` via `python3` (was broken: invoked deleted `.sh`)

### T078 R3 carve-out deletions (6)

- `docs/ai-generated/tasks/#000-webui-src-layout/check-migration-status.sh`
- `docs/ai-generated/tasks/#000-webui-src-layout/phase-1-migrate-all-files.sh`
- `docs/ai-generated/tasks/#000-webui-src-layout/phase-1-migrate-build-config.sh`
- `docs/ai-generated/tasks/#000-webui-src-layout/phase-1-migrate-structure.sh`
- `docs/ai-generated/tasks/#000-webui-src-layout/phase-1-test-single.sh`
- `docs/ai-generated/tasks/#000-webui-src-layout/phase-1-validate-changes.sh`

### Tick-box update (1)

- `specs/994-python-build-scripts/tasks.md` — `[ ] T078, T079, T080, T081, T082` → `[X]` (T083 and T084 remain `[ ]` until post-merge per the task wording)

## Handoff

- **Recommendation:** approve
- **Gate:** May commit/push: **yes**
- **No blocking bugs**
- Phase 7 is ready to commit and push to open the final docs(994) cleanup PR.

