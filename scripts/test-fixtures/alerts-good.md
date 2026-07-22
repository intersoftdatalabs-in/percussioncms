# Code Scanning Alerts for o/r — fixture/good
# Companion to scripts/test-fixtures/triage-good.md; the open-alert count (4)
# matches the triage row count.

State filter: open
Generated: 2026-07-21T00:00:00Z (UTC)

- **Alert #1** — `java/ssrf` (critical, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-21T00:00:00Z
  - **URL:** https://gh/x/1
  - **Location:** modules/extensions-main/src/main/java/com/percussion/extensions/general/PSProxyQueryResource.java:42
  - **Message:** ssrf

- **Alert #2** — `js/xss-through-dom` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-21T00:00:00Z
  - **URL:** https://gh/x/2
  - **Location:** WebUI/src/main/webapp/cm/widgets/PercDataTable/x.js:10
  - **Message:** xss

- **Alert #3** — `java/implicit-cast-in-compound-assignment` (medium, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-21T00:00:00Z
  - **URL:** https://gh/x/3
  - **Location:** deliverytiersuite/delivery-tier-suite/feeds/src/test/java/com/percussion/delivery/feeds/PSFeedServicePerformanceTest.java:100
  - **Message:** cast

- **Alert #4** — `java/weak-cryptographic-algorithm` (high, CodeQL)
  - **Tool:** CodeQL
  - **State:** open
  - **Created:** 2026-07-21T00:00:00Z
  - **URL:** https://gh/x/4
  - **Location:** modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java:50
  - **Message:** crypto
