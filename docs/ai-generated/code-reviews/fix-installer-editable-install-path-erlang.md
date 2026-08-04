# Erlang review: fix/installer-editable-install-path

**Date:** 2026-08-04  
**Reviewer:** Erlang (pre-commit / pre-PR)  
**Scope:** Uncommitted changes on `fix/installer-editable-install-path` vs `origin/main`  
**Intent:** Interactive CMS installer must show last-install directory as an editable default, not force it and skip the path prompt.

## Summary

`InstallerUserSettings.applyDefaults` correctly fills `cms.install.directory` for silent reuse. `InteractiveInstallWizard.runPhase1` previously treated a non-null path after defaults as “already supplied,” so interactive runs never prompted when last-install settings existed. The fix tracks CLI vs saved path (`pathFromCli`) and always prompts in interactive mode when the CLI path is absent, using the saved path only as `promptForInstallPath` default. Silent/non-TTY behavior is preserved.

## Scope

|            Item            |                                                   Detail                                                    |
|----------------------------|-------------------------------------------------------------------------------------------------------------|
| Files                      | `InteractiveInstallWizard.java`, `InstallerUserSettings.java` (Javadoc), two unit test classes              |
| Prior report               | none for this topic                                                                                         |
| Memory patterns hit        | Installer interactive vs silent gates; missing behavioral tests for non-trivial wizard logic                |
| Cross-platform path review | Clean — uses `Path` / `toAbsolutePath().normalize()`; no hardcoded separators; tests use `TempDir` / `Path` |

## Recommendation

**approve**

## Gate

|               Check                |                                 Result                                  |
|------------------------------------|-------------------------------------------------------------------------|
| Bugs                               | none                                                                    |
| Behavioral tests for changed logic | present (accept default, change path, silent still applies saved path)  |
| Change-class closure               | adequate for installer wizard path resolution (unit tests mirror peers) |
| Non-portable I/O                   | none                                                                    |
| **May commit/push**                | **yes**                                                                 |

## Issues

_None blocking._

### suggestion (non-blocking)

None material. Optional future: if other last-install keys are mis-treated as CLI overrides in interactive DB collection, audit separately (out of scope; upgrade path intentionally skips DB prompts).

## Test evidence (session)

```text
mvnw -Dtest=InteractiveInstallWizardTest,InstallerUserSettingsTest test
Tests run: 34, Failures: 0, Errors: 0
BUILD SUCCESS
```

