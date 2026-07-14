# Triage inventory (good fixture) — for verify-triage-inventory.sh tests.
# Schema follows contracts/C1.

# Code Scanning Triage Inventory — fixture/good
# Total open alerts: 4

| # | alert_id | rule_id | severity | file_path | module_owner | disposition (candidate) | target_action | target_milestone | linked_pr | notes |
|---|----------|---------|----------|-----------|--------------|-------------------------|---------------|------------------|-----------|-------|
| 1 | 1 | `java/ssrf` | critical | `modules/extensions-main/src/main/java/com/percussion/extensions/general/PSProxyQueryResource.java` | `modules/extensions-main/` | `valid` | fix SSRF | `8.2-blocker` | 1234 | — |
| 2 | 2 | `js/xss-through-dom` | high | `WebUI/src/main/webapp/cm/widgets/PercDataTable/x.js` | `WebUI/` | `obsolete` | remove | `8.2-must-fix` | — | — |
| 3 | 3 | `java/implicit-cast-in-compound-assignment` | medium | `deliverytiersuite/delivery-tier-suite/feeds/src/test/java/com/percussion/delivery/feeds/PSFeedServicePerformanceTest.java` | `deliverytiersuite/delivery-tier-suite/feeds/` | `false-positive` | suppress | `accepted-risk` | — | Test perf micro-benchmark intentionally narrows long->int for timer accuracy. No production impact. |
| 4 | 4 | `java/weak-cryptographic-algorithm` | high | `modules/perc-legacy/src/main/java/com/percussion/legacy/security/deprecated/PSAesCBC.java` | `modules/perc-legacy/` | `accepted-risk` | document | `accepted-risk` | — | AES/CBC in legacy module requires JDK 8 baseline; AES/GCM is JDK 9+. Defer to 8.3 per the legacy-crypto epic. |
