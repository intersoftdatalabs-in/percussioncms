# Java Compiler Warnings Fix & Log4j2 Parameterized Logging Plan

## Overview

This plan addresses fixing all Java compiler warnings in the Percussion CMS codebase while updating logging to use parameterized Log4j2 format. The work is divided into phases to enable incremental progress by agents.

## Current State Analysis

### Compiler Configuration (Root pom.xml)

- Uses `maven-compiler-plugin` version 3.14.1
- Java target: 21
- Enabled lint flags: `-Xlint` (all warnings)
- No current suppression mechanism for internal deprecation warnings

### Warning Categories Found

|               Category                | Count |                   Example Source                    |
|---------------------------------------|-------|-----------------------------------------------------|
| Deprecation (3rd party)               | ~90+  | StringEscapeUtils, StringUtils.equals(CharSequence) |
| Deprecation (internal com.percussion) | 35+   | Various legacy classes                              |
| unchecked                             | 102+  | Raw type usage in collections                       |
| rawtypes                              | 27+   | Generic type omissions                              |
| serial                                | ~20+  | Missing serialVersionUID                            |
| this-escape                           | ~10+  | Object construction issues                          |
| Path warnings                         | ~50+  | Missing JAR files in classpath                      |

### Logging Analysis

|                 Framework                 | File Count |                    Status                     |
|-------------------------------------------|------------|-----------------------------------------------|
| Log4j2 (org.apache.logging.log4j)         | ~250       | Modern - needs parameterized conversion       |
| Apache Commons Logging                    | ~59        | Legacy - needs migration to Log4j2            |
| Non-parameterized logging (string concat) | ~90+       | Needs update to `logger.debug("msg {}", var)` |

---

## Phase 1: Compiler Configuration Updates

### 1.1 Configure Deprecation Suppression for Internal Code

**Goal:** Suppress deprecation warnings only for `com.percussion` packages while keeping 3rd party deprecation warnings visible.

**Implementation:**
- Update root `pom.xml` compiler plugin configuration:

```xml
<compilerArgs>
  <arg>-proc:none</arg>
  <arg>-Xlint</arg>
  <arg>-Xlint:-dep</arg>  <!-- Disable all deprecation warnings -->
</compilerArgs>
```

- Add `@SuppressWarnings("deprecation")` at class-level for internal deprecation usage where needed (existing 35+ annotations already in place)

- **Alternative approach for finer control:** Use `-Xlint:-dep` globally, then add `@SuppressWarnings("deprecation")` ONLY where 3rd party deprecations are used in com.percussion code (to surface those)

### 1.2 Enable Specific Warning Categories

Add these compiler arguments to address other warnings:

```xml
<compilerArgs>
  <arg>-Xlint:serial</arg>   // Warn about missing serialVersionUID
  <arg>-Xlint:this-escape</arg>  // Warn about this escape in constructors
</compilerArgs>
```

---

## Phase 2: Fix 3rd Party Deprecation Warnings

### 2.1 StringEscapeUtils Deprecations

**Files affected:** ~1 major file identified
- `projects/sitemanage/src/main/java/com/percussion/pagemanagement/assembler/impl/PSLegacyLinkGenerator.java`

**Action:** Replace `org.apache.commons.lang3.StringEscapeUtils` with:
- `org.apache.commons.text.StringEscapeUtils` (new package)
- Or implement inline escaping methods for HTML/XML

**Dependency needed:** Add to parent pom.xml if not present:

```xml
<dependency>
  <groupId>org.apache.commons</groupId>
  <artifactId>commons-text</artifactId>
  <version>1.12.0</version>
</dependency>
```

### 2.2 StringUtils Deprecations

**Files affected:**
- `projects/sitemanage/src/main/java/com/percussion/activity/data/PSTrafficDetailsRequest.java`

**Action:** Replace deprecated `StringUtils.equals(CharSequence, CharSequence)` with:
- `StringUtils.equals(CharSequence, CharSequence)` from commons-text
- Or use standard `CharSequence.compare(CharSequence, CharSequence)`

### 2.3 Other 3rd Party Deprecations

Scan and fix any other 3rd party deprecation warnings by:
1. Upgrading dependency versions in parent pom.xml
2. Replacing deprecated APIs with current alternatives

---

## Phase 3: Fix Internal Deprecation Warnings

### 3.1 Strategy

Keep existing `@SuppressWarnings("deprecation")` annotations on internal code - these indicate known deprecated APIs used for backward compatibility. Add explanatory comments where helpful.

### 3.2 Files with Internal Deprecation Warnings

These files already have appropriate suppressions - verify they're sufficient:
- `system/src/main/java/com/percussion/workflow/PSConnectionMgr.java`
- `system/src/main/java/com/percussion/server/webservices/PSServerFolderProcessor.java`
- `system/src/main/java/com/percussion/cms/objectstore/PSAaRelationship.java`
- `system/src/main/java/com/percussion/cms/objectstore/PSContentType.java`
- `system/src/main/java/com/percussion/content/PSContentFactory.java`
- And 25+ more files...

---

## Phase 4: Fix Other Warning Categories

### 4.1 Serial Warning Fixes

**Pattern:** Add `serialVersionUID` to serializable classes

**Files needing attention:**
- `PSContentActivityList`, `PSEffectivenessList`, `PSEffectivenessRequest`
- `PSTrafficDetails`, `PSContentTrafficRequest`
- `PSItemProperties`, `PSDataItemSummary`

**Action:** Add to each affected class:

```java
private static final long serialVersionUID = 1L;
```

### 4.2 This-Escape Fixes

**Pattern:** Fix object construction to avoid 'this' escape

**Files needing attention:**
- `projects/sitemanage/src/main/java/com/percussion/activity/data/PSContentActivity.java`
- `projects/sitemanage/src/main/java/com/percussion/activity/data/PSContentTraffic.java`

**Action:** Use builder pattern or move field initialization after super() call

### 4.3 Unchecked/Rawtypes Fixes

**Pattern:** Add generic type parameters to collections

**Strategy:** These are widespread (~130 instances). Prioritize:
1. Public API classes
2. Service layer classes
3. Keep suppressions for legacy code with complex generics

---

## Phase 5: Logging Migration

### 5.1 Migrate Apache Commons Logging to Log4j2

**Files affected:** ~59 files using `org.apache.commons.logging.Log`

**Migration Pattern:**

```java
// Before
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
private static final Log LOG = LogFactory.getLog(MyClass.class);

// After  
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
private static final Logger LOG = LogManager.getLogger(MyClass.class);
```

**Priority Modules:**
1. `modules/segmentation-rx` (~10 files)
2. `modules/p13n-api` (~12 files)
3. `modules/ContentUI` (~5 files)
4. `deliverytiersuite/delivery-tier-suite/p13n-ds` (~10 files)
5. `system/workflow` (~4 files)

### 5.2 Convert to Parameterized Logging

**Pattern:** Convert string concatenation to SLF4J/Log4j2 parameterized style

```java
// Before (string concatenation - EVALUATED EVEN WHEN LOG LEVEL IS OFF)
logger.debug("Processing item with id: " + itemId);
logger.info("message " + var1 + " more " + var2);

// After (parameters lazy-evaluated only if log level enabled)
logger.debug("Processing item with id: {}", itemId);
logger.info("message {} more {}", var1, var2);

// With exception (already correct in most cases)
logger.error("Failed to process", exception);
```

**Files needing updates:** ~90 files identified (see search results)

**Priority Approach:**
1. First: Migrate Commons Logging files (Phase 5.1) - combines both migrations
2. Second: Update existing Log4j2 files with string concatenation
3. Focus on ERROR and WARN level first (production impact)
4. Then DEBUG/TRACE (development logging)

---

## Phase 6: Build Verification

### 6.1 Verification Steps

After each phase, run:

```bash
./mvnw clean compile 2>&1 | grep -E "(WARNING|ERROR)" | head -50
```

### 6.2 Expected Outcome

- Zero deprecation warnings from 3rd party libraries
- Zero compilation errors
- Warning count reduced from ~200+ to ~50 (mostly serial/this-escape in data classes)

---

## Execution Order for Agents

### Agent Task 1: Compiler Configuration

1. Update root pom.xml compiler args
2. Add commons-text dependency
3. Test build

### Agent Task 2: Fix 3rd Party Deprecations

1. Find all 3rd party deprecation warnings
2. Replace StringEscapeUtils usage
3. Replace deprecated StringUtils methods
4. Verify clean build for deprecation warnings

### Agent Task 3: Logging Migration (Commons Logging → Log4j2)

1. Migrate each affected module
2. Convert to parameterized style
3. Verify logging works

### Agent Task 4: Parameterized Logging Updates

1. Update remaining Log4j2 files with string concatenation
2. Focus on ERROR/WARN first

### Agent Task 5: Serial/This-Escape Fixes

1. Add serialVersionUID to serializable classes
2. Fix this-escape constructors

---

## Notes

- Internal deprecation warnings for `com.percussion` code should be left suppressed via `@SuppressWarnings("deprecation")` annotations
- All changes should maintain backward compatibility
- Run `./mvnw spotless:apply` then `./mvnw spotless:check` after changes (apply first, check second — per project guidelines)
- Unit tests must continue to pass

---

## Related Documentation

- [Log4j2 Parameterized Logging](https://logging.apache.org/log4j/2.x/manual/messages.html)
- [Apache Commons Text Migration](https://commons.apache.org/proper/commons-text/)
- Parent POM: `pom.xml` lines 2083-2097 for compiler configuration

