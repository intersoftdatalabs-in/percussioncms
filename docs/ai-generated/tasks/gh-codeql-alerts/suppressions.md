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

| 796 | `java/implicit-cast-in-compound-assignment` | `deliverytiersuite/delivery-tier-suite/feeds/src/test/java/com/percussion/delivery/feeds/PSFeedServicePerformanceTest.java` | 582 | Test perf micro-benchmark intentionally narrows long (millis-since-epoch difference) to int (running sum). The values are bounded by the test wall-clock duration (milliseconds), well within Integer.MAX_VALUE across the 100-iteration benchmark. The aggregate is asserted as `(sum / 100) < Threshold` for the average, so no precision loss is observable. See triage.md row for alert #796. | 2026-07-16 | kilo-code-bot | 2027-01-31 |
| 638 | `java/implicit-cast-in-compound-assignment` | `system/src/main/java/com/percussion/HTTPClient/BufferedInputStream.java` | 115 | Legacy HTTPClient library code path (alert #638). `pos` is the read position within an in-memory byte buffer; the buffer length is bounded by Integer.MAX_VALUE by definition (byte arrays are int-indexed). `n` is the value of an `int` parameter passed to the public skip() method, so the implicit cast cannot overflow. Refactoring to use long arithmetic would be a behavior-preserving cosmetic change with no observable benefit. | 2026-07-16 | kilo-code-bot | 2027-01-31 |
| 639 | `java/implicit-cast-in-compound-assignment` | `system/src/main/java/com/percussion/HTTPClient/RespInputStream.java` | 140 | Legacy HTTPClient library code path (alert #639). `offset` is the read position within the underlying HTTP response buffer; the buffer length is bounded by Integer.MAX_VALUE by definition (byte arrays are int-indexed). `num` is the long return value of `demux.skip()` but it represents a byte count bounded by the byte buffer length, so the implicit cast to int cannot overflow. Refactoring to use long arithmetic would be a behavior-preserving cosmetic change with no observable benefit. | 2026-07-16 | kilo-code-bot | 2027-01-31 |
