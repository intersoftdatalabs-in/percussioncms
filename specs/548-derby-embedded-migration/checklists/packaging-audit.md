# Packaging audit — H2 default vs Derby migration scope (T095 / QC-013 / SC-009)

**Date:** 2026-07-24 (updated 2026-08-11)  
**Branch context:** stack #1494–#1499 + US6 #1504 on **`main`**; residual QC freeze #3065  
**Goal:** New default installs do not require Derby as the **live** repository engine; H2 is present; Derby is migration-scoped (FR-021 window).

## Live defaults (PASS)

|                Surface                |                                   Expected                                    |                                      Evidence                                      |
|---------------------------------------|-------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| CMS `rxrepository.properties` default | `DB_DRIVER_NAME=h2`, `org.h2.Driver`                                          | `system/config/Default/rxrepository.properties`                                    |
| Jetty `perc-ds` defaults              | driver name `h2`, class `org.h2.Driver`                                       | `modules/perc-jetty/src/main/jetty/defaults/etc/perc-ds.properties`, `perc-ds.xml` |
| Install orchestration                 | No NetworkServerControl / 1527 for default path                               | US1 packaging tests; `installRepository.xml` redesign                              |
| DTS default datasources               | H2 props / dialect for product-managed services                               | DTS `DefaultH2BeansPackagingTest` and related props                                |
| Parent POM                            | `h2.version` + `dependencyManagement`; `derby.version` retained for migration | root `pom.xml`                                                                     |

Automated packaging tests (non-exhaustive):

- `modules/perc-distribution-tree/.../DefaultEmbeddedH2PackagingTest`
- `modules/perc-jetty/.../DefaultDatasourceH2PackagingTest`
- `deliverytiersuite/.../metadata/.../DefaultH2BeansPackagingTest`
- `modules/perc-distribution-tree/.../ExternalDbSamplePropsPackagingTest` (external samples stable)

## Derby migration scope (PASS for policy)

|                      Item                       |                    Status                    |
|-------------------------------------------------|----------------------------------------------|
| Derby on new-install **live** default classpath | Must not be required — H2 is default         |
| Derby for upgrade-time export / FR-021 window   | Allowed — see `derby-migration-classpath.md` |
| Post-SUCCESS live store                         | H2 only (no dual-write)                      |

## `.ppkg` / `psx_archiveInfo.xml` driver stamps (QC-023 / T096)

**Finding:** Dozens of packages under `modules/perc-packages/**/psx_archiveInfo.xml` still record `<driver>derby</driver>` (archive **build-time metadata** stamp). Count on audit host: **41** files with that stamp.

|                Severity                |                                                                                         Disposition                                                                                          |
|----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Soft (current)**                     | Stamps are historical package archive metadata, not Jetty/CMS live `rxrepository` defaults. They do **not** by themselves force new installs onto Derby as the live engine.                  |
| **Hard before claiming QC-023 closed** | Representative package **install** on an H2 default instance must succeed without requiring Derby as the live driver. Re-stamp packages only if install validation fails on driver identity. |

**Action (docs closeout):** Soft stamps documented only (no mass XML rewrite). Hard representative install tracked as human QA **#2333**.

## OS full-install smoke (T038)

Packaging **unit** evidence: PASS (see `os-smoke-matrix.md`).  
Full CMS login + DTS health on Windows/Linux/macOS: **open** human QA **#2332** (not agent multi-OS matrix). Packaging defaults above are not the blocker.

## Residual risks

1. DTS distribution may still carry Derby coordinates for migration/legacy paths — confirm start scripts use H2 home (QC-024), not only `derby.system.home`.
2. **QC-001 freeze is met** — inventory re-run **2026-08-11** reports **0 `unknown`** disposition rows (`python scripts/derby-surface-inventory.py --fail-on-unknown`; see `derby-surface-inventory.md`). Residual triage is disposition *quality* (soft/docs-only stamps, minified-JS false positives already reclassified), not an open unknown count.

## Sign-off

|                      QC                      |                                         Status                                         |
|----------------------------------------------|----------------------------------------------------------------------------------------|
| QC-013 / SC-009 (new default not live Derby) | **Met** for source defaults + packaging unit tests                                     |
| QC-023 package install on H2                 | Soft **met**; hard **passed** human QA **#2333** (2026-08-11, @vijaya-boddipudi)       |
| T038 full OS smoke                           | **Open** — human QA **#2332**                                                          |

