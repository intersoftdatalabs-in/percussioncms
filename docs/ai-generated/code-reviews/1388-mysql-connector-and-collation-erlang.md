# Erlang Code Review: Issue #1388 Fix - MySQL Connector 8.4.0 & Collation Alignment

**Date:** 2026-07-21  
**Target Branch:** `fix/1388-mysql-connector-and-collation-fix`  
**PR Target:** `development`  
**Author:** Antigravity

---

## 1. Summary of Changes

This review covers the fix for GitHub issue [#1388](https://github.com/intersoftdatalabs-in/percussioncms/issues/1388):
1. **Upgraded/Bundled MySQL JDBC Driver**: Added `mysql-connector-j` 8.4.0 to parent POM dependency management, included `mysql-connector-j` as a provided dependency in `modules/perc-distribution-tree/pom.xml`, updated staging copy filesets in `installDistributionFiles.xml`, added explicit delete entries in `install.xml` and `installRepository.xml`, and registered `mysql-connector-j-8.4.0.jar` in `BundledJdbcDrivers` test suite.
2. **MySQL 8.x View Collation Mismatch Fix**: Resolved `Illegal mix of collations for operation 'UNION'` on `PSX_DISPLAYFORMATPROPERTY_VIEW` by adding `COLLATE utf8mb4_0900_ai_ci` to `rtrim(cast(p.COMMUNITYID AS CHAR))` in `sqlMysql` view definitions across `installRepository.xml` and `system/installResources/install.xml`.

---

## 2. Review Findings

### 2.1 Correctness & Functional Logic — PASS

- **JDBC Driver Distribution**: `mysql-connector-j-8.4.0.jar` is properly staged into `jetty/base/lib/jdbc/` during `perc-distribution-tree` packaging and verified by `verify-jdbc-drivers`.
- **SQL Collation**: Explicitly casting `cast(p.COMMUNITYID AS CHAR)` to `utf8mb4_0900_ai_ci` ensures the UNION with `PSX_DISPLAYFORMATPROPERTIES.PROPERTYVALUE` succeeds on MySQL 8.0/8.4 default database collations.

### 2.2 Cross-Platform Compatibility — PASS

- All path inclusions and delete specifications use portable globs (`mysql-connector-j-*.jar`) and platform-independent XML configurations.

### 2.3 Maven Verification & Tests — PASS

- Standalone build (`cd modules/perc-distribution-tree && ../../mvnw clean install`) passes all 75 unit tests with zero failures or errors, including `StagingCleanupAntScriptTest`, `InstallXmlDeleteSetTest`, and `VerifyJdbcDrivers`.

---

## 3. Review Gate Conclusion

**PASS** — Approved for commit and PR submission.
