# Implementation Plan: Cross-Platform Python Build Scripts

**Branch**: `[994-python-build-scripts]` | **Date**: 2026-07-21 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/994-python-build-scripts/spec.md`

## Summary

Replace all in-scope build-time `.sh`/`.bat` scripts with cross-platform Python 3.9+ scripts and remove the original shell files. The migration is delivered as per-directory PRs (one PR per top-level in-scope scope: `scripts/`, `docker/`, `modules/perc-distribution-tree/scripts/`, `modules/ai-shared-develop/scripts/`, `docs/ai-generated/tasks/#000-webui-src-layout/`). A new top-level `scripts/requirements-dev.txt` pins pytest; new `scripts/run-python-tests.{sh,cmd}` runners install deps and execute pytest; a new `.github/workflows/python-build-scripts.yml` runs the Python-script tests on `ubuntu-latest` + `windows-latest` matrix (Python only, no Maven). Runtime scripts (installer, server start/stop, jetty, Derby DB, patch-tools, TableFactory admin, deployed test fixtures) are explicitly OUT OF SCOPE; `mvn-env.{sh,bat}` are also excluded (already cross-platform, per Clarification Q2).

## Technical Context

- **Language/Version**: Python 3.9+ (target); Bash 4+/Windows `cmd.exe` only as needed for the tiny `run-python-tests.{sh,cmd}` launcher shims (no logic — just delegate)
- **Owning Module(s)**:
  - `scripts/` (root — repo-wide dev/release tooling)
  - `docker/scripts/`, `docker/entrypoint/` (dev container tooling)
  - `modules/perc-distribution-tree/scripts/` + the developer convenience `APIUpdate-*.bat`, `UpdateTinyMCE.bat` (build verification + rebuild helpers)
  - `modules/ai-shared-develop/scripts/` + `modules/ai-shared-develop/src/main/resources/skills/*/scripts/` (AI dev tooling + skill helpers)
  - `docs/ai-generated/tasks/#000-webui-src-layout/*.sh` (one-shot WebUI migration helpers; these are git-history-style tooling, may be archived rather than migrated — see research.md R3)
- **AGENTS Hierarchy** (Rule Discovery Protocol — read in this order, apply most-specific first):
  - Root `/AGENTS.md` (cross-platform path rules, `-euo pipefail`-equivalent, `scripts/` convention, Pre-PR Maven gate)
  - `scripts/AGENTS.md` if present (likely empty — verify during implementation)
  - `modules/perc-distribution-tree/AGENTS.md` (build verification helpers)
  - `modules/ai-shared-develop/AGENTS.md` (AI tooling)
  - `docker/AGENTS.md` if present (docker dev patterns)
  - For per-PR work that touches Java code (e.g. `APIUpdate-*.bat` rewrites land as Python but the underlying Maven rebuild is unchanged): apply module `AGENTS.md` for the Java side
- **Dependencies & Storage**:
  - Runtime: Python stdlib only (`argparse`, `subprocess`, `pathlib`, `json`, `urllib`, `csv`, `hashlib`, `re`, `shutil`, `logging`, `concurrent.futures` as needed) — NO new third-party deps in `pom.xml`/`package.json` (FR-006)
  - Test: pytest pinned in `scripts/requirements-dev.txt` (e.g. `pytest==8.3.*`)
  - Storage: no DB; no new Maven artifacts; no `.ppkg` content
- **Testing**:
  - Per-script: pytest module `test_<name>.py` colocated (FR-009). Required scenarios: happy-path (exit 0), failure-path (non-zero, recognizable substring), `--help` (exit 0, usage printed)
  - Cross-OS: new GH Actions workflow `python-build-scripts.yml` matrix `ubuntu-latest` + `windows-latest`, path-filtered (SC-003)
  - Offline where possible: use fixtures from `scripts/test-fixtures/` (already exists: `triage-good.md`, `triage-bad.md`)
  - No new Maven tests needed (FR-006 limits new deps; FR-007/008 are pathlib/subprocess rules)
- **Scale/Impact**:
  - Scripts in scope (per FR-013 in-scope enumeration): ~30+ files across 5 directories; per-directory PRs (FR-001a)
  - User roles: developers, release/CI engineers, Erlang review maintainers, AI agent users, docker dev users (per Module Scope)
  - Install/upgrade impact: **none** — build/dev tooling only; no installer payload; no runtime deployable; no DB schema; no `.ppkg`

## Constitution Check

- [x] **I. Module-First Boundaries** — Owning modules enumerated above; AGENTS hierarchy documented; per-PR work stays inside its scope directory
- [x] **II. Evidence Over Invention** — No new APIs invented; Python replacements target existing CLI surfaces of replaced `.sh`/`.bat` files; stdlib only (no invented third-party deps)
- [x] **III. Test Discipline** — FR-009/FR-010 mandate pytest per script; SC-002/SC-003 enforce passing on Linux + Windows CI
- [x] **IV. Contract & Integration Integrity** — CLI contracts documented in `contracts/cli-schemas.md`; FR-002 enforces parity with replaced `.sh`/`.bat` CLIs; FR-013 protects all deployed/installer `.bat` files
- [x] **V. Safe Modernization** — No Spring Boot, no new frameworks; minimal blast radius; per-directory PR phasing (Clarification Q1)
- [x] **VI. Security by Default** — No new attack surface (no network code added; existing `gh` / `curl` / `docker compose` invocations are wrapped, not extended); secrets handling unchanged (e.g. Sigstore OIDC token cache lives only in `mvn-env.sh`, which is OUT of scope)
- [x] **VII. Build & Dependency Hygiene** — Python is a build/dev dep only, declared in `scripts/requirements-dev.txt`; not added to `pom.xml` or `package.json`; no JDK impact; per-module `./mvn-env.sh clean install` must remain green (SC-007)
- [x] **VIII. Documentation & Operability** — FR-011/FR-014 mandate README + AGENTS updates; per-script docstring with `## Behavioral Notes` (FR-009b); `quickstart.md` provides end-to-end validation
- [x] **IX. PR Review Comment Resolution** — Each per-directory PR follows the standard inline-reply + `resolveReviewThread` discipline per root `AGENTS.md`
- [x] **Complexity Budget** — No constitution violations; the per-directory phasing is the explicit simplification strategy (vs. a big-bang PR)

## Project Structure

### Documentation (this feature)

```text
specs/994-python-build-scripts/
├── plan.md              # Technical plan (this file)
├── research.md          # Phase 0 — research findings
├── data-model.md        # Phase 1 — entity / file model
├── quickstart.md        # Phase 1 — end-to-end validation guide
├── contracts/
│   └── cli-schemas.md   # CLI surface contract per in-scope script
├── checklists/
│   └── requirements.md  # (created by /speckit.specify)
└── tasks.md             # Generated by /speckit.tasks
```

### Source Code (affected paths — to be touched across per-directory PRs)

```text
# Scope 1 — repo-root scripts/
scripts/
├── *.py                (new — replaces *.sh / *.bat)
├── test_*.py           (new — colocated pytest per FR-009)
├── requirements-dev.txt (new — pinned pytest)
├── run-python-tests.sh  (new — Linux/macOS runner, FR-009a)
├── run-python-tests.cmd (new — Windows runner, FR-009a)
├── test-fixtures/       (existing; extended as needed)
├── README.md            (updated — FR-011, FR-014)
├── release-audit/       (Python conversions + tests; keep .sh removed)

# Scope 2 — docker dev tooling
docker/
├── scripts/
│   ├── *.py             (replaces *.sh)
│   └── test_*.py
├── entrypoint/
│   ├── *.py             (replaces *.sh)
│   └── test_*.py
└── README.md            (updated)

# Scope 3 — modules/perc-distribution-tree build scripts
modules/perc-distribution-tree/
├── scripts/
│   ├── *.py             (replaces *.sh / *.bat)
│   └── test_*.py
├── APIUpdate-*.bat      (deleted; equivalent Python under scripts/ or replaced by Maven invocation)
├── UpdateTinyMCE.bat    (deleted; equivalent Python)
└── scripts/README.md    (updated)

# Scope 4 — modules/ai-shared-develop dev + skill scripts
modules/ai-shared-develop/
├── scripts/
│   ├── *.py             (replaces *.sh)
│   └── test_*.py
└── src/main/resources/skills/*/scripts/
    ├── *.py             (replaces *.sh)
    └── test_*.py

# Scope 5 — docs/ai-generated/tasks/#000-webui-src-layout/
docs/ai-generated/tasks/#000-webui-src-layout/
├── *.sh                 (deleted; if no longer needed — see research.md R3)

# CI
.github/workflows/
└── python-build-scripts.yml  (new — FR-012a, SC-003)

# Touched AGENTS.md / README.md for cross-references
AGENTS.md                 (no change to mvn-env mention; only scripts/README references)
docker/README.md          (updated)
modules/perc-distribution-tree/AGENTS.md  (updated)
modules/ai-shared-develop/AGENTS.md       (updated)
```

### OUT OF SCOPE — must NOT be touched

- `mvn-env.sh`, `mvn-env.bat` (Clarification Q2)
- `system/release/installer/**`, `system/release/ShellScripts/**`, `system/installResources/**`, `system/Tools/**`
- `system/Testing/install.bat`, `system/Testing/beprovider/**`, `system/Testing/becredentials/**`
- `system/cms/content/applications/word_prj/signocx.bat`
- `system/src/test/resources/.../runRhythmyxItemCreator.{sh,bat}`
- `modules/perc-jetty/src/main/jetty/**` runtime scripts
- `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/**`
- `modules/TableFactory/{importData,exportData}.{sh,bat}`
- `modules/patch-tools/{install,uninstall}.{sh,bat}`
- `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/**`
- `deliverytiersuite/delivery-tier-suite/p13n-ds/resource/derby/{DatabaseShutdown,DatabaseStartup}.bat`
- `projects/sitemanage/src/test/resources/service/importSites.bat`

## Phase 0 — Research

See [research.md](research.md). Key decisions:
- **R1**: Python interpreter discovery — use `sys.executable` directly; rely on operator typing `python` / `python3` (no PEP 397 launcher dependency). Document in per-script docstring.
- **R2**: Subprocess semantics — `subprocess.run([...], shell=False, check=False, timeout=N)` per FR-008; replace bash traps with `try`/`finally` + explicit `signal.signal` only where load-bearing.
- **R3**: `docs/ai-generated/tasks/#000-webui-src-layout/*.sh` — these are historical one-shot migration helpers (already merged per the `#000-` task room convention). Decision: do NOT migrate; verify with maintainer during implementation whether to archive or delete.
- **R4**: pytest invocation under `subprocess.run` — invoke `python -m pytest` (not `pytest` binary) so PATH discovery differences don't bite on Windows.
- **R5**: GitHub Actions matrix — `ubuntu-latest` + `windows-latest`; Python 3.11 (matches CI standard for GitHub-hosted runners as of 2026); `actions/checkout@v4`; pip cache via `actions/setup-python@v5`'s built-in cache.

## Phase 1 — Design & Contracts

See [data-model.md](data-model.md), [contracts/cli-schemas.md](contracts/cli-schemas.md), and [quickstart.md](quickstart.md).

Key design points:
- **Entity model**: Build Script (CLI surface, in-scope directory, pytest colocated test), Script Catalog Entry, Colocated Test Module
- **CLI contracts**: documented per script in `contracts/cli-schemas.md` with `argparse` argument name, type, default, env var, behavioral note (when deviation exists)
- **Quickstart**: end-to-end validation per scope (clone, install pytest, run runner, observe green CI)

## Complexity Tracking

*(No constitution violations — see Constitution Check above.)*

The one Complexity-Budget item worth recording: per-directory phasing (Clarification Q1) is itself a deliberate non-scope-creep decision; a single PR was considered and rejected (see research.md R6). No justification table needed.

## Re-evaluation — Constitution Check (post-design)

- [x] **I. Module-First Boundaries** — Data model stays at file/module level; no new entities require module placement decisions
- [x] **II. Evidence Over Invention** — All CLI surfaces are direct ports of existing `.sh`/`.bat`; no new args invented
- [x] **III. Test Discipline** — Each script has pytest contract in `contracts/cli-schemas.md`; `quickstart.md` walks the full pytest run
- [x] **IV. Contract & Integration Integrity** — `contracts/cli-schemas.md` pins exact CLI parity per script; FR-013 protects all deployable `.bat` files
- [x] **V. Safe Modernization** — Per-directory PRs minimize blast radius
- [x] **VI. Security by Default** — No new attack surface; secrets handling unchanged; subprocess calls documented as argv-list only
- [x] **VII. Build & Dependency Hygiene** — pytest pinned; stdlib only at runtime; no Java/Kotlin deps touched
- [x] **VIII. Documentation & Operability** — `quickstart.md` covers end-to-end validation; README updates per FR-011; per-script `## Behavioral Notes` per FR-009b
- [x] **IX. PR Review Comment Resolution** — Per-PR inline reply + thread resolution per root AGENTS.md
- [x] **Complexity Budget** — No violations

