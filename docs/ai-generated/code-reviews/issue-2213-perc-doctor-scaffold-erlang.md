# Erlang review: #2213 perc-doctor slice 1

**Branch:** `feat/issue-2213-perc-doctor-scaffold`  
**Scope:** new module `modules/perc-doctor` + reactor wire-up in root `pom.xml`  
**Recommendation:** approve  
**May commit/push:** yes  
**Gate:** pass

## Summary

Scaffold CMS Doctor CLI (`com.intsof.percussioncms.doctor`) with global `--install-root`, `--dry-run`, `-v` and command `clean-heap-dumps`. Allowlist is bare-name `*.hprof` only; inventory/delete stays under resolved install root via `InstallRootGuard`. Unit tests cover allowlist, dry-run vs apply, outside-root rejection, and CLI exit paths. README documents usage; later commands/API/packaging deferred.

## Cross-platform path checklist

- [x] No hardcoded OS separators in filesystem ops (`Path` / NIO only)
- [x] Tests use `@TempDir` + `Path.resolve` (no Unix-only absolute paths)
- [x] Install-root containment uses absolute normalize + `startsWith`
- [x] Help/examples show both Unix and Windows install roots as display text only
- [x] No `user.home` / machine-specific paths required for tests

## Issues

None (bugs). Minor notes (non-blocking):

- **Nit:** `Path.startsWith` is case-sensitive; Windows operators should pass a consistent case for `--install-root` (same pattern as `intsof-common-utilities` `PathsUnder`).
- **Nit:** symlink-to-outside-file under root deletes the symlink entry only (expected without `FOLLOW_LINKS`); document if ops ever need realpath checks.
- **Deferred by design:** fat-jar / `bin` packaging, backups/logs commands, HTTP API.

## Tests

`cd modules/perc-doctor && ../../mvnw clean install` — BUILD SUCCESS, Tests run: 16, Failures: 0.

## Memory patterns hit

- Cross-platform NIO paths for new file I/O
- Dry-run / apply behavioral split with fixture isolation
- Containment guard before destructive ops
