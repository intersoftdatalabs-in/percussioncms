# Erlang review — issue #2232 perc-doctor clean-temp

**Date:** 2026-08-07  
**Branch:** `feat/issue-2232-clean-temp`  
**Scope:** uncommitted / pre-PR changes for `clean-temp` under `modules/perc-doctor`  
**Parent:** #2213

## Summary

Adds CLI/API command `clean-temp` that inventories and optionally deletes regular files under allowlisted install temp/work directories (`temp`, `jetty/base/work`, `Deployment/Server/temp`, `Deployment/Server/work`). Mirrors `CleanLogsCommand` scoped-walk pattern with `InstallRootGuard` containment, dry-run-first, TOCTOU re-check before delete, and retention of allowlisted root directories. Unit tests cover dry-run, apply, outside-root exclusion, missing dirs, CLI wiring, and API dry-run default. README + operator guide document the allowlist.

## Recommendation

**approve**

## Gate

|                Check                |                                                                               Result                                                                               |
|-------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Bugs                                | none found                                                                                                                                                         |
| Behavioral unit tests for new logic | yes (`CleanTempCommandTest` 9, CLI + API + InstallRootGuard coverage)                                                                                              |
| Portable path / file I/O            | pass — `java.nio.file.Path` / `Files` only; forward-slash relative allowlist; `resolveRelativeUnderRoot` + case-insensitive Windows containment via existing guard |
| Change-class companions             | CLI + command class + guard allowlist + API dispatch + docs + tests (same class as clean-logs)                                                                     |
| May commit/push                     | **yes**                                                                                                                                                            |

## Cross-platform path checklist

- [x] No hardcoded user homes / drive letters in product code
- [x] Relative allowlist uses `/` segments resolved via `resolveRelativeUnderRoot`
- [x] Containment uses `InstallRootGuard` (Windows case fold already present)
- [x] Tests use `@TempDir` and `Path.resolve` (portable)

## Issues

None (approve).

## Memory patterns hit

- Scoped clean commands: allowlisted dirs only, no user globs
- Dry-run never mutates; re-check containment before delete
- Document allowlist in README / operator guide

## Verification

```text
cd modules/perc-doctor && ../../mvnw clean install
CleanTempCommandTest: 9 tests, 0 failures
DoctorCliTest: 14 tests, 0 failures
InstallRootGuardTest: 13 tests, 0 failures
DoctorApiServiceTest: 9 tests, 0 failures
Module suite green
```

