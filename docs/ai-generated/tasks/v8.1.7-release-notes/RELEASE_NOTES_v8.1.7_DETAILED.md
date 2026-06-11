# Release Notes for v8.1.7 (Detailed)

## Overview

PercussionCMS v8.1.7 continues the 8.1.x maintenance line with a focus on:
- Modernizing the administrative Category management experience
- Supporting the industry transition to Google Analytics 4
- Hardening the Delivery Tier (DTS) startup and runtime behavior
- Executing a long-planned major dependency upgrade (PDFBox 3.x)
- Cleaning up technical debt (deprecations, build performance, logging noise)

This release maintains strict compatibility with **JDK 1.8.0**.

**Base:** v8.1.6  
**Current development version:** 8.1.7-SNAPSHOT

## Major Feature Work

### 1. Admin Category UI Refactor & Fancytree Migration

Multiple coordinated PRs delivered a ground-up modernization of the Category administration tools:

- **#778, #781, #783**: Primary refactor of the Category editor UI. Switched core tree component from Dynatree to Fancytree.
- Fixed long-standing visual and interaction bugs:
  - Overlapping icons
  - Tree rendering and expansion issues
  - Move up/down, add child, save/cancel placement problems
  - Noisy/unnecessary logging
- **#772, #761, #758**: Additional Fancytree/Category browser fixes (node activation, serialization formatting, creation behavior).
- **#755**: Fixed missing bullets and Category List fancytree errors in default Percussion theme on published sites (#750, #753).

**Impact:** Significantly more reliable Category management for content architects and improved consistency with other modernized tree UIs in the product.

### 2. Google Analytics 4 (GA4) Migration (#756, #760)

- Core migration work to support GA4 properties and updated tracking.
- Related gadget and authentication handling improvements (including better error surfacing for Google auth issues).

### 3. Bulk Upload Gadget Hardening (#710, #727, #775, #776)

- Proper error messaging and handling when zero-byte files are uploaded.
- General stability and user feedback improvements for the bulk upload workflow.

### 4. Deprecation Signaling for 8.2 (#720, #722)

- Added UI affordances (lists, warnings) identifying widgets and gadgets that will be removed in the 8.2 release.
- Removed the legacy "Community" section from relevant UIs.
- This is part of a broader cleanup effort ahead of the next major version.

## Delivery Tier & Infrastructure Improvements

### DTS Startup & Logging Hygiene (#748, #764, #782)

- Replaced noisy `sqlCheck` preconditions with `indexExists` checks.
- Added missing opencsv dependency to eliminate `ClassNotFoundException` for `LoadDataChange`.
- Java 8 TLS/cipher configuration refinements and reduction of JUL bridge noise.
- General reduction of spurious warnings on DTS startup.

### Windows & Platform Fixes (#672, #736)

- Multiple Windows-specific DTS fixes.

### Other DTS/Email/Process Improvements

- **#735**: Much clearer error reporting + help text when the system email subsystem is not configured.
- **#738**: Process monitor fixes.

## Widget, Gadget & UI Polish

- **Auto List Widgets** (#749, #771, #762): Fixed rendering failures on published sites.
- **Google Setup Gadget** (#768, #773): Null pointer exception resolved.
- **Form Widget** (#658): Added proper autocomplete accessibility attributes on email-from field; version bump to 1.4.8.
- **Breadcrumb Widget** (#653): Added missing `navigation` ARIA role.
- **Footer Alignment** (#757, #763, #767): Fixed misalignment by using `min-height` on vspan region classes.
- **Error Handling** (#774): Prevented JSON payload leakage in `extractDefaultErrorMessage` when `fieldErrors` is an empty array.
- **Jackson** (#675, #676): Improved null handling and compatibility after dependency bumps.

## Build, Packaging & Tooling

- Packaging performance refactor (faster builds, missing Caja dashboard resource fix).
- Maven launcher temp directory handling.
- Many Dependabot-driven updates with Java 8 compatibility enforcement (some higher version attempts were rolled back, similar to the 8.1.6 cycle).
- Spotless and Checkstyle maintenance.

## Dependency Changes (Actual Deployed Versions)

### Significant Upgrades (compared to v8.1.6)

|      Library      |        v8.1.6         |      v8.1.7      |                       Notes                       |
|-------------------|-----------------------|------------------|---------------------------------------------------|
| Apache PDFBox     | 2.0.30                | **3.0.6**        | Major upgrade; required code changes (#571, #572) |
| Jackson           | 2.20.1                | **2.21.1**       | Includes annotations module                       |
| Apache Shiro      | 1.13.0 (some modules) | **2.1.0**        | Security/maintenance bump                         |
| JSoup             | 1.21.2                | **1.22.1**       |                                                   |
| SnakeYAML         | 2.5                   | **2.6**          |                                                   |
| Netty (netty-all) | 4.2.9.Final           | **4.2.10.Final** |                                                   |
| OpenCSV           | (not present)         | **5.12.0**       | New dependency for DTS fixes                      |
| JUnit Jupiter     | 5.14.1                | 5.14.3           |                                                   |
| Swagger           | 2.2.42                | 2.2.43           |                                                   |
| AWS SDK           | 1.12.796              | 1.12.797         |                                                   |
| Commons Codec     | 1.20.0                | 1.21.0           |                                                   |

### Held at Java 8 Compatible Versions (No Change)

- Apache MyFaces: **2.3.11** (3.x requires Java 11+)
- Apache Shindig: **1.1-BETA5-incubating** (3.x requires Java 11+)
- OWASP CSRF Guard: **4.5.0** (jakarta variant not used)
- Apache Tika: **2.9.4**
- Eclipse Jetty: **9.4.58.v20250814**
- Apache CXF: **3.5.11**
- Apache FOP: **2.11**
- JSON (org.json): **20251224**
- Rome: **2.1.0**
- ICU4J: **77.1**

### Notes on Previous Rollbacks

During the 8.1.7 development cycle there were additional Dependabot attempts at newer major versions (particularly around Jackson and build plugins) that were rolled back or capped to maintain Java 8 compatibility and build stability, consistent with the approach taken for v8.1.6.

## Requirements

- **Java:** JDK 1.8.0 (Java 8). Set `JAVA_HOME` to a Java 8 JDK (Amazon Corretto 8 recommended) before building or running.
- **Database:** Supported databases unchanged from prior 8.1.x releases.
- **Browser:** Modern evergreen browsers (Chrome, Edge, Firefox, Safari).

## Upgrade Notes

- Because PDFBox has moved from the 2.x to the 3.x line, review any custom code that directly uses PDFBox APIs (rare for most customers).
- Category administration behavior is improved but functionally equivalent; no data migration is required.
- If using Google Analytics integrations, review the new GA4 configuration options post-upgrade.
- Widgets/gadgets marked deprecated in the UI should be migrated off before the 8.2 release.

## Known Issues / Deprecations

- Several older widgets and gadgets are now explicitly flagged in the UI as deprecated and scheduled for removal in 8.2.
- Legacy "Community" functionality continues to be reduced.

## Contributors & References

Significant PRs in this cycle (partial list):

- Category UI / Fancytree: #755, #758, #761, #766, #772, #778, #781, #783
- GA4: #756, #760
- Bulk Upload: #710, #727, #775, #776
- DTS / Startup: #664, #748, #764, #782 (and related)
- Various: #653, #658, #673, #675, #676, #720, #722, #735, #738, #749, #757, #768, #771, #773, #774

Version bump: #526

**Commit range:** `v8.1.6` .. `HEAD` (approximately 93 commits on development-8.1.x at time of drafting).

---

**Internal Note:** This document is the detailed working copy. For the public/GitHub-facing version, use `RELEASE_NOTES_v8.1.7_GITHUB_BODY.md`.
