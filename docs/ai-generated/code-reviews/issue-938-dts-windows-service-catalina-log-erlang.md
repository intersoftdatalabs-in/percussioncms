# Erlang review: issue #938 DTS Windows service catalina.log

**Date:** 2026-08-07  
**Branch:** `fix/issue-938-dts-windows-service-catalina-log`  
**Reviewer persona:** Erlang (pre-commit gate)  
**Recommendation:** approve  
**Gate:** May commit/push: **yes**

## Summary

Windows DTS Procrun installers (`DTSProductionService.bat`, `DTSStagingService.bat`)
now redirect StdOutput/StdError to `Deployment/Server/logs/catalina.log` (parity with
Linux `CATALINA_OUT`) and wire Log4j JUL + `log4j.configurationFile` correctly for
service mode (no `setenv.bat`). Packaging/script tests and operator docs added.
CMS Jetty logging untouched.

## Scope

|                           Path                            |          Change class           |
|-----------------------------------------------------------|---------------------------------|
| `delivery-tier-distribution/.../DTSProductionService.bat` | Windows service installer       |
| `delivery-tier-distribution/.../DTSStagingService.bat`    | Windows service installer       |
| `.../DtsWindowsServiceCatalinaLogTest.java`               | Packaging/script assertion peer |
| `.../README-windows-service.md`                           | Operator note                   |
| `.../README.md`                                           | Module operator note            |
| `docs/ai-generated/code-reviews/...-erlang.md`            | This report                     |

Cross-platform path review: bat scripts intentionally use Windows `\` and Procrun
flags (Windows-only entrypoints). Java tests use `Path.of` / UTF-8 reads only. Linux
`.sh` CATALINA_OUT assertion remains forward-slash as on disk. No hardcoded
`C:\…` install roots. No CMS Jetty (`perc-jetty`) changes.

Prior report / memory patterns: installer/packaging peers
(`DtsTomcat11WindowsServiceAlignmentTest`, `DtsServiceInstallScriptTest`); change-class
companions include script + test + operator doc.

## Issues

None at **bug** severity.

### suggestion

1. **Dual writers to `catalina.log`** — Log4j RollingFile CATALINA appender and Procrun
   stdout both target `catalina.log` (same as Linux CATALINA_OUT + Log4j). Acceptable for
   parity; operators may later drop CONSOLE appender if noise is high. No change required
   for #938.

### nit

1. README-windows-service.md ships via rootFiles; installDts does not specially co-locate
   it under Deployment/Server only — it follows existing rootFiles copy policy (acceptable;
   module README also documents the path).

## Gate checklist

- [x] Behavioral packaging tests for changed installer contract
- [x] No non-portable Java path I/O
- [x] Module `mvnw clean install` green (delivery-tier-distribution)
- [x] Scope limited to DTS Windows service logging (no Jetty)

## Recommendation detail

**approve** — root cause (stdout=auto + wrong JUL manager/config.file) fixed with
symmetric Production/Staging bats, regression tests, and operator docs.
