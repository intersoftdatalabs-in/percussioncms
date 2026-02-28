# SITEMANAGE MODULE COMPILATION FIX PLAN

**Date**: February 22, 2026
**Status**: Initial Analysis Complete
**Total Errors**: 200 compilation errors (pre-analysis filtering)
**Estimated Fix Time**: 8-10 hours (all phases)

---

## EXECUTIVE SUMMARY

The sitemanage module compilation identified **200 total errors** spanning across multiple error categories. By fixing the **TOP 10 files**, approximately **130+ errors (~65%)** can be eliminated. The remaining errors are distributed across other files and require case-by-case investigation.

### Key Findings:

- **Highest Impact**: File #1 (PSManagedLinkService) - 24 errors from single pattern
- **Pattern-Based Issues**: 126 errors follow repeatable patterns (Optional, Override, Symbols)
- **Most Common Error**: Missing symbol errors (68 errors, 34% of total)
- **Quick Wins**: Optional type issues and missing @Override annotations account for 102 errors

---

## ERROR DISTRIBUTION BY CATEGORY

|     Error Type      |  Count  |    %     | Affected Files | Severity | Fix Complexity |
|---------------------|---------|----------|----------------|----------|----------------|
| **MISSING_SYMBOL**  | 68      | 34%      | 18 files       | HIGH     | VARIABLE       |
| **OPTIONAL_TYPE**   | 58      | 29%      | 11 files       | HIGH     | LOW            |
| **METHOD_OVERRIDE** | 44      | 22%      | 11 files       | MEDIUM   | LOW            |
| **TYPE_MISMATCH**   | 8       | 4%       | 1 file         | MEDIUM   | MEDIUM         |
| **ABSTRACT_METHOD** | 6       | 3%       | 3 files        | HIGH     | MEDIUM         |
| **OTHER**           | 16      | 8%       | 7 files        | MEDIUM   | VARIABLE       |
| **TOTAL**           | **200** | **100%** | Multiple       | -        | -              |

---

## TOP 10 FILES REQUIRING FIXES

### 🔴 FILE 1: PSManagedLinkService.java

**Path**: `projects/sitemanage/src/main/java/com/percussion/linkmanagement/service/impl/PSManagedLinkService.java`
**Errors**: 24 | **Type**: OPTIONAL_TYPE (100%) | **Priority**: CRITICAL | **Impact**: 12% of total errors

#### Error Pattern:

```
incompatible types: Optional<PSManagedLink> cannot be converted to PSManagedLink
```

#### Affected Lines:

310, 334, 358, 464, 520, 626, 735, 1097, 1107, 1135, 1173, 1462

#### Root Cause:

Method `dao.findLinkByLinkId(int linkId)` returns `Optional<PSManagedLink>` but assignment expects unwrapped `PSManagedLink`.

#### Proposed Fix (Apply to ALL occurrences):

```java
// BEFORE (Line 310 example):
mLink = dao.findLinkByLinkId(Integer.parseInt(entry.getString(PERC_IMAGEPATH_LINKID)));

// AFTER:
mLink = dao.findLinkByLinkId(Integer.parseInt(entry.getString(PERC_IMAGEPATH_LINKID))).orElse(null);
```

#### Fix Pattern:

Replace: `dao.findLinkByLinkId(...)`
With: `dao.findLinkByLinkId(...).orElse(null)`

**Estimated Effort**: 10 minutes | **Impact**: Eliminates 24 errors (12% of total)

---

### 🔴 FILE 2: PSLinkableAsset.java

**Path**: `projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSLinkableAsset.java`
**Errors**: 14 | **Type**: METHOD_OVERRIDE (100%) | **Priority**: HIGH | **Impact**: 7% of total errors

#### Error Pattern:

```
method does not override or implement a method from a supertype
```

#### Affected Lines:

64, 99, 109, 132, 137, 142, 147

#### Root Cause:

Methods override supertype methods but lack `@Override` annotation. Java expects methods that implement interface/abstract class methods to be explicitly annotated.

#### Proposed Fix (Apply to ALL 7 methods):

```java
// BEFORE (Line 64 example):
public String getId() {
    ...
}

// AFTER:
@Override
public String getId() {
    ...
}
```

#### Methods Needing @Override:

1. Line 64: `getId()`
2. Line 99: `getTocId()`
3. Line 109: (method name needs verification)
4. Line 132: (method name needs verification)
5. Line 137: (method name needs verification)
6. Line 142: (method name needs verification)
7. Line 147: (method name needs verification)

**Estimated Effort**: 5 minutes | **Impact**: Eliminates 14 errors (7% of total)

---

### 🔴 FILE 3: PSTemplateDao.java

**Path**: `projects/sitemanage/src/main/java/com/percussion/pagemanagement/dao/impl/PSTemplateDao.java`
**Errors**: 14 | **Type**: MIXED (SYMBOL=10, ABSTRACT=2, OVERRIDE=2) | **Priority**: CRITICAL | **Impact**: 7% of total errors

#### Error Pattern 1 - Abstract Method Not Implemented:

```
Line 97: PSTemplateDao is not abstract and does not override abstract method remove(String) in IPSGenericDao
```

#### Error Pattern 2 - Missing Symbols:

```
Lines 156, 157, 353, 827, 838: cannot find symbol
```

#### Error Pattern 3 - Missing Override:

```
Line 153: method does not override or implement a method from a supertype
```

#### Root Causes:

1. Class implements `IPSGenericDao` but doesn't provide `remove(String)` method
2. References to undefined methods/variables at lines 156, 157, 353, 827, 838
3. Methods implementing interface contracts lack `@Override` annotation

#### Proposed Fixes:

**Fix 1 - Add Abstract Method (Line 97):**

```java
// Add this method to PSTemplateDao class:
@Override
public void remove(String id) {
    // Implementation needed - check interface contract
}
```

**Fix 2 - Add @Override (Line 153):**

```java
@Override
public [return_type] methodName(...) {
    ...
}
```

**Fix 3 - Resolve Missing Symbols (Lines 156, 157, 353, 827, 838):**
- Investigation required to identify undefined methods/variables
- Check method signatures in dependencies
- Verify import statements

**Estimated Effort**: 45 minutes | **Impact**: Eliminates 14 errors (7% of total)

---

### 🟠 FILE 4: PSPageService.java

**Path**: `projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSPageService.java`
**Errors**: 14 | **Type**: MIXED (SYMBOL=8, OPTIONAL=4, OTHER=2) | **Priority**: HIGH | **Impact**: 7% of total errors

#### Error Patterns:

**Pattern 1 - Missing Symbols (8 errors):**

```
Lines 466, 1230, 1232, 1234: cannot find symbol
```

**Pattern 2 - Optional Type Conversion (4 errors):**

```
Line 322: incompatible types: Optional<Long> cannot be converted to Long
Line 748: incompatible types: Optional<String> cannot be converted to String
```

**Pattern 3 - Method Signature Mismatch (2 errors):**

```
Line 1093: no suitable method found for getPageIds(Optional<Long>)
```

#### Proposed Fixes:

**Fix 1 - Optional Handling:**

```java
// BEFORE (Line 322):
siteSiteMetadataService.getSiteId(siteId)  // siteId is Optional<Long>

// AFTER (Option A - handle as Optional):
siteSiteMetadataService.getSiteId(siteId.orElse(-1L))

// AFTER (Option B - declare as Optional):
Optional<Long> siteId = ...
```

**Fix 2 - Optional String Handling (Line 748):**

```java
// BEFORE:
template.getStyleSheetId()  // Returns Optional<String>

// AFTER:
template.getStyleSheetId().orElse("")
```

**Fix 3 - Method Signature (Line 1093):**

```java
// Check signature of getPageIds() method
// Change: getPageIds(Optional<Long>)
// To: getPageIds(Long) or change to accept Optional
```

**Fix 4 - Missing Symbols (Lines 466, 1230-1234):**
- Requires investigation of undefined method/variable names
- Check method signatures and imports

**Estimated Effort**: 45 minutes | **Impact**: Eliminates 14 errors (7% of total)

---

### 🟠 FILE 5: PSResourceInstanceHelper.java

**Path**: `projects/sitemanage/src/main/java/com/percussion/pagemanagement/assembler/impl/PSResourceInstanceHelper.java`
**Errors**: 12 | **Type**: MIXED (OPTIONAL=8, SYMBOL=4) | **Priority**: HIGH | **Impact**: 6% of total errors

#### Error Patterns:

**Pattern 1 - Optional Type Issues (8 errors):**

```
Line 172: incompatible types: Optional<String> cannot be converted to String
Line 254: incompatible types: Optional<String> cannot be converted to String
Line 256: incompatible types: String cannot be converted to Optional<String>
Line 258: incompatible types: Optional<String> cannot be converted to String
```

**Pattern 2 - Missing Symbols (4 errors):**

```
Lines 118, 183: cannot find symbol
```

#### Proposed Fixes:

**Fix 1 - Optional String Handling:**

```java
// BEFORE (Line 172):
String path = resourceDef.getSomething()  // Returns Optional<String>

// AFTER (Option A):
String path = resourceDef.getSomething().orElse("")

// AFTER (Option B):
Optional<String> path = resourceDef.getSomething()
```

**Fix 2 - Reverse Conversion (Line 256):**

```java
// BEFORE:
Optional<String> value = someString;  // String cannot be converted to Optional

// AFTER:
Optional<String> value = Optional.of(someString);
// OR
Optional<String> value = Optional.ofNullable(someString);
```

**Fix 3 - Missing Symbols (Lines 118, 183):**
- Investigation required for undefined methods/variables

**Estimated Effort**: 40 minutes | **Impact**: Eliminates 12 errors (6% of total)

---

### 🟠 FILE 6: PSContentItemDao.java

**Path**: `projects/sitemanage/src/main/java/com/percussion/share/dao/impl/PSContentItemDao.java`
**Errors**: 10 | **Type**: MIXED (EXCEPTION=6, OVERRIDE=2, STREAM=2) | **Priority**: HIGH | **Impact**: 5% of total errors

#### Error Patterns:

**Pattern 1 - Stream to Collection (2 errors):**

```
Line 115: incompatible types: Stream<Integer> cannot be converted to Collection<Integer>
```

**Pattern 2 - Exception to String Conversion (6 errors):**

```
Line 253: incompatible types: Exception cannot be converted to String
Line 278: incompatible types: PSDataServiceException cannot be converted to String
Line 292: incompatible types: Exception cannot be converted to String
```

**Pattern 3 - Missing Override (2 errors):**

```
Line 219: method does not override or implement a method from a supertype
```

#### Proposed Fixes:

**Fix 1 - Stream to Collection (Line 115):**

```java
// BEFORE:
Collection<Integer> ids = itemIds.stream()...

// AFTER (option A - collect to list):
Collection<Integer> ids = itemIds.stream().collect(Collectors.toList())

// AFTER (option B - collect to set):
Collection<Integer> ids = itemIds.stream().collect(Collectors.toSet())
```

**Fix 2 - Exception to String (Lines 253, 278, 292):**

```java
// BEFORE:
catch (Exception e) {
    String error = e;  // ERROR
}

// AFTER (option A - use message):
catch (Exception e) {
    String error = e.getMessage();
}

// AFTER (option B - use toString):
catch (Exception e) {
    String error = e.toString();
}
```

**Fix 3 - Add @Override (Line 219):**

```java
@Override
public [return_type] methodName(...) {
    ...
}
```

**Estimated Effort**: 30 minutes | **Impact**: Eliminates 10 errors (5% of total)

---

### 🟠 FILE 7: PSPathItemService.java

**Path**: `projects/sitemanage/src/main/java/com/percussion/pathmanagement/service/impl/PSPathItemService.java`
**Errors**: 8 | **Type**: METHOD_OVERRIDE (100%) | **Priority**: MEDIUM | **Impact**: 4% of total errors

#### Error Pattern:

```
Lines 179, 189, 524, 529: method does not override or implement a method from a supertype
```

#### Root Cause:

Four methods override supertype methods but lack `@Override` annotation.

#### Proposed Fix (Apply to ALL 4 methods):

```java
@Override
public [return_type] methodName(...) {
    ...
}
```

#### Methods Needing @Override:

1. Line 179
2. Line 189
3. Line 524
4. Line 529

**Estimated Effort**: 5 minutes | **Impact**: Eliminates 8 errors (4% of total)

---

### 🟡 FILE 8: PSPageCatalogService.java

**Path**: `projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSPageCatalogService.java`
**Errors**: 8 | **Type**: MIXED (OPTIONAL=6, SYMBOL=2) | **Priority**: MEDIUM | **Impact**: 4% of total errors

#### Error Patterns:

**Pattern 1 - Optional Type Issues (6 errors):**

```
Line 342: incompatible types: Optional<Long> cannot be converted to Long
Line 425: incompatible types: Optional<Long> cannot be converted to Long
Line 454: incompatible types: Optional<Long> cannot be converted to Long
```

**Pattern 2 - Missing Symbol (2 errors):**

```
Line 334: cannot find symbol
```

#### Proposed Fixes:

**Fix 1 - Optional Long Handling:**

```java
// BEFORE:
Long siteId = something.getOptionalId()

// AFTER:
Long siteId = something.getOptionalId().orElse(-1L)
// OR
Optional<Long> siteId = something.getOptionalId()
```

**Estimated Effort**: 20 minutes | **Impact**: Eliminates 8 errors (4% of total)

---

### 🟡 FILE 9: PSPageAssemblyContextFactory.java

**Path**: `projects/sitemanage/src/main/java/com/percussion/pagemanagement/assembler/PSPageAssemblyContextFactory.java`
**Errors**: 6 | **Type**: MISSING_SYMBOL (100%) | **Priority**: MEDIUM | **Impact**: 3% of total errors

#### Error Pattern:

```
Lines 368, 542, 548: cannot find symbol
```

#### Proposed Fix:

- Requires investigation of undefined method/variable names at each line
- Check method signatures and imports
- Verify dependency availability

**Estimated Effort**: 30 minutes | **Impact**: Eliminates 6 errors (3% of total)

---

### 🟡 FILE 10: PSMockDataForUnassignedPages.java

**Path**: `projects/sitemanage/src/main/java/com/percussion/pagemanagement/service/impl/PSMockDataForUnassignedPages.java`
**Errors**: 6 | **Type**: MISSING_SYMBOL (100%) | **Priority**: MEDIUM | **Impact**: 3% of total errors

#### Error Pattern:

```
Lines 123, 125, 126: cannot find symbol
```

#### Proposed Fix:

- Requires investigation of undefined method/variable names
- Check method signatures and imports
- Verify dependency availability

**Estimated Effort**: 30 minutes | **Impact**: Eliminates 6 errors (3% of total)

---

## PRIORITY-BASED FIX SCHEDULE

### 🔥 PHASE 1: High-Impact Quick Wins (Est. 1-2 hours)

**Target**: Eliminate ~46 errors (23% of total)

1. **PSManagedLinkService.java** (24 errors)
   - Simple pattern replacement: `.orElse(null)`
   - Est. 10 minutes
2. **PSPathItemService.java** (8 errors)
   - Add `@Override` to 4 methods
   - Est. 5 minutes
3. **PSLinkableAsset.java** (14 errors)
   - Add `@Override` to 7 methods
   - Est. 5 minutes

**Phase 1 subtotal**: 46 errors, ~20 minutes work

### 🔶 PHASE 2: Medium-Complexity Issues (Est. 2-3 hours)

**Target**: Eliminate ~42 errors (21% of total)

4. **PSPageCatalogService.java** (8 errors)
   - Optional handling + missing symbols
   - Est. 20 minutes
5. **PSResourceInstanceHelper.java** (12 errors)
   - Optional handling + missing symbols
   - Est. 40 minutes
6. **PSContentItemDao.java** (10 errors)
   - Stream conversion, Exception handling, Override
   - Est. 30 minutes
7. **PSPageAssembler.java** (4 errors)
   - Method overrides
   - Est. 10 minutes

**Phase 2 subtotal**: 42 errors, ~1.5-2 hours work

### 🟠 PHASE 3: Complex Investigation Required (Est. 3-4 hours)

**Target**: Eliminate ~42 errors (21% of total)

8. **PSTemplateDao.java** (14 errors)
   - Abstract method implementation
   - Missing symbol investigation
   - Est. 45 minutes
9. **PSPageService.java** (14 errors)
   - Missing symbols investigation
   - Optional handling
   - Est. 45 minutes
10. **PSPageAssemblyContextFactory.java** (6 errors)
    - Missing symbol investigation
    - Est. 30 minutes
11. **PSMockDataForUnassignedPages.java** (6 errors)
    - Missing symbol investigation
    - Est. 30 minutes

**Phase 3 subtotal**: 42 errors, ~2.5-3 hours work

### Additional Work

**Remaining errors** (~70 errors, 35% of total) distributed across other files require case-by-case investigation.

---

## EXPECTED COMPILATION RESULTS AFTER FIXES

|     Phase     | Errors Fixed | Cumulative | % Complete |
|---------------|--------------|------------|------------|
| Initial       | 0            | 0          | 0%         |
| After Phase 1 | 46           | 46         | 23%        |
| After Phase 2 | 42           | 88         | 44%        |
| After Phase 3 | 42           | 130        | 65%        |
| Remaining     | ~70          | 200        | 100%       |

---

## IMPLEMENTATION NOTES

1. **Apply Phase 1 first** - One-line fixes with immediate impact
2. **Use Find & Replace** for PSManagedLinkService.java to speed up Optional handling
3. **Add @Override annotations systematically** - Use IDE support
4. **Investigation Phase** - For missing symbols, check:
   - Method definitions in current file
   - Dependencies in imported classes
   - Interface contracts
   - Recently refactored code
5. **Testing** - After each phase, recompile to verify error reduction

---

## OTHER REMAINING FILES WITH ERRORS

Beyond the top 10, the following files have additional compilation errors:

- PSAssetDao.java (4 errors)
- PSWidgetAssetRelationshipService.java (4 errors)
- PSCommentsService.java (4 errors)
- PSFeedsInfoService.java (4 errors)
- PSLivePublishChangeHandler.java (4 errors)
- PSItemService.java (4 errors)
- PSContentsService.java (various errors)
- PSWorkflowHelper.java (various errors)
- PSRoleService.java (various errors)
- PSApibridge/TemplateAdaptor.java (various errors)

---

## NEXT STEPS

1. Execute Phase 1 fixes immediately
2. Recompile and verify 46 error reduction
3. Execute Phase 2 fixes
4. Recompile and verify cumulative reduction
5. Investigate missing symbols for Phase 3
6. Execute Phase 3 fixes
7. Address remaining errors through targeted investigation

