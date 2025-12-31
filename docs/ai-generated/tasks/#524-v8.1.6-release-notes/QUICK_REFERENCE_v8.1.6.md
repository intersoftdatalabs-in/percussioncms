# Quick Reference: Dependency Versions for v8.1.6

## Use this as a quick lookup when updating the GitHub release draft

### ✅ Correct Versions (Use These)

| Dependency | Version | Status |
|------------|---------|--------|
| jackson.version | 2.20.1 | ✅ Deployed |
| jetty.version | 9.4.58.v20250814 | ✅ Deployed |
| tika.version | 2.9.4 | ✅ Deployed |
| json.version | 20251224 | ✅ Deployed |
| rome.version | 2.1.0 | ✅ Deployed |
| fop.version | 2.11 | ✅ Deployed |
| cxf.version | 3.5.11 | ✅ Deployed |
| pdfbox.version | 2.0.30 | ✅ Deployed |
| owasp.csrfguard.version | 4.5.0 | ✅ Deployed |
| com.ibm.icu:icu4j | 77.1 | ✅ Deployed |

### 🔄 Java 8 Compatible Versions (Unchanged)

| Dependency | Version | Status |
|------------|---------|--------|
| myfaces.version | 2.3.11 | 🔄 Not upgraded (3.x needs Java 11+) |
| shindig.version | 1.1-BETA5-incubating | 🔄 Not upgraded (3.x needs Java 11+) |

### ❌ Incorrect Versions in Draft (DO NOT USE)

| What Draft Says | What's Actually Deployed | Fix Required |
|-----------------|-------------------------|--------------|
| myfaces 3.0.3 | 2.3.11 | Remove PR #405 or mark as rolled back |
| shindig 3.0.0-beta4 | 1.1-BETA5-incubating | Remove PR #412 or mark as rolled back |
| pdfbox 3.0.6 | 2.0.30 | Update PR #283 to show correct version |
| CSRF Guard 4.5.0-jakarta | 4.5.0 | Update PR #63 to remove "-jakarta" |
| jackson 2.20 | 2.20.1 | Update PR #103 to be more specific |
| icu4j 78.1 (PR #474) | 77.1 | Use PR #511 instead |

## Copy-Paste Friendly Corrections

### For PR Descriptions in Release Notes:

**Instead of:**
- ❌ "Bump myfaces.version from 1.1.8 to 3.0.3 (#405)"

**Use:**
- ✅ "~~Bump myfaces.version to 3.0.3 (#405) - ROLLED BACK~~ - Remains at 2.3.11 for Java 8 compatibility"

---

**Instead of:**
- ❌ "Bump shindig.version from 1.1-BETA5-incubating to 3.0.0-beta4 (#412)"

**Use:**
- ✅ "~~Bump shindig.version to 3.0.0-beta4 (#412) - ROLLED BACK~~ - Remains at 1.1-BETA5-incubating for Java 8 compatibility"

---

**Instead of:**
- ❌ "Bump pdfbox.version to 3.0.6 (#283)"

**Use:**
- ✅ "Bump pdfbox.version from 2.0.24 to 2.0.30 (#283)"

---

**Instead of:**
- ❌ "Update owasp.csrfguard.version to 4.5.0-jakarta (#63)"

**Use:**
- ✅ "Update owasp.csrfguard.version to 4.5.0 (#63)"

---

**Instead of:**
- ❌ "Update jackson.version to 2.20 (#103)"

**Use:**
- ✅ "Update jackson.version to 2.20.1 (#103)"

---

**Instead of:**
- ❌ "Bump com.ibm.icu:icu4j to 78.1 (#474)"

**Use:**
- ✅ "Bump com.ibm.icu:icu4j from 74.2 to 77.1 (#511)"

## Suggested Addition to Release Notes

Add this paragraph at the beginning:

```markdown
## Important Note on Java 8 Compatibility

This release maintains full compatibility with JDK 1.8.0 (Java 8). Several 
attempted dependency upgrades to version 3.x were rolled back because they 
require Java 11 or higher. All dependencies listed below are confirmed to 
work with Java 8 and include the latest security updates and bug fixes 
available for Java 8-compatible versions.
```

## Verification

All versions verified in:
- Main pom.xml (lines 72-218)
- Commit: 0a58214c1b6378f07dec0cad2c868c09c7da2cc9
- Branch: development-8.1.x
