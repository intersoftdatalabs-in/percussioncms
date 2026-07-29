# QUICK REFERENCE: TOP 10 FILES SUMMARY

## At-a-Glance Error Summary

| #  |               File               | Errors |                Type                 |                              Lines                              |                Fix Pattern                 | Est. Time |
|----|----------------------------------|--------|-------------------------------------|-----------------------------------------------------------------|--------------------------------------------|-----------|
| 1  | **PSManagedLinkService**         | 24     | Optional                            | 310, 334, 358, 464, 520, 626, 735, 1097, 1107, 1135, 1173, 1462 | `.orElse(null)`                            | 10m       |
| 2  | **PSLinkableAsset**              | 14     | Override                            | 64, 99, 109, 132, 137, 142, 147                                 | Add `@Override`                            | 5m        |
| 3  | **PSTemplateDao**                | 14     | Mixed (Symbol, Abstract, Override)  | See detailed plan                                               | Multiple                                   | 45m       |
| 4  | **PSPageService**                | 14     | Mixed (Symbol, Optional, Other)     | See detailed plan                                               | Multiple                                   | 45m       |
| 5  | **PSResourceInstanceHelper**     | 12     | Mixed (Optional, Symbol)            | 118, 172, 183, 254, 256, 258                                    | `.orElse("")` + investigate                | 40m       |
| 6  | **PSContentItemDao**             | 10     | Mixed (Exception, Override, Stream) | 115, 219, 253, 278, 292                                         | `.collect()`, `.getMessage()`, `@Override` | 30m       |
| 7  | **PSPathItemService**            | 8      | Override                            | 179, 189, 524, 529                                              | Add `@Override`                            | 5m        |
| 8  | **PSPageCatalogService**         | 8      | Mixed (Optional, Symbol)            | 334, 342, 425, 454                                              | `.orElse()` + investigate                  | 20m       |
| 9  | **PSPageAssemblyContextFactory** | 6      | Symbol                              | 368, 542, 548                                                   | Investigate                                | 30m       |
| 10 | **PSMockDataForUnassignedPages** | 6      | Symbol                              | 123, 125, 126                                                   | Investigate                                | 30m       |

**TOTALS**: **116 errors** | **Est. 4.5 hours** | **58% of all errors**

---

## Phase 1: Quick Wins (20 minutes)

### 1.1 PSManagedLinkService.java - CRITICAL (24 errors)

**Pattern**: Replace all `.findLinkByLinkId()` results with `.orElse(null)`

```java
// Find all instances of this pattern:
mLink = dao.findLinkByLinkId(...)

// Replace with:
mLink = dao.findLinkByLinkId(...).orElse(null)
```

**Files to modify**: 12 locations

### 1.2 PSPathItemService.java (8 errors)

**Pattern**: Add `@Override` to 4 methods at lines 179, 189, 524, 529

### 1.3 PSLinkableAsset.java (14 errors)

**Pattern**: Add `@Override` to 7 methods at lines 64, 99, 109, 132, 137, 142, 147

**Phase 1 Result**: 46 errors eliminated ✓

---

## Phase 2: Medium Complexity (90 minutes)

### 2.1 PSPageCatalogService.java (8 errors)

- Lines 342, 425, 454: Use `.orElse(-1L)` for Optional<Long>
- Line 334: Investigate missing symbol

### 2.2 PSResourceInstanceHelper.java (12 errors)

- Lines 172, 254, 258: Use `.orElse("")` for Optional<String>
- Line 256: Wrap string in Optional
- Lines 118, 183: Investigate missing symbols

### 2.3 PSContentItemDao.java (10 errors)

- Line 115: Add `.collect(Collectors.toList())`
- Lines 253, 278, 292: Use `.getMessage()` for exceptions
- Line 219: Add `@Override`

**Phase 2 Result**: 42 additional errors eliminated ✓

---

## Phase 3: Complex Investigation (180 minutes)

### 3.1 PSTemplateDao.java (14 errors)

- Line 97: Implement `remove(String)` abstract method
- Line 153: Add `@Override`
- Lines 156, 157, 353, 827, 838: Investigate missing symbols

### 3.2 PSPageService.java (14 errors)

- Lines 466, 1230, 1232, 1234: Investigate missing symbols
- Lines 322, 748: Handle Optional types
- Line 1093: Fix method signature

### 3.3 PSPageAssemblyContextFactory.java (6 errors)

- Lines 368, 542, 548: Investigate missing symbols

### 3.4 PSMockDataForUnassignedPages.java (6 errors)

- Lines 123, 125, 126: Investigate missing symbols

**Phase 3 Result**: 40 additional errors eliminated ✓

---

## Critical Fix Strategies

### Strategy 1: Optional Type Handling

When you see: `Optional<T> cannot be converted to T`

**Solution 1 - Unwrap it**:

```java
Optional<String> opt = ...;
String value = opt.orElse("");  // or .orElse(null) or .orElseThrow()
```

**Solution 2 - Keep it as Optional**:

```java
Optional<String> value = something.getOptional();
```

### Strategy 2: Method Override Errors

When you see: `method does not override or implement a method from a supertype`

**Solution**: Add `@Override` annotation above method

```java
@Override
public String methodName() {
    ...
}
```

### Strategy 3: Missing Symbol Errors

When you see: `cannot find symbol`

**Debugging Steps**:
1. Check if method exists in imported classes
2. Verify import statements
3. Search for similar method names (possible rename)
4. Check if dependency was removed/refactored

### Strategy 4: Stream Conversion

When you see: `Stream<T> cannot be converted to Collection<T>`

**Solution**:

```java
// Add collector
Collection<Integer> result = stream.collect(Collectors.toList());
// or
Collection<Integer> result = stream.collect(Collectors.toSet());
```

### Strategy 5: Exception Conversion

When you see: `Exception cannot be converted to String`

**Solution**:

```java
// Use getMessage()
String msg = exception.getMessage();
// or toString()
String msg = exception.toString();
```

---

## File Paths Reference

```
projects/sitemanage/src/main/java/com/percussion/
├── linkmanagement/service/impl/PSManagedLinkService.java
├── pagemanagement/
│   ├── service/impl/
│   │   ├── PSLinkableAsset.java
│   │   ├── PSPageService.java
│   │   ├── PSPageCatalogService.java
│   │   └── PSMockDataForUnassignedPages.java
│   ├── dao/impl/PSTemplateDao.java
│   ├── assembler/
│   │   ├── PSPageAssemblyContextFactory.java
│   │   └── impl/PSResourceInstanceHelper.java
├── pathmanagement/service/impl/PSPathItemService.java
└── share/dao/impl/PSContentItemDao.java
```

---

## Compilation Test Points

After each phase, run:

```bash
./mvnw clean compile -pl projects/sitemanage -am
```

Expected progression:
- Phase 0: 200 errors
- Phase 1: 154 errors (46 fixed)
- Phase 2: 112 errors (88 fixed total)
- Phase 3: 72 errors (130 fixed total)

---

## Pro Tips

1. **Use IDE Find & Replace** for pattern matching
   - Find: `dao\.findLinkByLinkId\([^)]*\)([^;])`
   - Replace: `$0.orElse(null)$1`
2. **Batch add @Override annotations** using IDE Quick Fix
3. **Test incrementally** - Don't fix all 200 at once
4. **Keep the detailed plan handy** for Phase 3 investigation
5. **Document any refactorings found** - they may affect other modules

