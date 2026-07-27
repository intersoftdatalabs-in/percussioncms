# Erlang review — GH-1484–1487 Jetty startup WARN hygiene

**Date:** 2026-07-27  
**Scope:** Uncommitted `modules/perc-jetty` changes for issues #1484, #1485, #1486, #1487  
**Base:** `development` (`origin/development`)  
**Reviewer persona:** Erlang (independent of implementer)

## Summary

Packaging/config-only hygiene for CMS Jetty 12 startup console WARNs: single SLF4J
provider via `logging|default`, remove module `[exec]` fork drivers, migrate to
`ShutdownService` + SameSite cookie attribute, patch assembled `jetty.xml` named
Args, ship DigesterFactory W3C schema resources.

## Recommendation

**approve**

## Gate

| Check | Result |
|-------|--------|
| Bugs (functional / security) | None found |
| Behavioral tests for new non-trivial logic | Pass — config/packaging contracts covered by `StartupWarnHygieneTest` (same style as existing `PercLoggingLog4j2ConfigTest`) |
| Cross-platform path / file I/O | Pass — tests use `Path.of` / `Files`; line endings normalized; no OS-only path joins in product code |
| May commit/push | **yes** |

## Cross-platform path checklist

- [x] No new `".../" +` or `"...\\" +` filesystem path construction in Java
- [x] Path logic uses `Path` / `Files` in tests
- [x] Schema packaging uses Ant `basedir` + Maven paths (build-time only)
- [x] Tests do not assert Unix-only absolute path shapes
- [x] Line-ending sensitive assertions normalize `\r\n` → `\n`
- [x] Start/stop scripts retain Windows (`.bat`) counterparts; Linux StartJetty.sh already lacked server-side `STOP.PORT`

## Issues

_None (hard gate)._

### Notes (non-blocking)

1. **Structural tests:** Assertions read module/ini/XML text rather than a live
   `start.jar --dry-run`. Acceptable for this packaging module (matches GH-939
   log4j2 config tests). Optional follow-up: smoke `--list-modules` / `--dry-run`
   in CI if a Jetty home fixture becomes available.
2. **`jvm` still forks once:** Documented intentional residual via `jvm.ini`
   `--exec`. Does not reintroduce the smoke finding naming `[perc-logging, perc]`.
3. **Stop protocol:** Server uses `jetty.shutdown.*`; stop client keeps
   `-DSTOP.PORT`/`-DSTOP.KEY` with matching values — correct Jetty 12 split.

## Memory patterns hit

- Structural config tests OK when they *are* the packaging contract (not a
  substitute for runtime business logic)
- Cross-platform: Path/Files + CRLF normalize in tests
- Installer/start scripts: Windows + Unix pairs for required operator flows

## Evidence

- `cd modules/perc-jetty && ../../mvn-env.bat clean install`
- **BUILD SUCCESS** — Tests run: 39, Failures: 0, Errors: 0, Skipped: 0
