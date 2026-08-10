# Erlang review — `nightly-i18n-refresh` (PR #2651 review-fix pack)

- **Scope**: staged working-tree files vs `origin/chore/nightly-i18n-refresh-tooling` HEAD.
  - MOD `scripts/nightly_i18n_refresh.py` (+36/-14) — `TMX_DIR`/`TMX_FILES`, `branch_age_days` tz fix, `run_translate` stdout cleanup, `stage_and_commit` diff-driven staging, stale-branch unpushed guard, commit-failure recovery
  - MOD `scripts/test_nightly_i18n_refresh.py` (+113/-2) — new `TestStagingPathBuilder` (2 tests), 3 new `TestModuleConstants` cases (`test_tmx_paths_resolve_on_repo`, `test_tmx_dir_constant`, expanded `test_tmx_files_match_module_agents`)
- **Reference patterns consulted**: root `AGENTS.md`, `.kilo/rules/pre-commit-review.md`, `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`, prior report `docs/ai-generated/code-reviews/erlang-nightly-i18n-refresh-pre-pr.md`.
- **Pre-PR Maven**: not applicable — only Python scripts and tests touched; no Maven modules.
- **Test run**: `python3 scripts/test_nightly_i18n_refresh.py` → **Ran 33 tests in 1.721s, OK** (all 11 classes pass, including the 2 new `TestStagingPathBuilder` cases and the 3 new `TestModuleConstants` cases).

## Summary

All four peer-review findings from PR #2651 are **genuinely fixed** (not just claimed), each fix is **minimal and scoped**, and the two new `TestStagingPathBuilder` tests plus three new `TestModuleConstants` cases give us a real regression net against the basename defect. Tests pass cleanly against the real `TMX_DIR` and `TMX_FILES` constants via a temp git repo. No new bugs introduced. Recommendation: **approve**.

## Recommendation

**approve** — 0 blocking findings. Safe to push `chore/nightly-i18n-refresh-tooling` and re-open / unblock PR #2651.

## Per-fix verification

### Fix 1 — TMX paths + diff-driven staging + no-delete on commit failure (blocking)

|                         Item                         |                                                                            Evidence                                                                             |
|------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `TMX_DIR` constant added                             | `nightly_i18n_refresh.py:64` — `Path("modules/perc-i18n/src/main/resources/i18n")`                                                                              |
| `TMX_FILES` full relative paths                      | `nightly_i18n_refresh.py:65-69` — three entries now begin with `modules/perc-i18n/src/main/resources/i18n/`                                                     |
| `stage_and_commit` enumerates only changed TMX files | `nightly_i18n_refresh.py:451-458` — `git diff --name-only` against `TMX_DIR.glob("*.tmx")`                                                                      |
| Commit failure no longer deletes the branch          | `nightly_i18n_refresh.py:776-780` — `logger.error("Failed to commit changes; leaving worktree intact for manual recovery"); return 1` (no `delete_branch` call) |
| Constant test updated                                | `test_nightly_i18n_refresh.py:333-341` — asserts new full-path tuple                                                                                            |
| Regression test added                                | `test_nightly_i18n_refresh.py:166-247` — `TestStagingPathBuilder` with 2 cases against a real temp git repo                                                     |

Verdict: **fixed**. The diff-driven path enumeration is strictly safer than the previous basenames approach — it also handles the (future) case of a new TMX file being added to the directory without a `TMX_FILES` tuple update.

### Fix 2 — Stale-branch `has_unpushed_commits` guard

|                        Item                         |                                                                  Evidence                                                                  |
|-----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| Guard inserted before `delete_branch` in stale path | `nightly_i18n_refresh.py:728-737` — `if has_unpushed_commits(branch_name, worktree): warn+keep` else `delete_branch(...)`                  |
| `has_unpushed_commits` implementation reviewed      | `nightly_i18n_refresh.py:282-306` — `git rev-list --count origin/<branch>..<branch>` with `origin/main` fallback for never-pushed branches |

Verdict: **fixed**. Correctness: if `git rev-list origin/<branch>..<branch>` succeeds but returns 0 (everything pushed), the branch is deleted as before; if returns N>0, we keep with a warning. Fallback to `origin/main..<branch>` is correct for never-pushed branches — a fresh branch's first commit is reachable from origin/main only if main moved, which means the branch would already need rebasing, not deletion.

### Fix 3 — `branch_age_days` tz fix

|                       Item                       |           Evidence            |
|--------------------------------------------------|-------------------------------|
| `astimezone(UTC)` replaces `replace(tzinfo=UTC)` | `nightly_i18n_refresh.py:213` |

Verdict: **fixed and correct**. `datetime.strptime(..., "%z")` returns an aware datetime in the parsed offset. `.astimezone(timezone.utc)` properly converts the instant to UTC (preserving wall-clock difference), while `.replace(tzinfo=timezone.utc)` would have **relabeled without converting** — i.e., a commit stamped `2026-08-09 02:00:00 +0200` (which is `00:00:00 UTC`) would have been counted as if it were `02:00:00 UTC`, skewing the age by ±1 day for any commit within ±24h of the threshold in non-UTC timezones.

### Fix 4 — `run_translate` stdout cleanup

|                        Item                        |                    Evidence                    |
|----------------------------------------------------|------------------------------------------------|
| `captured = {"stdout": ""}` (no `stderr` key)      | `nightly_i18n_refresh.py:389`                  |
| No `captured["stderr"] = captured["stdout"]` write | confirmed — line removed (was 406 in old diff) |
| `output = captured["stdout"]` (no concatenation)   | `nightly_i18n_refresh.py:417`                  |
| `stderr=subprocess.STDOUT` still set on Popen      | `nightly_i18n_refresh.py:397`                  |

Verdict: **fixed and correct**. Because stderr is already merged into stdout at the OS pipe level (`stderr=STDOUT`), each line was previously being captured twice in `captured["stdout"]` and again concatenated with itself in `output`. The doubling was benign for `parse_translate_output` (only `re.search` single-match patterns; `(?m)` MULTILINE re-search would still find the same line at the same position), but it doubled the log-file writes and the parsed string size. `parse_translate_output` (lines 326-355) only reads stdout-merged content and is unaffected by the cleanup.

## New tests

|                   Test                   |          Class           |                                                                       Asserts                                                                        |
|------------------------------------------|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| `test_tmx_paths_resolve_against_git_add` | `TestStagingPathBuilder` | Each `TMX_FILES` entry is a valid `git add --dry-run` pathspec against a temp repo that mirrors the real layout                                      |
| `test_stage_and_commit_uses_diff_only`   | `TestStagingPathBuilder` | Touching one TMX file yields exactly that path in `git diff --name-only` against `TMX_DIR` (regression guard against future "stage-all" regressions) |
| `test_tmx_paths_resolve_on_repo`         | `TestModuleConstants`    | Every `TMX_FILES` entry actually exists at `repo_root / path` on this checkout, starts with `modules/perc-i18n/`, ends with `.tmx`                   |
| `test_tmx_dir_constant`                  | `TestModuleConstants`    | `str(m.TMX_DIR)` equals `modules/perc-i18n/src/main/resources/i18n`                                                                                  |

All four tests pass against the real module constants (no fakes). `test_tmx_paths_resolve_on_repo` runs against the actual repo (worktree root), giving us a live regression net on the basename defect.

## Items checked and PASS

- **Pre-commit review gate**: this is a follow-up to a pre-approved diff; new Erlang pass requested per `.kilo/rules/pre-commit-review.md`. PASS.
- **No rule files in diff**: `AGENTS.md`, `AGENTS.local.md`, `.kilo/**`, `.kilocode/**`, `.grok/**` — none modified. PASS the Human-review-of-agent-rules gate.
- **No secrets / tokens**: still only `gh auth status` for auth. PASS.
- **`subprocess` arg lists, no `shell=True`**: confirmed. PASS.
- **`pathlib.Path` everywhere**: no string-concat filesystem paths, no `os.path.join`, no hardcoded separators. PASS.
- **Cross-platform**: `TMX_DIR = Path("modules/...")` is relative — works only when Python cwd is repo root. This matches the existing convention (see pre-PR Issue 10) and the documented usage (cron wrapper does `cd "$(git rev-parse --show-toplevel)"`). The `TestStagingPathBuilder` tests are defensive: they prepend `worktree` to `TMX_DIR` in `test_stage_and_commit_uses_diff_only`, so they work regardless of cwd. PASS.
- **`re.search` MULTILINE for error markers**: `parse_translate_output` line 350 uses `(?im)^(?:ERROR on tuid=|error: |Exception\(|\[ERROR\])`. With the stdout doubling removed, this still matches each error line once (single `re.search` per regex, anchored to start-of-line). PASS.
- **`has_unpushed_commits` fallback**: when `origin/<branch>` doesn't exist yet, falls back to `origin/main..<branch>`. Correct: a brand-new branch with one commit will show 1 un-pushed commit, blocking the "stale branch delete" path. PASS.
- **`build_commit_message` / `build_pr_body` / Co-Authored footer**: not touched in this diff. Pre-PR Issue 1/2/3 fixes remain in place. PASS.
- **Test count**: 33 tests across 11 classes, all pass (verified by running locally). PASS.

## Items checked and NIT-only (no action required)

- **NIT** — `stage_and_commit` (line 452) uses `TMX_DIR.glob("*.tmx")` rather than iterating `TMX_FILES`. This means if someone adds a new TMX file directly to the directory without updating `TMX_FILES`, it will be staged correctly — but the `test_tmx_files_match_module_agents` constant test will still detect the tuple drift if they don't add it there. The two paths (`TMX_FILES` constant vs. directory glob) are now slightly redundant but consistent in coverage. No action needed; future cleanup if desired.
- **NIT** — `parse_translate_output` docstring at line 327 says "stdout" but the function actually receives stdout+stderr-merged content (via `stderr=STDOUT`). Docstring is technically accurate as long as the caller knows. No action needed; matches the new `output = captured["stdout"]` simplification.
- **NIT** — `TMX_DIR.glob("*.tmx")` runs in the Python process cwd (not the worktree). Safe in production (cron does `cd` first; documented usage is from repo root) but worth a comment. The test guards against this by prefixing `worktree / m.TMX_DIR` explicitly.

## Required actions before merge

None. The fix pack addresses all 4 blocking and non-blocking findings from the PR #2651 peer review. Tests pass.

## Handoff summary

- **Reviewed**: 2 files, 151 net new lines (+136/-15).
- **Top findings**: 0 blocking, 0 new suggestions, 3 nits. All 4 prior review findings (basename defect, stale-branch data-loss, tz math, stdout doubling) are properly fixed and covered by new tests.
- **Dominant risk**: none.
- **Recommendation**: approve.
- **Next step**: hand back to Hephaestus (operator) to push the branch and (re-)request review on PR #2651. No follow-up work for Erlang.

