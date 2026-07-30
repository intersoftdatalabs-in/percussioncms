# Erlang review: #1516 matrix-install-smoke DB teardown

**Date:** 2026-07-27  
**Branch:** `feature/matrix-install-smoke-db-teardown`  
**Base:** `origin/development`  
**Reviewer persona:** Erlang (pre-commit / pre-PR)

## Summary

The harness now tracks which external compose DBs it started vs reused, and tears
down only matrix-owned services by default after the cell loop (success or fail).
CLI opts (`--keep`, `--keep-db`, `--stop-db`) match issue #1516. Pure decision
logic is unit-tested; docker CLI remains subprocess argv lists (spec 994 style).

## Scope

|                     Path                      |                                  Change                                   |
|-----------------------------------------------|---------------------------------------------------------------------------|
| `docker/scripts/matrix-install-smoke.py`      | Ownership, `select_dbs_to_stop`, `stop_external_dbs`, CLI, `main` finally |
| `docker/scripts/test_matrix_install_smoke.py` | Policy + dry-run + mutex tests                                            |
| `docker/README.md`                            | Teardown policy table and examples                                        |

- Uncommitted diff vs `HEAD` (no prior commits on branch).
- Prior reports: `1500-matrix-smoke-*.md` (parent harness); no prior #1516 report.
- Memory patterns: portable Python + docker CLI; no shell-only entry; ownership
  must not destroy operator-owned resources by default.

## Recommendation

**approve**

## Gate

|                Check                |                                         Result                                         |
|-------------------------------------|----------------------------------------------------------------------------------------|
| Bugs                                | None found                                                                             |
| Behavioral unit tests for new logic | Present (`select_dbs_to_stop`, ownership mapping, CLI mutex, dry-run PG path)          |
| Cross-platform path / file I/O      | Clean — `pathlib.Path`, `str(compose_file)` to docker only; no hardcoded FS separators |
| May commit/push                     | **yes**                                                                                |

Cross-platform path review: no new filesystem path string joins; temp dirs in
tests use `tempfile` + `Path`; docker network/compose args are logical names.

## Issues

### suggestion

1. **`select_dbs_to_stop` docstring vs call site**  
   Docstring describes `used_external` as “selected for the matrix”; `main`
   passes **engaged** DBs only (those for which `start_db` ran). Behavior is
   correct and safer; consider renaming the parameter to `engaged_external` in
   a follow-up for clarity. Non-blocking.

### nit

1. **`start_db(..., repo_root)`** still unused (pre-existing). Harmless.

2. **`--keep` + `--stop-db`** allowed together; policy prefers `--keep`. Fine;
   optional help note later.

## Verification

```text
python -m pytest docker/scripts/test_matrix_install_smoke.py -v
# 25 passed
```

Maven clean install: **N/A** (Python/docs only; no module `pom.xml` / sources).
