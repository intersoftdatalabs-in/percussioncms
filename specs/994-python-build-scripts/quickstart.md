# Quickstart: Cross-Platform Python Build Scripts

**Branch**: `994-python-build-scripts` | **Date**: 2026-07-21 | **Spec**: [spec.md](spec.md)

End-to-end validation guide. The scenarios below prove the feature works: from a clean clone, on both Linux/macOS and Windows, you can install pytest, run all in-scope tests, and see them all pass. The scenarios are scoped per-directory so a per-directory PR can run its slice of this guide and pass before merging.

---

## Prerequisites

| Prereq |          Linux/macOS          |            Windows             |                                                Notes                                                |
|--------|-------------------------------|--------------------------------|-----------------------------------------------------------------------------------------------------|
| Git    | any 2.x+                      | any 2.x+                       | Clone the repo                                                                                      |
| Python | 3.9+ (3.11 recommended)       | 3.9+ (3.11 recommended)        | `python3 --version` / `python --version`                                                            |
| pip    | bundled with Python           | bundled with Python            | `python3 -m pip --version`                                                                          |
| JDK 21 | required by `mvn-env.sh` only | required by `mvn-env.bat` only | NOT required for this feature — `python-build-scripts.yml` does not invoke Maven (Clarification Q4) |

No Docker, no `gh`, no Maven, no `jq`. The Python-script test suite is self-contained; only scripts that themselves call out to `gh`/`docker` are gated behind `pytest.importorskip` / network markers (FR-010).

---

## Scenario A — Foundation validation (must pass before any per-directory PR merges)

### A.1 — Install pytest from the pinned manifest (Linux/macOS)

```bash
git clone https://github.com/intersoftdatalabs-in/percussioncms.git
cd percussioncms
git checkout 994-python-build-scripts
python3 -m pip install -r scripts/requirements-dev.txt
```

**Expected**: pip reports `Successfully installed pytest-<version>` (and any transitive test deps). Exit 0.

### A.2 — Run the test runner (Linux/macOS)

```bash
bash scripts/run-python-tests.sh
```

**Expected**:
- Runner prints `=== Installing pytest ===` then `=== Running pytest ===`
- pytest discovers the in-scope test files (initially zero or minimal until per-directory PRs land)
- Exit code `0` (all pass — green)

### A.3 — Install pytest + run runner (Windows, PowerShell)

```powershell
git clone https://github.com/intersoftdatalabs-in/percussioncms.git
cd percussioncms
git checkout 994-python-build-scripts
python -m pip install -r scripts\requirements-dev.txt
scripts\run-python-tests.cmd
```

**Expected**: same as A.2 — exit code `0`.

### A.4 — CI workflow is path-filtered and runs both runners

Push the branch (or open a draft PR); observe `.github/workflows/python-build-scripts.yml` triggering on the PR. The Actions tab shows:

|          Job           |      Runner      |                                     Steps                                     | Expected duration |
|------------------------|------------------|-------------------------------------------------------------------------------|-------------------|
| `python-build-scripts` | `ubuntu-latest`  | checkout → setup-python@v5 → pip install → `bash scripts/run-python-tests.sh` | < 3 min           |
| `python-build-scripts` | `windows-latest` | checkout → setup-python@v5 → pip install → `scripts\run-python-tests.cmd`     | < 5 min           |

**Expected**: both runners green; the workflow does NOT run any Java/Maven step.

---

## Scenario B — `scripts/` directory PR validation (Scope 1)

This scenario runs once per-directory PR; the `scripts/` PR exercises the root-level verify/audit/harvest/hot-deploy scripts.

### B.1 — Per-script help paths

```bash
# Linux/macOS — verify --help works for every converted script
for s in scripts/install-cms-dev.py scripts/verify-triage-inventory.py scripts/verify-valid-fixes.py scripts/verify-suppressions.py scripts/verify-distribution-archive.py scripts/verify-pr-review-resolution.py scripts/verify-no-finder-jsp-references.py scripts/verify-no-jqplot-vendor-refs.py scripts/verify-codeql-analyzer-of-record.py; do
  echo "--- $s ---"
  python3 "$s" --help
done
```

**Expected**: each command exits `0` and prints `Usage:` block.

### B.2 — Verify triage inventory (offline, uses fixture)

```bash
python3 scripts/verify-triage-inventory.py \
  --triage scripts/test-fixtures/triage-good.md \
  --alerts scripts/test-fixtures/alerts-good.md
```

**Expected**: exit `0`; stdout `OK: triage inventory valid`.

Now flip to the bad fixture:

```bash
python3 scripts/verify-triage-inventory.py \
  --triage scripts/test-fixtures/triage-bad.md \
  --alerts scripts/test-fixtures/alerts-bad.md
```

**Expected**: exit non-zero; stderr contains `Empty notes` or `unknown module_owner` substring (per FR-009 failure-path test).

### B.3 — Erlang harvest (offline, with fixture)

```bash
# Already Python today; just confirm CI gates it
python3 -m pytest scripts/test_erlang_harvest_review_patterns.py -v
```

**Expected**: all pytest cases pass.

### B.4 — Run the directory's pytest suite

```bash
python3 -m pytest scripts/ -v
```

**Expected**: every `test_*.py` under `scripts/` passes; coverage report (if generated) covers at least happy / failure / help paths per script (FR-009).

### B.5 — Windows re-run

```powershell
python -m pytest scripts\ -v
```

**Expected**: identical pass/fail to B.4 (modulo line endings in stdout).

### B.6 — Negative: confirm no `.sh`/`.bat` remains under `scripts/`

```bash
find scripts/ -type f \( -name '*.sh' -o -name '*.bat' \) -not -path 'scripts/test-fixtures/*'
```

**Expected**: empty output (no in-scope scripts left behind). Compare against the FR-013 out-of-scope list — `scripts/test-fixtures/` is not in-scope; no other carve-outs apply.

---

## Scenario C — `docker/` directory PR validation (Scope 2)

### C.1 — Docker tooling help paths

```bash
for s in docker/scripts/hot-deploy-jar.py docker/scripts/perc-devctl.py docker/entrypoint/install-update.py; do
  python3 "$s" --help
done
```

**Expected**: each exits `0`; `perc-devctl.py` lists its subcommands (`install`, `up`, `down`, `status`, `verify`, `deploy-jar`, etc.).

### C.2 — `perc-devctl.py` subcommand dispatch (offline stub test)

The `perc-devctl.py` unit tests stub out the actual `docker compose` calls. The pytest suite under `docker/scripts/test_perc_devctl.py` exercises each subcommand's argparse path and exits-0/exits-non-zero semantics.

```bash
python3 -m pytest docker/scripts/ -v
```

**Expected**: all tests pass.

### C.3 — Windows re-run

```powershell
python -m pytest docker\scripts\ -v
```

**Expected**: identical pass/fail.

---

## Scenario D — `modules/perc-distribution-tree/scripts/` PR validation (Scope 3)

### D.1 — Build verification helpers

```bash
python3 modules/perc-distribution-tree/scripts/verify-jdbc-drivers.py --help
python3 modules/perc-distribution-tree/scripts/check-no-glob-deletes.py --help
```

**Expected**: each exits `0`.

### D.2 — `api-update.py` consolidated helper

```bash
python3 modules/perc-distribution-tree/scripts/api-update.py --help
python3 modules/perc-distribution-tree/scripts/api-update.py --module webui --skip-tests --no-restart --dry-run
python3 modules/perc-distribution-tree/scripts/update-tinymce.py --help
```

**Expected**: each exits `0` (the `--dry-run` for `api-update` exits `0` without invoking Maven; without `--dry-run`, the script would invoke Maven and is NOT exercised by the pytest suite — that path is gated by a `@pytest.mark.requires_mvn` marker and skipped in CI).

### D.3 — Negative: confirm no `.bat` APIUpdate scripts remain

```bash
find modules/perc-distribution-tree -maxdepth 2 -type f -name 'APIUpdate-*.bat' -o -name 'UpdateTinyMCE.bat'
```

**Expected**: empty output.

---

## Scenario E — `modules/ai-shared-develop/scripts/` PR validation (Scope 4)

### E.1 — AI dev tooling

```bash
for s in modules/ai-shared-develop/scripts/sign-ai-resources.py \
         modules/ai-shared-develop/scripts/verify-signatures-hook.py \
         modules/ai-shared-develop/scripts/build-integrity-check.py; do
  python3 "$s" --help
done
```

**Expected**: each exits `0`.

### E.2 — Skill scripts

```bash
for s in modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/scripts/*.py; do
  python3 "$s" --help
done
```

**Expected**: each exits `0` and lists its purpose + subcommands.

### E.3 — Pytest coverage

```bash
python3 -m pytest modules/ai-shared-develop/scripts/ modules/ai-shared-develop/src/main/resources/skills/ -v
```

**Expected**: all pass on Linux; same on Windows.

---

## Scenario F — End-to-end SC-001..SC-008 verification (after all per-directory PRs merge)

Once every per-directory PR has landed on `development`, the following checks prove all success criteria:

```bash
# SC-001: zero in-scope .sh/.bat remain (mvn-env.{sh,bat} exempt)
git ls-files | grep -E '\.(sh|bat)$' \
  | grep -v 'system/release/installer/' \
  | grep -v 'system/release/ShellScripts/' \
  | grep -v 'system/installResources/' \
  | grep -v 'system/Tools/' \
  | grep -v 'system/Testing/' \
  | grep -v 'system/cms/content/applications/word_prj/signocx.bat' \
  | grep -v 'system/src/test/resources/com/percussion/test/util/itemcreator/runRhythmyxItemCreator' \
  | grep -v 'modules/perc-jetty/src/main/jetty/' \
  | grep -v 'modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/' \
  | grep -v 'modules/TableFactory/' \
  | grep -v 'modules/patch-tools/' \
  | grep -v 'deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/' \
  | grep -v 'deliverytiersuite/delivery-tier-suite/p13n-ds/resource/derby/' \
  | grep -v 'projects/sitemanage/src/test/resources/service/importSites.bat' \
  | grep -v 'mvn-env\.\(sh\|bat\)' \
  | grep -v 'docs/ai-generated/tasks/#000-webui-src-layout/' \
  | grep -v '\.specify/scripts/bash/' \
  | grep -v 'modules/ai-shared-develop/src/main/resources/skills/.*/scripts/'  # per R3 archived
```

**Expected**: empty output.

```bash
# SC-002: 100% of in-scope scripts have a Python equivalent with passing pytest on Linux
bash scripts/run-python-tests.sh
```

**Expected**: exit `0`; all in-scope `test_*.py` pass.

```bash
# SC-003: GH Actions matrix passes on both runners
gh run list --workflow=python-build-scripts.yml --limit=5 --json conclusion,status
```

**Expected**: most recent run on `development` is `success` for both `ubuntu-latest` and `windows-latest`.

```bash
# SC-004: mvn-env.{sh,bat} still exist and unchanged
test -x mvn-env.sh && test -f mvn-env.bat && head -5 mvn-env.sh && head -3 mvn-env.bat
```

**Expected**: both files present; first 3-5 lines unchanged from the pre-spec SHA.

```bash
# SC-005: verify scripts emit identical verdicts to original
diff <(python3 scripts/verify-triage-inventory.py --triage scripts/test-fixtures/triage-good.md 2>&1) \
     <(bash scripts/verify-triage-inventory.sh.LEGACY  --triage scripts/test-fixtures/triage-good.md 2>&1)
```

(`scripts/verify-triage-inventory.sh.LEGACY` is a one-shot copy kept aside for this exact diff — see `tasks.md`.)

**Expected**: output differs only in line endings (`\r\n` vs `\n`).

```bash
# SC-006: zero surviving doc references
git grep -E 'scripts/verify-[a-z-]+\.(sh|bat)' -- ':!*.ppkg' ':!docs/ai-generated/code-reviews/*' ':!docs/ai-generated/tasks/*/phase-*.sh'
```

**Expected**: empty output.

```bash
# SC-007: no new Maven warnings
cd rest && ../mvn-env.sh clean install && cd ..
cd projects/sitemanage && ../../mvn-env.sh clean install && cd ../..
cd modules/perc-distribution-tree && ../../mvn-env.sh clean install && cd ../..
cd modules/perc-jetty && ../../mvn-env.sh clean install && cd ../..
cd modules/ai-shared-develop && ../../mvn-env.sh clean install && cd ../..
```

**Expected**: each module's `BUILD SUCCESS` with `Tests run: N, Failures: 0`; no NEW warnings vs the pre-spec baseline.

```bash
# SC-008: requirements-dev.txt + runner exist and run idempotently
test -f scripts/requirements-dev.txt && test -x scripts/run-python-tests.sh && test -f scripts/run-python-tests.cmd
bash scripts/run-python-tests.sh --skip-install   # idempotent re-run
```

**Expected**: exit `0`; the `--skip-install` re-run completes the pytest phase without re-installing.

---

## Troubleshooting

|                                     Symptom                                      |              Likely cause              |                                                       Fix                                                       |
|----------------------------------------------------------------------------------|----------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| `python3: command not found`                                                     | Python not installed (Linux/macOS)     | Install via OS package manager (`brew install python@3.11`, `apt install python3.11`)                           |
| `python: command not found`                                                      | Python not on PATH (Windows)           | Install Python 3.11 from python.org; check "Add to PATH"                                                        |
| `pip install` fails with `externally-managed-environment`                        | PEP 668 enforced (newer Linux distros) | Use a venv: `python3 -m venv .venv && source .venv/bin/activate && pip install -r scripts/requirements-dev.txt` |
| `pytest` not found after install                                                 | Stale `pip` cache                      | `python3 -m pip install --upgrade -r scripts/requirements-dev.txt`                                              |
| Windows runner step fails with `python is not recognized`                        | PATH issue on `windows-latest`         | The workflow uses `actions/setup-python@v5` which sets PATH; confirm `python-version: '3.11'` is set (R5)       |
| `scripts/run-python-tests.cmd` exits with `Access is denied`                     | File association issue                 | Run from cmd.exe or PowerShell; do not double-click                                                             |
| `find_in_scope` returns hits in `docs/ai-generated/tasks/#000-webui-src-layout/` | R3 exception not applied               | Confirm with maintainer; either delete the dir or document the carve-out                                        |

---

## Validation matrix summary

|             Scenario             | Linux | Windows |                   CI gate                    |                            Spec SC                             |
|----------------------------------|-------|---------|----------------------------------------------|----------------------------------------------------------------|
| A.1 — Install pytest             | ✓     | ✓       | N/A                                          | SC-008                                                         |
| A.2/A.3 — Run runner             | ✓     | ✓       | ✓ (`ubuntu-latest`, `windows-latest` matrix) | SC-003, SC-008                                                 |
| A.4 — CI workflow                | ✓     | ✓       | ✓                                            | SC-003                                                         |
| B — `scripts/` PR                | ✓     | ✓       | ✓                                            | SC-002, SC-005, SC-006                                         |
| C — `docker/` PR                 | ✓     | ✓       | ✓                                            | SC-002                                                         |
| D — `perc-distribution-tree/` PR | ✓     | ✓       | ✓                                            | SC-002                                                         |
| E — `ai-shared-develop/` PR      | ✓     | ✓       | ✓                                            | SC-002                                                         |
| F — End-to-end                   | ✓     | ✓       | ✓                                            | SC-001, SC-002, SC-003, SC-004, SC-005, SC-006, SC-007, SC-008 |

A per-directory PR needs only the matching lettered scenario (B/C/D/E) to pass before merge; Scenario A is the foundation PR; Scenario F is the final post-merge verification.
