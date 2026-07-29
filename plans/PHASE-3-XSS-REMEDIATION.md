# Phase 3: XSS (CWE-79) Remediation Plan

**Status**: In Progress
**Total Vulnerabilities**: 23 across 11 files
**Test Suite**: XSSValidation - 13/13 tests passing ✅
**Utility Created**: `XSSValidation.java` with 6 escaping/validation methods

## Overview

Cross-site scripting (CWE-79) vulnerabilities occur when user-provided data is returned in REST API responses without proper HTML/XML escaping. This allows attackers to inject malicious scripts that execute in the browser.

### Security Utilities Framework

Three security utilities have been created in `modules/perc-security-utils/src/main/java/com/percussion/security/validation/`:

1. **URLValidation.java** (Phase 1a - SSRF) - ✅ Completed (22/22 tests)
2. **SerializationValidation.java** (Phase 1c - Unsafe Deserialization) - ✅ Completed (10/10 tests)
3. **XSSValidation.java** (Phase 3 - XSS) - ✅ Created (13/13 tests) - **NEW**

### XSSValidation Methods

```java
// HTML context escaping - use for REST API responses
public static String escapeHtml(String input)

// XML context escaping - use for XML output
public static String escapeXml(String input)

// JavaScript context escaping - use for JSON responses
public static String escapeJavaScript(String input)

// CSV injection prevention
public static String escapeCsv(String input)

// HTML tag removal (aggressive sanitization)
public static String stripHtmlTags(String input)

// Pattern-based detection of malicious payloads
public static boolean containsSuspiciousPatterns(String input)
```

## Vulnerable Files and Remediation

### File 1: PSFeedService.java

**Location**: [rest/modules/rest-services/src/main/java/com/percussion/rest/](rest/modules/rest-services/src/main/java/com/percussion/rest/PSFeedService.java)
**Vulnerabilities**: 1 (line 418)
**Severity**: HIGH

#### Vulnerable Code (line 418)

```java
// Before: readExternalFeed returns unescaped feeds
return feeds;
```

#### Fix Pattern

Use `XSSValidation.escapeHtml()` when returning user-influenced data:

```java
return feeds.stream()
    .map(feed -> {
        feed.setTitle(XSSValidation.escapeHtml(feed.getTitle()));
        feed.setDescription(XSSValidation.escapeHtml(feed.getDescription()));
        return feed;
    })
    .collect(Collectors.toList());
```

#### Test Case

Create test for ensuring feed titles/descriptions are HTML-safe.

---

### File 2: PSMetadataRestService.java

**Location**: [rest/modules/rest-services/src/main/java/com/percussion/rest/PSMetadataRestService.java](rest/modules/rest-services/src/main/java/com/percussion/rest/PSMetadataRestService.java)
**Vulnerabilities**: 1 (line 480)
**Severity**: HIGH

#### Vulnerable Code (line 480)

REST response returning unescaped metadata.

#### Fix Pattern

Wrap metadata values with `XSSValidation.escapeHtml()`:

```java
metadata.put("value", XSSValidation.escapeHtml(rawValue));
```

---

### File 3: ItemRestServiceImpl.java

**Location**: [rest/modules/rest-services/src/main/java/com/percussion/rest/ItemRestServiceImpl.java](rest/modules/rest-services/src/main/java/com/percussion/rest/ItemRestServiceImpl.java)
**Vulnerabilities**: 6 (lines 776, 797, 1859, 1861, 1944, 2024)
**Severity**: HIGH (Most impacted file)

#### Vulnerable Patterns

**Line 776-800**: updateItems() method serializes item data

```java
// Item fields returned in REST response without escaping
item.setName(itemData.getName());        // Should escape
item.setDescription(itemData.getDesc()); // Should escape
```

**Line 1859, 1861**: createItem() - same pattern

```java
newItem.setTitle(itemData.getTitle()); // Should escape
```

**Line 1944**: updateItem() - field assignment
**Line 2024**: Another field update in create/update flow

#### Fix Pattern

For all item field assignments from REST input:

```java
item.setName(XSSValidation.escapeHtml(itemData.getName()));
item.setDescription(XSSValidation.escapeHtml(itemData.getDescription()));
item.setTitle(XSSValidation.escapeHtml(itemData.getTitle()));
```

#### Implementation Strategy

1. Identify all REST input fields in ItemRestServiceImpl
2. Wrap names, titles, descriptions, labels, and text-based fields
3. Use `XSSValidation.escapeHtml()` for HTML contexts
4. Create unit test covering all 6 vulnerable lines

---

### File 4: PSAssetRestService.java

**Location**: [rest/modules/rest-services/src/main/java/com/percussion/rest/PSAssetRestService.java](rest/modules/rest-services/src/main/java/com/percussion/rest/PSAssetRestService.java)
**Vulnerabilities**: 3 (lines 234, 485, 531)
**Severity**: HIGH

#### Vulnerable Patterns

**Line 234**: Asset file name/description processing
**Line 485**: Asset metadata return
**Line 531**: Asset property assignment

#### Fix Pattern

```java
asset.setFileName(XSSValidation.escapeHtml(fileName));
asset.setDescription(XSSValidation.escapeHtml(description));
response.put("metadata", XSSValidation.escapeHtml(metadata));
```

---

### File 5: PSDashboardService.java

**Location**: [rest/modules/rest-services/src/main/java/com/percussion/rest/PSDashboardService.java](rest/modules/rest-services/src/main/java/com/percussion/rest/PSDashboardService.java)
**Vulnerabilities**: 1 (line 80)
**Severity**: MEDIUM (Error message exposure - partial overlap with Phase 1d)

#### Vulnerable Code (line 80)

```java
// Error thrown with user-influenced message
throw new WebApplicationException("Dashboard error: " + userInput);
```

#### Fix Pattern

This overlaps with Phase 1d (Error Exposure). Implementation:

```java
// Option 1: Generic error message (preferred)
throw new WebApplicationException("Dashboard processing failed");

// Option 2: If specific context needed, escape the input
String safeInput = XSSValidation.escapeHtml(userInput);
throw new WebApplicationException("Dashboard error: " + safeInput);
```

---

### File 6: PSUserProfileRestService.java

**Location**: [rest/modules/rest-services/src/main/java/com/percussion/rest/PSUserProfileRestService.java](rest/modules/rest-services/src/main/java/com/percussion/rest/PSUserProfileRestService.java)
**Vulnerabilities**: 1 (line 49)
**Severity**: HIGH

#### Vulnerable Code (line 49)

User profile field returned in REST response without escaping.

#### Fix Pattern

```java
profile.setBio(XSSValidation.escapeHtml(userInput.getBio()));
profile.setDisplayName(XSSValidation.escapeHtml(userInput.getDisplayName()));
```

---

### File 7: PSSiteimprove.java

**Location**: [extensions-siteimprove/src/main/java/com/percussion/siteimprove/PSSiteimprove.java](extensions-siteimprove/src/main/java/com/percussion/siteimprove/PSSiteimprove.java)
**Vulnerabilities**: 1 (line 198)
**Severity**: HIGH

#### Fix Pattern

Escape site improvement metric/status values before returning.

---

### File 8: PSPageRestService.java

**Location**: [rest/modules/rest-services/src/main/java/com/percussion/rest/PSPageRestService.java](rest/modules/rest-services/src/main/java/com/percussion/rest/PSPageRestService.java)
**Vulnerabilities**: 1 (line 344)
**Severity**: HIGH

#### Fix Pattern

```java
page.setTitle(XSSValidation.escapeHtml(pageData.getTitle()));
page.setMetaDescription(XSSValidation.escapeHtml(pageData.getMetaDescription()));
```

---

### File 9: PSRoleService.java

**Location**: [rest/modules/rest-services/src/main/java/com/percussion/rest/PSRoleService.java](rest/modules/rest-services/src/main/java/com/percussion/rest/PSRoleService.java)
**Vulnerabilities**: 1 (line 118)
**Severity**: MEDIUM

#### Fix Pattern

```java
role.setDescription(XSSValidation.escapeHtml(roleData.getDescription()));
role.setName(XSSValidation.escapeHtml(roleData.getName()));
```

---

### File 10: PSSiteDataRestService.java

**Location**: [rest/modules/rest-services/src/main/java/com/percussion/rest/PSSiteDataRestService.java](rest/modules/rest-services/src/main/java/com/percussion/rest/PSSiteDataRestService.java)
**Vulnerabilities**: 4 (lines 110, 127, 186, 209)
**Severity**: HIGH

#### Vulnerable Patterns

**Lines 110, 127**: Site metadata/properties

```java
siteData.setName(XSSValidation.escapeHtml(input.getName()));
siteData.setUrl(XSSValidation.escapeHtml(input.getUrl()));
```

**Lines 186, 209**: Additional site property updates

```java
siteData.setDomain(XSSValidation.escapeHtml(domain));
siteData.setLabel(XSSValidation.escapeHtml(label));
```

---

### File 11: PSUserService.java

**Location**: [rest/modules/rest-services/src/main/java/com/percussion/rest/PSUserService.java](rest/modules/rest-services/src/main/java/com/percussion/rest/PSUserService.java)
**Vulnerabilities**: 3 (lines 493, 743, 806)
**Severity**: HIGH

#### Vulnerable Patterns

**Line 493**: User creation

```java
user.setDisplayName(XSSValidation.escapeHtml(userData.getDisplayName()));
user.setEmail(XSSValidation.escapeHtml(userData.getEmail()));
```

**Lines 743, 806**: User update operations

```java
user.setFirstName(XSSValidation.escapeHtml(userData.getFirstName()));
user.setLastName(XSSValidation.escapeHtml(userData.getLastName()));
user.setTitle(XSSValidation.escapeHtml(userData.getTitle()));
```

---

## Implementation Strategy

### Approach: Progressive Escaping

1. **Import XSSValidation** at the top of each affected file:

   ```java
   import com.percussion.security.validation.XSSValidation;
   ```
2. **Identify REST Input Sources**:
   - Method parameters from @RequestBody/@RequestParam
   - User-provided fields in form submissions
   - Query parameters passed to API endpoints
3. **Apply Escaping at Assignment**:
   - Wrap user input immediately when assigning to business objects
   - Example: `item.setName(XSSValidation.escapeHtml(inputName))`
4. **Context Selection**:
   - **HTML responses**: Use `escapeHtml()` (default)
   - **JSON responses**: Use `escapeJavaScript()` for embedded JS contexts
   - **XML output**: Use `escapeXml()`
   - **CSV export**: Use `escapeCsv()`
5. **Testing**:
   - Create unit test for each file
   - Test with XSS payloads: `<script>alert(1)</script>`, `onerror=`, `javascript:`, etc.
   - Verify escaped output contains safe entities (`&lt;`, `&gt;`, `&amp;`)

### Testing Pattern

```java
@Test
@DisplayName("Should escape XSS payloads in item creation")
void testCreateItemWithXSSPayload() {
    ItemInput itemData = new ItemInput();
    itemData.setName("<img onerror='alert(1)'>");

    Item created = service.createItem(itemData);

    // Verify the dangerous characters are escaped
    assertFalse(created.getName().contains("<img"));
    assertTrue(created.getName().contains("&lt;"));
}
```

---

## Completion Checklist

### Phase 3 Completion Tasks

- [ ] PSFeedService.java (1 fix)
- [ ] PSMetadataRestService.java (1 fix)
- [ ] ItemRestServiceImpl.java (6 fixes + unit tests)
- [ ] PSAssetRestService.java (3 fixes + unit tests)
- [ ] PSDashboardService.java (1 fix - coordinate with Phase 1d generic messages)
- [ ] PSUserProfileRestService.java (1 fix)
- [ ] PSSiteimprove.java (1 fix)
- [ ] PSPageRestService.java (1 fix)
- [ ] PSRoleService.java (1 fix)
- [ ] PSSiteDataRestService.java (4 fixes + unit tests)
- [ ] PSUserService.java (3 fixes + unit tests)

### Validation Steps

1. **Code Review**:
   - Verify all 23 vulnerabilities have escaping applied
   - Check that user input is escaped at assignment point
   - Confirm no double-escaping occurs
2. **Testing**:
   - All 11 affected files have new unit tests
   - Tests cover XSS payload injection scenarios
   - All tests pass (expected: ~15-20 new tests)
3. **CodeQL Re-scan**:
   - Run: `/mvnw codeql database create --language=java --source-root=/home/nate/projects/percussioncms database`
   - Verify CWE-79 (XSS) alerts reduced from 23 to 0
   - Check for any new alerts introduced
4. **Build Verification**:
   - `./mvnw clean test -DskipITs=true`
   - All tests pass
   - No new compiler warnings

---

## Progress Summary

|   Phase   |           Vulnerability            | Files  |      Status      |   Tests   |
|-----------|------------------------------------|--------|------------------|-----------|
| 1a        | SSRF (CWE-918)                     | 4      | ✅ Complete       | 22/22 ✓   |
| 1b        | SQL Injection (CWE-89)             | 1      | ✅ Complete       | -         |
| 1c        | Unsafe Deserialization (CWE-502)   | 3      | ✅ Complete       | 10/10 ✓   |
| 1d        | Error Exposure (CWE-209)           | 3      | ✅ Complete       | -         |
| 2         | ZipSlip/Path Injection (CWE-22/23) | 1      | ✅ Complete       | 4/4 ✓     |
| 3         | XSS (CWE-79)                       | 11     | 🔄 In Progress   | 13/13 ✓*  |
| **TOTAL** | **6 CWE types**                    | **23** | **45% Complete** | **49/49** |

*XSSValidation utility tests passing; Phase 3 implementation tests pending

---

## Next Steps

1. **Immediate**: Begin implementing fixes in ItemRestServiceImpl.java (6 vulnerabilities - most impacted)
2. **Follow-up**: PSAssetRestService.java (3 vulnerabilities)
3. **Then**: PSSiteDataRestService.java (4 vulnerabilities), PSUserService.java (3 vulnerabilities)
4. **Finally**: Single-violation files (7 remaining)

## References

- **OWASP CWE-79**: https://owasp.org/www-community/attacks/xss/
- **Apache Commons Text StringEscapeUtils**: https://commons.apache.org/proper/commons-text/javadocs/api-release/org/apache/commons/text/StringEscapeUtils.html
- **Secure Coding Standards**: Follow OWASP Top 10 A03:2021 Injection

