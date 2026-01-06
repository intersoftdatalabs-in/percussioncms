# Release Notes for v8.1.6 (Corrected)

## Overview

This document contains the corrected release notes for v8.1.6 (tag: untagged-4aa88975603d6010e701). Several dependency updates mentioned in the original draft release notes were rolled back during development due to Java 8 compatibility issues. This document reflects the actual dependency versions deployed to the development-8.1.x branch.

## Important Note on Dependency Updates

Several dependency updates were attempted but rolled back to maintain compatibility with JDK 1.8.0. The versions listed below reflect what is actually deployed in the development-8.1.x branch.

## Dependency Updates

### Security Updates

#### OWASP CSRF Guard

- **Updated to:** 4.5.0 (not 4.5.0-jakarta as initially attempted)
- **Previous version:** (older version)
- **PR Reference:** #63 - Updated to reflect non-jakarta variant for Java 8 compatibility
- **Note:** The jakarta variant requires Java 11+, so the standard 4.5.0 version is used

### Core Framework Updates

#### Apache MyFaces

- **Current version:** 2.3.11
- **PR #405 - ROLLED BACK:** The attempted bump to 3.0.3 was rolled back
- **Reason:** MyFaces 3.x requires Java 11+ and is not compatible with JDK 1.8.0
- **Note:** Version 2.3.11 is the latest version compatible with Java 8

#### Apache Shindig

- **Current version:** 1.1-BETA5-incubating
- **PR #412 - ROLLED BACK:** The attempted bump to 3.0.0-beta4 was rolled back
- **Reason:** Shindig 3.x requires Java 11+ and is not compatible with JDK 1.8.0
- **Note:** Version 1.1-BETA5-incubating remains the deployed version for Java 8 compatibility

#### Apache PDFBox

- **Updated to:** 2.0.30
- **Previous version:** 2.0.24
- **PR #283:** Bump pdfbox.version from 2.0.24 to 2.0.30 (not 3.0.6)
- **Note:** PDFBox 3.x requires Java 11+. Version 2.0.30 is the latest 2.x release compatible with Java 8

### Data Processing Libraries

#### Jackson

- **Updated to:** 2.20.1
- **PR #103:** Updated jackson.version to 2.20.1 (more specific than "2.20")
- **Note:** This is a precise version update to the latest 2.20.x patch release

#### Apache Tika

- **Updated to:** 2.9.4 (final)
- **Note:** Successfully updated and deployed

#### Apache FOP

- **Updated to:** 2.11
- **Note:** Successfully updated and deployed

#### JSON

- **Updated to:** 20251224 (final)
- **Note:** Successfully updated and deployed with date-based versioning

#### Rome

- **Updated to:** 2.1.0
- **Note:** Successfully updated and deployed

### Infrastructure Libraries

#### Eclipse Jetty

- **Updated to:** 9.4.58.v20250814
- **Note:** Successfully updated to the latest 9.4.x release compatible with Java 8

#### Apache CXF

- **Updated to:** 3.5.11 (after revert)
- **Note:** An attempted upgrade was reverted, and version 3.5.11 is the deployed version

#### ICU4J (International Components for Unicode)

- **Updated to:** 77.1
- **PR #511:** Bump com.ibm.icu:icu4j from 74.2 to 77.1
- **Note:** This supersedes the version mentioned in PR #474 (78.1)
- **Deployment location:** modules/perc-security-utils/pom.xml

## Previously Noted Reverts (Still Accurate)

The following dependency update attempts were correctly noted as reverted in the original release notes:

### Apache Commons Digester

- **PR #416 - Reverted:** ✓ Correctly noted as reverted

### Spotless Maven Plugin

- **PR #303 - Reverted:** ✓ Correctly noted as reverted

### Apache CXF (intermediate update)

- **PR #132 - Reverted:** ✓ Correctly noted, final version 3.5.11 deployed

### Phloc Commons

- **PR #500 - Reverted:** ✓ Correctly noted as reverted

### javax.jcr

- **PR #499 - Reverted:** ✓ Correctly noted as reverted

## Summary of Corrections

The following PRs mentioned in the original release notes need to be updated or removed:

1. **PR #405** - Remove or update: MyFaces was NOT upgraded to 3.0.3 (remains at 2.3.11)
2. **PR #412** - Remove or update: Shindig was NOT upgraded to 3.0.0-beta4 (remains at 1.1-BETA5-incubating)
3. **PR #283** - Update: PDFBox was upgraded to 2.0.30 (not 3.0.6)
4. **PR #63** - Update: OWASP CSRF Guard was upgraded to 4.5.0 (not 4.5.0-jakarta)
5. **PR #103** - Update: Jackson was upgraded to 2.20.1 (be more specific than "2.20")
6. **PR #474** - Superseded by PR #511: ICU4J final version is 77.1 (not 78.1)

## Java 8 Compatibility

All dependencies in this release are compatible with JDK 1.8.0 (Java 8). Several attempted upgrades to newer major versions were rolled back because they required Java 11 or higher, which would break compatibility with the development-8.1.x branch's Java 8 requirement.

## Commit Reference

- **Final commit:** 0a58214c1b6378f07dec0cad2c868c09c7da2cc9

## GitHub Release

- **Draft Release:** https://github.com/intersoftdatalabs-in/percussioncms/releases/tag/untagged-4aa88975603d6010e701

---

**Note to Release Manager:** Please update the draft release body text with these corrected dependency versions before publishing the release.
