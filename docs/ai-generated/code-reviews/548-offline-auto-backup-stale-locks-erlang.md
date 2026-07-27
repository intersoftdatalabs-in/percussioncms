# Erlang review — fix/548-offline-auto-backup-stale-locks

**Date:** 2026-07-27  
**Scope:** Branch `fix/548-offline-auto-backup-stale-locks` vs `development` — Derby→H2 FR-018a offline auto-backup, stale lock cleanup, CMS/DTS running-instance install gates. SPA/login working tree excluded.  
**Memory patterns hit:** Installer offline gates; missing behavioral tests; non-portable paths; secrets in logs; false-green install when child still running.

## Summary

1. **FR-018a**: When CMS/DTS is offline, upgrade automatically performs product offline backup; stale Derby/H2 lock markers are cleared from the live tree and never archived into the backup.
2. **Running gates**: CMS install/upgrade runs `PSCheckRunningServer`; DTS install runs `PSCheckRunningDtsServer` (Production + Staging). Migrate Ant tasks also abort if the instance is running. DTS detection resolves `${http.port}` from `perc-catalina.properties` (literal port parse previously always failed open).

## Recommendation

**approve**

## Gate

| Check | Result |
|-------|--------|
| Bugs (logic / security) | None found after DTS placeholder fix |
| Behavioral unit tests for new logic | Present (offline backup + InstallUtilRunningServerTest) |
| Cross-platform path/file I/O | Clean (`Path` / `Files` for new DTS layout joins) |
| Secrets in logs/fixtures | Clean |
| May commit/push | **yes** |

## Cross-platform path checklist

- [x] No new `".../" +` filesystem path construction in new logic (`Path.of` / `resolve`)
- [x] Tests use `TempDir` + `Path.resolve`
- [x] Line-ending sensitive assertions N/A
- [x] Installer XML uses Ant `${install.dir}/…` path forms (URL/Ant convention)

## Issues

None (hard gate).

### Suggestions (non-blocking)

1. CMS `checkServerRunning` still uses string `File.separator` joins (pre-existing); not expanded in this change.
2. Optional Ant-task unit tests for `PSCheckRunningDtsServer` / `PSCheckRunningServer` throw path — covered at `InstallUtil` behavioral layer.

## Test evidence (pre-PR)

- `modules/utils`: `InstallUtilRunningServerTest` — pass
- `system`: migration unit suite (`PSEmbeddedRepositoryMigratorTest`, `PSRepositoryOfflineBackupTest`, related) — pass
- Standalone `clean install` / `install` on changed modules: `utils`, `perc-ant`, `system` (integrity skip only for local seal drift)
