# Data Model: Cross-Platform Python Build Scripts

**Branch**: `994-python-build-scripts` | **Date**: 2026-07-21 | **Spec**: [spec.md](spec.md)

This document captures the entities (file-level artifacts) that this feature introduces or constrains. The migration is essentially a refactor of executable scripts; the entities below describe the file shapes and their relationships.

---

## Entity 1 — Build Script (`*.py`)

**Purpose**: A cross-platform Python 3.9+ replacement for an existing `.sh`/`.bat` script.

**Attributes**:
| Field | Type | Description |
|-------|------|-------------|
| `path` | filesystem path | Absolute path under one of the in-scope top-level directories from FR-013 |
| `shebang` | `str` | Always `#!/usr/bin/env python3` (FR-003) |
| `encoding` | `str` | UTF-8; declared via `# -*- coding: utf-8 -*-` |
| `future_imports` | `bool` | `from __future__ import annotations` present (3.9 compatibility for postponed evaluation of annotations) |
| `module_docstring` | `str` | First statement; describes purpose, prereqs, usage; **must** include `## Behavioral Notes` if any deviation from the shell original exists (FR-009b) |
| `main_function` | `def main(argv: list[str] \| None = None) -> int` | Single entry point; returns exit code (0 = ok, non-zero = failure per FR-005) |
| `cli_parser` | `argparse.ArgumentParser` | One per script; mirrors the CLI of the replaced `.sh`/`.bat` (FR-002) |
| `stdlib_only` | `bool` | True; no third-party imports at runtime (FR-006) |
| `path_resolution` | `pathlib.Path` | `REPO_ROOT = Path(__file__).resolve().parents[N]` (R7) |
| `subprocess_calls` | `list[subprocess.run]` | Each call uses `shell=False`, explicit `timeout`, explicit error check (FR-008 / R2) |
| `logging` | `logging.getLogger(__name__)` | Standard library logging; format `%(asctime)s %(levelname)s %(message)s` |

**Validation rules** (enforced by `quickstart.md` + CI):
- `python3 -m py_compile <script>.py` exits 0
- `python3 <script>.py --help` exits 0 and prints usage containing the script's purpose (FR-009 happy/help paths)
- `python3 <script>.py <bogus-flag>` exits non-zero with a recognizable error substring (FR-009 failure path)
- No hardcoded `/` or `\\` in filesystem path joins (FR-007)
- No `shell=True` in `subprocess` calls (FR-008)
- All third-party imports checked: `grep -E "^import (?!pytest)|^from (?!pytest)" <script>.py` returns no PyPI-package names

**Lifecycle**: Created in the per-directory PR (FR-001a). Removed: never — the script lives until the workflow that uses it is itself deleted.

---

## Entity 2 — Colocated Test Module (`test_<script>.py`)

**Purpose**: pytest coverage for a Build Script (FR-009).

**Attributes**:
| Field | Type | Description |
|-------|------|-------------|
| `path` | filesystem path | Same directory as `<script>.py`; named `test_<script>.py` |
| `pytest_version` | `str` | Pinned in `scripts/requirements-dev.txt` (e.g. `pytest==8.3.*`) |
| `fixture_scope` | `str` | Default `function`; switch to `session` only when fixture is expensive and read-only |
| `invocation` | `subprocess.run([sys.executable, str(script_path), ...])` | Per R4 — never bare `python3` string |
| `happy_path_test` | `def test_<script>_succeeds()` | Asserts exit 0, captures stdout/stderr for verification |
| `failure_path_test` | `def test_<script>_fails_on_<scenario>()` | Asserts exit != 0; asserts stderr contains a recognizable substring |
| `help_path_test` | `def test_<script>_help()` | Asserts exit 0 and stdout contains `usage:` or equivalent (FR-009) |
| `network_access` | `bool` | False where possible (FR-010); True scripts get marked `@pytest.mark.network` and skipped in offline mode |

**Validation rules**:
- All three test cases exist (happy / failure / help)
- Tests run on Linux (SC-002) and Windows (SC-003) without modification
- Tests do not depend on the presence of `gh`, `docker`, or `mvn` — those are dependency-failures, not pytest failures; if a script requires them, the script's behavior is tested under a `try`/`except ImportError` or `pytest.importorskip` pattern

**Lifecycle**: Created alongside the Build Script. Removed: only if the Build Script is itself removed.

---

## Entity 3 — Script Catalog Entry (`scripts/README.md`)

**Purpose**: Human-readable documentation of each Build Script (FR-011, FR-014).

**Attributes**:
| Field | Type | Description |
|-------|------|-------------|
| `heading` | `Markdown` | `### <python-script-name>.py` |
| `purpose` | `str` | One-sentence "what this does" |
| `usage` | `code block` | `python3 <script>.py [args]` invocation; mention Windows `python` prefix in a note |
| `prereqs` | `list[str]` | E.g. `JDK 21`, `gh auth login`, etc. |
| `output` | `str` | What the script prints to stdout/stderr; `RESULT:OK STEP:<x> LOG:<path>` pattern preserved where the original used it |
| `cross_platform_note` | `str` | "Works on Windows, Linux, macOS" (delete the old "Cross-platform: Windows users run the `.cmd` counterpart" per FR-011) |
| `tests` | `code block` | `python3 -m pytest test_<script>.py -v` |
| `evidence_links` | `list[str]` | Where pytest results / CI logs land |

**Validation rules**:
- Each in-scope script has one catalog entry
- Catalog entry references the Python script by name (not the old `.sh`/`.bat`)
- Legacy "Windows users run the `.cmd` counterpart" notes deleted (FR-011)

---

## Entity 4 — Test Runner (`scripts/run-python-tests.{sh,cmd}`)

**Purpose**: Single command to install pytest + run all in-scope tests (FR-009a, SC-008).

**Attributes**:
| Field | Type | Description |
|-------|------|-------------|
| `path` | filesystem path | `scripts/run-python-tests.sh` (Linux/macOS) and `scripts/run-python-tests.cmd` (Windows) |
| `responsibility` | `list[str]` | (1) `python3 -m pip install -r scripts/requirements-dev.txt`; (2) `python3 -m pytest <in-scope script dirs>` |
| `in_scope_dirs` | `list[str]` | `scripts/`, `docker/scripts/`, `docker/entrypoint/`, `modules/perc-distribution-tree/scripts/`, `modules/ai-shared-develop/scripts/`, `modules/ai-shared-develop/src/main/resources/skills/*/scripts/` (FR-013 in-scope enumeration) |
| `exit_code` | `int` | Propagates pytest's exit code (0 = all pass; non-zero = any failure) |
| `idempotent` | `bool` | Re-running on a clean clone is safe; `pip install` is a no-op when up-to-date |

**Validation rules**:
- Running from a fresh clone on Linux exits 0 (SC-008)
- Running from a fresh clone on Windows exits 0 (SC-003 / SC-008)
- Pip install step fails loudly if `scripts/requirements-dev.txt` is missing

---

## Entity 5 — CI Workflow (`.github/workflows/python-build-scripts.yml`)

**Purpose**: Cross-OS CI gate enforcing SC-003 (FR-012a).

**Attributes**:
| Field | Type | Description |
|-------|------|-------------|
| `triggers` | `on: pull_request, push (development)` | Path-filtered |
| `path_filter` | `paths:` | Union of in-scope script directories + `scripts/requirements-dev.txt` + `scripts/run-python-tests.{sh,cmd}` + the workflow file itself |
| `matrix` | `[ubuntu-latest, windows-latest]` | Per R5 |
| `python_version` | `'3.11'` | Per R5 |
| `actions` | `actions/checkout@v4` → `actions/setup-python@v5` (with pip cache keyed on `scripts/requirements-dev.txt`) → run runner | Per R5 |
| `forbidden_steps` | `actions/setup-java`, `mvn`, any Maven invocation | Per Clarification Q4 — Python-only CI |

**Validation rules**:
- Workflow file lints against GitHub Actions schema
- Path filter correctly excludes unrelated PRs (e.g. changes to `system/` should not trigger)
- Both runners exit green on a known-good PR that touches only in-scope paths

---

## Relationships

```text
Build Script (Entity 1)
  ├── tested by ──▶ Colocated Test Module (Entity 2)        [FR-009]
  ├── documented by ──▶ Script Catalog Entry (Entity 3)      [FR-011, FR-014]
  └── exercised by ──▶ Test Runner (Entity 4)               [FR-009a]

Test Runner (Entity 4)
  ├── installs ──▶ scripts/requirements-dev.txt              [FR-009a]
  └── invoked by ──▶ CI Workflow (Entity 5)                 [FR-012a, SC-003]

CI Workflow (Entity 5)
  └── path-filters on ──▶ Build Script paths + Runner files   [SC-003]
```

## State Transitions

The migration is per-directory and per-PR; there are no runtime state machines. The relevant "state" is the per-directory PR lifecycle:

```text
[not started]
    │  (developer claims a directory in specs/994-python-build-scripts/tasks.md)
    ▼
[Python script written + tests passing locally]
    │  (PR opened; Erlang review passes)
    ▼
[PR merged → original .sh/.bat deleted]
    │  (next directory repeats the cycle)
    ▼
[all directories merged → SC-001 / SC-002 / SC-005 / SC-006 satisfied]
    │  (final SC-008 check)
    ▼
[migration complete]
```

---

## Data Volume / Scale Assumptions

- In-scope scripts: ~30+ files (exact count to be locked in `tasks.md` during `/speckit.tasks`)
- pytest runtime: ≤ 5 minutes per runner (CI budget)
- `scripts/requirements-dev.txt` size: ~10 lines (pytest + transitive test deps if any)
- New CI workflow file: ~50 lines
- Per-PR diff size: ≤ 2000 lines (typical for a directory conversion; the foundation PR is smaller)

