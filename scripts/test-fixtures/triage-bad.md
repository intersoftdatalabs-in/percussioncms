# Triage inventory (bad fixture) — for verify-triage-inventory.sh tests.
# Contains: (1) empty notes on a false-positive row, (2) unknown module_owner.
# Schema follows contracts/C1.

| # | alert_id | rule_id | severity | file_path | module_owner | disposition (candidate) | target_action | target_milestone | linked_pr | notes |
|---|----------|---------|----------|-----------|--------------|-------------------------|---------------|------------------|-----------|-------|
| 1 | 1 | `java/ssrf` | critical | `modules/extensions-main/src/main/java/com/percussion/extensions/general/PSProxyQueryResource.java` | `modules/extensions-main/` | `valid` | fix SSRF | `8.2-blocker` | 1234 | — |
| 2 | 2 | `js/xss-through-dom` | high | `WebUI/src/main/webapp/cm/widgets/PercDataTable/x.js` | `WebUI/` | `obsolete` | remove | `8.2-must-fix` | — | — |
| 3 | 3 | `java/implicit-cast-in-compound-assignment` | medium | `deliverytiersuite/.../feeds/.../PSFeedServicePerformanceTest.java` | `deliverytiersuite/delivery-tier-suite/feeds/` | `false-positive` | suppress | `accepted-risk` | — |  |
| 4 | 4 | `java/weak-cryptographic-algorithm` | high | `modules/some-unknown-module/x.java` | `modules/some-unknown-module/` | `accepted-risk` | document | `accepted-risk` | — | AES/CBC legacy — needs JDK 8 baseline. |
