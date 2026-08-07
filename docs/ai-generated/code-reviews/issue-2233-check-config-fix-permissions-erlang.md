# Erlang review — issue #2233 perc-doctor check-config / fix-permissions

**Branch:** `feat/issue-2233-check-config-fix-permissions`  
**Scope:** `modules/perc-doctor` CLI commands + tests + operator docs  
**Date:** 2026-08-07  
**Reviewer persona:** Erlang (independent pre-commit)

## Summary

Adds two peer CLI commands to `perc-doctor`:

1. **`check-config`** — read-only value/misconfig checklist for documented `server.properties` and `rxrepository.properties` (beyond presence-only checks used by open diagnose PR #2231).
2. **`fix-permissions`** — dry-run-first allowlisted mode/access report; POSIX owner-execute on `bin/perc-doctor`, owner rwx on known log dirs, owner-read on key configs. Windows reports access only (no ACL rewrite, no shell).

Wiring mirrors `clean-logs`: global `--install-root` / `--dry-run` / `-v`, `DoctorCli` dispatch, help/examples, packaging doc coverage. Unit tests cover containment, non-mutation, misconfig FAIL/WARN paths, CLI exit codes, and OS-conditioned POSIX fixes.

## Recommendation

**approve**

## Gate

| Gate | Result |
|------|--------|
| Bugs | none found |
| Behavioral unit tests for new logic | yes (`CheckConfigCommandTest` 11, `FixPermissionsCommandTest` 8/2 skipped on Windows, CLI coverage) |
| Portable path / file I/O | pass — `java.nio.file.Path` / `Files` only; relative segments via `InstallRootGuard.resolveRelativeUnderRoot`; no hardcoded user homes; Windows vs POSIX branched with attribute-view probe |
| May commit/push | **yes** |

## Cross-platform path checklist

- [x] No `C:\Users\...` / `/home/...` hardcoding in code or docs (generic `/opt/Percussion`, `C:\Percussion`)
- [x] Relative config/log/bin paths use forward-slash segment lists resolved with `Path` APIs
- [x] Containment re-checked before any mode mutation
- [x] POSIX-only APIs guarded (`supportedFileAttributeViews().contains("posix")` + try/catch `UnsupportedOperationException`)
- [x] Tests use `@TempDir` and `Path.resolve` (no separator literals that break OS)

## Issues

None (blocking).

### Notes (non-blocking)

- Admin HTTP API not wired for these two commands (CLI-first slice; clean-* API remains). Follow-up if product wants parity.
- Weak-password token list is intentionally conservative (WARN only; never prints secrets).
- Does not share types with open #2231 `DiagnoseReport` (independent checklist report) to avoid thrash.

## Memory patterns hit

- Install-root containment for doctor I/O
- Dry-run / report-first ops commands
- Prefer `Files` / `Path` over `File` and shell
