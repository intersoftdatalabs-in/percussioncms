# Erlang review — 1500 DTS matrix smoke

**Date:** 2026-07-26  
**Branch:** `1500-dts-matrix-smoke`  
**Scope:** Uncommitted DTS packaging fixes so Layer-1 matrix install-smoke passes for H2, PostgreSQL, MySQL, SQL Server  
**Base:** `development` @ `970ed35488` (PR #1507 merged)

## Summary

Restores property-driven Tomcat configuration for DTS (HTTP **9980**) after EE11 packaging left stock Tomcat **8080** server.xml, and fixes a missing `package` declaration on `PSSimpleRedirectorValve` that prevented digester from loading the valve class.

## Recommendation

**approve**

## Gate

| Check | Result |
|-------|--------|
| Bugs | None remaining in scope |
| Behavioral tests for new/changed logic | Yes — jar packaging guards + nested perc-tomcat-common package path |
| Cross-platform path/file I/O | Clean — NIO `Path`/`Files` in tests; no new OS-specific path joins |
| May commit/push | **yes** |

## Issues

_None._

### Cross-platform path checklist

- [x] No new hardcoded filesystem separators in product code
- [x] Tests use `Path.of` / zip entry names with `/` (ZIP paths)
- [x] Temp file for nested jar uses `Files.createTempFile`
- [x] Line-ending sensitive assertions not introduced

## Changed files (review)

| Path | Notes |
|------|--------|
| `tomcat-common/.../PSSimpleRedirectorValve.java` | Added missing `package com.percussion.tomcat.valves;` |
| `tomcat11/conf/server.xml` | Property-driven connectors + redirector valve (from working install) |
| `tomcat11/conf/catalina.properties` | `common/lib` on common.loader + PROPERTY_SOURCE |
| `delivery-tier-distribution/pom.xml` | Antrun force-overwrite Percussion conf after cargo stage |
| `DtsInstallerJarContainsTomcatTreeTest.java` | Asserts `${http.port}` + PROPERTY_SOURCE in shipping jar |
| `DtsInstallerJarContainsDeploymentCommonLibTest.java` | Asserts valve class path inside nested perc-tomcat-common |
| `docker/README.md` | DTS matrix command + packaging notes |

## Evidence (Layer-1 matrix)

| Cell | Result |
|------|--------|
| dts-h2 | RESULT:OK HTTP 200 (~34s) |
| dts-postgresql | RESULT:OK HTTP 200 (~40s) |
| dts-mysql | RESULT:OK HTTP 200 (~56s) |
| dts-sqlserver | RESULT:OK HTTP 200 (~66s) |

## Maven (standalone)

```text
cd deliverytiersuite/delivery-tier-suite/tomcat-common && ../../../mvnw clean install
cd deliverytiersuite/delivery-tier-suite/delivery-tier-distribution && ../../../mvnw clean install
```

BUILD SUCCESS both modules; surefire green on packaging tests.

## Memory patterns hit

- Packaging regression after EE11/Tomcat 11 conf tree swap (stock vs Percussion)
- Nested jar class package path must match digester `className`
