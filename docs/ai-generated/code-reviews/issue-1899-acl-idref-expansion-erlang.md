# Erlang self-review — issue #1899 (ACL Betwixt idref expansion)

**Change class:** Jackson XML pre-read helper + package ACL install safety (residual of #1889).

## Checklist

|         Gate         |                                                                                                                                                            Result                                                                                                                                                             |
|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Bugs / logic errors  | None found: two-pass collect definitions then materialize idref stubs; keeps local element name for cross-name refs (`first-owner` → `typed-principal`); unresolved idrefs left as empty stubs                                                                                                                                |
| Behavioral tests     | `PSBetwixtIdrefExpanderTest` (7): no-op, permission expand, first-owner expand, unresolved stub, baseline package snippet, negative without expansion. `PSSecurityXmlSerializationTest` (11): package smoke asserts idref community entries regain `RUNTIME_VISIBLE`; synthetic equal permission sets for full+idref siblings |
| Portable paths       | DOM parse via `PSXmlDocumentBuilder` / classpath fixtures only; no OS path construction                                                                                                                                                                                                                                       |
| Companions           | Expander in `modules/utils`; wired in `PSJacksonXmlSerializationHelper.readFromXml`; deviations doc product decision **expand on read**; system security tests updated                                                                                                                                                        |
| Spotless             | apply → check on `modules/utils` + `system` (+ in-scope deviations md); out-of-scope baseline Spotless rewrites discarded                                                                                                                                                                                                     |
| Module clean install | `cd modules/utils && ../../mvnw clean install` BUILD SUCCESS (272 tests). `cd system && ../mvnw clean install` BUILD SUCCESS (1012 tests, 0 failures)                                                                                                                                                                         |

## Product decision

Expand Betwixt `idref` graphs on Jackson read rather than mass-rewriting shipped `*.aclDef` package files.

## Residual

None for this slice. Live CMS package install remains out of scope (offline smoke only), same as #1889.
