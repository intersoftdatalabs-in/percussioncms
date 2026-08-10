# Erlang review — `nightly-i18n-refresh` (nightly cron wrapper)

- **Scope**: uncommitted working-tree files vs `origin/main` HEAD.
  - NEW `scripts/nightly_i18n_refresh.py` (677 lines)
  - NEW `scripts/nightly-i18n-refresh.sh` (16 lines)
  - NEW `scripts/test_nightly_i18n_refresh.py` (285 lines)
  - MOD `scripts/README.md` (+40-line section)
  - MOD `modules/perc-i18n/pom.xml` (+1 `<exclude>scripts/cache/**</exclude>`)
- **Reference patterns consulted**: `scripts/prune-stale-worktrees.py`, `scripts/test_prune_stale_worktrees.py`, `modules/perc-i18n/scripts/i18n_translate_direct.py`, root `AGENTS.md`, `modules/perc-i18n/AGENTS.md`, `.kilo/rules/{co-author-attribution,operator-pr-labels,pre-commit-review}.md`.

## Summary

Solid concept (lock, dedicated worktree, two-pass translate, dry-run, retry). **Five blocking findings** — three of them are simple convention violations (model name in Co-Authored footer, PR label slug, missing footer in PR body) and two are real defects (tests never import the wrapper; push-failure recovery silently discards the only copy of the committed TUVs on the next run). Recommend **request-changes**; re-review after the five blockers are fixed.

## Recommendation

**request-changes** (May commit/push: **no**) — fix the five blocking findings (footer model, PR label slug, PR-body footer, test drift, push-recovery data-loss), then re-run Erlang.

## Issues

### Issue 1 — Severity: bug

- **File**: `scripts/nightly_i18n_refresh.py:384`; mirrored in `scripts/test_nightly_i18n_refresh.py:96`
- **Description**: Co-Authored footer hardcodes the wrong model version: `> Co-Authored by Kilo using MiniMax-M2.7 with agent kilo.`. System prompt + `.kilo/rules/co-author-attribution.md` + `corrections.md` (memory) all require **`MiniMax-M3`**.
- **Suggestion**: Replace `MiniMax-M2.7` with `MiniMax-M3` in both files (footer string + the assertion in `test_commit_has_coauthor_footer`). Treat the model id as a single module-level constant in the wrapper so a future model swap is one edit, not two.

### Issue 2 — Severity: bug

- **File**: `scripts/nightly_i18n_refresh.py:447-448`
- **Description**: PR labels use `model:minimax`. `.kilo/rules/operator-pr-labels.md` requires the **stable lowercase slug** matching the session model id; the spec example is `model:claude-sonnet-4.5` (model **and** sub-version). For this session the correct label is `model:minimax-m3`.
- **Suggestion**: Replace with `model:minimax-m3`. Also: pull both labels (`operator:kilo`, `model:minimax-m3`) into a module-level constant tuple so they cannot drift from the README's documented labels.

### Issue 3 — Severity: bug

- **File**: `scripts/nightly_i18n_refresh.py:426-433` (`pr_body`)
- **Description**: `gh pr create` is one of the GitHub interactions explicitly covered by `.kilo/rules/co-author-attribution.md` and `corrections.md` (`github_coauthor_footer_required`, `agents_local_md_footer_all_github`). The PR body this script writes contains **no** Co-Authored footer; commits do, but the PR body does not.
- **Suggestion**: Append a Co-Authored footer line to `pr_body` (mirroring the commit footer) and document that the rule applies to both surfaces. Reuse the same constant as Issue 1.

### Issue 4 — Severity: bug (test gap; root `AGENTS.md` non-trivial-logic gate)

- **File**: `scripts/test_nightly_i18n_refresh.py:17-105`
- **Description**: The test file does `sys.path.insert(0, str(REPO_ROOT / "scripts"))` (line 15) but **never imports from `nightly_i18n_refresh`**. It then **redefines** `BASE_LOCALES`, `select_locale`, `parse_translate_output`, `generate_pr_body`, `generate_commit_message`, `is_branch_stale` locally. Every assertion therefore runs against the test's own copy, not the wrapper's. The sibling pattern `scripts/test_prune_stale_worktrees.py:7-26` correctly uses `importlib.util.spec_from_file_location(...)` to load the real module and bind it to `m` for assertion (`m.parse_worktree_porcelain(...)`, `m.decide(...)`). Drift between `nightly_i18n_refresh.py` and its "tests" is invisible — e.g., Issue 1 above shipped to both copies identically, which is exactly the failure mode this gate exists to catch.
- **Suggestion**: Rewrite the test file to load the real wrapper via `importlib.util.spec_from_file_location("nightly_i18n_refresh", SCRIPT_DIR / "nightly_i18n_refresh.py")`. Guard with `sys.modules.setdefault("fcntl", types.ModuleType("fcntl"))` + `setattr(..., "flock", lambda *a, **k: None)` so the module imports on Windows / macOS without a real `fcntl` (the real wrapper is Linux-only; tests should still be cross-platform). Then assert against the real `m.select_locale`, `m.parse_translate_output`, `m.generate_pr_body`, `m.generate_commit_message`, `m.is_branch_stale` (extract the last one from `main()` into a module-level function — currently it's an inline branch on line 611-617 and untestable as-is).

### Issue 5 — Severity: bug (data-loss on push failure)

- **File**: `scripts/nightly_i18n_refresh.py:401-422` (push retry), `656-660` (caller leaves branch), `641-648` (next run deletes branch)
- **Description**: Push can transiently fail (rate limit, GitHub 5xx). The wrapper retries 3× with backoff; on final failure it returns False and the caller logs `Branch left in place for recovery` and exits 1. The committed TUVs now exist only on the local branch tip in the worktree. **Next nightly run** reuses the branch (it's < 7 days old, so the staleness branch at line 611 does NOT delete it). The translation pass produces no new TUVs (already cached) and `get_tmx_diff` / `get_cache_diff` are both false → line 647 calls `delete_branch` and exits. The un-pushed commits are reachable only via `git reflog` for ~30 days; the next cron after that GC silently throws them away. No warning, no log marker for "I just deleted commits that were never pushed."
- **Suggestion**: Either (a) track the last-pushed SHA on the branch metadata and refuse to delete the branch unless `git rev-parse origin/<branch>` matches that SHA — i.e., delete only after a successful push-or-rebase-pushed; or (b) at minimum, when `not has_tmx_changes and not has_cache_changes` AND `git log @{u}..HEAD` is non-empty, log `WARN: branch {name} has unpushed commits; keeping` and skip the delete. Add a test for this branch (currently zero coverage — see Issue 6).

### Issue 6 — Severity: suggestion (test coverage; root `AGENTS.md` non-trivial-logic gate)

- **File**: `scripts/test_nightly_i18n_refresh.py` (whole file)
- **Description**: Beyond Issue 4 (no real-module import), the suite covers only pure logic functions. **None** of the state-mutating helpers have behavioral tests:
  - `ensure_worktree` (worktree create / fetch / checkout / reset, 5 branches)
  - `acquire_lock` / `release_lock` (BLOCKING vs non-blocking, exception cleanup)
  - `run_preflight_checks` (4 checks, all-fail-exit)
  - `get_open_pr_for_branch` (JSON parse happy + sad path)
  - `branch_age_days` (timezone math, malformed input → None)
  - `delete_branch` (currently-checked-out branch protection, detached fallback)
  - `stage_and_commit` (commit message assembly, dry-run short-circuit)
  - `push_and_create_pr` (retry/backoff schedule, PR body file lifecycle)
  - `get_tmx_diff` / `get_cache_diff` (path with `/` vs OS sep)
- **Suggestion**: Add at minimum: (i) a test for the lock contention path using `tempfile` + a real `flock` from a thread or subprocess that holds the lock; (ii) a test for the "no diffs but unpushed commits" branch from Issue 5; (iii) tests for the 4 pre-flight checks using a fake `check_fn` table; (iv) tests for `parse_translate_output` with the exact `\nDone. Missing: ...; inserted: ...; fixed: ...; skipped: ...` format from `i18n_translate_direct.py:712` (already partially covered, but assert against the real module per Issue 4).

### Issue 7 — Severity: suggestion

- **File**: `scripts/nightly_i18n_refresh.py:675-677`
- **Description**: `import logging.handlers` is inside `if __name__ == "__main__":`. `setup_logging` references `logging.handlers.RotatingFileHandler` (line 75) which is not imported at module load. This means **the wrapper cannot be imported** by the test file (or any future caller) without `if __name__ == "__main__"` having executed. That's exactly what blocks the importlib refactor in Issue 4.
- **Suggestion**: Move `import logging.handlers` to the top-level imports (next to `import logging`).

### Issue 8 — Severity: suggestion

- **File**: `scripts/nightly_i18n_refresh.py:150-155, 158-161, 164-167, 327-342`
- **Description**: `repo_root`, `get_current_branch`, `is_working_tree_clean`, `has_format_target`, `run_spotless` are defined but **never called** anywhere in the module (verified by inspection — only `get_current_branch_worktree` and `is_working_tree_clean_worktree` are used). Dead code increases diff noise and confuses future maintainers.
- **Suggestion**: Delete the five dead helpers. Keep only the worktree-suffixed variants and any helper actually referenced.

### Issue 9 — Severity: suggestion

- **File**: `scripts/nightly_i18n_refresh.py:265-324`
- **Description**: `run_translate` uses `subprocess.Popen` and reads `proc.stdout` line-by-line. The `try/finally` at line 298-315 only protects `log_handle`; if reading or writing raises (disk full, log rotated mid-write), the child process is **leaked** — `proc.kill()` is never called, the pipe is never drained. For a 2-3 hour translation pass this matters.
- **Suggestion**: Wrap the loop in `try/except BaseException: proc.kill(); proc.wait(); raise` before the existing `finally`. Also: the `assert proc.stdout is not None` is purely a type-narrowing aid for mypy; harmless on CPython (Popen with `stdout=PIPE` always sets `.stdout`), but a comment helps.

### Issue 10 — Severity: suggestion

- **File**: `scripts/nightly_i18n_refresh.py:114, 152`
- **Description**: `ensure_worktree` and `repo_root` call `subprocess.run(["git", ...])` without explicit `cwd=`. They rely on the Python process cwd being inside the repo. Cron via `nightly-i18n-refresh.sh` does `cd "$(git rev-parse --show-toplevel)"` first (line 13), so this works in production. But the wrapper's documented usage also includes `python3 scripts/nightly_i18n_refresh.py` directly (README line 65), which fails silently if invoked from outside the repo.
- **Suggestion**: Either (a) pass `cwd=REPO_ROOT` explicitly to those `git` calls, or (b) document in the README that the wrapper must be invoked from the repo root or via the shell wrapper.

### Issue 11 — Severity: suggestion

- **File**: `scripts/nightly_i18n_refresh.py:112-147` (`ensure_worktree`)
- **Description**: If `worktree.exists()` is true but `git worktree list --porcelain` does not list the path, the function logs a warning and returns False (line 122-123). The wrapper then returns 1 from main, but the stale non-worktree directory is **left in place**. Operator has no recovery hint and re-running produces the same warning.
- **Suggestion**: On that path, log a concrete mitigation: `Worktree path {worktree} exists but is not a registered git worktree. Remove it manually (rm -rf {worktree}) and re-run, or pass --worktree to use a different path.`

### Issue 12 — Severity: suggestion

- **File**: `scripts/nightly_i18n_refresh.py:260`
- **Description**: `result.had_error = "error" in output.lower() or "exception" in output.lower()`. Substring match anywhere in (stdout + stderr). False positives: any future benign log line containing "error" / "exception" (e.g., "0 errors", "no exception thrown") flags the run as errored. Impact is bounded — `had_error` only adds a commit-message warning line — but it's a stringly-typed contract that drifts easily.
- **Suggestion**: Match against a tighter pattern anchored to actual error emissions of `i18n_translate_direct.py` (`r"ERROR on tuid="` from line 525, `r"^error: "` from line 582). Or, better: have `i18n_translate_direct.py` print a single line `Done: status=ok|error inserted=N fixed=N skipped=N` that the wrapper parses deterministically.

### Issue 13 — Severity: nit

- **File**: `scripts/nightly_i18n_refresh.py:418`
- **Description**: `import time` inside the push-retry `for` loop.
- **Suggestion**: Move to top-level imports.

### Issue 14 — Severity: nit

- **File**: `scripts/nightly_i18n_refresh.py:305`
- **Description**: `bufsize=1` on `subprocess.Popen` with `text=True` is misleading. `bufsize` on the parent side controls the parent's read buffering; the child's write buffering is what causes deadlocks, and that's controlled by the child (here: `i18n_translate_direct.py` already uses `flush=True`). The comment is correct but `bufsize=1` is decorative.
- **Suggestion**: Drop `bufsize=1` and document why deadlock is prevented by the child (every child `print` uses `flush=True`; final summary is flushed on exit).

### Issue 15 — Severity: nit

- **File**: `scripts/nightly_i18n_refresh.py:424, 74-75` (test)
- **Description**: PR body uses `datetime.datetime.now().strftime("%Y-%m-%d")` — naive local time. Locale rotation already uses UTC (line 235). Mismatch in the same module.
- **Suggestion**: `datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d")`. Same fix in the test's `generate_pr_body`.

### Issue 16 — Severity: nit

- **File**: `scripts/test_nightly_i18n_refresh.py:17-20`
- **Description**: `BASE_LOCALES` is re-declared as a local constant; should be imported from the wrapper once Issue 4 is fixed. Same for `TMX_FILES`, `LOG_FILE`, etc.
- **Suggestion**: After the importlib refactor, drop the local redeclarations.

### Issue 17 — Severity: nit

- **File**: `scripts/nightly_i18n_refresh.py:425-433` (PR body)
- **Description**: References `~/logs/nightly-i18n-refresh.log`. If a different operator (Vijay, future contributor) runs this on a machine where `LOG_DIR` is overridden, the path in the PR body won't match. Hardcoded `~`/`Path.home()` baked into a string is a small maintainability hit.
- **Suggestion**: `f"Auto-generated by `scripts/nightly_i18n_refresh.py`. See `{LOG_FILE}` for the full run log."` (with `LOG_FILE` already a `Path`).

### Issue 18 — Severity: cross-platform

- **File**: `scripts/nightly_i18n_refresh.py:23` (`import fcntl`)
- **Description**: `fcntl` is **POSIX-only**. On Windows the import itself raises `ModuleNotFoundError`, so the wrapper cannot run there. AGENTS.md mandates cross-platform for any product code; this is a developer-only cron tool so Windows is unlikely, but the import is unguarded.
- **Suggestion**: Add a top-level guard so the import failure produces a clear "Windows unsupported" error instead of a stack trace, OR move the lock to a stdlib-portable alternative (`msvcrt.locking` on Windows) — though for a Linux/WSL cron tool the simpler fix is a clear error message. If Linux-only is acceptable, document it in the README section (currently the README says nothing about platform support).

### Issue 19 — Severity: cross-platform (narrow)

- **File**: `scripts/nightly_i18n_refresh.py:297, 437`
- **Description**: `open(LOG_FILE, "a", encoding="utf-8")` and `pr_body_file.write_text(..., encoding="utf-8")` are both portable — good. `LOG_FILE` / `LOCK_FILE` / `DEFAULT_WORKTREE` all built via `pathlib.Path` — good. **No** hardcoded `/` or `\\` in filesystem paths anywhere in the wrapper or the shell script. PASS.

### Issue 20 — Severity: convention (drift vs sibling)

- **File**: `scripts/nightly_i18n_refresh.py:189-204`
- **Description**: `get_open_pr_for_branch` calls `gh pr list --head <branch> --state open --json number`. If the user has a `~/.config/gh/hosts.yml` pointing at a different default repo than the one this checkout's `origin` remote points at, this returns 0 hits and the wrapper silently proceeds to create a **second** PR for a branch that already has an open one in the real repo. Pre-flight `gh auth status` doesn't check repo identity.
- **Suggestion**: Parse `origin`'s owner/name from `git remote get-url origin` and pass `--repo <owner>/<name>` to every `gh` call, the way `prune-stale-worktrees.py` does (line 186-187, `--repo` arg). Or accept `--repo` on the CLI for cron flexibility.

### Issue 21 — Severity: convention (mild)

- **File**: `scripts/nightly_i18n_refresh.py:36-52`
- **Description**: Module-level constants include both repo-relative paths (`REPO_ROOT`, `I18N_MODULE`, `I18N_SCRIPTS_DIR`, `CACHE_FILE`, `LOCK_FILE`) and user-relative paths (`DEFAULT_WORKTREE`, `LOG_DIR`, `LOG_FILE`). The README says the lock is at `tmp/nightly-i18n.lock` (repo-relative) which matches. But the log file is at `~/logs/...`, which is fine for cron but **invisible** if someone runs the wrapper interactively from a non-default account. `scripts/prune-stale-worktrees.py` keeps paths in `argparse` defaults rather than module constants — easier to override.
- **Suggestion**: Make `--worktree`, `--log-dir`, `--lock-file` argparse overrides with the current defaults. At minimum, add `--log-dir`.

### Issue 22 — Severity: convention (cosmetic)

- **File**: `scripts/nightly_i18n_refresh.py:39-40`
- **Description**: `LOG_DIR = Path.home() / "logs"` creates `~/logs/nightly-i18n-refresh.log`. AGENTS.md has no rule against this, but the project convention is repo-relative `tmp/` for transient artifacts (lock file uses this). Logs typically want a stable operator location though, so this is defensible — just calling it out.
- **Suggestion**: No change needed; optional consideration for `~/.cache/intersoft/percussioncms/logs/` if the project later standardizes a per-tool XDG-style path.

## Items checked and PASS

- **`shell=False` / `check=False`**: all `subprocess.run` / `Popen` calls pass arg lists, no `shell=True`. PASS.
- **`pathlib.Path` everywhere**: no string-concat filesystem paths, no `os.path.join`, no hardcoded separators. PASS.
- **Stdlib-only imports at runtime**: only `argparse`, `datetime`, `fcntl`, `json`, `logging`, `os`, `re`, `shutil`, `subprocess`, `sys`, `dataclasses`, `pathlib`, `typing` — no third-party. PASS.
- **Lock release on exception**: `try/finally` around `release_lock(lock_fd)` in `main()` (line 671-672). fd is closed on normal exit and SIGTERM (OS closes fds on process death, releasing flock). PASS.
- **Lock acquired before any side effects**: `acquire_lock` at line 576 runs before `ensure_worktree` (which does `git worktree add`, `git fetch`, `git checkout`, `git reset --hard`). PASS.
- **Pre-flight order is safe**: `ensure_worktree` (idempotent setup) → `run_preflight_checks` (trans on PATH, tree clean, branch, gh auth) → branch create/reuse → translation. No side-effecting git operations before all pre-flight checks pass. PASS.
- **Push retry with backoff**: 10s, 30s, 60s (line 409). Module imports correctly. PASS (modulo Issue 13).
- **Branch reuse vs deletion**: 7-day threshold at line 611 with `age is None → delete`. PASS.
- **Stale branch deletion respects currently-checked-out**: `delete_branch` switches to detached `origin/main` first if needed (line 471-478). PASS.
- **TMX file hand-edit avoidance**: wrapper only calls `i18n_translate_direct.py`; never reads/writes `<seg>` text directly. PASS per `perc-i18n/AGENTS.md` "Per-key translations are owned by i18n_translate.py" rule.
- **Locale list matches `perc-i18n/AGENTS.md` Quick Reference** ("Base locales" `ISBASE=1`): 16 codes are identical to the module AGENTS.md list. PASS.
- **No secrets / tokens in committed code**: only `gh auth status` for auth. PASS.
- **AGENTS.local.md handling**: wrapper does not touch `AGENTS.local.md` — correct; that's a Kilo session-start rule, not a wrapper rule.
- **No rule files in diff**: `AGENTS.md`, `AGENTS.local.md`, `.kilo/**`, `.kilocode/**`, `.grok/**` — none modified. PASS the Human-review-of-agent-rules gate.
- **No new test fakes with wrong types**: there are no Spring/DI test fakes in this change.
- **Pre-PR Maven verification**: Only `modules/perc-i18n/pom.xml` changed; the spotless exclude is a single `<exclude>` line under an existing `<excludes>` block. No code, no tests, no resources, no dependencies. Per root AGENTS.md "Pre-PR Maven verification" the threshold is "every module whose sources, tests, resources, or `pom.xml` they changed" → one `mvnw clean install` on `modules/perc-i18n` is sufficient. Flag: ensure the human runs that and pastes `BUILD SUCCESS` in the PR body (the current default-branch HEAD is 18 commits behind `origin/main`; `git pull --ff-only` first).

## Items checked and NIT-only

- The shell wrapper `nightly-i18n-refresh.sh` correctly `cd`s to the repo root, sources `~/.bashrc` (falling back to `~/.profile`), and `exec`s Python. PASS, modulo missing `.gitignore` for `tmp/nightly-i18n-pr-body.md` (the wrapper unlinks it after `gh pr create`, but if it's killed between write and PR creation, the temp file is left behind). NIT.
- The `pom.xml` change is one line under an existing `<excludes>` block; it doesn't introduce new dependencies or plugins. PASS.
- README section is in the same style as the `prune-stale-worktrees.py` entry above it. PASS.

## Required actions before merge

1. Fix Co-Authored footer model name in wrapper + test (Issue 1).
2. Fix PR label model slug (Issue 2).
3. Add Co-Authored footer to PR body (Issue 3).
4. Refactor tests to import the real wrapper module via `importlib.util.spec_from_file_location` like `test_prune_stale_worktrees.py` does (Issue 4).
5. Fix push-failure data-loss: detect "no diffs but branch has unpushed commits" and skip `delete_branch` (Issue 5).
6. Move `import logging.handlers` to top-level (Issue 7) — required for Issue 4's importlib refactor.
7. Add behavioral tests for at least: lock contention, push-recovery no-op, pre-flight failure paths (Issue 6).
8. Add a one-line platform-support note to the README (Issue 18).
9. Run `./mvnw -pl modules/perc-i18n clean install` (after `git pull --ff-only`) and paste `BUILD SUCCESS` in the PR body.

## Handoff summary

- **Reviewed**: 5 files (3 new, 2 mod), 994 net new lines.
- **Top findings**: 3 simple convention bugs (footer model, label slug, PR-body footer) + 1 structural test bug (tests redefine logic locally) + 1 silent-data-loss bug (push-failure branch recovery). All five must be fixed before merge.
- **Dominant risk**: Issue 5 (data loss after push failure) is the only correctness-class defect; the others are convention / test-coverage defects.
- **Recommendation**: request-changes.
- **Next step**: hand to Hephaestus with the 9-item required-actions list. Re-run Erlang after the fixes.

