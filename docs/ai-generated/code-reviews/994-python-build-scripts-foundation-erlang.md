# Erlang review — spec 994 foundation PR

## Summary

Foundation PR for spec 994-python-build-scripts. Ships the pytest manifest
(`scripts/requirements-dev.txt`), the cross-platform runner shims
(`scripts/run-python-tests.{sh,cmd}`), the path-filtered GitHub Actions matrix
workflow (`.github/workflows/python-build-scripts.yml`), the US1 regression
sentinel (`scripts/test_mvn_env_untouched.py`), and the supporting
`.gitignore` entries. Scope is intentionally narrow — harness + sentinel only —
and downstream per-directory conversions are explicitly deferred to later PRs
per FR-001a / Clarification Q1.

Smoke-tested locally on Linux (Python 3.12.3, pytest 8.3.5): all 18 sentinel
cases pass; runner `--help` works; `bash -n` clean; `py_compile` clean. Every
sentinel fingerprint was grep-verified against the real `mvn-env.{sh,bat}` on
the working tree. Cross-platform path checklist: clean. Constitution gates II,
III, VIII verified.

## Scope

- **Base**: `origin/development` (5a4a4cd3a1)
- **Head**: branch `994-python-build-scripts`, worktree uncommitted
- **Files**: 5 new + 1 modified (6 source files; spec docs in
  `specs/994-python-build-scripts/` excluded from review focus)
- **Prior report**: none (first Erlang pass on this branch)
- **Memory patterns hit**: `tests.structural-only` (verified non-application
  here), `paths.hardcoded-sep` (verified clean), `false-green-exit` (verified
  clean — pip install failure routes to exit 2 in both runners)
- **Independence**: this review was authored in a fresh read-only pass; no
  implementer memory of writing the foundation PR

## Recommendation

**approve**

## Gate

- **Blocking bugs**: 0
- **May commit/push**: **yes**

## Issues

### Issue 1 — Severity: info
- File: `scripts/run-python-tests.sh:87` and `scripts/run-python-tests.cmd:74`
- Description: `pip install` does not pass `--break-system-packages` (or set
  `PIP_BREAK_SYSTEM_PACKAGES=1`). On modern Ubuntu 24.04 host Pythons this can
  trip PEP 668. In the GH Actions context this is mitigated because
  `actions/setup-python@v5` installs Python from deadsnakes into a separate
  prefix where `pip install` works without the flag, but a developer on a
  fresh Ubuntu 24.04 box who runs the runner against their system Python will
  hit the externally-managed-environment error.
- Suggestion: optional. Either (a) document in the runner's usage block that
  developers on PEP 668 hosts should use `--skip-install` after pre-installing
  into a venv, or (b) add `PIP_BREAK_SYSTEM_PACKAGES=1` as an env-var set
  before the install call. Not a CI blocker because the workflow's Python is
  not the system Python.
- Status: open (non-blocking)

### Issue 2 — Severity: info
- File: `scripts/run-python-tests.cmd:32`
- Description: `REQUIREMENTS_FILE` is built as
  `%PROJECT_ROOT%\scripts\requirements-dev.txt` where `PROJECT_ROOT` is
  `%~dp0..` (i.e. `<reporoot>\scripts\..`). The resulting path contains a
  redundant `..\scripts` round-trip — it resolves correctly on Windows but is
  harder to read than `cd` once and using a relative `scripts\requirements-dev.txt`.
- Suggestion: optional. Either leave as-is (functional, just verbose) or
  change the check to use the post-`pushd` working directory:
  `if not exist "scripts\requirements-dev.txt"`. Not a bug.
- Status: open (non-blocking)

### Issue 3 — Severity: info
- File: `scripts/test_mvn_env_untouched.py:135-147`
- Description: The `.bat` balance heuristic counts standalone `)` lines as
  close-parens. If a future `mvn-env.bat` revision adds an unrelated `)`
  (e.g. in an `echo` or `for` body), the count drifts and the sentinel
  fires a false positive. Documented in the docstring as a "Heuristic".
- Suggestion: optional. The two if-blocks in the current file are stable and
  the heuristic is bounded by the file's narrow scope; future
  `mvn-env.bat` changes that add `for /f (...)` etc. would require updating
  the regex. Acceptable for a regression sentinel. No action required.
- Status: open (non-blocking)

### Issue 4 — Severity: info
- File: `specs/994-python-build-scripts/tasks.md:T007` (spec doc, not in diff)
- Description: The task description hints at `"set -e"` for `.sh` and
  `"@setlocal"` for `.bat` as example snippets; the shipped sentinel uses
  more comprehensive fingerprints (full content snippets + size floor +
  structural balance). This is a richer check than the spec hinted at, not a
  shortfall. Noted here for traceability.
- Suggestion: none. The shipped sentinel strictly satisfies SC-004 and the
  spec's intent.
- Status: noted

## Cross-platform path / file I/O checklist (always)

| Smell | Result |
|-------|--------|
| Hardcoded `/` or `\\` in filesystem paths | **None.** Sentinel uses `pathlib.Path.resolve().parent.parent` and `Path("mvn-env.sh")` joins only. `scripts/run-python-tests.sh` uses `$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)` (POSIX-portable). `scripts/run-python-tests.cmd` uses `%~dp0..` (Windows-portable). Workflow paths are GH Actions glob patterns, not OS paths. |
| Unix-only absolute roots (`/tmp`, `/var`, `/home`) | **None.** |
| Windows-only roots (`C:\…`) | **None** in Python or YAML. `.cmd` references are relative. |
| Multi-path list joined with `:` or `;` | **N/A** (no multi-path lists in this PR). |
| Path regex asserting Unix shape | **None.** Sentinel uses `pathlib`. |
| Case-sensitive path/import assumptions | **None** (paths via `Path` API). |
| CRLF-only line-ending assertions | **None.** Sentinel normalizes via `read_text(encoding="utf-8")`. |
| Unix-shell-only automation with no `.bat` counterpart | **None.** Runner ships both `.sh` and `.cmd`. |
| `shell=True` / `os.system` / `bash -c` in product code | **None.** Sentinel does no subprocess at all. |
| Hardcoded `subprocess.run(shell=True)` in any new Python | **N/A** — sentinel has no subprocess. (FR-008 applies to the later per-script conversions.) |

## Constitution check

- **II. Evidence Over Invention** — pytest 8.3, `pathlib`, `actions/setup-python@v5`,
  `re.findall` with `re.MULTILINE` — all real, all standard. No invented APIs.
- **III. Test Discipline** — sentinel itself is a behavioral test (18 cases
  on this host, all passing); runner shims exercised by the GH Actions matrix
  on `ubuntu-latest` + `windows-latest` per FR-012a.
- **VIII. Documentation & Operability** — sentinel module docstring explains
  rationale, snippet provenance, and size-floor maintenance. Runner shims have
  Usage / Flags / Exit-codes blocks.

## Verification performed (this session)

- `python3 -m py_compile scripts/test_mvn_env_untouched.py` → clean
- `bash -n scripts/run-python-tests.sh` → clean
- `bash scripts/run-python-tests.sh --help` → exit 0, prints usage
- `python3 scripts/test_mvn_env_untouched.py -v` (via pytest 8.3.5) →
  **18 passed in 0.23s**
- All 12 snippet fingerprints grep-verified against the live
  `mvn-env.{sh,bat}` on this working tree
- `if`/`fi` counts on `mvn-env.sh` balanced (3/3); `if … (` / `)` on
  `mvn-env.bat` balanced (2/2)
- All 6 in-scope script dirs (`scripts/`, `docker/scripts/`, `docker/entrypoint/`,
  `modules/perc-distribution-tree/scripts/`, `modules/ai-shared-develop/scripts/`,
  `modules/ai-shared-develop/src/main/resources/skills/`) exist
- All 5 new files LF (consistent with repo); `.cmd` runs on Windows even
  with LF because modern Windows tolerates it
- `git check-ignore -v scripts/__pycache__/ scripts/test.pyc` →
  both matched by the new `.gitignore` rules

## Notes (non-blocking)

- The sentinel is intentionally robust-but-bounded: it doesn't attempt a
  full bash/cmd parse, but covers the three classes of regression that
  matter for SC-004 (deletion, truncation, semantically-meaningful rewrite).
- The .gitignore additions also clear the `pycache_local_path` memory note
  ("scripts/__pycache__/ is produced locally by python3 -m py_compile;
  should be deleted before commit (no .gitignore entry yet)") — this PR
  closes that gap.
- No Maven, no `actions/setup-java`, no JDK work in this PR. SC-007 is
  explicitly deferred to per-directory phases (T064, T075).
- Pattern memory is unchanged; nothing in this PR warrants promoting a new
  general principle. The cross-platform checklist passes cleanly, and the
  sentinel's "snippet + size + balance" trio is itself worth considering as
  a future pattern for regression sentinels, but it is too narrow / one-off
  to justify a `patterns.md` entry.

## Required fixes

None.

## Handoff

- Foundation PR is structurally minimal, correct, and ready for merge.
- Author may commit and open the PR.
- Recommended PR title: `build(ci): add cross-platform Python-script test harness (spec 994 foundation)`
- PR body should include: (a) pointer to spec 994 / FR-009a / FR-012a /
  SC-003 / SC-004 / SC-008; (b) the `python3 -m pytest scripts/test_mvn_env_untouched.py -v`
  output showing 18/18 pass; (c) the GH Actions run URL once green on both
  runners (can be added after first push).