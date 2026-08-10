# Erlang review: issue #1937 tools.jar residual cleanup

**Branch:** `fix/issue-1937-tools-jar-residual`  
**Scope:** residual JDK `tools.jar` references (scripts + LAX test fixtures)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** none (script/fixture cleanup; no new I/O logic)

## Summary

Removes remaining product/test references to JDK `lib/tools.jar` that are not owned by open Jetty/Procrun work (#1804 / PR #1938). No production Java logic changed.

## Changes reviewed

|                            Path                             |                                    Change                                    |
|-------------------------------------------------------------|------------------------------------------------------------------------------|
| `system/Tools/RxFix/build.bat`                              | Dropped `%JAVA_SDK_HOME%\lib\tools.jar` from classpath; REM notes JDK 9+/21  |
| `system/installResources/install.sh`                        | Removed commented `#CLASSPATH=.../tools.jar` line; note left                 |
| `modules/utils/**/PercussionServer*.lax` (4)                | **Deleted** (review: legacy InstallAnywhere fixtures unused)                 |
| `modules/perc-ant/**/mockinstall/PercussionServer*.lax` (2) | **Deleted** (same)                                                           |
| `TestUpdateDTSConfiguration.java`                           | Stopped loading/copying LAX into temp mock root (DTS task does not read LAX) |
| `specs/.../derby-surface-inventory.md`                      | Removed inventory rows for deleted LAX fixtures                              |

## Intentionally not changed

- `modules/perc-jetty/.../install-jetty-service.bat` — owned by #1804 / PR #1938
- Tomcat `catalina.properties` package-exclusion `tools.jar` entries
- Vendored Tomcat docs under `system/release/tomcat/`

## Issues

None (bugs / missing behavioral tests / non-portable paths).

### Notes

- Review follow-up: deleted all six InstallAnywhere `PercussionServer*.lax` test fixtures rather than editing classpath lines.
- `TestUpdateDTSConfiguration` no longer stages LAX; DTS config task does not read them.
- Production upgrade code may still *look for* LAX at install root (`PSUpdateJettyConfigFromJBoss`) — only test fixtures were removed.
- Scripts: no new path construction; bat REM / sh comment only.
- Cross-platform path checklist: N/A for new filesystem I/O (none added).

## Verification

- `cd modules/utils && ../../mvnw clean install` — BUILD SUCCESS
- `cd modules/perc-ant && ../../mvnw clean install` — BUILD SUCCESS
- `mvnw -pl modules/utils,modules/perc-ant spotless:apply` then `spotless:check` — SUCCESS
- Root `spotless:check` still fails on unrelated baseline docs debt; out-of-scope hits restored, not committed

