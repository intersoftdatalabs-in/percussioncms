# Metadata Test Failures Analysis

**Date:** February 26, 2026
**Status:** Analysis Complete
**Root Causes:** Mixed - Both Code and Test Issues Identified

## Executive Summary

The test failures fall into **three distinct categories**:

1. **🔴 CRITICAL: Hibernate Path Expression Errors** (Code Bug - Affects 21 tests)
2. **🟡 IMPORTANT: Data Logic Issues** (Code Bug - Affects 9 tests)
3. **🟢 SUGGESTION: Test Data/Assertion Issues** (Test Logic - Affects 5 tests)

---

## Category 1: Hibernate Path Expression Errors (Code Bugs)

### Issue Type

Query path syntax error in HQL that Hibernate 6.x cannot interpret.

### Affected Tests (21 total)

All tests with error: `Could not interpret attribute 'name' of basic-valued path 'com.percussion.delivery.metadata.rdbms.impl.PSDbMetadataEntry(me).properties(p0).{element}.id'`

Including:
- `testCriteria_Single_Property_*` (multiple variations)
- `testOrderBy_*` (multiple variations)
- `testQuery`, `testQueryDuplicateEntries`, `testQueryLimit`
- `PSBlogsHelperTest.testBlogProcess`
- Several others in PSMetadataQueryService

### Root Cause

**File:** [PSMetadataQueryService.java](PSMetadataQueryService.java#L437)

**Lines:** 437-438

```java
clauseTemplate = " lower(p{0}.id.name) = lower(:{4}) and p{0}.{1} {2} :{3}";
inClauseTemplate = " lower(p{0}.id.name) = lower(:{4}) and p{0}.{1} {2} (:{3})";
```

The code attempts to access `p{0}.id.name`, but looking at the `PSDbMetadataProperty` entity:
- `id` is a simple `@Id` field of type `int` (auto-generated)
- `name` is a separate `@Column` field of type `String`

There is **no composite ID class or embedded object** where `id` contains a `name` attribute.

### What Happened

This appears to be a Hibernate version upgrade issue. Older versions of Hibernate (HQL) were more lenient with path expressions and allowed traversing through scalar fields, but **Hibernate 6.x with JPA 3.0** enforces strict path semantics.

### The Fix

Change lines 437-438 in `PSMetadataQueryService.java` from:

```java
clauseTemplate = " lower(p{0}.id.name) = lower(:{4}) and p{0}.{1} {2} :{3}";
inClauseTemplate = " lower(p{0}.id.name) = lower(:{4}) and p{0}.{1} {2} (:{3})";
```

To:

```java
clauseTemplate = " lower(p{0}.name) = lower(:{4}) and p{0}.{1} {2} :{3}";
inClauseTemplate = " lower(p{0}.name) = lower(:{4}) and p{0}.{1} {2} (:{3})";
```

**Impact:** Fixes 21 failing tests immediately.

---

## Category 2: Data Logic Issues (Code Bugs)

### Issue Type

Incorrect data being saved or counted - logic problems in the service code.

### Affected Tests (9 total)

#### A. `PSMetadataIndexerServiceTest`

- `testSaveDeleteSingle`: Expected 3 entries, got 2
- `testSave_MultipleValueProperties`: Expected 66.0, got 4.0

#### B. `PSMetadataQueryServiceTest`

- `testCriteria_Single_EntryField_*`: Expected 5 entries, various results (1, 3, 10, 15)
- `testOrderBy_Folder`: Folder path ordering logic broken
- `testOrderBy_PagePath`: Expected 5 results, got 3

#### C. Test Data Not Being Persisted Correctly

The `addTestEntries()` method creates test data, but some tests are:
- Getting fewer results than expected (suggests some entries aren't being saved)
- Getting more results than expected on certain fields (suggests data from previous tests leaking)
- Getting wrong values (suggests calculation or transformation logic issue)

### Root Cause Analysis

**Without Seeing the Full Data Flow**, likely causes:

1. **Duplicate Key Prevention**: The test data creation may have entries with duplicate keys that aren't being inserted
   - Location: [PSMetadataQueryServiceTest.java - addTestEntries() method](PSMetadataQueryServiceTest.java#L1437)
2. **Entry Cleanup Between Tests**: The `before()` method calls `indexer.deleteAllMetadataEntries()` but may not be working correctly
   - Location: [PSMetadataQueryServiceTest.java - before() method](PSMetadataQueryServiceTest.java#L63)
3. **Property Grouping/Aggregation**: For tests expecting specific counts (e.g., expecting 66.0), there may be aggregation logic in the service that's not handling duplicates correctly
4. **Folder Path Traversal**: The `testOrderBy_Folder` test expects folder ordering but the actual value returned suggests it's not being sorted properly

### Investigation Steps

1. Check `PSMetadataIndexerService.saveMetadataEntry()` - verify it's not silently failing on duplicates
2. Verify `deleteAllMetadataEntries()` is actually deleting all entries (check transaction handling)
3. Audit the test data creation - ensure no duplicate pagepaths are created
4. Check if there's a unique constraint violation in database that's not being reported

---

## Category 3: Test Data and Assertion Issues (Test Logic)

### Issue Type

Tests may have incorrect expectations or test data may be configured wrong.

### Affected Tests (5 total)

#### A. Jersey Test Setup Error

**Tests:**
- `PSBaseMetadataRestServiceTest` (2 tests)

**Error:**

```
Cannot invoke "org.glassfish.jersey.server.ApplicationHandler.getConfiguration()"
because "applicationHandler" is null
```

**Root Cause:** Jersey test framework not properly initialized. This is a test infrastructure issue, not a code logic issue.

**The Fix:** Check that:
- Jersey `test-beans.xml` properly defines REST test container
- `JerseyTest` framework is correctly configured
- Runtime dependencies for Jersey testing are included in test classpath

#### B. String Transformation Test (`PSMostReadServiceTest.testG`)

- Expected: `"should equal test"`
- Got: `"test"`

**Root Cause:** The test is checking a string transformation that removes a prefix. Either:
- The service code is not applying the transformation
- The test expectation is wrong

#### C. Count/Sum Expectation Issues

- `testSave_MultipleValueProperties`: Expects 66.0 but gets 4.0
- `testOrderBy_PagePath`: Expects 5 results but gets 3

These likely fall into Category 2 (data issues) but could also be test assertion logic bugs.

---

## Summary Table

| Category |       Issue Type       | Test Count |   Severity    |               Fix Location               |       Effort        |
|----------|------------------------|------------|---------------|------------------------------------------|---------------------|
| 1        | Hibernate Path Bug     | 21         | 🔴 CRITICAL   | PSMetadataQueryService.java:437-438      | Low (1 line change) |
| 2        | Data Logic/Persistence | 9          | 🟡 IMPORTANT  | PSMetadataIndexerService, Test Setup     | Medium-High         |
| 3        | Test Infrastructure    | 5          | 🟢 SUGGESTION | Test bean configuration, Test assertions | Medium              |

---

## Recommended Fix Order

1. **Fix the Hibernate path expression** (Category 1) - This will immediately clear 21 test failures
2. **Investigate data persistence** (Category 2) - The remaining 9 failures will likely point to the real issues
3. **Fix Jersey test configuration** (Category 3) - If needed

---

## Files to Review/Modify

**Priority 1 (High Impact):**
- [PSMetadataQueryService.java](PSMetadataQueryService.java) - Lines 437-438

**Priority 2 (Investigation):**
- [PSMetadataIndexerService.java](PSMetadataIndexerService.java) - Check `saveMetadataEntry()` and deletion logic
- [PSMetadataQueryServiceTest.java](PSMetadataQueryServiceTest.java) - Check `addTestEntries()` and `before()` methods
- [test-beans.xml](test-beans.xml) - Check Spring/Jersey bean configuration

**Priority 3 (If Needed):**
- PSBaseMetadataRestServiceTest.java - Jersey configuration

---

## Next Steps

1. Apply the immediate fix to line 437-438 and run tests
2. Analyze remaining failures
3. Root cause the 9 data-related failures
4. Fix test infrastructure issues if confirmed

