# Feature Specification: Cross-Platform Python Build Scripts

**Feature Branch**: `[994-python-build-scripts]`
**Created**: 2026-07-21
**Status**: Draft
**Input**: "We have been using bat scripts on windows and bash scripts on linux for build tasks, scripts, and automation. Maintaining these has become a challenge. I would like a spec and plan created to convert all BUILD time shell / bat files to cross platform python scripts. and remove the bat and sh files. This should not affect RUNTIME scripts that are deployed to customer installations."

⚠️ `branch_numbering` in `.specify/init-options.json` is deprecated. Rename to `feature_numbering`.

## Module Scope

- **Primary module(s)**: repo-root `scripts/`, `docker/scripts/`, `docker/entrypoint/`, `modules/perc-distribution-tree/scripts/`, `modules/ai-shared-develop/scripts/`, `modules/ai-shared-develop/src/main/resources/skills/*/scripts/`, `docs/ai-generated/tasks/#000-webui-src-layout/*.sh`
- **Explicitly EXCLUDED from this spec**: repo-root `mvnw / mvnw.cmd` — already works cross-platform and is left untouched (Q2 clarification)
- **Secondary / integration modules**: `modules/perc-distribution-tree/` (developer convenience `APIUpdate-*.bat`, `UpdateTinyMCE.bat`); documentation under `scripts/README.md`, module `AGENTS.md`, root `AGENTS.md`, `docker/README.md`, `.specify/**` only as it relates to commands the spec touches
- **AGENTS files to apply**: root `AGENTS.md` (cross-platform file I/O & paths, pre-PR Maven gate, `scripts/` convention, `-euo pipefail`-equivalent rule); `scripts/AGENTS.md` if present; `modules/perc-distribution-tree/AGENTS.md` (build verification helpers); `modules/ai-shared-develop/AGENTS.md`; `docker/AGENTS.md` if present
- **User roles affected**: developers (run `mvnw`, `install-cms-dev.sh`, `hot-deploy-local.sh`), release/CI engineers (run verify/audit scripts in `scripts/release-audit/` and `scripts/verify-*.sh`), Erlang review maintainers (`scripts/erlang-harvest-review-patterns.{sh,bat}`), AI agent users (skill scripts under `modules/ai-shared-develop/src/main/resources/skills/`), docker dev users
- **Install / upgrade impact**: none (build/dev tooling only — no installer payload, no `.ppkg` content, no runtime deployable). Runtime scripts (installer, console, server start/stop, jetty, Derby DB, patch-tools, TableFactory admin, test resources) are explicitly OUT OF SCOPE

## User Scenarios & Testing

Each story must be independently testable.

### User Story 1 - Developer runs the unchanged Maven wrapper on any OS (Priority: P1)

A developer clones the repo on Windows, Linux, or macOS, sets `JAVA_HOME_21`, and runs the build from a module directory. The existing `mvnw` (Linux/macOS) and `mvnw.cmd` (Windows) keep working exactly as today; this spec does NOT migrate them.

**Acceptance Scenarios**:
1. **Given** a developer on Linux with `JAVA_HOME_21` set, **When** they run `./mvnw clean install -pl rest -am` from `rest/`, **Then** the build proceeds exactly as before (no behavioral drift introduced by this spec)
2. **Given** a developer on Windows with `JAVA_HOME_21` set, **When** they run `mvnw.cmd clean install -pl rest -am` from `rest/`, **Then** the build proceeds exactly as before
3. **Given** `mvnw` and `mvnw.cmd` exist on `development`, **When** `git grep -E 'Maven wrapper\.(sh|bat)' -- ':!*.ppkg' ':!docs/ai-generated/code-reviews/*'` runs, **Then** all references to those files remain (none deleted) and all references in `AGENTS.md` etc. still describe the existing two-file UX

### User Story 2 - CI/release verify scripts run identically on every OS (Priority: P1)

A release engineer runs the gating suite (`verify-triage-inventory`, `verify-valid-fixes`, `verify-suppressions`, `verify-distribution-archive`, `verify-pr-review-resolution`, `verify-no-finder-jsp-references`, `verify-no-jqplot-vendor-refs`, `verify-codeql-analyzer-of-record`) on a CI agent that may be Linux or Windows. Each script must produce identical pass/fail behavior and exit codes on both, with the same CLI flags.

**Acceptance Scenarios**:
1. **Given** a CI agent on Linux running `python3 scripts/verify-triage-inventory.py` against a known-good fixture, **When** the fixture satisfies all rules, **Then** the script exits 0
2. **Given** the same script and fixture on Windows, **When** it runs, **Then** exit code, stdout, and stderr match the Linux run byte-for-byte except for hard line-ending differences
3. **Given** a fixture that violates a rule, **When** the script runs on either OS, **Then** the script exits non-zero and prints a diagnostic naming the failing row

### User Story 3 - Erlang review pattern harvesting works cross-platform (Priority: P2)

A maintainer of Erlang review pattern memory runs `erlang-harvest-review-patterns` from any developer machine. They get the same candidates report and the same `--apply` merge result, regardless of OS.

**Acceptance Scenarios**:
1. **Given** an authenticated `gh` CLI, **When** `python3 scripts/erlang-harvest-review-patterns.py --apply` runs on Linux, **Then** the candidate report is generated and the multi-PR themes are appended to `patterns.md`
2. **Given** the same command on Windows, **When** it runs, **Then** exit code and `patterns.md` diff are identical to the Linux run

### User Story 4 - AI skill scripts and docker dev tooling work cross-platform (Priority: P2)

An AI agent invokes a skill helper (`api-client.sh`, `install-cms.sh`, `start-cms.sh`, `start-dts.sh`, `install-dts.sh`, `download-latest.sh`, `generate-javadoc-stubs.sh`) and a developer uses docker dev tooling (`hot-deploy-jar.sh`, `perc-devctl.sh`, `install-update.sh`). Both must work on any host OS.

**Acceptance Scenarios**:
1. **Given** a developer on Windows invoking `python docker/scripts/perc-devctl.py up --build`, **When** the command runs, **Then** the docker compose project comes up the same way it does on Linux
2. **Given** an AI agent invoking `python modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/scripts/api-client.py --help`, **When** the command runs, **Then** the same help text and exit code is shown on Linux and Windows

### Edge Cases

- What happens when a script is invoked with `-h`/`--help`? → Each Python script must print a usage banner and exit 0 (argparse default) on either OS
- What happens when Python is missing on the host? → Out of scope: the project already requires Python 3.9+ for `erlang-harvest-review-patterns`; if a host lacks Python, the script should print a clear error and exit non-zero (no shell fallback)
- What happens when a build script is invoked from a module subdirectory? → Relative paths in scripts must be resolved with `pathlib.Path(__file__).resolve().parent` (no `cd`-then-`..` chains)
- What happens when a script is invoked from inside a docker container without `/bin/sh` semantics? → No script may invoke `bash`, `sh`, `cmd`, or `powershell` to do logic that should be Python; subprocess calls must pass `shell=False` with an argv list

## Clarifications

### Session 2026-07-21

- Q1: Migration phasing → A: Incremental per-directory PRs (one PR per top-level in-scope scope; each PR removes its in-scope `.sh`/`.bat` on landing)
- Q2: Windows launcher UX for `Maven wrapper.py` → A: Exclude `mvnw / mvnw.cmd` from this migration entirely — both files stay as-is (already cross-platform)
- Q3: pytest declaration location → A: New top-level `scripts/requirements-dev.txt` (pytest pinned) + `scripts/run-python-tests.{sh,cmd}` runner that installs + runs pytest over all in-scope script dirs
- Q4: Windows CI verification for SC-003 → A: New scoped workflow `.github/workflows/python-build-scripts.yml` with `ubuntu-latest` + `windows-latest` matrix, path-filtered to in-scope script paths; Python-script tests ONLY (no Maven / full build)
- Q5: Behavioral fidelity for shell-isms → A: Best-effort functional equivalence + a `## Behavioral Notes` section per script that calls out any deviation from the shell original

## Requirements

### Functional Requirements

- **FR-001**: Every build-time `.sh` and `.bat` script listed in the in-scope paths above MUST be replaced by a single Python 3.9+ script (`.py`) at the same directory, named after the original script without the extension. `mvnw / mvnw.cmd` are NOT in scope (per Clarification Q2)
- **FR-001a**: Migration MUST be delivered as per-directory PRs (one PR per top-level in-scope scope, e.g. `scripts/`, `docker/`, `perc-distribution-tree/scripts/`, `ai-shared-develop/scripts/`). Each PR removes only its own in-scope `.sh`/`.bat` files on landing (per Clarification Q1)
- **FR-002**: Each Python script MUST accept the same CLI arguments as the original `.sh`/`.bat` (same flags, same positional args, same env var inputs)
- **FR-003**: Each Python script MUST be executable directly (`python3 <name>.py [args]`) and via the standard `python <name>.py [args]` invocation on Windows; a `#!/usr/bin/env python3` shebang MUST be present so `chmod +x` works on Unix-likes
- **FR-004**: After the Python equivalent ships, the original `.sh` AND `.bat` (when both exist) MUST be removed from the repo; if only one platform variant existed originally, only that variant is removed (the Python replacement covers the gap)
- **FR-005**: Each Python script MUST exit with a non-zero status on any failure path (exit 1 for usage, exit 2 for prerequisite/IO, exit >0 for logic failures — keep semantics consistent with the replaced scripts where they existed)
- **FR-006**: Each Python script MUST use only the Python standard library plus packages already vendored in the repo or already declared in a sibling `requirements.txt` (no new third-party deps in `pom.xml` or `package.json`)
- **FR-007**: Each Python script MUST handle all file/path I/O via `pathlib.Path` and `os.path`-free helpers; hardcoded `/` or `\\` separators in filesystem paths are forbidden (see root `AGENTS.md` Cross-Platform File I/O & Paths)
- **FR-008**: Each Python script MUST invoke external programs via `subprocess.run([...], shell=False, check=False)` with explicit timeout where appropriate; no `os.system`, no `bash -c`, no `cmd /c` wrappers for logic
- **FR-009**: Each Python script MUST have a colocated `test_<name>.py` pytest module that exercises at minimum: a happy-path invocation (exit 0), a failure-path invocation (non-zero exit, error message contains a recognizable substring), and a `--help` invocation (exit 0, usage printed)
- **FR-009a**: pytest MUST be declared in a new top-level `scripts/requirements-dev.txt` (pytest version pinned). A new `scripts/run-python-tests.sh` (Linux/macOS) and `scripts/run-python-tests.cmd` (Windows) runner MUST install the pinned deps and run `python3 -m pytest` (or `python -m pytest` on Windows) over all in-scope script directories (per Clarification Q3)
- **FR-009b**: When a Python rewrite deviates from shell-isms of the original (e.g. bash traps, signal handlers, process substitution, here-docs, globbing), the script MUST include a `## Behavioral Notes` section in its module docstring enumerating each deviation and the rationale (per Clarification Q5)
- **FR-010**: The colocated pytest tests MUST run without network access where possible (use fixtures from `scripts/test-fixtures/` already in tree) and MUST pass on Linux
- **FR-011**: All references in repo docs (`scripts/README.md`, `AGENTS.md`, skill `SKILL.md`, docker `README.md`, `quickstart.md`, `installation.md`) to old `.sh`/`.bat` names MUST be updated to the new Python entry points; legacy "Cross-platform: Windows users run the `.cmd` counterpart" notes MUST be deleted
- **FR-012**: Any Maven plugin execution, GitHub Actions step, or `kilocode` workflow invocation of an old `.sh`/`.bat` name MUST be updated to the new Python invocation
- **FR-012a**: A new GitHub Actions workflow `.github/workflows/python-build-scripts.yml` MUST be added. It MUST have an `ubuntu-latest` + `windows-latest` matrix, MUST be path-filtered to in-scope script paths, and MUST run `scripts/run-python-tests.{sh,cmd}` only (no Maven invocation, no full build). It MAY run on `pull_request` and on `push` to `development` (per Clarification Q4)
- **FR-013**: The following script categories MUST NOT be converted (explicit out-of-scope to protect customer deployments):
  - Anything under `system/release/installer/**`, `system/release/ShellScripts/**`, `system/installResources/**`, `system/Tools/**`
  - Anything under `system/Testing/**` install/setup scripts (test fixtures, not build)
  - `system/cms/content/applications/word_prj/signocx.bat` (Word plugin helper deployed with content app)
  - `system/src/test/resources/com/percussion/test/util/itemcreator/runRhythmyxItemCreator.{sh,bat}` (deployed test resource)
  - `modules/perc-jetty/src/main/jetty/StartJetty.{sh,bat}`, `StopJetty.{sh,bat}`, `service/install-jetty-service.{sh,bat}` (deployed with jetty distribution)
  - `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/*.bat` (deployed installer scripts)
  - `modules/perc-distribution-tree/src/main/jetty/**` runtime scripts (if any)
  - `modules/TableFactory/importData.{sh,bat}`, `exportData.{sh,bat}` (operate on installed customer tree)
  - `modules/patch-tools/install.{sh,bat}`, `uninstall.{sh,bat}` (operate on installed customer tree)
  - `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/**` (deployed root files)
  - `deliverytiersuite/delivery-tier-suite/p13n-ds/resource/derby/DatabaseShutdown.bat`, `DatabaseStartup.bat` (deployed Derby helpers)
  - `projects/sitemanage/src/test/resources/service/importSites.bat` (deployed test fixture)
- **FR-014**: A new top-level `scripts/README.md` section MUST enumerate which scripts are in/out of scope and link to the spec

### Key Entities

- **Build Script**: A `.sh`/`.bat` file under an in-scope path (per FR-013) whose replacement is a `.py` module with a `main()` entry point, argparse-driven CLI, and pytest coverage
- **Script Catalog Entry**: An entry in `scripts/README.md` (and equivalent module READMEs) describing purpose, usage, prereqs, and Python entry point
- **Colocated Test Module**: A `test_<script>.py` next to the script that exercises happy/failure/help paths

## Success Criteria

### Measurable Outcomes

- **SC-001**: After all per-directory PRs land, `git ls-files | grep -E '\.(sh|bat)$' | grep -v '<runtime-paths-from-FR-013>' | grep -v 'Maven wrapper\.(sh|bat)'` returns zero results on the development branch (mvnw / mvnw.cmd are out of scope per Clarification Q2)
- **SC-002**: 100% of in-scope build-time scripts have a Python equivalent whose pytest tests pass on Linux
- **SC-003**: The new `.github/workflows/python-build-scripts.yml` runs successfully on both `ubuntu-latest` and `windows-latest` runners, executing `scripts/run-python-tests.{sh,cmd}` (Python-script tests only — no Maven / full build), path-filtered to in-scope paths
- **SC-004**: `mvnw` and `mvnw.cmd` continue to exist on `development` and continue to behave exactly as before (regression check via existing `Maven wrapper.*` test surface if any; otherwise documented "no-change" assertion)
- **SC-005**: All in-scope `verify-*.{sh,bat}` scripts removed; their Python replacements emit byte-identical (modulo line endings) PASS/FAIL verdicts on the same inputs as documented in `scripts/README.md`
- **SC-006**: `git grep -E 'scripts/verify-[a-z-]+\.(sh|bat)' -- ':!*.ppkg' ':!docs/ai-generated/code-reviews/*' ':!docs/ai-generated/tasks/*/phase-*.sh'` returns zero matches (no surviving references in non-runtime code paths) — `Maven wrapper\.(sh|bat)` is explicitly exempt
- **SC-007**: No new warnings or failures introduced in the per-module `./mvnw clean install` runs against the touched modules (`rest`, `projects/sitemanage`, `modules/perc-distribution-tree`, `modules/perc-jetty`, `modules/ai-shared-develop`) — per root `AGENTS.md` Pre-PR Maven verification hard gate
- **SC-008**: `scripts/requirements-dev.txt` declares pytest with a pinned version; `scripts/run-python-tests.sh` and `scripts/run-python-tests.cmd` both run successfully from a clean clone (idempotent install + run)

## Assumptions

- Python 3.9+ is already required by the repo for `erlang-harvest-review-patterns` and is available on all developer and CI hosts (no Python bootstrap work in scope)
- pytest is available or installable via `python3 -m pip install pytest`; the migration will vendor a `scripts/requirements-dev.txt` if pytest is not already present in a sibling lock file (per Clarification Q3)
- The Erlang review skill pattern memory (under `docs/ai-generated/code-reviews/`) does not need migration; only the executable scripts change
- The `.specify/scripts/bash/**` self-hosted tooling used by the speckit workflow is also a candidate for migration but is owned by the speckit install; the spec defers that to a follow-up rather than scope-creep here
- `mvnw` and `mvnw.cmd` are NOT in scope of this spec; this spec does not touch them in any way (per Clarification Q2)

## Open Questions

- None blocking; the in-scope/out-of-scope split in FR-013 is the only judgment call and is fully enumerated above so review can challenge specific entries without re-litigating the policy

