# PR Reference Corrections for v8.1.6 Release Notes

This document provides a detailed mapping of PR references that need to be corrected in the v8.1.6 release notes.

## PRs That Need to be REMOVED or Marked as ROLLED BACK

### PR #405 - MyFaces Update (ROLLED BACK)

- **Original claim:** "Bump myfaces.version from 1.1.8 to 3.0.3"
- **Actual status:** ROLLED BACK
- **Current deployed version:** 2.3.11
- **Reason:** MyFaces 3.x requires Java 11+, incompatible with JDK 1.8.0
- **Action:** Either remove this PR reference or add "ROLLED BACK" notation

### PR #412 - Shindig Update (ROLLED BACK)

- **Original claim:** "Bump shindig.version from 1.1-BETA5-incubating to 3.0.0-beta4"
- **Actual status:** ROLLED BACK
- **Current deployed version:** 1.1-BETA5-incubating (unchanged)
- **Reason:** Shindig 3.x requires Java 11+, incompatible with JDK 1.8.0
- **Action:** Either remove this PR reference or add "ROLLED BACK" notation

## PRs That Need Version CORRECTIONS

### PR #283 - PDFBox Update (VERSION INCORRECT)

- **Original claim:** "Bump pdfbox.version to 3.0.6"
- **Actual deployed version:** 2.0.30
- **Correct description:** "Bump pdfbox.version from 2.0.24 to 2.0.30"
- **Note:** PDFBox 3.x requires Java 11+. Version 2.0.30 is the latest 2.x release
- **Action:** Update the version number in the PR description from "3.0.6" to "2.0.30"

### PR #63 - OWASP CSRF Guard (VERSION INCORRECT)

- **Original claim:** "Update to 4.5.0-jakarta"
- **Actual deployed version:** 4.5.0 (non-jakarta variant)
- **Correct description:** "Update owasp.csrfguard.version to 4.5.0"
- **Note:** The jakarta variant requires Java 11+
- **Action:** Remove "-jakarta" suffix from version in PR description

### PR #103 - Jackson Update (NEEDS MORE PRECISION)

- **Original claim:** "Update to 2.20"
- **Actual deployed version:** 2.20.1
- **Correct description:** "Update jackson.version to 2.20.1"
- **Action:** Update version to be more specific: "2.20.1" instead of "2.20"

### PR #474 vs PR #511 - ICU4J (CONFLICTING INFORMATION)

- **PR #474 claim:** "Update to 78.1"
- **PR #511 claim:** "Bump com.ibm.icu:icu4j from 74.2 to 77.1"
- **Actual deployed version:** 77.1
- **Resolution:** PR #511 is the final state; PR #474 was superseded or rolled back
- **Action:** Remove PR #474 reference or note that PR #511 supersedes it

## PRs That Are CORRECT (No Changes Needed)

These PRs are accurately reflected in the current release notes:

### Already Correctly Noted as Reverted:

- **PR #416** - commons-digester revert ✓
- **PR #303** - spotless-maven-plugin revert ✓
- **PR #132** - cxf.version revert ✓
- **PR #500** - phloc-commons revert ✓
- **PR #499** - javax.jcr revert ✓

### Correctly Noted Updates:

- **jetty.version:** 9.4.58.v20250814 ✓
- **rome.version:** 2.1.0 ✓
- **fop.version:** 2.11 ✓
- **tika.version:** 2.9.4 ✓
- **json.version:** 20251224 ✓
- **cxf.version:** 3.5.11 (final after revert) ✓

## Verification Method

All versions were verified against:
- **File:** `/pom.xml` (lines 72-218, properties section)
- **Commit:** 0a58214c1b6378f07dec0cad2c868c09c7da2cc9
- **Branch:** development-8.1.x
- **ICU4J specifically verified in:** `/modules/perc-security-utils/pom.xml`

## Recommended Release Notes Update Strategy

1. **Remove or mark as rolled back:** PR #405 (MyFaces), PR #412 (Shindig)
2. **Correct version numbers:** PR #283 (PDFBox: 2.0.30 not 3.0.6), PR #63 (CSRF Guard: 4.5.0 not 4.5.0-jakarta), PR #103 (Jackson: 2.20.1 not 2.20)
3. **Clarify ICU4J:** Use PR #511 (77.1) as the final version, remove or supersede PR #474
4. **Add a note:** "Several dependency updates were rolled back to maintain Java 8 compatibility"

## Template Text for Release Notes

You can add this paragraph to the release notes:

```
**Note on Dependency Updates:** Several attempted dependency upgrades to major version 3.x 
were rolled back because they require Java 11 or higher. This release maintains full 
compatibility with JDK 1.8.0 while providing the latest security updates and bug fixes 
available for Java 8-compatible versions of our dependencies.
```

