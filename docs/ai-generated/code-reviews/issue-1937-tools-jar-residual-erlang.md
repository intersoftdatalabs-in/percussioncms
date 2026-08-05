# Erlang review: issue #1937 tools.jar residual cleanup

**Branch:** `fix/issue-1937-tools-jar-residual`  
**Scope:** residual JDK `tools.jar` references (scripts + LAX test fixtures)  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** none (script/fixture cleanup; no new I/O logic)

## Summary

Removes remaining product/test references to JDK `lib/tools.jar` that are not owned by open Jetty/Procrun work (#1804 / PR #1938). No production Java logic changed.

## Changes reviewed

| Path | Change |
|------|--------|
| `system/Tools/RxFix/build.bat` | Dropped `%JAVA_SDK_HOME%\lib\tools.jar` from classpath; REM notes JDK 9+/21 |
| `system/installResources/install.sh` | Removed commented `#CLASSPATH=.../tools.jar` line; note left |
| `modules/utils/**/PercussionServer*.lax` (4) | Dropped `JRE/lib/tools.jar` from `lax.class.path` |
| `modules/perc-ant/**/mockinstall/PercussionServer*.lax` (2) | Same |
| `specs/.../derby-surface-inventory.md` | Inventory lines updated to match fixtures |
| `PSJacksonXmlSerializationHelperTest.java` | Removed exact duplicate `@Test` method (pre-existing `main` testCompile break) |

## Intentionally not changed

- `modules/perc-jetty/.../install-jetty-service.bat` — owned by #1804 / PR #1938
- Tomcat `catalina.properties` package-exclusion `tools.jar` entries
- Vendored Tomcat docs under `system/release/tomcat/`

## Issues

None (bugs / missing behavioral tests / non-portable paths).

### Notes

- Consuming tests (`TestUpdateDTSConfiguration` and peers) load LAX for JVM options / file presence; no assertion on `lax.class.path` or `tools.jar`.
- Existing suite verifies fixture parse after classpath edit: utils 265 tests, perc-ant 38 tests, 0 failures.
- Scripts: no new path construction; bat REM / sh comment only.
- Cross-platform path checklist: N/A for new filesystem I/O (none added).

## Verification

- `cd modules/utils && ../../mvnw clean install` — BUILD SUCCESS
- `cd modules/perc-ant && ../../mvnw clean install` — BUILD SUCCESS
- `mvnw -pl modules/utils,modules/perc-ant spotless:apply` then `spotless:check` — SUCCESS
- Root `spotless:check` still fails on unrelated baseline docs debt; out-of-scope hits restored, not committed
