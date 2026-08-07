# Erlang review: feat(perc-doctor) diagnose/health (#2213)

**Branch:** `feat/issue-2213-diagnose-health`  
**Date:** 2026-08-06  
**Recommendation:** approve  
**Gate:** May commit/push: **yes**

## Summary

Adds read-only `diagnose` / `health` checklist to `modules/perc-doctor` with unit tests, CLI help, and operator docs. Does not re-touch clean-* commands or HTTP API apply paths.

## Scope

- `DiagnoseReport`, `DiagnoseCommand` (new)
- `DoctorCli` dispatch/help/print
- `DiagnoseCommandTest`, `DoctorCliTest`, packaging doc assertion
- README + operator-install-guide

Cross-platform path review: all layout/config/log probes use `InstallRootGuard.resolveRelativeUnderRoot` (forward-slash relative segments + `Path.resolve`) and re-check `isUnderInstallRoot`. No hardcoded user homes, no string path joins with `\` or `/` for filesystem ops. Free disk via `Files.getFileStore`. Java version from system properties only.

## Issues

None at `bug` severity.

- **nit:** `DiagnoseCommand.execute` declares `throws IOException` though current body does not throw it (consistent with clean commands; acceptable).

## Tests

Module `mvnw clean install`: all Surefire green including new `DiagnoseCommandTest` (9) and expanded `DoctorCliTest` (16). Behavioral coverage: report shape, path containment, no mutations, bare vs full tree, CLI alias/exit codes, help text.
