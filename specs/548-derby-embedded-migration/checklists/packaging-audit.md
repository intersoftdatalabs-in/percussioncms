# Packaging audit — H2 default vs Derby migration scope (T095 / QC-013 / SC-009)

**Date:** 2026-07-24  
**Branch context:** post-merge stack #1494–#1499 on `development`  
**Goal:** New default installs do not require Derby as the **live** repository engine; H2 is present; Derby is migration-scoped (FR-021 window).

## Live defaults (PASS)

| Surface | Expected | Evidence |
|---------|----------|----------|
| CMS `rxrepository.properties` default | `DB_DRIVER_NAME=h2`, `org.h2.Driver` | `system/config/Default/rxrepository.properties` |
| Jetty `perc-ds` defaults | driver name `h2`, class `org.h2.Driver` | `modules/perc-jetty/src/main/jetty/defaults/etc/perc-ds.properties`, `perc-ds.xml` |
| Install orchestration | No NetworkServerControl / 1527 for default path | US1 packaging tests; `installRepository.xml` redesign |
| DTS default datasources | H2 props / dialect for product-managed services | DTS `DefaultH2BeansPackagingTest` and related props |
| Parent POM | `h2.version` + `dependencyManagement`; `derby.version` retained for migration | root `pom.xml` |

Automated packaging tests (non-exhaustive):

- `modules/perc-distribution-tree/.../DefaultEmbeddedH2PackagingTest`
- `modules/perc-jetty/.../DefaultDatasourceH2PackagingTest`
- `deliverytiersuite/.../metadata/.../DefaultH2BeansPackagingTest`
- `modules/perc-distribution-tree/.../ExternalDbSamplePropsPackagingTest` (external samples stable)

## Derby migration scope (PASS for policy)

| Item | Status |
|------|--------|
| Derby on new-install **live** default classpath | Must not be required — H2 is default |
| Derby for upgrade-time export / FR-021 window | Allowed — see `derby-migration-classpath.md` |
| Post-SUCCESS live store | H2 only (no dual-write) |

## `.ppkg` / `psx_archiveInfo.xml` driver stamps (QC-023 / T096)

**Finding:** Dozens of packages under `modules/perc-packages/**/psx_archiveInfo.xml` still record `<driver>derby</driver>` (archive **build-time metadata** stamp). Count on audit host: **41** files with that stamp.

| Severity | Disposition |
|----------|-------------|
| **Soft (current)** | Stamps are historical package archive metadata, not Jetty/CMS live `rxrepository` defaults. They do **not** by themselves force new installs onto Derby as the live engine. |
| **Hard before claiming QC-023 closed** | Representative package **install** on an H2 default instance must succeed without requiring Derby as the live driver. Re-stamp packages only if install validation fails on driver identity. |

**Action for this closeout PR:** Document only (no mass XML rewrite). Follow-up PR if package install IT fails on H2.

## OS full-install smoke (T038)

Packaging **unit** evidence: PASS on Linux (see `os-smoke-matrix.md`).  
Full CMS login + DTS health on Windows/Linux/macOS: **pending** distribution artifact / agent runs. Not blocked by packaging defaults above.

## Residual risks

1. DTS distribution may still carry Derby coordinates for migration/legacy paths — confirm start scripts use H2 home (QC-024), not only `derby.system.home`.
2. Inventory file still has many **heuristic `unknown`** dispositions (QC-001 freeze) — separate triage pass; false positives include minified JS matching `derby` substring.

## Sign-off

| QC | Status |
|----|--------|
| QC-013 / SC-009 (new default not live Derby) | **Met** for source defaults + packaging unit tests |
| QC-023 package install on H2 | **Open** soft — stamps documented; install IT not re-run in this docs PR |
| T038 full OS smoke | **Open** |
