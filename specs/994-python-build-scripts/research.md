# Research: Cross-Platform Python Build Scripts

**Branch**: `994-python-build-scripts` | **Date**: 2026-07-21 | **Spec**: [spec.md](spec.md)

This document captures the research findings that resolve the "NEEDS CLARIFICATION" items and design questions raised by [plan.md](plan.md). Each finding has a Decision, Rationale, and Alternatives considered.

---

## R1 — Python interpreter discovery

**Context**: Each Python replacement script must work when invoked as `python3 <name>.py [args]` on Linux/macOS and `python <name>.py [args]` on Windows (FR-003). Developers may have only one of those on PATH.

**Decision**: Each script uses `sys.executable` for self-invocation but is **entered** via the operator's typed prefix (`python3` / `python`). The shebang is `#!/usr/bin/env python3` for Unix `chmod +x` ergonomics (FR-003). No automatic `python` ↔ `python3` discovery logic.

**Rationale**: Adding auto-discovery (try `python3` → fall back to `python` → fail) complicates the script and surfaces confusing errors when the fallback has different module versions. The simpler contract ("operator types the right name") matches the rest of the Python ecosystem and what `erlang-harvest-review-patterns.py` already does today.

**Alternatives considered**:
- Try `python3` then `python` automatically inside the script — rejected: silently picks wrong interpreter; masks broken Python installs.
- PEP 397 Windows launcher (`#!/usr/bin/env python3` shebang + Windows `py` launcher) — rejected: requires `py` launcher on PATH, not always installed; same problem as above.
- Ship a `.cmd` shim that runs the Python via `where python` first — rejected: defeats the FR-001 goal of one entry point per script.

---

## R2 — Subprocess semantics (replacing bash traps / signal handling)

**Context**: Several scripts in scope use bash traps (`trap 'cleanup' EXIT ERR INT TERM`), `set -euo pipefail`, and signal-based cleanup (e.g. `install-cms-dev.sh` cleans partial install on failure). Python's `subprocess.run(..., shell=False)` plus `try`/`finally` is the closest equivalent, but Windows has different signal semantics (no SIGTERM in interactive console; CTRL_BREAK_EVENT only with `creationflags`).

**Decision**:
- Use `subprocess.run([arg1, arg2, ...], shell=False, check=False, timeout=N, capture_output=True)` for all external calls (FR-008).
- Replace bash traps with `try`/`finally` blocks that call a cleanup function.
- For scripts that need to forward SIGINT/SIGTERM on Unix: use `signal.signal(signal.SIGINT, handler)` / `signal.SIGTERM`. Document the Windows limitation in `## Behavioral Notes` (FR-009b) — on Windows the cleanup still runs via `finally` but the signal handler is a no-op.
- Honor `set -euo pipefail` equivalent: each script checks the return code of subprocess calls explicitly and exits non-zero (FR-005).

**Rationale**: Matches Python ecosystem best practice. The behavior is "best-effort functional equivalence" per Clarification Q5. The behavioral delta (no SIGTERM on Windows) is rare in dev tooling — most scripts are invoked from interactive shells where Ctrl-C aborts the whole process tree anyway.

**Alternatives considered**:
- Wrap subprocess in a helper class that mimics `set -e` semantics — rejected: more complex; explicit `if rc != 0: sys.exit(rc)` is clearer.
- Use `subprocess.run(..., check=True)` — rejected: raises `CalledProcessError`; same effect as `sys.exit(e.returncode)` but harder to read; loses stderr in the exit message unless explicitly captured.

---

## R3 — `docs/ai-generated/tasks/#000-webui-src-layout/*.sh` scope decision

**Context**: Six `.sh` files under `docs/ai-generated/tasks/#000-webui-src-layout/` were identified by the glob scan as in-scope. The `#000-` prefix is the project's convention for **historical task rooms** (work that was already done; the directory is left in `docs/ai-generated/tasks/` as evidence). These scripts were one-shot WebUI src-layout migration helpers, now obsolete.

**Decision**: Do NOT migrate these to Python. Two acceptable outcomes:
- (Preferred) Delete the directory outright — the migration is complete; the scripts have no future callers.
- (Fallback) Leave the directory untouched — out-of-spec; covered by the existing `mvn-env.{sh,bat}` exemption-style carve-out.

This decision must be confirmed with the maintainer during implementation (the spec lists the directory in-scope; this research note is the implementer's exception request).

**Rationale**: Migrating historical scripts to Python wastes effort and pollutes pytest CI with tests for code that has no current consumers. The directory's naming convention (`#000-`) already signals "historical room".

**Alternatives considered**:
- Migrate to Python and add pytest coverage — rejected: zero user value.
- Keep `.sh`, add `## Behavioral Notes` docstring in Python stub — rejected: stub has nothing to test; misleading.

---

## R4 — pytest invocation under subprocess

**Context**: Some Python scripts in scope (e.g. `verify-distribution-archive`, `verify-pr-review-resolution`) themselves invoke shell commands (Maven, `gh`). When the test suite needs to invoke Python scripts under test, it should do so via `python -m <script>` not via a `pytest` binary on PATH.

**Decision**:
- Tests invoke scripts via `subprocess.run([sys.executable, "-m", "pytest", ...])` for pytest operations.
- Tests invoke scripts-under-test via `subprocess.run([sys.executable, str(script_path), ...])` where `script_path` is `pathlib.Path(__file__).parent / "<script>.py"`. The script is invoked as a module argument list, NOT via a `python3 <script>.py` string in `shell=True` mode.
- The CI runner `scripts/run-python-tests.{sh,cmd}` invokes `python3 -m pytest` (Linux/macOS) or `python -m pytest` (Windows) — NOT the bare `pytest` binary.

**Rationale**: Avoids PATH-discoverability differences across Windows / Linux. `python -m pytest` is the documented portable entry point per the pytest docs. The same applies to invoking the scripts-under-test from pytest fixtures.

**Alternatives considered**:
- Use `pytest` binary on PATH — rejected: not always installed; PATH discovery differs across shells.
- Use `pipx run pytest` — rejected: introduces a new third-party tool (pipx) not currently used in the repo.

---

## R5 — GitHub Actions matrix configuration

**Context**: SC-003 requires the new `.github/workflows/python-build-scripts.yml` to run on `ubuntu-latest` + `windows-latest` matrix, path-filtered to in-scope paths, Python-script tests only (no Maven).

**Decision**:
- Python version on runners: `python-version: '3.11'` (matches the GitHub-hosted runner default as of 2026; matches the project's `erlang-harvest-review-patterns.py` requirement of 3.9+ with headroom).
- Path filter (under `on.pull_request.paths` and `on.push.paths`): union of in-scope script directories from FR-013 + the workflow file itself + `scripts/requirements-dev.txt` + `scripts/run-python-tests.{sh,cmd}`.
- Steps: `actions/checkout@v4` → `actions/setup-python@v5` with `cache: 'pip'` and `cache-dependency-path: scripts/requirements-dev.txt` → `python -m pip install -r scripts/requirements-dev.txt` → `bash scripts/run-python-tests.sh` on ubuntu / `scripts\run-python-tests.cmd` on windows.
- NO `actions/setup-java`. NO Maven invocation. NO `mvn-env` reference (per Clarification Q2).

**Rationale**: `actions/setup-python@v5`'s built-in pip cache eliminates the need for `actions/cache@v4`. Pinning Python 3.11 (vs `3.x`) avoids surprise breakages when a new runner default ships.

**Alternatives considered**:
- `python-version: '3.x'` — rejected: surfaces breakage when GitHub rolls the default forward.
- Add a separate `nightly` cron to catch upstream pytest regressions — rejected: out of scope; pre-PR Maven gate is the project's only scheduled lint.

---

## R6 — Rejected big-bang-PR alternative

**Context**: Clarification Q1 asked whether to ship one PR or many. We recommended and the user accepted Option A (per-directory PRs). This entry records the explicit rejection of the big-bang alternative so the planning artifact is self-justifying.

**Decision**: Per-directory PRs only (5 PRs total for code + 1 PR for `scripts/requirements-dev.txt` + `scripts/run-python-tests.{sh,cmd}` + the GH Actions workflow + README updates, run first as a "foundation" PR). See [plan.md](plan.md) Source Code section for the per-directory breakdown.

**Rationale**:
- Each per-directory PR has its own reviewers (docker team, AI skills, dev tooling, CI/release).
- Per-PR Erlang review (root AGENTS pre-PR gate) catches issues faster; the review surface is one directory, not the whole script tree.
- Smaller PRs allow per-PR rollback if a single script regression slips through.
- The "foundation" PR (requirements-dev + runner + workflow) ships FIRST so subsequent PRs are gated by green CI from the start.

**Alternatives considered**:
- One big PR — rejected: high blast radius; reviewers see 30+ files; hard to bisect on regression; conflict-prone across scopes.
- Per-script PRs — rejected: too fine-grained; each PR has a 5-line diff which is below the Erlang-review noise floor; reviewers lose context.

---

## R7 — Path conventions across scopes

**Context**: Scripts live in 5 top-level scopes (`scripts/`, `docker/`, `modules/perc-distribution-tree/scripts/`, `modules/ai-shared-develop/scripts/`, `docs/ai-generated/tasks/#000-webui-src-layout/`). Each script needs to find its repo root (`pathlib.Path(__file__).resolve().parents[N]`) — the parent depth differs per scope.

**Decision**: Standardize on a small helper inside each script:

```python
from pathlib import Path
import sys

REPO_ROOT = Path(__file__).resolve().parents[N]
```

Where `N` is the depth from the script file to the repo root. Concretely:
- `scripts/<x>.py` → `parents[1]`
- `docker/scripts/<x>.py` → `parents[2]`
- `docker/entrypoint/<x>.py` → `parents[2]`
- `modules/perc-distribution-tree/scripts/<x>.py` → `parents[3]`
- `modules/ai-shared-develop/scripts/<x>.py` → `parents[3]`
- `modules/ai-shared-develop/src/main/resources/skills/<skill>/scripts/<x>.py` → `parents[6]`
- `docs/ai-generated/tasks/#000-webui-src-layout/<x>.py` → `parents[4]`

This helper is inlined per script (not imported) to keep each script self-contained — no shared `scripts/_common.py` import (avoids `sys.path` manipulation across scopes).

**Rationale**: Matches Python ecosystem norm. No shared import path means each script can be `python3 scripts/<x>.py` from anywhere; pytest discovers each `test_<x>.py` without `conftest.py` setup.

**Alternatives considered**:
- Shared `scripts/_common.py` with `REPO_ROOT` resolution — rejected: import path complexity; pytest needs `conftest.py` to find it; makes scripts harder to read in isolation.
- Pass repo root via env var from the runner — rejected: extra ceremony for a value derivable from `__file__`.

---

## R8 — Where to draw the line on `subprocess` wrapper complexity

**Context**: Some scripts invoke Maven (`mvn`), Docker Compose, `gh`, `curl`, `git`. Direct `subprocess.run([...])` is fine for one-shot calls, but scripts that make many calls (e.g. `perc-devctl.sh`) benefit from a small helper that standardizes timeout, error handling, and logging.

**Decision**: Per-script `def _run(cmd: list[str], **kwargs) -> subprocess.CompletedProcess` helper, inlined per script. No shared module. Standard kwargs:
- `timeout: int = 120` (default; override per call for long ops like Maven)
- `check: bool = False` (caller decides whether to propagate exit code)
- `capture_output: bool = False` (default False; scripts that need stdout/stderr set True)

**Rationale**: Aligns with R7's "self-contained scripts, no shared imports" principle. Keeps each script's complexity bounded.

**Alternatives considered**:
- Shared `_run` helper in `scripts/_common.py` — rejected (same reason as R7).
- Use `sh.something` from the `sh` PyPI package — rejected: third-party dep; FR-006 prohibits.
