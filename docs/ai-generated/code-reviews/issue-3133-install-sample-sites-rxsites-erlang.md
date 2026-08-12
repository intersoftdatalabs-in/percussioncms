# Erlang review: #3133 installSampleSites RXSITES

**Branch:** `fix/issue-3133-install-sample-sites-rxsites`  
**Scope:** uncommitted changes vs HEAD  
**Date:** 2026-08-12  
**Recommendation:** approve  
**Gate:** May commit/push: yes  

## Summary

`installSampleSites` only passed `RxffTableDef.xml` to `PSTableAction`. That task iterates **schema** entries and looks up data by name; `RxffTableDef` is only `RXS_CT_*` while sample graph data (including `RXSITES`) lives in `RxffTableData.xml`. Fix restores legacy FastForward dual def: `cmsTableDef.xml,RxffTableDef.xml`.

## Issues

None (bugs / missing behavioral tests / non-portable paths).

## Cross-platform path checklist

- ANT `${data.dir}/…` properties only — portable  
- Tests use `java.nio.file.Path` relative to module CWD (peer pattern)  
- No hardcoded separators or OS-specific paths  

## Tests

`InstallSampleSitesWiringTest` (3): dual tableDef wiring; RXSITES replace rows + site names; every sample data table has schema in combined defs.

## Memory patterns hit

- Installer/packaging companions: lockstep wiring test + historic peer (`installFastForward.xml`)  
- Behavioral regression for silent success path  

## Build

`modules/perc-distribution-tree`: `mvnw clean install` — BUILD SUCCESS; InstallSampleSitesWiringTest 3/3; module surefire totals 251 tests, 0 failures.
