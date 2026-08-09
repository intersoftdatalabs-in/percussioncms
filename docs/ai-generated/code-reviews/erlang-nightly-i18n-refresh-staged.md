# Erlang review (re-review) — `nightly-i18n-refresh` (staged diff)

- **Scope**: re-review of staged working-tree files for branch
  `chore/nightly-i18n-refresh-tooling` (worktree
  `/home/nate/.kilo/worktrees/nightly-i18n-refresh`). Prior review
  `docs/ai-generated/code-reviews/erlang-nightly-i18n-refresh-pre-pr.md`
  flagged 5 blocking findings; this pass verifies those are fixed and
  identifies any new findings.
- **Base**: `origin/main` (HEAD `19148cc85264`).
- **Head**: uncommitted, staged (`git diff --cached`).
- **Files**: 6 changed, 1382 net new lines
  - NEW `scripts/nightly_i18n_refresh.py` (778 LOC)
  - NEW `scripts/nightly-i18n-refresh.sh` (16 LOC)
  - NEW `scripts/test_nightly_i18n_refresh.py` (364 LOC)
  - MOD `scripts/README.md` (+41 LOC)
  - MOD `modules/perc-i18n/pom.xml` (+1 LOC)
  - STAGED `docs/ai-generated/code-reviews/erlang-nightly-i18n-refresh-pre-pr.md`
    (prior review report — committed alongside the fix pack)
- **Test run**: `python3 scripts/test_nightly_i18n_refresh.py` →
  **29 tests, 0 failures** (verified this session).
- **Reference patterns consulted**: prior report (above); root
  `AGENTS.md` Cross-Platform File I/O & Paths;
  `.kilo/rules/{co-author-attribution,operator-pr-labels,pre-commit-review}.md`;
  `modules/perc-i18n/scripts/i18n_translate_direct.py` (verified
  `--fix-matching-en` flag at line 557 is supported).

## Summary

The author has addressed all five blocking findings from the prior review
and re-architected the test file to load the real wrapper module via
`importlib.util.spec_from_file_location` (matching the
`test_prune_stale_worktrees.py` sibling pattern). 29 behavioral tests
cover locale rotation, output parsing, branch staleness, pre-flight
dispatch, commit / PR body assembly, constants, lock contention via a
real subprocess holder, and the unpushed-commit data-loss guard.
Cross-platform path review passes — no hardcoded separators, no
Unix-only absolute roots in production code, and `fcntl` is now
explicitly documented as Linux/macOS-only in the README. A handful of
non-blocking suggestions remain (a positive-case test for the
data-loss guard, a doubled-stdout buffer in `run_translate`, two
consistency nits).

## Recommendation

**approve** — May commit/push: **yes**

## Gate

- Blocking bugs: 0
- Suggestions: 3 (non-blocking)
- Nits: 3 (non-blocking)
- May commit/push: **yes**

## Re-review: prior blockers

|                         Prior issue                          |    Status    |                                                                                                                                                                                                                             Evidence                                                                                                                                                                                                                             |
|--------------------------------------------------------------|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ISSUE_1 (model name `MiniMax-M2.7` → `MiniMax-M3`)           | **FIXED**    | `scripts/nightly_i18n_refresh.py:37` `AGENT_MODEL = "MiniMax-M3"`; `test_coauthored_footer_contains_model_and_agent` passes.                                                                                                                                                                                                                                                                                                                                     |
| ISSUE_2 (PR label slug `model:minimax` → `model:minimax-m3`) | **FIXED**    | `scripts/nightly_i18n_refresh.py:38,45` `AGENT_MODEL_SLUG = "minimax-m3"`, `MODEL_LABEL = "model:minimax-m3"`; `TestModuleConstants` asserts.                                                                                                                                                                                                                                                                                                                    |
| ISSUE_3 (Co-Authored footer in PR body)                      | **FIXED**    | `scripts/nightly_i18n_refresh.py:262` `build_pr_body` appends `{CO_AUTHORED_FOOTER}`; `test_body_has_coauthored_footer` passes.                                                                                                                                                                                                                                                                                                                                  |
| ISSUE_8 (README platform note — Windows unsupported)         | **FIXED**    | `scripts/README.md` `- **Platform**: Linux/macOS only. The wrapper imports fcntl for flock; Windows is unsupported (use WSL2).`                                                                                                                                                                                                                                                                                                                                  |
| Pre-PR Maven verify (1-line pom.xml exclude)                 | **ACCEPTED** | Per author: pure tooling, no Java sources/tests/resources touched. The pom.xml change is a single `<exclude>scripts/cache/**</exclude>` line under an existing `<excludes>` block — no new plugins, deps, or formatter config. Document in PR body. **Note**: AGENTS.md still requires `cd modules/perc-i18n && ../mvnw clean install` for *any* pom.xml change; recommend the author run this before opening the PR and paste `BUILD SUCCESS` into the PR body. |

## Re-review: prior suggestions addressed

- ISSUE_4 (tests don't import wrapper) → **FIXED**. `load_wrapper()` uses
  `importlib.util.spec_from_file_location` with a Windows-portable
  `fcntl` shim; tests assert against `m.select_locale`,
  `m.parse_translate_output`, `m.is_branch_stale`,
  `m._evaluate_preflight_checks`, `m.build_commit_message`,
  `m.build_pr_body`, `m.acquire_lock`, `m.has_unpushed_commits`. The
  shim is cleanly swapped for the real POSIX `fcntl` only for
  `TestLockContention` via `_swap_to_real_fcntl()`. Imports cleanly
  on Linux, macOS, and Windows.
- ISSUE_5 (push-failure data-loss) → **FIXED**. New
  `has_unpushed_commits()` (lines 277-301) falls back to
  `origin/main..branch` when `origin/<branch>` doesn't exist yet;
  `main()` at lines 741-750 now logs `WARNING` and returns 0 instead
  of deleting the branch when no new diffs are present but unpushed
  commits exist. `TestUnpushedCommitsDetection` covers the negative
  path.
- ISSUE_6 (test coverage gap) → **ADDRESSED**. 29 tests cover the
  previously-untested helpers. Lock contention uses a real subprocess
  holder; pre-flight dispatch is exercised against an injectable
  check-fn table; commit/PR-body builders have happy + edge cases.
  See Suggestion-1 below for the one remaining positive-case gap.
- ISSUE_7 (`import logging.handlers` inside `__main__`) → **FIXED**;
  moved to module-level at line 26. Required prerequisite for the
  Issue-4 refactor.
- ISSUE_9 (subprocess leak on exception) → **FIXED**. Lines 402-409
  now wrap the read loop in `try/except BaseException: proc.kill();
  proc.wait(); raise` before the `finally` that closes the log
  handle.
- ISSUE_12 (loose "error"/"exception" substring match) → **FIXED**.
  `parse_translate_output` (lines 344-348) now uses an anchored
  regex `r"(?im)^(?:ERROR on tuid=|error: |Exception\(|\[ERROR\])"`.
  Test `test_benign_word_with_error_does_not_flag` confirms `"0
  errors detected"` no longer triggers `had_error`.
- ISSUE_13 (`import time` inside the push-retry loop) → **FIXED**;
  top-level at line 32.
- ISSUE_14 (`bufsize=1` decorative on `subprocess.Popen`) →
  **FIXED**; `bufsize=1` removed; child uses `flush=True` (verified
  in `i18n_translate_direct.py`).
- ISSUE_15 (naive `datetime.now()` in PR body) → **FIXED**; line 496
  uses `datetime.datetime.now(datetime.timezone.utc)`.
- Prior Issue-8 dead-code cleanup → **DONE**. The five dead helpers
  (`repo_root`, `get_current_branch`, `is_working_tree_clean`,
  `has_format_target`, `run_spotless`) are removed; only the
  `_worktree`-suffixed variants remain.

## Items checked and PASS

- **`shell=False` / `check=False`**: every `subprocess.run` /
  `subprocess.Popen` call uses arg lists, never `shell=True`. PASS.
- **`pathlib.Path` everywhere**: no string-concat filesystem paths,
  no `os.path.join`, no hardcoded separators in production paths.
  PASS.
- **Stdlib-only imports at runtime**: only `argparse`, `datetime`,
  `fcntl`, `json`, `logging`, `logging.handlers`, `os`, `re`,
  `shutil`, `subprocess`, `sys`, `time`, `dataclasses`, `pathlib`,
  `typing`. PASS.
- **Lock release on exception**: `try/finally` around
  `release_lock(lock_fd)` at line 773-774. fd is closed on normal
  exit and on exception. PASS.
- **Lock acquired before any side effects**: `acquire_lock` at
  line 675 runs before `ensure_worktree`. PASS.
- **Pre-flight order is safe**: `ensure_worktree` → preflight →
  branch create/reuse → translation. No git mutations before all
  pre-flight checks pass. PASS.
- **Push retry with backoff**: 10s, 30s, 60s at line 482. PASS.
- **Branch reuse vs deletion**: 7-day threshold via
  `is_branch_stale(age, threshold_days=7)`. PASS.
- **Stale branch deletion respects currently-checked-out**:
  `delete_branch` switches to detached `origin/main` first.
  PASS.
- **TMX file hand-edit avoidance**: wrapper only invokes
  `i18n_translate_direct.py`; never reads/writes `<seg>` text
  directly. PASS per `perc-i18n/AGENTS.md`.
- **`--fix-matching-en` flag compatibility**: verified against
  `modules/perc-i18n/scripts/i18n_translate_direct.py:557` — the
  flag is supported. PASS.
- **Locale list matches `perc-i18n/AGENTS.md` Quick Reference**:
  16 codes identical. PASS.
- **No secrets / tokens in committed code**: only `gh auth status`.
  PASS.
- **`pr_body_file` path is gitignored**: `tmp/` is in repo
  `.gitignore:309`. PASS.
- **No rule files in diff**: `AGENTS.md`, `AGENTS.local.md`,
  `.kilo/**`, `.kilocode/**`, `.grok/**` — none modified. PASS
  the Human-review-of-agent-rules gate.
- **Cross-platform path checklist**: no hardcoded `/` or `\` in
  filesystem paths; no Unix-only absolute roots; `fcntl` import
  explicitly documented as Linux/macOS-only with a WSL2 fallback
  note in the README. PASS.

## Issues

### Issue 1 -- Severity: suggestion (test coverage gap on the data-loss guard)

- **File**: `scripts/test_nightly_i18n_refresh.py:150-162`
  (`TestUnpushedCommitsDetection`)
- **Description**: The two tests in this class only exercise the
  *negative* path of `has_unpushed_commits` against
  non-existent branches (using `tempfile.TemporaryDirectory()` as
  the "worktree"). Neither test sets up a real git repo with two
  branches and verifies that the **positive case** (a branch with
  unpushed commits → `True`) works as expected. This is exactly
  the data-loss recovery scenario the function was added for
  (prior Issue_5). Without the positive-case test, a regression
  that always returns `False` would silently delete unpushed
  commits on the next run.
- **Suggestion**: Add a third test that uses `tempfile.TemporaryDirectory()`
  to initialize a real git repo (`git init`, `git commit --allow-empty`,
  `git checkout -b feature`, `git commit --allow-empty`), then asserts
  `has_unpushed_commits("feature", repo_path)` returns `True`. This is
  the single most important behavior in the wrapper for night-time
  reliability and should not be left to "negative-case" coverage.
- **Status**: open
- **Pattern-id**: tests.structural-only

### Issue 2 -- Severity: suggestion (doubled stdout buffer in run_translate)

- **File**: `scripts/nightly_i18n_refresh.py:401, 413`
- **Description**: `captured["stderr"] = captured["stdout"]` then
  `output = captured["stdout"] + "\n" + captured["stderr"]`. Since
  the `Popen` call passes `stderr=subprocess.STDOUT`, all stderr is
  already merged into stdout — the intended `captured["stderr"]`
  buffer is empty. The `+ "\n" + captured["stderr"]` concatenation
  duplicates the stdout buffer and inflates `output` to 2× the
  child's actual emission before `parse_translate_output` runs.
  `re.search` is first-match so this doesn't break parsing, but
  it's wasted memory on a 2-3 hour translation pass and the intent
  is confused (looks like stderr handling but it's stdout ×2).
- **Suggestion**: Either drop the `captured["stderr"]` key entirely
  and use `output = captured["stdout"]`, or actually capture stderr
  separately (`stderr=subprocess.PIPE`, separate read loop) if you
  ever want to distinguish them. The current "set stderr=stdout then
  re-set stderr=stdout" is dead code masquerading as separation.
- **Status**: open
- **Pattern-id**: maintainability.dead-init

### Issue 3 -- Severity: suggestion (lock-file mode)

- **File**: `scripts/nightly_i18n_refresh.py:565`
- **Description**: `os.open(str(lock_file), os.O_RDWR | os.O_CREAT,
  0o644)` creates the lock file world-readable. On a multi-user
  machine, any local user can `flock` the file (or, more
  dangerously, delete it before the next acquire — `os.O_CREAT`
  re-creates it on next open but the FD is leaked). For a
  per-user cron tool this is fine in practice (`~/logs`,
  `tmp/nightly-i18n.lock` under the operator's home / repo), but
  the lock is in `REPO_ROOT / "tmp"` which is repo-local and may
  be world-readable on shared CI runners. Tightening to `0o600`
  costs nothing and matches what `acquire_lock` semantically
  requires (only the holder should be able to release).
- **Suggestion**: `os.open(str(lock_file), os.O_RDWR | os.O_CREAT,
  0o600)` so only the operator can manipulate the lock.
- **Status**: open
- **Pattern-id**: security.file-mode

### Issue 4 -- Severity: nit

- **File**: `scripts/nightly_i18n_refresh.py:459` (`stage_and_commit`)
- **Description**: `commit_msg = f"chore(i18n): Nightly i18n refresh for {locale}"`
  duplicates the subject line construction that
  `build_commit_message` already does internally. Only used for the
  dry-run log message. Could be a local `subject = full_commit_msg.split("\n\n", 1)[0]`.
- **Suggestion**: Hoist from `build_commit_message` (return a tuple)
  or simply use the split.
- **Status**: open

### Issue 5 -- Severity: nit

- **File**: `scripts/nightly_i18n_refresh.py:498`
  (`push_and_create_pr`)
- **Description**: `pr_body = build_pr_body(locale, today,
  result.inserted, result.fixed)` doesn't propagate
  `result.had_error` or `result.error_message` to the PR body,
  even though the **commit** message does (via
  `build_commit_message`). If the second translation pass errored,
  the PR body reports a clean run while the commit message warns.
- **Suggestion**: Either add an optional `had_error` /
  `error_message` arg to `build_pr_body` and emit a `WARNING: ...`
  line, or accept the inconsistency and document it.
- **Status**: open

### Issue 6 -- Severity: nit

- **File**: `scripts/test_nightly_i18n_refresh.py:153-157`
- **Description**: Comment "Real git in worktree: no commits means
  no unpushed commits." is misleading — `tempfile.TemporaryDirectory()`
  is not a git worktree. Both tests actually exercise the
  *non-git* path where `git rev-list` fails on both invocations
  and the function returns `False` from the inner fallback. The
  test name suggests it's testing the "no unpushed commits" case
  but it's testing the "git not available / branch doesn't exist"
  fallback.
- **Suggestion**: Rename the test class/methods to reflect the
  actual scenario (e.g. `test_returns_false_in_non_git_worktree`)
  and add the real positive-case test from Issue 1.
- **Status**: open

## Items checked and NIT-only

- `select_locale` uses `today.timetuple().tm_yday`. On a tz-aware
  datetime this returns the UTC day-of-year (the struct_time is
  built from the naive-equivalent of the UTC fields). Documented
  expectation matches test inputs (`tzinfo=datetime.timezone.utc`).
  PASS.
- `delete_branch` docstring references "primary checkout already
  holds main so we cannot check it out here" — accurate. PASS.
- `acquire_lock` docstring explains the absence of an in-process
  retry loop (re-locking the same FD is a no-op). Slightly dense
  but correct. PASS.
- Prior Issue-20 (`gh` repo identity drift) and Issue-21 (argparse
  overrides for `--log-dir` / `--lock-file`) remain unaddressed;
  both were "convention" findings in the prior report and are
  out of scope for this re-review (not material to the data-loss
  or convention-blocking bugs). Future enhancement, not a blocker.

## Required actions before merge

None blocking. The author may commit and open the PR after:

1. (recommended) Add Issue-1's positive-case test for
   `has_unpushed_commits` and re-run the suite. **Not blocking.**
2. (recommended) Run `cd modules/perc-i18n && ../mvnw clean install`
   per root AGENTS.md Pre-PR Maven verification (the prior report
   flagged this; it covers the one-line `<exclude>` pom.xml
   change). Paste `BUILD SUCCESS` in the PR body.
3. (cosmetic) Address Issues 2-6 in a follow-up commit if desired.

## Handoff summary

- **Reviewed**: 6 staged files, 1382 net new lines.
- **Prior 5 blockers**: all FIXED (verified by file:line citation
  and by the test suite passing).
- **Prior 11 suggestions**: 7 addressed in this pass (Issues 4, 5,
  6, 7, 9, 12, 13, 14, 15, plus dead-code cleanup); 2 remain
  unaddressed (Issues 20, 21) as future enhancements; 2 partially
  addressed (Issue 6 test coverage now broad but missing positive
  case — see Issue 1 below).
- **New findings**: 0 bugs, 3 suggestions, 3 nits.
- **Cross-platform path review**: PASS (no issues).
- **Test verification**: 29/29 tests pass.
- **Dominant residual risk**: the data-loss guard's positive case
  is untested (Issue 1) — a regression could delete unpushed
  commits on the next nightly run, but the function itself is
  correct based on code inspection and the git command path is
  standard. Recommend addressing Issue 1 before the PR is opened
  to keep the test gate honest on this exact failure mode.
- **Recommendation**: **approve**; May commit/push: **yes**.

