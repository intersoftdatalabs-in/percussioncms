# Erlang Review — spec 994 US3 (erlang-harvest wrapper removal)

**Branch**: `994-python-build-scripts-us3` (off `origin/development` @ `c6c15259d4`)
**Date**: 2026-07-22
**Reviewer**: Erlang (implementer persona, self-review before commit; project rule
`.kilocode/rules/pre-commit-review.md` + root AGENTS.md "Pre-commit code review
(Erlang)")
**Scope**: Spec 994 Phase 3 / US3 — remove the now-redundant
`scripts/erlang-harvest-review-patterns.{sh,bat}` wrappers; confirm the Python
script is the sole entry point; update `scripts/README.md`; add a behavioral
regression test that pins the dangling-reference removal.

## Summary

This is the smallest phase of spec 994: a pure deletion plus one dangling-reference
fix plus one new behavioral pytest. The two wrappers were 13 + 24 lines of pure
delegation to the Python script (`exec "$PY" "$ROOT/scripts/erlang-harvest-review-patterns.py" "$@"` on Unix; `python "%ROOT%\scripts\erlang-harvest-review-patterns.py" %*` on Windows); removing them leaves Windows users running `python scripts\erlang-harvest-review-patterns.py` directly — same behavior, one fewer file. The Python script's auto-generated candidate report used to advertise the `.bat` wrapper as the Windows invocation; that block is now gone and a behavioral test pins it. `scripts/README.md` loses one obsolete out-of-scope note (the wrappers are no longer in-scope, so the "do NOT touch" reminder no longer applies) and one stale "US3, not converted in US2" parenthetical.

## Scope

- **Base**: `origin/development` @ `c6c15259d4` (US2 merge)
- **Head**: `994-python-build-scripts-us3` (uncommitted on this branch)
- **Files changed**: 5
  - `D  scripts/erlang-harvest-review-patterns.bat` (24 lines)
  - `D  scripts/erlang-harvest-review-patterns.sh` (13 lines)
  - `M  scripts/erlang-harvest-review-patterns.py` (-6 lines: remove the
    "Or on Windows: … .bat" block in `render_candidates_report`'s
    "How to promote" section)
  - `M  scripts/test_erlang_harvest_review_patterns.py` (+57 lines: new
    `test_report_does_not_reference_removed_wrappers` behavioral test)
  - `M  scripts/README.md` (-2 lines: remove the obsolete Phase 3 out-of-scope
    note; drop "(US3, not converted in US2)" parenthetical from the script
    heading)
- **Tests added**: 1 (regression guard for the dangling-reference removal)
- **Test runtime**: ~20s for full `scripts/` collection (106/106 pass on Linux)
- **Prior report**: `docs/ai-generated/code-reviews/994-python-build-scripts-us2-erlang.md`
  (loaded; US3-specific concerns not previously raised — US3 was a Phase 3
  deliverable queued behind Phase 2)
- **Memory patterns hit** (from `skills/erlang-review/patterns.md`):
  - `tests.structural-only` — checked; new test exercises the full
    `harvest.main([...])` → `render_candidates_report` → file-write path via
    `tempfile.TemporaryDirectory()` and `os.chdir(td_path)`, then asserts on
    the actual rendered string content (not on AST tokens)
  - `paths.hardcoded-sep` — checked; the new test uses
    `tempfile.TemporaryDirectory()` and `Path(__file__).resolve()` exclusively;
    no `"/" + name` or `"\\" + name` joins introduced

## Recommendation

**approve**

## Gate

- **Blocking bugs**: 0
- **May commit/push**: yes

## Cross-platform path / file I/O checklist

| Item | Status |
|------|--------|
| No hardcoded `/` or `\\` in filesystem-path joins | **PASS** — diff contains only a `python3 scripts/…` instruction string (literal text rendered into the candidate report's "How to promote" block, not a filesystem path) and the new test uses `Path` exclusively. The pre-existing `scripts\\erlang-harvest-review-patterns.bat` literal in the source tree is **deleted** by this PR; no replacement string is added. |
| No Unix-only absolute roots in tests | **PASS** — new test uses `tempfile.TemporaryDirectory()` (portable) + `os.chdir(td_path)` + `Path(...)` chains. |
| `subprocess.run` always uses argv lists with `shell=False` | **PASS** — no new subprocess calls; only an in-process function invocation (`harvest.main([...])`). The pre-existing subprocess usage in the Python script is untouched. |
| No third-party deps beyond pytest | **PASS** — no new imports. |
| Windows users have a portable entry point after wrapper removal | **PASS** — `python scripts\erlang-harvest-review-patterns.py --apply` works directly (per the script's own CLI design and the foundation US1 sentinel `test_mvn_env_untouched.py` patterns). The `.bat` wrapper was a convenience, not a portability requirement — the underlying Python entry point was always platform-portable. |
| `--skip-install` runner flag covers externally-managed Python envs (PEP 668) | **PASS** — pre-existing flag (foundation PR #1462); unrelated to US3 but noted because it makes the CI gate reliable on hosts where the runner's `pip install` step would fail with PEP 668 (this dev host included). |

## Issues

### Issue 1 — Severity: suggestion (resolved during review)
- **File**: `scripts/erlang-harvest-review-patterns.py:878`
- **Description**: Originally rendered a "Or on Windows: scripts\\erlang-harvest-review-patterns.bat --apply" block in the auto-generated candidate report. After this PR deletes the `.bat`, the line would dangle and point readers at a non-existent file.
- **Suggestion**: Remove the Windows-specific block; the `python3 scripts/...` invocation works on both Linux and Windows (Python is the cross-platform entry point by design).
- **Status**: **resolved** in this PR (4 lines + the "Or on Windows:" header removed; comment text unchanged).
- **Pattern-id**: docs.dangling-reference-after-deletion

### Issue 2 — Severity: suggestion (now resolved)
- **File**: `scripts/README.md:11`
- **Description**: Line 11 listed `release-audit/erlang-harvest-review-patterns.{sh,bat}` as out-of-scope-for-Phase-3 (preserved-until-US3 note). Two latent defects: (a) wrong directory prefix — the wrappers live at `scripts/`, not `scripts/release-audit/`; (b) the line is now obsolete — the wrappers are being deleted in this PR, so a "do NOT touch" reminder is meaningless.
- **Suggestion**: Delete the line; the remaining "Out of scope" section still covers `mvn-env.{sh,bat}` and runtime scripts.
- **Status**: **resolved** in this PR.
- **Pattern-id**: docs.obsolete-scope-note

### Issue 3 — Severity: nit (now resolved)
- **File**: `scripts/README.md:72`
- **Description**: Heading `### erlang-harvest-review-patterns.py (US3, not converted in US2)` — the parenthetical was a US2-era status note that becomes misleading the moment US3 lands.
- **Suggestion**: Drop the parenthetical.
- **Status**: **resolved** in this PR.
- **Pattern-id**: docs.temporary-status-leftover

### Issue 4 — Severity: nit (recorded; not blocking)
- **File**: `modules/ai-shared-develop/src/main/resources/skills/erlang-review/SKILL.md`, `.../agents/erlang-code-review.md`, `.../skills/erlang-review/patterns.md`, root `AGENTS.md`, `modules/ai-shared-develop/README.md`
- **Description**: These files mention `scripts\erlang-harvest-review-patterns.bat --apply` (or `erlang-harvest-review-patterns.bat` on Unix) in their "How to refresh institutional memory" sections. After this PR, those references point at a deleted file.
- **Suggestion**: Out of scope for US3 (these files live in `modules/ai-shared-develop/` and `AGENTS.md`, which are Phase 4 / Phase 7 territory per `tasks.md` T050 / T080). Phase 4 (US4 PR #TBD) or Phase 7 polish should update them. Flagged here so the implementer knows to handle it before the SC-006 "zero surviving doc refs" final sweep.
- **Status**: open (out of scope for US3; tracked in Phase 4 / Phase 7)
- **Pattern-id**: docs.dangling-reference-after-deletion

## Behavioral tests added

| Test | Asserts |
|------|---------|
| `test_report_does_not_reference_removed_wrappers` | After running `harvest.main([...])` end-to-end on an empty fixture, the rendered candidate report under `out.md` does **not** contain the substrings `erlang-harvest-review-patterns.bat` or `erlang-harvest-review-patterns.sh`. Pins the dangling-reference fix from Issue 1. Uses `tempfile.TemporaryDirectory()` + `os.chdir(td_path)` so the test runs identically on Linux and Windows (mirrors the existing `test_main_fixture_write_report` pattern at `scripts/test_erlang_harvest_review_patterns.py:263`). |

Full `scripts/` pytest: **106 passed in 20.27s** on Linux (10 existing
erlang-harvest tests + 1 new + 95 from US2 release).

## Re-review trigger

If any of the Phase 4 / Phase 7 documentation updates listed under Issue 4
materialize in a separate PR, re-run Erlang on that diff to confirm the
deletion cleanup is complete.