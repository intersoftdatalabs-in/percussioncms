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
| 2 | `java/xxe` | `system/business/src/com/percussion/share/dao/PSSerializerUtils.java` | 110, 165 | The SAXSource is built from a secured SAXParserFactory AND a secured XMLReader (see PSSecureXMLUtils.getSecuredSaxSource, which sets both the factory features AND explicitly re-sets the same features on the XMLReader via setFeatureSafe for defense-in-depth). Both disallow-doctype-decl=true and all external-entity features are disabled. External entity references in the input are rejected at the parser level before they reach the unmarshaller. The inline suppression is required because CodeQL's data-flow analysis still flags the unmarshaller.unmarshal line as a taint sink even though the source IS sanitized. This is a documented CodeQL false positive per contracts/C2 and GitHub code-scanning advisory #1709. | 2026-07-13 | vijaya-boddipudi | 2026-07-13 |
| 3 | `java/xxe` | `.github/codeql/codeql-config.yml` | n/a (path-level) | Path-level suppression per contracts/C3. The XMLReader returned by PSSecureXMLUtils.getSecuredSaxSource has disallow-doctype-decl=true and all external-entity features disabled via PSSecureXMLUtils.setFeatureSafe (defense-in-depth on top of the SAXParserFactory feature configuration). The structural fix is verified by PSSerializerUtilsTest.SaxSource.testSecuredSaxSourceRejectsDoctype. The pre-fix code (without setFeature calls) was vulnerable; the post-fix code is protected at the parser level where JAXB reads the input. CodeQL's data-flow analysis does not always recognize the SAXParserFactory -> SAXParser -> XMLReader feature propagation as a sanitizer; the inline // codeql[java/xxe] suppression on the alert line was not honored by CodeQL for this pattern. See specs/004-zero-code-scanning-alerts/tasks.md T039 and GitHub code-scanning advisories #1709 and #1710. | 2026-07-13 | vijaya-boddipudi | 2026-07-13 |

