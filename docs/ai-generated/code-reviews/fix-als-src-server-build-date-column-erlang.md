# Erlang review: fix ALS_SRC_SERVER_BUILD_DATE column name regression

**Date:** 2026-07-23  
**Scope:** Uncommitted deployer fix vs `development` (excludes unrelated staged `pom.xml` ai-build-integrity bump)  
**Intent:** Restore physical DB column name for `DPL_ARCHIVE_LOG_SUMMARY.SRC_SERVER_BUILD_DATE` after PR #1459 incorrectly set the Java constant value to `ALS_SRC_SERVER_BUILD_DATE`, breaking startup package install.

## Summary

PR #1459 renamed the *value* of `ALS_SRC_SERVER_BUILD_DATE` from `SRC_SERVER_BUILD_DATE` to `ALS_SRC_SERVER_BUILD_DATE`. Sibling constants (`ALS_SRC_SERVER_NAME` → `SRC_SERVER_NAME`, etc.) correctly omit the `ALS_` prefix from the physical column name. Schema in `cmsTableDef.xml` defines `SRC_SERVER_BUILD_DATE`. Runtime error: `PSDbmsHelper.processTable` → column not found in table schema → all startup packages fail.

This change restores the correct constant value and aligns unit test assertions. Date format fix from #1459 (`yyyy-MM-dd HH:mm:ss`) is preserved.

## Recommendation

**approve**

## Gate

|               Check                |                               Result                                |
|------------------------------------|---------------------------------------------------------------------|
| Bugs                               | none                                                                |
| Behavioral tests for changed logic | present (`PSLogHandlerTest` asserts column name + JDBC date format) |
| Cross-platform path/file I/O       | N/A (no path I/O)                                                   |
| Security / silent failure          | none — restores correct metadata write path                         |
| May commit/push                    | **yes**                                                             |

## Issues

None.

## Memory patterns hit

- Column name / schema alignment: constant value must match physical schema, not Java naming prefix.

## Verification noted

- `cd deployer && ../mvnw -o test -Dtest=PSLogHandlerTest` — Tests run: 2, Failures: 0
- Pre-PR: `cd deployer && ../mvnw -o clean install` (standalone)

