# CodeQL Suppression Index — intersoftdatalabs-in/percussioncms

**Branch**: `004-zero-code-scanning-alerts`
**Generated**: 2026-07-11 (initially empty; rows added per US4 closure)

Schema follows `specs/004-zero-code-scanning-alerts/contracts/README.md` C3. Every
inline `// codeql[rule-id]` suppression applied under this feature MUST have
exactly one row here, with `justification` matching the inline comment
verbatim. Path-level exclusions (`.github/codeql/codeql-config.yml`
`paths-ignore` / `query-filter` additions) MUST also have a row here with
`file_path = .github/codeql/codeql-config.yml`.

| alert_id | rule_id | file_path | line | justification | applied_on | applied_by | review_by |
|----------|---------|-----------|------|---------------|------------|------------|-----------|
| 1 | `java/implicit-cast-in-compound-assignment` | `deliverytiersuite/delivery-tier-suite/feeds/src/test/java/com/percussion/delivery/feeds/PSFeedServicePerformanceTest.java` | 582 | per-request delta is bounded by the @Threshold(value=2000) annotation (2s cap per request); the running `int` sum cannot exceed Integer.MAX_VALUE in any realistic run. The class is @Disabled and is a micro-benchmark for a single developer's laptop, not a production hot path; widening to long would obscure the test's intent. | 2026-07-13 | vijaya-boddipudi | 2026-07-13 |

