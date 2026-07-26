# Code Scanning Alerts for o/r — fixture/bad

# Companion to scripts/test-fixtures/triage-bad.md; same alert count (4) so the

# row-count check fires only on the secondary checks (empty notes / unknown

# module_owner).

State filter: open
Generated: 2026-07-21T00:00:00Z (UTC)

- **Alert #1** — `java/ssrf` (critical, CodeQL)
  - **Location:** modules/extensions-main/src/main/java/com/percussion/extensions/general/PSProxyQueryResource.java:42
- **Alert #2** — `js/xss-through-dom` (high, CodeQL)
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercDataTable/x.js:10
- **Alert #3** — `java/implicit-cast-in-compound-assignment` (medium, CodeQL)
  - **Location:** deliverytiersuite/.../feeds/.../PSFeedServicePerformanceTest.java:100
- **Alert #4** — `java/weak-cryptographic-algorithm` (high, CodeQL)
  - **Location:** modules/some-unknown-module/x.java:1

