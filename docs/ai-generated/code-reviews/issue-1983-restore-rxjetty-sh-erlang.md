# Erlang review: issue #1983 restore `defaults/bin/rxjetty.sh`

**Date:** 2026-08-05  
**Branch:** `fix/issue-1983-restore-rxjetty-sh`  
**Scope:** GH-1983 — restore Jetty start-helper template for Linux service install  
**Recommendation:** **approve**  
**May commit/push:** yes  
**Gate:** pass

## Summary

Restores the pre-Jetty-12 `rxjetty.sh` start-helper under
`modules/perc-jetty/src/main/jetty/defaults/bin/rxjetty.sh` so
`install-jetty-service.sh` can sed-substitute `${rxjetty_service}` into
`/etc/init.d/<ServiceName>` for both systemd and `--initd` paths. Assembly
copies `src/main/jetty/**` into the distribution; zip verified to include
`defaults/bin/rxjetty.sh`. Structural tests cover template markers and install
script contract. README-systemd documents the template path. Root `.gitignore`
`bin/` exception is required so the packaged path is versioned.

## Cross-platform path checklist

- [x] No new hard-coded OS separators for local filesystem joins in Java
- [x] Tests use `Path.of(...)` components
- [x] Shell template is Unix-only by product design (Linux service install); LF endings
- [x] Packaging path `defaults/bin/rxjetty.sh` matches install script contract

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

### Nits (non-blocking)

- Historical template retains shell-quoting caveats (`su -c` with expanded
  `RUN_CMD`); same as pre-a82b983bce contract — out of scope for restore.
- `UID -eq 0` alignment for start-stop-daemon when root (was historical `UID -eq 2`)
  matches Jetty 12 upstream and unit docs; nohup path remains fallback.

## Tests / build evidence

- `cd modules/perc-jetty && ../../mvnw clean install` → BUILD SUCCESS  
  Tests run: 58, Failures: 0, Errors: 0, Skipped: 7  
  New: `RxJettyStartHelperTemplateTest` (4), extended `InstallJettyServiceScriptTest` (7)
- Root `./mvnw spotless:apply` then `./mvnw spotless:check` → SUCCESS  
  (out-of-scope Spotless hits left uncommitted)

## Memory patterns hit

- Packaging companions: assert both source template and assembly output when possible
- Installer contract structural tests (string markers) over live root systemctl

